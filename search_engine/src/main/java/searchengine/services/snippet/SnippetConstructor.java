package searchengine.services.snippet;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import searchengine.config.SnippetSettings;
import searchengine.services.dictionary.Dictionary;
import searchengine.services.search.SearchRegex;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Data
@Slf4j
@Component
@NoArgsConstructor
public class SnippetConstructor {
    private  String content;
    private  Set<String> queryLemmas;
    private  Dictionary dictionary;
    private  SnippetSettings snippetSettings;
    private String formattedText;
    private int pageTextPixelWidth;
    private static final FontMetrics metrics = getMetrics();

    public SnippetConstructor(String content, Set<String> queryLemmas, Dictionary ddictionary, SnippetSettings snippetSettings){
        this.content = content;
        this.queryLemmas = queryLemmas;
        this.dictionary = ddictionary;
        this.snippetSettings = snippetSettings;
    }

    public String getSnippet() {

        String pageText = dictionary.removeTags(content);
        formattedText = format(pageText);

        setPageTextPixelWidth(getPixelWidth(formattedText));

        List<Word> words = findQueryWordsInText();
        List<Snippet> snippets = getSnippets(words);
        setSnippetsProperties(snippets, words, formattedText);
        snippets.sort(Snippet.COMPARATOR);

        Snippet metaSnippet = getMetaSnippet();
        Snippet snippet = metaSnippet.getLemmasCount() >= snippets.get(0).getLemmasCount() ? metaSnippet : snippets.get(0);

        return getDecoratedSnippet(snippet);
    }

    private List<Snippet> getSnippets(List<Word> words) {
        List<Snippet> snippets = new ArrayList<>();
        for (Word word : words) {
            Snippet snippet = new Snippet();
            int startOfText = Math.max(word.getWordStart() - 150, 0);
            int endOfText = Math.min(word.getWordEnd() + 150, formattedText.length());
            String textFragment = formattedText.substring(startOfText, endOfText);
            snippet.setText(textFragment);
            findSentence(snippet, word.getWord(), startOfText);
            setLines(snippet);
            snippets.add(snippet);
        }
        return snippets;
    }

    private void findSentence(Snippet snippet, String word, int start) {

        for (SearchRegex searchRegex : SearchRegex.values()) {
            String regex = searchRegex.getPattern().replaceAll("(\\b)WORD_TO_FIND(\\b)", word);
            Pattern pattern = Pattern.compile(regex, Pattern.UNICODE_CHARACTER_CLASS);
            Matcher matcher = pattern.matcher(snippet.getText());
            if (matcher.find()) {
                snippet.setStartOfSentence(matcher.start() + start);
                snippet.setSearchRegex(searchRegex);
                break;
            }
        }
    }

    private void setLines(Snippet snippet) {

        if (getPageTextPixelWidth() < snippetSettings.getMaxPixelWidth()) {
            snippet.setLines(formattedText);
            return;
        }

        int startOfSentence = snippet.getStartOfSentence();
        int frLength = getPixelWidth(formattedText.substring(startOfSentence));
        boolean snippetHasSuitableLength =
                frLength >= snippetSettings.getMaxPixelWidth();

        if (snippetHasSuitableLength) {
            snippet.setText(formattedText.substring(startOfSentence));
            findLines(snippet);
        } else {
            snippet.setText(formattedText);
            String reversedText = new StringBuilder(snippet.getText()).reverse().toString();
            snippet.setText(reversedText);
            findLines(snippet);
            snippet.setLines(new StringBuilder(snippet.getLines()).reverse().toString());
        }
    }

    private void setMetaLemmasCount(Snippet metaSnippet) {
        String[] metaWords = dictionary.getWordsArray(metaSnippet.getLines());
        Set<String> metaLemmas = Arrays.stream(metaWords).map(dictionary::getSingleLemma).collect(Collectors.toSet());
        metaLemmas.retainAll(queryLemmas);
        metaSnippet.setLemmasCount(metaLemmas.size());
    }

    private void setSnippetsProperties(List<Snippet> snippets, List<Word> wordsList, String formattedTex) {

        snippets.forEach(snippet -> {
            int snippetStartIndex = formattedTex.indexOf(snippet.getLines());
            int snippetEndIndex = formattedTex.indexOf(snippet.getLines()) + snippet.getLines().length();

            List<Word> queryWordsFoundInSnippet = wordsList.stream()
                    .filter(words -> words.getWordStart() >= snippetStartIndex &&
                            words.getWordEnd() <= snippetEndIndex)
                    .toList();

            snippet.setWordsCount(queryWordsFoundInSnippet.size());

            int commonDistance = 0;
            for (int i = 1; i < queryWordsFoundInSnippet.size(); i++) {
                commonDistance += queryWordsFoundInSnippet.get(i).getWordStart() -
                        queryWordsFoundInSnippet.get(i - 1).getWordEnd();
            }
            snippet.setDistanceBetweenWords(commonDistance);

            Map<String, Integer> lemmaMap = new HashMap<>();
            queryWordsFoundInSnippet.forEach(words -> lemmaMap.merge(words.lemma, 1, Integer::sum));
            snippet.setLemmasCount(lemmaMap.size());
        });

    }

    private void findLines(Snippet snippet) {
        String text = snippet.getText();
        StringBuilder lines = new StringBuilder();
        Pattern pattern = Pattern.compile("•*\\s*\\S*\\s*");
        Matcher matcher = pattern.matcher(text);

        int currentPixelWidth = 0;

        while (matcher.find()) {
            String nextFragment = matcher.group();
            int nextFragmentPixelWidth = getPixelWidth(nextFragment);

            if (currentPixelWidth + nextFragmentPixelWidth > snippetSettings.getMaxPixelWidth()) {
                break;
            }

            lines.append(nextFragment);
            currentPixelWidth += nextFragmentPixelWidth;
        }

        snippet.setLines(lines.toString());
    }

