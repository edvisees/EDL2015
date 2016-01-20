<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ page import="javax.naming.InitialContext,
    it.unimi.dsi.webgraph.labelling.ArcLabelledImmutableGraph,
    it.unimi.dsi.webgraph.labelling.ArcLabelledNodeIterator.LabelledArcIterator,
    org.apache.commons.lang3.StringEscapeUtils,
    edvisees.edl2015.lucene.*,
    java.util.*"
%><%!
String getFreebaseId(String nodeFbId) {
    return "http://www.freebase.com/m/" + nodeFbId.substring(nodeFbId.indexOf('.') + 1);
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
ArcLabelledImmutableGraph graph = (ArcLabelledImmutableGraph) ctx.lookup("java:comp/env/graph");
Searcher idSearcher = (Searcher) ctx.lookup("java:comp/env/idSearcher");
Searcher nameSearcher = (Searcher) ctx.lookup("java:comp/env/nameSearcher");
String nodeId = request.getParameter("nodeId");
nodeId = nodeId == null ? "" : nodeId; %>
<!DOCTYPE html>
<html>
<t:head title="EDL 2015 - Navigate Graph"></t:head>
<body>
<t:menu></t:menu>
<h1>Navigate Graph</h1>
<form method="get">
Enter node-index or node-id (freebase):
<input type="text" name="nodeId" size="10" value="<%=nodeId%>" />
<br>
<input type="submit" />
</form>
<br>
<%
if (nodeId != "") {
    // show the content of the node, plus a list of successors
    Integer nodeIndex = null;
    String fbId = null;
    try {
        nodeIndex = Integer.parseInt(nodeId);
        fbId = idSearcher.findByIdUnique(nodeId).getTitle();
    } catch (Exception e) {
        // it is a FB_ID -> get the nodeIndex
        try {
            fbId = nodeId;
            nodeIndex = Integer.parseInt(idSearcher.findByTitleUnique(nodeId).getId());
        } catch (Exception e2) { %>
            Invalid id: '<%=nodeId%>' (<%=e2.getMessage()%>)<br> <%
        }
    }
    if (nodeIndex != null) {
        String fbUrl = getFreebaseId(fbId);
        List<IndexItem> nodeNames = nameSearcher.findById(nodeIndex + "", 100);
        String nodeNamesStr = getNames(nodeNames); %>
        <b>SELECTED NODE: </b>
        <a href="<%=fbUrl%>" target="_blank"><%=fbId%></a> (index = <%=nodeIndex%>)
        <br>
        <b>names:</b> <%=nodeNamesStr%> <br>
        <a href="semantic_signatures.jsp?nodeIndex=<%=nodeIndex%>" target="_blank">semantic signature</a> <br>
        <b>successors:</b> <%
        LabelledArcIterator successorsIterator = graph.successors(nodeIndex);
        for (int successorIndex = successorsIterator.nextInt(); successorIndex > -1; successorIndex = successorsIterator.nextInt()) {
            // TODO: format floats
            // TODO: sort by weight
            float arcWeight = successorsIterator.label().getFloat();
            if (arcWeight == 0) continue;
            String successorFbId = idSearcher.findByIdUnique(successorIndex + "").getTitle();
            List<IndexItem> successorNames = nameSearcher.findById(successorIndex + "", 30);
            String successorNamesStr = getNames(successorNames); %>
            <br>&nbsp;&nbsp;&nbsp;&nbsp;
            <%=arcWeight%>: <a href="?nodeId=<%=successorIndex%>"><%=successorFbId%></a>
            &nbsp;&nbsp;
            <a href="<%=getFreebaseId(successorFbId)%>" target="_blank">(open in freebase)</a>
            &nbsp;&nbsp;
            <%=successorNamesStr%><%
        }
    }
} %>
</body>
</html>
