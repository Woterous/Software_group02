<%
    request.setAttribute("pageTitle", "Register - TA Recruitment System");
%>
<%@ include file="/WEB-INF/jsp/common/head.jspf" %>
<%@ include file="/WEB-INF/jsp/common/topbar.jspf" %>

<main class="public-shell login-shell">
    <section class="login-split glass-panel reveal-up">
        <div class="login-visual-panel" id="login-visual-panel">
            <div class="login-visual-brand">
                <div class="login-visual-logo">TA</div>
                <span>TA Recruitment System</span>
            </div>

            <div class="login-character-scene" id="login-character-scene">
                <div class="character character-purple" data-char="purple">
                    <div class="char-eyes">
                        <span class="char-eye"><span class="char-pupil"></span></span>
                        <span class="char-eye"><span class="char-pupil"></span></span>
                    </div>
                </div>
                <div class="character character-black" data-char="black">
                    <div class="char-eyes">
                        <span class="char-eye"><span class="char-pupil"></span></span>
                        <span class="char-eye"><span class="char-pupil"></span></span>
                    </div>
                </div>
                <div class="character character-orange" data-char="orange">
                    <div class="char-eyes dots">
                        <span class="char-dot"><span class="char-pupil dot"></span></span>
                        <span class="char-dot"><span class="char-pupil dot"></span></span>
                    </div>
                </div>
                <div class="character character-yellow" data-char="yellow">
                    <div class="char-eyes dots">
                        <span class="char-dot"><span class="char-pupil dot"></span></span>
                        <span class="char-dot"><span class="char-pupil dot"></span></span>
                    </div>
                    <span class="char-mouth" aria-hidden="true"></span>
                </div>
            </div>
            
            <div class="login-visual-footer">
                <span>Group02</span>
            </div>
        </div>

        <div class="login-form-panel">
            <section class="auth-card login-auth-card register-auth-card">
                <div class="eyebrow">Get Started</div>
                <h2>Create your account</h2>
                <p class="muted">Create your account to access role workflows.</p>

                <form id="register-form" class="form-grid-two">
                    <label>Full Name
                        <input type="text" name="name" placeholder="Alex Morgan" required />
                    </label>
                    <label>Email
                        <input type="email" name="email" placeholder="alex@school.edu" required />
                    </label>
                    <label>Password
                        <div class="password-input-wrap">
                            <input
                                id="register-password"
                                type="password"
                                name="password"
                                placeholder="At least 8 chars, e.g. Password123!"
                                autocomplete="new-password"
                                required
                            />
                            <button type="button" class="password-toggle" data-action="toggle-password" aria-label="Show password" aria-pressed="false">
                                <svg class="icon-eye" viewBox="0 0 24 24" aria-hidden="true">
                                    <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7"></path>
                                    <circle cx="12" cy="12" r="3"></circle>
                                </svg>
                                <svg class="icon-eye-off" viewBox="0 0 24 24" aria-hidden="true">
                                    <path d="m3 3 18 18"></path>
                                    <path d="M10.6 10.6A2 2 0 0 0 12 14a2 2 0 0 0 1.4-.6"></path>
                                    <path d="M6.7 6.7C4.3 8.3 2.9 10.7 2 12c0 0 3.5 7 10 7 2.1 0 3.9-.7 5.4-1.7"></path>
                                    <path d="M21 12s-3.5-7-10-7c-.8 0-1.6.1-2.3.3"></path>
                                </svg>
                            </button>
                        </div>
                    </label>
                    <label class="full-row">Role
                        <div class="role-segmented" role="radiogroup" aria-label="Role">
                            <span class="role-indicator" aria-hidden="true"></span>

                            <input id="register-role-ta" type="radio" name="role" value="ta" checked />
                            <label class="role-option" for="register-role-ta">
                                <svg viewBox="0 0 24 24" aria-hidden="true">
                                    <path d="m3 9 9-5 9 5-9 5-9-5"></path>
                                    <path d="M7 11v4.5c0 1.6 2.2 3 5 3s5-1.4 5-3V11"></path>
                                </svg>
                                <span>TA</span>
                            </label>

                            <input id="register-role-mo" type="radio" name="role" value="mo" />
                            <label class="role-option" for="register-role-mo">
                                <svg viewBox="0 0 24 24" aria-hidden="true">
                                    <path d="M9 3h6l1 2h3v16H5V5h3l1-2Z"></path>
                                    <path d="M9 11h6"></path>
                                    <path d="M9 15h4"></path>
                                </svg>
                                <span>MO</span>
                            </label>

                            <input id="register-role-admin" type="radio" name="role" value="admin" />
                            <label class="role-option" for="register-role-admin">
                                <svg viewBox="0 0 24 24" aria-hidden="true">
                                    <path d="m12 3 7 3v6c0 4.2-2.8 7.3-7 9-4.2-1.7-7-4.8-7-9V6l7-3Z"></path>
                                    <path d="m9 12 2 2 4-4"></path>
                                </svg>
                                <span>AD</span>
                            </label>
                        </div>
                    </label>
                    <label class="full-row">Skills
                        <input type="text" name="skills" placeholder="Java, SQL, Communication" />
                    </label>
                    <label id="register-cv-field" class="full-row register-cv-field">CV (optional)
                        <input id="register-cv-file-input" class="cv-upload-input" type="file" name="cvFile" accept=".pdf,.doc,.docx" />
                        <div id="register-cv-dropzone" class="cv-dropzone" tabindex="0" role="button" aria-label="Drop CV file here or choose file">
                            <div class="cv-dropzone-inner">
                                <div class="cv-dropzone-title">Drop your CV here</div>
                                <div class="cv-dropzone-hint">PDF, DOC, or DOCX up to 5MB</div>
                                <button id="register-cv-pick-btn" class="glass-secondary-btn inline cv-pick-btn" type="button">Choose File</button>
                                <div id="register-cv-selected-file" class="cv-dropzone-file">No file selected</div>
                            </div>
                        </div>
                    </label>
                    <button class="primary-btn full-row" type="submit">Create Account</button>
                </form>

                <div class="auth-links">
                    <span>Already registered?</span>
                    <a href="${pageContext.request.contextPath}/pages/login">Back to login</a>
                </div>
            </section>
        </div>
    </section>
</main>

<%@ include file="/WEB-INF/jsp/common/toast.jspf" %>
<%@ include file="/WEB-INF/jsp/common/modal.jspf" %>
<%@ include file="/WEB-INF/jsp/common/scripts.jspf" %>
