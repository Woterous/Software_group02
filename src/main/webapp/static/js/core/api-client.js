(function () {
    const cfg = window.TARS_CONFIG;

    const okEnvelope = (data, meta = null) => ({ success: true, data, meta, error: null });
    const errEnvelope = (code, message, details = []) => ({ success: false, data: null, meta: null, error: { code, message, details } });

    async function fetchJson(path, options = {}) {
        const isFormData = typeof FormData !== "undefined" && options.body instanceof FormData;
        const headers = {
            ...(options.headers || {})
        };
        const hasContentType = Object.keys(headers).some((key) => key.toLowerCase() === "content-type");
        if (!isFormData && !hasContentType) {
            headers["Content-Type"] = "application/json";
        }

        const res = await fetch(`${cfg.apiBasePath}${path}`, {
            credentials: "include",
            headers,
            ...options
        });
        const raw = await res.text();
        if (!raw) {
            if (!res.ok) {
                return errEnvelope(`HTTP_${res.status}`, `Request failed with status ${res.status}.`);
            }
            return okEnvelope({}, null);
        }
        try {
            return JSON.parse(raw);
        } catch (error) {
            return errEnvelope("SYSTEM_INVALID_RESPONSE", "Server response is not valid JSON.");
        }
    }

    function cvFileUrl(cvPath) {
        if (!cvPath) return "";
        const fileName = String(cvPath).replace(/\\/g, "/").split("/").pop();
        if (!fileName) return "";
        return `${cfg.apiBasePath}/files/cv/${encodeURIComponent(fileName)}`;
    }

    async function openCvFile(cvPath) {
        const url = cvFileUrl(cvPath);
        if (!url) {
            return errEnvelope("CV_FILE_MISSING", "No CV file is available.");
        }

        const previewWindow = window.open("about:blank", "_blank");
        if (!previewWindow) {
            return errEnvelope("CV_POPUP_BLOCKED", "The browser blocked the CV preview window.");
        }
        previewWindow.opener = null;
        previewWindow.document.write("<!doctype html><title>Opening CV</title><body style=\"font-family: sans-serif; padding: 24px;\">Opening CV...</body>");

        const res = await fetch(url, {
            credentials: "include",
            method: "GET"
        });
        if (!res.ok) {
            const raw = await res.text();
            let message = `CV file could not be opened. Server returned ${res.status}.`;
            try {
                const parsed = JSON.parse(raw);
                message = parsed?.error?.message || message;
            } catch (_) {
                // Non-JSON error bodies are still handled by the generic message.
            }
            previewWindow.document.body.innerHTML = `<p style="font-family: sans-serif; color: #8a1f27;">${message}</p>`;
            return errEnvelope(`HTTP_${res.status}`, message);
        }

        const blob = await res.blob();
        const objectUrl = URL.createObjectURL(blob);
        previewWindow.location.href = objectUrl;
        setTimeout(() => URL.revokeObjectURL(objectUrl), 60 * 1000);
        return okEnvelope({ opened: true });
    }

    function fromMock(result, meta = null) {
        if (!result.ok) {
            return errEnvelope(result.error.code, result.error.message, result.error.details || []);
        }
        return okEnvelope(result.data, meta || result.meta || null);
    }

    window.ApiClient = {
        mode: cfg.dataSource,
        cvFileUrl,
        openCvFile,

        async authLogin(payload) {
            if (this.mode === "mock") return fromMock(window.MockEngine.auth.login(payload));
            return fetchJson("/auth/login", { method: "POST", body: JSON.stringify(payload) });
        },
        async authRegister(payload, cvFile) {
            if (this.mode === "mock") {
                const mockPayload = { ...payload };
                if (cvFile && !mockPayload.cvPath) {
                    mockPayload.cvPath = `/uploads/${cvFile.name}`;
                }
                return fromMock(window.MockEngine.auth.register(mockPayload));
            }

            if (cvFile) {
                const formData = new FormData();
                Object.entries(payload || {}).forEach(([key, value]) => {
                    if (value == null) return;
                    formData.append(key, String(value));
                });
                formData.append("cvFile", cvFile);
                return fetchJson("/auth/register", { method: "POST", body: formData });
            }

            return fetchJson("/auth/register", { method: "POST", body: JSON.stringify(payload) });
        },
        async authLogout() {
            if (this.mode === "mock") return fromMock(window.MockEngine.auth.logout());
            return fetchJson("/auth/logout", { method: "DELETE" });
        },
        async authMe() {
            if (this.mode === "mock") return fromMock(window.MockEngine.auth.me());
            return fetchJson("/auth/me", { method: "GET" });
        },

        async taDashboard(userId) {
            if (this.mode === "mock") return fromMock(window.MockEngine.ta.dashboard(userId));
            return fetchJson("/ta/dashboard", { method: "GET" });
        },
        async taProfile(userId) {
            if (this.mode === "mock") return fromMock(window.MockEngine.ta.getProfile(userId));
            return fetchJson("/ta/profile", { method: "GET" });
        },
        async taUpdateProfile(userId, payload) {
            if (this.mode === "mock") return fromMock(window.MockEngine.ta.updateProfile(userId, payload));
            return fetchJson("/ta/profile", { method: "PUT", body: JSON.stringify(payload) });
        },
        async taUpdateCv(userId, payload) {
            if (this.mode === "mock") return fromMock(window.MockEngine.ta.updateCv(userId, payload.cvPath));
            return fetchJson("/ta/profile/cv", { method: "POST", body: JSON.stringify(payload) });
        },
        async taUploadCv(userId, file) {
            if (this.mode === "mock") return fromMock(window.MockEngine.ta.updateCv(userId, `/uploads/${file.name}`));
            const formData = new FormData();
            formData.append("cvFile", file);
            return fetchJson("/ta/profile/cv/upload", { method: "POST", body: formData });
        },
        async taDeleteCv(userId) {
            if (this.mode === "mock") return fromMock(window.MockEngine.ta.updateCv(userId, ""));
            return fetchJson("/ta/profile/cv", { method: "DELETE" });
        },
        async taListJobs(params) {
            if (this.mode === "mock") {
                const result = window.MockEngine.ta.listJobs(params);
                return fromMock(result, result.meta || null);
            }
            const query = new URLSearchParams(params).toString();
            return fetchJson(`/ta/jobs?${query}`, { method: "GET" });
        },
        async taJobDetail(jobId) {
            if (this.mode === "mock") return fromMock(window.MockEngine.ta.getJobDetail(jobId));
            return fetchJson(`/ta/jobs/${jobId}`, { method: "GET" });
        },
        async taApply(userId, jobId) {
            if (this.mode === "mock") return fromMock(window.MockEngine.ta.apply(userId, jobId));
            return fetchJson("/ta/applications", { method: "POST", body: JSON.stringify({ jobId }) });
        },
        async taMyApplications(userId, params) {
            if (this.mode === "mock") return fromMock(window.MockEngine.ta.listMyApplications(userId, params));
            const query = new URLSearchParams(params).toString();
            return fetchJson(`/ta/applications?${query}`, { method: "GET" });
        },

        async moDashboard() {
            if (this.mode === "mock") return fromMock(window.MockEngine.mo.dashboard());
            return fetchJson("/mo/dashboard", { method: "GET" });
        },
        async moListJobs(params, moUserId) {
            if (this.mode === "mock") return fromMock(window.MockEngine.mo.listJobs(params, moUserId));
            const query = new URLSearchParams(params).toString();
            return fetchJson(`/mo/jobs?${query}`, { method: "GET" });
        },
        async moSaveJob(payload, moUserId) {
            if (this.mode === "mock") return fromMock(window.MockEngine.mo.saveJob(payload, moUserId));
            const path = payload.jobId ? `/mo/jobs/${payload.jobId}` : "/mo/jobs";
            return fetchJson(path, { method: payload.jobId ? "PUT" : "POST", body: JSON.stringify(payload) });
        },
        async moListApplicants(params) {
            if (this.mode === "mock") return fromMock(window.MockEngine.mo.listApplicants(params));
            const query = new URLSearchParams(params).toString();
            return fetchJson(`/mo/applicants?${query}`, { method: "GET" });
        },
        async moReview(appId) {
            if (this.mode === "mock") return fromMock(window.MockEngine.mo.review(appId));
            return fetchJson(`/mo/review/${appId}`, { method: "GET" });
        },
        async moSetStatus(appId, status, reviewNote) {
            if (this.mode === "mock") return fromMock(window.MockEngine.mo.setStatus(appId, status, reviewNote));
            return fetchJson(`/mo/applications/${appId}/status`, { method: "PUT", body: JSON.stringify({ status, reviewNote }) });
        },

        async aiStatus() {
            return fetchJson("/ai/status", { method: "GET" });
        },
        async aiTaJobRecommendations() {
            return fetchJson("/ai/ta/job-recommendations", { method: "POST", body: JSON.stringify({}) });
        },
        async aiMoCandidateSummary(applicationId) {
            return fetchJson("/ai/mo/candidate-summary", { method: "POST", body: JSON.stringify({ applicationId }) });
        },
        async aiAdminRiskAnalysis(payload = {}) {
            return fetchJson("/ai/admin/risk-analysis", { method: "POST", body: JSON.stringify(payload) });
        },
        async aiChat(payload = {}) {
            return fetchJson("/ai/chat", { method: "POST", body: JSON.stringify(payload) });
        },

        async adminDashboard() {
            if (this.mode === "mock") return fromMock(window.MockEngine.admin.dashboard());
            return fetchJson("/admin/dashboard", { method: "GET" });
        },
        async adminListUsers(params) {
            if (this.mode === "mock") {
                const result = window.MockEngine.admin.listUsers(params);
                return fromMock(result, result.meta || null);
            }
            const query = new URLSearchParams(params).toString();
            return fetchJson(`/admin/users?${query}`, { method: "GET" });
        },
        async adminListApplications(params) {
            if (this.mode === "mock") return fromMock(window.MockEngine.admin.listApplications(params));
            const query = new URLSearchParams(params).toString();
            return fetchJson(`/admin/applications?${query}`, { method: "GET" });
        },
        async adminWorkload(params) {
            if (this.mode === "mock") return fromMock(window.MockEngine.admin.workload(params));
            const query = new URLSearchParams(params).toString();
            return fetchJson(`/admin/workload?${query}`, { method: "GET" });
        },

        lookups: {
            modules() {
                if (cfg.dataSource !== "mock") return [];
                return window.MockEngine.lookups.modules();
            },
            jobs() {
                if (cfg.dataSource !== "mock") return [];
                return window.MockEngine.lookups.jobs();
            }
        }
    };
})();
