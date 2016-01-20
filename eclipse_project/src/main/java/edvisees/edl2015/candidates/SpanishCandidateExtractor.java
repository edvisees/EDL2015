package edvisees.edl2015.candidates;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import edvisees.edl2015.lucene.Searcher;

public class SpanishCandidateExtractor extends CandidateExtractor {
    private static final String[] BAD_POS_TAGS_EXTREMES_S = {"c", "f", "i", "p", "s", "v", "r"};
    private static final List<String> BAD_POS_TAGS_EXTREMES_S_LIST = Arrays.asList(BAD_POS_TAGS_EXTREMES_S);
    private static final String[] BAD_POS_TAGS_EXTREMES_E = {"c", "f", "i", "p", "s", "v", "r", "d"};
    private static final List<String> BAD_POS_TAGS_EXTREMES_E_LIST = Arrays.asList(BAD_POS_TAGS_EXTREMES_E);
    private static final String[] BAD_WORDS_EXTREMES = {"a", "al", "con", "desde", "de", "del", "en", "entre",
        "hacia", "para", "sin", "por", "\"", "`", "``", "'", "''", "."};
    private static final List<String> BAD_WORDS_EXTREMES_LIST = Arrays.asList(BAD_WORDS_EXTREMES);

    public SpanishCandidateExtractor(Searcher nameSearcher, Searcher badIdSearcher) {
        super(nameSearcher, badIdSearcher);
    }

    @Override
    protected Properties getStanfordCoreNlpProperties() {
        Properties props = new Properties();
        props.setProperty("pos.model", "edu/stanford/nlp/models/pos-tagger/spanish/spanish-distsim.tagger");
        props.setProperty("ner.model", "/usr1/shared/projects/edl2015/models/ner-model-edl2015-spa-both.ser.gz");
        props.setProperty("ner.applyNumericClassifiers", "false");
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner");
        return props;
    }

    @Override
    protected List<String> getBadExtremesStart() {
        return BAD_POS_TAGS_EXTREMES_S_LIST;
    }

    @Override
    protected List<String> getBadExtremesEnd() {
        return BAD_POS_TAGS_EXTREMES_E_LIST;
    }
    
    @Override
    protected boolean isAdjectiveFragment(List<TaggedWord> windowTaggedWords) {
        if(windowTaggedWords.size() == 1) {
            TaggedWord taggedWord = windowTaggedWords.get(0);
            if(taggedWord.pos.toLowerCase().startsWith("a") && (taggedWord.word.matches("[A-Z].*") || countryMap.containsKey(taggedWord.word.toLowerCase().trim()))) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean isGoodWindow(List<TaggedWord> windowTaggedWords) {
        boolean isGood = super.isGoodWindow(windowTaggedWords);
        if (isGood) {
            // there are some words in Spanish are are poorly pos-tagged by Stanford
            // don't let the extremes be PREPOSITIONS, PUNCT, etc
            String firstWord = windowTaggedWords.get(0).word.toLowerCase();
            String lastWord = windowTaggedWords.get(windowTaggedWords.size() - 1).word.toLowerCase();
            if (BAD_WORDS_EXTREMES_LIST.contains(firstWord) || BAD_WORDS_EXTREMES_LIST.contains(lastWord)) {
                isGood = false;
            }
        }
        else {
            isGood = isAdjectiveFragment(windowTaggedWords);
        }
        return isGood;
    }
}
