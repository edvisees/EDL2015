<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ page import="javax.naming.InitialContext,
    edvisees.edl2015.candidates.*,
    edvisees.edl2015.lucene.*,
    org.apache.commons.lang3.StringEscapeUtils,
    java.util.*"
%><%!
String getNames(List<IndexItem> names) {
    if (names.size() == 0) return null;
    String namesStr = "";
    for (IndexItem item : names) {
        namesStr += item.getTitle() + " // ";
    }
    return StringEscapeUtils.escapeHtml4(namesStr);
}
%><%
response.setContentType("text/html");
InitialContext ctx = new InitialContext();
Searcher idSearcher = (Searcher) ctx.lookup("java:comp/env/idSearcher");
Searcher nameSearcher = (Searcher) ctx.lookup("java:comp/env/nameSearcher");
Searcher badIdsSearcher = (Searcher) ctx.lookup("java:comp/env/badIdsSearcher");

String sentence = request.getParameter("sentence");
sentence = sentence == null ? "" : sentence;

String textAreaContent = sentence.length() > 0 ? sentence : "Thomas and Mario are strikers playing in Munich.";

String language = request.getParameter("language");
language = (language == null || language.length() == 0) ? "en" : language;

String maxResultsStr = request.getParameter("maxResults");
int maxResults = (maxResultsStr == null || maxResultsStr.length() == 0) ? 100 : Integer.parseInt(maxResultsStr);

CandidateExtractor candidateExtractor = CandidateExtractorFactory.getCandidateExtractor(language, nameSearcher, badIdsSearcher); //new CandidateExtractor(nameSearcher, badIdsSearcher);

%>
<!DOCTYPE html>
<html>
<t:head title="EDL 2015 - extract candidate fragments">
    <script>
    $(function() {
        $(".header").click(function () {
            $header = $(this);
            $content = $header.next();
            $content.slideToggle(500, function () { /*$header.text(function () { return $content.is(":visible") ? "Collapse" : "Expand"; } );*/ });
        });
    });
    </script>
    <style>
        .container {
            /* width:100%; */
            border: 1px solid #d3d3d3;
            overflow: hidden;
        }
        .container div {
            /* width: 100%; */
        }
        .container .header {
            /* background-color:#d3d3d3; */
            padding: 2px;
            cursor: pointer;
            font-weight: bold;
        }
        .container .content {
            display: none;
            padding : 0px;
            font-size:80%;
            max-height: 200px;
            overflow-y: scroll;
        }
        .container p {
            margin: 0px;
        }
    </style>
</t:head>
<body>
<t:menu></t:menu>
<h1>extract candidate fragments</h1>
<form method="get">
    Enter sentence to extract candidate fragments from:
    <br>
    <textarea name="sentence" rows="5" cols="50"><%=textAreaContent%></textarea>
    <br>
    Language: <select name="language">
        <option value="en" <%=language.equals("en") ? "selected" : "" %>>english</option>
        <option value="es" <%=language.equals("es") ? "selected" : "" %>>spanish</option>
        <option value="zh" <%=language.equals("zh") ? "selected" : "" %>>chinese</option>
    </select>
    <br>
    Max Results (lucene): <input type="text" name="maxResults" size="10" value="<%=maxResults%>" />
    <br>
    <input type="submit" />
</form>
<br>
<%
if (sentence != "") {
    List<Fragment> fragments = candidateExtractor.findCandidateMeanings(new Fragment(sentence, 0), maxResults);
    Collections.sort(fragments); %>
    Number of fragments: <%=fragments.size()%>
    <div class="container"><%
        int i = 0;
        for (Iterator<Fragment> fragmentsIter = fragments.iterator(); fragmentsIter.hasNext(); ++i) {
            Fragment fragment = fragmentsIter.next(); %>
            <div class="header">#<%=i%> <%=fragment%> (<%=fragment.meanings.size()%> candidates)</div>
            <div class="content"><%
                for (CandidateMeaning fragmentCandidate : fragment.meanings) {
                    List<IndexItem> nodeNames = nameSearcher.findById(fragmentCandidate.entityId + "", 20);
                    String namesString = getNames(nodeNames);
                    namesString = namesString == null ? "(no names)" : namesString; %>
                    <p>
                        <a href="navigate.jsp?nodeId=<%=fragmentCandidate.entityId%>" target="_blank"><%=fragmentCandidate.entityId%></a>
                        <%=namesString%>
                    </p><%
                } %>
            </div> <%
        } %>
    </div><%
} %>
</body>
</html>
