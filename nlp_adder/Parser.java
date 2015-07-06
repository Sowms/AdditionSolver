package nlp_adder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class Parser {

	public static String dollarPreprocess(String input) {
		if (input.contains("$")) {
			String ans = "";
			boolean dollarFlag = false;
			for (String word : input.split(" ")) {
				if (!word.contains("$"))
					ans = ans + word + " ";
				else {
					////System.out.println("waka");
					dollarFlag = true;
					ans = ans + word.replace("$", "") + " ";
					if (!word.replace("$","").isEmpty()) {
						ans = ans + word.replace("$", "") + " ";
					
					}
					////System.out.println(ans);
					continue;
				}
				if (dollarFlag) {
					ans = ans + "dollars ";
					dollarFlag = false;
				}
			}
			return ans;
		}
		return input;
	}
	public static String entityResolution(String input, StanfordCoreNLP pipeline) {
		
		
		Annotation document = new Annotation(input);
	    pipeline.annotate(document);
	    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	    ArrayList<String> possibleEntities = new ArrayList<String>();
	    for (CoreMap sentence: sentences) {
	     	for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
		    	String pos = token.get(PartOfSpeechAnnotation.class);
		    	if (pos.contains("NNS") || pos.contains("NNPS") || token.originalText().contains("fish") || token.originalText().contains("sheep"))
		    		possibleEntities.add(token.originalText());
	     	}
	    }
	    String ans = "";
	    
	    for (CoreMap sentence: sentences) {
	    	boolean entity = true;
	    	//Tree tree = sentence.get(TreeAnnotation.class);
	    	//String parseExpr = tree.toString();
	    	////System.out.println(parseExpr);
	    	List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
	     	for (CoreLabel token: tokens) {
		    	String pos = token.get(PartOfSpeechAnnotation.class);
		    	ans = ans + token.originalText() + " ";
		    	if (!entity && (pos.contains("JJ") || pos.equals("NN")) && !possibleEntities.contains(tokens.get(tokens.indexOf(token)+1).originalText()) && (tokens.get(tokens.indexOf(token)-1).get(PartOfSpeechAnnotation.class).contains("CD")) && (tokens.get(tokens.indexOf(token)-1).get(PartOfSpeechAnnotation.class).contains("CD")))
		    			ans = ans + possibleEntities.get(0)+" ";
		    	if (pos.contains("CD") && !token.originalText().contains(".")) { 
		    		entity = possibleEntities.contains(tokens.get(tokens.indexOf(token)+1).originalText());
		    		if (!entity) {
		    			CoreLabel nextToken = tokens.get(tokens.indexOf(token)+1);
		    			if(!nextToken.get(PartOfSpeechAnnotation.class).contains("JJ") && !nextToken.get(PartOfSpeechAnnotation.class).contains("NN"))
		    				ans = ans + possibleEntities.get(0)+" ";
		    		}
		    	}
		    //	if (pos.contains("VB") && possibleEntities.contains(tokens.get(tokens.indexOf(token)+1).get(LemmaAnnotation.class)))
		    	//	ans = ans + "some ";
	     	}
	    }
	    ////System.err.println(ans);
	    return ans;
	}
	public static boolean containsVerb (String text, StanfordCoreNLP pipeline) {
		Annotation document = new Annotation(text);
	    pipeline.annotate(document);
	    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	    for (CoreMap sentence: sentences) {
	     	for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
		    	String pos = token.get(PartOfSpeechAnnotation.class);
		    	if (pos.contains("VB"))
		    		return true;
	     	}
	    }
	    return false;
	}
	
	public static String parse(String input, StanfordCoreNLP pipeline) {
		input = input.replace("-", "");
		input = ConjunctionResolver.parse(input, pipeline);
		String ans = "", text = dollarPreprocess(input);
		HashMap<String,String> coref = new HashMap<String,String>();
		//System.out.println(text);
	    Annotation document = new Annotation(text);
	    pipeline.annotate(document);
	    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	    Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);
	    //////////System.out.println(graph);
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
            ////System.out.println("representative mention: \"" + clust + "\" is mentioned by:");
            for(CorefMention m : c.getMentionsInTextualOrder()){
                String clust2 = "";
                tks = document.get(SentencesAnnotation.class).get(m.sentNum-1).get(TokensAnnotation.class);
                for(int i = m.startIndex-1; i < m.endIndex-1; i++)
                    clust2 += tks.get(i).get(TextAnnotation.class) + " ";
                clust2 = clust2.trim();
                //don't need the self mention
                if(clust.equals(clust2))
                    continue;
                ////System.out.println("\t" + clust2 + tks.get(m.startIndex-1).get(PartOfSpeechAnnotation.class));
                if (tks.get(m.startIndex-1).get(PartOfSpeechAnnotation.class).startsWith("P")) {
                	if (clust.contains("his") || clust.contains("her") || clust.contains("His") || clust.contains("Her")) {
                		coref.put(clust2, coref.entrySet().iterator().next().getValue());
                		continue;
                	}
                	if (clust.matches("\\d+\\.\\d*"))
                		continue;
                	////System.err.println(clust);
                	if(!clust2.toLowerCase().equals("their") && !clust2.toLowerCase().equals("they") && !clust2.isEmpty())
                		coref.put(clust2, clust);
                }
            }
        }

	    for(CoreMap sentence: sentences) {
	    	ArrayList<String> words = new ArrayList<String>();
	    	for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	    		String word = token.get(TextAnnotation.class);
	    	//	String lemma;
	   			//lemma = token.get(LemmaAnnotation.class);
	    		//String pos = token.get(PartOfSpeechAnnotation.class);
	    		//////////System.out.println(word+"|"+pos+"|"+lemma+"|"+token.get(NamedEntityTagAnnotation.class));
	    		words.add(word);
	    //		if (pos.contains("W"))
	    	//		questionSentence = counter;
	    	}
	    	Tree tree = sentence.get(TreeAnnotation.class);
	    	String parseExpr = tree.toString();
	    	//System.out.println(parseExpr);
	    	String[] constituents = parseExpr.split(" ");
	    	//boolean quesFlag = false;
	    	for (int i=0; i<constituents.length; i++) {
	    		String initialPart = "", finalPart = "", tempFinal = "", tempInitial = "";
	    		int pos = -1;
	    		//if (constituents[i].contains("(W"))
	    			//quesFlag = true;
	    		if (constituents[i].contains("VB")) {
	    			//System.out.println(constituents[i] + "|" + constituents[i+1] + "|" + constituents[i-1]);
	    			for (int j=i-1; j>=0; j--) {
	    				if (constituents[j].contains("(NP") || constituents[j].contains("(W")) 
	    					break;
	    				Pattern wordPattern = Pattern.compile("\\d+\\.\\d+|[^\\W\\d]+|\\d+");
						Matcher matcher = wordPattern.matcher(constituents[j]); 
						if (matcher.find()) {
							String candidate = matcher.group();
							if (words.contains(candidate)) {
								if (coref.containsKey(candidate))
									initialPart = coref.get(candidate) + " " + initialPart;
								else
									initialPart = candidate + " " + initialPart;
							}
						}
						//System.out.println(initialPart);
	    			}
	    			//System.out.println(initialPart);
	    			ArrayList<String> parenthesisStack = new ArrayList<String>();
	    			parenthesisStack.add("(");
	    			int j = i;
	    			for (; j<constituents.length; j++) {
	    				Pattern wordPattern = Pattern.compile("\\d+\\.\\d+|[^\\W\\d]+|\\d+");
	    				////System.err.println("\\d+\\.\\d+|[^\\W\\d]+|\\d+.");
						Matcher matcher = wordPattern.matcher(constituents[j]);
						if (parenthesisStack.isEmpty()){ 
	    					break;
	    				}
	    				if (matcher.find()) {
	    					String candidate = matcher.group();
	    					//System.out.println(candidate);
							if (candidate.equals("and") || candidate.equals("if") || candidate.equals("but")) {
								tempFinal = tempFinal + "mmmm";
								tempInitial = new String(initialPart + " " + finalPart);
								pos = j+1;
								//System.err.println(tempInitial+"|"+tempFinal);
								
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
	    				
	    				////////System.out.println(tempInitial + "|" + tempFinal);
	    				if (containsVerb(tempInitial, pipeline) && containsVerb(tempFinal, pipeline)) {
	    					tempInitial = (tempInitial.charAt(0) + "").toUpperCase() + tempInitial.substring(1);
	    	    			ans = (ans + tempInitial).trim() + ".\n";
	    	    			i = pos;
		    				continue;
	    				}
	    			}
	    			int next = j;
	    			if (containsVerb(initialPart, pipeline) && containsVerb(finalPart, pipeline)) {
	    				 next = initialPart.indexOf(finalPart);
	    				 finalPart = "";
	    			}
	    			//////System.out.println("P" + initialPart + "|" + finalPart);
	    			i = next;
	    			//initialPart = (initialPart.charAt(0) + "").toUpperCase() + initialPart.substring(1);
	    			ans = (ans + initialPart + finalPart).trim() + ".\n";
	    		}
	    	}
	    	//////System.out.println(tree);
	    	
	    }
	    
	    //System.out.println(ans);	    
	    return entityResolution(ans,pipeline);
	}
	
	public static void main(String[] args) {
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner,parse,dcoref");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		//////System.out.println(parse("Ned took a shortcut on the way home which was only 5 miles long",pipeline));
	}
}
