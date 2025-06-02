package us.poliscore.legiscan.view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LegiscanSupplementView {

    @JsonProperty("supplement_id")
    private int supplementId;

    @JsonProperty("bill_id")
    private int billId;

    private String date;

    @JsonProperty("type_id")
    private int typeId;

    private String type;

    private String title;

    private String description;

    private String mime;

    @JsonProperty("mime_id")
    private int mimeId;

    @JsonProperty("supplement_size")
    private int supplementSize;

    @JsonProperty("supplement_hash")
    private String supplementHash;

    private String doc;
}
