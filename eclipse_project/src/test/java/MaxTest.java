import it.unimi.dsi.webgraph.ArrayListMutableGraph;
import it.unimi.dsi.webgraph.BVGraph;
import it.unimi.dsi.webgraph.Transform;
import it.unimi.dsi.webgraph.labelling.ArcLabelledImmutableGraph;
import it.unimi.dsi.webgraph.labelling.ArcLabelledNodeIterator;
import it.unimi.dsi.webgraph.labelling.ArcLabelledNodeIterator.LabelledArcIterator;
import it.unimi.dsi.webgraph.labelling.BitStreamArcLabelledImmutableGraph;

import java.io.IOException;
import java.util.Properties;

import org.junit.Test;

import edvisees.edl2015.weights.PageRankSemanticSignatureBuilder;
import edvisees.edl2015.weights.SemanticSignatureBuilder;
import edvisees.edl2015.weights.TriangleWeightScorer;
import edvisees.edl2015.weights.WeightScorer;

public class MaxTest {

    @Test
    public void test1() {
        ArrayListMutableGraph graph = new ArrayListMutableGraph(5);
        graph.addArc(0, 1);
        graph.addArc(0, 2);
        graph.addArc(2, 1);
        graph.addArc(1, 0);
        graph.addArc(2, 4);
        graph.addArc(4, 0);
        
        try {
            String baseName = "../graph/tmp";
            BVGraph.store(graph.immutableView(), baseName);
            BVGraph.store(Transform.transpose(graph.immutableView()), baseName + ".trans");
            BVGraph bvGraph = BVGraph.load(baseName);
            BVGraph bvGraph_transpose = BVGraph.load(baseName + ".trans");
            System.out.println(bvGraph.numNodes() + " " + bvGraph.numArcs());
            WeightScorer ws = new TriangleWeightScorer();
            ArcLabelledImmutableGraph weightedGraph = ws.calcWeights(bvGraph, bvGraph_transpose, 2);
            System.out.println(weightedGraph.numNodes());
            ArcLabelledNodeIterator iter = weightedGraph.nodeIterator();
            while(iter.hasNext()) {
                int index = iter.nextInt();
                LabelledArcIterator arcIter = iter.successors();
                int dest = arcIter.nextInt();
                while(dest > -1) {
                    System.out.println(index + "--->" + dest + ": " + arcIter.label().getFloat());
                    dest = arcIter.nextInt();
                }
            }
            System.out.println("Persist weighted graph to disk:");
            System.out.println( "Compressing graph" );
            BVGraph.store(weightedGraph, baseName + "weight" + ArcLabelledImmutableGraph.UNDERLYINGGRAPH_SUFFIX);
            System.out.println( "Storing labels" );
            BitStreamArcLabelledImmutableGraph.store(weightedGraph, baseName + "weight", baseName + "weight" + ArcLabelledImmutableGraph.UNDERLYINGGRAPH_SUFFIX);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Test
    public void test2() {
        try {
            ArcLabelledImmutableGraph weightedGraph = ArcLabelledImmutableGraph.load("../graph/tmpweight");
            ArcLabelledNodeIterator iter = weightedGraph.nodeIterator();
            while(iter.hasNext()) {
                int index = iter.nextInt();
                LabelledArcIterator arcIter = iter.successors();
                int dest = arcIter.nextInt();
                while(dest > -1) {
                    System.out.println(index + "--->" + dest + ": " + arcIter.label().getFloat());
                    dest = arcIter.nextInt();
                }
            }
            
            SemanticSignatureBuilder semBuilder = new PageRankSemanticSignatureBuilder();
            Properties props = new Properties();
            props.setProperty("beta", "1.0");
            props.setProperty("threshold", "0.0001");
            ArcLabelledImmutableGraph semGraph = semBuilder.buildSemanticSignatureGraph(weightedGraph, 1, props);
            
            iter  = semGraph.nodeIterator();
            while(iter.hasNext()) {
                int index = iter.nextInt();
                LabelledArcIterator arcIter = iter.successors();
                int dest = arcIter.nextInt();
                while(dest > -1) {
                    System.out.println(index + "--->" + dest + ": " + arcIter.label().getFloat());
                    dest = arcIter.nextInt();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
