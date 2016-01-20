package edvisees.edl2015.graph;
import it.unimi.dsi.webgraph.ArrayListMutableGraph;
import it.unimi.dsi.webgraph.BVGraph;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.Transform;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class BVGraphBuilder {

    /**
     * Reads the arcs from a txt file (assuming there are qNodes nodes), and stores an unlabeled graph using newBaseName.
     * If storeTranspose is true, also calculates and stores the transpose of the given graph.
     *
     * @param arcsFileName
     * @param qNodes
     * @param newBaseName
     * @param storeTranspose
     */
    private static void buildBVGraphFromArcsFile(String arcsFileName, int qNodes, String newBaseName, boolean storeTranspose)
        throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(arcsFileName), "utf-8"));
        System.out.println("constructing graph");
        ArrayListMutableGraph graph = new ArrayListMutableGraph(qNodes);
        System.out.println("Done.");
        String line = reader.readLine();
        int i = 0;
        while (line != null) {
            String[] tokens = line.split("\t");
            int from = Integer.valueOf(tokens[0]);
            int to = Integer.valueOf(tokens[1]);
            if (i % 100000 == 0) {
                System.out.print(i + " ");
            }
            if (from != to) {
                graph.addArc(from, to);
            }
            i++;
            line = reader.readLine();
        }
        reader.close();
        System.out.println("\nGraph loaded! " + graph.numNodes() + " nodes and " + graph.numArcs() + " arcs");
        ImmutableGraph immuGraph = graph.immutableView();
        System.out.print("Constructing transpose graph...");
        ImmutableGraph graph_transpose = Transform.transpose(immuGraph);
        // persist!
        System.out.println("Now, persist both graphs (original and transpose) to disc ...");
        BVGraph.store(immuGraph, newBaseName);
        BVGraph.store(graph_transpose, newBaseName + ".trans");
        System.out.println("Done!");
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println(
                "Usage: java BVGraphBuilder FILENAME NUM_NODES NEW_BASE_NAME \n" +
                "\tjava BVGraphBuilder ../graph/graph_sorted_unique.txt 81193150 freebase_graph"
            );
            return;
        }
        String fileName = args[0];
        int numOfNodes = Integer.valueOf(args[1]);
        String baseName = args[2];
        try {
            buildBVGraphFromArcsFile(fileName, numOfNodes, baseName, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
