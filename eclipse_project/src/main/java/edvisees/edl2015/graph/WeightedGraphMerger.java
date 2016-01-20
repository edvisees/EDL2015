package edvisees.edl2015.graph;

import it.unimi.dsi.webgraph.labelling.ArcLabelledImmutableGraph;
import it.unimi.dsi.webgraph.labelling.ArcLabelledNodeIterator;
import it.unimi.dsi.webgraph.labelling.ArcLabelledNodeIterator.LabelledArcIterator;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.UnflaggedOption;

import es.yrbcn.graph.weighted.WeightedArc;
import es.yrbcn.graph.weighted.WeightedBVGraph;


public class WeightedGraphMerger {

    private final static Logger LOGGER = LoggerFactory.getLogger(WeightedGraphMerger.class);
    
    public static void main(String[] args) throws JSAPException, IOException {
        SimpleJSAP jsap = new SimpleJSAP (
                WeightedGraphMerger.class.getName(),
                "(description)", // TODO improve the descriptions
                new Parameter[] {
                    new UnflaggedOption("semsigBaseName", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
                            "The basename for the semantic signature graph (input & output)"),
                    new UnflaggedOption("subgraphBaseName", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
                            "The basename for the semantic signature subgraph (input & output)"),
                    new UnflaggedOption("suffixes", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
                            "The file contains the list of suffixes of subgraphs")
                }
        );
        
        final JSAPResult jsapResult = jsap.parse(args);
        if (jsap.messagePrinted()) System.exit(1);
        String graphBaseName = jsapResult.getString("semsigBaseName");
        String subGraphBaseName = jsapResult.getString("subgraphBaseName");
        String suffixName = jsapResult.getString("suffixes");
        BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(suffixName), "utf-8"));
        List<String> suffixes = new ArrayList<String>();
        while(input.ready()) {
            suffixes.add(input.readLine().trim());
        }
        input.close();
        
        List<String> subGraphBaseNames = new ArrayList<String>();
        for(String suffix : suffixes) {
            subGraphBaseNames.add(subGraphBaseName + "_" + suffix);
        }
        
        mergeWeightedGraph(subGraphBaseNames, graphBaseName);
    }
    
    public static void mergeWeightedGraph(List<String> subGraphBaseNames, String graphBaseName) throws IOException {
        int numNodes = -1;
        
        List<WeightedArc> arcList = new LinkedList<WeightedArc>();
        boolean[] nodes = null;
        for(String baseName : subGraphBaseNames) {
            LOGGER.info("loading subgraph: " + baseName);
            ArcLabelledImmutableGraph subGraph = WeightedGraphBuilder.deserializeWeightedGraph(baseName);
            LOGGER.info("completed");
            if(numNodes == -1) {
                numNodes = subGraph.numNodes();
                nodes = new boolean[numNodes];
                LOGGER.info("number of nodes: " + numNodes);
            }
            else if(numNodes != subGraph.numNodes()){
                LOGGER.error("number of nodes of subgraph is wrong: " + subGraph.numNodes());
                throw new RuntimeException();
            }
            
            LOGGER.info("merging subgraph " + baseName);
            ArcLabelledNodeIterator nodeIter = subGraph.nodeIterator();
            int n = numNodes, index, succ;
            while(n-- != 0) {
                index = nodeIter.nextInt();
                if(nodeIter.outdegree() > 0) {
                    if(nodes[index]) {
                        LOGGER.error("node " + index + " has appeared in previouse subgraph");
                        throw new RuntimeException();
                    }
                    else {
                        nodes[index] = true;
                    }
                    LabelledArcIterator succIter = nodeIter.successors();
                    while((succ = succIter.nextInt()) != -1) {
                        arcList.add(new WeightedArc(index, succ, succIter.label().getFloat()));
                    }
                }
            }
            LOGGER.info("completed");
        }
        
        LOGGER.info("building final graph " + graphBaseName);
        WeightedArc[] arcs = new WeightedArc[arcList.size()];
        int entry = 0;
        for(WeightedArc arc : arcList) {
            arcs[entry++] = arc;
        }
        WeightedGraphBuilder.storeWeightedGraph(new WeightedBVGraph(arcs, numNodes), graphBaseName);
        LOGGER.info("Done.");
    }

}
