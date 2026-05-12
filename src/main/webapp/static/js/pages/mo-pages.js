window.PageModules = window.PageModules || {};
window.PageModules.mo = window.PageModules.mo || {};

(function () {
    function requireSession() {
        return window.UIKit.ensureSessionOrRedirect(["mo"]);
    }

    function renderStack(container, rows, renderItem) {
        if (!container) return;
        if (!rows.length) {
            container.innerHTML = '<div class="stack-item muted">No records available.</div>';
            return;
        }
        container.innerHTML = rows.map(renderItem).join("");
    }

    function bindCvPreviewButtons(scope) {
        if (!scope) return;
        scope.querySelectorAll("[data-cv-path]").forEach((btn) => {
            btn.addEventListener("click", async () => {
                btn.disabled = true;
                const result = await window.ApiClient.openCvFile(btn.dataset.cvPath);
                btn.disabled = false;
                if (!result.success) {
                    window.UIKit.toast(result.error.message, "error");
                }
            });
        });
    }

    async function initDashboard() {
        const session = requireSession();
        if (!session) return;

        const result = await window.ApiClient.moDashboard();
        if (!result.success) {
            window.UIKit.toast(result.error.message, "error");
            return;
        }

        document.getElementById("mo-active-jobs").textContent = result.data.activeJobs;
        document.getElementById("mo-total-applicants").textContent = result.data.totalApplicants;
        document.getElementById("mo-pending-review").textContent = result.data.pendingReview;
        document.getElementById("mo-selected-count").textContent = result.data.selectedCount;

        renderStack(document.getElementById("mo-near-deadline"), result.data.nearDeadline, (job) => `
            <div class="stack-item">
                <strong>${window.UIKit.escapeHtml(job.title)}</strong>
                <div class="job-meta">
                    <span>${window.UIKit.escapeHtml(job.moduleName)}</span>
                    <span>${window.UIKit.escapeHtml(job.deadline)}</span>
                    <span>${window.UIKit.badge(job.status)}</span>
                </div>
            </div>
        `);
    }

    async function initJobs() {
        const session = requireSession();
        if (!session) return;

        const table = document.getElementById("mo-job-table");
        const filterForm = document.getElementById("mo-job-filter-form");
        const editor = document.getElementById("mo-job-editor");
        const jobForm = document.getElementById("mo-job-form");
        let currentEditingId = "";

        const load = async () => {
            const params = {
                keyword: filterForm.keyword.value.trim(),
                status: filterForm.status.value
            };
            const result = await window.ApiClient.moListJobs(params, session.userId);
            if (!result.success) {
                window.UIKit.toast(result.error.message, "error");
                return;
            }
            table.innerHTML = result.data.jobs.map((job) => `
                <tr>
                    <td>${window.UIKit.escapeHtml(job.jobId)}</td>
                    <td>${window.UIKit.escapeHtml(job.title)}</td>
                    <td>${window.UIKit.escapeHtml(job.moduleName)}</td>
                    <td>${window.UIKit.escapeHtml(job.deadline)}</td>
                    <td>${window.UIKit.badge(job.status)}</td>
                    <td>${window.UIKit.escapeHtml(job.applicantCount)}</td>
                    <td>
                        <div class="table-action-group">
                            <button class="ghost-btn table-action-btn" type="button" data-edit="${window.UIKit.escapeHtml(job.jobId)}">Edit</button>
                            <a class="ghost-btn inline table-action-btn" href="${window.APP_CONTEXT}/pages/mo/applicants?jobId=${window.UIKit.escapeHtml(job.jobId)}">Applicants</a>
                        </div>
                    </td>
                </tr>
            `).join("");

            table.querySelectorAll("[data-edit]").forEach((btn) => {
                btn.addEventListener("click", () => {
                    const id = btn.dataset.edit;
                    const row = result.data.jobs.find((j) => j.jobId === id);
                    if (!row) return;
                    currentEditingId = id;
                    editor.classList.remove("hidden");
                    jobForm.title.value = row.title;
                    jobForm.moduleName.value = row.moduleName;
                    jobForm.deadline.value = row.deadline;
                    jobForm.status.value = row.status;
                    jobForm.status.dispatchEvent(new Event("change", { bubbles: true }));
                    jobForm.requiredSkills.value = row.requiredSkills;
                    jobForm.description.value = row.description;
                    window.UIKit.refreshSelectComponents(jobForm);
                });
            });
        };

        document.getElementById("mo-open-create-job").addEventListener("click", () => {
            currentEditingId = "";
            editor.classList.remove("hidden");
            jobForm.reset();
            window.UIKit.refreshSelectComponents(jobForm);
        });

        document.getElementById("mo-cancel-job").addEventListener("click", () => {
            editor.classList.add("hidden");
            jobForm.reset();
            currentEditingId = "";
            window.UIKit.refreshSelectComponents(jobForm);
        });

        filterForm.addEventListener("submit", (event) => {
            event.preventDefault();
            load();
        });

        jobForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const payload = window.UIKit.formToObject(jobForm);
            if (currentEditingId) payload.jobId = currentEditingId;
            const result = await window.ApiClient.moSaveJob(payload, session.userId);
            if (!result.success) {
                window.UIKit.toast(result.error.message, "error");
                return;
            }
            window.UIKit.toast(currentEditingId ? "Job updated." : "Job created.", "success");
            editor.classList.add("hidden");
            jobForm.reset();
            currentEditingId = "";
            window.UIKit.refreshSelectComponents(jobForm);
            await load();
        });

        await load();
    }

    async function initApplicants() {
        const session = requireSession();
        if (!session) return;

        const form = document.getElementById("mo-applicant-filter-form");
        const table = document.getElementById("mo-applicant-table");

        const jobsResult = await window.ApiClient.moListJobs({}, session.userId);
        const jobs = jobsResult.success ? jobsResult.data.jobs : [];
        window.UIKit.setSelectOptions(form.jobId, jobs.map((j) => ({ value: j.jobId, label: `${j.moduleName} (${j.jobId})` })), "value", "label", true, "All Jobs");

        const queryJobId = window.UIKit.getQuery("jobId");
        if (queryJobId) {
            form.jobId.value = queryJobId;
            form.jobId.dispatchEvent(new Event("change", { bubbles: true }));
        }

        const load = async () => {
            const params = {
                jobId: form.jobId.value,
                status: form.status.value,
                keyword: form.keyword.value.trim()
            };
            const result = await window.ApiClient.moListApplicants(params);
            if (!result.success) {
                window.UIKit.toast(result.error.message, "error");
                return;
            }
            table.innerHTML = result.data.applicants.map((row) => {
                const cvUrl = row.cvPath ? window.ApiClient.cvFileUrl(row.cvPath) : "";
                return `
                <tr>
                    <td>${window.UIKit.escapeHtml(row.applicantName)}</td>
                    <td>${window.UIKit.escapeHtml(row.title)}</td>
                    <td>${window.UIKit.escapeHtml(row.applicantSkills)}</td>
                    <td>${window.UIKit.badge(row.status)}</td>
                    <td>
                        ${cvUrl ? `<button class="glass-secondary-btn inline table-action-btn" type="button" data-cv-path="${window.UIKit.escapeHtml(row.cvPath)}">View CV</button>` : '<span class="muted">Not uploaded</span>'}
                    </td>
                    <td><a class="ghost-btn inline" href="${window.APP_CONTEXT}/pages/mo/review?appId=${window.UIKit.escapeHtml(row.applicationId)}">Review</a></td>
                </tr>
            `;
            }).join("");
            bindCvPreviewButtons(table);
        };

        form.addEventListener("submit", (event) => {
            event.preventDefault();
            load();
        });

        await load();
    }

    async function initReview() {
        const session = requireSession();
        if (!session) return;

        const appId = window.UIKit.getQuery("appId") || "";
        const result = await window.ApiClient.moReview(appId);
        if (!result.success) {
            window.UIKit.toast(result.error.message, "error");
            return;
        }

        const app = result.data.application;
        const cvUrl = app.cvPath ? window.ApiClient.cvFileUrl(app.cvPath) : "";
        const aiBtn = document.getElementById("mo-ai-summary-btn");
        const aiOutput = document.getElementById("mo-ai-summary");
        document.getElementById("mo-review-candidate").innerHTML = `
            <strong>${window.UIKit.escapeHtml(app.applicantName)}</strong>
            <div class="job-meta">
                <span>Application: ${window.UIKit.escapeHtml(app.applicationId)}</span>
                <span>Job: ${window.UIKit.escapeHtml(app.title)}</span>
                <span>Module: ${window.UIKit.escapeHtml(app.moduleName)}</span>
                <span>${window.UIKit.badge(app.status)}</span>
            </div>
            <div class="cv-review-strip">
                <div>
                    <span class="section-kicker">Candidate document</span>
                    <p class="muted">${app.cvPath ? window.UIKit.escapeHtml(app.cvPath) : "No CV has been uploaded by this applicant."}</p>
                </div>
                ${cvUrl ? `<button class="glass-secondary-btn inline" type="button" data-cv-path="${window.UIKit.escapeHtml(app.cvPath)}">View CV</button>` : '<span class="muted">Not uploaded</span>'}
            </div>
        `;
        bindCvPreviewButtons(document.getElementById("mo-review-candidate"));

        const skills = (app.applicantSkills || []).map((skill) => `<div class="stack-item">${window.UIKit.escapeHtml(skill)}</div>`).join("");
        document.getElementById("mo-skill-match").innerHTML = `
            <div class="stack-item"><strong>Required:</strong> ${window.UIKit.escapeHtml(app.requiredSkills || "-")}</div>
            ${skills || '<div class="stack-item muted">No skill records</div>'}
        `;

        const noteEl = document.getElementById("mo-review-note");
        noteEl.value = app.reviewNote || "";

        if (aiBtn && aiOutput) {
            aiBtn.addEventListener("click", async () => {
                aiBtn.disabled = true;
                aiOutput.innerHTML = '<div class="ai-loading-card">Generating candidate review summary...</div>';
                const summaryResult = await window.ApiClient.aiMoCandidateSummary(app.applicationId);
                aiBtn.disabled = false;
                if (!summaryResult.success) {
                    window.UIKit.toast(summaryResult.error.message, "error");
                    aiOutput.innerHTML = "";
                    return;
                }
                const data = summaryResult.data;
                const cv = data.cv || {};
                aiOutput.innerHTML = `
                    <article class="ai-result-card">
                        <div class="ai-result-head">
                            <div>
                                <span class="section-kicker">${window.UIKit.escapeHtml(data.provider?.mode || "tool-only")}</span>
                                <h4>Review brief</h4>
                            </div>
                            <span class="badge ${cv.uploaded ? "selected" : "pending"}">${cv.uploaded ? "cv available" : "cv missing"}</span>
                        </div>
                        <p>${window.UIKit.escapeHtml(data.summary || "")}</p>
                        <div class="ai-chip-row">
                            ${(data.matchedSkills || []).map((skill) => `<span class="skill-pill">${window.UIKit.escapeHtml(skill)}</span>`).join("")}
                        </div>
                    </article>
                    <article class="ai-result-card ai-result-card--quiet">
                        <strong>Questions to verify</strong>
                        <ul class="ai-question-list">
                            ${(data.reviewQuestions || []).map((question) => `<li>${window.UIKit.escapeHtml(question)}</li>`).join("")}
                        </ul>
                    </article>
                `;
            });
        }

        document.getElementById("mo-select-btn").addEventListener("click", () => submitStatus("selected", app.applicationId, noteEl.value));
        document.getElementById("mo-reject-btn").addEventListener("click", () => submitStatus("rejected", app.applicationId, noteEl.value));
    }

    async function submitStatus(status, appId, note) {
        window.UIKit.openModal({
            title: "Confirm Decision",
            message: `Set status to ${status}?`,
            onConfirm: async () => {
                const result = await window.ApiClient.moSetStatus(appId, status, note);
                if (!result.success) {
                    window.UIKit.toast(result.error.message, "error");
                    return;
                }
                window.UIKit.toast(`Application ${status}.`, "success");
            }
        });
    }

    window.PageModules.mo.dashboard = initDashboard;
    window.PageModules.mo.jobs = initJobs;
    window.PageModules.mo.applicants = initApplicants;
    window.PageModules.mo.review = initReview;
})();
