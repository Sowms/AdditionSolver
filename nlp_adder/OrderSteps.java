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
		for (LinguisticStep step : info.sentences) {
			String tense = step.tense; 
			if (tense.equals("past") && allTenses.contains("present") && (step.procedureName == null || step.procedureName.isEmpty()))
				newInfo.sentences.add(0,step);
			else
				newInfo.sentences.add(step);
			allTenses.add(tense);
		}
		return newInfo;
	}
}
