package nlp_adder;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;

//import edu.stanford.nlp.io.EncodingPrintWriter.out;

class TimeStamp {
	String value;
	String name;
	String time;
	String qualifier;
}

class Owner {
	String name;
	HashMap<String, ArrayList<TimeStamp>> situation; // verb, state
}



public class KnowledgeRepresenter {

	private static final String CHANGE_OUT = "changeOut";
	private static final String CHANGE_IN = "changeIn";
	private static final String COMPARE_PLUS = "comparePlus";
	private static final String COMPARE_MINUS = "compareMinus";
	private static final String INCREASE = "increase";
	private static final String REDUCTION = "reduction";
	private static final String ALTOGETHER_EQ = "altogetherEq";
	private static final String COMPARE_PLUS_EQ = "comparePlusEq";
	private static final String COMPARE_MINUS_EQ = "compareMinusEq";
	
	private static final String X_VALUE = "x";
	private static final String VAR_PATTERN = "x\\d+";

	private static final String CHANGE = "change";
	private static final String OWNER_1 = "[owner1]";
	private static final String OWNER_2 = "[owner2]";
	private static final String ENTITY = "[entity]";
	private static final String UNKNOWN = "unknown";
	private static final String TIMESTAMP_PREFIX = "t";
	
	private static final int NO_OWNERS_SUPPORTED = 2;
	private static final String PRESENT = "present";
	private static final String PAST = "past";

	static int timeStep = 0;
	static int varCount = 1;
	static int unknownCounter = 0;

	static int questionTime = -1;
	static String questionEntity = "";
	static String questionOwner = "";
	static String questionOwner1 = "";
	static String questionOwner2 = "";
	static String questionVerb = "";
	static String equalityEquation = "";	
	static String finalAns = "";
	
	static boolean isQuestionAggregator = false;
	static boolean isQuestionDifference = false;
	static boolean isQuestionComparator = false;

	static HashMap<String,Owner> story = new HashMap<String,Owner>();
	static LinkedHashSet<String> entities = new LinkedHashSet<String>();
	static HashMap<String,String> variables = new HashMap<String,String>();
	static HashMap<String,String> procedureMap = new HashMap<String,String>();
	static ArrayList<String> storyTense = new ArrayList<String>();
	static HashMap<String,String> keywordMap = new HashMap<String,String>();
	static ArrayList<String> allEquations = new ArrayList<String>();
	

	private static void loadProcedureLookup() {
		keywordMap.put("put", CHANGE_OUT);
		keywordMap.put("place", CHANGE_OUT);
		keywordMap.put("sell", CHANGE_OUT);
		keywordMap.put("give", CHANGE_OUT);
		keywordMap.put("more than", COMPARE_PLUS);
		keywordMap.put("less than", COMPARE_MINUS);
		keywordMap.put("get", CHANGE_IN);
		keywordMap.put("buy", CHANGE_IN);
		keywordMap.put("pick", CHANGE_IN);
		keywordMap.put("take", CHANGE_IN);
		keywordMap.put("borrow", CHANGE_IN);
		keywordMap.put("lose", REDUCTION);
		keywordMap.put("remove", REDUCTION);
		keywordMap.put("spend", REDUCTION);
		keywordMap.put("eat", REDUCTION);
		keywordMap.put("more", INCREASE);
		keywordMap.put("find", INCREASE);
		keywordMap.put("decrease", REDUCTION);
		
		procedureMap.put(CHANGE_OUT, "[owner1]-[entity]. [owner2]+[entity]");
		procedureMap.put(CHANGE_IN, "[owner1]+[entity]. [owner2]-[entity]");
		procedureMap.put(COMPARE_PLUS, "[entity]+[owner2]");
		procedureMap.put(COMPARE_MINUS, "[owner2]-[entity]");
		procedureMap.put(REDUCTION, "[owner1]-[entity]");
		procedureMap.put(INCREASE, "[owner1]+[entity]");
		procedureMap.put(ALTOGETHER_EQ, "[owner1]+[owner2]=[entity]");
		procedureMap.put(COMPARE_PLUS_EQ, "[owner1] = [owner2]+[entity]");
		procedureMap.put(COMPARE_MINUS_EQ, "[owner1] = [owner2]-[entity]");
	}
	
	static void clear() {
		timeStep = 0;
		varCount = 1;
		unknownCounter = 0;
		isQuestionAggregator = false;
		isQuestionDifference = false;
		isQuestionComparator = false;

		questionTime = -1;
		questionEntity = "";
		questionOwner = "";
		questionOwner1 = "";
		questionOwner2 = "";
		finalAns = "";

		story = new HashMap<String,Owner>();
		variables = new HashMap<String,String>();
		procedureMap = new HashMap<String,String>();
		entities = new LinkedHashSet<String>();
		storyTense = new ArrayList<String>();
		allEquations = new ArrayList<String>();
	}
	
	public static String commonString(String s1, String s2) {
		   for (int i = Math.min(s1.length(), s2.length()); ; i--) {
		       if (s2.endsWith(s1.substring(0, i))) {
		    	   return s1.substring(0, i);
		       }
		   }    
	}

