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

import edvisees.edl2015.lucene.IndexItem;
import edvisees.edl2015.lucene.Searcher;

/**
 * input: a file with one fb_id per line
 * output: a file with: "node_index TAB fb_id" per line
 *
 */
public class ExtractNodeIndexFromDumps {
    private File inputFile;
    private Searcher idSearcher;
    private File outputFile;

    public ExtractNodeIndexFromDumps(File inputFile, String luceneIndexDir, File outputFile) throws IOException {
        this.inputFile = inputFile;
        this.idSearcher = new Searcher(luceneIndexDir + File.separator + "index_id");
        this.outputFile = outputFile;
    }

    private String processLine(String freebaseId) throws Exception {
        IndexItem nodeIndexItem = this.idSearcher.findByTitleUnique(freebaseId);
        if (nodeIndexItem == null) {
            System.out.println(freebaseId + " not found in lucene");
            return "\n";
        } else {
            return nodeIndexItem.getId() + '\t' + freebaseId + '\n';
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
                String newOutput = this.processLine(line.trim());
                if (newOutput != null) {
                    out.write(newOutput);
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
            System.out.println("\n\nUsage: ExtractNodeIndexFromDumps inputFile luceneIndexPath outputFilePath\n");
            return;
        }
        File inputFile = new File(args[0]);
        String luceneIndexPath = args[1];
        File outputFile = new File(args[2]);
        ExtractNodeIndexFromDumps nodeIndexExtractor = new ExtractNodeIndexFromDumps(inputFile, luceneIndexPath, outputFile);
        nodeIndexExtractor.process();
    }
}
