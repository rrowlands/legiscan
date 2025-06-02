
package us.poliscore.legiscan.view;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LegiscanResponse {
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("alert")
    private LegiscanAlert alert;
    
    @JsonProperty("bill")
    private LegiscanBillView bill;
    
    @JsonProperty("person")
    private LegiscanLegislatorView person;
    
    @JsonProperty("rollcall")
    private LegiscanRollCallView rollcall;
    
    @JsonProperty("bills")
    private List<LegiscanBillView> bills;
    
    @JsonProperty("people")
    private List<LegiscanLegislatorView> people;
    
    @JsonProperty("rollcalls")
    private List<LegiscanRollCallView> rollcalls;
    
    @JsonProperty("text")
    private LegiscanBillTextView text;
    
    @JsonProperty("sessions")
    private List<LegiscanSessionView> sessions;
    
    @JsonProperty("masterlist")
    private LegiscanMasterListView masterlist;
    
    private LegiscanAmendmentView amendment;
    
    private LegiscanSupplementView supplement;
    
    private LegiscanSearchView searchresult;
    
    private List<LegiscanDatasetView> datasetlist;
    
    private LegiscanDatasetView dataset;
    
    private List<LegiscanLegislatorView> sessionpeople;
    
    private LegiscanMasterListView monitorlist;
    
    private List<LegiscanSponsoredBillView> sponsoredbills;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LegiscanAlert {
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("message")
        private String message;
    }
}
