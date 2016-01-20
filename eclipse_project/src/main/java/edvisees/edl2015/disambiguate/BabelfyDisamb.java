package edvisees.edl2015.disambiguate;

import it.unimi.dsi.webgraph.labelling.ArcLabelledImmutableGraph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.UnflaggedOption;

import edvisees.edl2015.candidates.CandidateExtractor;
import edvisees.edl2015.candidates.CandidateExtractorFactory;
import edvisees.edl2015.candidates.DOMParser;
import edvisees.edl2015.candidates.Fragment;
import edvisees.edl2015.graph.WeightedGraphBuilder;
import edvisees.edl2015.lucene.IndexItem;
import edvisees.edl2015.lucene.Searcher;
import edvisees.edl2015.weights.SemanticSignatureMap;

public class BabelfyDisamb {
    private CandidateExtractor candidateExtractor;
    private SemanticSignatureMap semSigMap;
    public int queryCount = 0;
    public Map<String, Set<String>> fbRTypeMap;
    private Searcher idSearcher;
    private Searcher badIdSearcher;

    public BabelfyDisamb(
            SemanticSignatureMap semSigMap,
            CandidateExtractor candidateExtractor,
            String fbTypeFileName,
            Searcher idSearcher
    ) throws IOException {
        this.candidateExtractor = candidateExtractor;
        this.semSigMap = semSigMap;
        this.idSearcher = idSearcher;
        this.fbRTypeMap = this.loadFreebaseTypeMap(fbTypeFileName);
    }

