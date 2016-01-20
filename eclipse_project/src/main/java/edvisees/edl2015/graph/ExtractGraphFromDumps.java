package edvisees.edl2015.graph;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edvisees.edl2015.lucene.IndexItem;
import edvisees.edl2015.lucene.Searcher;

public class ExtractGraphFromDumps {
    private File inputDir;
    private Pattern inputFilesPattern;
    private String luceneIndexDir;
    private String ontologyKeysFilePath;
    private File outputFile;
    private Searcher badIdsSearcher;
    BufferedWriter outputWriter;
    private Pattern freebaseIdPattern = Pattern.compile("<http://rdf\\.basekb\\.com/ns/(m\\..*)>");
    private String ontologyElementPrefix = "(?:music|book|media_common|people|film|tv|location|business|fictional_universe|organization|biology|sports|award|education|event|architecture|government|soccer|medicine|cvg|projects|geography|visual_art|astronomy|olympics|internet|military|transportation|theater|periodicals|protected_sites|influence|broadcast|aviation|food|royalty|boats|travel|american_football|computer|baseball|chemistry|law|religion|library|cricket|basketball|symbols|comic_books|language|automotive|ice_hockey|exhibitions|opera|boxing|martial_arts|rail|games|time|tennis|spaceflight|zoos|amusement_parks|celebrities|interests|meteorology|conferences|digicams|engineering|fashion|radio|measurement_unit|skiing|bicycles|geology|comedy|physics)";
    private Pattern ontologyElementName = Pattern.compile("<http://rdf\\.basekb\\.com/ns/(" + ontologyElementPrefix + ".*)>");
    private Map<String, String> ontologyMap = new HashMap<String, String>();

    public ExtractGraphFromDumps(
            File inputDir,
            String inputFilesPattern,
            String luceneIndexDir,
            String ontologyKeysFilePath,
            String outputFilePath
    ) throws IOException {
        this.inputDir = inputDir;
        this.inputFilesPattern = Pattern.compile(inputFilesPattern);
        this.luceneIndexDir = luceneIndexDir;
        this.ontologyKeysFilePath = ontologyKeysFilePath;
        this.badIdsSearcher = new Searcher(this.luceneIndexDir + "/index_id_bad_nodes");
        this.outputFile = new File(outputFilePath);
        this.loadOntologyMap();
    }

    private void loadOntologyMap() throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(this.ontologyKeysFilePath), StandardCharsets.UTF_8));
        while(in.ready()) {
            String line = in.readLine();
            // line is like: "m.0000 \t aviation.aircraft_model.introduced"
            String[] fbNodeId_fbNodeTxt = line.split("\\t");
            this.ontologyMap.put(fbNodeId_fbNodeTxt[1], fbNodeId_fbNodeTxt[0]);
        }
        in.close();
    }

    // only one file output for each pattern
    protected void processDirectory() throws Exception {
        try {
            this.outputWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.outputFile), "utf-8"));
            // LS the dir, and get only the files that match the <inputFilesPattern>
            for (File inputFile : this.inputDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return inputFilesPattern.matcher(name).find();
                }
            })) {
                processFile(inputFile);
            }
        } catch (Exception e) {
            log("exception processing directory: " + this.inputDir.getName());
            throw e;
        } finally {
            if (this.outputWriter != null) this.outputWriter.close();
        }
    }

    protected static void log(String message) {
        System.out.println(message);
    }

    protected void processFile(File inputFile) throws Exception {
        log("processing file " + inputFile);
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8));
            String line = null;
            String[] s_p_o = null;
            for (int i=0; ; ++i) {
                if (i % 100000 == 0) {
                    log("\tProcessing line #" + (i+1));
                    this.outputWriter.flush();
                }
                line = in.readLine();
                if (line == null) break;
                s_p_o = line.split("\t");
                if (s_p_o.length > 2) {
                    this.processLine(s_p_o[0], s_p_o[1], s_p_o[2]);
                } else {
                    log("\n\nBAD INPUT LINE: " + line);
                }
            }
        } catch (Exception e) {
            log("exception processing file: " + this.inputDir.getName());
            throw e;
        } finally {
            if (in != null) in.close();
        }
        in.close();
    }

    /**
     * if both SUBJ and OBJ are good (considering ontology mapping)
     * (good == shape is m.xxxx and not in bad ids)
     * write to output file
     * @param subject
     * @param predictate
     * @param object
     * @throws IOException
     */
    protected void processLine(String subject, String predicate, String object) throws Exception {
        String subjectFbId = getFreebaseNodeId(subject);
        if (subjectFbId != null) {
            String objectFbId = getFreebaseNodeId(object);
            if (objectFbId != null) {
                this.outputWriter.write(subjectFbId + '\t' + predicate + '\t' + objectFbId + '\n');
            }
        }
    }

    /**
     * return m.xxxxx or null
     * @param fbNodeIdentifier
     * @return
     * @throws Exception
     */
    protected String getFreebaseNodeId(String fbNodeIdentifier) throws Exception {
        String fbShortId = null;
        Matcher freebaseIdMatcher = this.freebaseIdPattern.matcher(fbNodeIdentifier);
        if (freebaseIdMatcher.matches()) {
            fbShortId = freebaseIdMatcher.group(1);
        } else {
            // perhaps it is a concept/ontology_element
            String ontologyElementName = this.extractOntologyElementName(fbNodeIdentifier); // the interesting part of the URI
            if (ontologyElementName != null) {
                fbShortId = this.ontologyMap.get(ontologyElementName);
            }
        }
        if (fbShortId != null) {
            // it might be a bad id (we have an index of those, where: ID = old graph id; TITLE=fb id of the bad node)
            IndexItem badId = this.badIdsSearcher.findByTitleUnique(fbShortId);
            if (badId != null) {
                fbShortId = null;
            }
        }
        return fbShortId;
    }

    private String extractOntologyElementName(String fbNodeIdentifier) {
        Matcher ontologyElementMatcher = this.ontologyElementName.matcher(fbNodeIdentifier);
        if (ontologyElementMatcher.matches()) {
            return ontologyElementMatcher.group(1);
        } else {
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            log(
                "\n\nUsage: ExtractGraphFromDumps inputDir inputFilesPattern luceneIndexPath ontologyKeysFilePath outputFilePath\n" +
                "\tExample: ExtractGraphFromDumps ../unzipped/ keyNs*.txt ../lucene ../concepts/concept_mid_map.txt keyNs_new_graph.txt\n\n");
            return;
        }
        File inputDir = new File(args[0]);
        String inputFilesPattern = args[1];
        String luceneIndexPath = args[2];
        String ontologyKeysFilePath = args[3];
        String outputFilePath = args[4];
        
        ExtractGraphFromDumps graphExtractor = new ExtractGraphFromDumps(
                inputDir,
                inputFilesPattern,
                luceneIndexPath,
                ontologyKeysFilePath,
                outputFilePath);
        graphExtractor.processDirectory();
    }
}
