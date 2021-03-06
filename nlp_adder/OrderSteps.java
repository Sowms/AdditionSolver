package nlp_adder;

import java.util.ArrayList;

public class OrderSteps {

	private static ArrayList<String> allTenses = new ArrayList<>();
	private static ArrayList<String> allVerbs = new ArrayList<>();
	public static LinguisticInfo order(LinguisticInfo info) {
		LinguisticInfo newInfo = new LinguisticInfo();
		newInfo.entities = info.entities;
		newInfo.owners = info.owners;
		newInfo.sentences = new ArrayList<>();
		allTenses = new ArrayList<>();
		allVerbs = new ArrayList<>();
		boolean reduceFlag = false;
		
		String reduceEntity = ""; 
		int count = 0;
		for (LinguisticStep step : info.sentences) {
		    String tense = step.tense;
		    if (tense.equals("past") && allTenses.contains("present") && (step.procedureName == null || step.procedureName.isEmpty()) && !step.isQuestion)
				newInfo.sentences.add(0,step);
		    boolean checkVerb = false;
		    if (!step.verbQual.equals("has") && !allVerbs.contains(step.verbQual))
		    	checkVerb = true;
		    else if (step.verbQual.equals("has"))
		    	checkVerb = true;
			if (tense.equals("past") && checkVerb && allTenses.contains("present") && !allTenses.contains("past") && step.isQuestion) {
				System.err.println(step.entityName);
			    
				LinguisticStep temp = new LinguisticStep();
				temp.entityName = step.entityName;
				temp.owner1 = step.owner1;
				temp.owner2 = step.owner2;
				temp.entityValue = "some";
				temp.verbQual = "has";
				temp.keyword = step.keyword;
				temp.tense = step.tense;
				temp.isQuestion = false;
				newInfo.sentences.add(0,temp);
				newInfo.sentences.add(step);
			}
			else if (reduceFlag && step.entityName.equals(reduceEntity) && step.procedureName == null) {
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
			allTenses.add(tense);
			allVerbs.add(step.verbQual);
			count++;
			
		}
		if (newInfo.sentences.get(0).procedureName != null && newInfo.sentences.get(0).procedureName.equals("REDUCTION")) {
			LinguisticStep temp = newInfo.sentences.get(0);
			temp.procedureName = "";
			temp.keyword = "";
			newInfo.sentences.set(0, temp);
		}
		return newInfo;
	}
}
