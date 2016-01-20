package edvisees.edl2015.evaluation;

import java.util.ArrayList;
import java.util.List;

public class Evaluation {
    private CorpusAnnotations corpusGoldAnnotations;
    private CorpusAnnotations corpusBabelfyAnnotations;
    private int toalNamedEntityMatchesCount;
    private int totalNerMatchesCount;
    private int totalLinkMatchesCount;
    private int totalGoldAnnotationsSize;
    private int totalBabelfyAnnotationsSize;

    public Evaluation(CorpusAnnotations goldAnnotations, CorpusAnnotations babelfyAnnotations) {
        this.corpusGoldAnnotations = goldAnnotations;
        this.corpusBabelfyAnnotations = babelfyAnnotations;
    }

    public List<String> calculateResult(int windowSize) {
        List<String> results = new ArrayList<String>();
        for (String fileName: this.corpusBabelfyAnnotations.getDocuments()) {
            System.out.println("Precision and Recall for Document: " + fileName);
            List<Annotation> babelfyAnnotations = this.corpusBabelfyAnnotations.getDocument(fileName).getAnnotations();
            List<Annotation> goldAnnotations = this.corpusGoldAnnotations.getDocument(fileName).getAnnotations();
            results.addAll(matchCount(babelfyAnnotations, goldAnnotations, windowSize));
        }
        results.add("Precision and Recall for all documents: ");
        results.addAll(printResult(this.toalNamedEntityMatchesCount, this.totalNerMatchesCount, this.totalLinkMatchesCount, this.totalBabelfyAnnotationsSize, this.totalGoldAnnotationsSize));
        return results;
    }

    public List<String> matchCount(List<Annotation> bebelfyAnnotations, List<Annotation> goldAnnotations,int windowSize) {
        int namedEntityMatchesCount = 0;
        int nerMatchesCount = 0;
        int linkMatchesCount = 0;
        List<Boolean> labels = new ArrayList<Boolean>();
        for (Annotation babelfyAnnotation: bebelfyAnnotations) {
            boolean label = false;
            for (Annotation goldAnnotation: goldAnnotations) {
                if (babelfyAnnotation.namedEntityMatches(goldAnnotation, windowSize)) {
                    namedEntityMatchesCount += 1;
                    this.toalNamedEntityMatchesCount += 1;
                    label = true;
                }
                if (babelfyAnnotation.nerMatches(goldAnnotation, windowSize)) {
                    nerMatchesCount += 1;
                    this.totalNerMatchesCount += 1;
                }
                if (babelfyAnnotation.linkMatches(goldAnnotation, windowSize)) {
                    linkMatchesCount += 1;
                    this.totalLinkMatchesCount += 1;
                }
            }
            labels.add(label);
        }
        totalGoldAnnotationsSize += goldAnnotations.size();
        totalBabelfyAnnotationsSize += bebelfyAnnotations.size();
        return printResult(namedEntityMatchesCount, nerMatchesCount, linkMatchesCount, bebelfyAnnotations.size(), goldAnnotations.size());
    }

    public List<String> printResult(int matchesCount1, int matchesCount2, int matchesCount3, int length1, int length2) {
        List<String> results = new ArrayList<String>();
        results.add("-----name match-----");
        results.addAll(printPrecisionRecall(matchesCount1, length1, length2));
        results.add("-----ner match-----");
        results.addAll(printPrecisionRecall(matchesCount2, length1, length2));
        results.add("-----link match-----");
        results.addAll(this.printPrecisionRecall(matchesCount3, length1, length2));
        return results;
    }

    public List<String> printPrecisionRecall(int matchesCount, int length1, int length2) {
        List<String> results = new ArrayList<String>();
        results.add("precision: " + matchesCount + " / " + length1 + " = " + (1.0 * matchesCount / length1));
        results.add("recall: " + matchesCount + " / " + length2 + " = " + (1.0 * matchesCount / length2));
        return results;
    }

    public static void main(String[] args) throws Exception {
        Evaluation eval = new Evaluation(
                new CorpusAnnotations(args[0]),
                new CorpusAnnotations(args[1]));
        int windowSize = Integer.parseInt(args[2]);
        for (String resultLine : eval.calculateResult(windowSize)) {
            System.out.println(resultLine);
        }
    }
}
