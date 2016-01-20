<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ page import="javax.naming.InitialContext,
    java.text.DecimalFormat,
    edvisees.edl2015.candidates.*,
    edvisees.edl2015.lucene.*,
    org.apache.commons.lang3.StringEscapeUtils,
    it.unimi.dsi.webgraph.labelling.ArcLabelledNodeIterator,
    edvisees.edl2015.weights.SemanticSignatureMap,
    java.util.*"
%><%!
String getFreebaseId(String nodeFbId) {
    return "http://www.freebase.com/m/" + nodeFbId.substring(nodeFbId.indexOf('.') + 1);
}

String getNavigateGraphLink(int nodeIndex) {
    return "/navigate.jsp?nodeId=" + nodeIndex;
}

String getNames(List<IndexItem> names) {
    if (names.size() == 0) return "(no names)";
    String namesStr = "";
    for (IndexItem item : names) {
        namesStr += item.getTitle() + " // ";
    }
    return StringEscapeUtils.escapeHtml4(namesStr);
}
%><%
response.setContentType("text/html");
InitialContext ctx = new InitialContext();
SemanticSignatureMap semSigMap = (SemanticSignatureMap) ctx.lookup("java:comp/env/semSigMap");
Searcher idSearcher = (Searcher) ctx.lookup("java:comp/env/idSearcher");
Searcher nameSearcher = (Searcher) ctx.lookup("java:comp/env/nameSearcher");
String nodeIndexStr = request.getParameter("nodeIndex");
nodeIndexStr = nodeIndexStr == null ? "" : nodeIndexStr;
String showAllNodesWithSignature = request.getParameter("showAllNodesWithSignature");
DecimalFormat formatter = new DecimalFormat("#.########");
%>
<!DOCTYPE html>
<html>
<t:head title="EDL 2015 - inspect semantic signatures"></t:head>
<body>
<t:menu></t:menu>
<h1>inspect semantic signatures</h1>
<form method="get">
    Enter node index:
    <br>
    <input type="text" name="nodeIndex" size="50" value="<%=nodeIndexStr%>" />
    <br>
    <input type="submit" />
</form>
<br>
<form method="get">
    <input type="hidden" name="showAllNodesWithSignature" value="true"/>
    <input type="submit" value="show all nodes with signature"/>
    (NOTE: this will take some time and return many megabytes of response)
</form>
<br>
<%
if (nodeIndexStr != "") {
    String fbId = null;
    Integer nodeIndex = null;
    try {
        nodeIndex = Integer.parseInt(nodeIndexStr);
        fbId = idSearcher.findByIdUnique(nodeIndexStr).getTitle();
    } catch (Exception e) {
        // it is a FB_ID -> get the nodeIndex
        try {
            fbId = nodeIndexStr;
            nodeIndex = Integer.parseInt(idSearcher.findByTitleUnique(fbId).getId());
        } catch (Exception e2) { %>
            Invalid id: '<%=nodeIndexStr%>' (<%=e2.getMessage()%>)<br> <%
        }
    }
    if (nodeIndex != null) {
        String fbUrl = getFreebaseId(fbId);
        List<IndexItem> nodeNames = nameSearcher.findById(nodeIndex + "", 100);
        String nodeNamesStr = getNames(nodeNames);
        SortedMap<Integer, Float> signature = semSigMap.getSortedSignature(nodeIndex); %>
        SELECTED NODE: <a href="<%=fbUrl%>" target="_blank"><%=fbId%></a> (index = <%=nodeIndex%>)
        <br>
        names: <%=nodeNamesStr%> <br>
        semantic signature: <%
        for (Map.Entry<Integer, Float> signatureElem : signature.entrySet()) {
            int signatureNodeIndex = signatureElem.getKey();
            Float signatureNodeWeight = signatureElem.getValue();
            List<IndexItem> signatureElementNames = nameSearcher.findById(signatureNodeIndex + "", 100);
            String signatureElementNamesStr = getNames(signatureElementNames);
            String weightStr = signatureNodeWeight == null ? "NULL" : formatter.format(signatureNodeWeight); %>
            <p>
                <a href="<%=getNavigateGraphLink(signatureNodeIndex)%>" target="_blank"><%=signatureNodeIndex%></a>
                (<%=weightStr%>)
                <%=signatureElementNamesStr%>
            </p> <%
        }
    }
} else if (showAllNodesWithSignature != null) {
    ArcLabelledNodeIterator iter = semSigMap.getNodeIterator();
    while (iter.hasNext()) {
        int nodeIndex = iter.next();
        SortedMap<Integer, Float> signature = semSigMap.getSortedSignature(nodeIndex);
        if (signature.isEmpty()) continue; %>
        <b><%=nodeIndex + ": " %></b><%
        for (Map.Entry<Integer, Float> signatureElem : signature.entrySet()) { %>
            <%=signatureElem.getKey() + " (" + formatter.format(signatureElem.getValue()) + "); "%><%
        } %>
        <br><%
    }
} %>
</body>
</html>
