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
	static String question = "";
	
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
	private static LinkedHashSet<String> owners = new LinkedHashSet<String>();
	
	

	private static void loadProcedureLookup() {
		keywordMap.put("put", CHANGE_OUT);
		keywordMap.put("place", CHANGE_OUT);
		keywordMap.put("plant", CHANGE_OUT);
		keywordMap.put("sell", CHANGE_OUT);
		keywordMap.put("distribute", CHANGE_OUT);
		keywordMap.put("give", CHANGE_OUT);
		keywordMap.put("more than", COMPARE_PLUS);
		keywordMap.put("less than", COMPARE_MINUS);
		keywordMap.put("get", CHANGE_IN);
		keywordMap.put("buy", CHANGE_IN);
		keywordMap.put("pick", CHANGE_IN);
		keywordMap.put("cut", CHANGE_IN);
		keywordMap.put("take", CHANGE_IN);
		keywordMap.put("borrow", CHANGE_IN);
		keywordMap.put("lose", REDUCTION);
		keywordMap.put("remove", REDUCTION);
		keywordMap.put("spend", REDUCTION);
		keywordMap.put("eat", REDUCTION);
		keywordMap.put("more", INCREASE);
		keywordMap.put("carry", INCREASE);
		keywordMap.put("taller", INCREASE);
		keywordMap.put("find", INCREASE);
		keywordMap.put("build", CHANGE_OUT);
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
		//System.out.println(owner + "|update|" + newEntity.name + "|" + tense + "|" + newEntity.value + "|" +timeStep +"|"+nounQual+"|"+verbQual);
		if (!keywordMap.containsKey(verbQual) && !verbQual.equals("has")) {
			if (story.containsKey(owner)) {
				if (!story.get(owner).situation.containsKey("has") && !story.get(owner).situation.containsKey(verbQual))
					updateTimestamp (owner, newEntity, tense, nounQual, "has");
				if (story.get(owner).situation.containsKey("has") && story.get(owner).situation.containsKey(verbQual))
					updateTimestamp (owner, newEntity, tense, nounQual, "has");
			}
			else
				updateTimestamp (owner, newEntity, tense, nounQual, "has") ;
		}
		if (newEntity.name == null)
			return;
		entities.add(newEntity.name);
		if (verbQual.equals("buy") && newEntity.name.contains("dollar")) {
			verbQual = "spend";
		}
		if (newEntity.value.contains("some")) {
			String varName = X_VALUE + varCount++; 
			variables.put(varName, null);
			newEntity.value = newEntity.value.replace("some", varName);
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
			////////System.out.println("aa"+owner+currentSituation.entrySet().size());
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
			////////System.out.println("aa"+verbQual+"|"+verb);
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
						System.out.println(existingValue+"|"+newEntity.value);
						boolean equationFlag = false;
						if (existingValue.contains(X_VALUE) && (existingValue.contains("+") || existingValue.contains("-")))
							equationFlag = true;
						if (newEntity.value.contains(X_VALUE) && (newEntity.value.contains("+") || newEntity.value.contains("-")))
							equationFlag = true;
						if (equationFlag) {
							////////System.out.println("waka");
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
							////////System.out.println("aaaaaaaaaa"+equation);
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
								////////System.out.println("aaaaaaaaaa"+equation+variable);
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
		/*if (!verbStory.isEmpty() && !verbQual.equals("has") && keywordMap.containsKey(verbQual)) {
			String prevTime = TIMESTAMP_PREFIX + (changeTime - 1);
			for (TimeStamp oldTimeStamp : verbStory) {
				if (oldTimeStamp.time.equals(prevTime) && !oldTimeStamp.value.equals(newTimeStamp.value)) {
					newTimeStamp.value = oldTimeStamp.value + "+" + newTimeStamp.value;
				}
			}
		}*/
		if (!contains(verbStory,newTimeStamp))
			verbStory.add(newTimeStamp);
		currentSituation.put(verb, verbStory);
		////////System.out.println("aab"+owner+currentSituation.entrySet().size());
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
				////////System.out.println(variable + "|" + variables.get(variable));
				if (variables.get(variable) != null) {
					////////System.out.println(equation);
					equation = equation.replace(variable, variables.get(variable));
					////////System.out.println(equation);
				}	
			}
			newEquations.add(equation);
		}
		allEquations = newEquations;
	}
	
	private static void reflectChanges(String owner1, String owner2, Entity newEntity,
			   String keyword, String procedure, String tense, String nounQual, String verbQual) {
		System.out.println(owners);
		
		if (!keyword.isEmpty() && !keyword.contains("more") && !keyword.contains("less")) {
			verbQual = keyword;
		}
		if (verbQual.equals("buy") && newEntity.name.contains("dollar")) {
			verbQual = "spend";
			keyword = verbQual;
			procedure = keywordMap.get(keyword);
			System.out.println("check"+verbQual+procedure);
		}
		
		if (owner1.isEmpty()) {
			if (procedure != null && (procedure.contains("change") || procedure.contains("compare") || procedure.contains("Eq"))) {
			for (String owner: owners) {
				if (!owner2.isEmpty() && !owner.isEmpty()) {
					if (owner.contains(owner2) || owner2.contains(owner))
						break;
					if (owners.size() > 1 && story.containsKey(owner)) {
						owner1 = owner;
						break;
					}
				}
			}
			}
			if (owner1.isEmpty())
				owner1 = UNKNOWN + "0"; 
		}
		else if (owner2.isEmpty()) {
			if (procedure != null && (procedure.contains("change") || procedure.contains("compare") || procedure.contains("Eq"))) {
			for (String owner: owners) {
				//System.err.println(owner+"|"+owner1);
				if (!owner1.isEmpty() && !owner.isEmpty()) {
					if (owner.contains(owner1) || owner1.contains(owner))
						break;
					if (owners.size() > 1 && story.containsKey(owner)) {
						owner2 = owner;
						break;
					}
				}
			}}
			////System.out.println(owner2+"|"+owner1);
			if (owner2.isEmpty())
				owner2 = UNKNOWN + "0";
		}
		// There is no keyword here, an entity has been assigned a value
		System.out.println("e"+owner1 + "|" + owner2 + "|" + keyword + "|" + procedure + "|" + tense + "|" + newEntity.value +"|"+entities);
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
				//////////System.out.println("aa"+owner+currentSituation.entrySet().size());
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
			System.out.println(verbQual+"||"+verb);
			updateTimestamp (owner, newEntity, tense, nounQual, verb);				
		}
		if (procedure.isEmpty() && newEntity.name != null) 
			return;
		String verb = verbQual;
		String newName1 = "", newName2 = "";
		if (!keyword.contains("more") && !keyword.contains("less"))
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
			if (procedure.equals(REDUCTION)) {
				Iterator<Entry<String, Owner>> it = story.entrySet().iterator();
				while (it.hasNext()) {
					Entry<String,Owner> entry = it.next();
					if (!entry.getKey().equals(owner1)) {
						Iterator<Entry<String, ArrayList<TimeStamp>>> it1 = entry.getValue().situation.entrySet().iterator();
						while (it1.hasNext()) {
							Entry<String, ArrayList<TimeStamp>> newEntry = it1.next();
							Entity temp = new Entity();
							temp.name = newEntity.name;
							temp.value = newEntity.value;
							if (!keywordMap.containsKey(newEntry.getKey())) {
								ArrayList<TimeStamp> verbStory = newEntry.getValue();
								for (TimeStamp t : verbStory) {
									if (t.name.equals(newEntity.name) && t.time.equals(TIMESTAMP_PREFIX+timeStep)) {
										temp.value = t.value + "-" + newEntity.value; 
										break;
									}
								}
								updateTimestamp (entry.getKey(), temp, tense, nounQual, newEntry.getKey());
								break;
							}
						}
					}
				}
			}
			if (!owner1.isEmpty()) {
				if (!keywordMap.containsKey(verb) && story.containsKey(owner1) && story.get(owner1).situation.containsKey("has")) {
					ArrayList<TimeStamp> verbStory = story.get(owner1).situation.get("has");
					for (TimeStamp currentTimeStamp : verbStory) {
						if (currentTimeStamp.name.equals(newEntity.name) && currentTimeStamp.time.equals(TIMESTAMP_PREFIX+timeStep+"")) {
							oldValue1 = currentTimeStamp.value;
						}
					}
					verb = "has";
				}
				else{	
				
				addOwner(owner1, newEntity, nounQual, verb);
				ArrayList<TimeStamp> verbStory = story.get(owner1).situation.get(verb);
				for (TimeStamp currentTimeStamp : verbStory) {
					if (currentTimeStamp.name.equals(newEntity.name) && currentTimeStamp.time.equals(TIMESTAMP_PREFIX+timeStep+"")) {
						oldValue1 = currentTimeStamp.value;
					}
				}}
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
		////System.err.println("aa"+oldValue2);
		if (oldValue2 == null || oldValue2.isEmpty()) {
			////System.err.println("aa"+oldValue2);
			Entity correctEntity = resolveNullEntity(owner2,newEntity.name, nounQual, verb);
			oldValue2 = correctEntity.value;
			newName2 = correctEntity.name;
		}
		
		}
		System.err.println("aa"+oldValue1+"|"+oldValue2);
		if (oldValue1 == null || oldValue1.isEmpty()) {
			Entity correctEntity = resolveNullEntity(owner1,newEntity.name, nounQual, verb);
			oldValue1 = correctEntity.value;
			newName1 = correctEntity.name;
		}
		if (procedure.contains(CHANGE)) {
			timeStep++;
			tense = "";
		}
		
		////System.out.println("n"+newName1+newName2);
		String[] steps = procedureMap.get(procedure).split("\\.");
		System.out.println(procedure + "|" + procedureMap.get(procedure) + "|" + steps.length + owner1 + "|" + oldValue1 + "|" + owner2 + "|" + oldValue2);
		if (steps.length > NO_OWNERS_SUPPORTED) {
			////////////System.out.println("Invalid procedure description");
			//////System.exit(0);
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
				System.out.println("eee"+owner+"|"+modifiedEntity.name+"|"+modifiedEntity.value);
				updateTimestamp(owner, modifiedEntity, tense, nounQual, verb);
			}
			else {
				allEquations.add(step);
			}
		}
		inertia();
		////////System.out.println("hello");
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
			////////////System.out.println(procedure + "|" + procedureMap.get(procedure) + "|" + steps.length + owner1 + "|" + oldValue1 + "|" + owner2 + "|" + oldValue2);
			if (steps.length > NO_OWNERS_SUPPORTED) {
				////////////System.out.println("Invalid procedure description");
				//////System.exit(0);
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
					//	////////System.out.println("eee"+owner+"|"+modifiedEntity.name+"|"+modifiedEntity.value);
					updateTimestamp(owner, modifiedEntity, tense, nounQual, "has");
				}
				else
					allEquations.add(step);
			}
			////////System.out.println("hello1");
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
				if (!verb.equals("has"))
					continue;
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
		if (owner.isEmpty() || !story.containsKey(owner)) {
			Entity answer = new Entity();
			answer.name = name;
			answer.value = null;
			addOwner(owner, answer, nounQual, verbQual);
			ArrayList<TimeStamp> verbStory = story.get(owner).situation.get(verbQual);
			for (TimeStamp t : verbStory) {
				if (t.name.equals(name) && t.time.equals(TIMESTAMP_PREFIX + timeStep)) {
					answer.value = t.value;
					return answer;
				}
			}	
		}
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
		////System.err.println("a"+answer.name+"|"+answer.value);
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

	
	public static void represent(LinguisticInfo extractedInformation, String q) {
		loadProcedureLookup();
		question = q;
		entities = extractedInformation.entities;
		owners = extractedInformation.owners;
		//////System.err.println(extractedInformation.sentences.size());
		for (LinguisticStep ls : extractedInformation.sentences) {
			Entity currentEntity = new Entity();
			currentEntity.name = ls.entityName;
			currentEntity.value = ls.entityValue;
			System.out.println("kr" + ls.owner1 + "|" + ls.owner2 + "|" + currentEntity.name + "|" + currentEntity.value + "|" + ls.keyword + "|" + ls.procedureName + "|" + ls.tense +"|"+ls.verbQual);
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
		boolean ownerSwap = false;
		System.out.println(owners);
		if (!story.containsKey(questionOwner) && !questionOwner.isEmpty()) {
			if (story.entrySet().iterator().next().getKey().contains(questionOwner))
				questionOwner = story.entrySet().iterator().next().getKey();
			else {
				questionOwner = "";
				ownerSwap = true;
			}
		}
		if (!owners.contains(questionOwner))
			questionOwner = "";
		if (questionVerb.equals("spend"))
			questionEntity = "dollar";
		if (questionVerb.equals("weigh"))
			questionEntity = "";
		//if (questionOwner.isEmpty() && questionVerb.equals("has"))
			//questionVerb = "";
		System.out.println("ques|"+questionOwner1+"|"+questionOwner2+"|"+questionEntity+"|"+isQuestionAggregator+"|"+isQuestionDifference + "|" + isQuestionComparator+"|"+questionVerb+questionTime);
		
		if (!allEquations.isEmpty()) {
			////////System.out.println(allEquations.size());
			boolean changeFlag = true;
			while (changeFlag) {
				changeFlag = false;
				ArrayList<String> removeEquations = new ArrayList<String>();
				for (String equation : allEquations) {
					////////System.out.println(equation);
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
		if (isQuestionComparator && questionOwner2.isEmpty()) {
			Iterator<Map.Entry<String, Owner>> it = story.entrySet().iterator();
			int counter = 0;
			String big = "", small = "";
			while (it.hasNext()) {
			     Map.Entry<String, Owner> pairs = it.next();
			     Owner owner = pairs.getValue();
			     counter = 0;
			     Iterator<Entry<String, ArrayList<TimeStamp>>> it1 = owner.situation.entrySet().iterator();
				 while (it1.hasNext()) {
					 Entry<String, ArrayList<TimeStamp>> newPairs = it1.next();
					 ArrayList<TimeStamp> verbStory = newPairs.getValue();
					for (TimeStamp t : verbStory) {
						if (!questionEntity.isEmpty() && !(questionEntity.contains(t.name) || t.name.contains(questionEntity)) && entities.contains(questionEntity) && !t.name.contains(questionEntity))
							continue;
						if (!t.value.contains("x")) {
							System.err.println(t.value);
							if (counter == 0  && big.isEmpty()) {
								big = t.value;
								counter++;
							} else if (!t.value.contains("x") && !t.value.equals(big)){
								small = t.value;
							}
						}
					}
				 }
			}
			finalAns = questionOwner1 + " " + questionVerb + " " + EquationSolver.getSolution(big + "-" + "(" + small + ")") + " " + questionEntity + " more than " + questionOwner2;
			return;
		}
		if (questionOwner.isEmpty()) {
			////////System.out.println("ques|"+questionOwner+"|"+questionEntity+"|"+questionVerb);
			if (isQuestionAggregator) {
				Iterator<Map.Entry<String, Owner>> it = story.entrySet().iterator();
				String sum = "0";
				ArrayList<String> candidates = new ArrayList<String>();
				ArrayList<String> own = new ArrayList<String>();
				// //System.out.println("ll"+story.size());
				//1 owner diff times
				if (story.size() == 1) {
					 Map.Entry<String, Owner> pairs = it.next();
					 Owner owner = pairs.getValue();
				     Iterator<Entry<String, ArrayList<TimeStamp>>> it1 = owner.situation.entrySet().iterator();
					 while (it1.hasNext()) {
						Entry<String, ArrayList<TimeStamp>> newPairs = it1.next();
						if (!questionVerb.isEmpty() && !newPairs.getKey().equals(questionVerb) && owner.situation.containsKey(questionVerb) && !ownerSwap) 
							continue;
						ArrayList<TimeStamp> verbStory = newPairs.getValue();
						for (TimeStamp t : verbStory) {
							System.out.println(questionEntity+"|"+t.name+"|"+owner.name+entities);
							if (!questionEntity.isEmpty() && !questionEntity.equals(t.name) && entities.contains(questionEntity))
								continue;
							if (newPairs.getKey().isEmpty())
								continue;
							if (!t.value.contains("x")) {
								//if((newPairs.getKey().equals("has") && (t.value.endsWith(".0")||t.value.contains("+")||t.value.contains("-")) && t.time.equals(TIMESTAMP_PREFIX+questionTime)) || !newPairs.getKey().equals("has"))
									sum = sum + "+" + t.value;
							}
							System.err.println(sum+newPairs.getKey()+"|");
						}
					 }
				} else {
					boolean verbFlag = false;
					String sum1 = "0";
				while (it.hasNext()) {
				     Map.Entry<String, Owner> pairs = it.next();
				     Owner owner = pairs.getValue();
				     sum1 = "0";
				     Iterator<Entry<String, ArrayList<TimeStamp>>> it1 = owner.situation.entrySet().iterator();
				     while (it1.hasNext()) {
						Entry<String, ArrayList<TimeStamp>> newPairs = it1.next();
						//System.out.println("k"+verbFlag+questionVerb+newPairs.getKey()+owner.situation.containsKey(questionVerb));
						if (!questionVerb.isEmpty()  && !newPairs.getKey().equals(questionVerb) && (owner.situation.containsKey(questionVerb) || verbFlag))
							continue;
						if (!questionVerb.isEmpty() && newPairs.getKey().equals(questionVerb))
							verbFlag = true;
					    //System.out.println("l"+verbFlag);
						ArrayList<TimeStamp> verbStory = newPairs.getValue();
						for (TimeStamp t : verbStory) {
							//System.out.println(newPairs.getKey() + "|" + questionEntity+"|"+t.name+"|"+owner.name+entities);
							if (!questionEntity.isEmpty() && !(questionEntity.contains(t.name) || t.name.contains(questionEntity)) && entities.contains(questionEntity))
								continue;
							//System.out.println(questionEntity+"|"+t.name+"|"+owner.name+entities);
		
							if (!t.value.contains("x")) 
								if((newPairs.getKey().equals("has") && t.time.equals(TIMESTAMP_PREFIX+questionTime)) || !newPairs.getKey().equals("has")) {
									sum = sum + "+" + t.value;
									sum1 = sum1 + "+" + t.value;
								}
							
						}
					 }
				     candidates.add(sum1);
				     own.add(pairs.getKey());
				}}
				String ans = "";
				if (questionVerb.isEmpty() && story.size() != 1)
				for (String candidate : candidates) {
					System.out.println("aa"+candidate+" "+question.contains(" "+candidate.replace("0+", "").trim()+" "));
					if (candidate!=null && !candidate.contains("x") && !question.contains(" "+candidate.replace("0+", "").trim()+" ")) {
						ans = candidate;
						questionOwner = own.get(candidates.indexOf(candidate));
					}
				}
				else
					ans = sum;
				System.out.println("check2" + ans);
				if (ans.contains(X_VALUE)) {
					////////////System.out.println("Cannot be solved!");
					////////////System.out.println("Assuming initial conditions");
					ans = ans.replaceAll(VAR_PATTERN, "");
				}
				////////System.out.println("check2" + ans);
				if (ans.contains("+") || ans.contains("-"))
					finalAns = "Altogether " + EquationSolver.getSolution(ans) + " " + questionEntity;
				else
					finalAns = "Altogether " + ans + " " + questionEntity;
				if (!finalAns.contains(".") || question.contains(" " + ans + " ") || question.contains(" " + EquationSolver.getSolution(ans)+" ")) {
					while (it.hasNext()) {
					     Map.Entry<String, Owner> pairs = it.next();
					     Owner owner = pairs.getValue();
					     Iterator<Entry<String, ArrayList<TimeStamp>>> it1 = owner.situation.entrySet().iterator();
					     while (it1.hasNext()) {
							Entry<String, ArrayList<TimeStamp>> newPairs = it1.next();
							ArrayList<TimeStamp> verbStory = newPairs.getValue();
							for (TimeStamp t : verbStory) {
								//System.out.println(newPairs.getKey() + "|" + questionEntity+"|"+t.name+"|"+owner.name+entities);
								if (!questionEntity.isEmpty() && !(questionEntity.contains(t.name) || t.name.contains(questionEntity)) && entities.contains(questionEntity))
									continue;
								//System.out.println(questionEntity+"|"+t.name+"|"+owner.name+entities);
			
								if (!t.value.contains("x"))
									sum = sum + "+" + t.value;
								
							}
						 }
					}
					ans = sum;
					if (ans.contains("+") || ans.contains("-"))
						finalAns = "Altogether " + EquationSolver.getSolution(ans) + " " + questionEntity;
					else
						finalAns = "Altogether " + ans + " " + questionEntity;
				
				}
					
				//System.out.println(ans+finalAns);
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
							////////System.out.println(questionEntity+"|"+t.name);
							if (!questionEntity.isEmpty() && !(questionEntity.contains(t.name) || t.name.contains(questionEntity)) && entities.contains(questionEntity) && !t.name.contains(questionEntity))
								continue;
							if (!t.value.contains("x")) {
								if (counter == 0) {
									big = t.value;
									counter++;
								} else if (!t.value.contains("x")){
									small = t.value;
								}
							}
						}
					 }
				}	
				String ans = big + "-" + small;
				if (ans.contains(X_VALUE)) {
					////////////System.out.println("Cannot be solved!");
					////////////System.out.println("Assuming initial conditions");
					ans = ans.replaceAll(VAR_PATTERN, "");
				}
				if (ans.contains("+") || ans.contains("-"))
					finalAns = "Altogether " + EquationSolver.getSolution(ans) + " " + questionEntity;
				else
					finalAns = "Altogether " + ans + " " + questionEntity;
				////////////System.out.println("false");
				return;
			}
			if (isQuestionComparator) {
				Iterator<Map.Entry<String, Owner>> it = story.entrySet().iterator();
				int counter = 0;
				while (it.hasNext()) {
				     Map.Entry<String, Owner> pairs = it.next();
				     Owner owner = pairs.getValue();
				     counter = 0;
				     Iterator<Entry<String, ArrayList<TimeStamp>>> it1 = owner.situation.entrySet().iterator();
					 while (it1.hasNext()) {
						 Entry<String, ArrayList<TimeStamp>> newPairs = it1.next();
						 ArrayList<TimeStamp> verbStory = newPairs.getValue();
						for (TimeStamp t : verbStory) {
							if (!questionEntity.isEmpty() && !(questionEntity.contains(t.name) || t.name.contains(questionEntity)) && entities.contains(questionEntity) && !t.name.contains(questionEntity))
								continue;
							if (!t.value.contains("x")) {
								if (counter == 0) {
									big = t.value;
									counter++;
								} else if (!t.value.contains("x") && !t.value.equals(big)){
									small = t.value;
								}
							}
						}
					 }
				}
				finalAns = questionOwner1 + " " + questionVerb + " " + EquationSolver.getSolution(big + "-" + "(" + small + ")") + " " + questionEntity + " more than " + questionOwner2;
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
						//////////System.out.println("Cannot be solved!");
						//////////System.out.println("Assuming initial conditions");
						ans = ans.replaceAll(VAR_PATTERN, "");
						ans = ans.replaceAll("\\++\\+*", "+");
					}
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
			     if (story.get(questionOwner).situation.containsKey("has")) {
			    	 sum = "0";
			    	 verbStory = story.get(questionOwner).situation.get("has");
				     for (TimeStamp t : verbStory) {
				    	 if (counter == 0) {
				    		 big = t.value;
				    		 counter++;
				    	 }
				    	 else
				    		 small = t.value;
						 sum = sum + "+" + t.value;
				     }
			     }
			     if (!isQuestionAggregator && !isQuestionDifference) {
			    	String ans = sum;
					if (ans.contains(X_VALUE)) {
						//////////System.out.println("Cannot be solved!");
						//////////System.out.println("Assuming initial conditions");
						ans = ans.replaceAll(VAR_PATTERN, "");
						ans = ans.replaceAll("\\++\\+*", "+");
					}
					////////////System.out.println("aaa"+sum);
					if (ans.contains("+") || ans.contains("-"))
						finalAns = questionOwner + " " + questionVerb + " " + EquationSolver.getSolution(ans);
					else
						finalAns = questionOwner + " " + questionVerb + " " + ans;
			     }
			}}
			
			////////////System.out.println("aaa"+sum);
			if (isQuestionAggregator) {
				String ans = sum;
				if (ans.contains(X_VALUE)) {
					//////////System.out.println("Cannot be solved!");
					//////////System.out.println("Assuming initial conditions");
					ans = ans.replaceAll(VAR_PATTERN, "");
					ans = ans.replaceAll("\\++\\+*", "+");
				}
				////////////System.out.println("aaa"+sum);
				if (ans.contains("+") || ans.contains("-"))
					finalAns = questionOwner + " " + questionVerb + " " + EquationSolver.getSolution(ans);
				else
					finalAns = questionOwner + " " + questionVerb + " " + ans;
			}
			if (isQuestionDifference) {
				String ans = big + "-" + small;
				if (ans.contains(X_VALUE)) {
					//////////System.out.println("Cannot be solved!");
					//////////System.out.println("Assuming initial conditions");
					ans = ans.replaceAll(VAR_PATTERN, "");
					ans = ans.replaceAll("\\++\\+*", "+");
				}
				////////////System.out.println("aaa"+sum);
				if (ans.contains("+") || ans.contains("-"))
					finalAns = questionOwner + " " + questionVerb + " " + EquationSolver.getSolution(ans);
				else
					finalAns = questionOwner + " " + questionVerb + " " + ans;
			}
			if (isQuestionComparator) {
				System.out.println("waka");
				ArrayList<TimeStamp> verbStory1 = null;
				ArrayList<TimeStamp> verbStory2 = null;
				if (questionOwner1.isEmpty() || !story.containsKey(questionOwner1))
					questionOwner1 = "unknown0";
				if (questionOwner2.isEmpty() || !story.containsKey(questionOwner2))
					questionOwner2 = "unknown0";
				if (questionOwner2.equals("unknown0") && !story.containsKey("unknown0"))
					questionOwner2 = questionOwner1;
				if (story.get(questionOwner1).situation.containsKey(questionVerb))
					verbStory1 = story.get(questionOwner1).situation.get(questionVerb);
				else
					verbStory1 = story.get(questionOwner1).situation.entrySet().iterator().next().getValue();
				if (story.get(questionOwner2).situation.containsKey(questionVerb) && !questionOwner1.equals(questionOwner2))
					verbStory2 = story.get(questionOwner2).situation.get(questionVerb);
				else if (!questionOwner1.equals(questionOwner2))
					verbStory2 = story.get(questionOwner2).situation.entrySet().iterator().next().getValue();
				else {
					System.out.println("cccc");
					it1 = story.get(questionOwner2).situation.entrySet().iterator();
					while (it1.hasNext()) {
						Entry<String, ArrayList<TimeStamp>> pair = it1.next();
						if (!pair.getKey().equals(questionVerb)) {
							verbStory2 = pair.getValue();
							System.out.println(pair.getKey());
							break;
						}
					}
				}
				String value1 = "", value2 = "";
				for (TimeStamp t : verbStory1) {
					if (t.time.equals(TIMESTAMP_PREFIX + questionTime) || questionOwner1.equals(questionOwner2)) {
						value1 = t.value;
						questionEntity = t.name;
					}
				}
				for (TimeStamp t : verbStory2) {
					if (t.time.equals(TIMESTAMP_PREFIX + questionTime) || questionOwner1.equals(questionOwner2)) { 
						value2 = t.value;
						questionEntity = t.name;
					}
				}
				finalAns = questionOwner1 + " " + questionVerb + " " + EquationSolver.getSolution(value1 + "-" + "(" + value2 + ")") + " " + questionEntity + " more than " + questionOwner2;
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
					if (t.name.equals(questionEntity) && !t.value.contains("x")) {
						if (counter == 0) {
							big = t.value;
							counter++;
						}
						else if (!t.value.contains("x"))
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
			
			////////System.out.println(big + "-" +small);
			String ans = EquationSolver.getSolution(big + "-" +small);
			if (ans.contains("-"))
				ans = EquationSolver.getSolution(small + "-" + big);
			finalAns = ans + " " + questionEntity + " left";
			return;
		}
		if (isQuestionAggregator && !questionOwner.isEmpty() && !questionEntity.isEmpty()) {
			Owner owner = story.get(questionOwner);
			ArrayList<TimeStamp> verbStory = null;
			if (owner.situation.containsKey(questionVerb))
				verbStory = owner.situation.get(questionVerb);
			else if (owner.situation.containsKey("has"))
				verbStory = owner.situation.get("has");
			String sum = "0";
			if (verbStory != null) {
				for (TimeStamp t : verbStory) {
					if (t.name.contains(questionEntity) || questionEntity.contains(t.name)) 
						sum = t.value + "+" + sum ;
				}
				finalAns = questionOwner + " has " + EquationSolver.getSolution(sum) + " " + questionEntity + " altogether";
				System.out.println(" " + sum.replace("+0", "")+" ");
				if (!question.contains(" " + sum.replace("+0", "") + " ")) 
					return;
			} 
				
				if (question.contains(" " + sum.replace("+0", "") + " ")) {
					sum = "0";
					Iterator<Entry<String, ArrayList<TimeStamp>>> it1 = null;
					Iterator<Entry<String, Owner>> it = story.entrySet().iterator();
					while (it.hasNext()) {
						verbStory = it.next().getValue().situation.get("has");
						for (TimeStamp t : verbStory) {
							if (t.name.contains(questionEntity) || questionEntity.contains(t.name)) 
								sum = t.value + "+" + sum ;
							}
					}
					finalAns = EquationSolver.getSolution(sum) + " " + questionEntity + " altogether";
					return;
				}
				Iterator<Entry<String, ArrayList<TimeStamp>>> it1 = owner.situation.entrySet().iterator();
				while (it1.hasNext()) {
					verbStory = it1.next().getValue();
					for (TimeStamp t : verbStory) {
						if (t.name.contains(questionEntity) || questionEntity.contains(t.name)) 
							sum = t.value + "+" + sum ;
					}
				}
				
				return;
		}
		//repair
		if (isQuestionComparator && !questionEntity.isEmpty()) {
			ArrayList<TimeStamp> verbStory1 = null;
			ArrayList<TimeStamp> verbStory2 = null;
			
			if (questionOwner1.isEmpty())
				questionOwner1 = "unknown0";
			if (questionOwner2.isEmpty())
				questionOwner2 = "unknown0";
			if (questionOwner2.equals("unknown0") && !story.containsKey("unknown0"))
				questionOwner2 = questionOwner1;
			if (story.get(questionOwner1).situation.containsKey(questionVerb))
				verbStory1 = story.get(questionOwner1).situation.get(questionVerb);
			else
				verbStory1 = story.get(questionOwner1).situation.entrySet().iterator().next().getValue();
			if (story.get(questionOwner2).situation.containsKey(questionVerb) && !questionOwner1.equals(questionOwner2))
				verbStory2 = story.get(questionOwner2).situation.get(questionVerb);
			else if (!questionOwner1.equals(questionOwner2) || questionOwner2.equals("unknown0"))
				verbStory2 = story.get(questionOwner2).situation.entrySet().iterator().next().getValue();
			else {
				System.out.println("cccc");
				Iterator<Entry<String, ArrayList<TimeStamp>>> it1 = story.get(questionOwner2).situation.entrySet().iterator();
				while (it1.hasNext()) {
					Entry<String, ArrayList<TimeStamp>> pair = it1.next();
					if (!pair.getKey().equals(questionVerb)) {
						verbStory2 = pair.getValue();
						System.out.println(pair.getKey());
						break;
					}
				}
			}
			
			String value1 = "", value2 = "";
			for (TimeStamp t : verbStory1) {
				if (t.name.contains(questionEntity) && t.time.equals(TIMESTAMP_PREFIX + questionTime)) 
					value1 = t.value;
			}
			for (TimeStamp t : verbStory2) {
				if (t.name.contains(questionEntity) && t.time.equals(TIMESTAMP_PREFIX + questionTime)) 
					value2 = t.value;
			}
			if (value1.isEmpty()) {
				for (TimeStamp t : verbStory1) {
					if (t.time.equals(TIMESTAMP_PREFIX + questionTime)) 
						value1 = t.value;
				}
			}
			if (value2.isEmpty()) {
				for (TimeStamp t : verbStory2) {
					if (t.time.equals(TIMESTAMP_PREFIX + questionTime)) 
						value2 = t.value;
				}
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
			System.out.println("check"+questionOwner+"|"+questionVerb);
			ArrayList<TimeStamp> verbStory = null;
			ArrayList<String> candidates = new ArrayList<String>();
			ArrayList<String> own = new ArrayList<String>();
			if (questionOwner.isEmpty()) {
				Iterator<Entry<String, Owner>> it = story.entrySet().iterator();
				while (it.hasNext()) {
					questionOwner = it.next().getKey();
					own.add(questionOwner);
					if (story.get(questionOwner).situation.containsKey(questionVerb))
						verbStory = story.get(questionOwner).situation.get(questionVerb);
					else {
						if (story.get(questionOwner).situation.containsKey("has"))
							questionVerb = "has";
						else
							questionVerb = story.get(questionOwner).situation.entrySet().iterator().next().getKey();
						verbStory = story.get(questionOwner).situation.get(questionVerb);
					}
					//System.out.println(questionVerb);
					if (verbStory.size() == 1 && (verbStory.get(0).name.contains(questionEntity) || questionEntity.contains(verbStory.get(0).name)))
						ans = verbStory.get(0).value;
					else {
						ans = "0";
						for (TimeStamp t : verbStory) {
							if (t.time.equals(TIMESTAMP_PREFIX + questionTime) && (t.name.contains(questionEntity) || questionEntity.contains(t.name))) 
								ans = ans + "+" + t.value;
					 	}
					}
					candidates.add(ans);
				}
				ans = "";
				for (String candidate : candidates) {
					//System.out.println(question.contains(" "+ans.replace("0+", "")+" "));
					if (candidate!=null && !candidate.contains("x") && !question.contains(" "+candidate.replace("0+", "")+" ")) {
						ans = candidate;
						System.out.println("c"+candidate);
						questionOwner = own.get(candidates.indexOf(candidate));
					}
				}
				System.out.println("aaa"+ans);
				if (ans.isEmpty()) {
					String sum = "0";
					System.out.println("check1"+questionOwner+"|"+questionVerb);
					for (TimeStamp t : verbStory) {
						//System.out.println("waka");
						if ((t.name.contains(questionEntity) || questionEntity.contains(t.name))) 
							sum = t.value + "+" + sum;
					 }
					ans = sum;
				}
			}
				
		}
		
		if (ans == null) {
			System.out.println("checkx"+questionOwner+"|"+questionVerb);
			ArrayList<TimeStamp> verbStory = null;
			if (story.get(questionOwner).situation.containsKey(questionVerb))
				verbStory = story.get(questionOwner).situation.get(questionVerb);
			else {
				if (story.get(questionOwner).situation.containsKey("has"))
					questionVerb = "has";
				else
					questionVerb = story.get(questionOwner).situation.entrySet().iterator().next().getKey();
				verbStory = story.get(questionOwner).situation.get(questionVerb);
			}
			String sum = "0";
			//System.out.println("check1"+questionOwner+"|"+questionVerb);
			
			for (TimeStamp t : verbStory) {
				//System.out.println("waka");
				if (t.time.equals(TIMESTAMP_PREFIX + questionTime) && (t.name.contains(questionEntity) || questionEntity.contains(t.name))) 
					sum = t.value + "+" + sum;
			 }
			ans = sum;
			if (question.contains(ans.replace("+0", ""))) {
				sum = "0";
				
				System.out.println("check1"+questionOwner+"|"+questionVerb);
				for (TimeStamp t : verbStory) {
					//System.out.println("waka");
					if ((t.name.contains(questionEntity) || questionEntity.contains(t.name))) 
						sum = t.value + "+" + sum;
				 }
			}
			ans = sum;
		}
		System.out.println("bbb"+ans+"|");
		if (ans.equals("0")) {
			
			ArrayList<TimeStamp> verbStory = null;
			ArrayList<String> candidates = new ArrayList<String>();
			ArrayList<String> own = new ArrayList<String>();
			Iterator<Entry<String, Owner>> it = story.entrySet().iterator();
			while (it.hasNext()) {
				questionOwner = it.next().getKey();
				own.add(questionOwner);
				if (story.get(questionOwner).situation.containsKey(questionVerb))
					verbStory = story.get(questionOwner).situation.get(questionVerb);
				else {
					if (story.get(questionOwner).situation.containsKey("has"))
						questionVerb = "has";
					else
						questionVerb = story.get(questionOwner).situation.entrySet().iterator().next().getKey();//check
					verbStory = story.get(questionOwner).situation.get(questionVerb);
				}
				if (verbStory.size() == 1 && (verbStory.get(0).name.contains(questionEntity) || questionEntity.contains(verbStory.get(0).name)))
					ans = verbStory.get(0).value;
				else {
					ans = "0";
					for (TimeStamp t : verbStory) {
						if ((t.name.contains(questionEntity) || questionEntity.contains(t.name))) 
							ans = ans + "+" + t.value;
				 	}
				}
				System.out.println("bbb"+ans+"|");
				candidates.add(ans);
			}
			for (String candidate : candidates) {
				if (candidate!=null && !candidate.contains("x") && !candidate.equals("0")) {
					ans = candidate;
					questionOwner = own.get(candidates.indexOf(candidate));
				}
			}
			System.out.println("aaa"+ans);
			if (question.contains(ans.replace("+0", ""))) {
				String sum = "0";
				System.out.println("check1"+questionOwner+"|"+questionVerb);
				for (TimeStamp t : verbStory) {
					//System.out.println("waka");
					if ((t.name.contains(questionEntity) || questionEntity.contains(t.name))) 
						sum = t.value + "+" + sum;
				 }
				ans = sum;
			}
		}
		////////////System.out.println("a"+ans);
		if (ans.contains(X_VALUE)) {
			////////////System.out.println("Cannot be solved!");
			////////////System.out.println("Assuming initial conditions");
			ans = ans.replaceFirst(VAR_PATTERN, "");
		}
		////////////System.out.println("--");
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
		//represent(info);
		solve();
		////////System.out.println(finalAns);
		startGUI();
	}
}
