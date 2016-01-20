package edvisees.edl2015.disambiguate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import edvisees.edl2015.candidates.CandidateMeaning;
import edvisees.edl2015.candidates.Fragment;
import edvisees.edl2015.lucene.Searcher;
import edvisees.edl2015.weights.SemanticSignatureMap;

public class SemanticInterpretationGraph {
    public List<CandidateMeaning> v; // vertices
    public SortedSet<Fragment> fragments; // the order is important
    
    private double connectivity = 0.0;

    private SemanticInterpretationGraph(SortedSet<Fragment> fragments, List<CandidateMeaning> v) {
        this.fragments = fragments;
        this.v = v; // it is really included in fragments / linked from
    }

    /**
     * From the set of all the candidate fragments with their possible meanings, build a SemanticInterpretationGraph.
     * @param preliminaryGraph all the candidate fragments of the input text.
     * @param semanticSignatureMap key = nodeIndex (int, 0 based); value: Set<Integer> nodeIndexes of the signature of the given node
     * @return
     * @throws Exception
     */
    public static SemanticInterpretationGraph buildGraph(
            List<Fragment> preliminaryGraph,
            SemanticSignatureMap semanticSignatureMap,
            Searcher badIdsSearcher
    ) throws Exception {
        // get all the vertices from the semantic signature Map
        Map<String, Set<CandidateMeaning>> eid2MeaningSetMap = new HashMap<String, Set<CandidateMeaning>>();
        Set<CandidateMeaning> allCandidateMeanings = new HashSet<CandidateMeaning>();
        for (Fragment fragmentCandidate : preliminaryGraph) {
            for(CandidateMeaning candidateMeaning : fragmentCandidate.meanings) {
                if(!eid2MeaningSetMap.containsKey(candidateMeaning.entityId)) {
                    eid2MeaningSetMap.put(candidateMeaning.entityId, new HashSet<CandidateMeaning>());
                }
                eid2MeaningSetMap.get(candidateMeaning.entityId).add(candidateMeaning);
            }
            allCandidateMeanings.addAll(fragmentCandidate.meanings);
        }
        System.out.println("size of CandidateMeanings = " + allCandidateMeanings.size());
        // link them with edges and store connected vertices
        // get the "real" vertices, the subset of allCandidateMeanings that were actually connected in the graph.
        Set<CandidateMeaning> connectedVertices = new HashSet<CandidateMeaning>();
        long time = System.currentTimeMillis();
        for(Entry<String, Set<CandidateMeaning>> entry1 : eid2MeaningSetMap.entrySet()) {
            String eid1 = entry1.getKey();
            Map<Integer, Float> signature1 = semanticSignatureMap.getSignature(eid1);
            for(String eid2 : eid2MeaningSetMap.keySet()) {
                if(signature1.containsKey(Integer.parseInt(eid2))) {
                    for(CandidateMeaning cm1 : entry1.getValue()) {
                        for(CandidateMeaning cm2 : eid2MeaningSetMap.get(eid2)) {
                            if (!cm1.fragment.equals(cm2.fragment)) {
                                connectedVertices.add(cm1);
                                connectedVertices.add(cm2);
                                cm1.addSuccessor(cm2, semanticSignatureMap.getSignature(cm1.entityId).get(Integer.parseInt(cm2.entityId)));
                            }
                        }
                    }
                }
            }
        }
        
        removeDisconnectedMeanings(allCandidateMeanings, connectedVertices);
        
        time = System.currentTimeMillis() - time;
        System.out.println(String.format("Linking finished; %d connected vertices; took %.2f minutes.", connectedVertices.size(), time / 1000 / 60.0));
        List<CandidateMeaning> vI = new ArrayList<CandidateMeaning>(connectedVertices);
        SortedSet<Fragment> candidateFragments = new TreeSet<Fragment>(preliminaryGraph);
        return new SemanticInterpretationGraph(candidateFragments, vI).getSumConnectivity();
    }
    
