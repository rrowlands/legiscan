package us.poliscore.legiscan.view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LegiscanBillTextView {

	@JsonProperty("doc_id")
    private int docId;

    @JsonProperty("bill_id")
    private int billId;

    private String date;
    private String type;

    @JsonProperty("type_id")
    private int typeId;

    private String mime;

    @JsonProperty("mime_id")
    private int mimeId;

    @JsonProperty("text_size")
    private int textSize;

    @JsonProperty("text_hash")
    private String textHash;

    private String doc; // A base64 encoded string
}

