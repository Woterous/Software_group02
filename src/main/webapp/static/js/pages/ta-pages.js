window.PageModules = window.PageModules || {};
window.PageModules.ta = window.PageModules.ta || {};

(function () {
    function requireSession() {
        return window.UIKit.ensureSessionOrRedirect(["ta"]);
    }

    function renderEmptyState(container, title, description) {
        if (!container) return;
        container.innerHTML = `
            <div class="empty-state">
                <strong>${window.UIKit.escapeHtml(title || "Nothing here yet")}</strong>
                <p>${window.UIKit.escapeHtml(description || "This area will update as soon as data becomes available.")}</p>
            </div>
        `;
    }

    function renderList(container, rows, renderer, emptyState) {
        if (!container) return;
        if (!rows.length) {
            renderEmptyState(
                container,
                emptyState?.title || "No data available",
                emptyState?.description || "This module will populate when new activity is available."
            );
            return;
        }
        container.innerHTML = rows.map(renderer).join("");
    }

    function parseSkills(rawSkills) {
        if (!rawSkills) return [];
        return String(rawSkills)
            .split(",")
            .map((item) => item.trim())
            .filter(Boolean);
    }

    function parseDate(rawDate) {
        if (!rawDate) return null;
        const date = new Date(`${rawDate}T00:00:00`);
        return Number.isNaN(date.getTime()) ? null : date;
    }

    function formatShortDate(rawDate) {
        const date = parseDate(rawDate);
        if (!date) return rawDate || "TBD";
        return new Intl.DateTimeFormat("en-US", {
            month: "short",
            day: "numeric"
        }).format(date);
    }

    function deadlineMeta(rawDate) {
        const date = parseDate(rawDate);
        if (!date) {
            return {
                tone: "muted",
                label: "Deadline TBD",
                detail: "Schedule not announced"
            };
        }

        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const diffDays = Math.round((date.getTime() - today.getTime()) / (24 * 60 * 60 * 1000));
        if (diffDays < 0) {
            return {
                tone: "expired",
                label: `${Math.abs(diffDays)}d overdue`,
                detail: `Deadline was ${formatShortDate(rawDate)}`
            };
        }
        if (diffDays === 0) {
            return {
                tone: "today",
                label: "Due today",
                detail: `Closes on ${formatShortDate(rawDate)}`
            };
        }
        if (diffDays <= 3) {
            return {
                tone: "urgent",
                label: `${diffDays}d left`,
                detail: `Closes ${formatShortDate(rawDate)}`
            };
        }
        if (diffDays <= 7) {
            return {
                tone: "watch",
                label: `${diffDays}d left`,
                detail: `Closes ${formatShortDate(rawDate)}`
            };
        }
        return {
            tone: "calm",
            label: formatShortDate(rawDate),
            detail: `Deadline ${formatShortDate(rawDate)}`
        };
    }

    function applicationStatusContext(status, reviewNote) {
        const normalized = String(status || "").toLowerCase();
        if (reviewNote) {
            return reviewNote;
        }
        if (normalized === "selected") {
            return "Positive decision recorded by the module owner.";
        }
        if (normalized === "rejected") {
            return "Final decision recorded. Review other active openings.";
        }
        return "Still under review by the module owner.";
    }

    async function initDashboard() {
        const session = requireSession();
        if (!session) return;
        const result = await window.ApiClient.taDashboard(session.userId);
        if (!result.success) {
            window.UIKit.toast(result.error.message, "error");
            return;
        }
        document.getElementById("ta-name").textContent = session.name;
        document.getElementById("ta-open-jobs").textContent = result.data.openJobs;
        document.getElementById("ta-submitted").textContent = result.data.submitted;
        document.getElementById("ta-pending").textContent = result.data.pending;
        document.getElementById("ta-selected").textContent = result.data.selected;

        const heroHighlight = document.getElementById("ta-hero-highlight");
        const heroNote = document.getElementById("ta-hero-note");
        if (heroHighlight && heroNote) {
            if (result.data.pending > 0) {
                heroHighlight.textContent = `${result.data.pending} application${result.data.pending > 1 ? "s are" : " is"} currently awaiting review`;
                heroNote.textContent = "Use the applications table to watch for decisions and keep your CV aligned with upcoming roles.";
            } else if (result.data.openJobs > 0) {
                heroHighlight.textContent = `${result.data.openJobs} active role${result.data.openJobs > 1 ? "s are" : " is"} worth reviewing now`;
                heroNote.textContent = "Prioritise roles with nearer deadlines first, then refine by module fit and required skills.";
            } else {
                heroHighlight.textContent = "Your workspace is up to date for now";
                heroNote.textContent = "Check back later for new openings or refresh your profile to stay application-ready.";
            }
        }

        renderList(document.getElementById("ta-latest-apps"), result.data.latestApplications, (row) => `
            <article class="activity-card">
                <div class="activity-card-main">
                    <div class="activity-card-head">
                        <strong>${window.UIKit.escapeHtml(row.title)}</strong>
                        ${window.UIKit.badge(row.status)}
                    </div>
                    <div class="activity-card-meta">
                        <span>${window.UIKit.escapeHtml(row.moduleName)}</span>
                        <span>Updated ${window.UIKit.escapeHtml(formatShortDate(row.updatedAt))}</span>
                    </div>
                    <p class="activity-card-note">${window.UIKit.escapeHtml(applicationStatusContext(row.status, row.reviewNote))}</p>
                </div>
            </article>
        `, {
            title: "No applications yet",
            description: "When you apply for roles, the latest movement will appear here with review context."
        });

        renderList(document.getElementById("ta-recommended-jobs"), result.data.recommendedJobs, (row) => `
            <article class="recommend-card">
                <div class="recommend-card-head">
                    <div>
                        <span class="job-card-eyebrow">Module ${window.UIKit.escapeHtml(row.moduleName)}</span>
                        <h4>${window.UIKit.escapeHtml(row.title)}</h4>
                    </div>
                    <span class="deadline-chip deadline-chip--${deadlineMeta(row.deadline).tone}">${window.UIKit.escapeHtml(deadlineMeta(row.deadline).label)}</span>
                </div>
                <p class="recommend-card-copy">${window.UIKit.escapeHtml(row.description)}</p>
                <div class="recommend-meta-row">
                    <span>${window.UIKit.escapeHtml(deadlineMeta(row.deadline).detail)}</span>
                    <span>${window.UIKit.escapeHtml(`${row.weeklyHours || "-"} hrs / week`)}</span>
                </div>
                <div class="skill-pill-list recommend-skill-list">
                    ${parseSkills(row.requiredSkills).slice(0, 3).map((skill) => `<span class="skill-pill">${window.UIKit.escapeHtml(skill)}</span>`).join("")}
                </div>
                <a class="text-link" href="${window.APP_CONTEXT}/pages/ta/job-detail?id=${window.UIKit.escapeHtml(row.jobId)}">View details</a>
            </article>
        `, {
            title: "No recommendations right now",
            description: "Open roles will surface here once there are active opportunities to review."
        });
    }

    async function initProfile() {
        const session = requireSession();
        if (!session) return;

        const profileForm = document.getElementById("ta-profile-form");
        const cvForm = document.getElementById("ta-cv-form");
        const cvCurrent = document.getElementById("ta-cv-current");
        const cvFileInput = cvForm.querySelector('input[name="cvFile"]');
        const cvDropzone = document.getElementById("ta-cv-dropzone");
        const cvPickBtn = document.getElementById("ta-cv-pick-btn");
        const cvSelectedFile = document.getElementById("ta-cv-selected-file");
        const maxCvSize = 5 * 1024 * 1024;
        const allowedCvExt = new Set(["pdf", "doc", "docx"]);
        let selectedCvFile = null;

        const getCvExtension = (filename) => {
            if (!filename || !filename.includes(".")) return "";
            return filename.split(".").pop().toLowerCase();
        };

        const formatFileSize = (bytes) => {
            if (!Number.isFinite(bytes) || bytes <= 0) return "0 KB";
            if (bytes < 1024 * 1024) {
                return `${Math.max(1, Math.round(bytes / 1024))} KB`;
            }
            return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
        };

        const updateCvSelectionLabel = () => {
            if (!cvSelectedFile) return;
            cvSelectedFile.textContent = selectedCvFile
                ? `${selectedCvFile.name} (${formatFileSize(selectedCvFile.size)})`
                : "No file selected";
        };

        const clearSelectedCvFile = () => {
            selectedCvFile = null;
            if (cvFileInput) {
                cvFileInput.value = "";
            }
            updateCvSelectionLabel();
        };

        const validateCvFile = (file) => {
            if (!file) {
                return "Please choose a CV file first.";
            }
            const ext = getCvExtension(file.name);
            if (!allowedCvExt.has(ext)) {
                return "Only PDF, DOC, and DOCX files are allowed.";
            }
            if (file.size > maxCvSize) {
                return "File size must be 5MB or less.";
            }
            return null;
        };

        const setSelectedCvFile = (file) => {
            const error = validateCvFile(file);
            if (error) {
                clearSelectedCvFile();
                window.UIKit.toast(error, "warn");
                return false;
            }
            selectedCvFile = file;
            if (cvFileInput) {
                try {
                    const transfer = new DataTransfer();
                    transfer.items.add(file);
                    cvFileInput.files = transfer.files;
                } catch (_) {
                    // Some environments prevent setting input.files directly.
                }
            }
            updateCvSelectionLabel();
            return true;
        };

        const load = async () => {
            const result = await window.ApiClient.taProfile(session.userId);
            if (!result.success) return;
            const profile = result.data.profile;
            profileForm.name.value = profile.name || "";
            profileForm.email.value = profile.email || "";
            profileForm.skills.value = (profile.skills || []).join(", ");
            profileForm.major.value = profile.major || "";
            profileForm.contact.value = profile.contact || "";
            const hasCv = !!profile.cvPath;
            const cvFileName = hasCv ? String(profile.cvPath).split("/").pop() : "";
            const cvUrl = hasCv ? window.ApiClient.cvFileUrl(profile.cvPath) : "";
            cvCurrent.innerHTML = `
                <span class="section-kicker">${hasCv ? "Document on file" : "CV missing"}</span>
                <strong>${hasCv ? "Current CV uploaded and ready for review" : "Upload a CV to complete your application profile"}</strong>
                <p>${window.UIKit.escapeHtml(hasCv ? cvFileName : "Module owners expect a current academic CV before reviewing your suitability.")}</p>
                <span class="cv-status-meta">${window.UIKit.escapeHtml(hasCv ? profile.cvPath : "Accepted formats: PDF, DOC, DOCX up to 5MB")}</span>
                ${hasCv && cvUrl ? `<button class="glass-secondary-btn inline cv-view-link" type="button" data-cv-path="${window.UIKit.escapeHtml(profile.cvPath)}">View CV</button>` : ""}
            `;
            const viewCvBtn = cvCurrent.querySelector("[data-cv-path]");
            if (viewCvBtn) {
                viewCvBtn.addEventListener("click", async () => {
                    viewCvBtn.disabled = true;
                    const openResult = await window.ApiClient.openCvFile(viewCvBtn.dataset.cvPath);
                    viewCvBtn.disabled = false;
                    if (!openResult.success) {
                        window.UIKit.toast(openResult.error.message, "error");
                    }
                });
            }
            cvCurrent.classList.toggle("is-empty", !hasCv);
        };

        if (cvPickBtn && cvFileInput) {
            cvPickBtn.addEventListener("click", (event) => {
                event.preventDefault();
                event.stopPropagation();
                cvFileInput.click();
            });
        }

        if (cvDropzone && cvFileInput) {
            cvDropzone.addEventListener("click", () => {
                cvFileInput.click();
            });

            cvDropzone.addEventListener("keydown", (event) => {
                if (event.key === "Enter" || event.key === " ") {
                    event.preventDefault();
                    cvFileInput.click();
                }
            });

            ["dragenter", "dragover"].forEach((eventName) => {
                cvDropzone.addEventListener(eventName, (event) => {
                    event.preventDefault();
                    event.stopPropagation();
                    cvDropzone.classList.add("is-dragover");
                });
            });

            ["dragleave", "drop"].forEach((eventName) => {
                cvDropzone.addEventListener(eventName, (event) => {
                    event.preventDefault();
                    event.stopPropagation();
                    cvDropzone.classList.remove("is-dragover");
                });
            });

            cvDropzone.addEventListener("drop", (event) => {
                const file = event.dataTransfer?.files?.[0];
                setSelectedCvFile(file);
            });
        }

        if (cvFileInput) {
            cvFileInput.addEventListener("change", () => {
                const file = cvFileInput.files?.[0];
                if (!file) {
                    clearSelectedCvFile();
                    return;
                }
                setSelectedCvFile(file);
            });
        }

        updateCvSelectionLabel();
        await load();

        profileForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const payload = window.UIKit.formToObject(profileForm);
            const result = await window.ApiClient.taUpdateProfile(session.userId, payload);
            if (!result.success) {
                window.UIKit.toast(result.error.message, "error");
                return;
            }
            window.UIKit.toast("Profile updated.", "success");
            await load();
        });

        cvForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const file = selectedCvFile || cvFileInput?.files?.[0];
            const error = validateCvFile(file);
            if (error) {
                window.UIKit.toast(error, "warn");
                return;
            }
            const result = await window.ApiClient.taUploadCv(session.userId, file);
            if (!result.success) {
                window.UIKit.toast(result.error.message, "error");
                return;
            }
            window.UIKit.toast("CV uploaded.", "success");
            cvForm.reset();
            clearSelectedCvFile();
            await load();
        });

        document.getElementById("ta-cv-remove").addEventListener("click", async () => {
            const result = await window.ApiClient.taDeleteCv(session.userId);
            if (result.success) {
                window.UIKit.toast("CV removed.", "warn");
                clearSelectedCvFile();
                await load();
            }
        });
    }

    async function initJobs() {
        const session = requireSession();
        if (!session) return;

        const form = document.getElementById("ta-job-filter-form");
        const listEl = document.getElementById("ta-job-list");
        const paginationEl = document.getElementById("ta-jobs-pagination");
        const aiBtn = document.getElementById("ta-ai-match-btn");
        const aiOutput = document.getElementById("ta-ai-recommendations");
        let currentPage = 1;

        const modules = window.ApiClient.lookups.modules();
        window.UIKit.setSelectOptions(form.module, modules, "value", "label", true, "All Modules");

        const normalizeTextList = (value) => Array.isArray(value)
            ? value.map((item) => String(item || "").trim()).filter(Boolean)
            : [];

        const renderAiList = (title, items, tone) => `
            <div class="ai-model-list ai-model-list--${tone}">
                <strong>${window.UIKit.escapeHtml(title)}</strong>
                <ul>
                    ${items.map((item) => `<li>${window.UIKit.escapeHtml(item)}</li>`).join("")}
                </ul>
            </div>
        `;

        const renderModelView = (modelView, provider) => {
            const priority = modelView?.priority || {};
            const strengths = normalizeTextList(modelView?.strengths);
            const risks = normalizeTextList(modelView?.risks);
            const nextActions = normalizeTextList(modelView?.nextActions);
            return `
                <article class="ai-model-summary">
                    <div class="ai-model-summary-head">
                        <span>${window.UIKit.escapeHtml(provider.mode || "tool-only")}</span>
                        <strong>${window.UIKit.escapeHtml(modelView?.headline || "Profile match analysis is ready.")}</strong>
                    </div>
                    <div class="ai-model-priority">
                        <div>
                            <span class="section-kicker">Priority role</span>
                            <h4>${window.UIKit.escapeHtml(priority.title || "Review top match")}</h4>
                            <p>${window.UIKit.escapeHtml(priority.reason || "Open the highest ranked role and confirm evidence before applying.")}</p>
                        </div>
                        ${priority.jobId ? `<span class="module-tag">${window.UIKit.escapeHtml(priority.jobId)}</span>` : ""}
                    </div>
                    <div class="ai-model-grid">
                        ${renderAiList("Strengths", strengths.length ? strengths : ["Matched skills and role data were reviewed."], "strength")}
                        ${renderAiList("Risks", risks.length ? risks : ["Confirm missing evidence before applying."], "risk")}
                        ${renderAiList("Next actions", nextActions.length ? nextActions : ["Open the role detail page and review the deadline."], "action")}
                    </div>
                </article>
            `;
        };

        const renderAiRecommendations = (data) => {
            const rows = data.recommendations || [];
            if (!aiOutput) return;
            if (!rows.length) {
                renderEmptyState(aiOutput, "No AI matches available", "Complete your profile skills or check again when new jobs are posted.");
                return;
            }
            const provider = data.provider || {};
            const assistantText = data.modelAnalysis || data.guidance || provider.message || "Review the generated matches before applying.";
            aiOutput.innerHTML = `
                ${data.modelView
                    ? renderModelView(data.modelView, provider)
                    : `<div class="ai-provider-note"><span>${window.UIKit.escapeHtml(provider.mode || "tool-only")}</span><p>${window.UIKit.escapeHtml(assistantText)}</p></div>`}
                ${rows.map((row) => `
                    <article class="ai-result-card">
                        <div class="ai-result-head">
                            <div>
                                <span class="section-kicker">${window.UIKit.escapeHtml(row.moduleName || "-")}</span>
                                <h4>${window.UIKit.escapeHtml(row.title || "-")}</h4>
                            </div>
                            <strong class="ai-score">${window.UIKit.escapeHtml(row.score)}%</strong>
                        </div>
                        <p>${window.UIKit.escapeHtml(row.rationale || "")}</p>
                        <div class="ai-chip-row">
                            ${(row.matchedSkills || []).map((skill) => `<span class="skill-pill">${window.UIKit.escapeHtml(skill)}</span>`).join("")}
                            ${row.alreadyApplied ? '<span class="badge pending">already applied</span>' : ""}
                        </div>
                        <a class="text-link" href="${window.APP_CONTEXT}/pages/ta/job-detail?id=${window.UIKit.escapeHtml(row.jobId)}">Open role detail</a>
                    </article>
                `).join("")}
            `;
        };

        if (aiBtn && aiOutput) {
            aiBtn.addEventListener("click", async () => {
                aiBtn.disabled = true;
                aiOutput.innerHTML = '<div class="ai-loading-card">Generating profile-based matches...</div>';
                const result = await window.ApiClient.aiTaJobRecommendations();
                aiBtn.disabled = false;
                if (!result.success) {
                    window.UIKit.toast(result.error.message, "error");
                    aiOutput.innerHTML = "";
                    return;
                }
                renderAiRecommendations(result.data);
            });
        }

        const load = async (page = 1) => {
            currentPage = page;
            const params = {
                page,
                size: 6,
                keyword: form.keyword.value.trim(),
                module: form.module.value,
                status: form.status.value
            };
            const result = await window.ApiClient.taListJobs(params);
            if (!result.success) {
                window.UIKit.toast(result.error.message, "error");
                return;
            }
            const rows = result.data.jobs;
            if (!rows.length) {
                renderEmptyState(
                    listEl,
                    "No roles match these filters",
                    "Broaden the keyword, remove a filter, or review a different module to discover more openings."
                );
                window.UIKit.renderPagination(paginationEl, result.meta, load);
                return;
            }

            listEl.innerHTML = rows.map((job) => {
                const deadline = deadlineMeta(job.deadline);
                const skills = parseSkills(job.requiredSkills);
                return `
                <article class="job-card job-card--${deadline.tone}">
                    <div class="job-card-head">
                        <div class="job-card-title-wrap">
                            <div class="job-card-topline">
                                <span class="job-card-eyebrow">Job ${window.UIKit.escapeHtml(job.jobId)}</span>
                                <span class="job-hours-chip">${window.UIKit.escapeHtml(`${job.weeklyHours || "-"} hrs / week`)}</span>
                            </div>
                            <h3>${window.UIKit.escapeHtml(job.title)}</h3>
                        </div>
                        <div class="job-card-status-group">
                            ${window.UIKit.badge(job.status)}
                            <span class="deadline-chip deadline-chip--${deadline.tone}">${window.UIKit.escapeHtml(deadline.label)}</span>
                        </div>
                    </div>
                    <p class="job-card-description">${window.UIKit.escapeHtml(job.description)}</p>
                    <div class="job-meta-grid">
                        <div class="job-meta-cell">
                            <span>Module</span>
                            <strong>${window.UIKit.escapeHtml(job.moduleName)}</strong>
                        </div>
                        <div class="job-meta-cell">
                            <span>Deadline</span>
                            <strong>${window.UIKit.escapeHtml(deadline.detail)}</strong>
                        </div>
                        <div class="job-meta-cell">
                            <span>Required Skills</span>
                            <strong>${window.UIKit.escapeHtml(skills.length ? `${skills.length} skill areas` : "Not specified")}</strong>
                        </div>
                    </div>
                    <div class="skill-pill-list job-card-skills">
                        ${skills.map((skill) => `<span class="skill-pill">${window.UIKit.escapeHtml(skill)}</span>`).join("")}
                    </div>
                    <div class="button-row job-card-actions">
                        <a class="glass-secondary-btn inline" href="${window.APP_CONTEXT}/pages/ta/job-detail?id=${window.UIKit.escapeHtml(job.jobId)}">View Details</a>
                        <button class="primary-btn" type="button" data-apply="${window.UIKit.escapeHtml(job.jobId)}">Quick Apply</button>
                    </div>
                    <p class="job-card-helper">Review the full role brief first if you are comparing multiple modules with similar deadlines.</p>
                </article>
            `;
            }).join("");

            listEl.querySelectorAll("[data-apply]").forEach((btn) => {
                btn.addEventListener("click", () => {
                    const jobId = btn.dataset.apply;
                    window.UIKit.openModal({
                        title: "Submit Application",
                        message: "Submit this application now?",
                        onConfirm: async () => {
                            const applyResult = await window.ApiClient.taApply(session.userId, jobId);
                            if (!applyResult.success) {
                                window.UIKit.toast(applyResult.error.message, "error");
                                return;
                            }
                            window.UIKit.toast("Application submitted.", "success");
                            await load(currentPage);
                        }
                    });
                });
            });

            window.UIKit.renderPagination(paginationEl, result.meta, load);
        };

        form.addEventListener("submit", (event) => {
            event.preventDefault();
            load(1);
        });

        await load(1);
    }

    async function initJobDetail() {
        const session = requireSession();
        if (!session) return;

        let jobId = window.UIKit.getQuery("id");
        if (!jobId) {
            const first = window.ApiClient.lookups.jobs()[0];
            jobId = first?.jobId;
        }
        if (!jobId) return;

        const result = await window.ApiClient.taJobDetail(jobId);
        if (!result.success) {
            window.UIKit.toast(result.error.message, "error");
            return;
        }

        const job = result.data.job;
        const card = document.getElementById("ta-job-detail-card");
        const skills = parseSkills(job.requiredSkills);
        const deadline = deadlineMeta(job.deadline);
        const skillHtml = skills.length
            ? skills.map((skill) => `<span class="skill-pill">${window.UIKit.escapeHtml(skill)}</span>`).join("")
            : '<span class="muted">No required skills listed.</span>';
        card.innerHTML = `
            <div class="job-detail-layout">
                <section class="job-detail-main">
                    <header class="job-detail-header">
                        <div class="job-detail-topline">
                            <span class="job-card-eyebrow">Job ${window.UIKit.escapeHtml(job.jobId)}</span>
                            <span class="deadline-chip deadline-chip--${deadline.tone}">${window.UIKit.escapeHtml(deadline.label)}</span>
                        </div>
                        <div class="job-detail-headline">
                            <h2>${window.UIKit.escapeHtml(job.title)}</h2>
                            ${window.UIKit.badge(job.status)}
                        </div>
                        <p class="job-detail-lead">${window.UIKit.escapeHtml(job.description)}</p>
                    </header>

                    <section class="job-detail-section">
                        <h4>Role Summary</h4>
                        <p class="muted">This role supports <strong>${window.UIKit.escapeHtml(job.moduleName)}</strong> and requires application readiness before <strong>${window.UIKit.escapeHtml(deadline.detail)}</strong>.</p>
                    </section>

                    <section class="job-detail-section">
                        <h4>Required Skills</h4>
                        <div class="skill-pill-list">
                            ${skillHtml}
                        </div>
                    </section>
                </section>

                <aside class="job-detail-side">
                    <h4>Key Information</h4>
                    <div class="detail-kv-grid">
                        <div class="detail-kv-item">
                            <span>Job ID</span>
                            <strong>${window.UIKit.escapeHtml(job.jobId)}</strong>
                        </div>
                        <div class="detail-kv-item">
                            <span>Module</span>
                            <strong>${window.UIKit.escapeHtml(job.moduleName)}</strong>
                        </div>
                        <div class="detail-kv-item">
                            <span>Deadline</span>
                            <strong>${window.UIKit.escapeHtml(deadline.detail)}</strong>
                        </div>
                        <div class="detail-kv-item">
                            <span>Status</span>
                            <strong>${window.UIKit.escapeHtml(job.status)}</strong>
                        </div>
                        <div class="detail-kv-item">
                            <span>Workload</span>
                            <strong>${window.UIKit.escapeHtml(`${job.weeklyHours || "-"} hrs / week`)}</strong>
                        </div>
                    </div>
                </aside>
            </div>
        `;

        const applyBtn = document.getElementById("ta-apply-btn");
        applyBtn.addEventListener("click", () => {
            window.UIKit.openModal({
                title: "Confirm Apply",
                message: `Apply for ${job.title}?`,
                onConfirm: async () => {
                    const applyResult = await window.ApiClient.taApply(session.userId, job.jobId);
                    if (!applyResult.success) {
                        window.UIKit.toast(applyResult.error.message, "error");
                        return;
                    }
                    window.UIKit.toast("Application submitted.", "success");
                }
            });
        });
    }

    async function initApplications() {
        const session = requireSession();
        if (!session) return;

        const form = document.getElementById("ta-app-filter-form");
        const tbody = document.getElementById("ta-app-table");

        const load = async () => {
            const params = {
                status: form.status.value,
                keyword: form.keyword.value.trim()
            };
            const result = await window.ApiClient.taMyApplications(session.userId, params);
            if (!result.success) {
                window.UIKit.toast(result.error.message, "error");
                return;
            }
            const rows = result.data.applications;
            if (!rows.length) {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="5">
                            <div class="empty-state empty-state--table">
                                <strong>No applications match this filter</strong>
                                <p>Try removing one filter or search for a different module keyword.</p>
                            </div>
                        </td>
                    </tr>
                `;
                return;
            }
            tbody.innerHTML = rows.map((row) => `
                <tr>
                    <td>
                        <div class="table-cell-primary">
                            <strong>${window.UIKit.escapeHtml(row.applicationId)}</strong>
                            <span class="table-meta">Updated ${window.UIKit.escapeHtml(formatShortDate(row.updatedAt))}</span>
                        </div>
                    </td>
                    <td>
                        <div class="table-cell-primary">
                            <strong>${window.UIKit.escapeHtml(row.title)}</strong>
                            <span class="table-meta">${window.UIKit.escapeHtml(applicationStatusContext(row.status, row.reviewNote))}</span>
                        </div>
                    </td>
                    <td><span class="module-tag">${window.UIKit.escapeHtml(row.moduleName)}</span></td>
                    <td>${window.UIKit.badge(row.status)}</td>
                    <td>
                        <div class="table-cell-secondary">
                            <strong>${window.UIKit.escapeHtml(formatShortDate(row.updatedAt))}</strong>
                            <span class="table-meta">${window.UIKit.escapeHtml(String(row.status || "").toLowerCase() === "pending" ? "Still active" : "Decision recorded")}</span>
                        </div>
                    </td>
                </tr>
            `).join("");
        };

        form.addEventListener("submit", (event) => {
            event.preventDefault();
            load();
        });

        await load();
    }

    window.PageModules.ta.dashboard = initDashboard;
    window.PageModules.ta.profile = initProfile;
    window.PageModules.ta.jobs = initJobs;
    window.PageModules.ta["job-detail"] = initJobDetail;
    window.PageModules.ta.applications = initApplications;
})();
