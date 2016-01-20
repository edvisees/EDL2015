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
import java.util.HashSet;
import java.util.Set;

/**
 * This class is for taking a preliminary graph (without music, media, etc)
 * and a list of bad ids (file)
 * and then remove all the lines in the graph which contain those nodes.
 * 
 * 2 steps: 1st, load the bad ids; then, read the input graph and filter it.
 *
 * INPUT:
 *  * preliminaryGraph: file with the following format:
 *      * fb_id     full_fb_predicate       fb_id
 *  * badFbIds: a list of all the bad entities

 * OUTPUT:
 *  * same as input, but removing all lines whose Subject OR Object are in BAD_FB_IDS
 *
 * @author eeelnico
 *
 */
public class FilterPreliminaryGraph {
    private File inputFile;
    private File badIdsFile;
    private File outputFile;
    private Set<String> badIds = new HashSet<String>();
    BufferedWriter outputWriter;

    public FilterPreliminaryGraph(
            File inputFile,
            File badIdsFile,
            String outputFilePath
    ) throws IOException {
        this.inputFile = inputFile;
        this.badIdsFile = badIdsFile;
        this.outputFile = new File(outputFilePath);
    }

    protected static void log(String message) {
        System.out.println(message);
    }

    protected void process() throws Exception {
        this.loadBadIds();
        log("processing file " + this.inputFile);
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(this.inputFile), StandardCharsets.UTF_8));
            this.outputWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.outputFile), "utf-8"));
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
                this.processLine(s_p_o[0], s_p_o[1], s_p_o[2]);
            }
        } catch (Exception e) {
            log("exception processing file: " + inputFile.getName());
            throw e;
        } finally {
            if (in != null) in.close();
            if (this.outputWriter != null) this.outputWriter.close();
        }
    }

    private void loadBadIds() throws Exception {
        log("loading bad ids from file " + this.badIdsFile);
        BufferedReader badIdsReader = null;
        try {
            badIdsReader = new BufferedReader(new InputStreamReader(new FileInputStream(this.badIdsFile), StandardCharsets.UTF_8));
            String line = null;
            for (int i=0; ; ++i) {
                if (i % 100000 == 0) {
                    log("\tProcessing line #" + (i+1));
                }
                line = badIdsReader.readLine();
                if (line == null) break;
                this.badIds.add(line);
            }
        } catch (Exception e) {
            log("exception loading bad ids from file: " + this.badIdsFile.getName());
            throw e;
        } finally {
            if (badIdsReader != null) badIdsReader.close();
        }
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
        // if subject or object in bad ids, return null
        if (! this.badIds.contains(subject) && this.badIds.contains(object)) {
            // write to output file
            this.outputWriter.write(subject + '\t' + predicate + '\t' + object + '\n');
        }
    }


    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            log(
                "\n\nUsage: FilterPreliminaryGraph preliminaryGraphFile badIdsFile outputFile\n" +
                "\tExample: FilterPreliminaryGraph good_edges.sorted.txt freebase_support_fb_ids.unique.sorted.txt graph_4.0.txt\n\n");
            return;
        }
        File preliminaryGraphFile = new File(args[0]);
        File badIdsFile = new File(args[1]);
        String outputFilePath = args[2];
        
        FilterPreliminaryGraph graphExtractor = new FilterPreliminaryGraph(
                preliminaryGraphFile,
                badIdsFile,
                outputFilePath);
        graphExtractor.process();
    }
}
