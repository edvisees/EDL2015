<%@page import="java.nio.file.Paths"%>
<%@page import="java.nio.file.Files"%>
<%@page import="edvisees.edl2015.candidates.CandidateExtractorFactory"%>
<%@page import="edvisees.edl2015.candidates.CandidateExtractor"%>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ page import="javax.naming.InitialContext,
    org.apache.commons.lang3.StringEscapeUtils,
    java.io.*,
    java.util.*"
%><%
response.setContentType("text/html");
InitialContext ctx = new InitialContext();
String runtimeDir = (String) ctx.lookup("java:comp/env/runtimeDir");

String action = request.getParameter("action");
action = action == null ? "" : action;

String language = request.getParameter("language");
language = (language == null || language.length() == 0) ? "eng" : language; // eng spa cmn

String source = request.getParameter("source");
source = (source == null || source.length() == 0) ? "newswire" : source;

String maxResultsStr = request.getParameter("maxResults");
int maxResults = (maxResultsStr == null || maxResultsStr.length() == 0) ? 100 : Integer.parseInt(maxResultsStr);

String scoreThresholdStr = request.getParameter("scoreThreshold");
scoreThresholdStr = scoreThresholdStr == null ? "" : scoreThresholdStr.trim();
float scoreThreshold = 0.5f;
if (scoreThresholdStr.length() > 0) {
    scoreThreshold = Float.parseFloat(scoreThresholdStr); // TODO: handle exceptions
}

%>
<!DOCTYPE html>
<html>
<t:head title="EDL 2015 - evaluation">
<style>
    #training_files {
        display: inline-block;
        /*border: 1px solid #d3d3d3;*/
        padding-left: 10px;
        padding-right: 30px;
        font-size: 80%;
        max-height: 300px;
        overflow-y: scroll;
    }
</style>
</t:head>
<body>
<t:menu></t:menu>
<h1>evaluate edl 2015</h1>
<form method="get">
    Language: <select name="language">
        <option value="eng" <%=language.equals("eng") ? "selected" : "" %>>english</option>
        <option value="spa" <%=language.equals("spa") ? "selected" : "" %>>spanish</option>
        <option value="cmn" <%=language.equals("cmn") ? "selected" : "" %>>chinese</option>
    </select>
    <br>
    Source: <select name="source">
        <option value="discussion_forum" <%=source.equals("discussion") ? "selected" : "" %>>discussion forum</option>
        <option value="newswire" <%=source.equals("newswire") ? "selected" : "" %>>news wire</option>
    </select>
    <br>
    Max Results (lucene): <input type="text" name="maxResults" size="10" value="<%=maxResults%>" />
    <br>
    Score Threshold: <input type="text" name="scoreThreshold" size="10" value="<%=scoreThreshold%>" />
    <br>
    <br>
    <input type="submit" value="load training corpus" name="action" />
    <br><br><%
    if (action.length() > 0) { %>
        <b>Training Corpus: </b><%
        String trainingCorpusDir = runtimeDir + "/../training_data_2.0/data/";
        String trainingFilesDirPath = trainingCorpusDir + "source_docs/" + language + "_xml/" + source + "/";
        File trainingFilesDir = new File(trainingFilesDirPath);
        File[] trainingFiles = trainingFilesDir.listFiles(); %>
        <br>
        corpus dir: "<%=trainingFilesDir.getAbsolutePath()%>"<br>
        <div id="training_files"><%
        for (File trainingFile : trainingFiles) {
            if (trainingFile.isFile()) {
                String trainingFileName = trainingFile.getName(); %>
                <a href="evaluate.jsp?language=<%=language%>&source=<%=source%>&scoreThreshold=<%=scoreThreshold%>&fileName=<%=trainingFileName%>" target="_blank">
                    <%=trainingFileName%>
               </a>
               <br><%
            }
        }
    } %>
    </div>
</form>
</body>
</html>