	private static void updateTimestamp (String owner, Entity newEntity, String tense, String nounQual, String verbQual) {
		System.out.println(owner + "|update|" + newEntity.name + "|" + tense + "|" + newEntity.value + "|" +timeStep +"|"+nounQual+"|"+verbQual);
		entities.add(newEntity.name);
		if (newEntity.value.equals("some")) {
			String varName = X_VALUE + varCount++; 
			variables.put(varName, null);
			newEntity.value = varName;
		}
		int changeTime = timeStep;
		if (tense.equals(PAST) && timeStep!=0 && storyTense.contains(PRESENT))
			changeTime = 0;
		storyTense.add(tense);
		TimeStamp newTimeStamp = new TimeStamp();
		newTimeStamp.qualifier = nounQual;
		newTimeStamp.name = newEntity.name;
		newTimeStamp.value = newEntity.value;
		newTimeStamp.time = TIMESTAMP_PREFIX + changeTime;
		HashMap<String,ArrayList<TimeStamp>> currentSituation = new HashMap<String,ArrayList<TimeStamp>>();
		if (story.containsKey(owner)) {
			currentSituation = story.get(owner).situation;
			//////System.out.println("aa"+owner+currentSituation.entrySet().size());
		}
		String verb = verbQual;
		ArrayList<TimeStamp> verbStory = new ArrayList<TimeStamp>();
		if (currentSituation.containsKey(verbQual)) 
			verbStory = currentSituation.get(verbQual);
		else {
			if (verbQual.equals("has") && !currentSituation.isEmpty()) {
				verb = currentSituation.entrySet().iterator().next().getKey();
				if (!keywordMap.containsKey(verb)) 
					verbStory = currentSituation.get(verb);
				else 
					verb = "has";
			}
			//////System.out.println("aa"+verbQual+"|"+verb);
			if (verb.equals("has") && currentSituation.containsKey(verb))
				verbStory = currentSituation.get(verb);
		}
		TimeStamp deleteNode = new TimeStamp();
		if (!verbStory.isEmpty()) {
			for (TimeStamp oldTimeStamp : verbStory) {
				if (oldTimeStamp.time.equals(newTimeStamp.time) && (oldTimeStamp.name.contains(newTimeStamp.name) || newTimeStamp.name.contains(oldTimeStamp.name))) {
					newTimeStamp.name = oldTimeStamp.name;
					String existingValue = oldTimeStamp.value;
					if (existingValue.contains(X_VALUE) && !newEntity.value.contains(X_VALUE) || !existingValue.contains(X_VALUE) && newEntity.value.contains(X_VALUE)) {
						//////System.out.println(existingValue+"|"+newEntity.value);
						boolean equationFlag = false;
						if (existingValue.contains(X_VALUE) && (existingValue.contains("+") || existingValue.contains("-")))
							equationFlag = true;
						if (newEntity.value.contains(X_VALUE) && (newEntity.value.contains("+") || newEntity.value.contains("-")))
							equationFlag = true;
						if (equationFlag) {
							//////System.out.println("waka");
							Pattern varPattern = Pattern.compile(VAR_PATTERN);
							Matcher varMatcher = varPattern.matcher(existingValue);
							ArrayList<String> vars = new ArrayList<String>();
							while (varMatcher.find()) 
								vars.add(varMatcher.group());
							Collections.sort(vars);
							String variable;
							if (vars.size() > 1) {
								//initial conditions
								existingValue = existingValue.replace(vars.get(0), "0");
								variable = vars.get(1);
							}
							else
								variable = vars.get(0);
							String equation = existingValue.replaceFirst(VAR_PATTERN, "x");
							equation = equation + "=" + newEntity.value;
							equation = equation.trim();
							//////System.out.println("aaaaaaaaaa"+equation);
							String ans = EquationSolver.getSolution(equation);
							if (ans.contains("0.0")) {
								deleteNode = oldTimeStamp;
								break;
							}
							variables.put(variable, EquationSolver.getSolution(equation));
							varMatcher = varPattern.matcher(newEntity.value);
							if (varMatcher.find()) {
								variable = varMatcher.group();
								equation = newEntity.value.replaceFirst(VAR_PATTERN, "x");
								equation = existingValue + "=" + equation;
								equation = equation.trim();
								//////System.out.println("aaaaaaaaaa"+equation+variable);
								ans = EquationSolver.getSolution(equation);
								if (ans.contains("0.0")) {
									deleteNode = oldTimeStamp;
									break;
								}
								variables.put(variable, EquationSolver.getSolution(equation));
								updateValues();
								return;
							}
						}
						else
							variables.put(existingValue, newEntity.value);
						updateValues();
						deleteNode = oldTimeStamp;
				    }
					else if (!newTimeStamp.value.contains("-") && !newTimeStamp.value.contains("+")){
						//past?
						timeStep++;
						newTimeStamp.time = TIMESTAMP_PREFIX + timeStep;
					}
					else
						deleteNode = oldTimeStamp;
				}
			}
		}
		verbStory.remove(deleteNode);
		if (!contains(verbStory,newTimeStamp))
			verbStory.add(newTimeStamp);
		currentSituation.put(verb, verbStory);
		//////System.out.println("aab"+owner+currentSituation.entrySet().size());
		Owner currentOwner = new Owner();
		currentOwner.name = owner;
		currentOwner.situation = currentSituation;
		story.put(owner,currentOwner);
		displayStory();
	}
	
	private static boolean contains(ArrayList<TimeStamp> verbStory,
			TimeStamp newTimeStamp) {
		for (TimeStamp t : verbStory)
			if (t.name.equals(newTimeStamp.name) && t.qualifier.equals(newTimeStamp.qualifier) && t.time.equals(newTimeStamp.time) && t.value.equals(newTimeStamp.value))
				return true;
		return false;
	}

	private static void updateValues() {
		HashMap<String,Owner> newStory = new HashMap<String,Owner>();
		Iterator<Entry<String, Owner>> ownerIterator = story.entrySet().iterator(); 
		while (ownerIterator.hasNext()) {
			Owner owner = ownerIterator.next().getValue();
			Owner newOwner = new Owner();
			HashMap<String,ArrayList<TimeStamp>> currentSituation = owner.situation;
			Iterator<Entry<String,ArrayList<TimeStamp>>> verbIterator = currentSituation.entrySet().iterator(); 
			while (verbIterator.hasNext()) {
				Entry<String,ArrayList<TimeStamp>> next = verbIterator.next(); 
				ArrayList<TimeStamp> verbStory = next.getValue();
				String verb = next.getKey();
				ArrayList<TimeStamp> newVerbStory = new ArrayList<TimeStamp>();
				for (TimeStamp currentTimeStamp : verbStory) {
					if (currentTimeStamp.value.contains(X_VALUE)) {
						Pattern varPattern = Pattern.compile(VAR_PATTERN);
						Matcher varMatcher = varPattern.matcher(currentTimeStamp.value.toString());
						if (varMatcher.find()) {
							String variable = varMatcher.group();
							if (variables.get(variable) != null) {
								currentTimeStamp.value = currentTimeStamp.value.replaceFirst(VAR_PATTERN, variables.get(variable));
							}	
						}
					}
					newVerbStory.add(currentTimeStamp);
				}
				currentSituation.put(verb,newVerbStory);
			}
			newOwner.name = owner.name;
			newOwner.situation = currentSituation;
			newStory.put(owner.name,owner);
		}
		story = newStory;
		ArrayList<String> newEquations = new ArrayList<String>();
		for (String equation : allEquations) {
			Pattern varPattern = Pattern.compile(VAR_PATTERN);
			Matcher varMatcher = varPattern.matcher(equation);
			while (varMatcher.find()) {
				String variable = varMatcher.group();
				//////System.out.println(variable + "|" + variables.get(variable));
				if (variables.get(variable) != null) {
					//////System.out.println(equation);
					equation = equation.replace(variable, variables.get(variable));
					//////System.out.println(equation);
				}	
			}
			newEquations.add(equation);
		}
		allEquations = newEquations;
	}
	
