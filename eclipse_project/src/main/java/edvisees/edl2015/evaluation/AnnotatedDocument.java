package edvisees.edl2015.evaluation;

import java.util.ArrayList;
import java.util.List;

public class AnnotatedDocument {
    public String name;
    private List<Annotation> annotations;

    public AnnotatedDocument(String name) {
        this.name = name;
        this.annotations = new ArrayList<Annotation>();
    }

    public void addAnnotation(Annotation annotation) {
        this.annotations.add(annotation);
    }

    public List<Annotation> getAnnotations() {
        return this.annotations;
    }
}