    private Map<String, Set<String>> loadFreebaseTypeMap(String fbTypeFileName) throws IOException {
        Map<String, Set<String>> freebaseTypeMap = new HashMap<String, Set<String>>();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(fbTypeFileName), StandardCharsets.UTF_8));
            log("Loading freebase NER map file......");
            String line = null;
            String[] pair = null;
            while(in.ready()) {
                line = in.readLine();
                pair = line.split(" ");
                if (pair.length == 2) {
                    if (!freebaseTypeMap.containsKey(pair[0])) {
                        freebaseTypeMap.put(pair[0], new HashSet<String>());
                    }
                    freebaseTypeMap.get(pair[0]).add(pair[1]);
                }
            }
            log("Finished Loading freebase NER map file......");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) in.close();
        }
        return freebaseTypeMap;
    }

    private static void log(String msg) {
        System.out.println(msg);
    }

    public Map<Fragment, String> disamb(Fragment inputFragment, int maxResultsLucene, int ambiguityLevel, float scoreThreshold) throws Exception {
        List<Fragment> inputFragments = new ArrayList<Fragment>();
        inputFragments.add(inputFragment);
        return this.disamb(inputFragments, maxResultsLucene, ambiguityLevel, scoreThreshold);
    }

    /**
     * Main entry point for this application.
     *
     * @param inputSentence Input sentence to process.
     * @param ambiguityLevel Number of meanings a fragment is allowed to have (for the densest subgraph).
     * @param scoreThreshold The minimum score a candidateMeaning has to have in order to be considered a "good" solution.
     * @return
     * @throws Exception
     */
    public Map<Fragment, String> disamb(List<Fragment> inputFragments, int maxResultsLucene, int ambiguityLevel, float scoreThreshold) throws Exception {
        List<Fragment> graphFragments = new ArrayList<Fragment>();
        for (Fragment inputFragment : inputFragments) {
            List<Fragment> subFragments = this.candidateExtractor.findCandidateMeanings(inputFragment, maxResultsLucene);
            graphFragments.addAll(subFragments);
        }
        SemanticInterpretationGraph graph = SemanticInterpretationGraph.buildGraph(graphFragments, this.semSigMap, this.badIdSearcher);
        // graph.debugShape();
        graph = graph.densify(ambiguityLevel);
        graph.debugMeanings(10);
        return graph.chooseBestMeanings(scoreThreshold);
    }

    public static String getNERType(Set<String> allTypes){
        if (allTypes == null) {
            return null;
        }
        if (allTypes.contains("people.person")) {
            return "PER";
        }
        if (allTypes.contains("organization.organization")) {
            return "ORG";
        }
        if (allTypes.contains("location.country") || allTypes.contains("location.administrative_division") || allTypes.contains("location.statistical_region")){
            return "GPE";
        }
        if (allTypes.contains("architecture.structure")) {
            return "FAC";
        }
        if (allTypes.contains("location.location")) {
            return "LOC";
        }
        return null;
    }

    public List<Fragment> process(
            List<Fragment> inputText,
            int maxNumberResultsLucene,
            String time,
            int ambiguityLevel,
            float scoreThreshold
    ) throws Exception{
        Map<Fragment, String> bestMeanings = this.disamb(inputText, maxNumberResultsLucene, ambiguityLevel, scoreThreshold);
        List<Fragment> finalFragments = new ArrayList<Fragment>();
        String NIL = "NIL";
        for(Entry<Fragment, String> entry : bestMeanings.entrySet()) {
            Fragment fragment = entry.getKey();
            String entityId = entry.getValue();
            if(entityId.equals(NIL)) {
                fragment.freebaseId = NIL;
                if(fragment.nerStanford != null) {
                    fragment.nerType = fragment.nerStanford;
                    finalFragments.add(fragment);
                }
            }
            else {
                IndexItem idResult = idSearcher.findByIdUnique(entry.getValue());
                fragment.freebaseId = idResult.getTitle();
                fragment.nerType = getNERType(this.fbRTypeMap.get(fragment.freebaseId));
                if (fragment.nerType != null) {
                    finalFragments.add(fragment);
                }
            }
        }
        postProcessing(finalFragments);
        return finalFragments;
    }

    private void postProcessing(List<Fragment> finalFragments) {
        // merge Person or GPE entities, delete nested entities with same eids
        Set<Fragment> removed = new HashSet<Fragment>();
        for(Fragment fragment : finalFragments) {
            if(fragment.nerType.equals("PER") || fragment.nerType.equals("GPE")) {
                for(Fragment otherFragment : finalFragments) {
                    if(fragment.cover(otherFragment)) {
                        removed.add(otherFragment);
                    }
                }
            }
            else {
                for(Fragment otherFragment : finalFragments) {
                    if(fragment.cover(otherFragment) && fragment.freebaseId.equals(otherFragment.freebaseId)) {
                        removed.add(otherFragment);
                    }
                }
            }
        }
        finalFragments.removeAll(removed);
    }
    
    private void writeResults(List<Fragment> finalFragments, String time, String fileName) throws IOException {
        fileName = fileName.replaceAll("\\.(xml|df|nw)", "");
        PrintWriter fileWriter = new PrintWriter(new FileOutputStream(new File("EDL_CMU_Edvisees_Result_" + fileName + "_" + time + ".txt"), true));
        for (Fragment fragment : finalFragments) {
            fileWriter.println(this.getOutputString(fragment, fileName));
            this.queryCount++;
        }
        fileWriter.close();
    }

    private String getOutputString(Fragment fragment, String inputFileName) {
        return "CMU_Edvisees\t" +
                "QUERY_" + String.format("%05d", this.queryCount) + "\t" +
                fragment.text + "\t" +
                inputFileName + ":" + fragment.startOffset + "-" + (fragment.endOffset - 1) + "\t" +
                fragment.freebaseId + "\t" +
                fragment.nerType + "\t" +
                "NAM" + "\t" +
                "1.0";
    }
    
    public List<Fragment> processInputFile(File inputFile, int maxNumberResultsLucene, int ambiguityLevel, float scoreThreshold, String mode) throws Exception {
        if(mode.equals("doc")) {
            return processInputFileWithDoc(inputFile, maxNumberResultsLucene, ambiguityLevel, scoreThreshold);
        }
        else if(mode.equals("paragraph")) {
            return processInputFileWithPara(inputFile, maxNumberResultsLucene, ambiguityLevel, scoreThreshold);
        }
        else {
            throw new Exception("invalide processing mode: " + mode);
        }
    }
    
    public List<Fragment> processInputFileWithPara(File inputFile, int maxNumberResultsLucene, int ambiguityLevel, float scoreThreshold) throws Exception {
        log("processing file: " + inputFile.getName());
        SimpleDateFormat ft = new SimpleDateFormat ("yyyy_MM_dd_HH_mm_ss");
        String time = ft.format(new Date());
        DOMParser parser = new DOMParser(inputFile);
        final int LENGTH_TO_PROCESS = 500;
        List<Fragment> fragmentsToProcess = new ArrayList<Fragment>();
        List<Fragment> finalFragments = new ArrayList<Fragment>();
        int sumLength = 0;
        fragmentsToProcess.add(parser.headline); // the first context is the title
        sumLength += parser.headline.text.length();
        for (int i = 0; i < parser.paragraphs.size(); ++i) {
            Fragment paragraph = parser.paragraphs.get(i);
            fragmentsToProcess.add(paragraph);
            sumLength += paragraph.text.length();
            if(sumLength < LENGTH_TO_PROCESS && i + 1 < parser.paragraphs.size()) {
                continue;
            }
            
            finalFragments.addAll(process(fragmentsToProcess, maxNumberResultsLucene, time, ambiguityLevel, scoreThreshold));
            fragmentsToProcess = new ArrayList<Fragment>();
            sumLength = 0;
        }
        
        finalFragments.addAll(parser.authors);
        writeResults(finalFragments , time, inputFile.getName());
        return finalFragments;
    }

    public List<Fragment> processInputFileWithDoc(File inputFile, int maxNumberResultsLucene, int ambiguityLevel, float scoreThreshold) throws Exception {
        log("processing file: " + inputFile.getName());
        SimpleDateFormat ft = new SimpleDateFormat ("yyyy_MM_dd_HH_mm_ss");
        String time = ft.format(new Date());
        DOMParser parser = new DOMParser(inputFile);
        List<Fragment> fragmentsToProcess = new ArrayList<Fragment>();
        fragmentsToProcess.add(parser.headline); // the first context is the title
        for (Fragment paragraph: parser.paragraphs){
            fragmentsToProcess.add(paragraph);
        }

        List<Fragment> finalFragments = process(fragmentsToProcess, maxNumberResultsLucene, time, ambiguityLevel, scoreThreshold);
        finalFragments.addAll(parser.authors);
        writeResults(finalFragments, time, inputFile.getName());
        return finalFragments;
    }

    public static void main(String[] args) throws Exception {
        SimpleJSAP jsap = new SimpleJSAP(
            BabelfyDisamb.class.getName(),
            "(description)", // TODO improve the descriptions
            new Parameter[] {
                new UnflaggedOption("semSigBaseName", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
                        "The basename of the semantic signature graph."),
                new UnflaggedOption("luceneIndexDir", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
                        "Base dir for Lucene index (indexes inside are hardcoded: id, name, bad_ids)"),
                new UnflaggedOption("fbTypeFileName", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
                        "Freebase NER type files"),
                new UnflaggedOption("inputXmlDir", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
                        "The directory of input xml files"),
                new UnflaggedOption("language", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
                        "language"),
                new UnflaggedOption("ambiguityLevel", JSAP.INTEGER_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
                        "How many meanings per fragment allowed in the densified sub-graph (e.g. 10)."),
                new UnflaggedOption("scoreThreshold", JSAP.FLOAT_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
                        "Minimum score a fragment should have to be kept."),
                new UnflaggedOption("maxNumberResultsLucene", JSAP.INTEGER_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
                        "Maximum number of results we allow lucene to return when we query one fragment."),
                new UnflaggedOption("processingMode", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
                        "[doc|paragraph] process the whole doc or paragraph by paragraph.")
            }
        );
        final JSAPResult jsapResult = jsap.parse(args);
        if (jsap.messagePrinted()) {
            System.exit( 1 );
        }
        final String semSigBaseName = jsapResult.getString("semSigBaseName");
        final String luceneIndexDir = jsapResult.getString("luceneIndexDir");
        final String fbTypeFileName = jsapResult.getString("fbTypeFileName");
        final String inputXmlDir = jsapResult.getString("inputXmlDir");
        final String language = jsapResult.getString("language");
        final int ambiguityLevel = jsapResult.getInt("ambiguityLevel");
        final float scoreThreshold = jsapResult.getFloat("scoreThreshold");
        final int maxNumberResultsLucene = jsapResult.getInt("maxNumberResultsLucene");
        final String mode = jsapResult.getString("processingMode");

        List<String> validLangs = Arrays.asList(new String[]{"es", "en", "zh"});
        if (!validLangs.contains(language)) {
            log("invalid language input");
            System.exit(0);
        }
        // load the semantic graph and build a real semGraph
        ArcLabelledImmutableGraph semSigGraph = WeightedGraphBuilder.deserializeWeightedGraph(semSigBaseName);
        SemanticSignatureMap semSigMap = new SemanticSignatureMap(semSigGraph);
        Searcher nameSearcher = new Searcher(luceneIndexDir + File.separator + "index_name");
        File badIdSearcherFile = new File(luceneIndexDir + File.separator + "index_id_bad_ids");
        Searcher badIdSearcher = badIdSearcherFile.exists() ? new Searcher(badIdSearcherFile.getAbsolutePath()) : null;
        CandidateExtractor candidateExtractor = CandidateExtractorFactory.getCandidateExtractor(language, nameSearcher, badIdSearcher);
        Searcher idSearcher = new Searcher(luceneIndexDir + File.separator + "index_id");
        BabelfyDisamb disambiguator = new BabelfyDisamb(semSigMap, candidateExtractor, fbTypeFileName, idSearcher);

        File xmlDir = new File(inputXmlDir);
        File[] xmlDirectoryListing = xmlDir.listFiles();
        if (xmlDirectoryListing == null) {
            log(inputXmlDir +" is not a valid directory");
            System.exit(1);
        }
        Arrays.sort(xmlDirectoryListing);
        for (int i=0; i < xmlDirectoryListing.length; i++){
            List<Fragment> fragments = disambiguator.processInputFile(xmlDirectoryListing[i], maxNumberResultsLucene, ambiguityLevel, scoreThreshold, mode);
            for (Fragment fragment : fragments) {
                log(fragment.toString());
            }
        }
    }
}
