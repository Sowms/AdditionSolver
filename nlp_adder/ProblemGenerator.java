package nlp_adder;

import java.util.ArrayList;
import java.util.HashMap;

import simplenlg.features.Feature;
import simplenlg.features.InterrogativeType;
import simplenlg.features.NumberAgreement;
import simplenlg.features.Tense;
import simplenlg.framework.NLGFactory;
import simplenlg.lexicon.Lexicon;
import simplenlg.phrasespec.NPPhraseSpec;
import simplenlg.phrasespec.PPPhraseSpec;
import simplenlg.phrasespec.SPhraseSpec;
import simplenlg.phrasespec.VPPhraseSpec;
import simplenlg.realiser.english.Realiser;

public class ProblemGenerator {
	static HashMap<String,String> procedureMap = new HashMap<String,String>();
	static HashMap<String,String> owner2Map = new HashMap<String,String>();
	static HashMap<String,String> objectMap = new HashMap<String,String>();
    static HashMap<String,String> keywordMap = new HashMap<String,String>();
    private static final String CHANGE_OUT = "changeOut";
    private static final String CHANGE_IN = "changeIn";
    private static final String COMPARE_PLUS = "comparePlus";
    private static final String COMPARE_MINUS = "compareMinus";
    private static final String INCREASE = "increase";
    private static final String REDUCTION = "reduction";
    private static final String ALTOGETHER_EQ = "altogetherEq";
    private static final String COMPARE_PLUS_EQ = "comparePlusEq";
    private static final String PLACE = "place";
    private static final String PERSON = "person";
    private static final String FOREST = "forest";
    private static final String CONTAINER = "container";
    private static final String COMPARE_MINUS_EQ = "compareMinusEq";
	private static final String LIQUID = "liquid";
	private static final String FOOD = "food";
	private static final String MONEY = "money";
	private static final String PLANT = "plant";
    
