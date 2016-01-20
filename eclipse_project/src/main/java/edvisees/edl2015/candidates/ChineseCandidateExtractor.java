package edvisees.edl2015.candidates;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edvisees.edl2015.lucene.Searcher;

public class ChineseCandidateExtractor extends CandidateExtractor {
    protected static final int WINDOW_SIZE = 10;
    
    private static final Set<String> punctSet = new HashSet<String>(Arrays.asList(",", ".", "!", "?", ":", ";", "\"", "`", "…",
                                                                "，", "。", "！", "？", "：", "“", "”", "、", "；", "《", "》"));
    
    private static final Set<String> BAD_CHARACTER_EXTREMES_LIST = new HashSet<String>(Arrays.asList(" ", "的", "了", "吗", "[", "]", "(", ")", "（", "）", "【", "】"));
    
    private static final Set<String> single_character_entity = new HashSet<String>();
    
    private static void getSingleCharacterEntity() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("/usr1/shared/projects/edl2015/models/single_character_entity.txt"), "utf-8"));
            while(reader.ready()) {
                String line = reader.readLine().trim();
                single_character_entity.add(line);
            }
            reader.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public ChineseCandidateExtractor(Searcher nameSearcher, Searcher badIdSearcher) {
        super(nameSearcher, badIdSearcher);
        getSingleCharacterEntity();
    }

    @Override
    protected Properties getStanfordCoreNlpProperties() {
        Properties props = new Properties();
        props.setProperty("customAnnotatorClass.segment", "edu.stanford.nlp.pipeline.ChineseSegmenterAnnotator");
        props.setProperty("segment.model", "/usr1/shared/projects/edl2015/models/segment-model-cmn.ser.gz");
        props.setProperty("segment.serDictionary", "/usr1/shared/projects/edl2015/models/dict-chris6.ser.gz");
        props.setProperty("segment.sighanCorporaDict", "/usr1/shared/projects/edl2015/models/data/");
        props.setProperty("segment.sighanPostProcessing", "true");
        props.setProperty("inputEncoding", "UTF-8");
        props.setProperty("ssplit.boundaryTokenRegex", "[.]|[!?]+|[。]|[！？]+");
        props.setProperty("ner.model", "/usr1/shared/projects/edl2015/models/ner-model-edl2015-cmn-both.ser.gz");
        props.setProperty("ner.applyNumericClassifiers", "false");
        props.setProperty("ner.useSUTime", "false");
        props.setProperty("annotators", "segment, ssplit, ner");
        return props;
    }

    @Override
    public List<Fragment> findCandidateMeanings(Fragment sentence, int maxResultsLucene) throws Exception {
        Set<Fragment> fragmentsWithMeaning = this.extract(sentence, maxResultsLucene, 0, sentence.text.length());
        List<TaggedWord> taggedWords = this.getTags(sentence.text);
        int startOffset = sentence.startOffset;
        Set<Fragment> stanfordFragments = getStanfordFragments(taggedWords, startOffset);
        
        addStanfordFragmentToFragmentWithMeaning(null, fragmentsWithMeaning, stanfordFragments, maxResultsLucene);
        return new ArrayList<Fragment>(fragmentsWithMeaning);
    }
    
    @Override
    protected Set<Fragment> getStanfordFragments(List<TaggedWord> taggedWords, int startOffset){
        Set<Fragment> stanfordFragments = new HashSet<Fragment>();
        TaggedWord temp = null;
        for (int i = 0; i < taggedWords.size(); i++) {
            if (taggedWords.get(i).ner.startsWith("B-")){
                temp = new TaggedWord(taggedWords.get(i).word, null, taggedWords.get(i).ner.substring(2), 
                        taggedWords.get(i).startOffset, taggedWords.get(i).endOffset);
            }
            else if (taggedWords.get(i).ner.startsWith("I-")) {
                temp.word += taggedWords.get(i).word;
                temp.endOffset = taggedWords.get(i).endOffset;
            }
            else {
                if (temp != null){
                    int realStartOffset = startOffset + temp.startOffset;
                    int realEndOffset = startOffset + temp.endOffset;
                    String alternativeText = countryMap.get(temp.word.toLowerCase());
                    Fragment stanfordFragment = new Fragment(temp.word, alternativeText, realStartOffset, realEndOffset);
                    stanfordFragment.nerStanford = temp.ner;
                    stanfordFragments.add(stanfordFragment);
                    System.out.println("Stanford NER: " + temp.word + " ( " + realStartOffset + ", " + realEndOffset + " ) " + " -> NER -> " + temp.ner);
                }
                temp = null;
            }
        }
        return stanfordFragments;
    }
    
    private boolean isPunct(String character) {
        return punctSet.contains(character);
    }
    
    private boolean containsPunct(String text) {
        for(int i = 0; i < text.length(); ++i) {
            if(isPunct(text.substring(i, i + 1))) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isBadWindow(String text) {
        return BAD_CHARACTER_EXTREMES_LIST.contains(text.substring(0, 1)) 
                || BAD_CHARACTER_EXTREMES_LIST.contains(text.substring(text.length() - 1))
                || (text.length() == 1 && !single_character_entity.contains(text))
                || containsPunct(text);
    }

    private Set<Fragment> extract(Fragment fragment, int maxNumberResultsLucene, int from, int to) throws Exception {
        Set<Fragment> subFragments = new HashSet<Fragment>();
        if (from >= to) {
            return subFragments;
        }
        outer: for (int k = WINDOW_SIZE; k > 0; --k) {
            //System.out.println(String.format("searching in '%s' using k = %d.", fragment.text, k));
            for (int s = from; s + k < to; s++) {
                int t = s + k;
                String text = fragment.text.substring(s, t);
                if(isBadWindow(text)) {
                    continue;
                }
                String alternativeText = countryMap.get(text.toLowerCase());
                Fragment subFragment = new Fragment(text, alternativeText, fragment.startOffset + s, fragment.startOffset + t);
                this.getCandidateMeaningsFromLucene(subFragment, maxNumberResultsLucene);
                if (subFragment.meanings.size() > 0) {
                    System.out.println("Expand Frag: " + subFragment + " ( " + subFragment.startOffset + ", " + subFragment.endOffset + " ) " 
                            + " ( " + fragment.startOffset + " ) " + " ( " + s + " )" + " ( " + t + " )");
                    subFragments.add(subFragment);
                    subFragments.addAll(this.extract(fragment, maxNumberResultsLucene, from, s));
                    subFragments.addAll(this.extract(fragment, maxNumberResultsLucene, t, to));
                    break outer;
                }
            }
        }
        return subFragments;
    }

    private List<TaggedWord> getTags(String fragment) {
        List<TaggedWord> fragmentTagged = new ArrayList<TaggedWord>();
        Annotation annotation = new Annotation(fragment);
        this.pipeline.annotate(annotation);
        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            for (CoreLabel token: sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                String word = token.get(CoreAnnotations.TextAnnotation.class);
                String ner = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
                int startOffset = token.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
                int endOffset = token.get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
//                System.out.println(token.originalText() + " -> NER -> " + ner);
                fragmentTagged.add(new TaggedWord(word, null, ner, startOffset, endOffset));
            }
        }
        return fragmentTagged;
    }

    public static void main(String[] args) throws Exception {
        ChineseCandidateExtractor cce = new ChineseCandidateExtractor(null, null);
        String input = "(国际)驻伊美军称“基地”组织伊拉克分支二号头目被打死";
        List<Fragment> candidates = cce.findCandidateMeanings(new Fragment(input, 0, input.length()));
        for (Fragment candidate : candidates) {
            System.out.println(candidate);
        }
    }
}
