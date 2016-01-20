package edvisees.edl2015.weights;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import es.yrbcn.graph.weighted.WeightedArc;
import es.yrbcn.graph.weighted.WeightedBVGraph;
import es.yrbcn.graph.weighted.WeightedPageRank;
import es.yrbcn.graph.weighted.WeightedPageRankWithConnectedTerm;
import es.yrbcn.graph.weighted.WeightedPageRank.StoppingCriterion;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.webgraph.labelling.ArcLabelledImmutableGraph;
import it.unimi.dsi.webgraph.labelling.ArcLabelledNodeIterator.LabelledArcIterator;

public class ApproxPageRankSemanticSignatureBuilder extends PageRankSemanticSignatureBuilder {
    
    public ApproxPageRankSemanticSignatureBuilder() {
        super();
    }
    
    private ArcLabelledImmutableGraph buildApproxGraph(int rootNode, ArcLabelledImmutableGraph weightedGraph, ArrayList<Integer> nodes) {
//        LOGGER.info("building approx graph");
        double thres = threshold * 0.001;
        double[] dist = new double[numNodes];
        boolean[] visit = new boolean[numNodes];
        Map<Integer, Integer> indexMap = new HashMap<Integer, Integer>();
        
        dist[rootNode] = 1.0;
        nodes.add(rootNode);
        indexMap.put(rootNode, 0);
        int s, e;
        for(s = 0, e = 1; s < e; ++s) {
            int outdegree = weightedGraph.outdegree(nodes.get(s));
            LabelledArcIterator succIter = weightedGraph.successors(nodes.get(s));
            int j = outdegree;
            while(j-- != 0) {
                int succ = succIter.nextInt();
                if(visit[succ]) {
                    if(dist[succ] < dist[nodes.get(s)] * succIter.label().getFloat()) {
                        dist[succ] = dist[nodes.get(s)] * succIter.label().getFloat();
                    }
                }
                else {
                    if(thres < dist[nodes.get(s)] * succIter.label().getFloat()) {
                        dist[succ] = dist[nodes.get(s)] * succIter.label().getFloat();
                        visit[succ] = true;
                        nodes.add(succ);
                        indexMap.put(succ, e++);
                    }
                }
            }
        }
        
        int numNodes = nodes.size();
        float[] sumWeights = new float[numNodes];
        ArrayList<WeightedArc> arcList = new ArrayList<WeightedArc>();
        for(int i = 0; i < numNodes; ++i) {
            int from = nodes.get(i);
            int outdegree = weightedGraph.outdegree(from);
            LabelledArcIterator succIter = weightedGraph.successors(from);
            int j = outdegree;
            while(j-- != 0) {
                int succ = succIter.nextInt();
                if(indexMap.containsKey(succ)) {
                    arcList.add(new WeightedArc(i, indexMap.get(succ), succIter.label().getFloat()));
                    sumWeights[i] += succIter.label().getFloat();
                }
            }
        }
        
        for(WeightedArc arc : arcList) {
            arc.weight = arc.weight / sumWeights[arc.src];
        }
        
        WeightedArc[] arcs = new WeightedArc[arcList.size()];
        arcList.toArray(arcs);
//        LOGGER.info("approx graph: " + numNodes + " nodes " + arcList.size() + " edges");
        return new WeightedBVGraph(arcs, numNodes);
    }
    
    protected void buildSemanticSignatureOfNode(WeightedPageRank pagerank, StoppingCriterion stpc, int index, List<WeightedArc> arcList, ArcLabelledImmutableGraph originalGraph) {
        try {
            ArrayList<Integer> nodes = new ArrayList<Integer>();
            ArcLabelledImmutableGraph approxGraph = buildApproxGraph(index, originalGraph, nodes);
            pagerank.resetGraph(approxGraph);
            pagerank.preference = DoubleArrayList.wrap(new double[approxGraph.numNodes()]);
            pagerank.preference.set(0, 1.0);
            pagerank.stepUntil(stpc);
            pagerank.preference.set(0, 0.0);
            for(int j = 1; j < approxGraph.numNodes(); ++j) {
                if(pagerank.rank[j] > threshold) {
                    arcList.add(new WeightedArc(index, nodes.get(j), (float) pagerank.rank[j]));
//                    System.out.println(index + "-->" + nodes.get(j) + ": " + (float) pagerank.rank[j]);
                }
            }
//            LOGGER.info("size: " + arcList.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    protected ArcLabelledImmutableGraph getSemanticSignature(ArcLabelledImmutableGraph weightedGraph, ArrayList<Integer> activeNodes, int numThreads, double alpha, double beta, String norm, StoppingCriterion stpc) {
        int numActiveNodes = activeNodes.size();
        LOGGER.info("Initializing Threads...");
        int range = (numActiveNodes + numThreads - 1) / numThreads;
        ApproxPageRankSemSigThread[] threads = new ApproxPageRankSemSigThread[numThreads];
        for(int i = 0; i < numThreads; ++i) {
            ArcLabelledImmutableGraph originalGraph = i == 0 ? weightedGraph : weightedGraph.copy();
            WeightedPageRank pagerank = new WeightedPageRankWithConnectedTerm(originalGraph, alpha, beta, norm, false);
            threads[i] = new ApproxPageRankSemSigThread(i, i * range, Math.min((i + 1) * range, numActiveNodes), pagerank, stpc, activeNodes, originalGraph);
        }
        currentNum = 0;
        
        LOGGER.info("Building semantic signatures...");
        try {
            for(ApproxPageRankSemSigThread thread : threads) {
                thread.start();
            }
            
            for(ApproxPageRankSemSigThread thread : threads) {
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
    
    class ApproxPageRankSemSigThread extends Thread {
        public List<WeightedArc> arcList;
        private int threadId;
        private int startIndex;
        private int endIndex;
        private WeightedPageRank pagerank;
        private ArcLabelledImmutableGraph originalGraph;
        private StoppingCriterion stpc;
        private ArrayList<Integer> activeNodes;
        
        public ApproxPageRankSemSigThread(int threadId, int startIndex, int endIndex, WeightedPageRank pagerank, StoppingCriterion stpc, ArrayList<Integer> activeNodes, ArcLabelledImmutableGraph originalGraph) {
            arcList = new LinkedList<WeightedArc>();
            this.threadId = threadId;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.pagerank = pagerank;
            this.stpc = stpc;
            this.activeNodes = activeNodes;
            this.originalGraph = originalGraph;
        }
        
        public void run() {
            for(int i = startIndex, num = 1; i < endIndex; ++i, ++num) {
                if(num % 10000 == 0) {
                    currentNum += 10000;
                    LOGGER.info("Complete " + currentNum + " nodes");
                }
                
                int index = activeNodes.get(i);
                LOGGER.info("thread " + threadId + " is processing node: " + index);
                buildSemanticSignatureOfNode(pagerank, stpc, index, arcList, originalGraph);
            }
        }
    }
}
