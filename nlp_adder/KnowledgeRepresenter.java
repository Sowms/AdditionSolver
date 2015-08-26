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



class TimeStamp {
	String time;
	String entity;
	Set value;
}

class State extends ArrayList<TimeStamp> {};

class Situation extends HashMap<String, State> {}; // verb, state


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
	static char setName = 'A';
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

	static HashMap<String,Set> sets = new HashMap<>();
	static HashMap<String,Situation> story = new HashMap<>();
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
		keywordMap.put("stack", CHANGE_OUT);
		keywordMap.put("add", CHANGE_OUT);
		keywordMap.put("sell", CHANGE_OUT);
		keywordMap.put("distribute", CHANGE_OUT);
		keywordMap.put("give", CHANGE_OUT);
		keywordMap.put("load", CHANGE_OUT);
		keywordMap.put("build", CHANGE_OUT);
		
		keywordMap.put("more than", COMPARE_PLUS);
		keywordMap.put("less than", COMPARE_MINUS);
		keywordMap.put("get", CHANGE_IN);
		keywordMap.put("buy", CHANGE_IN);
		keywordMap.put("pick", CHANGE_IN);
		keywordMap.put("cut", CHANGE_IN);
		keywordMap.put("take", CHANGE_IN);
		keywordMap.put("borrow", CHANGE_IN);
		keywordMap.put("lose", REDUCTION);
		keywordMap.put("use", REDUCTION);
		keywordMap.put("leave", REDUCTION);
		keywordMap.put("transfer", REDUCTION);
		keywordMap.put("spill", REDUCTION);
		keywordMap.put("remove", REDUCTION);
		keywordMap.put("spend", REDUCTION);
		keywordMap.put("eat", REDUCTION);
		keywordMap.put("more", INCREASE);
		keywordMap.put("immigrate", INCREASE);
		keywordMap.put("increase", INCREASE);
		keywordMap.put("carry", INCREASE);
		keywordMap.put("saw", REDUCTION);
		keywordMap.put("taller", INCREASE);
		//keywordMap.put("find", INCREASE);
		keywordMap.put("decrease", REDUCTION);
		keywordMap.put("break", REDUCTION);
		
		
		procedureMap.put(CHANGE_OUT, "[owner1]-[entity].[owner2]+[entity]");
		procedureMap.put(CHANGE_IN, "[owner1]+[entity].[owner2]-[entity]");
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
		setName = 'A';
		isQuestionAggregator = false;
		isQuestionDifference = false;
		isQuestionComparator = false;

		questionTime = 0;
		questionEntity = "";
		questionOwner = "";
		questionOwner1 = "";
		questionOwner2 = "";
		finalAns = "";

		story = new HashMap<String,Situation>();
		variables = new HashMap<String,String>();
		procedureMap = new HashMap<String,String>();
		entities = new LinkedHashSet<String>();
		storyTense = new ArrayList<String>();
		allEquations = new ArrayList<String>();
		
		sets.put(Set.Empty.name, Set.Empty);
	}
	
	public static String commonString(String s1, String s2) {
		   for (int i = Math.min(s1.length(), s2.length()); ; i--) {
		       if (s2.endsWith(s1.substring(0, i))) {
		    	   return s1.substring(0, i);
		       }
		   }    
	}

	private static void updateTimestamp (String owner, Set value, 
			String tense, String verbQual, String entity) {
		//System.out.println(owner + "|update|" +  "|" +timeStep +"|"+ verbQual);
		String changeTime = "";
		if (tense.equals(PAST) && storyTense.contains(PRESENT))
			changeTime  = "0";
		else
			changeTime = timeStep + "";
		storyTense.add(tense);
		String time = TIMESTAMP_PREFIX + changeTime;
		Situation newSituation = new Situation();
		if (story.containsKey(owner)) {
			newSituation = story.get(owner);
		}
		State newState = new State();
		if (!newSituation.isEmpty() && newSituation.containsKey(verbQual))
			newState = newSituation.get(verbQual);
		Set existingValue = new Set();
		String lhs = "", rhs = value.cardinality;
		for (TimeStamp t : newState) {
			if (t.time.equals(time)) {
				existingValue = t.value;
				lhs = existingValue.cardinality;
				Set unknown;
				if (lhs.contains("x") || rhs.contains("x")) {
					String ans = EquationSolver.getSolution(lhs + "=" + rhs);
					Set replace = null;
					if (lhs.contains("x")) 
						replace = existingValue;
					else
						replace = value;
					unknown = replace.unknownComponent();
					if (ans.endsWith(".0"))
						ans = ans.replace(".0", ""); 
					unknown.cardinality = ans;
					replace.components.put(unknown.name, unknown);
					replace.computeCardinality();
				}
			}
		}
		TimeStamp t = new TimeStamp();
		t.time = time;
		t.value = value;
		t.entity = entity;
		newState.add(t);
		newSituation.put(verbQual,newState);
		story.put(owner, newSituation);
		if (!keywordMap.containsKey(verbQual) && !verbQual.equals("has"))
			updateTimestamp(owner,value,tense,"has",entity);
	}
	private static void reflectChanges(String owner1, String owner2, Entity newEntity,
			   String keyword, String procedure, String tense, String nounQual, String verbQual) {
		//System.out.println(owners);
		Set newSet = new Set();
		if (newEntity.value.equals("some"))
			newSet.cardinality = "x";
		else
			newSet.cardinality = newEntity.value;
		String entity = newEntity.name;
		newSet.name = setName + "";
		newSet.compute = newSet.name;
		sets.put(newSet.name, newSet);
		int next = setName + 1;
		setName = (char) next;
		
		if (!keyword.isEmpty() && !keyword.contains("more") && !keyword.contains("less")) {
			verbQual = keyword;
		}
		if (verbQual.equals("buy") && newEntity.name != null && newEntity.name.contains("dollar")) {
			verbQual = "spend";
			keyword = verbQual;
			procedure = keywordMap.get(keyword);
			//System.out.println("check"+verbQual+procedure);
		}
		
		if (owner1.isEmpty()) {
			if (procedure != null && (procedure.contains("change") || procedure.contains("compare") 
					|| procedure.contains("Eq"))) {
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
			else if (!owners.isEmpty()) {
				for (String owner: owners) {
					if (story.containsKey(owner)) {
							owner1 = owner;
							break;
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
						if (owner.equals(owner1))
							break;
						if (owners.size() > 1 && story.containsKey(owner)) {
							owner2 = owner;
							break;
						}
					}
				}	
			}
			//System.err.println(owner2+"|"+owner1);
			if (owner2.isEmpty())
				owner2 = UNKNOWN + "0";
		}
		// There is no keyword here, an entity has been assigned a value
		if (procedure != null && procedure.equals(COMPARE_PLUS) && owner2.isEmpty())
			procedure = INCREASE;
		if (owner1.contains(UNKNOWN) && !owners.isEmpty())
			owner1 = owners.iterator().next();
		if (owner2.contains(UNKNOWN) && !owners.isEmpty() && entities.contains(owner1)) {
			for (String owner: owners) {
				if (story.containsKey(owner)) {
						owner2 = owner;
						break;
				}
			}
		}
			
		
		//System.out.println("e"+owner1 + "|" + owner2 + "|" + keyword + "|" + procedure + "|" + tense + "|" + newEntity.value +"|"+entities);
		if (newEntity.name == null)
			newEntity.name = entities.iterator().next();
		String owner = "";
		if (procedure == null)
			procedure = "";
		if (procedure.contains(CHANGE)) 
			timeStep++;
		if (!procedure.contains("Eq") && (keyword.equals(verbQual) || keyword.isEmpty())) {	
			if (entities.contains(owner1))
				owner = owner2;
			else
				owner = owner1;
			HashMap<String, State> currentSituation = new HashMap<>();
			if (story.containsKey(owner)) {
				currentSituation = story.get(owner);
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
			//System.out.println(verbQual+"||"+verb);
			updateTimestamp (owner, newSet, tense, verb, entity);				
		}
		if (entities.contains(owner1))
			owner = owner2;
		else
			owner = owner1;
		if (procedure.contains(CHANGE)) { 
			tense = "";
			timeStep++;
		}
		if (procedure.isEmpty() && newEntity.name != null) {
			displayStory();
			return;
		}
		String verb = verbQual;
		String newName1 = "", newName2 = "";
		if (!keyword.contains("more") && !keyword.contains("less"))
			verb = "has";
		Set oldValue1 = new Set(), oldValue2 = new Set();
		try {
			State verbStory = story.get(owner1).get(verb);
			//modularize
			for (TimeStamp currentTimeStamp : verbStory) {
				if (currentTimeStamp.entity.equals(newEntity.name)) {
					oldValue1 = currentTimeStamp.value;
				}
			}
		} catch (NullPointerException ex) {
			Set correctValue = resolveNullEntity(newEntity.name, owner1, verb);
			oldValue1 = correctValue;
		} catch (IndexOutOfBoundsException ex) {
		}
		if (procedure.contains("change") || procedure.contains("compare") || procedure.contains("Eq")) {
			try {
				State verbStory = story.get(owner2).get(verb);
				//modularize
				for (TimeStamp currentTimeStamp : verbStory) {
					if (currentTimeStamp.entity.equals(newEntity.name)) {
						oldValue2 = currentTimeStamp.value;
					}
				}
				if (oldValue2.name.equals(Set.Empty.name))
					throw new NullPointerException();
			} catch (NullPointerException ex) {
				System.out.println("hi");
				Set correctValue = resolveNullEntity(newEntity.name, owner2, verb);
				oldValue2 = correctValue;
			} catch (IndexOutOfBoundsException ex) {
				System.out.println("hii");
				Set correctValue = resolveNullEntity(newEntity.name, owner2, verb);
				oldValue2 = correctValue;
			}
		}
		//System.err.println("aa"+oldValue1.name+"|"+oldValue2.name);		
		//System.err.println("aa"+timeStep);
		String[] steps = procedureMap.get(procedure).split("\\.");
		//System.out.println(procedure + "|" + procedureMap.get(procedure) + "|" + steps.length + owner1 + "|" + oldValue1 + "|" + owner2 + "|" + oldValue2);
		if (steps.length > NO_OWNERS_SUPPORTED) {
			//System.out.println("Invalid procedure description");
			System.exit(0);
		}
		for (int i = 0; i < steps.length; i++) {
			Set changeSet = new Set();
			String step = steps[i];
			step = step.replace(OWNER_1, oldValue1.name);
			step = step.replace(OWNER_2, oldValue2.name);
			step = step.replace(ENTITY, newSet.name);
			if (!procedure.contains("Eq")) {
				if (i == 0) {
					owner = owner1;
					if (!newName1.isEmpty() && !newName1.equals(changeSet.name))
						entity = newName1;
				} else {
					owner = owner2;
					if (!newName2.isEmpty() && !newName2.equals(changeSet.name))
						entity = newName2;
				}
				String split = "";
				if (step.contains("+")) {
					split = "\\+";
				}
				else
					split = "-";
				//System.out.println(step);
				Set A = sets.get(step.split(split)[0]);
				Set B = sets.get(step.split(split)[1]);
				//System.out.println(A.name);
				//System.out.println(B.name);
				changeSet = split.equals("\\+") ? Set.union(A, B) : Set.difference(A, B);
				updateTimestamp(owner, changeSet, tense, verb, entity);
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
			ArrayList<TimeStamp> verbStory = story.get(owner1).get(verbQual);
			Set value = new Set();
			for (TimeStamp currentTimeStamp : verbStory) {
				if (currentTimeStamp.value.name.equals(newEntity.name) && currentTimeStamp.time.equals(TIMESTAMP_PREFIX+timeStep+"")) {
					value = currentTimeStamp.value;
				}
			}
			////////////System.out.println(procedure + "|" + procedureMap.get(procedure) + "|" + steps.length + owner1 + "|" + oldValue1 + "|" + owner2 + "|" + oldValue2);
			//if (steps.length > NO_OWNERS_SUPPORTED) {
				////////////System.out.println("Invalid procedure description");
				//////System.exit(0);
			//}
			for (int i = 0; i < steps.length; i++) {
				Set changeSet = new Set();
				Entity modifiedEntity = new Entity();
				modifiedEntity.name = newEntity.name;
				String step = steps[i];
				step = step.replace(OWNER_1, oldValue1.name);
				step = step.replace(OWNER_2, oldValue2.name);
				step = step.replace(ENTITY, "("+value+")");
				modifiedEntity.value = step;
				if (!procedure.contains("EQ")) {
					if (i == 0) {
						owner = owner1;
						if (!newName1.isEmpty() && !newName1.equals(changeSet.name))
							entity = newName1;
					} else {
						owner = owner2;
						if (!newName2.isEmpty() && !newName2.equals(changeSet.name))
							entity = newName2;
					}
					String split = "";
					if (step.contains("+")) {
						split = "\\+";
					}
					else
						split = "-";
					Set A = sets.get(step.split(split)[0]);
					Set B = sets.get(step.split(split)[1]);
					changeSet = split.equals("\\+") ? Set.union(A, B) : Set.difference(A, B);
					updateTimestamp(owner, changeSet, tense, "has", entity);
				}
				else
					allEquations.add(step);
			}
			////////System.out.println("hello1");
			inertia();
		}
		displayStory();
	}
	
	private static Set resolveNullEntity(String name, String owner, String verb) {
		Set correctValue = Set.Empty;
		try {
			Situation currentSituation = story.get(owner);
			if (!currentSituation.containsKey(verb))
				verb = "has";
			State currentState = currentSituation.get(verb);
			System.out.println("waka"+owner);
			String time = TIMESTAMP_PREFIX + (timeStep-1);
			for (TimeStamp t : currentState) {
				System.out.println(t.entity);
				if ((t.entity.contains(name) || name.contains(t.entity))) {
					return t.value;
				}
			}
		} catch (Exception e) {
			System.out.println("error");
			return correctValue;
		}
		return correctValue;
	}

	private static void inertia() {
		if (timeStep == 0)
			return;
	}
	
	
	private static void displayStory() {
		System.out.println("----------------------------------------------------");
		Iterator<Entry<String,Situation>> storyIterator = story.entrySet().iterator();
		ArrayList<String> dispStory = new ArrayList<String>();
		for (int i = 0; i <= timeStep; i++)
			dispStory.add("");
		while (storyIterator.hasNext()) {
			 Entry<String, Situation> pair = storyIterator.next();
			 String owner = pair.getKey();
		     Situation currentSituation = pair.getValue();
		     Iterator<Entry<String,State>> verbIterator = currentSituation.entrySet().iterator();
			 while (verbIterator.hasNext()) {
				Entry<String, State> newPairs = verbIterator.next();
				String verb = newPairs.getKey();
				State verbStory = newPairs.getValue();
				for (TimeStamp currentTimeStamp : verbStory) {
					String ans = owner + " " + verb + " " + currentTimeStamp.value.name + " " + currentTimeStamp.entity;
					String oldStatus = "";
					int index = Integer.parseInt(currentTimeStamp.time.replace(TIMESTAMP_PREFIX, "")); 
					try {
						oldStatus = dispStory.get(index);
						ans = oldStatus + "\n" + ans;
					} catch (Exception e) {
						
					}
					dispStory.set(index, ans);
				}
			} 
			for (int counter = 0; counter <= timeStep; counter++) {
				for (String ans : dispStory) {
					if (dispStory.indexOf(ans) == counter && !ans.isEmpty()) {
						System.out.println(TIMESTAMP_PREFIX + counter);
						System.out.println(ans);
						System.out.println("-----------------------------------");
					}
				}
			}
		}
		for (Entry<String,Set> set : sets.entrySet()) {
			if (!set.getKey().equals(Set.Empty.name))
				System.out.println(set.getKey() + " " + set.getValue().cardinality);
		}
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
			//System.out.println("kr" + ls.owner1 + "|" + ls.owner2 + "|" + 
				//currentEntity.name + "|" + currentEntity.value + "|" + 
				//	ls.keyword + "|" + ls.procedureName + "|" + ls.tense +"|"+ls.verbQual);
			if (ls.isQuestion) {
				if (isQuestionAggregator)
					continue;
				//System.out.println("waka");
				questionOwner = questionOwner1 = ls.owner1;
				if (!ls.owner2.isEmpty())
					questionOwner2 = ls.owner2;
				questionEntity = ls.entityName;
				if (ls.tense.equals(PAST) && storyTense.contains(PRESENT))
					questionTime = 0;
				else
					questionTime = timeStep;
				isQuestionAggregator = ls.aggregator;
				isQuestionDifference = ls.difference;
				isQuestionComparator = ls.comparator;
				questionVerb = ls.verbQual;
				//System.out.println("krq" + ls.owner1 + "|" + ls.owner2 + "|" + 
				  // currentEntity.name + "|" + currentEntity.value + "|" + 
				  //  ls.keyword + "|" + ls.procedureName + "|" + ls.tense +"|"+ls.verbQual+ ls.aggregator);
				if (ls.entityValue == null)
					continue;
			}
			String nounQual = ls.nounQual, verbQual = ls.verbQual;
			if (verbQual == null)
				verbQual = "has";
			if (nounQual == null)
				nounQual = "";
			//System.err.println(ls.tense);
			reflectChanges(ls.owner1, ls.owner2, currentEntity, ls.keyword, 
					ls.procedureName, ls.tense, nounQual, verbQual);
		}
	}
	
	
	public static void solve() {
		State ansState = story.get(questionOwner).get(questionVerb);
		if (ansState == null) {
			Iterator<Entry<String, State>> it = story.get(questionOwner).entrySet().iterator();
			while (it.hasNext()) {
				questionVerb = it.next().getKey();
				if (!questionVerb.equals("has"))
					break;
			}
		}
		ansState = story.get(questionOwner).get(questionVerb);
		boolean isEvent = keywordMap.containsKey(questionVerb);
		String ans = "";
		for (TimeStamp t : ansState) {
			if (t.entity.equals(questionEntity)) {
				if (!isEvent) {
					if (t.time.equals(TIMESTAMP_PREFIX+questionTime))
						ans = sets.get(t.value.name).cardinality;
				}
				else
					ans = sets.get(t.value).cardinality;
			}		
		}
		if (ans.contains("x")) {
			Iterator<Entry<String, State>> it = story.get(questionOwner).entrySet().iterator();
			while (it.hasNext()) {
				questionVerb = it.next().getKey();
				if (!questionVerb.equals("has"))
					break;
			}
		}
		isEvent = keywordMap.containsKey(questionVerb);
		ans = "";
		ansState = story.get(questionOwner).get(questionVerb);
		for (TimeStamp t : ansState) {
			if (t.entity.equals(questionEntity)) {
				if (!isEvent) {
					if (t.time.equals(TIMESTAMP_PREFIX+questionTime))
						ans = sets.get(t.value.name).cardinality;
				}
				else
					ans = sets.get(t.value.name).cardinality;
			}		
		}
		
		finalAns = questionOwner + " " + questionVerb + " " + ans + " " + questionEntity;
	}
}
