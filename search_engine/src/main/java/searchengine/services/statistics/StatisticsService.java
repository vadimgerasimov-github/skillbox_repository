package searchengine.services.statistics;

import org.springframework.transaction.annotation.Transactional;
import searchengine.dto.statistics.StatisticsResponse;

public interface StatisticsService {
    @Transactional()
    StatisticsResponse getStatistics();
}