    private static void removeDisconnectedMeanings(Set<CandidateMeaning> allCandidateMeanings, Set<CandidateMeaning> connectedMeanings) {
        for(CandidateMeaning candinateMeaning : allCandidateMeanings) {
            if(!connectedMeanings.contains(candinateMeaning)) {
                candinateMeaning.fragment.meanings.remove(candinateMeaning);
            }
        }
    }

    public SemanticInterpretationGraph densify(int ambiguityLevel) {
        Map<Fragment, Fragment> fragmentMap = new HashMap<Fragment, Fragment>();
        Map<CandidateMeaning, CandidateMeaning> candidateMeaningMap = new HashMap<CandidateMeaning, CandidateMeaning>();
        long time = System.currentTimeMillis();
        SemanticInterpretationGraph GIstar = this.clone(fragmentMap, candidateMeaningMap);
        time = System.currentTimeMillis() - time;
        System.out.println(String.format("Clone initial graph took %.2f seconds.", time / 1000.0));
        
        log("\nDensifying!");
        time = System.currentTimeMillis();
        
        long clone_graph_time = 0;
        Set<CandidateMeaning> discardedVertexes = new HashSet<CandidateMeaning>();
        Set<Fragment> fMaxs = new HashSet<Fragment>();
        while (true) {
            fMaxs.clear();
            int fMaxSize = this.getMostAmbiguousFragment(fMaxs);
            //log("Densifying, most ambiguous fragment is: '" + fMax.text + "' (" + fMaxSize + ")");
            if (fMaxSize <= ambiguityLevel) {
                break;
            }
            // discard the weakest interpretation of fMax (remove vertex and edges)
            Set<CandidateMeaning> weakestMeanings = this.getWorstInterpretation(fMaxs);
            for(CandidateMeaning weakestMeaning : weakestMeanings) {
                this.discardVertex(weakestMeaning);
                discardedVertexes.add(weakestMeaning);
                //log("this degree = " + this.getDegree() + ", GIStar degree = " + GIstar.getDegree());
                if (this.getDensity() > GIstar.getDensity()) {
                    long clone_time = System.currentTimeMillis();
                    for(CandidateMeaning discardedVertex : discardedVertexes) {
                        GIstar.fakeClone(candidateMeaningMap.get(discardedVertex));
                    }
                    discardedVertexes.clear();
                    clone_graph_time += System.currentTimeMillis() - clone_time;
                }
            }
        }
        time = System.currentTimeMillis() - time;
        System.out.println(String.format("Densify graph took %.2f seconds, clone graph took %.2f seconds", time / 1000.0, clone_graph_time / 1000.0));
        return GIstar;
    }

    private static void log(String msg) {
        System.out.println(msg + "\n");
    }

    private void discardVertex(CandidateMeaning candidateMeaning) {
        // update connectivity
        connectivity -= 2 * candidateMeaning.getConnectivity();
        // discard the node
        Set<CandidateMeaning> disconnectedNodes = removeVertex(candidateMeaning, true);
        // remove disconnected nodes
        for(CandidateMeaning disconnectedNode : disconnectedNodes) {
            removeVertex(disconnectedNode, false);
        }
    }
    
    private Set<CandidateMeaning> removeVertex(CandidateMeaning candidateMeaning, boolean checkDisconnect) {
        this.v.remove(candidateMeaning);
        // the given node is still a predecessor and successor, remove those
        for (CandidateMeaning predecessor : candidateMeaning.predecessors.keySet()) {
            predecessor.successors.remove(candidateMeaning);
        }
        for (CandidateMeaning successor : candidateMeaning.successors.keySet()) {
            successor.predecessors.remove(candidateMeaning);
        }
        
        candidateMeaning.fragment.meanings.remove(candidateMeaning);
        
        if(checkDisconnect) {
            double eps = 1e-7;
            Set<CandidateMeaning> removed = new HashSet<CandidateMeaning>();
            for (CandidateMeaning predecessor : candidateMeaning.predecessors.keySet()) {
                if(predecessor.getConnectivity() < eps) {
                    removed.add(predecessor);
                }
            }
            for (CandidateMeaning successor : candidateMeaning.successors.keySet()) {
                if(successor.getConnectivity() < eps) {
                    removed.add(successor);
                }
            }
            return removed;
        }
        else {
            return null;
        }
    }

    
    @SuppressWarnings("unused")
    private List<CandidateMeaning> checkGraphConnectivity() {
        double eps = 1e-7;
        List<CandidateMeaning> removed = new ArrayList<CandidateMeaning>();
        for (CandidateMeaning cm : this.v) {
            if (cm.getConnectivity() < eps) {
                removed.add(cm);
            }
        }
        return removed;
    }
    
