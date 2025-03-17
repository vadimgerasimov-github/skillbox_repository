package searchengine.services.snippet;

import lombok.Data;
import lombok.NoArgsConstructor;
import searchengine.services.search.SearchRegex;

import java.util.Comparator;
import java.util.Set;

@NoArgsConstructor
@Data
public class Snippet {
    String text;
    String lines;
    int wordsCount;
    int distanceBetweenWords;
    int lemmasCount;
    int startOfSentence;
    SearchRegex searchRegex;

    public static final Comparator<Snippet> COMPARATOR =
            Comparator.comparing((Snippet s) -> s.lemmasCount).reversed()
                    .thenComparing(Snippet::getSearchRegex, Comparator.comparingInt(Snippet::getSearchRegexComparison))
                    .thenComparing(s -> s.distanceBetweenWords)
                    .thenComparing(Comparator.comparing(Snippet::getWordsCount).reversed());

    private static int getSearchRegexComparison(SearchRegex searchRegex) {
        return switch (searchRegex) {
            case IN_START_OF_SENTENCE -> 1;
            case IN_MIDDLE_OR_END_OF_SENTENCE -> 2;
            case AFTER_INTERPUNCT -> 3;
            case ANYWHERE -> 4;
        };
    }

    @Override
    public String toString() {
        return "Snippet:" + "\n" + lines + "\n" +
                "* lemmasCount = " + lemmasCount + "\n" +
                "* searchRegex = " + searchRegex + "\n" +
                "* distanceBetweenWords = " + distanceBetweenWords + "\n" +
                "* countOfQueryWords = " + wordsCount + "\n" +
                "length = " + lines.length()
                ;
    }
}