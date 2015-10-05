package nlp_adder;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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
	private LinkedHashSet<String> verbs = new LinkedHashSet<String>();
	private ArrayList<String> aggregators = new ArrayList<String>();
	private ArrayList<String> differences = new ArrayList<String>();
	private ArrayList<String> comparators = new ArrayList<String>();
	
	public SentencesAnalyzer() {
		keywordMap.put("put", CHANGE_OUT);
		keywordMap.put("plant", CHANGE_OUT);
		keywordMap.put("place", CHANGE_OUT);
		keywordMap.put("distribute", CHANGE_OUT);
		keywordMap.put("stack", CHANGE_OUT);
		keywordMap.put("transfer", REDUCTION);
		keywordMap.put("serve", CHANGE_OUT);
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
		keywordMap.put("produce", INCREASE);
		keywordMap.put("decrease", REDUCTION);
		keywordMap.put("leave", REDUCTION);
		keywordMap.put("spill", REDUCTION);
		keywordMap.put("lose", REDUCTION);
		keywordMap.put("use", REDUCTION);
		keywordMap.put("spend", REDUCTION);
		keywordMap.put("saw", REDUCTION);
		keywordMap.put("eat", REDUCTION);
		keywordMap.put("break", REDUCTION);
		keywordMap.put("leak", REDUCTION);
		keywordMap.put("more", INCREASE);
		keywordMap.put("build", CHANGE_OUT);
		keywordMap.put("taller", INCREASE);
		keywordMap.put("load", CHANGE_OUT);
		keywordMap.put("increase", INCREASE);
		keywordMap.put("immigrate", INCREASE);
		//keywordMap.put("find", INCREASE);
		
		aggregators.add(" together");
		aggregators.add(" either ");
		aggregators.add("ltogether");
		aggregators.add(" overall");
		aggregators.add(" total");
		aggregators.add(" either ");
		aggregators.add("in all");
		aggregators.add("In all");
		aggregators.add("combine");
		aggregators.add("combined");
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
	public boolean isAntonym (String word1, String word2) {
		try {
			if (entities.contains("dollars"))
				return false;
			if (word1.equals(word2))
				return false;
			if (word1.equals("has") || word2.equals("has"))
				return false;
			Document doc = Jsoup.connect("http://www.thesaurus.com/browse/"+word1)
					  .userAgent("Mozilla")
					  .cookie("auth", "token")
					  .timeout(3000)
					  .post();
			Elements sections = doc.select("section");
			for (Element section : sections) {
				////System.out.println(section.attr("abs:class"));
				String className = section.attr("abs:class");
				if (className.contains("container-info antonyms")) {
					////System.out.println("in");
					Elements links = section.select("a");
					for (Element link : links) {
						////System.out.println(link.attr("abs:href"));
						String linkAddress = link.attr("abs:href");
						if (linkAddress.contains(word2)) {
							//System.out.println(word1+word2);
							return true;
						}
					}
					return false;
				}
			}
		} catch (IOException e) {
			//System.out.println("errrr");
			return false;
		}
		return false;
	}
	public boolean isAntonym (String word) {
		for (String verb : verbs) {
			if (isAntonym(verb,word)) {
				//System.out.println("anti"+verb+"|"+word);
				return true;
			}
		}
		return false;
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
		    		if (pos.contains(POS_VERB)) {
		    			System.out.println("haa"+pos+"|"+lemma);
		    			verb = lemma;		  
		    		}
		    		////System.err.println("ervb"+verb+"|"+sentence.toString());
		    		if ((pos.contains(POS_VBD) || pos.contains(POS_VBN)) && tense.isEmpty())
			    		tense = PAST;
		    		else if (tense.isEmpty() && pos.contains(POS_VERB))
		    			tense = PRESENT;
		    		////////System.out.println(sentence.toString() + "|" + (sentence.toString().contains("to")));
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
	    	//System.out.println(verb);
	    	////////////////System.err.println(sentence.toString()+tense);
	    	SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
	    	//////System.out.println(dependencies);
	    	ArrayList<SemanticGraphEdge> edges = (ArrayList<SemanticGraphEdge>) dependencies.edgeListSorted();
	    	preprocessedSteps.addAll(processDependencies(sentence, edges, tense, keyword, verb));
	    }
	    LinguisticInfo problemInfo = new LinguisticInfo();
	    problemInfo.entities = entities;
	    problemInfo.owners = owners;
	    problemInfo.sentences = preprocessedSteps;
		return problemInfo;
	}
	private ArrayList<LinguisticStep> processDependencies(CoreMap sentence,
			ArrayList<SemanticGraphEdge> edges, String tense, String keyword, String verb) {
		ArrayList<LinguisticStep> steps = new ArrayList<LinguisticStep>();
    	ArrayList<Entity> sentenceEntities = new ArrayList<Entity>();
    	String owner1 = "", owner2 = "";
    	boolean isQuestion = false;
    	Entity newEntity = new Entity();
    	
    	
    	//////////System.out.println(edges);
		for (SemanticGraphEdge edge : edges) {
    		String pos = edge.getTarget().tag();
    		String relation = edge.getRelation().toString();
			if (pos.equals(POS_NUMBER) && (relation.contains(PARSER_NUMBER) || relation.equals(PARSER_ADVERBCLAUSE))) {
    			if (!edge.getSource().lemma().matches("[a-zA-Z]+"))
    				continue;
    			newEntity = new Entity();
    			newEntity.name = edge.getSource().originalText();
    			entities.add(edge.getSource().lemma());
    			////////System.out.println("waka"+entities);
    			IndexedWord intermediateNode = edge.getSource();
    			IndexedWord nnNode = null, jjNode = null, secondNode = null;
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
    				secondNode = null;
    				String prep = "";
    				if (innerEdge.getSource().equals(intermediateNode)) {
    					if ((innerRelation.contains("prep_of") || innerRelation.startsWith("prep_with") || innerRelation.startsWith("prep_for")) && (innerPos.contains(POS_NOUN) || innerPos.equals(POS_MOD))) {
    						prep = innerRelation.replace("prep_", "");
    						if (!innerEdge.getTarget().originalText().equals("more")) {
    							secondNode = innerEdge.getTarget();
    						}
    					}
    					//System.out.println("second"+secondNode);
    					IndexedWord innNode = null, ijjNode = null;
    	    			for (SemanticGraphEdge iEdge : edges) {
    	    				String iRelation = iEdge.getRelation().toString();
    	    				String iPos = iEdge.getTarget().tag();
    	    				if (iEdge.getSource().equals(secondNode)) {
    	    					if (iRelation.contains(PARSER_MOD)&& (iPos.contains(POS_NOUN) || iPos.equals(POS_MOD)) || iRelation.contains(PARSER_NN) && (iPos.contains(POS_NOUN) || iPos.equals(POS_MOD))) {
    	    						if (!innerEdge.getTarget().originalText().equals("more")) {
    	    							if (innerRelation.contains(PARSER_NN))
    	    								innNode = innerEdge.getTarget();
    	    							else
    	    								ijjNode = innerEdge.getTarget();
    	    						}
    	    					}
    	    				}
    	    			}
    	    			if (innNode != null && !newEntity.name.equals(secondNode.originalText()))
        					newEntity.name = newEntity.name + " " + prep + " " + innNode.originalText().toLowerCase() + "_" + secondNode.originalText();
    	    			else if (ijjNode != null && !newEntity.name.equals(secondNode.originalText()))
    	    				newEntity.name = newEntity.name + " " + prep + " " + ijjNode.originalText().toLowerCase() + "_" + secondNode.originalText();
    	    			else if (secondNode !=null && !newEntity.name.equals(secondNode.originalText()))
    	    				newEntity.name = newEntity.name + " " + prep + " " + secondNode.originalText();
            		
    					
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
        		////////System.err.println("waka"+entities);
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
    								////System.out.println("aaa"+innerEdge.getTarget().originalText()+"|"+innerEdge.getSource().originalText()+"|"+intermediateNode.originalText());;
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
    						////System.out.println(nnNode);
    						if (nnNode != null) {
    	    					owners.add(owner1);
    							owner1 = nnNode.originalText().toLowerCase() + "_" + owner1;
    						}
    						else if (jjNode != null) {
    							owners.add(owner1);
    							owner1 = jjNode.originalText().toLowerCase() + "_" + owner1;
    						}
    						////System.out.println(owner1);
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
    								////System.out.println("aaa"+innerEdge.getTarget().originalText()+"|"+innerEdge.getSource().originalText()+"|"+intermediateNode.originalText());;
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
    						////System.out.println(nnNode);
    						if (nnNode != null) {
    	    					owners.add(owner2);
    							owner2 = nnNode.originalText().toLowerCase() + "_" + owner2;
    						}
    						else if (jjNode != null) {
    							owners.add(owner2);
    							owner2 = jjNode.originalText().toLowerCase() + "_" + owner2;
    						}
    						//System.out.println("aa"+owner2);
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
								////System.out.println("aaa"+innerEdge.getTarget().originalText()+"|"+innerEdge.getSource().originalText()+"|"+intermediateNode.originalText());;
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
						////System.out.println(nnNode);
						if (nnNode != null) {
	    					owners.add(owner2);
							owner2 = nnNode.originalText().toLowerCase() + "_" + owner2;
						}
						else if (jjNode != null) {
							owners.add(owner2);
							owner2 = jjNode.originalText().toLowerCase() + "_" + owner2;
						}////System.out.println(owner1);
					}	
    			}
    		}
    		
    	}
		System.out.println("ssss"+owner1+"|"+owner2+"|"+newEntity.value);
		if (keyword.isEmpty() || !keyword.isEmpty() && !keywordMap.get(keyword).contains("change") && !keywordMap.get(keyword).contains("compare") && !keywordMap.get(keyword).contains("Eq"))
			if (owner1.isEmpty() || !entities.contains(owner1))
				owner2 = "";
		boolean someFlag = false;
		System.err.println(owner1+owner2+newEntity.value);
		if (newEntity.value == null) {
		for (String name : entities) {
			if (sentence.toString().contains(name) && !sentence.toString().toLowerCase().contains("how") && !sentence.toString().contains(" a "+name) && !owner1.contains(name) && !name.contains(owner1))
				someFlag = true;
		}
		if (verb.equals("buy") || verb.equals("purchase"))
			if (entities.contains("dollar") || entities.contains("dollars"))
				someFlag = true;
		for (String name : Parser.entities) {
			//System.out.println(name);
			if (sentence.toString().contains(name) && !sentence.toString().toLowerCase().contains("how") && !sentence.toString().contains(" a "+name) && !owner1.contains(name) && !name.contains(owner1))
				someFlag = true;
		}}
		System.err.println(someFlag);
		if (sentence.toString().contains(" some ") || sentence.toString().contains(" several ") || sentence.toString().contains(" rest ") || sentence.toString().contains(" few ") || someFlag) {
		    if (newEntity.value == null) {
		    	ArrayList<CoreLabel> tokens = (ArrayList<CoreLabel>) sentence.get(TokensAnnotation.class);
		    	for (CoreLabel token: tokens) {
		    		String lemma = token.get(LemmaAnnotation.class);
		    		if (!entities.isEmpty() && entities.contains(lemma) || Parser.entities.contains(token.originalText())) {
		    			String temp = token.originalText();
		    			temp = tokens.get(tokens.indexOf(token)-1).originalText() + " " + temp;
		    			if (Parser.entities.contains(temp))
		    				newEntity.name = temp.replace(" ", "_");
		    			else
		    				newEntity.name = token.originalText();
		    		}
		    	}
    		newEntity.value = "some";
    		if (newEntity.name == null)
    			newEntity.name = "dollars";
    		sentenceEntities.add(newEntity);
    		}
		}
		//System.out.println(newEntity.value);
		if (newEntity.value == null || sentence.toString().toLowerCase().contains("how ")) {
			System.out.println("waka"+owners);
			isQuestion = true;
    		String questionEntity = "", questionOwner1 = "", questionOwner2 = "",prevWord = "", prevLemma = "";
    		
    		List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
    		//System.out.println(entities+"|"+owners);
    		for (CoreLabel token: tokens) {
    	    	String word = token.originalText().toLowerCase();
    	    	String lemma = token.get(LemmaAnnotation.class);
    	    	String pos = token.get(PartOfSpeechAnnotation.class);
    	    	////System.out.println(word+"|"+lemma+"|"+prevLemma+"|"+prevWord+"|"+prevLemma.toLowerCase()+"_"+lemma+"|"+prevWord.toLowerCase() + "_" + word.toLowerCase()+"|"+questionEntity+questionOwner1+questionOwner2);
    	    	if (word.equals("'s"))
    	    		continue;
    	    	if (tokens.indexOf(token) != tokens.size()-1) {
    	    		if (tokens.get(tokens.indexOf(token)+1).originalText().equals("'s")) {
    	    			prevWord = word;
    	    			prevLemma = lemma;
    	    			continue;
    	    		}
    	    	}
    	    	if (pos.equals(POS_ADVMOD) || pos.equals(POS_MOD)) {
    	    		prevWord = word;
        	    	prevLemma = lemma;
    	    		continue;
    	    	}
    	    	//System.out.println(pos+"|"+word);
    	    	if (entities.contains(word.toLowerCase()) && questionEntity.isEmpty()) { 
    	    		if (entities.contains(prevWord + "_" + word))
    	    			questionEntity = prevWord.toLowerCase() + "_" + word.toLowerCase();
    	    		else
    	    			questionEntity = word.toLowerCase();
    	    	}
    	    	if (entities.contains(lemma.toLowerCase()) && questionEntity.isEmpty()) { 
    	    		if (entities.contains(prevLemma + "_" + lemma))
    	    			questionEntity = prevLemma + "_" + lemma;
    	    		else if ((entities.contains(prevWord + "_" + word)))
    	    			questionEntity = prevWord + "_" + word;
    	    		else
    	    			questionEntity = lemma;
    	    	}
    	    	System.out.println(word+"|"+lemma+"|"+owners);
    	    	if (owners.contains(word) && questionOwner1.isEmpty()) { 
    	    		if (owners.contains(prevWord.toLowerCase() + "_" + word.toLowerCase()))
    	    			questionOwner1 = prevWord.toLowerCase() + "_" + word.toLowerCase();
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
    	    		if (owners.contains(prevWord.toLowerCase() + "_" + word.toLowerCase()))
    	    			questionOwner2 = prevWord.toLowerCase() + "_" + word.toLowerCase();
    	    		else
    	    			questionOwner2 = word;
    	    		//System.out.println(questionOwner2);
    	    		if (questionOwner2.equals(questionOwner1))
    	    			questionOwner2 = "";
    	    	}
    	    	if (owners.contains(lemma) && questionOwner2.isEmpty()) { 
    	    		if (owners.contains(prevLemma.toLowerCase() + "_" + lemma))
    	    			questionOwner2 = prevLemma.toLowerCase() + "_" + lemma;
    	    		else
    	    			questionOwner2 = lemma;
    	    		//System.out.println(questionOwner2);
    	    		if (questionOwner2.equals(questionOwner1))
    	    			questionOwner2 = "";
    	    	}
    	    	prevWord = word;
    	    	prevLemma = lemma;
    		}
    		//////////////System.out.println("a"+questionEntity);
    		//System.out.println("q"+"|"+sentence.toString()+"|"+questionOwner1+"|"+questionOwner2+"|"+questionEntity+"|"+entities);
    		//if (questionOwner.equals(DUMMY))
    			//questionOwner = "";
    		LinguisticStep s = new LinguisticStep();
			s.owner1 = questionOwner1;
			s.owner2 = questionOwner2;
			s.isQuestion = isQuestion;
			s.tense = tense;
			////////System.out.println(s.tense+"|"+verb);
			s.entityName = questionEntity;
			s.entityValue = newEntity.value;
			if (verb.equals("be") || verb.equals("have") || verb.equals("do"))
				verb = "has";
			s.verbQual = verb;
			verbs.add(verb);
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
			s.setCompletor = isAntonym(verb);
			////System.out.println("q" + owner1 + "|" + owner2 + s.setCompletor+isAntonym(verb)+"|"+verb);
			steps.add(s);
    	}
		else {
			//System.err.println("b"+entities+owner1+owner2);
			for (Entity e : sentenceEntities) {
				Entity tempEntity = new Entity();
				tempEntity.value = e.value;
				tempEntity.name = e.name;
				//System.out.println(owner1 + "|" + owner2 + "|" + keyword + "|" + tense + "|" + tempEntity.name + "|" + tempEntity.value);
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
				
				LinguisticStep s = new LinguisticStep();
				s.owner1 = owner1;
				if (entities.contains(owner2)) {
					owners.remove(owner2);
					for (String owner : owners) {
						if (sentence.toString().contains(owner) && !owner.equals(owner1))
							owner2 = owner;
					}
				}
				//System.err.println(owner1 + "|" + owner2);
				s.owner2 = owner2;
				////System.out.println(owners);
				if (!entities.contains(owner2) && !owner2.trim().isEmpty())
					owners.add(owner2);
				if (!entities.contains(owner1) && !owner1.trim().isEmpty())
					owners.add(owner1);
				entities.add(tempEntity.name);
				s.isQuestion = isQuestion;
				s.tense = tense;
				if (verb.equals("be") || verb.equals("have") || verb.equals("do"))
					verb = "has";
				System.out.println("ha"+s.tense+"|"+verb+"|"+keyword);
				verbs.add(verb);
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
				System.out.println("oo" + owner1 + owner2 + entities);
				if (!(entities.contains(owner1) && owner2.isEmpty() && !owner1.isEmpty()))
					steps.add(s);
				else if (entities.contains(owner1) && sentence.toString().toLowerCase().contains("there"))
					steps.add(s);
			}
		}
		return steps;
	}
}
