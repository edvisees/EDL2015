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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extract fb_ids of subjects whose:
 *      * predicate is: "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>"
 *      * object in: <http://rdf.basekb.com/ns/freebase.*>, <http://rdf.basekb.com/ns/type.attribution> etc etc
 */
public class GetFreebaseSupportEntitiesFromDumps {
    private File inputDir;
    private Pattern inputFilesPattern;
    private File outputFile;
    BufferedWriter outputWriter;
    private Pattern freebaseIdPattern = Pattern.compile("<http://rdf\\.basekb\\.com/ns/(m\\..*)>");
    private Pattern badPredicatePattern = Pattern.compile("<http://www\\.w3\\.org/1999/02/22\\-rdf\\-syntax\\-ns#type>");
    private Pattern badObjectPattern = Pattern.compile("<http://rdf\\.basekb\\.com/ns/(freebase\\.[^>]+|common\\.image|type\\.(attribution|content|content_import|domain|extension|enumeration|lang|media_type|namespace|permission|text_encoding|user|usergroup))>");

    public GetFreebaseSupportEntitiesFromDumps(
            File inputDir,
            String inputFilesPattern,
            String outputFilePath
    ) throws IOException {
        this.inputDir = inputDir;
        this.inputFilesPattern = Pattern.compile(inputFilesPattern);
        this.outputFile = new File(outputFilePath);
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
            // check predicate
            Matcher predicateMatcher = this.badPredicatePattern.matcher(predicate);
            if (predicateMatcher.matches()) {
                // check object
                Matcher badObjectMatcher = this.badObjectPattern.matcher(object);
                if (badObjectMatcher.matches()) {
                    this.outputWriter.write(subjectFbId + '\n');
                }
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
        }
        return fbShortId;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            log(
                "\n\nUsage: GetFreebaseSupportEntitiesFromDumps inputDir inputFilesPattern outputFilePath\n" +
                "\tExample: GetFreebaseSupportEntitiesFromDumps ../unzipped/ keyNs*.txt keyNs_new_graph.txt\n\n");
            return;
        }
        File inputDir = new File(args[0]);
        String inputFilesPattern = args[1];
        String outputFilePath = args[2];
        
        GetFreebaseSupportEntitiesFromDumps graphExtractor = new GetFreebaseSupportEntitiesFromDumps(
                inputDir,
                inputFilesPattern,
                outputFilePath);
        graphExtractor.processDirectory();
    }
}
