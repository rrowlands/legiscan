
package us.poliscore.legiscan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LegiscanSupplementView {
    // Supplement fields can be added based on API documentation
}
