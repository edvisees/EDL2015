package edvisees.edl2015.candidates;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


/**
 * a CandidateMeaning is a couple of a kb_entity (the meaning) and a text fragment
 */
public class CandidateMeaning implements Comparable<CandidateMeaning> {
    public String entityId; // v
    public Fragment fragment; // f
    public Map<CandidateMeaning, Float> successors = new HashMap<CandidateMeaning, Float>();
    public Map<CandidateMeaning, Float> predecessors = new HashMap<CandidateMeaning, Float>();

    public CandidateMeaning(String entityId, Fragment fragment) {
        this.entityId = entityId;
        this.fragment = fragment;
        this.fragment.meanings.add(this);
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CandidateMeaning)) return false;
        CandidateMeaning otherCandidateMeaning = (CandidateMeaning) obj;
        return this.entityId.equals(otherCandidateMeaning.entityId) && this.fragment.equals(otherCandidateMeaning.fragment);
    }

    public void addSuccessor(CandidateMeaning cm2, float weight) {
        this.successors.put(cm2, weight);
        cm2.addPredecessor(this, weight);
    }

    private void addPredecessor(CandidateMeaning cm2, float weight) {
        this.predecessors.put(cm2, weight);
    }

    public float getConnectivity() {
        float connectivity = (float) 0.0;
        for(Entry<CandidateMeaning, Float> entry : successors.entrySet()) {
            connectivity += entry.getValue();
        }
        for(Entry<CandidateMeaning, Float> entry : predecessors.entrySet()) {
            connectivity += entry.getValue();
        }
        return connectivity;
    }

    @Override
    public String toString() {
        return this.fragment + " -> " + this.entityId;
    }

    @Override
    public int compareTo(CandidateMeaning otherCm) {
        float delta = this.getScore() - otherCm.getScore();
        if (delta > 0) return -1;
        if (delta < 0) return 1;
        return 0;
    }

    public float getScore() {
        Set<Fragment> connectedFragments = new HashSet<Fragment>();
        for (CandidateMeaning successor : this.successors.keySet()) {
            connectedFragments.add(successor.fragment);
        }
        for (CandidateMeaning predecessor : this.predecessors.keySet()) {
            connectedFragments.add(predecessor.fragment);
        }
        return (float) connectedFragments.size() * this.getConnectivity();
    }
    
    public float getFinalScore() {
        float score = getScore();
        float sum_score = (float) 0.0;
        for(CandidateMeaning candidateMeaning : this.fragment.meanings) {
            sum_score += candidateMeaning.getScore();
        }
        return score / sum_score;
    }
}
