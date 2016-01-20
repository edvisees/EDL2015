package edvisees.edl2015.candidates;

import java.util.HashMap;
import java.util.Map;

import edvisees.edl2015.lucene.Searcher;

public class CandidateExtractorFactory {
    private static Map<String, CandidateExtractor> extractors = new HashMap<String, CandidateExtractor>();

    synchronized public static CandidateExtractor getCandidateExtractor(String language, Searcher nameSearcher, Searcher badIdSearcher) throws Exception {
        if (!extractors.containsKey(language)) {
            if (language.equals("en")) {
                extractors.put(language, new CandidateExtractor(nameSearcher, badIdSearcher));
            } else if (language.equals("es")) {
                extractors.put(language, new SpanishCandidateExtractor(nameSearcher, badIdSearcher));
            } else if (language.equals("zh")) {
                extractors.put(language, new ChineseCandidateExtractor(nameSearcher, badIdSearcher));
            } else {
                throw new Exception("Unsupported language: '" + language + "'");
            }
        }
        return extractors.get(language);
    }
}
