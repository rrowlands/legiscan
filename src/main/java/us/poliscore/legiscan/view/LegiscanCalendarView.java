package us.poliscore.legiscan.view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LegiscanCalendarView {

    @JsonProperty("type_id")
    private Integer typeId;

    @JsonProperty("type")
    private String type;

    @JsonProperty("date")
    private String date;

    @JsonProperty("time")
    private String time;

    @JsonProperty("location")
    private String location;

    @JsonProperty("description")
    private String description;
}
