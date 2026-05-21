/**
 * 登录和注册页面的交互逻辑。
 * <p>
 * 信息流位置：用户填表单 → 此处收集数据 → ApiClient → HTTP → Servlet → ... → 返回JSON → 此处处理结果
 * <p>
 * 注册信息流：initRegister() 绑定表单submit → formToObject()收集数据 → ApiClient.authRegister()发请求 → toast提示结果 → 跳转登录页
 * 登录信息流：initLogin() 绑定表单submit → ApiClient.authLogin() → session存储 → 跳转角色仪表盘
 */
window.PageModules = window.PageModules || {};
window.PageModules.public = window.PageModules.public || {};

(function () {
    function initLoginCharacters({ emailInput, passwordInput }) {
        const visual = document.getElementById("login-visual-panel");
        const scene = document.getElementById("login-character-scene");
        if (!visual || !scene) {
            return {
                sync() {}
            };
        }

        const purple = scene.querySelector('[data-char="purple"]');
        const black = scene.querySelector('[data-char="black"]');
        const orange = scene.querySelector('[data-char="orange"]');
        const yellow = scene.querySelector('[data-char="yellow"]');
        const characters = [
            { node: purple, skew: -10, eyeX: 6.4, eyeY: 4.6 },
            { node: black, skew: -8.5, eyeX: 5.8, eyeY: 4.2 },
            { node: orange, skew: -5.5, eyeX: 4.6, eyeY: 3.8 },
            { node: yellow, skew: -5.5, eyeX: 4.4, eyeY: 3.6 }
        ];

        const state = {
            x: window.innerWidth * 0.5,
            y: window.innerHeight * 0.5,
            typing: false,
            reveal: false,
            peek: false
        };
        let rafId = 0;
        let blinkTimer = null;
        let awkwardTimer = null;

        const clamp = (value, min, max) => Math.max(min, Math.min(max, value));

        const updateClasses = () => {
            scene.classList.toggle("is-typing", state.typing);
            scene.classList.toggle("is-reveal", state.reveal);
            scene.classList.toggle("is-hide-peek", state.peek);
        };

        const applyTransforms = () => {
            const rect = scene.getBoundingClientRect();
            if (!rect.width || !rect.height) return;

            const nx = clamp((state.x - rect.left) / rect.width - 0.5, -0.72, 0.72);
            const ny = clamp((state.y - rect.top) / rect.height - 0.5, -0.72, 0.72);
            characters.forEach((cfg) => {
                if (!cfg.node) return;
                const skew = state.reveal ? 0 : nx * cfg.skew;
                const eyeDx = state.reveal ? -6 : nx * (cfg.eyeX + 10);
                const eyeDy = state.reveal ? -4.5 : ny * (cfg.eyeY + 10);
                cfg.node.style.setProperty("--tx", "0px");
                cfg.node.style.setProperty("--ty", "0px");
                cfg.node.style.setProperty("--skew", `${skew.toFixed(2)}deg`);
                cfg.node.style.setProperty("--eyes-tx", `${eyeDx.toFixed(2)}px`);
                cfg.node.style.setProperty("--eyes-ty", `${eyeDy.toFixed(2)}px`);

                const mouth = cfg.node.querySelector(".char-mouth");
                if (mouth) {
                    const mouthDx = state.reveal ? -4 : eyeDx * 0.95;
                    const mouthDy = state.reveal ? -3 : eyeDy * 0.85;
                    mouth.style.setProperty("--mouth-tx", `${mouthDx.toFixed(2)}px`);
                    mouth.style.setProperty("--mouth-ty", `${mouthDy.toFixed(2)}px`);
                }
            });

            scene.querySelectorAll(".char-pupil").forEach((pupil) => {
                if (pupil.classList.contains("dot")) {
                    pupil.style.transform = "translate(-50%, -50%)";
                    return;
                }
                const holder = pupil.parentElement;
                if (!holder) return;
                const eyeRect = holder.getBoundingClientRect();
                const centerX = eyeRect.left + eyeRect.width * 0.5;
                const centerY = eyeRect.top + eyeRect.height * 0.5;
                const deltaX = state.reveal ? -4 : state.x - centerX;
                const deltaY = state.reveal ? -4 : state.y - centerY;
                const holderSize = Math.min(eyeRect.width, eyeRect.height);
                const pupilSize = Math.max(1, pupil.offsetWidth || 0);
                const safeDistance = Math.max(0, (holderSize - pupilSize) * 0.5);
                const maxDistance = Math.max(0.8, safeDistance - 1.15);
                const distance = Math.min(Math.sqrt(deltaX * deltaX + deltaY * deltaY), maxDistance);
                const angle = Math.atan2(deltaY, deltaX);
                const px = Math.cos(angle) * distance;
                const py = Math.sin(angle) * distance;
                pupil.style.transform = `translate(calc(-50% + ${px.toFixed(2)}px), calc(-50% + ${py.toFixed(2)}px))`;
            });
        };

        const requestUpdate = () => {
            if (rafId) return;
            rafId = window.requestAnimationFrame(() => {
                rafId = 0;
                applyTransforms();
            });
        };

        const syncState = () => {
            const active = document.activeElement;
            const hasPassword = !!(passwordInput && passwordInput.value);
            state.typing = active === emailInput || active === passwordInput;
            state.reveal = passwordInput && passwordInput.type === "text" && hasPassword;
            state.peek = passwordInput && passwordInput.type === "password" && hasPassword;
            updateClasses();
            requestUpdate();
        };

        const onPointerMove = (event) => {
            state.x = event.clientX;
            state.y = event.clientY;
            requestUpdate();
        };

        const triggerBlink = () => {
            if (Math.random() > 0.5 && purple) {
                purple.classList.add("is-blinking");
                setTimeout(() => purple.classList.remove("is-blinking"), 140);
            } else if (black) {
                black.classList.add("is-blinking");
                setTimeout(() => black.classList.remove("is-blinking"), 140);
            }
            blinkTimer = setTimeout(triggerBlink, 2800 + Math.random() * 3800);
        };

        const replaySceneClass = (className, durationMs) => {
            scene.classList.remove(className);
            // Force reflow to make repeated triggers restart animation reliably.
            scene.offsetWidth;
            scene.classList.add(className);
            return window.setTimeout(() => {
                scene.classList.remove(className);
            }, durationMs);
        };

        visual.addEventListener("mousemove", onPointerMove);
        visual.addEventListener("mouseenter", onPointerMove);
        window.addEventListener("resize", requestUpdate);
        emailInput?.addEventListener("focus", syncState);
        emailInput?.addEventListener("blur", () => setTimeout(syncState, 0));
        passwordInput?.addEventListener("focus", syncState);
        passwordInput?.addEventListener("blur", () => setTimeout(syncState, 0));
        passwordInput?.addEventListener("input", syncState);
        passwordInput?.addEventListener("change", syncState);

        syncState();
        requestUpdate();
        blinkTimer = setTimeout(triggerBlink, 1000 + Math.random() * 2000);

        return {
            sync: syncState,
            playAwkwardFail() {
                if (awkwardTimer) window.clearTimeout(awkwardTimer);
                awkwardTimer = replaySceneClass("is-fail-awkward", 1100);
            },
            destroy() {
                visual.removeEventListener("mousemove", onPointerMove);
                visual.removeEventListener("mouseenter", onPointerMove);
                window.removeEventListener("resize", requestUpdate);
                if (blinkTimer) clearTimeout(blinkTimer);
                if (awkwardTimer) clearTimeout(awkwardTimer);
            }
        };
    }

    function initSegmentedRoles(scope) {
        const roots = Array.from((scope || document).querySelectorAll(".role-segmented"));
        roots.forEach((root) => {
            const indicator = root.querySelector(".role-indicator");
            const radios = Array.from(root.querySelectorAll('input[type="radio"][name="role"]'));
            if (!indicator || !radios.length) return;

            const optionById = new Map();
            const radioById = new Map();
            radios.forEach((radio) => {
                radioById.set(radio.id, radio);
                const option = root.querySelector(`label[for="${radio.id}"]`);
                if (option) optionById.set(radio.id, option);
            });
            if (!optionById.size) return;

            const moveIndicator = (option) => {
                if (!option) return;
                const optionRect = option.getBoundingClientRect();
                const rootRect = root.getBoundingClientRect();
                indicator.style.width = `${optionRect.width}px`;
                indicator.style.transform = `translate3d(${optionRect.left - rootRect.left}px, 0, 0)`;
            };

            const refresh = () => {
                const active = radios.find((node) => node.checked) || radios[0];
                if (!active) return;
                if (!active.checked) active.checked = true;
                optionById.forEach((node) => node.classList.remove("is-active"));
                const option = optionById.get(active.id);
                if (option) {
                    option.classList.add("is-active");
                    moveIndicator(option);
                }
            };

            let dragging = false;
            const activateByPointer = (event) => {
                if (!dragging) return;
                optionById.forEach((option, id) => {
                    const rect = option.getBoundingClientRect();
                    const hitX = event.clientX >= rect.left && event.clientX <= rect.right;
                    const hitY = event.clientY >= rect.top && event.clientY <= rect.bottom;
                    if (hitX && hitY) {
                        const radio = radioById.get(id);
                        if (radio && !radio.checked) {
                            radio.checked = true;
                            radio.dispatchEvent(new Event("change", { bubbles: true }));
                        }
                    }
                });
            };

            radios.forEach((radio) => {
                radio.addEventListener("change", refresh);
            });

            root.addEventListener("mousemove", (event) => {
                const rect = root.getBoundingClientRect();
                root.style.setProperty("--x", `${event.clientX - rect.left}px`);
                root.style.setProperty("--y", `${event.clientY - rect.top}px`);
                activateByPointer(event);
            });

            root.addEventListener("mousedown", () => {
                dragging = true;
            });
            window.addEventListener("mouseup", () => {
                dragging = false;
            });

            window.addEventListener("resize", () => {
                window.requestAnimationFrame(refresh);
            });

            refresh();
        });
    }

    function currentSession() {
        try {
            return JSON.parse(window.sessionStorage.getItem("tars.session.user") || "null");
        } catch (_) {
            return null;
        }
    }

    function bindPasswordToggle(form, passwordInput, options) {
        const onChange = options && typeof options.onChange === "function" ? options.onChange : null;
        const toggleBtn = form.querySelector('[data-action="toggle-password"]');
        if (!toggleBtn || !passwordInput) return;
        toggleBtn.addEventListener("click", () => {
            const visible = passwordInput.type === "text";
            passwordInput.type = visible ? "password" : "text";
            toggleBtn.classList.toggle("is-visible", !visible);
            toggleBtn.setAttribute("aria-pressed", String(!visible));
            toggleBtn.setAttribute("aria-label", visible ? "Show password" : "Hide password");
            if (onChange) onChange();
        });
    }

    async function initLogin() {
        const existing = currentSession();
        if (existing) {
            window.location.href = window.UIKit.roleHome(existing.role);
            return;
        }

        const form = document.getElementById("login-form");
        if (!form) return;
        initSegmentedRoles(form);

        const emailInput = form.querySelector('input[name="email"]');
        const passwordInput = form.querySelector('input[name="password"]');
        const characters = initLoginCharacters({ emailInput, passwordInput });
        bindPasswordToggle(form, passwordInput, {
            onChange: () => characters.sync()
        });

        form.addEventListener("submit", async (event) => {
            event.preventDefault();
            const payload = window.UIKit.formToObject(form);
            const result = await window.ApiClient.authLogin(payload);
            if (!result.success) {
                characters.playAwkwardFail();
                window.UIKit.toast(result.error.message, "error");
                return;
            }
            window.sessionStorage.setItem("tars.session.user", JSON.stringify(result.data.user));
            window.UIKit.toast("Login successful.", "success");
            const role = result.data.user.role;
            setTimeout(() => {
                window.location.href = window.UIKit.roleHome(role);
            }, 280);
        });
    }

    async function initRegister() {
        const existing = currentSession();
        if (existing) {
            window.location.href = window.UIKit.roleHome(existing.role);
            return;
        }

        const form = document.getElementById("register-form");
        if (!form) return;
        initSegmentedRoles(form);
        const emailInput = form.querySelector('input[name="email"]');
        const passwordInput = form.querySelector('input[name="password"]');
        const characters = initLoginCharacters({ emailInput, passwordInput });
        bindPasswordToggle(form, passwordInput, {
            onChange: () => characters.sync()
        });

        const cvFileInput = document.getElementById("register-cv-file-input");
        const cvDropzone = document.getElementById("register-cv-dropzone");
        const cvPickBtn = document.getElementById("register-cv-pick-btn");
        const cvSelectedFile = document.getElementById("register-cv-selected-file");
        const cvField = document.getElementById("register-cv-field");
        const roleRadios = Array.from(form.querySelectorAll('input[type="radio"][name="role"]'));
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

        const updateCvLabel = () => {
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
            updateCvLabel();
        };

        const validateCvFile = (file) => {
            if (!file) return "Please choose a CV file first.";
            const ext = getCvExtension(file.name);
            if (!allowedCvExt.has(ext)) return "Only PDF, DOC, and DOCX files are allowed.";
            if (file.size > maxCvSize) return "File size must be 5MB or less.";
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
            updateCvLabel();
            return true;
        };

        const selectedRole = () => {
            const checked = roleRadios.find((radio) => radio.checked);
            return checked ? checked.value : "ta";
        };

        const syncCvVisibility = () => {
            const isTa = selectedRole() === "ta";
            if (cvField) {
                cvField.classList.toggle("is-hidden", !isTa);
                cvField.setAttribute("aria-hidden", String(!isTa));
            }
            if (!isTa) {
                clearSelectedCvFile();
            }
        };

        roleRadios.forEach((radio) => {
            radio.addEventListener("change", syncCvVisibility);
        });

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
                if (file) {
                    setSelectedCvFile(file);
                }
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

        updateCvLabel();
        syncCvVisibility();

        form.addEventListener("submit", async (event) => {
            event.preventDefault();
            const payload = window.UIKit.formToObject(form);
            delete payload.cvFile;

            const file = payload.role === "ta" ? (selectedCvFile || cvFileInput?.files?.[0] || null) : null;
            if (file) {
                const error = validateCvFile(file);
                if (error) {
                    window.UIKit.toast(error, "warn");
                    return;
                }
            }

            const result = await window.ApiClient.authRegister(payload, file);
            if (!result.success) {
                window.UIKit.toast(result.error.message, "error");
                return;
            }
            window.UIKit.toast("Account created. Please sign in.", "success");
            form.reset();
            clearSelectedCvFile();
            syncCvVisibility();
            setTimeout(() => {
                window.location.href = `${window.APP_CONTEXT}/pages/login`;
            }, 360);
        });
    }

    window.PageModules.public.login = initLogin;
    window.PageModules.public.register = initRegister;
})();
