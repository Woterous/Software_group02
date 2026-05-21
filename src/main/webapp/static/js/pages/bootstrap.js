/**
 * 页面启动器 —— 所有页面的统一入口。
 * <p>
 * 信息流：DOMContentLoaded → 此处 → UIKit.bindGlobalActions() + 读body data-role/data-page → 调对应PageModules.xxx.init()
 * <p>
 * 执行顺序：①绑定全局UI组件 ②读取body上的data-role和data-page属性 ③调用对应页面的初始化函数。
 */
window.PageModules = window.PageModules || {};

(function () {
    function deriveFromPath() {
        const path = window.location.pathname || "";
        if (path.includes("/pages/login")) return { role: "public", page: "login" };
        if (path.includes("/pages/register")) return { role: "public", page: "register" };
        if (path.includes("/pages/ta/job-detail")) return { role: "ta", page: "job-detail" };
        if (path.includes("/pages/ta/")) return { role: "ta", page: path.split("/").pop() };
        if (path.includes("/pages/mo/")) return { role: "mo", page: path.split("/").pop() };
        if (path.includes("/pages/admin/")) return { role: "admin", page: path.split("/").pop() };
        return { role: "public", page: "login" };
    }

    function initPage(role, page) {
        const roleModules = window.PageModules[role] || {};
        const initializer = roleModules[page];
        if (typeof initializer === "function") {
            initializer();
        }
    }

    function boot() {
        window.UIKit.bindGlobalActions();
        let role = document.body.dataset.role || "";
        let page = document.body.dataset.page || "";
        if (!window.PageModules[role] || !window.PageModules[role][page]) {
            const derived = deriveFromPath();
            role = derived.role;
            page = derived.page;
        }
        initPage(role, page);
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", boot);
    } else {
        boot();
    }
})();
