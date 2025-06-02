package us.poliscore.legiscan.view;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LegiscanMasterListView {

    private Map<String, BillSummary> bills = new HashMap<>();

    @JsonAnySetter
    public void addBill(String key, BillSummary value) {
        bills.put(key, value);
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BillSummary {
        @JsonProperty("bill_id")
        private int billId;

        private String number;

        @JsonProperty("change_hash")
        private String changeHash;

        private String url;

        @JsonProperty("status_date")
        private String statusDate;

        private String status;

        @JsonProperty("last_action_date")
        private String lastActionDate;

        @JsonProperty("last_action")
        private String lastAction;

        private String title;
        private String description;
    }
}
