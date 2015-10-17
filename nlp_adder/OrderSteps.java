package nlp_adder;

import java.util.ArrayList;

public class OrderSteps {

	private static ArrayList<String> allTenses = new ArrayList<>();
	public static LinguisticInfo order(LinguisticInfo info) {
		LinguisticInfo newInfo = new LinguisticInfo();
		newInfo.entities = info.entities;
		newInfo.owners = info.owners;
		newInfo.sentences = new ArrayList<>();
		allTenses = new ArrayList<>();
		boolean reduceFlag = false;
		String reduceEntity = ""; 
		int count = 0;
		for (LinguisticStep step : info.sentences) {
		    String tense = step.tense;
		    allTenses.add(tense);
			if (tense.equals("past") && allTenses.contains("present") && (step.procedureName == null || step.procedureName.isEmpty()) && !step.isQuestion)
				newInfo.sentences.add(0,step);
			else if (reduceFlag && step.entityName.equals(reduceEntity)) {
				newInfo.sentences.add(0,step);
				reduceFlag = false;
				continue;
			}
			else
				newInfo.sentences.add(step);
			if (step.procedureName != null && step.procedureName.equals("reduction") && count == 0) {
				reduceFlag = true;
				reduceEntity = step.entityName;
			}
			count++;
		}
		return newInfo;
	}
}
