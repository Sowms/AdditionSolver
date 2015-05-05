package nlp_adder;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Timestamp {
	String time;
	HashMap<String, State> situation;
}

class State {
	String owner;
	HashMap<String, String> ownedEntities;
}

public class KnowledgeRepresenter {

	private static final String CHANGE_OUT = "changeOut";
	private static final String CHANGE_IN = "changeIn";
	private static final String COMPARE_PLUS = "comparePlus";
	private static final String REDUCTION = "reduction";
	private static final String INCREASE = "increase";
	private static final String X_VALUE = "x";

	private static final String CHANGE = "change";
	private static final String OWNER_1 = "[owner1]";
	private static final String OWNER_2 = "[owner2]";
	private static final String ENTITY = "[entity]";
	private static final String UNKNOWN = "unknown";
	private static final String TIMESTAMP_PREFIX = "t";

	private static final int NO_OWNERS_SUPPORTED = 2;
	//private static final String PRESENT = "present";
	private static final String PAST = "past";

	private static final String VAR_PATTERN = "x\\d+";

	static int timeStep = 0;
	static int varCount = 1;
	static int unknownCounter = 0;

	static int questionTime = -1;
	static String questionEntity = "";
	static String questionOwner = "";
	static String altogetherEquation = "";
	static String finalAns = "";
	
	static boolean isQuestionAggregator = false;

	static ArrayList<Timestamp> story = new ArrayList<Timestamp>();
	static LinkedHashSet<String> entities = new LinkedHashSet<String>();
	static HashMap<String,String> variables = new HashMap<String,String>();
	static HashMap<String,String> procedureMap = new HashMap<String,String>();

	private static void loadProcedureLookup() {
		procedureMap.put(CHANGE_OUT, "[owner1]-[entity]. [owner2]+[entity]");
		procedureMap.put(CHANGE_IN, "[owner1]+[entity]. [owner2]-[entity]");
		procedureMap.put(COMPARE_PLUS, "[entity]+[owner2]");
		procedureMap.put(REDUCTION, "[owner1]-[entity]");
		procedureMap.put(INCREASE, "[owner1]+[entity]");
	}
	
	static void clear() {
		timeStep = 0;
		varCount = 1;
		unknownCounter = 0;
		isQuestionAggregator = false;
		
		questionTime = -1;
		questionEntity = "";
		questionOwner = "";
		altogetherEquation = "";
		finalAns = "";

		story = new ArrayList<Timestamp>();
		variables = new HashMap<String,String>();
		procedureMap = new HashMap<String,String>();
		entities = new LinkedHashSet<String>();
	}

	private static void updateTimestamp (String owner, Entity newEntity, String tense) {
		////System.out.println(owner + "|update|" + newEntity.name + "|" + tense + "|" + newEntity.value);
		//entities.add(newEntity.name);
		HashMap<String,State> currentSituation = new HashMap<String,State>();
		int changeTime = timeStep;
		if (tense.equals(PAST))
			changeTime = 0;
		if (story.size() != timeStep) {
			currentSituation = story.get(changeTime).situation;
		}
		State currentState;
		if (currentSituation.containsKey(owner)) { 
			currentState = currentSituation.get(owner);
		} else {
			currentState = new State();
			currentState.owner = owner;
			currentState.ownedEntities = new HashMap<String,String>();
		}
		if (currentState.ownedEntities.containsKey(newEntity.name)) {
			String existingValue = currentState.ownedEntities.get(newEntity.name);
			if (existingValue.contains(X_VALUE) && !newEntity.value.contains(X_VALUE)) {
				if (existingValue.contains("+") || existingValue.contains("-")) {
					Pattern varPattern = Pattern.compile(VAR_PATTERN);
					Matcher varMatcher = varPattern.matcher(existingValue);
					if (varMatcher.find()) {
						String variable = varMatcher.group();
						String equation = existingValue.replaceFirst(VAR_PATTERN, "x");
						equation = equation + "=" + newEntity.value;
						equation = equation.trim();
						variables.put(variable, EquationSolver.getSolution(equation));
					}
				}
				else
					variables.put(existingValue, newEntity.value);
				updateValues();
			}
		}
		currentState.ownedEntities.put(newEntity.name, newEntity.value);
		currentSituation.put(owner, currentState);
		Timestamp updatedTimestamp = new Timestamp();
		updatedTimestamp.time = TIMESTAMP_PREFIX + changeTime;
		updatedTimestamp.situation = currentSituation;
		if (story.size() == changeTime) {
			story.add(changeTime,updatedTimestamp);
		}
		else {
			story.remove(changeTime);
			story.add(changeTime,updatedTimestamp);
		}
	}
	