    protected void fakeClone(CandidateMeaning candidateMeaning) {
        this.discardVertex(candidateMeaning);
    }

    protected SemanticInterpretationGraph clone(Map<Fragment, Fragment> fragmentMap, Map<CandidateMeaning, CandidateMeaning> candidateMeaningMap) {
        // clone fragments
        SortedSet<Fragment> fragments_clone = new TreeSet<Fragment>();
        for(Fragment fragment : this.fragments) {
            Fragment fragment_clone = fragment.clone();
            fragmentMap.put(fragment, fragment_clone);
            fragments_clone.add(fragment_clone);
//            System.out.println("frag: " + fragment.toString() + " frag clone " + fragment_clone);
        }
        // clone candidate meanings
        List<CandidateMeaning> vI_clone = new ArrayList<CandidateMeaning>();
        for(CandidateMeaning candidateMeaning : this.v) {
            Fragment fragment_clone = fragmentMap.get(candidateMeaning.fragment);
//            System.out.println("\t frag: " + candidateMeaning.fragment.toString());
//            System.out.println("\tclone frag: " + fragment_clone.toString());
            CandidateMeaning candidateMeaning_clone = new CandidateMeaning(candidateMeaning.entityId, fragment_clone);
            candidateMeaningMap.put(candidateMeaning, candidateMeaning_clone);
            vI_clone.add(candidateMeaning_clone);
        }
        // copy edges
        for(CandidateMeaning candidateMeaning : this.v) {
            CandidateMeaning candidateMeaning_clone = candidateMeaningMap.get(candidateMeaning);
            for(Entry<CandidateMeaning, Float> entry : candidateMeaning.successors.entrySet()) {
                CandidateMeaning succ_clone = candidateMeaningMap.get(entry.getKey());
                candidateMeaning_clone.addSuccessor(succ_clone, entry.getValue());
            }
        }
        return new SemanticInterpretationGraph(fragments_clone, vI_clone).getSumConnectivity();
    }

    private int getMostAmbiguousFragment(Set<Fragment> fMaxs) {
        if(this.fragments.isEmpty()) {
            return -1;
        }
        int fMaxDegree = -1;
        for (Fragment candidateFragment : this.fragments) {
            if (candidateFragment.meanings.size() > fMaxDegree) {
                fMaxs.clear();
                fMaxs.add(candidateFragment);
                fMaxDegree = candidateFragment.meanings.size();
            }
            else if(candidateFragment.meanings.size() == fMaxDegree) {
                fMaxs.add(candidateFragment);
            }
        }
        return fMaxDegree;
    }

    private SemanticInterpretationGraph getSumConnectivity() {
        double sumConnectivity = 0.0;
        for (CandidateMeaning cm : this.v) {
            sumConnectivity += cm.getConnectivity();
        }
        this.connectivity = sumConnectivity;
        return this;
    }

    private double getDensity() {
        //log("e.size() = " + numEdges + ", v.size() = " + this.v.size());
        return connectivity / this.v.size();
    }

