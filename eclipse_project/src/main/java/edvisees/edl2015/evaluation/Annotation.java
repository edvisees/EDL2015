package edvisees.edl2015.evaluation;

import edvisees.edl2015.candidates.Fragment;

public class Annotation {
    public String text;
    public int startOffset;
    public int endOffset;
    public NerType nerType;
    public String freebaseId; // this can be NILxxx

    public Annotation(String text, int startOffset, int endOffset, NerType nerType, String freebaseId) {
        this.text = text;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.nerType = nerType;
        this.freebaseId = freebaseId;
    }

    public Annotation(Fragment fragment) {
        this(fragment.text,
             fragment.startOffset,
             fragment.endOffset,
             NerType.valueOf(fragment.nerType),
             fragment.freebaseId
        );
    }

    public boolean isInWindow(int thisOne, int anotherOne, int windowSize) {
        return thisOne - windowSize <= anotherOne && anotherOne <= thisOne + windowSize;
    }

    public boolean namedEntityMatches(Annotation another, int windowSize) {
        return this.text.equals(another.text) &&
                this.isInWindow(this.startOffset, another.startOffset, windowSize) &&
                this.isInWindow(this.endOffset, another.endOffset, windowSize);
    }

    public boolean linkMatches(Annotation another, int windowSize) {
        return this.namedEntityMatches(another, windowSize) &&
               (this.freebaseId.equals(another.freebaseId) ||
                  (this.freebaseId.startsWith("NIL") && another.freebaseId.startsWith("NIL")));
    }

    public boolean nerMatches(Annotation another, int windowSize) {
        return this.namedEntityMatches(another, windowSize) &&
               this.nerType.equals(another.nerType);
    }

    @Override
    public String toString() {
        return String.format("'%s' (%d, %d) %s %s", this.text, this.startOffset, this.endOffset, this.nerType, this.freebaseId);
    }
}
