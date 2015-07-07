package nlp_adder;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.util.CoreMap;

class Entity {
	String name;
	String value;
}
public class SentencesAnalyzer {
	
	static HashMap<String,String> keywordMap = new HashMap<String,String>();
	private static final String CHANGE_OUT = "changeOut";
	private static final String CHANGE_IN = "changeIn";
	private static final String COMPARE_PLUS = "comparePlus";
	private static final String REDUCTION = "reduction";
	private static final String INCREASE = "increase";
	private static final String DUMMY = "x";

	// NLP Parser constants
	private final String POS_VERB = "VB";
	private final String POS_VBD = "VBD";
	private final String POS_ADVMOD = "RBR";
	private final String POS_MOD = "JJ";
	private final String POS_VBN = "VBN";
	private final String POS_NUMBER = "CD";
	private final String POS_NOUN = "NN";
	
	private final String PARSER_NUMBER = "num";
	private final String PARSER_MOD = "mod";
	private final String PARSER_ADVERBCLAUSE = "advcl";
	private final String PARSER_NN = "nn";
	private final String PARSER_SUBJECT = "nsubj";
	private final String PARSER_PREP = "prep";
	private final String PARSER_IOBJ = "iobj";
	private final String PARSER_POBJ = "pobj";
	
	
	
	
	private final String PRESENT = "present";
	private final String PAST = "past";

	private LinkedHashSet<String> entities = new LinkedHashSet<String>();
	private LinkedHashSet<String> owners = new LinkedHashSet<String>();
	private ArrayList<String> aggregators = new ArrayList<String>();
	private ArrayList<String> differences = new ArrayList<String>();
	private ArrayList<String> comparators = new ArrayList<String>();
	
	public SentencesAnalyzer() {
		keywordMap.put("put", CHANGE_OUT);
		keywordMap.put("place", CHANGE_OUT);
		keywordMap.put("sell", CHANGE_OUT);
		keywordMap.put("give", CHANGE_OUT);
		keywordMap.put("more than", COMPARE_PLUS);
		keywordMap.put("get", CHANGE_IN);
		keywordMap.put("carry", INCREASE);
		keywordMap.put("buy", CHANGE_IN);
		keywordMap.put("take", CHANGE_IN);
		keywordMap.put("pick", CHANGE_IN);
		keywordMap.put("borrow", CHANGE_IN);
		keywordMap.put("decrease", REDUCTION);
		keywordMap.put("lose", REDUCTION);
		keywordMap.put("spend", REDUCTION);
		keywordMap.put("eat", REDUCTION);
		keywordMap.put("more", INCREASE);
		keywordMap.put("find", INCREASE);
		
		aggregators.add("together");
		aggregators.add("overall");
		aggregators.add("total");
		aggregators.add("in all");
		aggregators.add("In all");
		aggregators.add("combine");
		aggregators.add("sum");
		
		differences.add("left");
		differences.add("remaining");
		differences.add("over");
		differences.add("difference");
		
		comparators.add("more");
		comparators.add("longer");
		comparators.add("larger");
		comparators.add("further");
		comparators.add("farther");
		comparators.add("taller");
		comparators.add("bigger");
	}
	
