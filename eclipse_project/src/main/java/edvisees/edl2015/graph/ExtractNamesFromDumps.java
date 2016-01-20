package edvisees.edl2015.graph;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

public class ExtractNamesFromDumps extends ExtractGraphFromDumps {
    //<http://www.w3.org/2000/01/rdf-schema#label>
    //<http://rdf.basekb.com/ns/common.topic.alias>
    //<http://rdf.basekb.com/ns/type.object.name>
    //<http://rdf.basekb.com/ns/base.schemastaging.context_name.official_name>
    private Pattern namePredicatePattern = Pattern.compile("<http://(www\\.w3\\.org/2000/01/rdf\\-schema#label|rdf\\.basekb\\.com/ns/(common\\.topic\\.alias|type\\.object\\.name|base\\.schemastaging\\.context_name\\.official_name))>");

    public ExtractNamesFromDumps(
            File inputDir,
            String inputFilesPattern,
            String luceneIndexDir,
            String ontologyKeysFilePath,
            String outputFilePath
    ) throws IOException {
        super(inputDir, inputFilesPattern, luceneIndexDir, ontologyKeysFilePath, outputFilePath);
    }

    @Override
    /**
     * if line is: good_subject name_predicate XXX
     * keep the "clean" line: m.xxxx \t pred \t name
     */
    protected void processLine(String subject, String predicate, String object) throws Exception {
        String subjectFbId = getFreebaseNodeId(subject);
        if (subjectFbId != null) {
            if (this.isNamePredicate(predicate)) {
                this.outputWriter.write(subjectFbId + '\t' + predicate + '\t' + object + '\n');
            }
        }
    }

    private boolean isNamePredicate(String predicate) {
        return this.namePredicatePattern.matcher(predicate).matches();
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            log(
                "\n\nUsage: ExtractNamesFromDumps inputDir inputFilesPattern luceneIndexPath ontologyKeysFilePath outputFilePath\n" +
                "\tExample: ExtractNamesFromDumps ../unzipped/ keyNs*.txt ../lucene ../concepts/concept_mid_map.txt keyNs_new_graph.txt\n\n");
            return;
        }
        File inputDir = new File(args[0]);
        String inputFilesPattern = args[1];
        String luceneIndexPath = args[2];
        String ontologyKeysFilePath = args[3];
        String outputFilePath = args[4];
        
        ExtractNamesFromDumps nameExtractor = new ExtractNamesFromDumps(
                inputDir,
                inputFilesPattern,
                luceneIndexPath,
                ontologyKeysFilePath,
                outputFilePath);
        nameExtractor.processDirectory();
    }
}

