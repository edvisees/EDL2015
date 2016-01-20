package edvisees.edl2015.lucene;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Main {
    private static final int defaultResultSize = 2000;
    private File inputFile;
    private File indexDir; // directory where we are gonna store the index
    private Indexer indexer;

    // input file={node_index \t fb_id}
    // (node_index should start with 0)
    public Main(File inputFile, File indexDir) {
        this.inputFile = inputFile;
        this.indexDir = indexDir;
    }

    private void indexFile() throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(this.inputFile), StandardCharsets.UTF_8));
        System.out.println("Creating index:\n\t * in '" + this.indexDir + "'\n\t * with data from '" + this.inputFile + "'");
        this.indexer = new Indexer(this.indexDir.getAbsolutePath());
        String line = null;
        String[] pair = null;
        for (int i=0; ; ++i) {
            line = in.readLine();
            if (line == null) break;
            pair = line.split("\t");
            if (pair.length == 2 && pair[1].length() > 0) {
                IndexItem item = new IndexItem(Integer.parseInt(pair[0]) + "", pair[1]);
                if (i % 10000 == 0) {
                    System.out.println("\tIndexing item #" + i + ": " + item);
                }
                this.indexer.index(item);
            }
        }
        in.close();
        this.indexer.close();
        System.out.println("Finished adding the input file to Index");
    }

    private  void searchIndex() throws Exception {
        Searcher searcher = new Searcher(this.indexDir.getAbsolutePath());
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Type Q/q to quit.");
        do {
            System.out.print("Enter 'id: 123' or 'title: obama' or i:123 / t:obama: ");
            String input = reader.readLine();
            if (input.equalsIgnoreCase("q")) {
                break;
            } else {
                try {
                    String[] query = input.split(":");
                    String realQuery = query[1].trim();
                    List<IndexItem> result = null;
                    if (query[0].equals("title") || query[0].equals("t")) {
                        System.out.println("find by title: '" + realQuery + "'");
                        result = searcher.findByTitle(realQuery, defaultResultSize);
                    } else if (query[0].equals("id") || query[0].equals("i")) {
                        System.out.println("find by id: '" + realQuery + "'");
                        result = searcher.findById(realQuery, defaultResultSize);
                    } else {
                        System.out.println("unknown action: '" + query[0] + "'");
                        continue;
                    }
                    print(result);
                } catch (Exception e) {
                    System.out.println("bad input: " + e.getMessage());
                }
            }
        } while (true);
    }

    private static void print(List<IndexItem> result) {
        System.out.println("Result Size: " + result.size());
        for (IndexItem item : result) {
            System.out.println(item);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("usage: Main inputFile indexDir");
            return;
        }
        File inputFile = new File(args[0]);
        File indexDir = new File(args[1]);
        Main main = new Main(inputFile, indexDir);
        main.indexFile();
        main.searchIndex();
    }

}
