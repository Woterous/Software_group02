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

    const moduleLookups = [
        { value: "EBU6301", label: "EBU6301" },
        { value: "EBU6302", label: "EBU6302" },
        { value: "EBU6303", label: "EBU6303" },
        { value: "EBU6304", label: "EBU6304" },
        { value: "EBU6305", label: "EBU6305" },
        { value: "EBU6306", label: "EBU6306" },
        { value: "EBU5201", label: "EBU5201" }
    ];

    window.ApiClient = {
        mode: "api",
        cvFileUrl,
        openCvFile,

        async authLogin(payload) {
            return fetchJson("/auth/login", { method: "POST", body: JSON.stringify(payload) });
        },
        async authRegister(payload, cvFile) {
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
            return fetchJson("/auth/logout", { method: "DELETE" });
        },
        async authMe() {
            return fetchJson("/auth/me", { method: "GET" });
        },

        async taDashboard() {
            return fetchJson("/ta/dashboard", { method: "GET" });
        },
        async taProfile() {
            return fetchJson("/ta/profile", { method: "GET" });
        },
        async taUpdateProfile(userId, payload) {
            return fetchJson("/ta/profile", { method: "PUT", body: JSON.stringify(payload) });
        },
        async taUpdateCv(userId, payload) {
            return fetchJson("/ta/profile/cv", { method: "POST", body: JSON.stringify(payload) });
        },
        async taUploadCv(userId, file) {
            const formData = new FormData();
            formData.append("cvFile", file);
            return fetchJson("/ta/profile/cv/upload", { method: "POST", body: formData });
        },
        async taDeleteCv() {
            return fetchJson("/ta/profile/cv", { method: "DELETE" });
        },
        async taListJobs(params) {
            const query = new URLSearchParams(params).toString();
            return fetchJson(`/ta/jobs?${query}`, { method: "GET" });
        },
        async taJobDetail(jobId) {
            return fetchJson(`/ta/jobs/${jobId}`, { method: "GET" });
        },
        async taApply(userId, jobId) {
            return fetchJson("/ta/applications", { method: "POST", body: JSON.stringify({ jobId }) });
        },
        async taMyApplications(userId, params) {
            const query = new URLSearchParams(params).toString();
            return fetchJson(`/ta/applications?${query}`, { method: "GET" });
        },

        async moDashboard() {
            return fetchJson("/mo/dashboard", { method: "GET" });
        },
        async moListJobs(params) {
            const query = new URLSearchParams(params).toString();
            return fetchJson(`/mo/jobs?${query}`, { method: "GET" });
        },
        async moSaveJob(payload) {
            const path = payload.jobId ? `/mo/jobs/${payload.jobId}` : "/mo/jobs";
            return fetchJson(path, { method: payload.jobId ? "PUT" : "POST", body: JSON.stringify(payload) });
        },
        async moListApplicants(params) {
            const query = new URLSearchParams(params).toString();
            return fetchJson(`/mo/applicants?${query}`, { method: "GET" });
        },
        async moReview(appId) {
            return fetchJson(`/mo/review/${appId}`, { method: "GET" });
        },
        async moSetStatus(appId, status, reviewNote) {
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
            return fetchJson("/admin/dashboard", { method: "GET" });
        },
        async adminListUsers(params) {
            const query = new URLSearchParams(params).toString();
            return fetchJson(`/admin/users?${query}`, { method: "GET" });
        },
        async adminListApplications(params) {
            const query = new URLSearchParams(params).toString();
            return fetchJson(`/admin/applications?${query}`, { method: "GET" });
        },
        async adminWorkload(params) {
            const query = new URLSearchParams(params).toString();
            return fetchJson(`/admin/workload?${query}`, { method: "GET" });
        },

        lookups: {
            modules() {
                return moduleLookups;
            },
            jobs() {
                return [];
            }
        }
    };
})();
