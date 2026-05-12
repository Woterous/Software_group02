(function () {
    function appendMessage(root, role, content, meta) {
        if (!root) return null;
        const node = document.createElement("div");
        node.className = `global-ai-message ${role}`;
        node.innerHTML = `
            <div>${window.UIKit.escapeHtml(content || "")}</div>
            ${meta ? `<small>${window.UIKit.escapeHtml(meta)}</small>` : ""}
        `;
        root.appendChild(node);
        root.scrollTop = root.scrollHeight;
        return node;
    }

    function appendStructuredMessage(root, data, meta) {
        if (!root || !data?.answerView || typeof window.UIKit.renderAiStructuredView !== "function") {
            return appendMessage(root, "assistant", data?.answer || "No answer returned.", meta);
        }
        const view = data.answerView;
        const node = document.createElement("div");
        node.className = "global-ai-message assistant global-ai-message--structured";
        node.innerHTML = `
            ${window.UIKit.renderAiStructuredView({
                providerMode: data.modelCalled ? data.model || "model" : "local fallback",
                headline: view.headline,
                priority: view.priority,
                sections: view.sections
            })}
            ${meta ? `<small>${window.UIKit.escapeHtml(meta)}</small>` : ""}
        `;
        root.appendChild(node);
        root.scrollTop = root.scrollHeight;
        return node;
    }

    async function refreshStatus(statusEl) {
        if (!statusEl) return;
        const result = await window.ApiClient.aiStatus();
        if (!result.success) {
            statusEl.textContent = "Model status unavailable.";
            return;
        }
        const provider = result.data.provider || {};
        const mode = provider.mode || "unknown";
        const model = provider.model || "not configured";
        statusEl.textContent = `${mode} · ${model}`;
        statusEl.classList.toggle("is-ready", !!provider.providerReady);
    }

    function initGlobalAssistant() {
        const panel = document.getElementById("global-ai-assistant");
        const backdrop = document.querySelector(".global-ai-backdrop");
        const form = document.getElementById("global-ai-form");
        const messages = document.getElementById("global-ai-messages");
        const statusEl = document.getElementById("global-ai-status");
        if (!panel || !backdrop || !form || !messages) return;

        const open = () => {
            panel.classList.remove("hidden");
            backdrop.classList.remove("hidden");
            refreshStatus(statusEl);
            form.message.focus();
        };

        const close = () => {
            panel.classList.add("hidden");
            backdrop.classList.add("hidden");
        };

        document.querySelectorAll('[data-action="ai-open"]').forEach((btn) => {
            btn.addEventListener("click", open);
        });
        document.querySelectorAll('[data-action="ai-close"]').forEach((btn) => {
            btn.addEventListener("click", close);
        });

        form.addEventListener("submit", async (event) => {
            event.preventDefault();
            const message = form.message.value.trim();
            if (!message) return;
            appendMessage(messages, "user", message);
            form.reset();

            const submitBtn = form.querySelector("button");
            submitBtn.disabled = true;
            const loading = appendMessage(messages, "assistant is-loading", "GLM is analyzing the workspace...");
            const result = await window.ApiClient.aiChat({
                message,
                page: document.body.dataset.page || window.location.pathname
            });
            submitBtn.disabled = false;
            loading?.remove();

            if (!result.success) {
                appendMessage(messages, "assistant error", result.error?.message || "AI request failed.");
                return;
            }

            const data = result.data || {};
            appendStructuredMessage(messages, data, data.modelCalled ? `Model: ${data.model || "configured model"}` : "Local fallback");
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initGlobalAssistant);
    } else {
        initGlobalAssistant();
    }
})();
