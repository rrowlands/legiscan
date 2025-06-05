package us.poliscore.legiscan.view;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class UpdateBillsResult {
	
	protected List<LegiscanBillView> updatedBills = new ArrayList<LegiscanBillView>();
	
//	protected List<LegiscanPeopleView> updatedPeople = new ArrayList<LegiscanPeopleView>();
//	
//	protected List<LegiscanVoteView> updatedVotes = new ArrayList<LegiscanVoteView>();
	
}