	public LinguisticInfo extract(String simplifiedProblem, StanfordCoreNLP pipeline) {
		ArrayList<LinguisticStep> preprocessedSteps = new ArrayList<LinguisticStep>();
		Annotation document = new Annotation(simplifiedProblem);
	    pipeline.annotate(document);
	    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	    for (CoreMap sentence : sentences) {
	    	String tense = "", keyword = "", verb = "";
	    	for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
		    	String lemma = token.get(LemmaAnnotation.class);
		    	String pos = token.get(PartOfSpeechAnnotation.class);
		    	if (pos.contains(POS_VERB) || pos.contains(POS_ADVMOD) || pos.contains(POS_MOD)) {
		    		if (pos.contains(POS_VERB))
		    			verb = lemma;
		    		
		    		if ((pos.contains(POS_VBD) || pos.contains(POS_VBN)) && tense.isEmpty())
			    		tense = PAST;
		    		else if (tense.isEmpty() && pos.contains(POS_VERB))
		    			tense = PRESENT;
		    		////////System.out.println(pos+"|"+tense);
		    		if (keywordMap.containsKey(lemma)) {
		    			if (!lemma.equals("more") && lemma.equals(verb)) 
		    				keyword = lemma;
		    			else if (sentence.toString().contains("more than")) 
		    				keyword = "more than";
		    			else if (keyword.isEmpty())
		    				keyword = "more";
		    		}	
		    	}
			}
	    	////////System.err.println(sentence.toString()+tense);
	    	SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
	    	System.out.println(dependencies);
	    	ArrayList<SemanticGraphEdge> edges = (ArrayList<SemanticGraphEdge>) dependencies.edgeListSorted();
	    	preprocessedSteps.addAll(processDependencies(sentence, edges, tense, keyword, verb));
	    }
	    LinguisticInfo problemInfo = new LinguisticInfo();
	    problemInfo.entities = entities;
	    problemInfo.owners = owners;
	    problemInfo.sentences = preprocessedSteps;
		return problemInfo;
	}
	// will be replaced with learner later
	
	private ArrayList<LinguisticStep> processDependencies(CoreMap sentence,
			ArrayList<SemanticGraphEdge> edges, String tense, String keyword, String verb) {
		ArrayList<LinguisticStep> steps = new ArrayList<LinguisticStep>();
    	ArrayList<Entity> sentenceEntities = new ArrayList<Entity>();
    	String owner1 = "", owner2 = "";
    	boolean isQuestion = false;
    	Entity newEntity = new Entity();
    	
    	
    	//System.out.println(edges);
		for (SemanticGraphEdge edge : edges) {
    		String pos = edge.getTarget().tag();
    		String relation = edge.getRelation().toString();
			if (pos.equals(POS_NUMBER) && (relation.equals(PARSER_NUMBER) || relation.equals(PARSER_ADVERBCLAUSE))) {
    			if (!edge.getSource().lemma().matches("[a-zA-Z]+"))
    				continue;
    			newEntity = new Entity();
    			newEntity.name = edge.getSource().lemma();
    			entities.add(newEntity.name);
    			IndexedWord intermediateNode = edge.getSource();
    			IndexedWord nnNode = null, jjNode = null;
    			for (SemanticGraphEdge innerEdge : edges) {
    				String innerRelation = innerEdge.getRelation().toString();
    				String innerPos = innerEdge.getTarget().tag();
    				if (innerEdge.getSource().equals(intermediateNode)) {
    					if (innerRelation.contains(PARSER_MOD) || innerRelation.contains(PARSER_NN) && (innerPos.contains(POS_NOUN) || innerPos.equals(POS_MOD))) {
    						if (!innerEdge.getTarget().originalText().equals("more")) {
    							if (innerRelation.contains(PARSER_NN))
    								nnNode = innerEdge.getTarget();
    							else
    								jjNode = innerEdge.getTarget();
    						}
    					}
    				}
    					
    			}
    			if (nnNode != null)
    					newEntity.name = nnNode.originalText().toLowerCase() + "_" + newEntity.name;
    			else if (jjNode != null)
    				newEntity.name = jjNode.originalText().toLowerCase() + "_" + newEntity.name;
        		newEntity.value = edge.getTarget().originalText();
        		String prevWord = "", prevLemma = "";
        		for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
        	    	String word = token.get(TextAnnotation.class);
        	    	String lemma = token.get(LemmaAnnotation.class);
        	    	if (entities.contains(word.toLowerCase())) { 
        	    		if (entities.contains(prevWord + "_" + word) && !newEntity.name.contains("_"))
        	    			newEntity.name = prevWord.toLowerCase() + "_" + word.toLowerCase();
        	    	}
        	    	if (entities.contains(lemma.toLowerCase())) { 
        	    		if (entities.contains(prevLemma + "_" + lemma) && !newEntity.name.contains("_"))
        	    			newEntity.name = prevLemma + "_" + lemma;
        	    	}
        	    	prevWord = word;
        	    	prevLemma = lemma;
        		}
        		//System.out.println(sentence.toString()+"|"+newEntity.name);
    			entities.add(newEntity.name);
    			sentenceEntities.add(newEntity);
    		}
    		if (edge.getTarget().toString().contains("NN")) {
    			relation = edge.getRelation().toString(); 
    			if (relation.equals(PARSER_SUBJECT)) {
    				if (owner1.isEmpty())
    					owner1 = edge.getTarget().lemma();
    				else
    					owner2 = edge.getTarget().lemma();
    			}
    			else if (relation.contains(PARSER_PREP) || relation.contains(PARSER_IOBJ) || relation.contains(PARSER_POBJ))
    				owner2 = edge.getTarget().lemma();
    		}
    		
    	}
		if (sentence.toString().contains("some") || sentence.toString().contains("several") || sentence.toString().contains("few")) {
		    if(!entities.isEmpty()) {
			for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
		    	String lemma = token.get(LemmaAnnotation.class);
		    	if (entities.contains(lemma))
    				newEntity.name = lemma;
    		}
    		newEntity.value = "some";
    		sentenceEntities.add(newEntity);}
		}
		
		if (newEntity.value == null) {
			//////System.out.println("waka"+entities);
			isQuestion = true;
    		String questionEntity = "", questionOwner = "", prevWord = "", prevLemma = "";
    		for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
    	    	String word = token.get(TextAnnotation.class);
    	    	String lemma = token.get(LemmaAnnotation.class);
    	    	//////System.out.println(word+"|"+lemma);
    	    	if (entities.contains(word.toLowerCase())) { 
    	    		if (entities.contains(prevWord + "_" + word))
    	    			questionEntity = prevWord.toLowerCase() + "_" + word.toLowerCase();
    	    		else
    	    			questionEntity = word.toLowerCase();
    	    	}
    	    	if (entities.contains(lemma.toLowerCase())) { 
    	    		if (entities.contains(prevLemma + "_" + lemma))
    	    			questionEntity = prevLemma + "_" + lemma;
    	    		else
    	    			questionEntity = lemma;
    	    	}
    	    	if (owners.contains(word)) { 
    	    		if (!questionOwner.isEmpty() && !questionOwner.equals(lemma))
    	    			questionOwner = DUMMY;
    	    		else
    	    			questionOwner = word;
    	    	}
    	    	if (owners.contains(lemma)) {
    	    		if (!questionOwner.isEmpty() && !questionOwner.equals(lemma))
    	    			questionOwner = DUMMY;
    	    		else
    	    			questionOwner = lemma;
    	    	}
    	    	prevWord = word;
    	    	prevLemma = lemma;
    		}
    		//////System.out.println("a"+questionEntity);
    		//////////////System.out.println("q"+questionOwner);
    		if (questionOwner.equals(DUMMY))
    			questionOwner = "";
    		
    		LinguisticStep s = new LinguisticStep();
			s.owner1 = questionOwner;
			s.owner2 = owner2;
			s.isQuestion = isQuestion;
			s.tense = tense;
			System.out.println(s.tense+"|"+verb);
			s.entityName = questionEntity;
			s.entityValue = newEntity.value;
			if (verb.equals("be") || verb.equals("have") || verb.equals("do"))
				verb = "has";
			s.verbQual = verb;
			s.procedureName = keywordMap.get(keyword);
			s.keyword = keyword;
			s.aggregator = false;
			for (String aggregator : aggregators) {
				if (sentence.toString().contains(aggregator))
					s.aggregator = true;	
			}
			s.comparator = false;
			for (String comparator : comparators) {
				if (sentence.toString().contains(comparator))
					s.comparator = true;	
			}
			s.difference = false;
			for (String difference : differences) {
				if (sentence.toString().contains(difference))
					s.difference = true;	
			}
			//////////System.out.println("q" + owner1 + "|" + owner2);
			steps.add(s);
    	}
		else {
			//////////System.out.println("b"+entities+owner1+owner2);
			for (Entity e : sentenceEntities) {
				Entity tempEntity = new Entity();
				tempEntity.value = e.value;
				tempEntity.name = e.name;
				////////System.out.println(owner1 + "|" + owner2 + "|" + keyword + "|" + tense + "|" + tempEntity.name + "|" + tempEntity.value);
				if ((entities.contains(owner1) || entities.contains(owner2)) && !e.name.isEmpty() && (!entities.contains(e.name) || !owners.contains(e.name))) {
					if (entities.contains(owner1) && !entities.contains(e.name)) {
						String entity = owner1;
						owner1 = tempEntity.name;
						tempEntity.name = entity;
					} else if (entities.contains(owner2) && !entities.contains(e.name)) {
						String entity = owner2;
						owner2 = tempEntity.name;
						tempEntity.name = entity;
					}
				}
				System.err.println(owner1 + "|" + owner2 + "|" + keyword + "|" + tense + "|" + tempEntity.name + "|" + tempEntity.value);
				LinguisticStep s = new LinguisticStep();
				s.owner1 = owner1;
				if (entities.contains(owner2)) {
					for (String owner : owners) {
						if (sentence.toString().contains(owner) && !owner.equals(owner1))
							owner2 = owner;
					}
				}
				s.owner2 = owner2;
				if (!entities.contains(owner2))
					owners.add(owner2);
				if (!entities.contains(owner1))
					owners.add(owner1);
				entities.add(tempEntity.name);
				s.isQuestion = isQuestion;
				s.tense = tense;
				if (verb.equals("be") || verb.equals("have") || verb.equals("do"))
					verb = "has";
				System.out.println(s.tense+"|"+verb);
				
				s.verbQual = verb;
				s.entityName = tempEntity.name;
				s.entityValue = tempEntity.value;
				s.keyword = keyword;
				s.procedureName = keywordMap.get(keyword);
				s.aggregator = false;
				for (String aggregator : aggregators) {
					if (sentence.toString().contains(aggregator))
						s.aggregator = true;	
				}
				
				steps.add(s);
			}
		}
		return steps;
	}
}
