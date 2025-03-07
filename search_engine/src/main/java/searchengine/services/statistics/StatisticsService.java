package searchengine.services.statistics;

import jico.ImageReadException;
import searchengine.dto.statistics.StatisticsResponse;

import java.io.IOException;

public interface StatisticsService {
    StatisticsResponse getStatistics() throws IOException, ImageReadException;
}
