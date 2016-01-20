package edvisees.edl2015.weights;

import it.unimi.dsi.webgraph.labelling.ArcLabelledImmutableGraph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.UnflaggedOption;

import edvisees.edl2015.graph.WeightedGraphBuilder;

public abstract class SemanticSignatureBuilder {

    public static SemanticSignatureBuilder createBuilder(String builderClassName) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return (SemanticSignatureBuilder) Class.forName(SemanticSignatureBuilder.class.getPackage().getName() + "." + builderClassName).newInstance();
    }

    // build semantic signatures for all the nodes
    public abstract ArcLabelledImmutableGraph buildSemanticSignatureGraph(ArcLabelledImmutableGraph weightedGraph, int numThreads, Properties props);
    
    // build semantic signatures for the nodes from index from to to
    public abstract ArcLabelledImmutableGraph buildSemanticSignatureGraph(ArcLabelledImmutableGraph weightedGraph, int from, int to, int numThreads, Properties props);
    
    // build semantic signatures for the nodes whose index are in the list nodes
    public abstract ArcLabelledImmutableGraph buildSemanticSignatureGraph(ArcLabelledImmutableGraph weightedGraph, List<Integer> nodes, int numThreads, Properties props);
    
    /**
     * read an existing weighted graph from disc,
     * and calculate the page rank, and build the semantic signatures
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        SimpleJSAP jsap = new SimpleJSAP(
            SemanticSignatureBuilder.class.getName(),
            "(description)", // TODO improve the descriptions
            new Parameter[] {
                new UnflaggedOption("weightedGraphBaseName", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
                        "The basename for the arc-labelled graph (input)"),
                new UnflaggedOption("semanticSignatureGraphBaseName", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
                        "The basename for the semantic signature graph (output)"), 
                new UnflaggedOption("nodeListFileName", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
                        "The file contains the indexed of the nodes to be processed"),
                new UnflaggedOption("threadNum", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
                        "The number of threads")
            }
        );
        final JSAPResult jsapResult = jsap.parse(args);
        if (jsap.messagePrinted()) System.exit(1);
        final String weightedGraphBaseName = jsapResult.getString("weightedGraphBaseName");
        final String semanticSignatureGraphBaseName = jsapResult.getString("semanticSignatureGraphBaseName");
        final String nodeFileName = jsapResult.getString("nodeListFileName");
        final int threadNum = Integer.valueOf(jsapResult.getString("threadNum"));

        ArcLabelledImmutableGraph wGraph = WeightedGraphBuilder.deserializeWeightedGraph(weightedGraphBaseName);
        SemanticSignatureBuilder semBuilder = createBuilder("ApproxPageRankSemanticSignatureBuilder");
        Properties props = new Properties();
        ArrayList<Integer> nodes = new ArrayList<Integer>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(nodeFileName)), "utf-8"));
        while(reader.ready()) {
            nodes.add(Integer.valueOf(reader.readLine().trim()));
        }
        reader.close();
        
        ArcLabelledImmutableGraph semGraph = semBuilder.buildSemanticSignatureGraph(wGraph, nodes, threadNum, props);
        WeightedGraphBuilder.storeWeightedGraph(semGraph, semanticSignatureGraphBaseName);
    }
}