	private static void updateValues() {
		ArrayList<Timestamp> newStory = new ArrayList<Timestamp>();
		for (Timestamp t : story) {
			Iterator<Map.Entry<String,State>> it = t.situation.entrySet().iterator();
			while (it.hasNext()) {
			     Map.Entry<String,State> pairs = it.next();
			     State s = pairs.getValue();
			     String entityName= "", newValue = "";
			     Iterator<Map.Entry<String,String>> it1 = s.ownedEntities.entrySet().iterator();
				 while (it1.hasNext()) {
					Map.Entry<String,String> newPairs = it1.next();
					if (newPairs.getValue().contains(X_VALUE)) {
						Pattern varPattern = Pattern.compile(VAR_PATTERN);
						Matcher varMatcher = varPattern.matcher(newPairs.getValue().toString());
						if (varMatcher.find()) {
							entityName = newPairs.getKey();
							String variable = varMatcher.group();
							if (variables.get(variable) != null) {
								newValue = newPairs.getValue().replaceFirst(VAR_PATTERN, variables.get(variable));
							}
									
						}
					}
				 }
				 if (!newValue.isEmpty())
					 s.ownedEntities.put(entityName, newValue);
				 t.situation.put(pairs.getKey(), s);		 
			}
			newStory.add(t);
		}	
		story = newStory;
	}
	
	private static void reflectChanges(String owner1, String owner2, Entity newEntity,
			   String keyword, String procedure, String tense) {
		if (owner1.isEmpty()) {
			owner1 = UNKNOWN + (unknownCounter++); 
		}
		else if (owner2.isEmpty()) {
			owner2 = UNKNOWN + (unknownCounter++);
		}
		// There is no keyword here, an entity has been assigned a value
		//System.out.println("e"+owner1 + "|" + owner2 + "|" + keyword + "|" + procedure + "|" + tense + "|" + newEntity.value);
		if (keyword.isEmpty() && newEntity.name != null) {
			////System.out.println(keyword+"|"+owner1+"|"+owner2+"|"+newEntity.name+"|"+newEntity.value+"|"+entities);
			if (entities.contains(owner1))
				updateTimestamp(owner2, newEntity, tense);
			else
				updateTimestamp(owner1, newEntity, tense);
			return;
		} 
		String oldValue1 = "", oldValue2 = "";
		try {
			oldValue1 = story.get(timeStep).situation.get(owner1).ownedEntities.get(newEntity.name);
		} catch (NullPointerException ex) {
			if (!owner1.isEmpty()) {
				addOwner(owner1, newEntity.name);
				oldValue1 = story.get(timeStep).situation.get(owner1).ownedEntities.get(newEntity.name);
			}
		} catch (IndexOutOfBoundsException ex) {
			updateTimestamp(owner1, newEntity, tense);
			addOwner(owner1, newEntity.name);
			oldValue1 = story.get(timeStep).situation.get(owner1).ownedEntities.get(newEntity.name);
		}
		try {
			oldValue2 = story.get(timeStep).situation.get(owner2).ownedEntities.get(newEntity.name);
		} catch (NullPointerException ex) {
			if (!owner2.isEmpty()) {
				addOwner(owner2, newEntity.name);
				oldValue2 = story.get(timeStep).situation.get(owner2).ownedEntities.get(newEntity.name);
			}
		} catch (IndexOutOfBoundsException ex) {
			updateTimestamp(owner2, newEntity, tense);
			addOwner(owner2, newEntity.name);
			oldValue2 = story.get(timeStep).situation.get(owner2).ownedEntities.get(newEntity.name);
		}
		String newName1 = "", newName2 = "";
		if (oldValue1 == null) {
			Entity correctEntity = resolveNullEntity(owner1,newEntity.name);
			oldValue1 = correctEntity.value;
			newName1 = correctEntity.name;
		}
		if (oldValue2 == null) {
			Entity correctEntity = resolveNullEntity(owner2,newEntity.name);
			oldValue2 = correctEntity.value;
			newName2 = correctEntity.name;
		}
		//System.out.println("n"+newName1+newName2);
		if (procedure.contains(CHANGE)) {
			timeStep++;
			tense = "";
		}
		String[] steps = procedureMap.get(procedure).split("\\.");
		////System.out.println(procedure + "|" + procedureMap.get(procedure) + "|" + steps.length + owner1 + "|" + oldValue1 + "|" + owner2 + "|" + oldValue2);
		if (steps.length > NO_OWNERS_SUPPORTED) {
			////System.out.println("Invalid procedure description");
			System.exit(0);
		}
		for (int i = 0; i < steps.length; i++) {
			Entity modifiedEntity = new Entity();
			modifiedEntity.name = newEntity.name;
			String step = steps[i];
			step = step.replace(OWNER_1, oldValue1);
			step = step.replace(OWNER_2, oldValue2);
			step = step.replace(ENTITY, newEntity.value);
			modifiedEntity.value = step;
			String owner;
			if (i == 0) {
				owner = owner1;
				if (!newName1.isEmpty() && !newName1.equals(modifiedEntity.name))
					modifiedEntity.name = newName1;
			} else {
				owner = owner2;
				if (!newName2.isEmpty() && !newName2.equals(modifiedEntity.name))
					modifiedEntity.name = newName2;
			}
			//System.out.println("eee"+owner+"|"+modifiedEntity.name+"|"+modifiedEntity.value);
			updateTimestamp(owner, modifiedEntity, tense);
		}
		inertia();
	
	}
	
