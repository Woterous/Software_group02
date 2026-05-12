window.PageModules = window.PageModules || {};
window.PageModules.admin = window.PageModules.admin || {};

(function () {
    function requireSession() {
        return window.UIKit.ensureSessionOrRedirect(["admin"]);
    }

    function renderRows(container, rows, mapper) {
        if (!container) return;
        if (!rows.length) {
            container.innerHTML = '<div class="stack-item muted">No records available.</div>';
            return;
        }
        container.innerHTML = rows.map(mapper).join("");
    }

    function renderStructuredAi(data, fallbackBody) {
        const view = data?.modelView;
        if (view && typeof window.UIKit.renderAiStructuredView === "function") {
            return window.UIKit.renderAiStructuredView({
                providerMode: data.provider?.mode || "tool-only",
                headline: view.headline,
                priority: view.priority,
                sections: view.sections
            });
        }
        return `
            <div class="ai-provider-note">
                <span>${window.UIKit.escapeHtml(data?.provider?.mode || "tool-only")}</span>
                <p>${window.UIKit.escapeHtml(fallbackBody || data?.summary || "No risk summary generated.")}</p>
            </div>
        `;
    }

    async function initDashboard() {
        const session = requireSession();
        if (!session) return;

        const result = await window.ApiClient.adminDashboard();
        if (!result.success) {
            window.UIKit.toast(result.error.message, "error");
            return;
        }

        document.getElementById("admin-total-users").textContent = result.data.totalUsers;
        document.getElementById("admin-open-jobs").textContent = result.data.openJobs;
        document.getElementById("admin-total-apps").textContent = result.data.totalApplications;
        document.getElementById("admin-overload-count").textContent = result.data.overloadCount;

        renderRows(document.getElementById("admin-recent-apps"), result.data.recentApplications, (app) => `
            <div class="stack-item">
                <strong>${window.UIKit.escapeHtml(app.applicantName)}</strong>
                <div class="job-meta">
                    <span>${window.UIKit.escapeHtml(app.title)}</span>
                    <span>${window.UIKit.badge(app.status)}</span>
                    <span>${window.UIKit.escapeHtml(app.updatedAt)}</span>
                </div>
            </div>
        `);

        renderRows(document.getElementById("admin-workload-alerts"), result.data.overloadUsers, (user) => `
            <div class="stack-item">
                <strong>${window.UIKit.escapeHtml(user.name)}</strong>
                <div class="job-meta">
                    <span>Modules: ${window.UIKit.escapeHtml(user.selectedModules)}</span>
                    <span>Hours: ${window.UIKit.escapeHtml(user.totalHours)}</span>
                    <span>${window.UIKit.badge(user.riskLevel)}</span>
                </div>
            </div>
        `);
    }

    async function initUsers() {
        const session = requireSession();
        if (!session) return;

        const form = document.getElementById("admin-user-filter-form");
        const table = document.getElementById("admin-user-table");
        const pagination = document.getElementById("admin-users-pagination");
        let currentPage = 1;

        const load = async (page = 1) => {
            currentPage = page;
            const params = {
                page,
                size: 8,
                role: form.role.value,
                keyword: form.keyword.value.trim()
            };
            const result = await window.ApiClient.adminListUsers(params);
            if (!result.success) {
                window.UIKit.toast(result.error.message, "error");
                return;
            }
            table.innerHTML = result.data.users.map((user) => `
                <tr>
                    <td>${window.UIKit.escapeHtml(user.userId)}</td>
                    <td>${window.UIKit.escapeHtml(user.name)}</td>
                    <td>${window.UIKit.escapeHtml(user.email)}</td>
                    <td>${window.UIKit.badge(user.role)}</td>
                    <td>${window.UIKit.escapeHtml((user.skills || []).join(", "))}</td>
                </tr>
            `).join("");
            window.UIKit.renderPagination(pagination, result.meta, load);
        };

        form.addEventListener("submit", (event) => {
            event.preventDefault();
            load(1);
        });

        await load(currentPage);
    }

    async function initApplications() {
        const session = requireSession();
        if (!session) return;

        const form = document.getElementById("admin-application-filter-form");
        const table = document.getElementById("admin-application-table");

        window.UIKit.setSelectOptions(form.module, window.ApiClient.lookups.modules(), "value", "label", true, "All Modules");

        const load = async () => {
            const params = {
                status: form.status.value,
                module: form.module.value,
                keyword: form.keyword.value.trim()
            };
            const result = await window.ApiClient.adminListApplications(params);
            if (!result.success) {
                window.UIKit.toast(result.error.message, "error");
                return;
            }
            table.innerHTML = result.data.applications.map((row) => `
                <tr>
                    <td>${window.UIKit.escapeHtml(row.applicationId)}</td>
                    <td>${window.UIKit.escapeHtml(row.applicantName)}</td>
                    <td>${window.UIKit.escapeHtml(row.title)}</td>
                    <td>${window.UIKit.escapeHtml(row.moduleName)}</td>
                    <td>${window.UIKit.badge(row.status)}</td>
                    <td>${window.UIKit.escapeHtml(row.updatedAt)}</td>
                </tr>
            `).join("");
        };

        form.addEventListener("submit", (event) => {
            event.preventDefault();
            load();
        });

        await load();
    }

    async function initWorkload() {
        const session = requireSession();
        if (!session) return;

        const form = document.getElementById("admin-workload-filter-form");
        const table = document.getElementById("admin-workload-table");
        const aiBtn = document.getElementById("admin-ai-risk-btn");
        const aiOutput = document.getElementById("admin-ai-risk-output");

        const load = async () => {
            const params = {
                riskLevel: form.riskLevel.value
            };
            const result = await window.ApiClient.adminWorkload(params);
            if (!result.success) {
                window.UIKit.toast(result.error.message, "error");
                return;
            }
            table.innerHTML = result.data.workload.map((row) => `
                <tr>
                    <td>${window.UIKit.escapeHtml(row.name)}</td>
                    <td>${window.UIKit.escapeHtml(row.selectedModules)}</td>
                    <td>${window.UIKit.escapeHtml(row.totalHours)}</td>
                    <td>${window.UIKit.badge(row.riskLevel)}</td>
                </tr>
            `).join("");
        };

        form.addEventListener("submit", (event) => {
            event.preventDefault();
            load();
        });

        if (aiBtn && aiOutput) {
            aiBtn.addEventListener("click", async () => {
                aiBtn.disabled = true;
                aiOutput.innerHTML = '<div class="ai-loading-card">Analyzing workload and role-level risks...</div>';
                const result = await window.ApiClient.aiAdminRiskAnalysis({ riskLevel: form.riskLevel.value });
                aiBtn.disabled = false;
                if (!result.success) {
                    window.UIKit.toast(result.error.message, "error");
                    aiOutput.innerHTML = "";
                    return;
                }
                const data = result.data;
                aiOutput.innerHTML = `
                    ${renderStructuredAi(data, data.summary)}
                    <div class="ai-grid-two">
                        <article class="ai-result-card">
                            <h4>People risk</h4>
                            ${(data.riskPeople || []).slice(0, 4).map((row) => `
                                <div class="ai-mini-row">
                                    <strong>${window.UIKit.escapeHtml(row.name)}</strong>
                                    <span>${window.UIKit.escapeHtml(row.totalHours)} hrs/week</span>
                                    ${window.UIKit.badge(row.riskLevel)}
                                </div>
                            `).join("") || '<p class="muted">No people risk under the selected filter.</p>'}
                        </article>
                        <article class="ai-result-card">
                            <h4>Role signals</h4>
                            ${(data.roleSignals || []).slice(0, 4).map((row) => `
                                <div class="ai-mini-row">
                                    <strong>${window.UIKit.escapeHtml(row.moduleName)}</strong>
                                    <span>${window.UIKit.escapeHtml(row.reason)}</span>
                                </div>
                            `).join("") || '<p class="muted">No role-level risk signal detected.</p>'}
                        </article>
                    </div>
                `;
            });
        }

        await load();
    }

    window.PageModules.admin.dashboard = initDashboard;
    window.PageModules.admin.users = initUsers;
    window.PageModules.admin.applications = initApplications;
    window.PageModules.admin.workload = initWorkload;
})();
