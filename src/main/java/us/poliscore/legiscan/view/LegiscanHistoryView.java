package us.poliscore.legiscan.view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LegiscanHistoryView {

    @JsonProperty("date")
    private String date;

    @JsonProperty("action")
    private String action;

    @JsonProperty("chamber")
    private String chamber;

    @JsonProperty("chamber_id")
    private Integer chamberId;

    @JsonProperty("importance")
    private Integer importance;
}