    private List<Word> findQueryWordsInText() {
        List<Word> queryWordsInText = new ArrayList<>();
        String wordRegex = "[A-Za-zА-ЯËа-яё0-9]*";
        Pattern pattern = Pattern.compile(wordRegex);
        Matcher matcher = pattern.matcher(formattedText);
        while (matcher.find()) {
            String word = matcher.group();
            String lemma = dictionary.getSingleLemma(word);
            if (lemma == null) {
                continue;
            }
            for (String queryLemma : queryLemmas) {
                if (lemma.equals(queryLemma)) {
                    Word foundWord = new Word();
                    foundWord.setWord(word);
                    foundWord.setLemma(lemma);
                    foundWord.setWordStart(matcher.start());
                    foundWord.setWordEnd(matcher.end());
                    queryWordsInText.add(foundWord);
                }
            }
        }
        return queryWordsInText;
    }

    private static FontMetrics getMetrics() {
        try {

            InputStream inputStream = SnippetConstructor.class
                    .getResourceAsStream("/static/assets/fonts/Montserrat/Montserrat-Thin.ttf");

            assert inputStream != null;
            Font montserrat = Font.createFont(Font.PLAIN, inputStream).deriveFont(14F);
            JLabel jLabel = new JLabel();
            jLabel.setFont(montserrat);
            return jLabel.getFontMetrics(montserrat);
        } catch (FontFormatException | IOException e) {
            throw new RuntimeException("Font not found");
        }

    }
    
    private String format(String text) {
        return text.replaceAll("&nbsp;", " ")
                .replaceAll("\\s{2,}", "\n")
                .replaceAll("(?<=[а-яё])(?=[А-ЯË])", " ")
                .replaceAll("\n", " • ")
                .replaceAll("(\\s•)*(?=\\s•)", "")
                .replaceAll("(?<=[.,?!;:—])\\s•", "")
                .replaceAll("(?<=•\\s)([—•;:]\\s*)", "")
                .replaceAll("(?<=•.{1,5})•\\s*", "")
                .replaceAll("(?<=[а-яёa-z]\\.)(?=[А-ЯËA-Z])", " ")
                .trim();
    }

    private int getPixelWidth(String string) {
        return metrics.stringWidth(string);
    }

    private Snippet getMetaSnippet() {

        String metaDescription = dictionary.getMetaDescription(content);
        metaDescription = format(metaDescription);
        Snippet metaSnippet = new Snippet();
        metaSnippet.setText(metaDescription);
        findLines(metaSnippet);
        setMetaLemmasCount(metaSnippet);

        if (getPixelWidth(metaSnippet.getLines()) < (snippetSettings.getMaxPixelWidth())) {
            metaSnippet.setText(metaSnippet.getLines().concat(formattedText.substring(0, Math.min(1000, formattedText.length()))));
            findLines(metaSnippet);
        }

        return metaSnippet;
    }

    private String getDecoratedSnippet(Snippet snippet) {
        String lines = snippet.getLines();

        boolean textEndsWithLines = (formattedText.indexOf(lines) + lines.length()) == formattedText.length();

        lines = lines.replaceAll("^[\\s;]*•|•[\\s;]*(\\z|$)", "");
        lines = addEllipsis(lines, snippet.getSearchRegex(), textEndsWithLines);
        lines = highlightQueryWords(lines);

        return lines;
    }


    private String highlightQueryWords(String lines) {
        String[] snippetWords = dictionary.getWordsArray(lines);
        for (String snippetWord : snippetWords) {
            String snippetLemma = dictionary.getSingleLemma(snippetWord);
            if (snippetLemma == null) continue;
            if (getQueryLemmas().stream().anyMatch(snippetLemma::equals)) {
                String regex = "(?<!(<b>)\\b)" + snippetWord + "\\b(?!(</b>))";
                Pattern pattern = Pattern.compile(regex, Pattern.UNICODE_CHARACTER_CLASS);
                Matcher matcher = pattern.matcher(lines);
                lines = matcher.replaceAll("<b>" + snippetWord + "</b>");
            }
        }
        lines = lines.replaceAll("(</b> <b>)", " ");
        return lines;
    }

    private String addEllipsis(String lines, SearchRegex searchRegex, boolean textEndsWithLines) {
        boolean smallPageText = pageTextPixelWidth < snippetSettings.getMaxPixelWidth();
        if (smallPageText) {
            lines = lines.replaceAll("(?<=[^.?!])\\z", ".");
            lines = lines.replaceAll("(?<=\\S)\\s(?=\\S$)", "");
            return lines;
        }
        if (!textEndsWithLines) {
            lines = lines.replaceAll("[^\\dа-яёa-zА-ЯËA-Z]*$", "")
                    .concat("...");
        }
        lines = lines.trim();
        if (searchRegex.equals(SearchRegex.ANYWHERE) || Character.isLowerCase(lines.charAt(0))) {
            lines = "...".concat(lines);
        }
        int firstLetter = lines.charAt(0);
        if (Character.isLowerCase(firstLetter)) {
            lines = lines.replaceFirst(String.valueOf(firstLetter), String.valueOf(Character.toUpperCase(firstLetter)));
        }
        if (textEndsWithLines) {
            lines = lines.replaceAll("(?<=[^.?!])\\z", ".");
        }
        return lines;
    }

    @Data
    private static class Word {
        private String word;
        private String lemma;
        private int wordStart;
        private int wordEnd;
    }

}
