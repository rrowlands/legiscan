
package us.poliscore.legiscan.view;

import java.util.List;
import java.util.Map;

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
    private LegiscanPeopleView person;
    
    @JsonProperty("roll_call")
    private LegiscanRollCallView rollcall;
    
    @JsonProperty("bills")
    private List<LegiscanBillView> bills;
    
    @JsonProperty("people")
    private List<LegiscanPeopleView> people;
    
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
    
    private List<LegiscanPeopleView> sessionpeople;
    
    private List<LegiscanSponsoredBillView> sponsoredbills;
    
    private Map<String, LegiscanMonitorView> monitorlist;
    
    @JsonProperty("return")
    private Map<String, String> returnMap;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LegiscanAlert {
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("message")
        private String message;
    }
}
