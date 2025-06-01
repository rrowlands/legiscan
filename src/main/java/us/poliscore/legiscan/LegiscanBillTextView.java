package us.poliscore.legiscan;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LegiscanBillTextView {

    private String status;
    private Text text;

    @Data
    public static class Text {
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
}

