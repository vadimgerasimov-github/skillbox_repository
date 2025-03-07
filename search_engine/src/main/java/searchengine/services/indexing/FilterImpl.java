package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FilterImpl implements Filter {

    private final PageRepository pageRepository;

    public HashSet<String> getRelevantLinks(Set<String> links, String homePage, Site site) {

        return links.stream()
                .map(l -> l.replace("www.", ""))
                .filter(l -> (l.replace(homePage, "")).startsWith("/"))
                .filter(l -> l.startsWith(homePage))
                .filter(l -> !l.equals(homePage))
                .filter(l -> !pageRepository.existsByPathAndSite(l.replace(homePage, ""), site))
                .filter(l -> !l.matches(".*(sort|login|\\?|goout\\.php|lang|form|rss|#).*"))
                .filter(l -> !l.matches("^\\S+(\\.(?i)(jpe?g|png|gif|bmp|pdf|doc?x))$"))
                .map(l-> l.replaceAll("/$", ""))
                .collect(Collectors.toCollection(HashSet::new));
    }
}