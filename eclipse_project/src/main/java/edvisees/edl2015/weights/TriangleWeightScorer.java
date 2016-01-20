package edvisees.edl2015.weights;

import it.unimi.dsi.webgraph.BVGraph;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;
import it.unimi.dsi.webgraph.labelling.ArcLabelledImmutableGraph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import es.yrbcn.graph.weighted.WeightedArc;
import es.yrbcn.graph.weighted.WeightedBVGraph;

public class TriangleWeightScorer extends WeightScorer {
    private static int currentNum = 0;

    public TriangleWeightScorer() {
        super();
    }
    
    protected int calcWeightOfNode(BVGraph graph, ImmutableGraph graph_transpose, int index, int[][] arcTriples, int[] sumoutWeight, int entry) {
        Set<Integer> antecSet = getAntecSet(graph_transpose.successors(index));
        
        LazyIntIterator succIter = graph.successors(index);
        int succ;
        int trii = entry;
        while((succ = succIter.nextInt()) != -1) {
            int triangle = getNumOfTriangle(antecSet, graph.successors(succ), succ) + 1;
            arcTriples[trii][0] = index;
            arcTriples[trii][1] = succ;
            arcTriples[trii][2] = triangle;
            sumoutWeight[index] += triangle;
            trii++;
        }
        return trii;
    }
    
    private Set<Integer> getAntecSet(LazyIntIterator antecIter) {
        Set<Integer> set = new HashSet<Integer>();
        int antec = antecIter.nextInt();
        while(antec != -1) {
            set.add(antec);
            antec = antecIter.nextInt();
        }
        return set;
    }
    
    private int getNumOfTriangle(Set<Integer> set, LazyIntIterator succIter, int succ) {
        int res = set.contains(succ) ? 1 : 0;
        int succOfSucc;
        while((succOfSucc = succIter.nextInt()) != -1) {
            if(set.contains(succOfSucc)) {
                res++;
            }
        }
        return res;
    }

    private int[] calcOffsets(BVGraph graph, int numThreads) {
        int[] offsets = new int[numThreads];
        int numNodes = graph.numNodes();
        int range = (numNodes + numThreads - 1) / numThreads;
        int offset = 0;
        for(int i = 0; i < numNodes; ++i) {
            int threadId = i / range;
            if(threadId + 1 == numThreads) {
                break;
            }
            offset += graph.outdegree(i);
            if(threadId + 1 < numThreads) {
                offsets[threadId + 1] = offset;
            }
        }
        return offsets;
    }
    
    protected void checkArcWeights(int[][] arcTriples, int[] sumoutWeight, int numNodes) {
        int[] sw = new int[numNodes];
        for(int j = 0; j < arcTriples.length; ++j) {
            sw[arcTriples[j][0]] += arcTriples[j][2];
        }
        
        for(int j = 0; j < numNodes; ++j) {
            if(sw[j] != sumoutWeight[j]) {
                throw new RuntimeException("node " + j + "'s out weight " + sw[j] + " not equal to original weight: " + sumoutWeight[j]);
            }
        }
    }

    private WeightedArc[] removeZeroEdges(int[][] arcTriples, int[] sumoutWeight) {
        ArrayList<WeightedArc> arcList = new ArrayList<WeightedArc>();
        for(int i = 0; i < arcTriples.length; ++i) {
            if(arcTriples[i][2] > 0) {
                arcList.add(new WeightedArc(arcTriples[i][0], arcTriples[i][1], ((float) arcTriples[i][2]) / sumoutWeight[arcTriples[i][0]]));
            }
        }
        WeightedArc[] res = new WeightedArc[arcList.size()];
        arcList.toArray(res);
        return res;
    }
    
    @Override
    public ArcLabelledImmutableGraph calcWeights(BVGraph graph, BVGraph graph_transpose, int numThreads) {
        System.out.print("Calculating offsets...");
        int[] offsets = calcOffsets(graph, numThreads);
        System.out.println("Done.");
        System.out.println();
        int numNodes = graph.numNodes();
        int[][] arcTriples = new int[(int) graph.numArcs()][3];
        int[] sumoutWeight = new int[numNodes];
        
        int range = (numNodes + numThreads - 1) / numThreads;
        CalcTriangleThread[] threads = new CalcTriangleThread[numThreads];
        for(int i = 0; i < numThreads; ++i) {
            threads[i] = new CalcTriangleThread(i, i * range, Math.min((i + 1) * range, numNodes), offsets[i], 
                    arcTriples, sumoutWeight, i == 0 ? graph : graph.copy(), i == 0 ? graph_transpose : graph_transpose.copy());
        }
        
        System.out.println("Calculating triangles...");
        currentNum = 0;
        try {
            for(CalcTriangleThread thread : threads) {
                thread.start();
            }
            
            for(CalcTriangleThread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Done.");
        System.out.print("Checking arc weights...");
        checkArcWeights(arcTriples, sumoutWeight, numNodes);
        System.out.println("Done.");
        System.out.print("Removing arcs with zero weight...");
        WeightedArc[] arcsWOZero = removeZeroEdges(arcTriples, sumoutWeight);
        System.out.println("Done.");
        System.out.println("Number of arcs in weighted graph: " + arcsWOZero.length);
        return new WeightedBVGraph(arcsWOZero, numNodes);
    }
    
    class CalcTriangleThread extends Thread {
        private int threadId;
        private int offset;
        private BVGraph graph;
        private ImmutableGraph graph_transpose;
        private int[][] arcTriples;
        private int[] sumoutWeight;
        private int startIndex;
        private int endIndex;
        
        public CalcTriangleThread(int threadId, int startIndex, int endIndex, int offset, int[][] arcTripe, int[] sumoutWeight, BVGraph graph, ImmutableGraph graph_transpose) {
            this.threadId = threadId;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.offset = offset;
            this.arcTriples = arcTripe;
            this.sumoutWeight = sumoutWeight;
            this.graph = graph;
            this.graph_transpose = graph_transpose;
        }
        
        public void run() {
            int entry = offset;
            for(int i = startIndex, num = 1; i < endIndex; ++i, ++num) {
                if(num % 10000 == 0) {
                    currentNum += 10000;
                    System.out.print(threadId + ": " + currentNum + ", ");
                }
                entry = calcWeightOfNode(graph, graph_transpose, i, arcTriples, sumoutWeight, entry);
            }
        }
     }
}
