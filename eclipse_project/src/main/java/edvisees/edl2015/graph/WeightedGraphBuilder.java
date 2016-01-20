package edvisees.edl2015.graph;
import it.unimi.dsi.webgraph.BVGraph;
import it.unimi.dsi.webgraph.labelling.ArcLabelledImmutableGraph;
import it.unimi.dsi.webgraph.labelling.ArcLabelledNodeIterator;
import it.unimi.dsi.webgraph.labelling.ArcLabelledNodeIterator.LabelledArcIterator;
import it.unimi.dsi.webgraph.labelling.BitStreamArcLabelledImmutableGraph;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.UnflaggedOption;

import edvisees.edl2015.lucene.IndexItem;
import edvisees.edl2015.lucene.Searcher;
import edvisees.edl2015.weights.WeightScorer;

public class WeightedGraphBuilder {
    public static ArcLabelledImmutableGraph deserializeWeightedGraph(String baseName) throws IOException {
        System.out.println("Loading weighted graph from '" + baseName + "'");
        long start = System.currentTimeMillis() / 1000;
        ArcLabelledImmutableGraph weightedGraph = BitStreamArcLabelledImmutableGraph.load(baseName);
        System.out.println("\nWeighted Graph loaded! " + weightedGraph.numNodes() + " nodes and " + weightedGraph.numArcs() + " arcs");
        System.out.println("Took: " + (System.currentTimeMillis() / 1000 - start) + " seconds");
        return weightedGraph;
    }

    public static void storeWeightedGraph(ArcLabelledImmutableGraph weightedGraph, String baseName) throws IOException {
        System.out.println("Persist weighted graph to disk:");
        System.out.println("Compressing graph");
        BVGraph.store(weightedGraph, baseName + ArcLabelledImmutableGraph.UNDERLYINGGRAPH_SUFFIX);
        System.out.println("Done compressing, now Storing labels");
        // There is a bug in BVGraph when storing the labelled graph. The underlying basename in properties file is not correct 
        BitStreamArcLabelledImmutableGraph.store(weightedGraph, baseName, (new File(baseName)).getName() + ArcLabelledImmutableGraph.UNDERLYINGGRAPH_SUFFIX);
        System.out.println("Done Storing labels" );
    }

    private static ArcLabelledImmutableGraph buildWeightedGraph(String graphBaseName, String weightedGraphBaseName)
        throws Exception
    {
        System.out.println("Loading graph");
        BVGraph graph = BVGraph.load(graphBaseName);
        System.out.println("\nGraph loaded! " + graph.numNodes() + " nodes and " + graph.numArcs() + " arcs");
        System.out.println("\nLoading Transpose Graph");
        BVGraph transposeGraph = BVGraph.load(graphBaseName + ".trans");
        System.out.println("\nTranspose Graph loaded! " + transposeGraph.numNodes() + " nodes and " + transposeGraph.numArcs() + " arcs");

        System.out.println("\nCalculating scores");
        WeightScorer scorer = WeightScorer.createWeightScorer("TriangleWeightScorer");
        long clock = System.currentTimeMillis() / 1000;
        ArcLabelledImmutableGraph weightedGraph = scorer.calcWeights(graph, transposeGraph, 20);
        System.out.println("Scores finished: took: " + (System.currentTimeMillis() / 1000 - clock));

        storeWeightedGraph(weightedGraph, weightedGraphBaseName);
        return weightedGraph;
    }

    private static String getNames(List<IndexItem> names) {
        if (names.size() == 0) return "(no names)";
        List<String> namesStr = new ArrayList<String>();
        for (IndexItem name : names) {
            namesStr.add(name.getTitle());
        }
        return String.join(" // ", namesStr);
    }
    public static String iterateWeightedGraph(
        ArcLabelledImmutableGraph wGraph,
        Searcher idSearcher,
        Searcher nameSearcher
    ) throws Exception {
        List<String> nodesOutput = new ArrayList<String>();
        for (ArcLabelledNodeIterator nodeIter = wGraph.nodeIterator(); nodeIter.hasNext();) {
            Integer nodeIndex = nodeIter.next();
            if (nodeIndex > 50) break;
            String fbId = idSearcher.findByIdUnique(nodeIndex + "").getTitle();
            List<IndexItem> names = nameSearcher.findById(nodeIndex + "", 30);
            String nodeNames = getNames(names);
            StringBuffer msg = new StringBuffer();
            LabelledArcIterator successorsIterator = wGraph.successors(nodeIndex);
            int qSuccessors = 0;
            for (int successorIndex = successorsIterator.nextInt(); successorIndex > -1; ++qSuccessors, successorIndex = successorsIterator.nextInt()) {
                float arcWeight = successorsIterator.label().getFloat();
                if (arcWeight == 0) continue;
                String successorFbId = idSearcher.findByIdUnique(successorIndex + "").getTitle();
                List<IndexItem> successorNames = nameSearcher.findById(successorIndex + "", 30);
                String successorNamesStr = getNames(successorNames);
                msg.append("\t[").append(successorIndex)
                   .append(" = ").append(successorFbId)
                   .append(" = '").append(successorNamesStr).append("'")
                   .append("] w/ weight = ").append(arcWeight).append("\n");
            }
            // "Node with index 48 (m.xxxx) 'name1, name2' has 4 successors."
            StringBuffer header = new StringBuffer();
            header.append("Node with index ").append(nodeIndex).append(" (").append(fbId).append(")")
               .append(" '").append(nodeNames).append("'")
               .append(" has ").append(qSuccessors).append(" successors:\n");
            nodesOutput.add(header.toString() + msg.toString());
        }
        return String.join("\n", nodesOutput);
    }


    public static void main(String[] args) throws Exception {
        SimpleJSAP jsap = new SimpleJSAP(
            WeightedGraphBuilder.class.getName(),
            "Weighted Graph Builder: the input is an unlabelled graph, the output is the weight-labelled graph.",
            new Parameter[] {
                new UnflaggedOption("graphBaseName", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
                        "The basename of the unlabelled graph."),
                new UnflaggedOption("weightedGraphBaseName", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
                        "The basename for the arc-labelled graph (output)")
            }
        );
        final JSAPResult jsapResult = jsap.parse(args);
        if (jsap.messagePrinted()) System.exit(1);
        final String graphBaseName = jsapResult.getString("graphBaseName");
        final String weightedGraphBaseName = jsapResult.getString("weightedGraphBaseName");
        try {
            buildWeightedGraph(graphBaseName, weightedGraphBaseName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
