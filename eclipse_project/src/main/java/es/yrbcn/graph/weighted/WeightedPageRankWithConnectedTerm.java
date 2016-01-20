package es.yrbcn.graph.weighted;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edvisees.edl2015.graph.WeightedGraphBuilder;
import it.unimi.dsi.fastutil.io.FastBufferedOutputStream;
import it.unimi.dsi.law.Util;
import it.unimi.dsi.webgraph.labelling.ArcLabelledImmutableGraph;
import it.unimi.dsi.webgraph.labelling.ArcLabelledNodeIterator;
import it.unimi.dsi.webgraph.labelling.ArcLabelledNodeIterator.LabelledArcIterator;

/**
 * Computes PageRank using the Power Method and adds the connected term to make sure the graph is fully connected.
 * @author max
 *
 */
public class WeightedPageRankWithConnectedTerm extends WeightedPageRankPowerMethod {
    
    /** The default connected factor. */
    public final static double DEFAULT_BETA = 0.9;
    
    public double beta = DEFAULT_BETA;
    
    private final static Logger LOGGER = LoggerFactory.getLogger( WeightedPageRankWithConnectedTerm.class );
    
    protected final boolean useDanglingNodes;
    
    protected double[] preVals;
    
    public WeightedPageRankWithConnectedTerm(final ArcLabelledImmutableGraph g, Logger logger, boolean useDanglingNodes) {
        super(g, logger);
        this.useDanglingNodes = useDanglingNodes;
        if(!useDanglingNodes) {
            this.preVals = new double[g.numNodes()];
        }
    }
    
    public WeightedPageRankWithConnectedTerm(final ArcLabelledImmutableGraph g, boolean useDanglingNodes) {
        this(g, LOGGER, useDanglingNodes);
    }
    
    public WeightedPageRankWithConnectedTerm(final ArcLabelledImmutableGraph graph, double alpha, double beta, String norm, boolean useDanglingNodes) {
        this(graph, useDanglingNodes);
        this.alpha = alpha;
        this.beta = beta;
        this.stronglyPreferential = false;
        this.norm = WeightedPageRank.Norm.valueOf(norm);
    }
    
    @Override
    public void resetGraph(final ArcLabelledImmutableGraph g) {
        super.resetGraph(g);
        if(!useDanglingNodes) {
            this.preVals = new double[g.numNodes()];
        }
    }
    
    protected void stepWithDanglingNodes() throws IOException {
        double[] oldRank = rank, newRank = previousRank;
        java.util.Arrays.fill(newRank, 0.0);
        iterationNumber++;
        
        // for each node, calculate its outdegree and redistribute its rank among pointed nodes
        double accum_dangling = 0.0;
        double accum_connected = 0.0;
        
        final ArcLabelledNodeIterator nodeIterator = g.nodeIterator();
        int i, outdegree, j, n = numNodes;
        
        while(n-- != 0) {
            i = nodeIterator.nextInt();
            outdegree = nodeIterator.outdegree();
            
            accum_connected += oldRank[i];
            if (outdegree == 0 || buckets != null && buckets.get( i )) {
                accum_dangling += oldRank[ i ];
            }
            else {
                j = outdegree;
                LabelledArcIterator succIter = nodeIterator.successors();
                while(j-- != 0) {
                    int succ = succIter.nextInt();
                    newRank[succ] += (oldRank[i] * succIter.label().getFloat());
                }
            }
        }
        
        final double accum_danglingOverNumNodes = accum_dangling / numNodes;
        final double accum_connectedOverNumNodes = accum_connected / numNodes;
        
        final double oneOverNumNodes = 1.0 / numNodes;
        if (preference != null) {
            if(preferentialAdjustment == null) {
                for(i = numNodes; i-- != 0;) {
                    newRank[i] = alpha * beta * newRank[i] + alpha * beta * accum_danglingOverNumNodes 
                            + alpha * (1 - beta) * accum_connectedOverNumNodes 
                            + (1 - alpha) * preference.getDouble(i);
                }
            }
            else {
                for(i = numNodes; i-- != 0;) {
                    newRank[i] = alpha * beta * newRank[i] + alpha * beta * accum_dangling * preferentialAdjustment.getDouble(i) 
                            + alpha * (1 - beta) * accum_connectedOverNumNodes
                            + ( 1 - alpha ) * preference.getDouble( i );
                }
            }
        }
        else {
            if(preferentialAdjustment == null) {
                for(i = numNodes; i-- != 0;) {
                    newRank[i] = alpha * beta * newRank[i] + alpha * beta * accum_danglingOverNumNodes 
                            + alpha * (1 - beta) * accum_connectedOverNumNodes
                            + ( 1 - alpha ) * oneOverNumNodes;
                }
            }
            else {
                for(i = numNodes; i-- != 0;) {
                    newRank[i] = alpha * beta * newRank[ i ] + alpha * beta * accum_dangling * preferentialAdjustment.getDouble(i) 
                            + alpha * (1 - beta) * accum_connectedOverNumNodes
                            + ( 1 - alpha ) * oneOverNumNodes;
                }
            }
        }
        //make the rank just computed the new rank
        rank = newRank;
        previousRank = oldRank;

        // Compute derivatives.
        n = iterationNumber;

        if ( subset == null ) {
            for( i = 0; i < order.length; i++ ) {
                final int k = order[ i ];
                final double alphak = Math.pow( alpha, k );
                final double nFallingK = Util.falling( n, k );
                for( j = 0; j < numNodes; j++ ) derivative[ i ][ j ] += nFallingK * ( rank[ j ] - previousRank[ j ] ) / alphak;
            }
        }
        else {
            for( i = 0; i < order.length; i++ ) {
                final int k = order[ i ];
                final double alphak = Math.pow( alpha, k );
                final double nFallingK = Util.falling( n, k );

                for( int t: subset ) derivative[ i ][ t ] += nFallingK * ( rank[ t ] - previousRank[ t ] ) / alphak;
            }
        }
        
        // Compute coefficients, if required.

        if ( coeffBasename != null ) { 
            final DataOutputStream coefficients = new DataOutputStream( new FastBufferedOutputStream( new FileOutputStream( coeffBasename + "-" + ( iterationNumber ) ) ) );
            final double alphaN = Math.pow( alpha, n );
            for( i = 0; i < numNodes; i++ ) coefficients.writeDouble( ( rank[ i ] - previousRank[ i ] ) / alphaN );
            coefficients.close();           
        }
    }
    
