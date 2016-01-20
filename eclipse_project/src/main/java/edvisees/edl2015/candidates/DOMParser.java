package edvisees.edl2015.candidates;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

public class DOMParser {
    public String content;
    public Fragment headline;
    public ArrayList<Fragment> paragraphs;
    public ArrayList<Fragment> authors;
    private static Pattern startsWithSpaces = Pattern.compile("^\\s+");
    private static Pattern authorPattern = Pattern.compile(".* author=\"([^\"].*)\" .*");
    private static Pattern quotePattern = Pattern.compile("(<quote[^>]*>([^<>]*)</quote>)");
    private static Pattern htmlPattern = Pattern.compile("(&lt;[^&]*&gt;)");
    private static Pattern allSpaces = Pattern.compile("\\p{Zs}");
    private static String goodUrlChars = "-a-zA-Z0-9+&@#/%?=~_|";
    private static Pattern urlPattern = Pattern.compile("\\b((?:https?|ftp|file)://[" + goodUrlChars + "!:,.;]*[" + goodUrlChars + "])");

    public DOMParser(String fileName) throws IOException{
        this(new File(fileName));
    }
    public DOMParser(File file) throws IOException{
        this.content = getContentAsString(file);
        if (file.toString().contains("NW")){
            this.headline = getHeadline("HEADLINE");
            this.paragraphs = getParagraphs("P");
            this.authors = new ArrayList<Fragment>();
        }
        else if(file.toString().contains("DF")){
            this.headline = getHeadline("headline");
            this.paragraphs = getPosts("post");
            this.authors = getAuthors("post");
        }
    }

    public static void main(String[] args) throws ParserConfigurationException, IOException {
        if (args.length > 0) {
            DOMParser parser = new DOMParser(args[0]);
            System.out.println(parser.headline.toString());
            for (Fragment paragraph: parser.paragraphs){
                System.out.println(paragraph.toString());
            }
        }
    }

    public List<Fragment> getFragments() {
        List<Fragment> fragments = new ArrayList<Fragment>();
        fragments.add(this.headline);
        fragments.addAll(this.paragraphs);
        return fragments;
    }

    public static String getContentAsString (File xmlFile) throws IOException{
        BufferedReader br = new BufferedReader(new FileReader(xmlFile));
        StringBuffer fileContents = new StringBuffer();
        String line = br.readLine();
        while (line != null) {
            fileContents.append(line + " ");
            line = br.readLine();
        }
        br.close();
        return allSpaces.matcher(fileContents.toString()).replaceAll(" ");
    }

    public Fragment getHeadline(String tag){
        String startTag = "<" + tag + ">";
        String endTag = "</" + tag + ">";
        int startOffSet = this.content.indexOf(startTag) + startTag.length() + 1;
        int endOffSet = this.content.indexOf(endTag) - 1;
        return new Fragment(this.content.substring(startOffSet, endOffSet), startOffSet, endOffSet);
    }

    public ArrayList<Fragment> getParagraphs(String tag){
        String startTag = "<" + tag + ">";
        String endTag = "</" + tag + ">";
        ArrayList<Integer> startOffSets = new ArrayList<Integer>();
        ArrayList<Integer> endOffSets = new ArrayList<Integer>();
        ArrayList<Fragment> temp = new ArrayList<Fragment>();
        for (int i = -1; (i = this.content.indexOf(startTag, i + 1)) != -1; ) {
            startOffSets.add(i + startTag.length() + 1);
        }
        for (int i = -1; (i = this.content.indexOf(endTag, i + 1)) != -1; ) {
            endOffSets.add(i - 1);
        }
        for (int i=0; i < startOffSets.size(); i++){
            String fragment = this.content.substring(startOffSets.get(i), endOffSets.get(i));
            if (fragment.trim().length() >  0) {
                temp.add(new Fragment(fragment, startOffSets.get(i), endOffSets.get(i)));
            }
        }
        return temp;
    }