    public Map<Fragment, String> chooseBestMeanings(float theta) {
        // get density
        double density = this.getDensity();
        
        // finally, get the best interpretation for each fragment.
        Map<Fragment, String> selected = new HashMap<Fragment, String>();
        // for each fragment in graph.v:
        String NIL = "NIL";
        for (Fragment candidateFragment : this.fragments) {
            if(candidateFragment.isNIL) {
                selected.put(candidateFragment, NIL);
                log("NIL: " + candidateFragment.text + " -> " + selected.get(candidateFragment));
            }
            else {
                CandidateMeaning bestMeaning = this.getBestInterpretation(candidateFragment);
                if(bestMeaning == null) {
                    // TODO fragment is disconnected in the final densest graph (NIL or other?)
                    selected.put(candidateFragment, NIL);
                    log("disconnected: " + candidateFragment.text + " -> " + selected.get(candidateFragment));
                }
                else {
                    float bestMeaningScore = bestMeaning.getFinalScore();
                    float bestScore = bestMeaning.getScore();
                    //log("\t" + bestMeaning.fragment.text + " = " + bestMeaning.entityId + " -> score = " + bestMeaningScore);
                    if (bestMeaningScore >= theta && bestScore >= density) {
                        candidateFragment.finalScore = bestMeaningScore;
                        selected.put(candidateFragment, bestMeaning.entityId);
                        log("best meaning: " + candidateFragment.text + " -> " + selected.get(candidateFragment));
                    }
                    else {
                        // TODO best meaning score is below the threshold (NIL or other?)
                        selected.put(candidateFragment, NIL);
                        log("small score: " + candidateFragment.text + " -> " + selected.get(candidateFragment));
                    }
                }
            }
        }
        return selected;
    }

    private CandidateMeaning getBestInterpretation(Fragment fragment) {
        return this.getTopInterpretation(fragment, true);
    }

    private Set<CandidateMeaning> getWorstInterpretation(Set<Fragment> fMaxs) {
        Set<CandidateMeaning> worsts = new HashSet<CandidateMeaning>();
        for(Fragment fragment : fMaxs) {
            worsts.add(this.getTopInterpretation(fragment, false));
        }
        return worsts;
    }

    private CandidateMeaning getTopInterpretation(Fragment fragment, boolean best) {
        if(fragment.meanings.isEmpty()) {
            return null;
        }
        
        CandidateMeaning topInterpretation = fragment.meanings.iterator().next();
        for (CandidateMeaning candidateMeaning : fragment.meanings) {
            float delta = topInterpretation.getScore() - candidateMeaning.getScore(); // negative means cand > top
            if ((best && delta < 0) || (!best && delta > 0)) {
                topInterpretation = candidateMeaning;
            }
        }
        return topInterpretation;
    }

    // iterate every fragment and show all its meanings
    public void debugMeanings(int topN) {
        double density = this.getDensity();
        for (Fragment candidateFragment : this.fragments) {
            if(candidateFragment.meanings.isEmpty()) {
                log("\t" + candidateFragment.text + " is disconnected");
                continue;
            }
            SortedSet<CandidateMeaning> sortedMeanings = new TreeSet<CandidateMeaning>(candidateFragment.meanings);
            Iterator<CandidateMeaning> iter = sortedMeanings.iterator();
            for (int i=0; i < Math.min(topN, sortedMeanings.size()); ++i) {
                CandidateMeaning cm = iter.next();
                log("\t" + cm + " score: " + cm.getScore() + " density: " + density + " final score: " + cm.getFinalScore());
            }
        }
    }

    /**
     * prints the number of vertices and edges, and each fragment
     */
    public void debugShape() {
        log("SemInterpretGraph has " + this.v.size() + " vertices");
        for (CandidateMeaning cm : this.v) {
            log("Node: '" + cm + "' has " + cm.getConnectivity() + " connectivity.");
            for(Entry<CandidateMeaning, Float> entry : cm.successors.entrySet()) {
                log("    ---> " + entry.getKey() + " " + entry.getValue());
            }
            log("------------------------------");
            for(Entry<CandidateMeaning, Float> entry : cm.predecessors.entrySet()) {
                log("    <--- " + entry.getKey() + " " + entry.getValue());
            }
            log("------------------------------");
        }
    }
}