    protected void stepWODanglingNodes() throws IOException {
        double[] oldRank = rank, newRank = previousRank;
        System.arraycopy(preVals, 0, newRank, 0, numNodes);
//        LOGGER.info("Iter: " + iterationNumber);
        iterationNumber++;
        
        // for each node, calculate its outdegree and redistribute its rank among pointed nodes
        final ArcLabelledNodeIterator nodeIterator = g.nodeIterator();
        int i, succ, n = numNodes;
        double alpha_by_beta = alpha * beta;
        
        while(n-- != 0) {
            i = nodeIterator.nextInt();
            LabelledArcIterator succIter = nodeIterator.successors();
            while((succ = succIter.nextInt()) != -1) {
                newRank[succ] += alpha_by_beta * oldRank[i] * succIter.label().getFloat();
            }
        }
        
        //make the rank just computed the new rank
        rank = newRank;
        previousRank = oldRank;
    }
    
    /** Computes the next step of the Power Method and connected term.
     */
    @Override
    public void step() throws IOException {
        if(useDanglingNodes) {
            stepWithDanglingNodes();
        }
        else {
            stepWODanglingNodes();
        }
    }
    
    @Override
    protected void reset() throws IOException {
        super.reset();
        if(useDanglingNodes) {
            return;
        }
        //set preVals (only for the case not using dangling nodes
        int i;
        final double oneOverNumNodes = 1.0 / numNodes;
        if (preference != null) {
            for(i = numNodes; i-- != 0;) {
                preVals[i] = alpha * (1 - beta) * oneOverNumNodes + (1 - alpha) * preference.getDouble(i);
            }
        }
        else {
            for(i = numNodes; i-- != 0;) {
                preVals[i] = (1 - alpha * beta) * oneOverNumNodes;
            }
        }
    }
    
    @Override
    public void displayRank(PrintStream printer, int srcIndex) {
        for(int i = 0; i < g.numNodes(); ++i) {
            System.out.println(srcIndex + "--->" + i + " rank: " + rank[i]);
        }
        System.out.println("------------------------");
    }
    
    public static void main(String[] args) throws IOException {
        String weightedGraphBaseName = args[0];
        ArcLabelledImmutableGraph wGraph = WeightedGraphBuilder.deserializeWeightedGraph(weightedGraphBaseName);
        double alpha = 1, beta = 0.9;
        String norm = "INFTY";
        WeightedPageRank pagerank = new WeightedPageRankWithConnectedTerm(wGraph, alpha, beta, norm, false);
        
        //create stoping criterion
        int maxIter = 100;
        double norm_delta_threhold = 1E-7;
        StoppingCriterion stpc = WeightedPageRank.or(new WeightedPageRank.NormDeltaStoppingCriterion(norm_delta_threhold), 
                new WeightedPageRank.IterationNumberStoppingCriterion(maxIter));
        
        pagerank.stepUntil(stpc);
        
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(args[1])), "UTF-8"));
        for(int i = 0; i < wGraph.numNodes(); ++i) {
            writer.write(i + "\t" + pagerank.rank[i]);
            writer.newLine();
        }
        writer.close();
    }
}