	private static void inertia() {
		
		if (story.size() == 1)
			return;
		Timestamp prevMoment = story.get(story.size()-2);
		Timestamp currentMoment = story.get(story.size()-1);
		HashMap<String,State> prevSituation = prevMoment.situation;
		Iterator<Map.Entry<String,State>> it = prevSituation.entrySet().iterator();
		while (it.hasNext()) {
		     Map.Entry<String,State> pairs = it.next();
		     if (!currentMoment.situation.containsKey(pairs.getKey())) {
		    	 currentMoment.situation.put(pairs.getKey(), pairs.getValue());
			     continue;
			 }
		     Iterator<Map.Entry<String,String>> it1 = pairs.getValue().ownedEntities.entrySet().iterator();
			 while (it1.hasNext()) {
				Map.Entry<String,String> newPairs = it1.next();
				if (!currentMoment.situation.get(pairs.getKey()).ownedEntities.containsKey(newPairs.getKey())) {
			    	State s = currentMoment.situation.get(pairs.getKey()); 
					s.ownedEntities.put(newPairs.getKey(), newPairs.getValue());
					currentMoment.situation.put(pairs.getKey(), s);
				}
			 }   
		}
		story.remove(timeStep);
		story.add(currentMoment);
	}
	
	private static void displayStory() {
		System.out.println("----------------------------------------------------");
		for (Timestamp t : story) {
			System.out.println(t.time);
			Iterator<Map.Entry<String, State>> it = t.situation.entrySet().iterator();
			while (it.hasNext()) {
			     Map.Entry<String, State> pairs = it.next();
			     System.out.print(pairs.getKey() + " ");
			     State s = pairs.getValue();
			     Iterator<Map.Entry<String, String>> it1 = s.ownedEntities.entrySet().iterator();
				 while (it1.hasNext()) {
					Map.Entry<String, String> newPairs = it1.next();
					System.out.print(newPairs.getKey() + " " + newPairs.getValue());
				 }
				 System.out.println("");
			}	
		}
	}
	
