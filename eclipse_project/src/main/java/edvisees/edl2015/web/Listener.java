package edvisees.edl2015.web;

import it.unimi.dsi.webgraph.labelling.ArcLabelledImmutableGraph;

import java.io.File;

import javax.naming.InitialContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import edvisees.edl2015.graph.WeightedGraphBuilder;
import edvisees.edl2015.lucene.Searcher;
import edvisees.edl2015.weights.SemanticSignatureMap;

public class Listener implements ServletContextListener {

    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("\n\n\n\nInitializing Context ...");
        try {
            InitialContext ctx = new InitialContext();
            String runtimeDir = ((String) ctx.lookup("java:comp/env/runtimeDir")) + File.separator;
            String wGraphFileName = runtimeDir + "graph" + File.separator + "freebase_graph_weighted";
            String semSigGraphFileName = runtimeDir + "graph" + File.separator + "semsig" + File.separator + "semanticSignatureIgnoreIsolated";
            String luceneIndexPath = runtimeDir + "lucene";
            System.out.println("Using params:\n" +
                "\twGraphFileName = '" + wGraphFileName + "'\n" +
                "\tsemSigGraphFileName = '" + semSigGraphFileName + "'\n" +
                "\tluceneIndexPath = '" + luceneIndexPath + "'\n"
            );
            ArcLabelledImmutableGraph wGraph = WeightedGraphBuilder.deserializeWeightedGraph(wGraphFileName);
            ArcLabelledImmutableGraph semSigGraph = WeightedGraphBuilder.deserializeWeightedGraph(semSigGraphFileName);
            SemanticSignatureMap semSigMap = new SemanticSignatureMap(semSigGraph);
            Searcher idSearcher = new Searcher(luceneIndexPath + File.separator + "index_id");
            Searcher nameSearcher = new Searcher(luceneIndexPath + File.separator + "index_name");
            File badIdsSearcherFile = new File(luceneIndexPath + File.separator + "index_id_bad_ids");
            Searcher badIdsSearcher = badIdsSearcherFile.exists() ? new Searcher(badIdsSearcherFile.getAbsolutePath()) : null;
            ctx.rebind("java:comp/env/graph", wGraph);
            ctx.rebind("java:comp/env/semSigMap", semSigMap);
            ctx.rebind("java:comp/env/idSearcher", idSearcher);
            ctx.rebind("java:comp/env/nameSearcher", nameSearcher);
            ctx.rebind("java:comp/env/badIdsSearcher", badIdsSearcher);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("\n\n\n\nContext DESTROYED\n\n\n\n");
    }
}
