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
	//private static final String DUMMY = "x";

	// NLP Parser constants
	private final String POS_VERB = "VB";
	private final String POS_VBD = "VBD";
	private final String POS_ADVMOD = "RBR";
	private final String POS_MOD = "JJ";
	private final String POS_VBN = "VBN";
	private final String POS_NUMBER = "CD";
	private final String POS_NOUN = "NN";
	
	private final String PARSER_NUMBER = "num";
	private final String PARSER_MOD = "amod";
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
		keywordMap.put("plant", CHANGE_OUT);
		keywordMap.put("place", CHANGE_OUT);
		keywordMap.put("distribute", CHANGE_OUT);
		keywordMap.put("sell", CHANGE_OUT);
		keywordMap.put("give", CHANGE_OUT);
		keywordMap.put("add", CHANGE_OUT);
		keywordMap.put("more than", COMPARE_PLUS);
		keywordMap.put("get", CHANGE_IN);
		keywordMap.put("carry", INCREASE);
		keywordMap.put("buy", CHANGE_IN);
		keywordMap.put("take", CHANGE_IN);
		keywordMap.put("cut", CHANGE_IN);
		keywordMap.put("pick", CHANGE_IN);
		keywordMap.put("borrow", CHANGE_IN);
		keywordMap.put("decrease", REDUCTION);
		keywordMap.put("leave", REDUCTION);
		keywordMap.put("spill", REDUCTION);
		keywordMap.put("lose", REDUCTION);
		keywordMap.put("spend", REDUCTION);
		keywordMap.put("eat", REDUCTION);
		keywordMap.put("break", REDUCTION);
		keywordMap.put("more", INCREASE);
		keywordMap.put("build", CHANGE_OUT);
		keywordMap.put("taller", INCREASE);
		keywordMap.put("load", CHANGE_OUT);
		keywordMap.put("increase", INCREASE);
		
		keywordMap.put("find", INCREASE);
		
		aggregators.add(" together");
		aggregators.add(" overall");
		aggregators.add(" total");
		aggregators.add(" either ");
		aggregators.add("in all");
		aggregators.add("In all");
		aggregators.add("combine");
		aggregators.add("sum");
		
		differences.add(" left ");
		differences.add(" remain ");
		differences.add(" over ");
		differences.add(" difference ");
		
		comparators.add(" more ");
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
	    	List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
	    	for (CoreLabel token: tokens) {
		    	String lemma = token.get(LemmaAnnotation.class);
		    	String pos = token.get(PartOfSpeechAnnotation.class);
		    	if (pos.contains(POS_VERB) || pos.contains(POS_ADVMOD) || pos.contains(POS_MOD)) {
		    		if (pos.contains(POS_VERB))
		    			verb = lemma;		  
		    		System.err.println("ervb"+verb+"|"+sentence.toString());
		    		if ((pos.contains(POS_VBD) || pos.contains(POS_VBN)) && tense.isEmpty())
			    		tense = PAST;
		    		else if (tense.isEmpty() && pos.contains(POS_VERB))
		    			tense = PRESENT;
		    		////System.out.println(sentence.toString() + "|" + (sentence.toString().contains("to")));
		    		if (keywordMap.containsKey(lemma)) {
		    			if (!lemma.equals("more") && lemma.equals(verb) && !lemma.equals("take")) 
		    				keyword = lemma;
		    			else if (lemma.equals("take") && !sentence.toString().contains(" to "))
		    				keyword = lemma;
		    			else if (sentence.toString().contains("more than")) 
		    				keyword = "more than";
		    			else if (keyword.isEmpty() && !lemma.equals("take"))
		    				keyword = "more";
		    		}	
		    	}
			}
	    	////////////System.err.println(sentence.toString()+tense);
	    	SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
	    	//System.out.println(dependencies);
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
    	
    	
    	//////System.out.println(edges);
		for (SemanticGraphEdge edge : edges) {
    		String pos = edge.getTarget().tag();
    		String relation = edge.getRelation().toString();
			if (pos.equals(POS_NUMBER) && (relation.contains(PARSER_NUMBER) || relation.equals(PARSER_ADVERBCLAUSE))) {
    			if (!edge.getSource().lemma().matches("[a-zA-Z]+"))
    				continue;
    			newEntity = new Entity();
    			newEntity.name = edge.getSource().lemma();
    			entities.add(newEntity.name);
    			////System.out.println("waka"+entities);
    			IndexedWord intermediateNode = edge.getSource();
    			IndexedWord nnNode = null, jjNode = null;
    			for (SemanticGraphEdge innerEdge : edges) {
    				String innerRelation = innerEdge.getRelation().toString();
    				String innerPos = innerEdge.getTarget().tag();
    				if (innerEdge.getSource().equals(intermediateNode)) {
    					if (innerRelation.contains(PARSER_MOD)&& (innerPos.contains(POS_NOUN) || innerPos.equals(POS_MOD)) || innerRelation.contains(PARSER_NN) && (innerPos.contains(POS_NOUN) || innerPos.equals(POS_MOD))) {
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
        		////System.err.println("waka"+entities);
    			entities.add(newEntity.name);
    			sentenceEntities.add(newEntity);
    		}
			if (edge.getTarget().toString().contains("NN")) {
    			relation = edge.getRelation().toString(); 
    			if (relation.equals(PARSER_SUBJECT)) {
    				if (owner1.isEmpty()) {
    					owner1 = edge.getTarget().lemma();
    					if (!entities.contains(owner1)) {
    						IndexedWord intermediateNode = edge.getTarget();
    						IndexedWord nnNode = null, jjNode = null;
    						for (SemanticGraphEdge innerEdge : edges) {
    							String innerRelation = innerEdge.getRelation().toString();
    							String innerPos = innerEdge.getTarget().tag();
    							if (innerEdge.getSource().equals(intermediateNode)) {
    								System.out.println("aaa"+innerEdge.getTarget().originalText()+"|"+innerEdge.getSource().originalText()+"|"+intermediateNode.originalText());;
    								if (innerRelation.contains(PARSER_MOD)&& (innerPos.contains(POS_NOUN) || innerPos.equals(POS_MOD)) || innerRelation.contains("poss") && (innerPos.contains(POS_NOUN) || innerPos.equals(POS_MOD)) || innerRelation.contains(PARSER_NN) && (innerPos.contains(POS_NOUN) || innerPos.equals(POS_MOD))) {
    									if (!innerEdge.getTarget().originalText().equals("more")) {
    										if (innerRelation.contains(PARSER_NN))
    											nnNode = innerEdge.getTarget();
    										else
    											jjNode = innerEdge.getTarget();
    									}
    								}
    							}
    						}
    						System.out.println(nnNode);
    						if (nnNode != null) {
    	    					owners.add(owner1);
    							owner1 = nnNode.originalText().toLowerCase() + "_" + owner1;
    						}
    						else if (jjNode != null) {
    							owners.add(owner1);
    							owner1 = jjNode.originalText().toLowerCase() + "_" + owner1;
    						}
    						System.out.println(owner1);
    					}
    				}
    				else {
    					owner2 = edge.getTarget().lemma();
    					if (!entities.contains(owner2)) {
    						IndexedWord intermediateNode = edge.getTarget();
    						IndexedWord nnNode = null, jjNode = null;
    						for (SemanticGraphEdge innerEdge : edges) {
    							String innerRelation = innerEdge.getRelation().toString();
    							String innerPos = innerEdge.getTarget().tag();
    							if (innerEdge.getSource().equals(intermediateNode)) {
    								System.out.println("aaa"+innerEdge.getTarget().originalText()+"|"+innerEdge.getSource().originalText()+"|"+intermediateNode.originalText());;
    								if (innerRelation.contains(PARSER_MOD) && (innerPos.contains(POS_NOUN) || innerPos.equals(POS_MOD)) ||  innerRelation.contains("poss") && (innerPos.contains(POS_NOUN) || innerPos.equals(POS_MOD)) || innerRelation.contains(PARSER_NN) && (innerPos.contains(POS_NOUN) || innerPos.equals(POS_MOD))) {
    									if (!innerEdge.getTarget().originalText().equals("more")) {
    										if (innerRelation.contains(PARSER_NN))
    											nnNode = innerEdge.getTarget();
    										else
    											jjNode = innerEdge.getTarget();
    									}
    								}
    							}
    						}
    						System.out.println(nnNode);
    						if (nnNode != null) {
    	    					owners.add(owner2);
    							owner2 = nnNode.originalText().toLowerCase() + "_" + owner2;
    						}
    						else if (jjNode != null) {
    							owners.add(owner2);
    							owner2 = jjNode.originalText().toLowerCase() + "_" + owner2;
    						}System.out.println(owner2);
    					}	
    			    }
    			}
    			else if (((relation.contains(PARSER_PREP) && !relation.contains("of") && !relation.contains("for")) || relation.contains(PARSER_IOBJ) || relation.contains(PARSER_POBJ))) {
    				owner2 = edge.getTarget().lemma();
    				if (!entities.contains(owner2)) {
						IndexedWord intermediateNode = edge.getTarget();
						IndexedWord nnNode = null, jjNode = null;
						for (SemanticGraphEdge innerEdge : edges) {
							String innerRelation = innerEdge.getRelation().toString();
							String innerPos = innerEdge.getTarget().tag();
							if (innerEdge.getSource().equals(intermediateNode)) {
								System.out.println("aaa"+innerEdge.getTarget().originalText()+"|"+innerEdge.getSource().originalText()+"|"+intermediateNode.originalText());;
								if (innerRelation.contains(PARSER_MOD) && (innerPos.contains(POS_NOUN) || innerPos.equals(POS_MOD)) ||innerRelation.contains("poss") && (innerPos.contains(POS_NOUN) || innerPos.equals(POS_MOD)) ||  innerRelation.contains(PARSER_NN) && (innerPos.contains(POS_NOUN) || innerPos.equals(POS_MOD))) {
									if (!innerEdge.getTarget().originalText().equals("more")) {
										if (innerRelation.contains(PARSER_NN))
											nnNode = innerEdge.getTarget();
										else
											jjNode = innerEdge.getTarget();
									}
								}
							}
						}
						System.out.println(nnNode);
						if (nnNode != null) {
	    					owners.add(owner2);
							owner2 = nnNode.originalText().toLowerCase() + "_" + owner2;
						}
						else if (jjNode != null) {
							owners.add(owner2);
							owner2 = jjNode.originalText().toLowerCase() + "_" + owner2;
						}System.out.println(owner1);
					}	
    			}
    		}
    		
    	}
		System.out.println("ssss"+owner1+"|"+owner2);
		if (keyword.isEmpty() || !keyword.isEmpty() && !keywordMap.get(keyword).contains("change") && !keywordMap.get(keyword).contains("compare") && !keywordMap.get(keyword).contains("Eq"))
			if (owner1.isEmpty() || !entities.contains(owner1))
				owner2 = "";
		if (sentence.toString().contains(" some ") || sentence.toString().contains(" several ") || sentence.toString().contains(" rest ") || sentence.toString().contains(" few ")) {
		    if(!entities.isEmpty()) {
			for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
		    	String lemma = token.get(LemmaAnnotation.class);
		    	if (entities.contains(lemma))
    				newEntity.name = lemma;
    		}
    		newEntity.value = "some";
    		sentenceEntities.add(newEntity);}
		}
		
		if (newEntity.value == null || sentence.toString().toLowerCase().contains("how ")) {
			System.out.println("waka"+owners);
			isQuestion = true;
    		String questionEntity = "", questionOwner1 = "", questionOwner2 = "",prevWord = "", prevLemma = "";
    		List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
    		for (CoreLabel token: tokens) {
    	    	String word = token.get(TextAnnotation.class);
    	    	String lemma = token.get(LemmaAnnotation.class);
    	    	//System.out.println(word+"|"+lemma+"|"+prevLemma+"|"+prevWord+"|"+prevLemma.toLowerCase()+"_"+lemma);
    	    	if (word.equals("'s"))
    	    		continue;
    	    	if (tokens.indexOf(token) != tokens.size()-1) {
    	    		if (tokens.get(tokens.indexOf(token)+1).originalText().equals("'s")) {
    	    			prevWord = word;
    	    			prevLemma = lemma;
    	    			continue;
    	    		}
    	    	}
    	    	if (entities.contains(word.toLowerCase()) && questionEntity.isEmpty()) { 
    	    		if (entities.contains(prevWord + "_" + word))
    	    			questionEntity = prevWord.toLowerCase() + "_" + word.toLowerCase();
    	    		else
    	    			questionEntity = word.toLowerCase();
    	    	}
    	    	if (entities.contains(lemma.toLowerCase()) && questionEntity.isEmpty()) { 
    	    		if (entities.contains(prevLemma + "_" + lemma))
    	    			questionEntity = prevLemma + "_" + lemma;
    	    		else
    	    			questionEntity = lemma;
    	    	}
    	    	if (owners.contains(word) && questionOwner1.isEmpty()) { 
    	    		if (owners.contains(prevWord.toLowerCase() + "_" + word))
    	    			questionOwner1 = prevWord.toLowerCase() + "_" + word;
    	    		else
    	    			questionOwner1 = word;
    	    	}
    	    	if (owners.contains(lemma) && questionOwner1.isEmpty()) { 
    	    		if (owners.contains(prevLemma.toLowerCase() + "_" + lemma))
    	    			questionOwner1 = prevLemma.toLowerCase() + "_" + lemma;
    	    		else
    	    			questionOwner1 = lemma;
    	    	}
    	    	if (owners.contains(word) && questionOwner2.isEmpty()) { 
    	    		if (owners.contains(prevWord.toLowerCase() + "_" + word))
    	    			questionOwner2 = prevWord.toLowerCase() + "_" + word;
    	    		else
    	    			questionOwner2 = word;
    	    		if (questionOwner2.equals(questionOwner1))
    	    			questionOwner2 = "";
    	    	}
    	    	if (owners.contains(lemma) && questionOwner2.isEmpty()) { 
    	    		if (owners.contains(prevLemma.toLowerCase() + "_" + lemma))
    	    			questionOwner2 = prevLemma.toLowerCase() + "_" + lemma;
    	    		else
    	    			questionOwner2 = lemma;
    	    		if (questionOwner2.equals(questionOwner1))
    	    			questionOwner2 = "";
    	    	}
    	    	prevWord = word;
    	    	prevLemma = lemma;
    		}
    		//////////System.out.println("a"+questionEntity);
    		System.out.println("q"+"|"+questionOwner1+"|"+questionOwner2+"|"+questionEntity+"|"+entities);
    		//if (questionOwner.equals(DUMMY))
    			//questionOwner = "";
    		
    		LinguisticStep s = new LinguisticStep();
			s.owner1 = questionOwner1;
			s.owner2 = questionOwner2;
			s.isQuestion = isQuestion;
			s.tense = tense;
			////System.out.println(s.tense+"|"+verb);
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
			//////////////System.out.println("q" + owner1 + "|" + owner2);
			steps.add(s);
    	}
		else {
			//////////////System.out.println("b"+entities+owner1+owner2);
			for (Entity e : sentenceEntities) {
				Entity tempEntity = new Entity();
				tempEntity.value = e.value;
				tempEntity.name = e.name;
				////////////System.out.println(owner1 + "|" + owner2 + "|" + keyword + "|" + tense + "|" + tempEntity.name + "|" + tempEntity.value);
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
				//System.err.println(owner1 + "|" + owner2 + "|" + keyword + "|" + tense + "|" + tempEntity.name + "|" + tempEntity.value);
				LinguisticStep s = new LinguisticStep();
				s.owner1 = owner1;
				if (entities.contains(owner2)) {
					owners.remove(owner2);
					for (String owner : owners) {
						if (sentence.toString().contains(owner) && !owner.equals(owner1))
							owner2 = owner;
					}
				}
				s.owner2 = owner2;
				System.out.println(owners);
				if (!entities.contains(owner2) && !owner2.trim().isEmpty())
					owners.add(owner2);
				if (!entities.contains(owner1) && !owner1.trim().isEmpty())
					owners.add(owner1);
				entities.add(tempEntity.name);
				s.isQuestion = isQuestion;
				s.tense = tense;
				if (verb.equals("be") || verb.equals("have") || verb.equals("do"))
					verb = "has";
				////System.out.println(s.tense+"|"+verb);
				
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
				if (s.aggregator && !owner1.isEmpty() && !owner2.isEmpty())
					s.procedureName = "altogetherEq";
				steps.add(s);
			}
		}
		return steps;
	}
}