	private static void reflectChanges(String owner1, String owner2, Entity newEntity,
			   String keyword, String procedure, String tense, String nounQual, String verbQual) {
		//////System.out.println(keyword+"|"+owner1+"|"+owner2+"|"+newEntity.name+"|"+newEntity.value+"|"+entities+"|"+nounQual+"|"+verbQual);
		if (owner1.isEmpty()) {
			owner1 = UNKNOWN + (unknownCounter++); 
		}
		else if (owner2.isEmpty()) {
			owner2 = UNKNOWN + (unknownCounter++);
		}
		// There is no keyword here, an entity has been assigned a value
		//////System.out.println("e"+owner1 + "|" + owner2 + "|" + keyword + "|" + procedure + "|" + tense + "|" + newEntity.value);
		String owner = "";
		if (procedure == null)
			procedure = "";
		if (!procedure.contains("Eq") && (keyword.equals(verbQual) || keyword.isEmpty())) {	
			if (entities.contains(owner1))
				owner = owner2;
			else
				owner = owner1;
			HashMap<String,ArrayList<TimeStamp>> currentSituation = new HashMap<String,ArrayList<TimeStamp>>();
			if (story.containsKey(owner)) {
				currentSituation = story.get(owner).situation;
				////////System.out.println("aa"+owner+currentSituation.entrySet().size());
			}
			String verb = verbQual;
			if (verbQual.equals("has") && !currentSituation.isEmpty()) {
					verb = currentSituation.entrySet().iterator().next().getKey();
					if (keywordMap.containsKey(verb)) 
						verb = "has";
			}
			if (newEntity.value.isEmpty()) {
				newEntity.value = X_VALUE + varCount;
				variables.put(newEntity.value, null);
				varCount++;
			}
			updateTimestamp (owner, newEntity, tense, nounQual, verb);				
		}
		if (procedure.isEmpty() && newEntity.name != null) 
			return;
		String verb = verbQual;
		String newName1 = "", newName2 = "";
		if (verbQual.equals(keyword))
			verb = "has";
		String oldValue1 = "", oldValue2 = "";
		try {
			ArrayList<TimeStamp> verbStory = story.get(owner1).situation.get(verb);
			//modularize
			for (TimeStamp currentTimeStamp : verbStory) {
				if (currentTimeStamp.name.equals(newEntity.name)) {
					oldValue1 = currentTimeStamp.value;
				}
			}
		} catch (NullPointerException ex) {
			if (!owner1.isEmpty()) {
				addOwner(owner1, newEntity, nounQual, verb);
				ArrayList<TimeStamp> verbStory = story.get(owner1).situation.get(verb);
				for (TimeStamp currentTimeStamp : verbStory) {
					if (currentTimeStamp.name.equals(newEntity.name) && currentTimeStamp.time.equals(TIMESTAMP_PREFIX+timeStep+"")) {
						oldValue1 = currentTimeStamp.value;
					}
				}
			}
		} catch (IndexOutOfBoundsException ex) {
			updateTimestamp(owner1, newEntity, tense, nounQual, verb);
			addOwner(owner1, newEntity, nounQual, verb);
			ArrayList<TimeStamp> verbStory = story.get(owner1).situation.get(verb);
			for (TimeStamp currentTimeStamp : verbStory) {
				if (currentTimeStamp.name.equals(newEntity.name) && currentTimeStamp.time.equals(TIMESTAMP_PREFIX+timeStep+"")) {
					oldValue1 = currentTimeStamp.value;
				}
			}
		}
		if (procedure.contains("change") || procedure.contains("compare") || procedure.contains("Eq")) {
		try {
			ArrayList<TimeStamp> verbStory = story.get(owner2).situation.get(verb);
			//modularize
			for (TimeStamp currentTimeStamp : verbStory) {
				if (currentTimeStamp.name.equals(newEntity.name)) {
					oldValue2 = currentTimeStamp.value;
				}
			}
		} catch (NullPointerException ex) {
			if (!owner2.isEmpty()) {
				addOwner(owner2, newEntity, nounQual, verb);
				ArrayList<TimeStamp> verbStory = story.get(owner2).situation.get(verb);
				for (TimeStamp currentTimeStamp : verbStory) {
					if (currentTimeStamp.name.equals(newEntity.name) && currentTimeStamp.time.equals(TIMESTAMP_PREFIX+timeStep+"")) {
						oldValue2 = currentTimeStamp.value;
					}
				}
			}
		} catch (IndexOutOfBoundsException ex) {
			updateTimestamp(owner2, newEntity, tense, nounQual, verb);
			addOwner(owner2, newEntity, nounQual, verb);
			ArrayList<TimeStamp> verbStory = story.get(owner2).situation.get(verb);
			for (TimeStamp currentTimeStamp : verbStory) {
				if (currentTimeStamp.name.equals(newEntity.name) && currentTimeStamp.time.equals(TIMESTAMP_PREFIX+timeStep+"")) {
					oldValue2 = currentTimeStamp.value;
				}
			}
		}
		//System.err.println("aa"+oldValue2);
		if (oldValue2 == null || oldValue2.isEmpty()) {
			//System.err.println("aa"+oldValue2);
			Entity correctEntity = resolveNullEntity(owner2,newEntity.name, nounQual, verb);
			oldValue2 = correctEntity.value;
			newName2 = correctEntity.name;
		}
		
		}
		////System.err.println("aa"+oldValue1+"|"+oldValue2);
		if (oldValue1 == null || oldValue1.isEmpty()) {
			Entity correctEntity = resolveNullEntity(owner1,newEntity.name, nounQual, verb);
			oldValue1 = correctEntity.value;
			newName1 = correctEntity.name;
		}
		if (procedure.contains(CHANGE)) {
			timeStep++;
			tense = "";
		}
		
		//System.out.println("n"+newName1+newName2);
		String[] steps = procedureMap.get(procedure).split("\\.");
		//////System.out.println(procedure + "|" + procedureMap.get(procedure) + "|" + steps.length + owner1 + "|" + oldValue1 + "|" + owner2 + "|" + oldValue2);
		if (steps.length > NO_OWNERS_SUPPORTED) {
			//////////System.out.println("Invalid procedure description");
			////System.exit(0);
		}
		for (int i = 0; i < steps.length; i++) {
			Entity modifiedEntity = new Entity();
			modifiedEntity.name = newEntity.name;
			String step = steps[i];
			step = step.replace(OWNER_1, oldValue1);
			step = step.replace(OWNER_2, oldValue2);
			step = step.replace(ENTITY, newEntity.value);
			modifiedEntity.value = step;
			if (!procedure.contains("Eq")) {
				if (i == 0) {
					owner = owner1;
					if (!newName1.isEmpty() && !newName1.equals(modifiedEntity.name))
						modifiedEntity.name = newName1;
				} else {
					owner = owner2;
					if (!newName2.isEmpty() && !newName2.equals(modifiedEntity.name))
						modifiedEntity.name = newName2;
				}
				//	//////System.out.println("eee"+owner+"|"+modifiedEntity.name+"|"+modifiedEntity.value);
				updateTimestamp(owner, modifiedEntity, tense, nounQual, verb);
			}
			else {
				allEquations.add(step);
			}
		}
		inertia();
		//////System.out.println("hello");
		if (!verbQual.equals(keyword) && keywordMap.containsKey(verbQual)) {
			procedure = keywordMap.get(verbQual);
			steps = procedureMap.get(procedure).split("\\.");
			ArrayList<TimeStamp> verbStory = story.get(owner1).situation.get(verbQual);
			String value = "";
			for (TimeStamp currentTimeStamp : verbStory) {
				if (currentTimeStamp.name.equals(newEntity.name) && currentTimeStamp.time.equals(TIMESTAMP_PREFIX+timeStep+"")) {
					value = currentTimeStamp.value;
				}
			}
			//////////System.out.println(procedure + "|" + procedureMap.get(procedure) + "|" + steps.length + owner1 + "|" + oldValue1 + "|" + owner2 + "|" + oldValue2);
			if (steps.length > NO_OWNERS_SUPPORTED) {
				//////////System.out.println("Invalid procedure description");
				////System.exit(0);
			}
			for (int i = 0; i < steps.length; i++) {
				Entity modifiedEntity = new Entity();
				modifiedEntity.name = newEntity.name;
				String step = steps[i];
				step = step.replace(OWNER_1, oldValue1);
				step = step.replace(OWNER_2, oldValue2);
				step = step.replace(ENTITY, "("+value+")");
				modifiedEntity.value = step;
				if (!procedure.contains("EQ")) {
					if (i == 0) {
						owner = owner1;
						if (!newName1.isEmpty() && !newName1.equals(modifiedEntity.name))
							modifiedEntity.name = newName1;
					} else {
						owner = owner2;
						if (!newName2.isEmpty() && !newName2.equals(modifiedEntity.name))
							modifiedEntity.name = newName2;
					}
					//	//////System.out.println("eee"+owner+"|"+modifiedEntity.name+"|"+modifiedEntity.value);
					updateTimestamp(owner, modifiedEntity, tense, nounQual, "has");
				}
				else
					allEquations.add(step);
			}
			//////System.out.println("hello1");
			inertia();
		}
	}
	
