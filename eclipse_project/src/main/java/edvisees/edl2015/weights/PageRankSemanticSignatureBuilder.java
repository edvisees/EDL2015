package edvisees.edl2015.weights;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.webgraph.labelling.ArcLabelledImmutableGraph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.yrbcn.graph.weighted.WeightedArc;
import es.yrbcn.graph.weighted.WeightedBVGraph;
import es.yrbcn.graph.weighted.WeightedPageRank;
import es.yrbcn.graph.weighted.WeightedPageRank.StoppingCriterion;
import es.yrbcn.graph.weighted.WeightedPageRankWithConnectedTerm;

public class PageRankSemanticSignatureBuilder extends SemanticSignatureBuilder {

    protected final static Logger LOGGER = LoggerFactory.getLogger(PageRankSemanticSignatureBuilder.class);
    
    protected static int currentNum = 0;
    
    protected double threshold;
    protected int numNodes;
    
    public PageRankSemanticSignatureBuilder() {
        super();
    }
    
    protected void buildSemanticSignatureOfNode(WeightedPageRank pagerank, StoppingCriterion stpc, int index, List<WeightedArc> arcList) {
        try {
            pagerank.stepUntil(stpc);
            for(int j = 0; j < numNodes; ++j) {
                if(j != index && pagerank.rank[j] > threshold) {
                    arcList.add(new WeightedArc(index, j, (float) pagerank.rank[j]));
//                    System.out.println(index + "-->" + j + ": " + (float) pagerank.rank[j]);
                }
            }
//            LOGGER.info("size: " + arcList.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    ArrayList<Integer> getActiveNodes(ArcLabelledImmutableGraph weightedGraph) {
        ArrayList<Integer> activeNodeList = new ArrayList<Integer>();
        for(int j = 0; j < numNodes; ++j) {
            if(weightedGraph.outdegree(j) > 0) {
                activeNodeList.add(j);
            }
        }
        return activeNodeList;
    }
    
    ArrayList<Integer> getActiveNodes(ArcLabelledImmutableGraph weightedGraph, int from, int to) {
        ArrayList<Integer> activeNodeList = new ArrayList<Integer>();
        for(int j = from; j < to; ++j) {
            if(weightedGraph.outdegree(j) > 0) {
                activeNodeList.add(j);
            }
        }
        return activeNodeList;
    }
    
    ArrayList<Integer> getActiveNodes(ArcLabelledImmutableGraph weightedGraph, List<Integer> nodes) {
        ArrayList<Integer> activeNodeList = new ArrayList<Integer>();
        for(Integer j : nodes) {
            if(weightedGraph.outdegree(j) > 0) {
                activeNodeList.add(j);
            }
        }
        return activeNodeList;
    }
    
    protected ArcLabelledImmutableGraph getSemanticSignature(ArcLabelledImmutableGraph weightedGraph, ArrayList<Integer> activeNodes, int numThreads, double alpha, double beta, String norm, StoppingCriterion stpc) {
        int numActiveNodes = activeNodes.size();
        LOGGER.info("Initializing Threads...");
        int range = (numActiveNodes + numThreads - 1) / numThreads;
        PageRankSemSigThread[] threads = new PageRankSemSigThread[numThreads];
        for(int i = 0; i < numThreads; ++i) {
            WeightedPageRank pagerank = new WeightedPageRankWithConnectedTerm(i == 0 ? weightedGraph : weightedGraph.copy(), alpha, beta, norm, false);
            threads[i] = new PageRankSemSigThread(i, i * range, Math.min((i + 1) * range, numActiveNodes), pagerank, stpc, activeNodes);
        }
        currentNum = 0;
        
        LOGGER.info("Building semantic signatures...");
        try {
            for(PageRankSemSigThread thread : threads) {
                thread.start();
            }
            
            for(PageRankSemSigThread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        LOGGER.info("Done.");
        
        LOGGER.info("Creating semantic signature graph...");
        int numArcs = 0;
        for(int i = 0; i < numThreads; ++i) {
            numArcs += threads[i].arcList.size();
        }
        WeightedArc[] semanticArcs = new WeightedArc[numArcs];
        int entry = 0;
        for(int i = 0; i < numThreads; ++i) {
            for(WeightedArc arc : threads[i].arcList) {
                semanticArcs[entry++] = arc;
            }
        }
        LOGGER.info("Done.");
        return new WeightedBVGraph(semanticArcs, numNodes);
    }
    
    @Override
    public ArcLabelledImmutableGraph buildSemanticSignatureGraph(ArcLabelledImmutableGraph weightedGraph, int numThreads, Properties props) {
        Double alpha = Double.valueOf(props.getProperty("alpha", "0.85"));
        Double beta = Double.valueOf(props.getProperty("beta", "0.9"));
        String norm = props.getProperty("norm", "INFTY");
        this.threshold = Double.valueOf(props.getProperty("threshold", "1e-4"));
        this.numNodes = weightedGraph.numNodes();
        
        LOGGER.info("Collecting active nodes...");
        ArrayList<Integer> activeNodes = getActiveNodes(weightedGraph);
        LOGGER.info("Done.");
        LOGGER.info("The number of active nodes is: " + activeNodes.size());
        
        //create stoping criterion
        int maxIter = 30;
        double norm_delta_threhold = 1E-5;
        StoppingCriterion stpc = WeightedPageRank.or(new WeightedPageRank.NormDeltaStoppingCriterion(norm_delta_threhold), 
                new WeightedPageRank.IterationNumberStoppingCriterion(maxIter));
        
        return getSemanticSignature(weightedGraph, activeNodes, numThreads, alpha, beta, norm, stpc);
        
    }
    
    @Override
    public ArcLabelledImmutableGraph buildSemanticSignatureGraph(ArcLabelledImmutableGraph weightedGraph, int from,
            int to, int numThreads, Properties props) {
        Double alpha = Double.valueOf(props.getProperty("alpha", "0.85"));
        Double beta = Double.valueOf(props.getProperty("beta", "0.9"));
        String norm = props.getProperty("norm", "INFTY");
        this.threshold = Double.valueOf(props.getProperty("threshold", "1e-4"));
        this.numNodes = weightedGraph.numNodes();
        
        LOGGER.info("Collecting active nodes...");
        ArrayList<Integer> activeNodes = getActiveNodes(weightedGraph, from, to);
        LOGGER.info("Done.");
        LOGGER.info("The number of active nodes is: " + activeNodes.size());
        
        //create stoping criterion
        int maxIter = 30;
        double norm_delta_threhold = 1E-5;
        StoppingCriterion stpc = WeightedPageRank.or(new WeightedPageRank.NormDeltaStoppingCriterion(norm_delta_threhold), 
                new WeightedPageRank.IterationNumberStoppingCriterion(maxIter));
        
        return getSemanticSignature(weightedGraph, activeNodes, numThreads, alpha, beta, norm, stpc);
    }
    
    @Override
    public ArcLabelledImmutableGraph buildSemanticSignatureGraph(ArcLabelledImmutableGraph weightedGraph,
            List<Integer> nodes, int numThreads, Properties props) {
        Double alpha = Double.valueOf(props.getProperty("alpha", "0.85"));
        Double beta = Double.valueOf(props.getProperty("beta", "0.9"));
        String norm = props.getProperty("norm", "INFTY");
        this.threshold = Double.valueOf(props.getProperty("threshold", "1e-4"));
        this.numNodes = weightedGraph.numNodes();
        
        LOGGER.info("The number of nodes to be processed is: " + nodes.size());
        LOGGER.info("Collecting active nodes...");
        ArrayList<Integer> activeNodes = getActiveNodes(weightedGraph, nodes);
        LOGGER.info("Done.");
        LOGGER.info("The number of active nodes is: " + activeNodes.size());
        
        //create stoping criterion
        int maxIter = 30;
        double norm_delta_threhold = 1E-5;
        StoppingCriterion stpc = WeightedPageRank.or(new WeightedPageRank.NormDeltaStoppingCriterion(norm_delta_threhold), 
                new WeightedPageRank.IterationNumberStoppingCriterion(maxIter));
        
        return getSemanticSignature(weightedGraph, activeNodes, numThreads, alpha, beta, norm, stpc);
    }
    
    class PageRankSemSigThread extends Thread {
        public List<WeightedArc> arcList;
        private int threadId;
        private int startIndex;
        private int endIndex;
        private WeightedPageRank pagerank;
        private StoppingCriterion stpc;
        private ArrayList<Integer> activeNodes;
        
        public PageRankSemSigThread(int threadId, int startIndex, int endIndex, WeightedPageRank pagerank, StoppingCriterion stpc, ArrayList<Integer> activeNodes) {
            arcList = new LinkedList<WeightedArc>();
            this.threadId = threadId;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.pagerank = pagerank;
            this.stpc = stpc;
            this.activeNodes = activeNodes;
        }
        
        public void run() {
            pagerank.preference = DoubleArrayList.wrap(new double[numNodes]);
            for(int i = startIndex, num = 1; i < endIndex; ++i, ++num) {
                if(num % 10000 == 0) {
                    currentNum += 10000;
                    LOGGER.info("Complete " + currentNum + " nodes");
                }
                
                int index = activeNodes.get(i);
                LOGGER.info("thread " + threadId + " is processing node: " + index);
                pagerank.preference.set(index, 1.0);
                buildSemanticSignatureOfNode(pagerank, stpc, index, arcList);
                pagerank.preference.set(index, 0.0);
            }
        }
    }

}
