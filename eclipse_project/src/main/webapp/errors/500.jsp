<%@ page isErrorPage="true" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<t:head title="EDL 2015 - Error 500"></t:head>
<body>
<h1>Edvisees EDL 2015 tool - Internal Server Error</h1>
<br>
<p>Message: ${exception.message}</p>
<p>Message 2: ${pageContext.errorData.throwable.cause}</p>
</body>
</html>

