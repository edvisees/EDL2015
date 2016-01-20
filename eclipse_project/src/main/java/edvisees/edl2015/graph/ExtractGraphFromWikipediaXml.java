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
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

public class ExtractGraphFromWikipediaXml {
    private File inputFile;
    private BufferedReader inputReader;
    private String luceneIndexDir;
    private File graphFile;
    private File dictionaryFile;
    private BufferedWriter graphWriter;
    private BufferedWriter dictionaryWriter;
    private Map<String, Integer> dictionary = new HashMap<String, Integer>();
    private int wikipediaId = 0;

    public ExtractGraphFromWikipediaXml(
            File inputFile,
            String luceneIndexDir,
            String outputGraphFilePath,
            String outputDictionaryFilePath
    ) throws IOException {
        this.inputFile = inputFile;
        this.luceneIndexDir = luceneIndexDir;
        this.graphFile = new File(outputGraphFilePath);
        this.dictionaryFile = new File(outputDictionaryFilePath);
    }

    protected void processXml() throws Exception {
        try {
            this.graphWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.graphFile), "utf-8"));
            this.inputReader = new BufferedReader(new InputStreamReader(new FileInputStream(this.inputFile), StandardCharsets.UTF_8));

            // parse xml
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader parser = factory.createXMLStreamReader(this.inputReader);
            // for each article, for each link, output a graph line
            String tagName;
            Integer tagContentId = null, articleTitleId = null, linkId;
            for (int event = parser.next();
                event != XMLStreamConstants.END_DOCUMENT;
                event = parser.next()
            ) {
                switch (event) {
                    case XMLStreamConstants.START_DOCUMENT:
                        log("START DOCUMENT");
                        break;
                    case XMLStreamConstants.START_ELEMENT:
                        // D P T L
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        tagName = parser.getLocalName();
                        switch (tagName) {
                            case "t": // end of title
                                articleTitleId = tagContentId;
                                break;
                            case "p": // end of article
                                break;
                            case "l": // end of link -> output!
                                linkId = tagContentId;
                                this.graphWriter.write("" + articleTitleId + '\t' + linkId + '\n');
                                break;
                        }
                        break;
                    case XMLStreamConstants.CHARACTERS:
                        tagContentId = this.getTagContentId(parser);
                        break;
                }
            }
            parser.close();
            // now persist dict:
            this.dictionaryWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.dictionaryFile), "utf-8"));
            for (Map.Entry<String, Integer> article : this.dictionary.entrySet()) {
                this.dictionaryWriter.write("" + article.getValue() + '\t' + article.getKey() + '\n');
            }
        } catch (Exception e) {
            log("exception processing file: " + this.inputFile.getName());
            throw e;
        } finally {
            if (this.graphWriter != null) this.graphWriter.close();
            if (this.dictionaryWriter != null) this.dictionaryWriter.close();
            if (this.inputReader != null) this.inputReader.close();
        }
    }

    private int getTagContentId(XMLStreamReader parser) {
        String tagContent = parser.getText().trim();
        if (!this.dictionary.containsKey(tagContent)) {
            this.dictionary.put(tagContent, this.wikipediaId++);
        }
        return this.dictionary.get(tagContent);
    }

    protected static void log(String message) {
        System.out.println(message);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            log(
                "\n\nUsage: ExtractGraphFromWikipediaXml inputFile luceneIndexPath graphFilePath dictFilePath \n" +
                "\tExample: ExtractGraphFromWikipediaXml wiki.xml ../lucene new_graph.txt new_dict.txt \n\n");
            return;
        }
        File inputFile = new File(args[0]);
        String luceneIndexPath = args[1];
        String graphFilePath = args[2];
        String dictFilePath = args[3];
        ExtractGraphFromWikipediaXml graphExtractor = new ExtractGraphFromWikipediaXml(
                inputFile,
                luceneIndexPath,
                graphFilePath,
                dictFilePath);
        graphExtractor.processXml();
    }
}
