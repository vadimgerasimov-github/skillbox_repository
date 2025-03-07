package searchengine.services.indexing;

import org.springframework.stereotype.Service;
import searchengine.model.Site;

import java.util.HashSet;
import java.util.Set;

@Service
public interface Filter {
    HashSet<String> getRelevantLinks(Set<String> links, String homePage, Site site);
}
