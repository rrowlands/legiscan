
package us.poliscore.legiscan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LegiscanCommitteeView {
    
    @JsonProperty("committee_id")
    private Integer committeeId;
    
    @JsonProperty("committee_body")
    private String committeeBody;
    
    @JsonProperty("committee_name")
    private String committeeName;
}
