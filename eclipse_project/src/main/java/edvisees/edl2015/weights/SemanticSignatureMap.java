package edvisees.edl2015.weights;

import it.unimi.dsi.webgraph.labelling.ArcLabelledImmutableGraph;
import it.unimi.dsi.webgraph.labelling.ArcLabelledNodeIterator;
import it.unimi.dsi.webgraph.labelling.ArcLabelledNodeIterator.LabelledArcIterator;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * models a map:
 *         key: vertex (entity id)
 *         value: semantic signature of the vertex (a set of other entities)
 */
public class SemanticSignatureMap {
    private ArcLabelledImmutableGraph semSigGraph;

    public SemanticSignatureMap(ArcLabelledImmutableGraph semSigGraph) {
        this.semSigGraph = semSigGraph;
    }

    public Map<Integer, Float> getSignature(String nodeIndexStr) {
        return this.getSignature(Integer.parseInt(nodeIndexStr));
    }

    public Map<Integer, Float> getSignature(int nodeIndex) {
        Map<Integer, Float> signature = new HashMap<Integer, Float>();
        LabelledArcIterator signatureIter = this.semSigGraph.successors(nodeIndex);
        for (int signatureNodeIndex = signatureIter.nextInt(); signatureNodeIndex > -1; signatureNodeIndex = signatureIter.nextInt()) {
            signature.put(signatureNodeIndex, signatureIter.label().getFloat());
        }
        return signature;
    }

    public SortedMap<Integer, Float> getSortedSignature(int nodeIndex) {
        Map<Integer, Float> signature = getSignature(nodeIndex);
        TreeMap<Integer, Float> sortedSignature = new TreeMap<Integer, Float>(new ValueComparator(signature));
        sortedSignature.putAll(signature);
        return sortedSignature;
    }

    public ArcLabelledNodeIterator getNodeIterator() {
        return this.semSigGraph.nodeIterator();
    }
}


class ValueComparator implements Comparator<Integer> {
    Map<Integer, Float> base;

    public ValueComparator(Map<Integer, Float> base) {
        this.base = base;
    }

    public int compare(Integer a, Integer b) {
        if (this.base.get(a) >= this.base.get(b)) {
            return -1;
        } else {
            return 1;
        }
    }
}
