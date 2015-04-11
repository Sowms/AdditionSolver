package nlp_adder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
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
		String ans = "";
		HashMap<String,String> coref = new HashMap<String,String>();
	    String text = input; 
	    Annotation document = new Annotation(text);
	    pipeline.annotate(document);
	    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	    Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);
	    ////System.out.println(graph);
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
            //System.out.println("representative mention: \"" + clust + "\" is mentioned by:");
            for(CorefMention m : c.getMentionsInTextualOrder()){
                String clust2 = "";
                tks = document.get(SentencesAnnotation.class).get(m.sentNum-1).get(TokensAnnotation.class);
                for(int i = m.startIndex-1; i < m.endIndex-1; i++)
                    clust2 += tks.get(i).get(TextAnnotation.class) + " ";
                clust2 = clust2.trim();
                //don't need the self mention
                if(clust.equals(clust2))
                    continue;
                //System.out.println("\t" + clust2 + tks.get(m.startIndex-1).get(PartOfSpeechAnnotation.class));
                if (tks.get(m.startIndex-1).get(PartOfSpeechAnnotation.class).startsWith("P"))
                	coref.put(clust2, clust);
            }
        }

	    for(CoreMap sentence: sentences) {
	    	ArrayList<String> words = new ArrayList<String>();
	    	for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	    		String word = token.get(TextAnnotation.class);
	    	//	String lemma;
	   			//lemma = token.get(LemmaAnnotation.class);
	    		//String pos = token.get(PartOfSpeechAnnotation.class);
	    		////System.out.println(word+"|"+pos+"|"+lemma+"|"+token.get(NamedEntityTagAnnotation.class));
	    		words.add(word);
	    //		if (pos.contains("W"))
	    	//		questionSentence = counter;
	    	}
	    	Tree tree = sentence.get(TreeAnnotation.class);
	    	String parseExpr = tree.toString();
	    	System.out.println(parseExpr);
	    	String[] constituents = parseExpr.split(" ");
	    	//boolean quesFlag = false;
	    	for (int i=0; i<constituents.length; i++) {
	    		String initialPart = "", finalPart = "", tempFinal = "", tempInitial = "";
	    		int pos = -1;
	    		//if (constituents[i].contains("(W"))
	    			//quesFlag = true;
	    		if (constituents[i].contains("VB")) {
	    			for (int j=i-1; j>=0; j--) {
	    				if (constituents[j].contains("(NP") || constituents[j].contains("(W")) 
	    					break;
	    				Pattern wordPattern = Pattern.compile("[A-Za-z0-9]+");
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
	    			}
	    			ArrayList<String> parenthesisStack = new ArrayList<String>();
	    			parenthesisStack.add("(");
	    			int j = i;
	    			for (; j<constituents.length; j++) {
	    				Pattern wordPattern = Pattern.compile("[A-Za-z0-9]+");
						Matcher matcher = wordPattern.matcher(constituents[j]);
	    				if (parenthesisStack.isEmpty() || constituents[j].contains("(S") || constituents[j].contains("(,")){ 
	    					break;
	    				}
	    				if (matcher.find()) {
	    					String candidate = matcher.group();
							if (candidate.equals("and")) {
								tempFinal = tempFinal + "mmmm";
								tempInitial = new String(initialPart + " " + finalPart);
								pos = j+1;
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
	    				//System.out.println(tempInitial + "|" + tempFinal);
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
	    			System.out.println("P" + initialPart + "|" + finalPart);
	    			i = next;
	    			initialPart = (initialPart.charAt(0) + "").toUpperCase() + initialPart.substring(1);
	    			ans = (ans + initialPart + finalPart).trim() + ".\n";
	    		}
	    	}
	    	//System.out.println(tree);
	    	
	    }
	    
	    	    
	    return ans;
	}
}
