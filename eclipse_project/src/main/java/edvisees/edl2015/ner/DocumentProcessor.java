package edvisees.edl2015.ner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import edu.stanford.nlp.process.ChineseDocumentToSentenceProcessor;
import edvisees.edl2015.candidates.DOMParser;
import edvisees.edl2015.candidates.Fragment;

public class DocumentProcessor {

    public static void dexml(String xmlfile, String rawtextfile) throws IOException {
        DOMParser parser = new DOMParser(xmlfile);
        BufferedWriter rawWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(rawtextfile), "utf-8"));
        rawWriter.write(parser.headline.toString().trim());
        rawWriter.newLine();
        rawWriter.newLine();
        for(Fragment fragment : parser.paragraphs) {
            String[] lines = fragment.text.trim().split("<quote[^>]*>.*</quote>");
            for(String line : lines) {
                if(line.trim().length() == 0) {
                    continue;
                }
                rawWriter.write(line.trim());
                rawWriter.newLine();
            }
            rawWriter.newLine();
        }
        rawWriter.close();
    }
    
    public static void splitChineseSentence(String rawtextfile, String outputfile) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(rawtextfile), "utf-8"));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputfile), "utf-8"));
        
        String input = in.readLine();
        while(input != null){
            input = input.trim();
            List<String> sent = ChineseDocumentToSentenceProcessor.fromPlainText(input);
            for (String a : sent) {
                out.write(a);
                out.newLine();
                out.flush();
            }
            input = in.readLine();
        }
        in.close();
        out.close();
    }
    
    private static HashMap<String, HashMap<String, String>> buildGoldMap(String goldfile) throws IOException {
        HashMap<String, HashMap<String, String>> goldMap = new HashMap<String, HashMap<String, String>>();
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(goldfile), "utf-8"));
        while(in.ready()) {
            String line = in.readLine().trim();
            String[] tokens = line.split("\t");
            String entity = tokens[2];
            String filename = tokens[3].substring(0, tokens[3].indexOf(':'));
            String nerType = tokens[5];
            String entityType = tokens[6];
            if(!entityType.equals("NAM")) {
                continue;
            }
            HashMap<String, String> entity2TypeMap = goldMap.get(filename);
            if(entity2TypeMap == null) {
                entity2TypeMap = new HashMap<String, String>();
                goldMap.put(filename, entity2TypeMap);
            }
            String oldNerType = entity2TypeMap.get(entity);
            if(oldNerType != null && !oldNerType.equals(nerType)) {
//                in.close();
//                throw new RuntimeException("ner type dismatch: " + filename + " " + entity + " " + oldNerType + " " + nerType);
                System.err.println("ner type dismatch: " + filename + " " + entity + " " + oldNerType + " " + nerType);
                continue;
            }
            entity2TypeMap.put(entity, nerType);
        }
        in.close();
        return goldMap;
    }
    
    private static void getNerTypesforRange(String[] words, int from, int to, String[] nerTypes, HashMap<String, String> entity2TypeMap, String separator) {
        for(int k = to - from; k > 0; --k) {
            for(int s = from; s + k <= to; ++s) {
                int t = s + k;
                StringBuilder eBuilder = new StringBuilder(words[s]);
                for(int j = s + 1; j < t; ++j) {
                    eBuilder.append(separator + words[j]);
                }
                String entity = eBuilder.toString().trim();
                String nerType = entity2TypeMap.get(entity);
                if(nerType != null) {
                    nerTypes[s] = "B-" + nerType;
                    for(int j = s + 1; j < t; ++j) {
                        nerTypes[j] = "I-" + nerType;
                    }
                    if(s > from) {
                        getNerTypesforRange(words, from, s, nerTypes, entity2TypeMap, separator);
                    }
                    if(t < to) {
                        getNerTypesforRange(words, t, to, nerTypes, entity2TypeMap, separator);
                    }
                    return;
                }
            }
        }
    }
    
    /**
     * transform .sent file to conll format (for cmn)
     * @param sentfile
     * @param entity2TypeMap
     * @param conllfile
     * @throws IOException
     */
    private static void sent2CoNLL(File sentfile, HashMap<String, String> entity2TypeMap, String conllfile) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(sentfile), "utf-8"));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(conllfile), "utf-8"));
        while(in.ready()) {
            String sent = in.readLine().trim();
            String[] words = new String[sent.length()];
            for(int j = 0; j < words.length; ++j) {
                words[j] = sent.substring(j, j + 1);
            }
            String[] nerTypes = new String[words.length];
            Arrays.fill(nerTypes, "O");
            getNerTypesforRange(words, 0, words.length, nerTypes, entity2TypeMap, "");
            for(int j = 0; j < words.length; ++j) {
                out.write(words[j] + "\t" + nerTypes[j] + "\n");
            }
            out.newLine();
        }
        in.close();
        out.close();
    }
    
    /**
     * 
     * @param sentfile
     * @param conllfile
     * @throws IOException
     */
    private static void sent2CoNLLNoTag(File sentfile, String conllfile) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(sentfile), "utf-8"));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(conllfile), "utf-8"));
        while(in.ready()) {
            String sent = in.readLine().trim();
            String[] words = new String[sent.length()];
            for(int j = 0; j < words.length; ++j) {
                words[j] = sent.substring(j, j + 1);
            }
            for(int j = 0; j < words.length; ++j) {
                out.write(words[j] + "\n");
            }
            out.newLine();
        }
        in.close();
        out.close();
    }
    
    /**
     * transform .tok file to conll format (for eng and spa)
     * @param tokfile
     * @param entity2TypeMap
     * @param conllfile
     * @throws IOException
     */
    private static void tok2CoNLL(File tokfile, HashMap<String, String> entity2TypeMap, String conllfile) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(tokfile), "utf-8"));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(conllfile), "utf-8"));
        while(in.ready()) {
            String sent = in.readLine().trim();
            if(sent.equals("<P>")) {
                continue;
            }
            String[] words = sent.split(" ");
            String[] nerTypes = new String[words.length];
            Arrays.fill(nerTypes, "O");
            getNerTypesforRange(words, 0, words.length, nerTypes, entity2TypeMap, " ");
            for(int j = 0; j < words.length; ++j) {
                out.write(words[j] + "\t" + nerTypes[j] + "\n");
            }
            out.newLine();
        }
        in.close();
        out.close();
    }
    
    /**
     * 
     * @param tokfile
     * @param conllfile
     * @throws IOException
     */
    private static void tok2CoNLLNoTag(File tokfile, String conllfile) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(tokfile), "utf-8"));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(conllfile), "utf-8"));
        while(in.ready()) {
            String sent = in.readLine().trim();
            if(sent.equals("<P>")) {
                continue;
            }
            String[] words = sent.split(" ");
            for(int j = 0; j < words.length; ++j) {
                out.write(words[j] + "\n");
            }
            out.newLine();
        }
        in.close();
        out.close();
    }
    
    /**
     * 
     * @param file
     * @param goldMap
     * @throws IOException
     */
    public static void trans2CoNLL(File file, HashMap<String, HashMap<String, String>> goldMap) throws IOException {
        if(file.isDirectory()) {
            File[] files = file.listFiles();
            Arrays.sort(files);
            for(File subfile : files) {
                trans2CoNLL(subfile, goldMap);
            }
        }
        else if(file.isFile()) {
            String name = file.getName();
            String path = file.getPath();
            if(name.endsWith(".tok")) {
                System.out.println("processing " + name);
                tok2CoNLL(file, goldMap.get(name.substring(0, name.indexOf('.'))), path.substring(0, path.length() - 4) + ".conll");
            }
            else if(name.endsWith(".sent")) {
                System.out.println("processing " + name);
                sent2CoNLL(file, goldMap.get(name.substring(0, name.indexOf('.'))), path.substring(0, path.length() - 5) + ".conll");
            }
        }
        else {
            throw new IOException(file.getPath() + " is not a normal file or a directory");
        }
    }
    
    /**
     * 
     * @param file
     * @throws IOException
     */
    public static void trans2CoNLLNoTag(File file) throws IOException {
        if(file.isDirectory()) {
            File[] files = file.listFiles();
            Arrays.sort(files);
            for(File subfile : files) {
                trans2CoNLLNoTag(subfile);
            }
        }
        else if(file.isFile()) {
            String name = file.getName();
            String path = file.getPath();
            if(name.endsWith(".tok")) {
                System.out.println("processing " + name);
                tok2CoNLLNoTag(file, path.substring(0, path.length() - 4) + ".conll");
            }
            else if(name.endsWith(".sent")) {
                System.out.println("processing " + name);
                sent2CoNLLNoTag(file, path.substring(0, path.length() - 5) + ".conll");
            }
        }
        else {
            throw new IOException(file.getPath() + " is not a normal file or a directory");
        }
    }
    
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println(
                "Usage: java DocumentProcessor MODE INPUTFILENAME OUTPUTFILENAME \n" +
                "\tjava DocumentProcessor [dexml|cssplit|trans2conll] <inputfile> <outputfile>"
            );
            return;
        }
        
        if(args[0].equals("dexml")) {
            dexml(args[1], args[2]);
        }
        else if(args[0].equals("cssplit")) {
            splitChineseSentence(args[1], args[2]);
        }
        else if(args[0].equals("trans2conll")) {
            if(args.length > 2) {
                trans2CoNLL(new File(args[1]), buildGoldMap(args[2]));
            }
            else {
                trans2CoNLLNoTag(new File(args[1]));
            }
        }
        else {
            System.err.println("unrecognized mode: " + args[0]);
            System.out.println(
                "Usage: java DocumentProcessor MODE INPUTFILENAME OUTPUTFILENAME \n" +
                "\tjava DocumentProcessor [dexml|cssplit] <inputfile> <outputfile>"
            );
        }
    }

}
