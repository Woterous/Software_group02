(function () {
    let selectRuntimeReady = false;

    function byId(id) {
        return document.getElementById(id);
    }

    function escapeHtml(input) {
        const div = document.createElement("div");
        div.textContent = input == null ? "" : String(input);
        return div.innerHTML;
    }

    function closeCustomSelect(root) {
        if (!root) return;
        root.classList.remove("is-open");
        const trigger = root.querySelector(".glass-select-trigger");
        if (trigger) trigger.setAttribute("aria-expanded", "false");
    }

    function positionCustomSelectPanel(root) {
        if (!root) return;
        const trigger = root.querySelector(".glass-select-trigger");
        const panel = root.querySelector(".glass-select-panel");
        if (!trigger || !panel) return;

        const triggerRect = trigger.getBoundingClientRect();
        const viewportGap = 12;
        const panelGap = 8;
        const preferredMaxHeight = 260;
        const belowSpace = window.innerHeight - triggerRect.bottom - viewportGap;
        const aboveSpace = triggerRect.top - viewportGap;
        const openUp = belowSpace < 170 && aboveSpace > belowSpace;
        const availableHeight = Math.max(120, Math.min(preferredMaxHeight, openUp ? aboveSpace - panelGap : belowSpace - panelGap));

        panel.style.left = `${triggerRect.left}px`;
        panel.style.width = `${triggerRect.width}px`;
        panel.style.maxHeight = `${availableHeight}px`;
        panel.style.top = openUp ? "auto" : `${triggerRect.bottom + panelGap}px`;
        panel.style.bottom = openUp ? `${window.innerHeight - triggerRect.top + panelGap}px` : "auto";
        panel.dataset.placement = openUp ? "top" : "bottom";
    }

    function closeAllCustomSelects(except) {
        document.querySelectorAll(".glass-select.is-open").forEach((root) => {
            if (except && root === except) return;
            closeCustomSelect(root);
        });
    }

    function hasOpenCustomSelect() {
        return !!document.querySelector(".glass-select.is-open");
    }

    function updateSelectLabel(selectEl, trigger) {
        if (!selectEl || !trigger) return;
        const labelEl = trigger.querySelector(".glass-select-trigger-label");
        if (!labelEl) return;
        const selected = selectEl.options[selectEl.selectedIndex];
        labelEl.textContent = selected ? selected.textContent : "Select";
    }

    function syncCustomSelect(selectEl) {
        if (!selectEl) return;
        const root = selectEl.closest(".glass-select");
        if (!root) return;

        const trigger = root.querySelector(".glass-select-trigger");
        const panel = root.querySelector(".glass-select-panel");
        if (!trigger || !panel) return;

        panel.innerHTML = "";
        Array.from(selectEl.options).forEach((opt) => {
            const option = document.createElement("button");
            option.type = "button";
            option.className = "glass-select-option";
            option.dataset.value = opt.value;
            option.textContent = opt.textContent;
            option.disabled = !!opt.disabled;
            if (opt.value === selectEl.value) option.classList.add("active");

            option.addEventListener("click", () => {
                if (opt.disabled) return;
                if (selectEl.value !== opt.value) {
                    selectEl.value = opt.value;
                    selectEl.dispatchEvent(new Event("change", { bubbles: true }));
                } else {
                    updateSelectLabel(selectEl, trigger);
                }
                closeCustomSelect(root);
                trigger.focus();
            });

            panel.appendChild(option);
        });

        updateSelectLabel(selectEl, trigger);
        trigger.disabled = !!selectEl.disabled;
        if (root.classList.contains("is-open")) {
            positionCustomSelectPanel(root);
        }
    }

    function buildCustomSelect(selectEl) {
        if (!selectEl || selectEl.multiple || selectEl.dataset.nativeSelect === "true") return;
        if (selectEl.closest(".role-segmented")) return;
        if (selectEl.closest(".glass-select")) {
            syncCustomSelect(selectEl);
            return;
        }

        const root = document.createElement("div");
        root.className = "glass-select";
        selectEl.parentNode.insertBefore(root, selectEl);
        root.appendChild(selectEl);
        selectEl.classList.add("native-select-hidden");
        selectEl.setAttribute("tabindex", "-1");

        const trigger = document.createElement("button");
        trigger.type = "button";
        trigger.className = "glass-select-trigger";
        trigger.setAttribute("aria-haspopup", "listbox");
        trigger.setAttribute("aria-expanded", "false");
        trigger.innerHTML = `
            <span class="glass-select-trigger-label">Select</span>
            <span class="glass-select-trigger-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24">
                    <path d="m6 9 6 6 6-6"></path>
                </svg>
            </span>
        `;

        const panel = document.createElement("div");
        panel.className = "glass-select-panel";
        panel.setAttribute("role", "listbox");

        trigger.addEventListener("click", () => {
            syncCustomSelect(selectEl);
            const willOpen = !root.classList.contains("is-open");
            closeAllCustomSelects(root);
            if (willOpen) {
                root.classList.add("is-open");
                trigger.setAttribute("aria-expanded", "true");
                positionCustomSelectPanel(root);
            } else {
                closeCustomSelect(root);
            }
        });

        trigger.addEventListener("keydown", (event) => {
            if (event.key === "ArrowDown" || event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                if (!root.classList.contains("is-open")) {
                    closeAllCustomSelects(root);
                    root.classList.add("is-open");
                    trigger.setAttribute("aria-expanded", "true");
                    positionCustomSelectPanel(root);
                }
                const active = panel.querySelector(".glass-select-option.active") || panel.querySelector(".glass-select-option");
                active?.focus();
            }
            if (event.key === "Escape") {
                closeCustomSelect(root);
            }
        });

        panel.addEventListener("keydown", (event) => {
            if (event.key === "Escape") {
                closeCustomSelect(root);
                trigger.focus();
            }
        });

        selectEl.addEventListener("change", () => {
            syncCustomSelect(selectEl);
        });

        root.appendChild(trigger);
        root.appendChild(panel);
        syncCustomSelect(selectEl);
    }

    function initCustomSelects(scope = document) {
        const source = scope && scope.querySelectorAll ? scope : document;
        source.querySelectorAll("select").forEach((selectEl) => buildCustomSelect(selectEl));

        if (selectRuntimeReady) return;
        selectRuntimeReady = true;

        let viewportCloseFrame = null;
        const closeSelectsOnViewportShift = () => {
            if (!hasOpenCustomSelect()) return;
            if (viewportCloseFrame !== null) return;
            viewportCloseFrame = window.requestAnimationFrame(() => {
                viewportCloseFrame = null;
                closeAllCustomSelects();
            });
        };

        document.addEventListener("click", (event) => {
            if (!event.target.closest(".glass-select")) closeAllCustomSelects();
        });

        window.addEventListener("resize", closeSelectsOnViewportShift, { passive: true });
        window.addEventListener("scroll", closeSelectsOnViewportShift, { passive: true });
    }

    function refreshSelectComponents(scope = document) {
        const source = scope && scope.querySelectorAll ? scope : document;
        source.querySelectorAll("select").forEach((selectEl) => {
            if (selectEl.closest(".glass-select")) {
                syncCustomSelect(selectEl);
            } else {
                buildCustomSelect(selectEl);
            }
        });
    }

    function toast(message, level = "info", timeout = 2600) {
        const root = byId("toast-root");
        if (!root) return;
        const node = document.createElement("div");
        node.className = `toast ${level}`;
        node.innerHTML = `<strong>${level.toUpperCase()}</strong><div>${escapeHtml(message)}</div>`;
        root.appendChild(node);
        setTimeout(() => {
            node.style.opacity = "0";
            node.style.transform = "translateX(20px)";
            setTimeout(() => node.remove(), 180);
        }, timeout);
    }

    function setSelectOptions(selectEl, items, valueKey = "value", labelKey = "label", includeAll = false, allLabel = "All") {
        if (!selectEl) return;
        const current = selectEl.value;
        let html = includeAll ? `<option value="">${allLabel}</option>` : "";
        html += items.map((item) => {
            const v = typeof item === "string" ? item : item[valueKey];
            const l = typeof item === "string" ? item : item[labelKey];
            return `<option value="${escapeHtml(v)}">${escapeHtml(l)}</option>`;
        }).join("");
        selectEl.innerHTML = html;
        if ([...selectEl.options].some((o) => o.value === current)) {
            selectEl.value = current;
        }
        selectEl.dispatchEvent(new Event("change", { bubbles: true }));
        refreshSelectComponents(selectEl.closest("form") || document);
    }

    function formToObject(form) {
        const fd = new FormData(form);
        return Object.fromEntries(fd.entries());
    }

    function getQuery(name) {
        return new URLSearchParams(window.location.search).get(name);
    }

    function badge(status) {
        const safe = (status || "").toLowerCase();
        return `<span class="badge ${safe}">${escapeHtml(status || "-")}</span>`;
    }

    function formatDate(date) {
        return date || "-";
    }

    function renderPagination(container, meta, onChange) {
        if (!container || !meta) return;
        const chips = [];
        const total = meta.totalPages || 1;
        for (let p = 1; p <= total; p += 1) {
            chips.push(`<button class="page-chip ${p === meta.page ? "active" : ""}" data-page="${p}">${p}</button>`);
        }
        container.innerHTML = chips.join("");
        container.querySelectorAll(".page-chip").forEach((btn) => {
            btn.addEventListener("click", () => onChange(Number(btn.dataset.page)));
        });
    }

    function roleHome(role) {
        if (role === "mo") return `${window.APP_CONTEXT}/pages/mo/dashboard`;
        if (role === "admin") return `${window.APP_CONTEXT}/pages/admin/dashboard`;
        return `${window.APP_CONTEXT}/pages/ta/dashboard`;
    }

    function openModal({ title, message, onConfirm }) {
        const root = byId("modal-root");
        if (!root) return;
        byId("modal-title").textContent = title || "Confirm";
        byId("modal-message").textContent = message || "Please confirm this action.";
        root.classList.remove("hidden");

        const close = () => {
            root.classList.add("hidden");
            confirmBtn.removeEventListener("click", confirmHandler);
            cancelBtn.removeEventListener("click", close);
            backdrop.removeEventListener("click", close);
        };

        const confirmHandler = () => {
            close();
            if (typeof onConfirm === "function") onConfirm();
        };

        const confirmBtn = root.querySelector('[data-action="modal-confirm"]');
        const cancelBtn = root.querySelector('[data-action="modal-cancel"]');
        const backdrop = root.querySelector('[data-action="modal-close"]');

        confirmBtn.addEventListener("click", confirmHandler);
        cancelBtn.addEventListener("click", close);
        backdrop.addEventListener("click", close);
    }

    function initSidebarSegmentedNav() {
        document.querySelectorAll(".nav-segmented").forEach((navRoot) => {
            if (navRoot.dataset.segmentedReady === "1") return;
            navRoot.dataset.segmentedReady = "1";

            const indicator = navRoot.querySelector(".nav-indicator");
            const items = Array.from(navRoot.querySelectorAll(".nav-item"));
            if (!indicator || !items.length) return;

            const moveTo = (item) => {
                if (!item) return;
                const itemRect = item.getBoundingClientRect();
                const rootRect = navRoot.getBoundingClientRect();
                indicator.style.height = `${itemRect.height}px`;
                indicator.style.transform = `translate3d(0, ${itemRect.top - rootRect.top}px, 0)`;
            };

            const activeItem = items.find((item) => item.classList.contains("active")) || items[0];
            let navRect = null;
            let pointerFrame = null;
            let pointerX = 0;
            let pointerY = 0;

            const refreshNavRect = () => {
                navRect = navRoot.getBoundingClientRect();
            };

            const syncPointerGlow = () => {
                pointerFrame = null;
                if (!navRect) refreshNavRect();
                navRoot.style.setProperty("--x", `${pointerX - navRect.left}px`);
                navRoot.style.setProperty("--y", `${pointerY - navRect.top}px`);
            };

            moveTo(activeItem);
            refreshNavRect();

            navRoot.addEventListener("mousemove", (event) => {
                pointerX = event.clientX;
                pointerY = event.clientY;
                if (pointerFrame !== null) return;
                pointerFrame = window.requestAnimationFrame(syncPointerGlow);
            });

            navRoot.addEventListener("mouseleave", () => {
                if (pointerFrame !== null) {
                    window.cancelAnimationFrame(pointerFrame);
                    pointerFrame = null;
                }
                navRoot.style.setProperty("--x", "50%");
                navRoot.style.setProperty("--y", "50%");
                moveTo(activeItem);
            });

            navRoot.addEventListener("mouseenter", refreshNavRect);

            items.forEach((item) => {
                item.addEventListener("mouseenter", () => moveTo(item));
                item.addEventListener("focus", () => moveTo(item));
            });

            window.addEventListener("resize", () => {
                refreshNavRect();
                window.requestAnimationFrame(() => moveTo(activeItem));
            }, { passive: true });

            window.addEventListener("scroll", () => {
                navRect = null;
            }, { passive: true });
        });
    }

    function bindGlobalActions() {
        initCustomSelects(document);
        initSidebarSegmentedNav();

        document.querySelectorAll('[data-action="theme-toggle"]').forEach((btn) => {
            btn.addEventListener("click", () => {
                document.body.classList.toggle("theme-soft");
            });
        });

        document.querySelectorAll('[data-action="logout"]').forEach((btn) => {
            btn.addEventListener("click", async () => {
                btn.disabled = true;
                const result = await window.ApiClient.authLogout();
                if (!result.success) {
                    toast(result.error?.message || "Logout failed.", "error");
                    btn.disabled = false;
                    return;
                }
                if (window.MockEngine && typeof window.MockEngine.setSession === "function") {
                    window.MockEngine.setSession(null);
                }
                window.location.href = `${window.APP_CONTEXT}/pages/login`;
            });
        });
    }

    function ensureSessionOrRedirect(allowedRoles) {
        const session = window.MockEngine.getSession();
        if (!session || (allowedRoles && !allowedRoles.includes(session.role))) {
            window.location.href = `${window.APP_CONTEXT}/pages/login`;
            return null;
        }
        return session;
    }

    window.UIKit = {
        byId,
        toast,
        badge,
        formToObject,
        getQuery,
        formatDate,
        setSelectOptions,
        renderPagination,
        roleHome,
        openModal,
        bindGlobalActions,
        ensureSessionOrRedirect,
        escapeHtml,
        refreshSelectComponents
    };
})();
