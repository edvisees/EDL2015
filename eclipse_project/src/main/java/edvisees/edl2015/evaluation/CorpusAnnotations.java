package edvisees.edl2015.evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edvisees.edl2015.candidates.Fragment;

public class CorpusAnnotations {
    private Map<String, AnnotatedDocument> corpusAnnotations;

    public CorpusAnnotations(String inputFileName) throws Exception {
        // read the file and fill the corpus annotations
        this.corpusAnnotations = new HashMap<String, AnnotatedDocument>();
        File inputFile = new File(inputFileName);
        if (!inputFile.exists()) {
            throw new Exception("Wrong file: '" + inputFileName + "'");
        }
        this.processInputFile(inputFile);
    }

    public CorpusAnnotations(List<Fragment> fragments, String fileName) {
        this.corpusAnnotations = new HashMap<String, AnnotatedDocument>();
        AnnotatedDocument doc = new AnnotatedDocument(fileName);
        for (Fragment fragment : fragments) {
            doc.addAnnotation(new Annotation(fragment));
        }
        this.corpusAnnotations.put(fileName, doc);
    }

    private void processInputFile(File inputFile) throws Exception {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "utf-8"));
            while (reader.ready()) {
                String line = reader.readLine();
                String[] columns = line.split("\\t");
                this.processLine(columns);
            }
        } catch (Exception e) {
            System.out.println("error processing evaluation file.");
            throw e;
        } finally {
            if (reader != null) reader.close();
        }
    }

    // line format:
    // LDC  QUERY_ID  TEXT  FILE_NAME:1248-1257  NIL00001  FAC  NAM  1.0  Y  N  N
    private void processLine(String[] columns) {
        String text = columns[2];
        String[] docNameAndOffsets = columns[3].split(":"); // contains offset!
        String freebaseId = columns[4];
        NerType ner = NerType.valueOf(columns[5].toUpperCase());
        String docName = docNameAndOffsets[0];
        String[] offsets = docNameAndOffsets[1].split("\\-");
        int startOffset = Integer.parseInt(offsets[0]);
        int endOffset = Integer.parseInt(offsets[1]);
        this.getDocument(docName).addAnnotation(new Annotation(text, startOffset, endOffset, ner, freebaseId));
    }

    public Set<String> getDocuments() {
        return this.corpusAnnotations.keySet();
    }

    public AnnotatedDocument getDocument(String documentFileName) {
        if (!this.corpusAnnotations.containsKey(documentFileName)) {
            this.corpusAnnotations.put(documentFileName, new AnnotatedDocument(documentFileName));
        }
        return this.corpusAnnotations.get(documentFileName);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("usage: java CorpusAnnotation annotationFileName");
            System.exit(0);
        }
        String inputFileName = args[0];
        CorpusAnnotations ca = new CorpusAnnotations(inputFileName);
        for (String docName : ca.getDocuments()) {
            for (Annotation annotation : ca.getDocument(docName).getAnnotations()) {
                System.out.println(docName + " " + annotation);
            }
        }
    }
}
