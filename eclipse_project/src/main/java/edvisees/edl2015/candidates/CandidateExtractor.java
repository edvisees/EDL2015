package edvisees.edl2015.candidates;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edvisees.edl2015.lucene.IndexItem;
import edvisees.edl2015.lucene.LuceneIndexSearcher;
import edvisees.edl2015.lucene.Searcher;

public class CandidateExtractor {
    public static final int MAX_RESULTS = 50;
    protected static final int WINDOW_SIZE = 5;
    protected LuceneIndexSearcher nameSearcher;
    protected Searcher badIdSearcher;
    protected StanfordCoreNLP pipeline;
    private static final String[] BAD_POS_TAGS_EXTREMES_S = {"p", "v", "i", "c", "t", ".", ",", "''", "'", "``", "`", ":"};
    private static final List<String> BAD_POS_TAGS_EXTREMES_S_LIST = Arrays.asList(BAD_POS_TAGS_EXTREMES_S);
    private static final String[] BAD_POS_TAGS_EXTREMES_E = {"p", "v", "i", "c", "t", ".", ",", "''", "'", "``", "`", ":", "d"};
    private static final List<String> BAD_POS_TAGS_EXTREMES_E_LIST = Arrays.asList(BAD_POS_TAGS_EXTREMES_E);
    protected static final Map<String, String> countryMap = new HashMap<String, String>();

    public CandidateExtractor(LuceneIndexSearcher searcher, Searcher badIdSearcher) {
        this.nameSearcher = searcher;
        this.pipeline = new StanfordCoreNLP(this.getStanfordCoreNlpProperties());
        this.badIdSearcher = badIdSearcher;
        getCountryMap();
    }

