package edvisees.edl2015.graph;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edvisees.edl2015.lucene.IndexItem;
import edvisees.edl2015.lucene.Searcher;

/**
 * read a file of node indexes
 * and generate a file of:
 *  nodeindex alias1
 *  nodeindex alias2
 *  etc
 * @author eeelnico
 *
 */
public class ExtractAcronymsOfNodeIndexes {
    private File inputFile;
    private Searcher nameSearcher;
    private File outputFile;
    private static Pattern titleCasePattern = Pattern.compile("^([A-Z][a-z]+(\\s([A-Z][a-z]+|of|and|the|or))+)$");
    private static Pattern badEndingsPattern = Pattern.compile("^(Corp|Co|Inc|Ltd|Llc)\\.?$");

    public ExtractAcronymsOfNodeIndexes(File inputFile, String luceneIndexDir, File outputFile) throws IOException {
        this.inputFile = inputFile;
        this.nameSearcher = new Searcher(luceneIndexDir + File.separator + "index_name");
        this.outputFile = outputFile;
    }

    /**
     * get all the names of the given entity
     * @param nodeIndex
     * @return
     * @throws Exception
     */
    private List<String> processLine(String nodeIndex) throws Exception {
        Set<String> acronyms = new HashSet<String>();
        List<IndexItem> allNodeNames = this.nameSearcher.findById(nodeIndex, 100);
        for (IndexItem nodeName : allNodeNames) {
            String longName = nodeName.getTitle();
            String acronym = computeAcronym(longName);
            if (acronym != null) {
                acronyms.add(acronym + "\t" + longName);
            }
        }
        List<String> nodeAcronyms = new ArrayList<String>();
        for (String acronym : acronyms) {
            nodeAcronyms.add(nodeIndex + '\t' + acronym + '\n');
        }
        return nodeAcronyms;
    }

    private static String computeAcronym(String longName) {
        Matcher titleCaseMatcher = titleCasePattern.matcher(longName);
        if (titleCaseMatcher.matches()) {
            String[] words = longName.split("\\s");
            String acronym = "";
            for (String word : words) {
                if (badEndingsPattern.matcher(word).matches()) continue; // discard Inc, Ltd, Corp, etc
                String firstLetter = word.substring(0, 1);
                if (firstLetter.toUpperCase().equals(firstLetter)) {
                    acronym += firstLetter;
                }
            }
            return acronym;
        } else {
            return null;
        }
    }

    protected void process() throws Exception {
        BufferedReader in = null;
        BufferedWriter out = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(this.inputFile), StandardCharsets.UTF_8));
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.outputFile), StandardCharsets.UTF_8));
            String line = null;
            while (in.ready()) {
                line = in.readLine();
                for (String acronym : this.processLine(line.trim())) {
                    out.write(acronym);
                }
            }
        } catch (Exception e) {
            System.out.println("exception processing file: " + this.inputFile.getName());
            throw e;
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }

    /**
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("\n\nUsage: ExtractAliasesOfNodeIndexes inputFile luceneIndexDir outputFilePath\n");
            return;
        }
        File inputFile = new File(args[0]);
        String luceneIndexDir = args[1];
        File outputFile = new File(args[2]);
        ExtractAcronymsOfNodeIndexes acronymExtractor = new ExtractAcronymsOfNodeIndexes(inputFile, luceneIndexDir, outputFile);
        acronymExtractor.process();
    }

//    public static void main(String[] args) {
//        String input = "Islamic State of Iraq and the Levant";
//        System.out.println(computeAcronym(input));
//    }
}
