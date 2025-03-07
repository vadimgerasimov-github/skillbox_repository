package searchengine.services.dictionary;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public interface Dictionary {
    String[] getWordsArray(String text);
    String getSingleLemma(String word);
    Map<String, Integer> getLemmaMap(String[] text);
    String removeTags(String text);
    String getMetaDescription(String content);
}
