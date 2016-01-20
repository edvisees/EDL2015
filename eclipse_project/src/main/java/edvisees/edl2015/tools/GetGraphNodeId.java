package edvisees.edl2015.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import edvisees.edl2015.lucene.IndexItem;
import edvisees.edl2015.lucene.Searcher;

public class GetGraphNodeId {

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("\n\n\n\n\tUsage: GetGraphNodeId input_file output_file lucene_index_by_fb_id \n\n\n\n");
            return;
        }
        File inputFile = new File(args[0]);
        File outputFile = new File(args[1]);
        String luceneIndexPath = args[2];
        Searcher searcher = new Searcher(luceneIndexPath);
        Map<String, String> goodTypes = new HashMap<String, String>();
        goodTypes.put("person.person", "m.123456");
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            System.out.println("processing '" + inputFile + "'");
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "utf-8"));
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "utf-8"));
            int i = 1;
            for (String fbId = reader.readLine(); fbId != null; fbId = reader.readLine(), i++) {
                if (i % 10000 == 0) System.out.println("processed line #" + i);
                Integer nodeIndex = null;
                try {
                    if (!fbId.startsWith("m.")) {
                        // look another way?
                        if (goodTypes.containsKey(fbId)) {
                            fbId = goodTypes.get(fbId);
                        } else {
                            System.out.println("id unknown: " + fbId);
                            continue;
                        }
                    }
                    IndexItem nodeId = searcher.findByTitleUnique(fbId);
                    nodeIndex = Integer.parseInt(nodeId.getId());
                    writer.write(nodeIndex + "\n");
                } catch (Exception e) {
                    System.out.println("error retrieving node index for '" + fbId + "'");
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.out.println("error processing bad ids");
            throw e;
        } finally {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
        }
    }

}