    public static void loadProcedureLookup() {
		keywordMap.put("put", CHANGE_OUT);
		keywordMap.put("place", CHANGE_OUT);
		keywordMap.put("plant", CHANGE_OUT);
		keywordMap.put("stack", CHANGE_OUT);
		keywordMap.put("add", CHANGE_OUT);
		keywordMap.put("sell", CHANGE_OUT);
		keywordMap.put("give", CHANGE_OUT);
		keywordMap.put("load", CHANGE_OUT);
		keywordMap.put("pour", CHANGE_OUT);
		keywordMap.put("more than", COMPARE_PLUS);
		keywordMap.put("less than", COMPARE_MINUS);
		keywordMap.put("get", CHANGE_IN);
		keywordMap.put("buy", CHANGE_IN);
		keywordMap.put("pick", CHANGE_IN);
		keywordMap.put("take", CHANGE_IN);
		keywordMap.put("receive", CHANGE_IN);
		keywordMap.put("borrow", CHANGE_IN);
		keywordMap.put("lose", REDUCTION);
		keywordMap.put("use", REDUCTION);
		keywordMap.put("leave", REDUCTION);
		keywordMap.put("transfer", REDUCTION);
		keywordMap.put("spill", REDUCTION);
		keywordMap.put("leak", REDUCTION);
		keywordMap.put("produce", INCREASE);
		keywordMap.put("remove", REDUCTION);
		keywordMap.put("spend", REDUCTION);
		keywordMap.put("eat", REDUCTION);
		keywordMap.put("increase", INCREASE);
		keywordMap.put("carry", INCREASE);
		keywordMap.put("saw", REDUCTION);
		//keywordMap.put("taller", INCREASE);
		//keywordMap.put("find", INCREASE);
		keywordMap.put("decrease", REDUCTION);
		//keywordMap.put("break", REDUCTION);
		keywordMap.put("finish", REDUCTION);
		
		owner2Map.put("put", PLACE);
		owner2Map.put("place", PLACE);
		owner2Map.put("plant", FOREST);
		owner2Map.put("stack", PLACE);
		owner2Map.put("add", PLACE);
		owner2Map.put("sell", PERSON);
		owner2Map.put("give", PERSON);
		owner2Map.put("load", PLACE);
		owner2Map.put("pour", PLACE);
		owner2Map.put("more than", PERSON);
		owner2Map.put("less than", PERSON);
		owner2Map.put("get", PERSON);
		owner2Map.put("buy", PERSON);
		owner2Map.put("pick", PLACE);
		owner2Map.put("take", PERSON);
		owner2Map.put("receive", PERSON);
		owner2Map.put("borrow", PERSON);
		
		objectMap.put("spill", LIQUID);
		objectMap.put("pour", LIQUID);
		objectMap.put("eat", FOOD);
		objectMap.put("plant", PLANT);
		objectMap.put("spend", MONEY);
		
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
    public static String getPerson() {
    	String[] persons = {"John", "Mary", "Katherine", "Hermione"};
    	int randIndex = (int) Math.floor(Math.random()*3);
    	return persons[randIndex];
    }
    public static String getPlace() {
    	String[] persons = {"a basket", "a bowl", "a plate", "a cup"};
    	int randIndex = (int) Math.floor(Math.random()*3);
    	return persons[randIndex];
    }
    public static String getLiquid() {
    	String[] liquids = {"oil", "water", "milk", "honey"};
    	int randIndex = (int) Math.floor(Math.random()*3);
    	return liquids[randIndex];
    }
    public static String getPlants() {
    	String[] plants = {"tree", "bush", "rose", "flower"};
    	int randIndex = (int) Math.floor(Math.random()*3);
    	return plants[randIndex];
    }
    public static String getForest() {
    	String[] greens = {"a forest", "a park", "a garden", "the backyard"};
    	int randIndex = (int) Math.floor(Math.random()*3);
    	return greens[randIndex];
    }
    public static String getEntity() {
    	String[] entities = {"orange", "chocolate", "banana", "apple"};
    	int randIndex = (int) Math.floor(Math.random()*3);
    	return entities[randIndex];
    }
    public static String getMoney() {
    	String[] entities = {"dollar", "euro", "rupee", "yen"};
    	int randIndex = (int) Math.floor(Math.random()*3);
    	return entities[randIndex];
    }
	public static String keywordSame(String problem, Attributes attributes) {
		loadProcedureLookup();
		String newProblem = "";
		String owner1, owner2 = "", keyword, entity;
		int value1 = 0, value2 = 0 ;
		owner1 = getPerson();
		keyword = attributes.keywords.get(0);
		
                String procedure = procedureMap.get(attributes.schemas.get(0));
                boolean owner2Flag = procedure.equals(CHANGE_IN) || procedure.equals(CHANGE_OUT) || procedure.equals(COMPARE_PLUS) || procedure.equals(COMPARE_MINUS);
                if (owner2Flag) {
                    switch (owner2Map.get(keyword)) {
			case PLACE : owner2 = getPlace(); break;
			case PERSON : owner2 = getPerson(); break;
			case FOREST : owner2 = getForest(); break;
                    }
                }
                if (attributes.numLength <= 1.0) {
			value1 = (int)Math.floor(Math.random()*8) + 2;
			value2 = (int)Math.floor(Math.random()*8) + 2;
		} else if (attributes.numLength <= 2.0) {
			value1 = (int)Math.floor(Math.random()*98) + 2;
			value2 = (int)Math.floor(Math.random()*98) + 2;
		}
		if (procedure.split("\\.")[0].contains("-")) {
			while (value1 <= value2) {
				if (attributes.numLength <= 1.0) {
					value1 = (int)Math.floor(Math.random()*8) + 2;
					value2 = (int)Math.floor(Math.random()*8) + 2;
				} else if (attributes.numLength <= 2.0) {
					value1 = (int)Math.floor(Math.random()*98) + 2;
					value2 = (int)Math.floor(Math.random()*98) + 2;
				}
			}
		}
		while (owner1.equals(owner2)) {
			owner1 = getPerson();
			owner2 = getPerson();
		}
		
		entity = getEntity();
		
		if (objectMap.containsKey(keyword)) {
			switch (objectMap.get(keyword)) {
				case MONEY : entity = getMoney(); break;
				case LIQUID : entity = getLiquid(); break;
				case PLANT : entity = getPlants(); break;
			}
		}
		
		Lexicon lexicon = Lexicon.getDefaultLexicon();
        NLGFactory nlgFactory = new NLGFactory(lexicon);
        Realiser realiser = new Realiser(lexicon);
        SPhraseSpec p = nlgFactory.createClause();
        p.setSubject(owner1);
        p.setFeature(Feature.TENSE, Tense.PAST);
        p.setVerb("has");
        NPPhraseSpec object = nlgFactory.createNounPhrase(entity);
		object.setFeature(Feature.NUMBER, NumberAgreement.PLURAL);
		object.addPreModifier(value1+"");
		p.setObject(object);
		newProblem = newProblem + realiser.realiseSentence(p);
		p = nlgFactory.createClause();
        p.setSubject(owner1);
        p.setVerb(keyword);
        p.setFeature(Feature.TENSE, Tense.PAST);
        object = nlgFactory.createNounPhrase(entity);
	object.setFeature(Feature.NUMBER, NumberAgreement.PLURAL);
	object.addPreModifier(value2+"");
	p.setObject(object);
        PPPhraseSpec complement = null;
        if (owner2Flag) {
            if (attributes.schemas.get(0).equals("CHANGE_IN"))
		complement = nlgFactory.createPrepositionPhrase("from");
            else if (attributes.schemas.get(0).equals("CHANGE_OUT") && (owner2Map.get(keyword).equals(PLACE) || owner2Map.get(keyword).equals(FOREST)))
		complement = nlgFactory.createPrepositionPhrase("in");
            else if (attributes.schemas.get(0).equals("CHANGE_OUT") )
		complement = nlgFactory.createPrepositionPhrase("to");
            else
		complement = nlgFactory.createPrepositionPhrase("in");
            complement.setComplement(owner2);
            p.setComplement(complement);
        }
	newProblem = newProblem + " " + realiser.realiseSentence(p);
	p = nlgFactory.createClause();
	p.setFeature(Feature.INTERROGATIVE_TYPE,InterrogativeType.HOW_MANY);
        NPPhraseSpec subject = nlgFactory.createNounPhrase(entity);
    	subject.setFeature(Feature.NUMBER, NumberAgreement.PLURAL);
    	p.setSubject(subject);
    	p.setFeature(Feature.TENSE, Tense.PRESENT);
    	VPPhraseSpec verb = nlgFactory.createVerbPhrase("are");
    	p.setVerb(verb);
    	complement = nlgFactory.createPrepositionPhrase("with");
    	complement.setComplement(owner1);
    	p.setComplement(complement);
    	newProblem = newProblem + " " + realiser.realiseSentence(p);       
        //System.out.println(output2);
		return newProblem;
	}
	
	public static void main(String[] args) {
		String problem = "John had 7 apples. He ate 1 apple. How many apples does he have now?";
		Attributes a = ExtractAttributes.extract(problem);
		System.out.println("extraNo = " + a.extraNo);
        System.out.println("extraInfo = " + a.extraInfo);
        System.out.println("avgLength = " + a.avgLength);
        System.out.println("avgDigitLength = " + a.numLength);
        System.out.println("isDecimal = " + a.isDecimal);
        System.out.println(a.schemas);
        System.out.println(a.keywords);
        System.out.println("Original Problem\n-----------------\n"+problem);
        System.out.println("Similar Problems\n-----------------\n");
		System.out.println(keywordSame(problem, a));
		System.out.println(keywordSame(problem, a));
	}

}