	private static void inertia() {
		if (timeStep == 0)
			return;
		HashMap<String,Owner> newStory = new HashMap<String,Owner>();
		Iterator<Entry<String, Owner>> storyIterator = story.entrySet().iterator();
		String currentMoment = TIMESTAMP_PREFIX + timeStep;
		String previousMoment = TIMESTAMP_PREFIX + (timeStep-1);
		while (storyIterator.hasNext()) {
		     Owner owner = storyIterator.next().getValue();
		     Owner newOwner = new Owner();
			 HashMap<String,ArrayList<TimeStamp>> currentSituation = owner.situation;
		     Iterator<Entry<String, ArrayList<TimeStamp>>> verbStoryIterator = owner.situation.entrySet().iterator();
			 while (verbStoryIterator.hasNext()) {
				Entry<String, ArrayList<TimeStamp>> newPairs = verbStoryIterator.next();
				String verb = newPairs.getKey();
				//if (!verb.equals("has"))
					//continue;
				ArrayList<TimeStamp> verbStory = newPairs.getValue();
				ArrayList<TimeStamp> newVerbStory = new ArrayList<TimeStamp>();
				for (TimeStamp currentTimeStamp : verbStory) {
					if (currentTimeStamp.time.equals(previousMoment))
						if(checkIfActionNeeded(verbStory,currentTimeStamp)) {
							TimeStamp newTimeStamp = new TimeStamp();
							newTimeStamp.name = currentTimeStamp.name;
							newTimeStamp.qualifier = currentTimeStamp.qualifier;
							newTimeStamp.value = currentTimeStamp.value;
							newTimeStamp.time = currentMoment;
							newVerbStory.add(newTimeStamp);
						}
					newVerbStory.add(currentTimeStamp);
				}
				currentSituation.put(verb,newVerbStory);
			} 
			newOwner.name = owner.name;
			newOwner.situation = currentSituation;
			newStory.put(owner.name,owner);
		}
		story = newStory;
	}
	
	private static boolean checkIfActionNeeded(ArrayList<TimeStamp> verbStory, TimeStamp compareTimeStamp) {
		String currentMoment = TIMESTAMP_PREFIX + timeStep;
		boolean inertiaFlag = true;
		for (TimeStamp currentTimeStamp : verbStory) {
			if (currentTimeStamp.time.equals(currentMoment) && currentTimeStamp.name.equals(compareTimeStamp.name)) 
				inertiaFlag = false;
		}
		return inertiaFlag;
	}