    private static void getCountryMap() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("/usr1/shared/projects/edl2015/models/country_adj.txt"), "utf-8"));
            while(reader.ready()) {
                String line = reader.readLine().trim();
//                System.out.println(line);
                String[] tokens = line.split("\t");
                String originalName = tokens[0];
                String[] adjectiveNames = tokens[1].split(", ");
                for(String adjectiveName : adjectiveNames) {
                    countryMap.put(adjectiveName.toLowerCase().trim(), originalName.toLowerCase().trim());
                }
            }
            reader.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    protected Properties getStanfordCoreNlpProperties() {
        Properties props = new Properties();
        props.setProperty("ner.model", "/usr1/shared/projects/edl2015/models/ner-model-edl2015-eng-both.ser.gz");
        props.setProperty("ner.applyNumericClassifiers", "false");
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner");
        return props;
    }

    private List<TaggedWord> getTags(String fragment) {
        List<TaggedWord> fragmentTagged = new ArrayList<TaggedWord>();
        Annotation annotation = new Annotation(fragment);
        this.pipeline.annotate(annotation);
        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            for (CoreLabel token: sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                String word = token.get(CoreAnnotations.TextAnnotation.class);
                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                String ner = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
                int startOffset = token.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
                int endOffset = token.get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
//                System.out.println(token.originalText() + " -> NER -> " + ner);
                fragmentTagged.add(new TaggedWord(word, pos, ner, startOffset, endOffset));
            }
        }
        return fragmentTagged;
    }

    public List<Fragment> findCandidateMeanings(Fragment inputText) throws Exception {
        return getSubFragmentsWithCandidateMeanings(inputText, MAX_RESULTS, false);
    }

    public List<Fragment> findCandidateMeanings(Fragment inputText, int maxResultsLucene) throws Exception {
        return getSubFragmentsWithCandidateMeanings(inputText, maxResultsLucene, false);
    }

    public List<Set<Fragment>> extractAllFragments(Fragment sentence) throws Exception {
        Set<Fragment> expandedFragments = new HashSet<Fragment>();
        List<TaggedWord> taggedWords = this.getTags(sentence.text);
        System.out.println("P: |" + sentence.text + "|");
        for (int i = 0; i < taggedWords.size(); i++) {
            // get expanded-fragments of size from 1 to WINDOW_SIZE
            List<TaggedWord> windowTaggedWords = new ArrayList<TaggedWord>();
            String expandedFragmentText = "";
            for (int j = 0; j < Math.min(taggedWords.size() - i, WINDOW_SIZE); j++) {
                TaggedWord nWord = taggedWords.get(i + j);
                expandedFragmentText += " " + nWord.word;
                windowTaggedWords.add(nWord);
                if (this.isGoodWindow(windowTaggedWords)) {
                    String alternativeText = countryMap.get(expandedFragmentText.trim().toLowerCase());
                    Fragment expandedFragment = new Fragment(expandedFragmentText.trim(), alternativeText, 
                            sentence.startOffset + taggedWords.get(i).startOffset, sentence.startOffset + nWord.endOffset);
                    expandedFragment.setPosTags(windowTaggedWords);
                    expandedFragments.add(expandedFragment);
                    System.out.println("Expand Frag: " + expandedFragment.getWordsAndTags() 
                    + " ( " + expandedFragment.startOffset + ", " + expandedFragment.endOffset + " ) " 
                            + " ( " + sentence.startOffset + " ) " + " ( " + taggedWords.get(i).startOffset + " )"
                            + " ( " + taggedWords.get(i).endOffset + " )");
                }
            }
        }
        Set<Fragment> stanfordFragments = this.getStanfordFragments(taggedWords, sentence.startOffset);
        List<Set<Fragment>> res = new ArrayList<Set<Fragment>>();
        res.add(expandedFragments);
        res.add(stanfordFragments);
        return res;
    }

    protected Set<Fragment> getStanfordFragments(List<TaggedWord> taggedWords, int startOffset){
        Set<Fragment> stanfordFragments = new HashSet<Fragment>();
        TaggedWord temp = null;
        for (int i = 0; i < taggedWords.size(); i++) {
            if (taggedWords.get(i).ner.startsWith("B-")){
                temp = new TaggedWord(taggedWords.get(i).word, taggedWords.get(i).pos, taggedWords.get(i).ner.substring(2), 
                        taggedWords.get(i).startOffset, taggedWords.get(i).endOffset);
            }
            else if (taggedWords.get(i).ner.startsWith("I-")) {
                temp.word += " " + taggedWords.get(i).word;
                temp.pos += " " + taggedWords.get(i).pos;
                temp.endOffset = taggedWords.get(i).endOffset;
            }
            else {
                if (temp != null){
                    int realStartOffset = startOffset + temp.startOffset;
                    int realEndOffset = startOffset + temp.endOffset;
                    String alternativeText = countryMap.get(temp.word.trim().toLowerCase());
                    Fragment stanfordFragment = new Fragment(temp.word, alternativeText, realStartOffset, realEndOffset);
                    stanfordFragment.nerStanford = temp.ner;
                    stanfordFragment.posTags = Arrays.asList(temp.pos.split(" "));
                    stanfordFragments.add(stanfordFragment);
                    System.out.println("Stanford NER: " + temp.word + " ( " + realStartOffset + ", " + realEndOffset + " ) " + " -> NER -> " + temp.ner);
                }
                temp = null;
            }
        }
        return stanfordFragments;
    }

    // assume trimmed()
    protected List<Fragment> getSubFragmentsWithCandidateMeanings(Fragment inputText, int maxResultsLucene, boolean includeEmptyCandidates) throws Exception {
        List<Set<Fragment>> fragmentSetList = this.extractAllFragments(inputText);
        Set<Fragment> expandedFragments = fragmentSetList.get(0);
        for (Fragment expandedFragment : expandedFragments) {
            this.getCandidateMeaningsFromLucene(expandedFragment, maxResultsLucene); // this will leave the fragment linked with meanings
        }
        Set<Fragment> fragmentsWithMeaning = expandedFragments.stream().filter(f -> f.meanings.size() > 0).collect(Collectors.toCollection(HashSet::new));
        
        Set<Fragment> stanfordFragments = fragmentSetList.get(1);
        addStanfordFragmentToFragmentWithMeaning(expandedFragments, fragmentsWithMeaning, stanfordFragments, maxResultsLucene);
        
        return includeEmptyCandidates
                ? new ArrayList<Fragment>(expandedFragments)
                : new ArrayList<Fragment>(fragmentsWithMeaning);
    }
    
    protected void addStanfordFragmentToFragmentWithMeaning(Set<Fragment> expandedFragments, Set<Fragment> fragmentsWithMeaning, 
            Set<Fragment> stanfordFragments, int maxResultsLucene) throws Exception{
     // self to self map (looks weird, for update stanford ner type)
        Map<Fragment, Fragment> fragIDMap = new HashMap<Fragment, Fragment>();
        for(Fragment fragment : fragmentsWithMeaning) {
            fragIDMap.put(fragment, fragment);
        }
        for(Fragment stanfordFragment : stanfordFragments) {
            if(fragmentsWithMeaning.contains(stanfordFragment)) {
                fragIDMap.get(stanfordFragment).nerStanford = stanfordFragment.nerStanford;
                continue;
            }
            this.getCandidateMeaningsFromLucene(stanfordFragment, maxResultsLucene);
            if(stanfordFragment.meanings.size() > 0) {
                fragmentsWithMeaning.add(stanfordFragment);
            }
            else if(expandedFragments == null || expandedFragments.contains(stanfordFragment)) {
                stanfordFragment.isNIL = true;
                fragmentsWithMeaning.add(stanfordFragment);
            }
        }
    }
    
    protected boolean isAdjectiveFragment(List<TaggedWord> windowTaggedWords) {
        if(windowTaggedWords.size() == 1) {
            TaggedWord taggedWord = windowTaggedWords.get(0);
            if(taggedWord.pos.toLowerCase().startsWith("j") && (taggedWord.word.matches("[A-Z].*") || countryMap.containsKey(taggedWord.word.toLowerCase().trim()))) {
                return true;
            }
        }
        return false;
    }

    protected boolean isGoodWindow(List<TaggedWord> windowTaggedWords) {
        boolean isGood = false;
        // at least one N
        for (TaggedWord taggedWord : windowTaggedWords) {
            if (taggedWord.pos.toLowerCase().startsWith("n")) {
                isGood = true;
                break;
            }
        }
        if (isGood) {
            // don't let the extremes be P, V, In, Cc, etc
            String firstPOS = windowTaggedWords.get(0).pos.toLowerCase().substring(0, 1);
            String lastPOS = windowTaggedWords.get(windowTaggedWords.size() - 1).pos.toLowerCase().substring(0, 1);
            if (this.getBadExtremesStart().contains(firstPOS) || this.getBadExtremesEnd().contains(lastPOS)) {
                isGood = false;
            }
        }
        else {
            isGood = isAdjectiveFragment(windowTaggedWords);
        }
        return isGood;
    }

    /**
     * extract all candidates for this particular fragment.
     */
    protected void getCandidateMeaningsFromLucene(Fragment fragment, int maxNumberResultsLucene) throws Exception {
        if (fragment.text.trim().length() == 0) return;
        List<CandidateMeaning> fragmentCandidates = new ArrayList<CandidateMeaning>();
        try {
            String textToSearch = fragment.alternativeText == null ? fragment.text : fragment.alternativeText;
            List<IndexItem> entities = this.nameSearcher.findByTitle(textToSearch, maxNumberResultsLucene); // this will return integer ids (as string)
            for (IndexItem entity : entities) {
                if (this.badIdSearcher != null) {
                    // only add the candidates that are not bad ids!
                    IndexItem badId = this.badIdSearcher.findByIdUnique(entity.getId());
                    if (badId != null) {
                        //System.out.println("bad id matched: " + badId);
                    } else {
                        fragmentCandidates.add(new CandidateMeaning(entity.getId(), fragment));
                    }
                } else {
                    fragmentCandidates.add(new CandidateMeaning(entity.getId(), fragment));
                }
            }
        } catch(Exception e) {
            System.out.println("ERROR QUERYING '" + fragment.text + "': " + e.getMessage() );
        }
    }

    private static List<Fragment> extractFragmentsFromXmlFile(File inputFile) throws IOException {
        List<Fragment> fragments = new ArrayList<Fragment>();
        DOMParser parser = new DOMParser(inputFile);
        for (Fragment fragment : parser.getFragments()) {
            fragments.add(fragment);
        }
        return fragments;
    }

    protected List<String> getBadExtremesStart() {
        return BAD_POS_TAGS_EXTREMES_S_LIST;
    }

    protected List<String> getBadExtremesEnd() {
        return BAD_POS_TAGS_EXTREMES_E_LIST;
    }

    public static void main(String[] args) throws Exception {
        List<String> languages = Arrays.asList(new String[]{"en", "es", "zh"});
        if (args.length < 4) {
            System.out.println("usage: CandidateExtractor lang lucene_dir input_path outputFile");
            return;
        }
        String language = args[0].toLowerCase();
        if (!languages.contains(language)) {
            System.out.println("usage: CandidateExtractor lang lucene_dir input_path outputFile");
            return;
        }
        String luceneDir = args[1];
        Searcher nameSearcher = new Searcher(luceneDir + File.separator + "index_name");
        File badIdsSearcherFile = new File(luceneDir + File.separator + "index_id_bad_ids");
        Searcher badIdsSearcher = badIdsSearcherFile.exists() ? new Searcher(badIdsSearcherFile.getAbsolutePath()) : null;
        File inputPath = new File(args[2]);
        String outputFileName = args[3];
        CandidateExtractor instance = CandidateExtractorFactory.getCandidateExtractor(language, nameSearcher, badIdsSearcher);
        List<Fragment> fragments = new ArrayList<Fragment>();
        if (inputPath.isDirectory()) {
            System.out.println("DIRECTORY");
            for (File inputFile : inputPath.listFiles()) {
                if (inputFile.getName().endsWith(".xml")) {
                    System.out.println(inputFile.getName());
                    fragments.addAll(extractFragmentsFromXmlFile(inputFile));
                }
            }
        } else {
            List<Fragment> fileFragments = extractFragmentsFromXmlFile(inputPath);
            fragments.addAll(fileFragments);
        }
        // for each fragment, run the Candidate Extractor
        Set<Integer> goodNodes = new HashSet<Integer>(50000);
        int qNodes = 0; // not unique
        for (Fragment fragment : fragments) {
            List<Fragment> subFragments = instance.findCandidateMeanings(fragment);
            for (Fragment subFragment : subFragments) {
                if (subFragment.meanings.size() > 0) {
                    qNodes += subFragment.meanings.size();
                    System.out.println("------------\n" + subFragment + " " + subFragment.meanings.size() + " nodes");
                    for (CandidateMeaning meaning : subFragment.meanings) {
                        goodNodes.add(new Integer(meaning.entityId));
                    }
                }
            }
        }
        System.out.println("Found " + goodNodes.size() + " good nodes (total = " + qNodes + ")");
        ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(outputFileName));
        FileWriter streamString = new FileWriter(outputFileName + ".str");
        for (Integer nodeId : goodNodes) {
            stream.writeObject(nodeId);
            streamString.write(nodeId + "\n");
        }
        stream.close();
        streamString.close();
    }
}

class TaggedWord {
    String word;
    String pos;
    String ner;
    int startOffset;
    int endOffset;

    public TaggedWord(String word, String pos, String ner, int startOffset, int endOffset) {
        this.word = word;
        this.pos = pos;
        this.ner = ner;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }
}
