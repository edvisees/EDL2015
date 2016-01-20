<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ page import="javax.naming.InitialContext,
    edvisees.edl2015.lucene.*,
    org.apache.commons.lang3.StringEscapeUtils,
    java.util.*"
%><%!
String getFreebaseId(String nodeFbId) {
    return "http://www.freebase.com/m/" + nodeFbId.substring(nodeFbId.indexOf('.') + 1);
}

String getNavigateGraphLink(int nodeIndex) {
    return "/navigate.jsp?nodeId=" + nodeIndex;
}
%><%
response.setContentType("text/html");
InitialContext ctx = new InitialContext();
Searcher idSearcher = (Searcher) ctx.lookup("java:comp/env/idSearcher");
Searcher nameSearcher = (Searcher) ctx.lookup("java:comp/env/nameSearcher");
String query = request.getParameter("query");
query = query == null ? "" : query;
%>
<!DOCTYPE html>
<html>
<t:head title="Query Lucene Index for names"></t:head>
<body>
<t:menu></t:menu>
<h1>Query Lucene Index</h1>
<form method="get">
Enter text to search for:
<input type="text" name="query" size="50" value="<%=query%>" />
<br>
<input type="submit" />
</form>
<br>
<%
if (query != "") {
    // query lucene
    List<IndexItem> result = new ArrayList<IndexItem>();
    try {
        int nodeId =  Integer.parseInt(query);
        // it is a node id, combine both the FB_ID with the FB_NAMES
        result.add(idSearcher.findByIdUnique(query));
        result.addAll(nameSearcher.findById(query, 50)); // 50 should give an idea of the name of the entity
    } catch (Exception e) {
        // maybe FB_ID?
        if (query.startsWith("m.")) {
            result = idSearcher.findByTitle(query, 10); // should be only 1!!!!
        } else {
            // assume we are searching by name text
            result = nameSearcher.findByTitle(query, 5000); // when searching by name no number is big enough (?)
        }
    } %>
    <table>
        <thead>
            <tr>
                <th colspan="2">Hits: <%=result.size()%></th>
            </tr>
        </thead>
        <tbody><%
        for (IndexItem item : result) { %>
            <tr>
                <td>
                    <a href="<%=getNavigateGraphLink(Integer.parseInt(item.getId()))%>" target="_blank"><%=item.getId()%></a>
                </td>
                <td><%=StringEscapeUtils.escapeHtml4(item.getTitle())%></td>
            </tr><%
        } %>
        </tbody>
    </table><%
}  %>
</body>
</html>