	private static void displayStory() {
		System.out.println("----------------------------------------------------");
		Iterator<Entry<String, Owner>> storyIterator = story.entrySet().iterator();
		while (storyIterator.hasNext()) {
		     Owner owner = storyIterator.next().getValue();
		     System.out.println(owner.name);
		     Iterator<Entry<String, ArrayList<TimeStamp>>> verbStoryIterator = owner.situation.entrySet().iterator();
			 while (verbStoryIterator.hasNext()) {
				Entry<String, ArrayList<TimeStamp>> newPairs = verbStoryIterator.next();
				System.out.println(newPairs.getKey()+" ");
				ArrayList<TimeStamp> verbStory = newPairs.getValue();
				for (TimeStamp currentTimeStamp : verbStory) {
					System.out.print(currentTimeStamp.name + " ");
					System.out.print(currentTimeStamp.qualifier + " ");
					System.out.print(currentTimeStamp.value + " ");
					System.out.println(currentTimeStamp.time + " ");
				}
			} 
		}
	}
	
	private static Entity resolveNullEntity(String owner, String name, String nounQual, String verbQual) {
		ArrayList<TimeStamp> verbStory = story.get(owner).situation.get(verbQual);
		Entity answer = new Entity();
		answer.name = "";
		String sum = "0";
		for (TimeStamp t : verbStory) {
			if (t.name.contains(name) || name.contains(t.name)) {
				answer.name = t.name;
				  if(t.time.equals(TIMESTAMP_PREFIX + timeStep))
					  sum = t.value;
			}
		}
		answer.value = sum;
		if (!sum.equals("0")) 
			return answer;
		if (answer.name.isEmpty())
			answer.name = name;
		if (answer.value.equals( "0"))
			answer.value = null;
		//System.err.println("a"+answer.name+"|"+answer.value);
		addOwner(owner, answer, nounQual, verbQual);
		verbStory = story.get(owner).situation.get(verbQual);
		for (TimeStamp t : verbStory) {
			if (t.name.equals(name) && t.time.equals(TIMESTAMP_PREFIX + timeStep)) {
				answer.value = t.value;
				return answer;
			}
		}
		return new Entity();
	}

