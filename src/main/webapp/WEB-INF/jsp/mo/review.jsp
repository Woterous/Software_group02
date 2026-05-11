<%
    request.setAttribute("pageTitle", "MO Review Center");
%>
<%@ include file="/WEB-INF/jsp/common/head.jspf" %>
<%@ include file="/WEB-INF/jsp/common/topbar.jspf" %>

<div class="workspace">
    <%@ include file="/WEB-INF/jsp/common/sidebar.jspf" %>
    <main class="content-area" id="mo-review-page">
        <section class="split-grid reveal-up">
            <article class="panel glass-panel">
                <div class="panel-head"><h2>Candidate Review</h2></div>
                <div id="mo-review-candidate" class="stack-block"></div>
                <label>Review Note
                    <textarea id="mo-review-note" rows="5" placeholder="Add review notes"></textarea>
                </label>
                <div class="button-row">
                    <button class="primary-btn" id="mo-select-btn" type="button">Select Candidate</button>
                    <button class="danger-btn" id="mo-reject-btn" type="button">Reject Candidate</button>
                </div>
            </article>
            <article class="panel glass-panel">
                <div class="panel-head panel-head--rich">
                    <div>
                        <span class="section-kicker">AI review assistant</span>
                        <h3>Candidate Summary</h3>
                    </div>
                    <button class="ghost-btn" id="mo-ai-summary-btn" type="button">Generate Summary</button>
                </div>
                <div id="mo-ai-summary" class="ai-result-stack"></div>
                <div class="panel-divider"></div>
                <div class="panel-head"><h3>Skill Match Matrix</h3></div>
                <div id="mo-skill-match" class="list-stack"></div>
            </article>
        </section>
    </main>
</div>

<%@ include file="/WEB-INF/jsp/common/toast.jspf" %>
<%@ include file="/WEB-INF/jsp/common/modal.jspf" %>
<%@ include file="/WEB-INF/jsp/common/scripts.jspf" %>
