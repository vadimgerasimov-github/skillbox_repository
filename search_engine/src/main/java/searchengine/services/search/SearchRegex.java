package searchengine.services.search;

import lombok.Getter;

@Getter
public enum SearchRegex {

    IN_START_OF_SENTENCE("(?<!([А-ЯËA-Z]|гг)\\.\\s?)((?<=^|[.?!]\"?\\s?)[\"«]?\\b)WORD_TO_FIND(\\b.*?)"),
    IN_MIDDLE_OR_END_OF_SENTENCE
            ("((?<=^|[.?!•]\"?\\s?)[\"«]?[А-ЯËA-Z\\d]([^.!?•]|" +
            "((\\d{2}\\.\\d{2}\\.)?\\d{4}\\s?г\\.\\,?\\s[^А-ЯËA-Z])|(\\d{2}\\.\\d{2})|(т\\.[дк]\\.)(?!\\s[A-ZА-ЯË])|([А-ЯË]\\.){1,2}" +
            "\\s?|[а-яёa-z]\\.[а-яёa-z])*?«?\\b)WORD_TO_FIND(\\b.*?)"),
    AFTER_INTERPUNCT("•[^•]*?(\\b)WORD_TO_FIND(\\b)"),
    ANYWHERE("WORD_TO_FIND");

    private final String pattern;
    SearchRegex(String pattern) {
        this.pattern = pattern;
    }
    
}