	private static void addOwner(String owner, Entity newEntity, String nounQual, String verbQual) {
		String varName = X_VALUE + varCount;
		Owner newOwner = new Owner();
		newOwner.name = owner;
		HashMap<String,ArrayList<TimeStamp>> newSituation = new HashMap<String,ArrayList<TimeStamp>>();
		if (story.containsKey(owner))
			newSituation = story.get(owner).situation;
		ArrayList<TimeStamp> verbStory = new ArrayList<TimeStamp>();
		if (newSituation.containsKey(verbQual))
			verbStory = newSituation.get(verbQual);
		for (int i = 0; i <= timeStep; i++) {
			TimeStamp newTimeStamp = new TimeStamp();
			newTimeStamp.name = newEntity.name;
			newTimeStamp.value = varName;
			newTimeStamp.qualifier = nounQual;
			newTimeStamp.time = TIMESTAMP_PREFIX + i;
			verbStory.add(newTimeStamp);
		}
		newSituation.put(verbQual, verbStory);
		newOwner.situation = newSituation;
		story.put(owner, newOwner);
		variables.put(varName, null);
		varCount++;	
	}

	
	public static void represent(LinguisticInfo extractedInformation) {
		loadProcedureLookup();
		entities = extractedInformation.entities;
		////System.err.println(extractedInformation.sentences.size());
		for (LinguisticStep ls : extractedInformation.sentences) {
			Entity currentEntity = new Entity();
			currentEntity.name = ls.entityName;
			currentEntity.value = ls.entityValue;
			////System.out.println("kr" + ls.owner1 + "|" + ls.owner2 + "|" + currentEntity.name + "|" + currentEntity.value + "|" + ls.keyword + "|" + ls.procedureName + "|" + ls.tense);
			if (ls.isQuestion) {
				questionOwner = questionOwner1 = ls.owner1;
				if (!ls.owner2.isEmpty())
					questionOwner2 = ls.owner2;
				questionEntity = ls.entityName;
				if (ls.tense.equals("past") && storyTense.contains(PRESENT))
					questionTime = 0;
				else
					questionTime = timeStep;
				isQuestionAggregator = ls.aggregator;
				isQuestionDifference = ls.difference;
				isQuestionComparator = ls.comparator;
				questionVerb = ls.verbQual;
				continue;
			}
			String nounQual = ls.nounQual, verbQual = ls.verbQual;
			if (verbQual == null)
				verbQual = "has";
			if (nounQual == null)
				nounQual = "";
			reflectChanges(ls.owner1, ls.owner2, currentEntity, ls.keyword, ls.procedureName, ls.tense, nounQual, verbQual);
			displayStory();
		}
		
	}
	
	
	public static void solve() {
		
		System.out.println("ques|"+questionOwner+"|"+questionEntity+"|"+isQuestionAggregator+"|"+isQuestionDifference + "|" + questionVerb+questionTime);
		if (!allEquations.isEmpty()) {
			//////System.out.println(allEquations.size());
			boolean changeFlag = true;
			while (changeFlag) {
				changeFlag = false;
				ArrayList<String> removeEquations = new ArrayList<String>();
				for (String equation : allEquations) {
					//////System.out.println(equation);
					if (equation.split(X_VALUE).length > 2) 
						continue;
					Pattern varPattern = Pattern.compile(VAR_PATTERN);
					Matcher varMatcher = varPattern.matcher(equation);
					String var;
					if (varMatcher.find()) {
						var = varMatcher.group();
						equation = equation.replaceFirst(VAR_PATTERN, "x");
						changeFlag = true;
						removeEquations.add(equation);
						variables.put(var, EquationSolver.getSolution(equation));
						updateValues();
						displayStory();
					}
				}
				for (String rEquation : removeEquations) {
					allEquations.remove(rEquation);
				}
			}
			displayStory();
		}
		if (questionOwner.isEmpty()) {
			//////System.out.println("ques|"+questionOwner+"|"+questionEntity+"|"+questionVerb);
			if (isQuestionAggregator) {
				Iterator<Map.Entry<String, Owner>> it = story.entrySet().iterator();
				String sum = "0";

				// System.out.println("ll"+story.size());
				//1 owner diff times
				if (story.size() == 1) {
					 Map.Entry<String, Owner> pairs = it.next();
				     Owner owner = pairs.getValue();
				     Iterator<Entry<String, ArrayList<TimeStamp>>> it1 = owner.situation.entrySet().iterator();
					 while (it1.hasNext()) {
						Entry<String, ArrayList<TimeStamp>> newPairs = it1.next();
						if (!questionVerb.isEmpty() && !newPairs.getKey().equals(questionVerb) && owner.situation.containsKey(questionVerb)) 
							continue;
						ArrayList<TimeStamp> verbStory = newPairs.getValue();
						for (TimeStamp t : verbStory) {
							System.out.println(questionEntity+"|"+t.name+"|"+owner.name+entities);
							if (!questionEntity.isEmpty() && !questionEntity.equals(t.name) && entities.contains(questionEntity))
								continue;
							if (!t.value.contains("x"))
								sum = sum + "+" + t.value;
							
						}
					 }
				} else {
					boolean verbFlag = false; 
				while (it.hasNext()) {
				     Map.Entry<String, Owner> pairs = it.next();
				     Owner owner = pairs.getValue();
				     Iterator<Entry<String, ArrayList<TimeStamp>>> it1 = owner.situation.entrySet().iterator();
				     while (it1.hasNext()) {
						Entry<String, ArrayList<TimeStamp>> newPairs = it1.next();
						System.out.println("k"+verbFlag+questionVerb+newPairs.getKey()+owner.situation.containsKey(questionVerb));
						if (!questionVerb.isEmpty() && !newPairs.getKey().equals(questionVerb) && (owner.situation.containsKey(questionVerb) || verbFlag))
							continue;
						if (!questionVerb.isEmpty() && newPairs.getKey().equals(questionVerb))
							verbFlag = true;
					    System.out.println("l"+verbFlag);
						ArrayList<TimeStamp> verbStory = newPairs.getValue();
						for (TimeStamp t : verbStory) {
							System.out.println(newPairs.getKey() + "|" + questionEntity+"|"+t.name+"|"+owner.name+entities);
							if (!questionEntity.isEmpty() && !(questionEntity.contains(t.name) || t.name.contains(questionEntity)) && entities.contains(questionEntity))
								continue;
							System.out.println(questionEntity+"|"+t.name+"|"+owner.name+entities);
		
							if (t.time.equals(TIMESTAMP_PREFIX + questionTime) && !t.value.contains("x"))
								sum = sum + "+" + t.value;
							
						}
					 }
				}}
				//////System.out.println("check1" + sum);
				String ans = sum;
				if (ans.contains(X_VALUE)) {
					//////////System.out.println("Cannot be solved!");
					//////////System.out.println("Assuming initial conditions");
					ans = ans.replaceAll(VAR_PATTERN, "");
				}
				//////System.out.println("check2" + ans);
				if (ans.contains("+") || ans.contains("-"))
					finalAns = "Altogether " + EquationSolver.getSolution(ans) + " " + questionEntity;
				else
					finalAns = "Altogether " + ans + " " + questionEntity;
				//////////System.out.println("false");
				return;
			}
			String big = "0", small = "0";
			if (isQuestionDifference) {
				Iterator<Map.Entry<String, Owner>> it = story.entrySet().iterator();
				int counter = 0;
				while (it.hasNext()) {
				     Map.Entry<String, Owner> pairs = it.next();
				     Owner owner = pairs.getValue();
				     Iterator<Entry<String, ArrayList<TimeStamp>>> it1 = owner.situation.entrySet().iterator();
					 while (it1.hasNext()) {
						Entry<String, ArrayList<TimeStamp>> newPairs = it1.next();
						ArrayList<TimeStamp> verbStory = newPairs.getValue();
						for (TimeStamp t : verbStory) {
							//////System.out.println(questionEntity+"|"+t.name);
							if (!questionEntity.isEmpty() && !(questionEntity.contains(t.name) || t.name.contains(questionEntity)) && entities.contains(questionEntity) && !t.name.contains(questionEntity))
								continue;
							if (t.time.equals(TIMESTAMP_PREFIX + questionTime)) {
								if (counter == 0) {
									big = t.value;
									counter++;
								} else {
									small = t.value;
								}
							}
						}
					 }
				}	
				String ans = big + "-" + small;
				if (ans.contains(X_VALUE)) {
					//////////System.out.println("Cannot be solved!");
					//////////System.out.println("Assuming initial conditions");
					ans = ans.replaceAll(VAR_PATTERN, "");
				}
				if (ans.contains("+") || ans.contains("-"))
					finalAns = "Altogether " + EquationSolver.getSolution(ans) + " " + questionEntity;
				else
					finalAns = "Altogether " + ans + " " + questionEntity;
				//////////System.out.println("false");
				return;
			}
		}
		else if (questionEntity.isEmpty()) {
			Owner owner = story.get(questionOwner);
			Iterator<Entry<String, ArrayList<TimeStamp>>> it1 = owner.situation.entrySet().iterator();
			String sum = "0", big = "0", small = "0";
			int counter = 0;
			if (owner.situation.containsKey(questionVerb)) {
				ArrayList<TimeStamp> verbStory = owner.situation.get(questionVerb);
			     for (TimeStamp t : verbStory) {
			    	 if (counter == 0) {
			    		 big = t.value;
			    		 counter++;
			    	 }
			    	 else
			    		 small = t.value;
			    	 sum = sum + "+" + t.value;
			     }
			     if (!isQuestionAggregator && !isQuestionDifference) {
			    	String ans = sum;
					if (ans.contains(X_VALUE)) {
						////////System.out.println("Cannot be solved!");
						////////System.out.println("Assuming initial conditions");
						ans = ans.replaceAll(VAR_PATTERN, "");
						ans = ans.replaceAll("\\++\\+*", "+");
					}
					//////////System.out.println("aaa"+sum);
					if (ans.contains("+") || ans.contains("-"))
						finalAns = questionOwner + " " + questionVerb + " " + EquationSolver.getSolution(ans);
					else
						finalAns = questionOwner + " " + questionVerb + " " + ans;
			     }
			}
			else {	
			while (it1.hasNext()) {
			     Entry<String, ArrayList<TimeStamp>> newPairs = it1.next();
			     ArrayList<TimeStamp> verbStory = newPairs.getValue();
			     for (TimeStamp t : verbStory) {
			    	 if (counter == 0) {
			    		 big = t.value;
			    		 counter++;
			    	 }
			    	 else
			    		 small = t.value;
					 sum = sum + "+" + t.value;
			     }
			     if (!isQuestionAggregator && !isQuestionDifference) {
			    	String ans = sum;
					if (ans.contains(X_VALUE)) {
						////////System.out.println("Cannot be solved!");
						////////System.out.println("Assuming initial conditions");
						ans = ans.replaceAll(VAR_PATTERN, "");
						ans = ans.replaceAll("\\++\\+*", "+");
					}
					//////////System.out.println("aaa"+sum);
					if (ans.contains("+") || ans.contains("-"))
						finalAns = questionOwner + " " + questionVerb + " " + EquationSolver.getSolution(ans);
					else
						finalAns = questionOwner + " " + questionVerb + " " + ans;
			     }
			}}	
			//////////System.out.println("aaa"+sum);
			if (isQuestionAggregator) {
				String ans = sum;
				if (ans.contains(X_VALUE)) {
					////////System.out.println("Cannot be solved!");
					////////System.out.println("Assuming initial conditions");
					ans = ans.replaceAll(VAR_PATTERN, "");
					ans = ans.replaceAll("\\++\\+*", "+");
				}
				//////////System.out.println("aaa"+sum);
				if (ans.contains("+") || ans.contains("-"))
					finalAns = questionOwner + " " + questionVerb + " " + EquationSolver.getSolution(ans);
				else
					finalAns = questionOwner + " " + questionVerb + " " + ans;
			}
			if (isQuestionDifference) {
				String ans = big + "-" + small;
				if (ans.contains(X_VALUE)) {
					////////System.out.println("Cannot be solved!");
					////////System.out.println("Assuming initial conditions");
					ans = ans.replaceAll(VAR_PATTERN, "");
					ans = ans.replaceAll("\\++\\+*", "+");
				}
				//////////System.out.println("aaa"+sum);
				if (ans.contains("+") || ans.contains("-"))
					finalAns = questionOwner + " " + questionVerb + " " + EquationSolver.getSolution(ans);
				else
					finalAns = questionOwner + " " + questionVerb + " " + ans;
			}
			return;
		}
		if (!isQuestionDifference && questionOwner.isEmpty() && questionEntity.isEmpty()) {
			Iterator<Map.Entry<String, Owner>> it = story.entrySet().iterator();
			String sum = "0";
			while (it.hasNext()) {
			     Map.Entry<String, Owner> pairs = it.next();
			     Owner owner = pairs.getValue();
			     Iterator<Entry<String, ArrayList<TimeStamp>>> it1 = owner.situation.entrySet().iterator();
				 while (it1.hasNext()) {
					Entry<String, ArrayList<TimeStamp>> newPairs = it1.next();
					ArrayList<TimeStamp> verbStory = newPairs.getValue();
					for (TimeStamp t : verbStory) {
						if (t.time.equals(TIMESTAMP_PREFIX + questionTime))
							sum = sum + "+" + t.value;
					}
				 }
			}	
			finalAns = "Altogether " + EquationSolver.getSolution(sum);
			return;
		}
		if (isQuestionDifference && questionOwner.isEmpty() && questionEntity.isEmpty()) {
			Iterator<Map.Entry<String, Owner>> it = story.entrySet().iterator();
			String big = "0", small = "0";
			int counter = 0;
			while (it.hasNext()) {
			     Map.Entry<String, Owner> pairs = it.next();
			     Owner owner = pairs.getValue();
			     Iterator<Entry<String, ArrayList<TimeStamp>>> it1 = owner.situation.entrySet().iterator();
				 while (it1.hasNext()) {
					Entry<String, ArrayList<TimeStamp>> newPairs = it1.next();
					ArrayList<TimeStamp> verbStory = newPairs.getValue();
					for (TimeStamp t : verbStory) {
						if (counter == 0) {
					    	 big = t.value;
					    	 counter++;
					    }
					    else
					    	 small = t.value;
					}
				 }
			}	
			finalAns = EquationSolver.getSolution(big + "-" +small) + " left";
			return;
		}
		if (isQuestionDifference && !questionOwner.isEmpty() && !questionEntity.isEmpty()) {
			Owner owner = story.get(questionOwner);
			Iterator<Entry<String, ArrayList<TimeStamp>>> it1 = owner.situation.entrySet().iterator();
			String small = "0",big = "0";
			int counter = 0;
			while (it1.hasNext()) {
				Entry<String, ArrayList<TimeStamp>> newPairs = it1.next();
				ArrayList<TimeStamp> verbStory = newPairs.getValue();
				for (TimeStamp t : verbStory) {
					if (t.name.equals(questionEntity)) {
						if (counter == 0) {
							big = t.value;
							counter++;
						}
						else
							small = t.value;
					}
				}
				
			 }
			if (small.isEmpty()) {
				Iterator<Entry<String, Owner>> it = story.entrySet().iterator();
				Owner newOwner = null;
				while (it.hasNext()) {
					Owner tempOwner = it.next().getValue(); 
					if (!tempOwner.name.equals(questionOwner))
						newOwner = tempOwner;
				}
				if (newOwner != null)
					small = newOwner.situation.entrySet().iterator().next().getValue().get(0).value;
				else {
					Iterator<Entry<String, ArrayList<TimeStamp>>> it2 = owner.situation.entrySet().iterator();
					while (it2.hasNext()) {
						Entry<String, ArrayList<TimeStamp>> newPairs = it2.next();
						ArrayList<TimeStamp> verbStory = newPairs.getValue();
						for (TimeStamp t : verbStory) {
							if (!t.value.equals(big)) {
								small = t.value;
							}
						}
						
					 }
				}
			}
			//////System.out.println(big + "-" +small);
			String ans = EquationSolver.getSolution(big + "-" +small);
			if (ans.contains("-"))
				ans = EquationSolver.getSolution(small + "-" + big);
			finalAns = ans + " " + questionEntity + " left";
			return;
		}
		if (isQuestionAggregator && !questionOwner.isEmpty() && !questionEntity.isEmpty()) {
			Owner owner = story.get(questionOwner);
			ArrayList<TimeStamp> verbStory = owner.situation.get(questionVerb);
			String sum = "0";
			for (TimeStamp t : verbStory) {
					if (t.name.contains(questionEntity)) 
						sum = t.value + "+" + sum ;
			}
			finalAns = EquationSolver.getSolution(sum) + " " + questionEntity + " altogether";
			return;
		}
		//repair
		if (isQuestionComparator && !questionEntity.isEmpty()) {
			ArrayList<TimeStamp> verbStory1 = story.get(questionOwner1).situation.get(questionVerb);
			ArrayList<TimeStamp> verbStory2 = story.get(questionOwner2).situation.get(questionVerb);
			String value1 = "", value2 = "";
			for (TimeStamp t : verbStory1) {
				if (t.name.contains(questionEntity) && t.time.equals(TIMESTAMP_PREFIX + questionTime)) 
					value1 = t.value;
			}
			for (TimeStamp t : verbStory2) {
				if (t.name.contains(questionEntity) && t.time.equals(TIMESTAMP_PREFIX + questionTime)) 
					value2 = t.value;
			}
			finalAns = questionOwner1 + " " + questionVerb + " " + EquationSolver.getSolution(value1 + "-" + "(" + value2 + ")") + " " + questionEntity + " more than " + questionOwner2;
			return;
		}
		String ans = null;
		if (questionEntity.isEmpty()) {
			Iterator<Entry<String, ArrayList<TimeStamp>>> it = story.get(questionOwner).situation.entrySet().iterator();
			while (it.hasNext()) {
			     Entry<String, ArrayList<TimeStamp>> pairs = it.next();
			     ArrayList<TimeStamp> verbStory = pairs.getValue();
				 for (TimeStamp t : verbStory) {
					if (t.time.equals(TIMESTAMP_PREFIX + questionTime)) {
						ans = t.value;
						questionEntity = t.name;
					}
				 }
			}
		} else {
			//////System.out.println("check"+questionOwner+"|"+questionVerb);
			ArrayList<TimeStamp> verbStory = null;
			if (questionOwner.isEmpty())
				questionOwner = story.entrySet().iterator().next().getKey();
			if (story.get(questionOwner).situation.containsKey(questionVerb))
				verbStory = story.get(questionOwner).situation.get(questionVerb);
			else {
				if (story.get(questionOwner).situation.containsKey("has"))
					questionVerb = "has";
				else
					questionVerb = story.get(questionOwner).situation.entrySet().iterator().next().getKey();
				verbStory = story.get(questionOwner).situation.get(questionVerb);
			}
			if (verbStory.size() == 1 && (verbStory.get(0).name.contains(questionEntity) || questionEntity.contains(verbStory.get(0).name)))
				ans = verbStory.get(0).value;
			else {
				ans = "0";
				for (TimeStamp t : verbStory) {
					if (t.time.equals(TIMESTAMP_PREFIX + questionTime) && (t.name.contains(questionEntity) || questionEntity.contains(t.name))) 
						ans = ans + "+" + t.value;
			 	}
			}
		}
		if (ans == null) {
			ArrayList<TimeStamp> verbStory = story.get(questionOwner).situation.get(questionVerb);
			String sum = "0";
			for (TimeStamp t : verbStory) {
				if (t.time.equals(TIMESTAMP_PREFIX + questionTime) && t.name.contains(questionEntity)) 
					sum = t.value + "+" + sum;
			 }
			ans = sum;
		}
		//////////System.out.println("a"+ans);
		if (ans.contains(X_VALUE)) {
			//////////System.out.println("Cannot be solved!");
			//////////System.out.println("Assuming initial conditions");
			ans = ans.replaceFirst(VAR_PATTERN, "");
		}
		//////////System.out.println("--");
		if (ans.contains("+") || ans.contains("-"))
			finalAns = questionOwner + " " + questionVerb + " " + EquationSolver.getSolution(ans) + " " + questionEntity;
		else
			finalAns = questionOwner + " " + questionVerb + " " + ans + " " + questionEntity;
	}
	public static void startGUI() {
		JFrame KRGUI = new JFrame();
		KRGUI.setTitle("Explanation");
		KRGUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		KRGUI.setSize(500,500);
		KRGUI.setLocation(300, 200);
		KRGUI.add(new KRPanel(story));
		KRGUI.setVisible(true);
	}
	public static void main(String[] args) {
		ArrayList<LinguisticStep> steps = new ArrayList<LinguisticStep>();
		LinguisticStep s = new LinguisticStep();
		s.owner1 = "Henry";
		s.owner2 = "brother";
		s.isQuestion = false;
		s.tense = PAST;
		s.entityName = "sticker";
		s.entityValue = "5";
		s.procedureName = "changeOut";
		s.keyword = "give";
		s.nounQual = "";
		s.verbQual = "give";
		s.aggregator = false;
		steps.add(s);
		s = new LinguisticStep();
		s.owner1 = "Henry";
		s.owner2 = "";
		s.isQuestion = false;
		s.tense = PRESENT;
		s.entityName = "sticker";
		s.entityValue = "9";
		s.procedureName = "";
		s.keyword = "";
		s.nounQual = "";
		s.verbQual = "has";
		s.aggregator = false;
		steps.add(s);
		s = new LinguisticStep();
		s.owner1 = "Henry";
		s.owner2 = "";
		s.isQuestion = true;
		s.tense = PAST;
		s.entityName = "sticker";
		s.entityValue = "";
		s.procedureName = "";
		s.keyword = "";
		s.verbQual = "has";
		s.aggregator = false;
		steps.add(s);
		LinkedHashSet<String> tempEntities = new LinkedHashSet<String>();
		tempEntities.add("cookie");
		LinguisticInfo info = new LinguisticInfo();
		info.entities = tempEntities;
		info.sentences = steps;
		represent(info);
		solve();
		//////System.out.println(finalAns);
		startGUI();
	}
}
