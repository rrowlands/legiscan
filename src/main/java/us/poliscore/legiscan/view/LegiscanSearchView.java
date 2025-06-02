package us.poliscore.legiscan.view;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LegiscanSearchView {

    private Summary summary;

    private Map<String, Result> results = new HashMap<>();

    @JsonAnySetter
    public void addResult(String key, Object value) {
        if (!"summary".equals(key) && value instanceof Map) {
            // Let Jackson handle conversion in a clean way
            Result result = new Result((Map<?, ?>) value);
            results.put(key, result);
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Summary {
        private String page;
        private String range;
        private String relevancy;
        private int count;

        @JsonProperty("page_current")
        private int pageCurrent;

        @JsonProperty("page_total")
        private int pageTotal;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private int relevance;
        private String state;

        @JsonProperty("bill_number")
        private String billNumber;

        @JsonProperty("bill_id")
        private int billId;

        @JsonProperty("change_hash")
        private String changeHash;

        private String url;

        @JsonProperty("text_url")
        private String textUrl;

        @JsonProperty("research_url")
        private String researchUrl;

        @JsonProperty("last_action_date")
        private String lastActionDate;

        @JsonProperty("last_action")
        private String lastAction;

        private String title;

        // Constructor that maps from a generic map (safely, with checks)
        public Result(Map<?, ?> map) {
            this.relevance = getInt(map.get("relevance"));
            this.state = getString(map.get("state"));
            this.billNumber = getString(map.get("bill_number"));
            this.billId = getInt(map.get("bill_id"));
            this.changeHash = getString(map.get("change_hash"));
            this.url = getString(map.get("url"));
            this.textUrl = getString(map.get("text_url"));
            this.researchUrl = getString(map.get("research_url"));
            this.lastActionDate = getString(map.get("last_action_date"));
            this.lastAction = getString(map.get("last_action"));
            this.title = getString(map.get("title"));
        }

        private String getString(Object val) {
            return val != null ? val.toString() : null;
        }

        private int getInt(Object val) {
            if (val instanceof Number n) {
                return n.intValue();
            } else if (val instanceof String s) {
                try {
                    return Integer.parseInt(s);
                } catch (NumberFormatException ignored) {}
            }
            return 0;
        }
    }
}
