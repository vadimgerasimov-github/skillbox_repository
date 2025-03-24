package searchengine.services.statistics;

import jico.Ico;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    @Override
    public StatisticsResponse getStatistics() {

        List<Site> configSites = sites.getSites();

        saveFavicons();

        TotalStatistics total = new TotalStatistics();
        total.setSites(configSites.size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = getDetailedStatisticsItems(configSites);
        total.setPages((int) pageRepository.count());
        total.setLemmas((int) lemmaRepository.count());

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);

        return response;
    }

    private List<DetailedStatisticsItem> getDetailedStatisticsItems(List<Site> configSites) {
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        for (Site site : configSites) {
            String url = site.getUrl();
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(url);
            Optional<searchengine.model.Site> siteModelOptional = siteRepository.findOneByUrl(url);
            if (siteModelOptional.isPresent()) {
                searchengine.model.Site siteEntity = siteModelOptional.get();
                item.setPages(pageRepository.countBySite(siteEntity));
                item.setLemmas(lemmaRepository.countBySite(siteEntity));
                item.setStatus(siteEntity.getSiteStatus().toString());
                item.setError(siteEntity.getLastError());
                item.setStatusTime(siteEntity.getDateTime().atZone(ZoneId.systemDefault())
                        .toInstant().toEpochMilli());
            } else {
                item.setPages(0);
                item.setLemmas(0);
                item.setStatus("");
                item.setError("");
                item.setStatusTime(System.currentTimeMillis());
            }
            detailed.add(item);
        }
        return detailed;
    }


    private void saveFavicons() {
        String directory = System.getProperty("user.home") + File.separator + "search_engine" + File.separator + "favicons";
        File externalDirectory = new File(directory);

        if (!externalDirectory.exists()) {
            boolean created = externalDirectory.mkdirs();
            if (!created) {
                log.error("Failed to create directory: {}", directory);
                return;
            }
        }

        List<String> favicons = Arrays.stream(Objects.requireNonNull(externalDirectory.listFiles()))
                .map(File::getName)
                .toList();

        for (Site site : sites.getSites()) {
            String favIconFileName = site.getName() + ".png";
            if (favicons.stream().anyMatch(n -> n.equals(favIconFileName))) {
                continue;
            }

            BufferedImage favicon = null;

            try {
                URL url = new URL(site.getUrl().concat("/favicon.ico"));
                favicon = Ico.read(url).get(0);
            } catch (Exception e) {
                try {
                    InputStream inputStream = StatisticsService.class.getResourceAsStream("/static/assets/favicons/websearch.png");
                    if (inputStream != null) {
                        favicon = ImageIO.read(inputStream);
                    } else log.error("Default favicon is not found");
                } catch (Exception e1) {
                    log.error("Error reading default favicon");
                }
            }
            if (favicon != null) {
                File file = new File(externalDirectory, favIconFileName);
                try {
                    ImageIO.write(favicon, "png", file);
                } catch (Exception e2) {
                    log.error("Error writing favicon");
                }
            }

        }

    }
}