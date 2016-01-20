<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ page import="javax.naming.InitialContext,
    edvisees.edl2015.lucene.*,
    edvisees.edl2015.disambiguate.BabelfyDisamb,
    edvisees.edl2015.weights.SemanticSignatureMap,
    edvisees.edl2015.candidates.Fragment,
    edvisees.edl2015.candidates.CandidateExtractorFactory,
    edvisees.edl2015.candidates.CandidateExtractor,
    org.apache.commons.lang3.StringEscapeUtils,
    java.util.*"
%><%!
// get the id without the m.
String getFreebaseIdNoM(String nodeFbId) {
    return nodeFbId.substring(nodeFbId.indexOf('.') + 1);
}

String getFreebaseUrl(String fbIdNoM) {
    return "http://www.freebase.com/m/" + fbIdNoM;
}
%><%
response.setContentType("text/html");
InitialContext ctx = new InitialContext();
Searcher idSearcher = (Searcher) ctx.lookup("java:comp/env/idSearcher");
Searcher nameSearcher = (Searcher) ctx.lookup("java:comp/env/nameSearcher");
Searcher badIdsSearcher = (Searcher) ctx.lookup("java:comp/env/badIdsSearcher");
SemanticSignatureMap semSigMap = (SemanticSignatureMap) ctx.lookup("java:comp/env/semSigMap");

String inputText = request.getParameter("inputText");
inputText = inputText == null ? "" : inputText;

String startOffsetStr = request.getParameter("startOffsetStr");
startOffsetStr = startOffsetStr == null ? "" : startOffsetStr.trim();
int startOffset = 0;
if (startOffsetStr.length() > 0) {
    startOffset = Integer.parseInt(startOffsetStr); // TODO: handle exceptions
}

String ambiguityLevelStr = request.getParameter("ambiguityLevel");
ambiguityLevelStr = ambiguityLevelStr == null ? "" : ambiguityLevelStr.trim();
int ambiguityLevel = 10;
if (ambiguityLevelStr.length() > 0) {
    ambiguityLevel = Integer.parseInt(ambiguityLevelStr); // TODO: handle exceptions
}

String scoreThresholdStr = request.getParameter("scoreThreshold");
scoreThresholdStr = scoreThresholdStr == null ? "" : scoreThresholdStr.trim();
float scoreThreshold = 0.1f;
if (scoreThresholdStr.length() > 0) {
    scoreThreshold = Float.parseFloat(scoreThresholdStr); // TODO: handle exceptions
}

String language = request.getParameter("language");
language = (language == null || language.length() == 0) ? "en" : language;

String maxResultsStr = request.getParameter("maxResults");
int maxResults = (maxResultsStr == null || maxResultsStr.length() == 0) ? 100 : Integer.parseInt(maxResultsStr);

CandidateExtractor candidateExtractor = CandidateExtractorFactory.getCandidateExtractor(language, nameSearcher, badIdsSearcher);

//TODO horrible, hardcoded!
String runtimeDir = (String) ctx.lookup("java:comp/env/runtimeDir");
BabelfyDisamb disambiguator = new BabelfyDisamb(semSigMap, candidateExtractor, runtimeDir + "/fb_ner_type.txt", idSearcher);

%>
<!DOCTYPE html>
<html>
<t:head title="Disambiguation Tool"></t:head>
<body>
<t:menu></t:menu>
<h1>Disambiguation Tool</h1>
<form method="get">
    Enter sentence to extract candidate fragments from:
    <br>
    <textarea name="inputText" rows="5" cols="50"><%=inputText%></textarea>
    <br>
    Language: <select name="language">
        <option value="en" <%=language.equals("en") ? "selected" : "" %>>english</option>
        <option value="es" <%=language.equals("es") ? "selected" : "" %>>spanish</option>
        <option value="zh" <%=language.equals("zh") ? "selected" : "" %>>chinese</option>
    </select>
    <br>
    Max Results (lucene): <input type="text" name="maxResults" size="10" value="<%=maxResults%>" />
    <br>
    Start Offset: <input type="text" name="startOffset" size="10" value="<%=startOffset%>" />
    <br>
    Ambiguity Level: <input type="text" name="ambiguityLevel" size="10" value="<%=ambiguityLevel%>" />
    <br>
    Score Threshold: <input type="text" name="scoreThreshold" size="10" value="<%=scoreThreshold%>" />
    <br>
    <input type="submit" />
</form>
<br>
<%
if (inputText != "") {
    // perform the disambiguation
    Fragment inputFragment = new Fragment(inputText, startOffset);
    Map<Fragment, String> disambiguatedFragments = disambiguator.disamb(inputFragment, maxResults, ambiguityLevel, scoreThreshold);
    %>
    <table border="1">
        <thead>
            <tr>
                <th colspan="4">Found: <%=disambiguatedFragments.keySet().size()%> fragments</th>
            </tr>
            <tr>
                <th>Fragment</th>
                <th>Node Index</th>
                <th>Freebase Id</th>
                <th>Score</th>
            </tr>
        </thead>
        <tbody><%
        for (Fragment fragment : disambiguatedFragments.keySet()) {
            String nodeIndex = disambiguatedFragments.get(fragment);
            String fbId = "NIL";
            String navigateLink = "";
            String fbLink = "";
            String fbImage = "";
            if (! nodeIndex.equals("NIL")) {
                navigateLink = "<a href='/navigate.jsp?nodeId=" + nodeIndex + "' target='_blank'>" + nodeIndex + "</a>";
                fbId = idSearcher.findByIdUnique(nodeIndex).getTitle();
                // go to the freebase url and get an image for the entity
                String fbIdNoM = getFreebaseIdNoM(fbId); // m.1234 => 1234
                String fbIdUrl = getFreebaseUrl(fbIdNoM); // freebase.com/m/1234
                fbImage = "<img src='https://www.googleapis.com/freebase/v1/image/m/" + fbIdNoM + "?&amp;" +
                    "maxwidth=125&amp;maxheight=125&amp;mode=fillcropmid&amp;errorid=%2Ffreebase%2Fno_image_png' " +
                    "width='125' height='125' alt='" + fbId + "'>";
                fbLink = "<a href='" + fbIdUrl + "' target='_blank'>" + fbImage + "</a>";
            } %>
            <tr>
                <td>
                    <%=fragment.text%>
                    (<%=fragment.startOffset%> - <%=fragment.endOffset%>)
                </td>
                <td>
                    <%=navigateLink%>
                </td>
                <td>
                    <%=fbImage%>
                </td>
                <td>
                    <%=fragment.finalScore%>
                </td>
            </tr><%
        } %>
        </tbody>
    </table><%
}  %>
</body>
</html>
