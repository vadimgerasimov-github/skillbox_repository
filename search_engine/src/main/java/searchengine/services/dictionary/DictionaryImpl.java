package searchengine.services.dictionary;

import lombok.Data;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Data
public class DictionaryImpl implements Dictionary {

    private final LuceneMorphology russianLuceneMorphology;
    private final LuceneMorphology englishLuceneMorphology;
    private static final Set<String> functionWords = Set.of("СОЮЗ", "ПРЕДЛ", "МЕЖД", "ЧАСТ", "CONJ", "PREP");

    public DictionaryImpl() throws IOException {
        this.russianLuceneMorphology = new RussianLuceneMorphology();
        this.englishLuceneMorphology = new EnglishLuceneMorphology();
    }

    @Override
    public String[] getWordsArray(String text) {
        text = text.replaceAll("[^А-ЯЁа-яёA-Za-z0-9]+", " ");
        return text.split("\\s+");
    }

    @Override
    public Map<String, Integer> getLemmaMap(String[] words) {
        Map<String, Integer> lemmaMap = new HashMap<>();
        for (String word : words) {
            String lemma;
            try {
                lemma = getSingleLemma(word);
                if (lemma.length() <= 1) {
                    continue;
                }
            } catch (Exception e) {
                continue;
            }
            lemmaMap.merge(lemma, 1, Integer::sum);
        }
        return lemmaMap;
    }

    @Override
    public String getSingleLemma(String word) {

        String wordLowerCase = word.toLowerCase().replaceAll("ё", "е");
        String resultWorld;

        if (wordLowerCase.matches("[а-яёА-ЯЁ]+")) {
            resultWorld = getLemma(russianLuceneMorphology, wordLowerCase);
        } else if (wordLowerCase.matches("[a-zA-Z]+")) {
            resultWorld = getLemma(englishLuceneMorphology, wordLowerCase);
        } else {
            resultWorld = wordLowerCase;
        }
        return resultWorld;
    }

    private String getLemma(LuceneMorphology luceneMorphology, String word) {

        List<String> morphInfo = luceneMorphology.getMorphInfo(word);

        if (isFunctionWord(morphInfo)) {
            return null;
        }

        List<String> lemmaList1 = luceneMorphology.getNormalForms(word);
        String firstLemmaType = lemmaList1.get(0);
        List<String> lemmaList2 = luceneMorphology.getNormalForms(firstLemmaType);

        if (lemmaList2.size() > 1) {
            List<String> morphInfos = lemmaList2.stream()
                    .map(bf -> luceneMorphology.getMorphInfo(firstLemmaType))
                    .flatMap(Collection::stream)
                    .distinct()
                    .toList();

            Optional<String> lemmaOptional = morphInfos.stream()
                    .filter(m -> m.contains("мр"))
                    .findAny();

            if (lemmaOptional.isPresent()) {
                return lemmaOptional.get().replaceAll("(\\|.*)", "");
            }

        }
        return lemmaList2.get(0);
    }

    private static boolean isFunctionWord(List<String> morphInfo) {
        for (String m : morphInfo) {
            List<String> morphProperties = List.of(m.split("\\s+"));
            if (!Collections.disjoint(morphProperties, functionWords)) {
                return true;
            }
        }
        return false;
    }

    public String removeTags(String content) {
        Document.OutputSettings settings = new Document.OutputSettings();
        settings.prettyPrint(false);
        Document document = Jsoup.parse(content);
        document.select("aside").remove();
        document.select("[class=*'navigation']").remove();
        document.select("body").forEach(b -> b.attr("class", ""));
        document.getAllElements()
                .stream()
                .filter(e -> e.attributes().asList().stream()
                        .anyMatch(attr -> attr.getValue().matches(".*(sale|promo).*")))
                .forEach(Element::remove);
        document.select("[class*='goback']").remove();
        document.select("h1, h2, h3, h4, h5, h6, li, p").after(" • ").unwrap();
        document.select("a, strong, b, i, em").unwrap();
        List<String> classesToRemoveList = new ArrayList<>(List.of("button", "btn", "sidebar", "cookies",
                "message", "referral", "about", "banner", "footer", "bottom", "menu", "subscribe", "newsletter",
                "error", "timer", "filter"));
        String classesToRemoveString = classesToRemoveList.stream()
                .map(c -> "[class*='" + c + "']")
                .collect(Collectors.joining(", "));
        document.select(classesToRemoveString).remove();
        document.select("[style*='hidden']").remove();
        document.select("button, footer, form, time, nav, header, br").remove();
        return Jsoup.clean(document
                .body()
                .html(), "", Safelist.simpleText(), settings);
    }

    public String getMetaDescription(String content) {
        Optional<String> metaDescription = Jsoup.parse(content).select("metaDescription[name=description]")
                .stream()
                .map(e -> e.attr("content"))
                .findFirst();
        return metaDescription.orElse("");
    }
}