<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ page import="javax.naming.InitialContext,
    org.apache.commons.lang3.StringEscapeUtils,
    edvisees.edl2015.candidates.CandidateExtractor,
    edvisees.edl2015.candidates.CandidateExtractorFactory,
    edvisees.edl2015.disambiguate.BabelfyDisamb,
    edvisees.edl2015.candidates.Fragment,
    edvisees.edl2015.lucene.Searcher,
    edvisees.edl2015.weights.SemanticSignatureMap,
    edvisees.edl2015.evaluation.Annotation,
    edvisees.edl2015.evaluation.AnnotatedDocument,
    edvisees.edl2015.evaluation.CorpusAnnotations,
    edvisees.edl2015.evaluation.Evaluation,
    java.io.*,
    java.util.*"
%><%
response.setContentType("text/html");
InitialContext ctx = new InitialContext();
String runtimeDir = (String) ctx.lookup("java:comp/env/runtimeDir");
Searcher idSearcher = (Searcher) ctx.lookup("java:comp/env/idSearcher");
Searcher nameSearcher = (Searcher) ctx.lookup("java:comp/env/nameSearcher");
Searcher badIdsSearcher = (Searcher) ctx.lookup("java:comp/env/badIdsSearcher");
SemanticSignatureMap semSigMap = (SemanticSignatureMap) ctx.lookup("java:comp/env/semSigMap");


String action = request.getParameter("action");
action = action == null ? "" : action;

String language = request.getParameter("language");
language = (language == null || language.length() == 0) ? "eng" : language; // eng spa cmn

String source = request.getParameter("source");
source = (source == null || source.length() == 0) ? "newswire" : source;

String maxResultsStr = request.getParameter("maxResults");
int maxResults = (maxResultsStr == null || maxResultsStr.length() == 0) ? 100 : Integer.parseInt(maxResultsStr);

String inputFileName = request.getParameter("fileName");

String trainingCorpusDir = runtimeDir + "/../training_data_2.0/data/";
String goldFilePath = trainingCorpusDir + "tac_kbp_2015_tedl_training_gold_standard_entity_mentions.tab";
String trainingFilesDirPath = trainingCorpusDir + "source_docs/" + language + "_xml/" + source + "/";

File inputFile = new File(trainingFilesDirPath + inputFileName);
CorpusAnnotations goldenAnnotations = new CorpusAnnotations(goldFilePath);

String scoreThresholdStr = request.getParameter("scoreThreshold");
scoreThresholdStr = scoreThresholdStr == null ? "" : scoreThresholdStr.trim();
float scoreThreshold = 0.5f;
if (scoreThresholdStr.length() > 0) {
    scoreThreshold = Float.parseFloat(scoreThresholdStr); // TODO: handle exceptions
}

String twoLetterLang = language.equals("eng") ? "en" : language.equals("spa") ? "es" : language.equals("cmn") ? "zh" : null;
CandidateExtractor candidateExtractor = CandidateExtractorFactory.getCandidateExtractor(twoLetterLang, nameSearcher, badIdsSearcher);
BabelfyDisamb disambiguator = new BabelfyDisamb(semSigMap, candidateExtractor, runtimeDir + "/fb_ner_type.txt", idSearcher);
List<Fragment> fragments = disambiguator.processInputFileWithDoc(inputFile, 100, 10, scoreThreshold);

// golden annotations for file
String docFileName = inputFile.getName().substring(0, inputFile.getName().indexOf(".")); // remove all the extensions, .xml, .df.xml, etc
AnnotatedDocument goldenAnnotationsForFile = goldenAnnotations.getDocument(docFileName);

// our annotations for file
CorpusAnnotations fileAnnotations = new CorpusAnnotations(fragments, docFileName);
Evaluation eval = new Evaluation(goldenAnnotations, fileAnnotations);
List<String> evalResults = eval.calculateResult(1);
%>
<!DOCTYPE html>
<html>
<t:head title="EDL 2015 - evaluation"></t:head>
<body>
<t:menu></t:menu>
<h1>evaluate edl 2015</h1>

<h2>evaluation results for input file: <b><%=inputFile.getPath()%></b></h2>
<br><%
Collections.sort(fragments);
for (Fragment fragment : fragments) { %>
    <%=fragment.toString() + " " + fragment.nerType + " " + fragment.freebaseId%><br><%
}
%>
<h2> GOLDEN ANNOTATIONS: </h2>
<%

for (Annotation annotation : goldenAnnotationsForFile.getAnnotations()) { %>
    <%=annotation%><br><%
}
%>
<br>
<h2> EVAL RESULTS: </h2>
<%
for (String evalResult : evalResults) { %>
    <%=evalResult%><br><%
} %>
</body>
</html>
