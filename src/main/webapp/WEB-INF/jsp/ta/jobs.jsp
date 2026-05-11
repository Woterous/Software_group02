<%
    request.setAttribute("pageTitle", "TA Job Board");
%>
<%@ include file="/WEB-INF/jsp/common/head.jspf" %>
<%@ include file="/WEB-INF/jsp/common/topbar.jspf" %>

<div class="workspace">
    <%@ include file="/WEB-INF/jsp/common/sidebar.jspf" %>
    <main class="content-area" id="ta-jobs-page">
        <section class="page-intro glass-panel reveal-up">
            <div>
                <span class="eyebrow eyebrow-subtle">Role discovery</span>
                <h2>Available TA Positions</h2>
                <p class="muted">Filter by module, compare deadlines, and prioritise roles where your subject fit is strongest.</p>
            </div>
            <div class="page-intro-note">
                <span class="section-kicker">Selection tip</span>
                <p>Use deadline cues first, then review module relevance and skill alignment before applying.</p>
            </div>
        </section>

        <section class="panel ai-assistant-panel glass-panel reveal-up">
            <div class="panel-head panel-head--rich">
                <div>
                    <span class="section-kicker">AI match assistant</span>
                    <h3>Find roles that fit your profile</h3>
                </div>
                <button class="primary-btn" id="ta-ai-match-btn" type="button">Generate Matches</button>
            </div>
            <p class="panel-description">Uses your profile skills, current openings, deadlines, and previous applications to rank suitable roles.</p>
            <div id="ta-ai-recommendations" class="ai-result-stack"></div>
        </section>

        <section class="panel jobs-filter-panel glass-panel reveal-up">
            <div class="panel-head panel-head--rich">
                <div>
                    <span class="section-kicker">Search and refine</span>
                    <h3>Role filters</h3>
                </div>
                <span class="muted">Results update after each filter action</span>
            </div>
            <form id="ta-job-filter-form" class="filter-shell">
                <div class="filter-shell-grid">
                    <label class="filter-field">Keyword
                        <input type="text" name="keyword" placeholder="Search by title or module" />
                    </label>
                    <label class="filter-field">Module
                        <select name="module">
                            <option value="">All Modules</option>
                        </select>
                    </label>
                    <label class="filter-field">Status
                        <select name="status">
                            <option value="">Any Status</option>
                            <option value="open">Open</option>
                            <option value="closing">Closing Soon</option>
                        </select>
                    </label>
                </div>
                <div class="filter-shell-actions">
                    <button class="ghost-btn" type="submit">Apply Filters</button>
                    <p class="filter-helper">Focus on deadline urgency first, then compare module and skill fit.</p>
                </div>
            </form>
        </section>

        <section class="panel jobs-results-panel glass-panel">
            <div class="panel-head panel-head--rich">
                <div>
                    <span class="section-kicker">Results</span>
                    <h3>Open roles</h3>
                </div>
                <span class="muted">Each card highlights title, deadline, module, and required skills.</span>
            </div>
            <div id="ta-job-list" class="card-list"></div>
            <div id="ta-jobs-pagination" class="pagination"></div>
        </section>
    </main>
</div>

<%@ include file="/WEB-INF/jsp/common/toast.jspf" %>
<%@ include file="/WEB-INF/jsp/common/modal.jspf" %>
<%@ include file="/WEB-INF/jsp/common/scripts.jspf" %>