	private static Entity resolveNullEntity(String owner, String name) {
		HashMap<String,String> entities = story.get(timeStep).situation.get(owner).ownedEntities;
		Entity answer = new Entity();
		Iterator<Map.Entry<String, String>> it1 = entities.entrySet().iterator();
		String sum = "";
		while (it1.hasNext()) {
			Map.Entry<String, String> newPairs = it1.next();
			if (newPairs.getKey().contains(name)) {
				answer.name = newPairs.getKey();
				sum = newPairs.getValue();
			}
		}
		answer.value = sum;
		//System.out.println("s"+sum);
		if (!sum.isEmpty()) 
			return answer;
		answer.name = name;
		addOwner(owner,name);
		if (story.get(timeStep).situation.get(owner).ownedEntities.get(name) != null) {
			answer.value = story.get(timeStep).situation.get(owner).ownedEntities.get(name);
			return answer;
		}
		return new Entity();
	}

	private static void addOwner(String owner, String name) {
		String varName = X_VALUE + varCount;
		for (int i = 0; i <= timeStep; i++) {
			HashMap<String,State> iSituation = story.get(i).situation;
			State newState = new State();
			HashMap <String,String> ownedEntities = new HashMap<String,String>();
			if (iSituation.containsKey(owner)) {
				newState = iSituation.get(owner);
				ownedEntities = newState.ownedEntities;
				//""
			}
			newState.owner = owner;
			ownedEntities.put(name, varName);
			newState.ownedEntities = ownedEntities;
			////System.out.println(owner + "|" + ownedEntities);
			//sdgfg
			iSituation.put(owner, newState);
			story.get(i).situation = iSituation;
		}
		variables.put(varName, null);
		varCount++;	
	}

	
	public static void represent(LinguisticInfo extractedInformation) {
		loadProcedureLookup();
		entities = extractedInformation.entities;
		for (LinguisticStep ls : extractedInformation.sentences) {
			Entity currentEntity = new Entity();
			currentEntity.name = ls.entityName;
			currentEntity.value = ls.entityValue;
			//System.out.println("kr" + ls.owner1 + "|" + ls.owner2 + "|" + currentEntity.name + "|" + currentEntity.value + "|" + ls.keyword + "|" + ls.procedureName + "|" + ls.tense);
			if (ls.isQuestion) {
				questionOwner = ls.owner1;
				questionEntity = ls.entityName;
				if (ls.tense.equals("past"))
					questionTime = 0;
				else
					questionTime = timeStep;
				isQuestionAggregator = ls.aggregator;
				continue;
			}
			reflectChanges(ls.owner1, ls.owner2, currentEntity, ls.keyword, ls.procedureName, ls.tense);
			displayStory();
		}
		
	}
	
	
	public static void solve() {
		
		//System.out.println("ques|"+questionOwner+"|"+questionEntity+"|"+isQuestionAggregator);
		if (questionOwner.isEmpty()) {
			System.out.println("ques|"+questionOwner+"|"+questionEntity+"|");
			if (isQuestionAggregator) {
				Timestamp t = story.get(timeStep);
				Iterator<Map.Entry<String, State>> it = t.situation.entrySet().iterator();
				String sum = "";
				while (it.hasNext()) {
				     Map.Entry<String, State> pairs = it.next();
				     State s = pairs.getValue();
				     Iterator<Map.Entry<String, String>> it1 = s.ownedEntities.entrySet().iterator();
					 while (it1.hasNext()) {
						Map.Entry<String, String> newPairs = it1.next();
						System.out.println(questionEntity+"|"+newPairs.getKey());
						if (!questionEntity.isEmpty() && !questionEntity.equals(newPairs.getKey()) && entities.contains(questionEntity) && !newPairs.getKey().contains(questionEntity))
							continue;
						sum = sum + "+" + newPairs.getValue();
					 }
				}	
				String ans = sum;
				if (ans.contains(X_VALUE)) {
					////System.out.println("Cannot be solved!");
					////System.out.println("Assuming initial conditions");
					ans = ans.replaceAll(VAR_PATTERN, "");
				}
				if (ans.contains("+") || ans.contains("-"))
					finalAns = "Altogether " + EquationSolver.getSolution(ans) + " " + questionEntity;
				else
					finalAns = "Altogether " + ans + " " + questionEntity;
				////System.out.println("false");
				return;
			}
		}
		else if (questionEntity.isEmpty()) {
			State s = story.get(timeStep).situation.get(questionOwner);
			Iterator<Map.Entry<String, String>> it1 = s.ownedEntities.entrySet().iterator();
			String sum = "";
			while (it1.hasNext()) {
			     Map.Entry<String, String> newPairs = it1.next();
			     sum = sum + "+" + newPairs.getValue();
			     if (!isQuestionAggregator) {
			    	String ans = sum;
					if (ans.contains(X_VALUE)) {
						//System.out.println("Cannot be solved!");
						//System.out.println("Assuming initial conditions");
						ans = ans.replaceAll(VAR_PATTERN, "");
						ans = ans.replaceAll("\\++\\+*", "+");
					}
					////System.out.println("aaa"+sum);
					if (ans.contains("+") || ans.contains("-"))
						finalAns = questionOwner + " has " + EquationSolver.getSolution(ans) + " " + newPairs.getKey();
					else
						finalAns = questionOwner + " has " + ans + " " + newPairs.getKey();
			     }
			}	
			////System.out.println("aaa"+sum);
			if (isQuestionAggregator) {
				String ans = sum;
				if (ans.contains(X_VALUE)) {
					//System.out.println("Cannot be solved!");
					//System.out.println("Assuming initial conditions");
					ans = ans.replaceAll(VAR_PATTERN, "");
					ans = ans.replaceAll("\\++\\+*", "+");
				}
				////System.out.println("aaa"+sum);
				if (ans.contains("+") || ans.contains("-"))
					finalAns = questionOwner + " has " + EquationSolver.getSolution(ans);
				else
					finalAns = questionOwner + " has " + ans;
			}
			return;
		}
		if (isQuestionAggregator && questionOwner.isEmpty() && questionEntity.isEmpty()) {
			Timestamp t = story.get(timeStep);
			Iterator<Map.Entry<String, State>> it = t.situation.entrySet().iterator();
			String sum = "";
			while (it.hasNext()) {
			     Map.Entry<String, State> pairs = it.next();
			     State s = pairs.getValue
			    		 ();
			     Iterator<Map.Entry<String, String>> it1 = s.ownedEntities.entrySet().iterator();
				 while (it1.hasNext()) {
					Map.Entry<String, String> newPairs = it1.next();
					sum = sum + "+" + newPairs.getValue();
				 }
			}	
			finalAns = "Altogether " + EquationSolver.getSolution(sum);
			return;
		}
		String ans;
		if (questionEntity.isEmpty()) {
			Map<String,String> entities = story.get(questionTime).situation.get(questionOwner).ownedEntities;
			ans = entities.entrySet().iterator().next().getValue();
			questionEntity = entities.entrySet().iterator().next().getKey();
		}
		else
			ans = story.get(questionTime).situation.get(questionOwner).ownedEntities.get(questionEntity);
		if (ans == null) {
			Map<String,String> entities = story.get(questionTime).situation.get(questionOwner).ownedEntities;
			Iterator<Map.Entry<String, String>> it1 = entities.entrySet().iterator();
			String sum = "";
			while (it1.hasNext()) {
				Map.Entry<String, String> newPairs = it1.next();
				if (newPairs.getKey().contains(questionEntity))
					sum = sum + "+" + newPairs.getValue();
			}	
			ans = sum;
		}
		////System.out.println("a"+ans);
		if (ans.contains(X_VALUE)) {
			////System.out.println("Cannot be solved!");
			////System.out.println("Assuming initial conditions");
			ans = ans.replaceFirst(VAR_PATTERN, "");
		}
		////System.out.println("--");
		if (ans.contains("+") || ans.contains("-"))
			finalAns = questionOwner + " has " + EquationSolver.getSolution(ans) + " " + questionEntity;
		else
			finalAns = questionOwner + " has " + ans + " " + questionEntity;
	}
}
