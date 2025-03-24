package searchengine.dto.indexing;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude (JsonInclude.Include.NON_NULL)
public class IndexingResponse {
    private final boolean result;
    private String error;

    public IndexingResponse(boolean result) {
        this.result = result;
    }
    public IndexingResponse(boolean result, String error) {
        this.result = result;
        this.error = error;
    }
}