    public ArrayList<Fragment> getPosts(String tag){
        String startTag = "<" + tag;
        String endTag = "</" + tag + ">";
        ArrayList<Integer> startOffSets = new ArrayList<Integer>();
        ArrayList<Integer> endOffSets = new ArrayList<Integer>();
        ArrayList<Fragment> postsWithoutQuote = new ArrayList<Fragment>();
        for (int i = -1; (i = this.content.indexOf(startTag, i + 1)) != -1; ) {
            int j = this.content.substring(i).indexOf(">") + i + 1;
            startOffSets.add(j + 1);
        }
        for (int i = -1; (i = this.content.indexOf(endTag, i + 1)) != -1; ) {
            endOffSets.add(i - 1);
        }
        for (int i=0; i < startOffSets.size(); i++){
            if (startOffSets.get(i) > endOffSets.get(i)) {
                continue;
            }
            String fragment = this.content.substring(startOffSets.get(i), endOffSets.get(i));
            if (fragment.trim().length() > 0) {
                int startOffset = startOffSets.get(i);
                Matcher matcher = startsWithSpaces.matcher(fragment);
                if (matcher.find()) {
//                    System.out.println("start offset because of spaces: " + matcher.group().length() + " -> " + fragment);
                    startOffset += matcher.group().length();
                }
                Fragment cleanFragment = this.getFragmentWithoutQuote(fragment.trim(), startOffset);
                if (cleanFragment.text.trim().length() > 0) {
                    postsWithoutQuote.add(cleanFragment);
                }
            }
        }
        return postsWithoutQuote;
    }

    public Fragment getFragmentWithoutQuote(String fragment, int startOffset){
        Matcher quoteMatcher = quotePattern.matcher(fragment);
        while (quoteMatcher.find()) {
            fragment = quoteMatcher.replaceFirst(this.getSpacesOfLenght(quoteMatcher.group(1).length()));
            quoteMatcher = quotePattern.matcher(fragment);
        }
        return new Fragment(this.removeURL(this.removeHTML(fragment)), startOffset);
    }

    public String removeHTML(String fragment){
        return removePattern(fragment, htmlPattern);
    }

    public String removeURL(String fragment){
        return removePattern(fragment, urlPattern);
    }

    public String removePattern(String fragment, Pattern pattern){
        Matcher matcher = pattern.matcher(fragment);
        while (matcher.find()) {
            fragment = matcher.replaceFirst(this.getSpacesOfLenght(matcher.group(1).length()));
            matcher = pattern.matcher(fragment);
        }
        return fragment;
    }

    private String getSpacesOfLenght(int numSpaces) {
        StringBuffer outputBuffer = new StringBuffer(numSpaces);
        for (int i = 0; i < numSpaces; i++){
           outputBuffer.append(" ");
        }
        return outputBuffer.toString();
    }

    public ArrayList<Fragment> getAuthors(String tag){
        String startTag = "<" + tag;
        ArrayList<Integer> startOffSets = new ArrayList<Integer>();
        ArrayList<Fragment> temp = new ArrayList<Fragment>();
        ArrayList<String> lines = new ArrayList<String>();
        for (int i = -1; (i = this.content.indexOf(startTag, i + 1)) != -1; ) {
            int j = this.content.substring(i).indexOf(">") + i + 1;
            String line = this.content.substring(i, j);
            lines.add(line);
            startOffSets.add(i);
        }
        for (int i=0; i < lines.size(); i++){
            Matcher matcher = authorPattern.matcher(lines.get(i));
            if (matcher.matches()) {
                String author = matcher.group(1);
                int startOffset = startOffSets.get(i) + lines.get(i).indexOf(author);
                Fragment f = new Fragment(author, startOffset);
                f.nerType = "PER";
                f.isNIL = true;
                f.freebaseId = "NIL";
                temp.add(f);
            } else if (lines.get(i).startsWith("<post")){
                System.out.println("bad author format: " + lines.get(i));
            } else {
                System.out.println("bad author format: " + lines.get(i));
                System.exit(0);
            }
        }
        return temp;
    }
}
