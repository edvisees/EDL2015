package edvisees.edl2015.weights;

import it.unimi.dsi.webgraph.BVGraph;
import it.unimi.dsi.webgraph.labelling.ArcLabelledImmutableGraph;

public abstract class WeightScorer {

    public WeightScorer() {
        
    }
    
    public static WeightScorer createWeightScorer(String scorerClassName) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return (WeightScorer) Class.forName(WeightScorer.class.getPackage().getName() + "." + scorerClassName).newInstance();
    }
    
    public abstract ArcLabelledImmutableGraph calcWeights(BVGraph graph, BVGraph graph_transpose, int numThreads);
}
