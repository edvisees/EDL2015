package edvisees.edl2015.web;

import it.unimi.dsi.webgraph.labelling.ArcLabelledImmutableGraph;

import java.io.IOException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edvisees.edl2015.graph.WeightedGraphBuilder;
import edvisees.edl2015.lucene.Searcher;

public class GraphServlet extends HttpServlet {
    private static final long serialVersionUID = -2051584475642415677L;

    @Override
    public void init() throws ServletException {
        super.init();
        System.out.println("\n\n\n\nGraphServlet INIT\n\n\n\n");
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println("<h1>Graph Servlet</h1>");
        response.getWriter().println("session id = " + request.getSession(true).getId());
        try {
            InitialContext ctx = new InitialContext();
            ArcLabelledImmutableGraph wGraph = (ArcLabelledImmutableGraph) ctx.lookup("java:comp/env/graph");
            Searcher idSearcher = (Searcher) ctx.lookup("java:comp/env/idSearcher");
            Searcher nameSearcher = (Searcher) ctx.lookup("java:comp/env/nameSearcher");

            // TODO: iterate in more like a web way
            response.getWriter().println("<br>Graph has " + wGraph.numNodes() + " nodes and " + wGraph.numArcs() + " arcs.<br>");
            String iterationMsg = "";
            try {
                iterationMsg = WeightedGraphBuilder.iterateWeightedGraph(wGraph, idSearcher, nameSearcher);
            } catch (Exception e) {
                iterationMsg = "ERROR: " + e.getMessage();
                e.printStackTrace();
            }
            iterationMsg = iterationMsg.replaceAll("\n", "<br>");
            response.getWriter().println(iterationMsg);
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }
}
