package nlp_adder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.TypesafeMap;

public class Parser {
	private final String POS_VERB = "VB";
	private final String POS_VBD = "VBD";
	private final String POS_ADVMOD = "RBR";
	private final static String POS_MOD = "JJ";
	private final String POS_VBN = "VBN";
	private final static String POS_NUMBER = "CD";
	private final static String POS_NOUN = "NN";
	
	private final static String PARSER_NUMBER = "num";
	private final static String PARSER_MOD = "mod";
	private final static String PARSER_ADVERBCLAUSE = "advcl";
	private final static String PARSER_NN = "nn";
	private final String PARSER_SUBJECT = "nsubj";
	private final String PARSER_PREP = "prep";
	private final String PARSER_IOBJ = "iobj";
	private final String PARSER_POBJ = "pobj";
	
	public static String dollarPreprocess(String input) {
		if (input.contains("$")) {
			String ans = "";
			//boolean dollarFlag = false;
			for (String word : input.split(" ")) {
				if (!word.contains("$"))
					ans = ans + word + " ";
				else {
					//////System.out.println("waka");
					//dollarFlag = true;
					
					//ans = ans + word.replace("$", "");
					Pattern wordPattern = Pattern.compile("\\d+\\.?\\d*");
					Matcher matcher = wordPattern.matcher(word); 
					if (matcher.find()) {
						String candidate = matcher.group();
						String remaining = word.replace("$", "");
						remaining = remaining.replace(candidate,"");
						ans = ans + candidate + " dollars " + remaining;
					}
				}
			}
			return ans;
		}
		return input;
	}
	public static boolean checkPossibilities(LinkedHashSet<String> possibleEntities, CoreLabel token1, CoreLabel token2, CoreLabel token3) {
		for (String possibility : possibleEntities) {
			////System.out.println(possibility+token1+token2+token3);
			////System.out.println("aa"+possibility+token1.originalText());
			if (possibility.split(" ")[0].equals(token1.originalText())) {
				String secondWord = "";
				if (possibility.split(" ").length > 1) 
					secondWord = possibility.split(" ")[1];
				////System.out.println("aa"+possibility+token1.originalText()+token2);
				if (token2==null && !secondWord.isEmpty())
					continue;
				if (token2!=null && !secondWord.isEmpty() && !secondWord.equals(token2.originalText().toLowerCase()))
					return false;
				return true;
			}
			if (possibility.split(" ").length > 1 && token2!=null && possibility.split(" ")[1].equals((token2.originalText()))) {
				String secondWord = "";
				if (possibility.split(" ").length > 1) 
					secondWord = possibility.split(" ")[1];
				if (token3==null && !secondWord.isEmpty())
					continue;
				if (token3!=null && !secondWord.isEmpty() && !secondWord.equals(token3.originalText().toLowerCase()))
					return false;
				return true;
			}
		}
		return false;
	}
	public static String entityResolution(String input, StanfordCoreNLP pipeline) {
		////System.out.println("kjkj"+input);
		Annotation document = new Annotation(input);
	    pipeline.annotate(document);
	    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	    LinkedHashSet<String> possibleEntities = new LinkedHashSet<String>();
	    for (CoreMap sentence: sentences) {
	    	SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
	     	System.out.println(dependencies);
	     	ArrayList<SemanticGraphEdge> edges = (ArrayList<SemanticGraphEdge>) dependencies.edgeListSorted();
	 	    for (SemanticGraphEdge edge : edges) {
	     		String pos = edge.getTarget().tag();
	     		String relation = edge.getRelation().toString();
	 			if (pos.equals(POS_NUMBER) && (relation.equals(PARSER_NUMBER) || relation.equals(PARSER_ADVERBCLAUSE))) {
	     			if (!edge.getSource().lemma().matches("[a-zA-Z]+"))
	     				continue;
	     			String name = "";
	     			name = edge.getSource().originalText();
	     			possibleEntities.add(name);
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
	     					name = nnNode.originalText() + " " + name;
	     			else if (jjNode != null)
	     				name = jjNode.originalText() + " " + name;
	         		possibleEntities.add(name);
	     		}
	 	    }
	 	    
	    }
	    String ans = "";
	    ////System.out.println(possibleEntities);
	    for (CoreMap sentence: sentences) {
	    	boolean entity = true;
	    	Tree tree = sentence.get(TreeAnnotation.class);
	    	String parseExpr = tree.toString();
	    	//System.out.println(parseExpr);
	    	List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
	     	for (CoreLabel token: tokens) {
		    	String pos = token.get(PartOfSpeechAnnotation.class);
		    	ans = ans + token.originalText() + " ";
		    	//try {
		    	
		    	CoreLabel token1 = null;
		    	CoreLabel token2 = null;
		    	CoreLabel token3 = null;
		    	if (!(tokens.size() <= tokens.indexOf(token)+1))
		    		token1 = tokens.get(tokens.indexOf(token)+1);
		    	if (!(tokens.size() <= tokens.indexOf(token)+2))
		    		token2 = tokens.get(tokens.indexOf(token)+2);
		    	if (!(tokens.size() <= tokens.indexOf(token)+3))
		    		token3 = tokens.get(tokens.indexOf(token)+3);
		    	
		    	if (!entity && (pos.contains("JJ") || pos.equals("NN")) && !checkPossibilities(possibleEntities,token1,token2,token3) && (tokens.get(tokens.indexOf(token)-1).get(PartOfSpeechAnnotation.class).contains("CD"))) {
		    		ans = ans +getEntity(possibleEntities,sentence.toString(),true) +" ";
		    	}
		    	if (pos.contains("CD") && !token.originalText().contains(".")) {
		    		token1 = tokens.get(tokens.indexOf(token)+1);
			    	token2 = null;
			    	token3 = null;
			    	if (!(tokens.size() <= tokens.indexOf(token)+2))
			    		token2 = tokens.get(tokens.indexOf(token)+2);
			    	if (!(tokens.size() <= tokens.indexOf(token)+3))
			    		token3 = tokens.get(tokens.indexOf(token)+3);
		    		entity = checkPossibilities(possibleEntities,token1,token2,token3);
		    		////System.out.println("haha"+entity);
		    		if (!entity) {
		    			CoreLabel nextToken = tokens.get(tokens.indexOf(token)+1);
		    			if(!nextToken.get(PartOfSpeechAnnotation.class).contains("JJ") && !nextToken.get(PartOfSpeechAnnotation.class).contains("NN"))
		    				ans = ans + getEntity(possibleEntities,sentence.toString(),false)+" ";
		    		}
		    		////System.err.println("waka"+ans);
		    	}
		    	//} catch (Exception e) {};
	     	}
	    }
	    //System.err.println(ans);
	    return ans;
	}
	private static String getEntity(LinkedHashSet<String> possibleEntities,
			String sentence, boolean adjFlag) {
		for (String entity : possibleEntities) {
			////System.out.println("aaaaaaaaaaaaaaaaaaaaaaaa"+entity+sentence);
			if (sentence.contains(entity) && entity.contains(" "))
				return entity;
		}
		for (String entity : possibleEntities) {
			if (sentence.contains(entity))
				return entity;
		}
		String ans1 = "", ans2 = "";
		Iterator<String> it =  possibleEntities.iterator();
		ans1 = it.next();
		////System.out.println("wwww"+ans1+possibleEntities + possibleEntities.size());
		if (adjFlag)
			return ans1;
		if (possibleEntities.size() > 1) {
			 ans2 = it.next();
			 ////System.out.println("ww"+ans2);
			 return ans2;
		}
		return ans1;
	}
	public static boolean containsVerb (String text, StanfordCoreNLP pipeline) {
		Annotation document = new Annotation(text);
	    pipeline.annotate(document);
	    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	    ////System.out.println("d"+sentences.size());
	    for (CoreMap sentence: sentences) {
	     	for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
		    	String pos = token.get(PartOfSpeechAnnotation.class);
		    	////System.out.println("ans"+token.originalText()+"|"+pos);
		    	if (pos.contains("VB"))
		    		return true;
	     	}
	    }
	    return false;
	}
	
	public static String parse(String input, StanfordCoreNLP pipeline) {
		
		input = input.replace("-", "");
		input = input.replace(", but", ".");
		ArrayList<String> numbers = new ArrayList<String>();
		input = ConjunctionResolver.parse(input, pipeline);
		String ans = "", text = dollarPreprocess(input);
		System.out.println(text);
		HashMap<String,String> coref = new HashMap<String,String>();
		////System.out.println(text);
	    Annotation document = new Annotation(text);
	    pipeline.annotate(document);
	    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	    Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);
	    ////////////System.out.println(graph);
	    //http://stackoverflow.com/questions/6572207/stanford-core-nlp-understanding-coreference-resolution
	    for(Map.Entry<Integer, CorefChain> entry : graph.entrySet()) {
            CorefChain c = entry.getValue();
            //this is because it prints out a lot of self references which aren't that useful
            if(c.getMentionsInTextualOrder().size() <= 1)
                continue;
            CorefMention cm = c.getRepresentativeMention();
            String clust = "";
            List<CoreLabel> tks = document.get(SentencesAnnotation.class).get(cm.sentNum-1).get(TokensAnnotation.class);
            for(int i = cm.startIndex-1; i < cm.endIndex-1; i++)
                clust += tks.get(i).get(TextAnnotation.class) + " ";
            clust = clust.trim();
            System.out.println("representative mention: \"" + clust + "\" is mentioned by:");
            for(CorefMention m : c.getMentionsInTextualOrder()){
                String clust2 = "";
                tks = document.get(SentencesAnnotation.class).get(m.sentNum-1).get(TokensAnnotation.class);
                for(int i = m.startIndex-1; i < m.endIndex-1; i++)
                    clust2 += tks.get(i).get(TextAnnotation.class) + " ";
                clust2 = clust2.trim();
                //don't need the self mention
                if(clust.equals(clust2))
                    continue;
                System.out.println("\t" + clust2 + tks.get(m.startIndex-1).get(PartOfSpeechAnnotation.class));
                if (tks.get(m.startIndex-1).get(PartOfSpeechAnnotation.class).startsWith("P") || clust2.toLowerCase().contains("the")) {
                	if (clust.contains("his ") || clust.contains("her ") || clust.contains("His ") || clust.contains("Her ") || clust.toLowerCase().equals("she") || clust.toLowerCase().equals("he")) {
                		System.out.println("check!"+clust);
                		if (!coref.isEmpty()) {
                			coref.put(clust2, coref.entrySet().iterator().next().getValue());
                		}
                		continue;
                	}
                	if ((clust2.equals("she") || clust2.equals("he")) && clust.contains(" "))
                		continue;
                	if (clust.matches("\\d+\\.\\d*")||clust.matches(".*\\d.*"))
                		continue;
                	////System.err.println(clust+clust2);
                	if (clust.toLowerCase().contains("they") && clust2.toLowerCase().contains("their"))
                		continue;
                	if (clust.toLowerCase().contains("their") && clust2.toLowerCase().contains("they"))
                		continue;
                	if (clust.contains("'s")) {
                		String root = clust.replace("'s", "").trim();
                		System.out.println(root+"|"+clust+"|"+clust2);
                		if (!clust2.equals("his") && !clust2.equals("theirs") && !clust2.equals("hers"))
                			coref.put(clust2, root);
                		else if (!clust.contains(clust2))
                			coref.put(clust2, clust);
                		continue;
                	}
                	if(!clust2.isEmpty())
                		coref.put(clust2, clust);
                }
            }
        }
	    for(CoreMap sentence: sentences) {
	    	for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	    		String pos = token.get(PartOfSpeechAnnotation.class);
	    		if (pos.contains("CD"))
		    		numbers.add(token.originalText());
	    	}
	    }
	    
        Iterator<Entry<String, String>> it = coref.entrySet().iterator();
        while (it.hasNext()) {
        	Entry<String, String> pair = it.next();
        	if (pair.getKey().toLowerCase().contains("the"))
        		text = text.replace(pair.getKey(), pair.getValue());
        }
        document = new Annotation(text);
	    pipeline.annotate(document);
	    sentences = document.get(SentencesAnnotation.class);
	    for(CoreMap sentence: sentences) {
	    	ArrayList<String> words = new ArrayList<String>();
	    	for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	    		String word = token.get(TextAnnotation.class);
	    	//	String lemma;
	   			//lemma = token.get(LemmaAnnotation.class);
	    		String pos = token.get(PartOfSpeechAnnotation.class);
	    		////////////System.out.println(word+"|"+pos+"|"+lemma+"|"+token.get(NamedEntityTagAnnotation.class));
	    		words.add(word);
	    //		if (pos.contains("W"))
	    	//		questionSentence = counter;
	    	    
	    	}
	    	Tree tree = sentence.get(TreeAnnotation.class);
	    	String parseExpr = tree.toString();
	    	System.out.println(parseExpr);
	    	String[] constituents = parseExpr.split(" ");
	    	//boolean quesFlag = false;
	    	int prevj = 0;
	    	for (int i=0; i<constituents.length; i++) {
	    		String initialPart = "", finalPart = "", tempFinal = "", tempInitial = "", verb = "";
	    		int pos = -1;
	    		//if (constituents[i].contains("(W"))
	    			//quesFlag = true;
	    		if (constituents[i].contains("VB")) {
	    			////System.out.println(constituents[i] + "|" + constituents[i+1] + "|" + constituents[i-1]);
	    			verb = constituents[i+1].replace(")", "");
	    			//System.out.println("v"+verb);
	    			
	    			for (int j=i-1; j>=prevj; j--) {
	    			
	    				if ((constituents[j].contains("(NP") || constituents[j].contains("(W")) && !sentence.toString().toLowerCase().contains("how") && !sentence.toString().toLowerCase().contains("'") && !constituents[j-1].contains("and")) 
	    					break;
	    				//if (constituents[j+1].contains("how"))
	    					//break;
	    				Pattern wordPattern = Pattern.compile("\\d+\\.\\d+|[^\\W\\d]+|\\d+");
						Matcher matcher = wordPattern.matcher(constituents[j]); 
						if (matcher.find()) {
							String candidate = matcher.group();
							if (words.contains(candidate)) {
								if (coref.containsKey(candidate)) {
									if (initialPart.startsWith("'"))
										initialPart = coref.get(candidate) + initialPart;
									else
										initialPart = coref.get(candidate) + " " + initialPart;
								}
								else
									initialPart = candidate + " " + initialPart;
							}
						}
						if (constituents[j].contains(","))
							initialPart = "," + initialPart;
						if (constituents[j].contains("'")) {
							System.err.println(constituents[j]);
							initialPart = constituents[j].replaceAll("(\\s|\\))+","").trim() + " " + initialPart;
						}

						
						////System.out.println(initialPart);
	    			}
	    			////System.out.println(initialPart);
	    			ArrayList<String> parenthesisStack = new ArrayList<String>();
	    			parenthesisStack.add("(");
	    			int j = i;
	    			for (; j<constituents.length; j++) {
	    				Pattern wordPattern = Pattern.compile("\\d+\\.\\d+|[^\\W\\d]+|\\d+");
	    				//////System.err.println("\\d+\\.\\d+|[^\\W\\d]+|\\d+.");
						Matcher matcher = wordPattern.matcher(constituents[j]);
						if (parenthesisStack.isEmpty()){ 
	    					break;
	    				}
	    				if (matcher.find() || constituents[j].contains(",")) {
	    					String candidate;
	    					try {
	    						candidate = matcher.group();
	    					}
	    					catch (Exception e) {
	    							candidate = ",";
	    					}
	    					if (candidate.equals("s"))
	    						candidate = "'s";
	    					System.out.println(candidate);
							if (candidate.contains(",")) {
								tempFinal = tempFinal + "mmmm";
								tempInitial = new String(initialPart + " " + finalPart);
								pos = j+1;
								////System.err.println(tempInitial+"|"+tempFinal);
								
							}
							else if (!tempFinal.isEmpty()) {
								if (words.contains(candidate)) {
									if (coref.containsKey(candidate))
										tempFinal = tempFinal + " " + coref.get(candidate);
									else
										tempFinal = tempFinal + " " + candidate;
								}
							}
							if (words.contains(candidate)) {
								if (coref.containsKey(candidate))
									finalPart = finalPart + " " + coref.get(candidate);
								else
									finalPart = finalPart + " " + candidate;
							}
							
	    				}
	    				Pattern leftParenthesisPattern = Pattern.compile("\\(");
	    				Pattern rightParenthesisPattern = Pattern.compile("\\)");
	    				matcher = leftParenthesisPattern.matcher(constituents[j]);
	    				while (matcher.find()) {
	    					parenthesisStack.add(matcher.group());
	    				}
	    				matcher = rightParenthesisPattern.matcher(constituents[j]);
	    				while (matcher.find()) {
	    					parenthesisStack.remove("(");
	    					matcher.group();
	    				}
	    			}
	    			/*if (quesFlag) {
	    				initialPart = "? " + initialPart;
	    				quesFlag = false;
	    			}*/
	    			if (!tempFinal.isEmpty()) {
	    				tempFinal = tempFinal.replace("mmmm", "");
	    				
	    				System.out.println(tempInitial + "|" + tempFinal);
	    				if (containsVerb(tempInitial, pipeline) && containsVerb(tempFinal, pipeline)) {
	    					tempInitial = (tempInitial.charAt(0) + "").toUpperCase() + tempInitial.substring(1);
	    	    			ans = (ans + tempInitial).replaceAll("\\s+"," ").trim() + "\n";
	    	    			System.out.println(ans);
	    	    			i = pos;
	    	    			prevj = i;
		    				continue;
	    				}
	    			}
	    			int next = j;
	    			//System.out.println("aaaa"+initialPart+"|"+finalPart+verb);
	    			/*if (containsVerb(initialPart, pipeline) && containsVerb(finalPart, pipeline)) {
	    				 next = sentence.toString().indexOf(finalPart);
	    				 //System.out.println("hhh"+next);
	    				 finalPart = "";
	    				 ans = (ans + initialPart + ".\n" + finalPart).trim() + "\n";
	    				 initialPart = "";
	    			}
	    			else if (containsVerb(finalPart.replace(verb, ""),pipeline)) {
	    				//System.out.println("mmm"+sentence.toString()+"|"+finalPart.replace(verb, "").trim());
	    				next = sentence.toString().indexOf(finalPart.replace(verb, "").trim());
	    				//System.out.println("hhh"+next);
	    				finalPart = initialPart;
	    				initialPart = initialPart + verb;
	    				ans = (ans + initialPart + ".\n" + finalPart).trim() + "\n";
	    				initialPart = "";
	    			}
	    			else*/
	    			ans = (ans + initialPart + " " + finalPart).trim() + ".\n";
	    			i = next;
	    			prevj = i;
	    			//initialPart = (initialPart.charAt(0) + "").toUpperCase() + initialPart.substring(1);
	    			
	    			//System.out.println("P" + initialPart + "|" + finalPart+ans);
	    		}
	    	}
	    	////////System.out.println(tree);
	    	
	    }
	    
	    ////System.out.println(ans);	    
	    String finalAns = entityResolution(ans,pipeline).replace(" , , ",", ").replaceAll("\\s+'s", "'s").trim();
	    Pattern numPattern = Pattern.compile("\\d+(\\.\\d+)*");
		Matcher matcher = numPattern.matcher(finalAns); 
		if (!matcher.find())
	    	return input;
		if (!new ArrayList<String>(Arrays.asList(finalAns.split(" "))).containsAll(numbers))
			return entityResolution(input,pipeline);
		int countNum = 0;
		matcher = numPattern.matcher(finalAns);
		while (matcher.find()) 
			countNum++;
		System.out.println("hi"+countNum+numbers.size());
		if (countNum != numbers.size())
			return entityResolution(input,pipeline);
	    return finalAns;
	}
	
	public static void main(String[] args) {
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner,parse,dcoref");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		System.out.println(parse("Ruth had 3 apples. She put 2 apples into a basket. How many apples are there in the basket now, if in the beginning there were 4 apples in the basket?",pipeline));
	}
}
