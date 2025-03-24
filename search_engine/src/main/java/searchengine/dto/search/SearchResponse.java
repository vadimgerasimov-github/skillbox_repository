package searchengine.dto.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResponse {
    private boolean result;
    private String error;
    private Integer count;
    private List<SearchResult> data;

    public SearchResponse (boolean result, String error) {
        this.result = result;
        this.error = error;
    }
}