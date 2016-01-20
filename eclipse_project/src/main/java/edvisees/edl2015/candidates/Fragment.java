package edvisees.edl2015.candidates;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Fragment implements Comparable<Fragment> {
    public String text;
    public int startOffset;
    public int endOffset;
    public Set<CandidateMeaning> meanings;
    public List<String> posTags;
    public String freebaseId;
    public String nerType;
    public String nerStanford = null;
    public String alternativeText = null;
    public Boolean isNIL = false;
    public Float finalScore;

    public Fragment(String text, int startOffset, int endOffset) {
        this(text, null, startOffset, endOffset);
    }
    
    public Fragment(String text, int startOffset) {
        this(text, null, startOffset, startOffset + text.length());
    }

    public Fragment(String text, String alternativeText, int startOffset, int endOffset) {
        this.text = text;
        this.alternativeText = alternativeText;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.meanings = new HashSet<CandidateMeaning>();
    }
    
    public Fragment(String text, String alternativeText, int startOffset) {
        this(text, alternativeText, startOffset, startOffset + text.length());
    }
    
    private Fragment(Fragment fragment) {
        this.text = fragment.text;
        this.alternativeText = fragment.alternativeText;
        this.startOffset = fragment.startOffset;
        this.endOffset = fragment.endOffset;
        this.meanings = new HashSet<CandidateMeaning>();
        this.posTags = fragment.posTags;
        this.freebaseId = fragment.freebaseId;
        this.nerStanford = fragment.nerStanford;
        this.nerType = fragment.nerType;
        this.isNIL = fragment.isNIL;
    }
    
    @Override
    public Fragment clone() {
        return new Fragment(this);
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public String toString() {
        return String.format("'%s' (%d, %d)", this.text, this.startOffset, this.endOffset);
    }

    @Override
    public int compareTo(Fragment otherFragment) {
        int deltaOffsets = this.startOffset - otherFragment.startOffset;
        if (deltaOffsets != 0) {
            return deltaOffsets;
        } else {
            // same starting offset -> choose by segment size
            return this.text.length() - otherFragment.text.length();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Fragment)) return false;
        Fragment otherFragment = (Fragment) obj;
        return this.text.equals(otherFragment.text) && this.startOffset == otherFragment.startOffset;
    }
    
    public boolean cover(Fragment otherFragment) {
        return this.startOffset <= otherFragment.startOffset && this.endOffset >= otherFragment.endOffset && !this.equals(otherFragment);
    }

    // TODO: refactor, this is horrible, use the TaggedWord class
    public String getWordsAndTags() {
        StringBuffer msg = new StringBuffer();
        String[] words = this.text.split("\\s");
        List<String> posTags = this.posTags;
        for (int i = 0; i < words.length; i++) {
            msg.append(words[i]).append("/").append((posTags == null ? "<POS>" : posTags.get(i))).append(" ");
        }
        return msg.toString();
    }

    public void setPosTags(List<TaggedWord> taggedWords) {
        this.posTags = new ArrayList<String>();
        for (TaggedWord taggedWord : taggedWords) {
            this.posTags.add(taggedWord.pos);
        }
    }
}
