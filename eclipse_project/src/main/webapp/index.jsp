<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ page import="javax.naming.InitialContext" %><%
InitialContext ctx = new InitialContext();
response.setContentType("text/html");
response.setStatus(HttpServletResponse.SC_OK);
%>
<!DOCTYPE html>
<html>
<t:head title="EDL 2015 - Home"></t:head>
<body>
<t:menu></t:menu>
<h1>Edvisees EDL 2015 tool</h1>
Using graph read from: <br>
<b>runtime</b> = <%=ctx.lookup("java:comp/env/runtimeDir")%><br>
</body>
</html>
