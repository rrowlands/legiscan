package us.poliscore.legiscan.view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LegiscanMonitorView {
    private int bill_id;
    private String state;
    private String number;
    private int stance;
    private String change_hash;
    private String url;
    private String status_date;
    private int status;
    private String last_action_date;
    private String last_action;
    private String title;
    private String description;
}
