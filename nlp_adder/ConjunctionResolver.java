package nlp_adder;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class ConjunctionResolver {
	
	public static boolean containsVerb (String text, List<CoreLabel> tokens) {
		 for (CoreLabel token: tokens) {
		   	String pos = token.get(PartOfSpeechAnnotation.class);
		   	if (pos.contains("VB"))
		   		return true;
	     }
	    return false;
	}
	public static boolean containsPrep (String text, List<CoreLabel> tokens) {
		//  boolean afterVerb = false;
	   // if (!containsVerb(text,pipeline))
	    	//afterVerb = true;
	   // for (CoreMap sentence: sentences) {
	     	for (CoreLabel token: tokens) {
		    	String pos = token.get(PartOfSpeechAnnotation.class);
		    //	if (pos.contains("VB") && containsVerb(text,pipeline))
		    		//afterVerb = true;
		    	if ((pos.contains("IN") || pos.contains("TO")))
		    		return true;
	     	}
	    //}
	    return false;
	}
	public static String getVerbPhrase (String text, List<CoreLabel> tokens) {
		String verbPhrase = "";
		for (CoreLabel token: tokens) {
			//if (token.originalText().equals(text.split(" ")[0]) && tokens.get(tokens.indexOf(token)+1).originalText().equals(text.split(" ")[1])) 
			//	begin = true;
			//if (!begin)
			//	continue;
	    	String pos = token.get(PartOfSpeechAnnotation.class);
	    	verbPhrase = verbPhrase + token.originalText() + " ";
	    	System.out.println("vvvvv"+verbPhrase+pos);
	    	if (pos.contains("VB"))
	    		return verbPhrase;
     	}
		return verbPhrase;
	}
	public static String getPrepPhrase (String text, List<CoreLabel> tokens) {
		boolean crossPrep = false, crossVerb = false;
		String prepPhrase = "";
	    //boolean begin = false;
	    for (CoreLabel token: tokens) {
			String pos = token.get(PartOfSpeechAnnotation.class);
			System.out.println(token.originalText()+pos);
			if (pos.contains("VB"))
	    		crossVerb = true;
			if (containsVerb(text,tokens) && !crossVerb)
				continue;
			if (pos.contains("IN") || pos.contains("TO"))
	    		crossPrep = true;
			if (crossPrep)
				prepPhrase = prepPhrase + token.originalText() + " ";
     	}
	    prepPhrase = prepPhrase.replace(" .",".");
	    System.out.println("waka" + prepPhrase);
		
	    return prepPhrase.trim();
	}
	public static String parse(String input, StanfordCoreNLP pipeline) {
		String ans = "";
		input = input.replace(" .", ". ");
		Annotation document = new Annotation(input);
	    pipeline.annotate(document);
	    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for(CoreMap sentence: sentences) {
			boolean condition1 = sentence.toString().contains(" and ");
			boolean condition2 = sentence.toString().contains(" but ");
			boolean condition3 = sentence.toString().contains(" if ");
			//boolean condition4 = sentence.toString().contains(" to ");
			String splitString = "";
			if (condition1)
				splitString = " and ";
			if (condition2)
				splitString = " but ";
			if (condition3)
				splitString = " if ";
			//if (condition4)
				//splitString = "to";
			if (condition1 || condition2 || condition3) {
				String firstPart = sentence.toString().split(splitString)[0];
				String secondPart = sentence.toString().split(splitString)[1];
				String VP1="", VP2="", PrP1="", PrP2="", L1="", L2="", P1="", P2="";
				System.out.println(firstPart+"|"+secondPart);
				List<CoreLabel> firstPartTokens = new ArrayList<CoreLabel>();
				List<CoreLabel> secondPartTokens = new ArrayList<CoreLabel>();
				boolean endFirst = false;
				for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
					if (token.originalText().equals(splitString.trim())) {
						endFirst = true;
						continue;
					}
					if (endFirst) {
						secondPartTokens.add(token);
					}
					else
						firstPartTokens.add(token);
				}
				if (!containsVerb(firstPart, firstPartTokens)) {
					ans = ans + sentence.toString()+" ";
					continue;
				}
				VP1 = getVerbPhrase(firstPart, firstPartTokens);
				System.out.println("vp1"+VP1);
				String[] words = VP1.split(" ");
				String verb1 = words[words.length-1], verb2 = "";
				P1 = VP1.replace(verb1, "").trim();
				if (containsVerb(secondPart,secondPartTokens)) {
					VP2 = getVerbPhrase(secondPart,secondPartTokens);
					words = VP2.split(" ");
					verb2 = words[words.length-1];
					P2 = VP2.replace(verb2, "");
				}
				if (containsPrep(firstPart,firstPartTokens)) {
					PrP1 = getPrepPhrase(firstPart,firstPartTokens);
				}
				if (containsPrep(secondPart,secondPartTokens)) {
					PrP2 = getPrepPhrase(secondPart,secondPartTokens);
				}
				//System.out.println(PrP2);
				if (verb2.isEmpty())
					verb2 = verb1;
				if (VP2.isEmpty())
					VP2 = P2 + " " + verb2;
				if (P2.trim().isEmpty()) {
					P2 = P1; 
				}
				if (PrP1.isEmpty())
					PrP1 = PrP2;
				L1 = firstPart.replace(VP1,"");
				L1 = L1.replace(PrP1,"");
				L2 = secondPart.replace(VP2,"");
				L2 = L2.replace(PrP2,"");
				System.out.println(P1 + "|" + verb1 + "|" + L1 + " " + PrP1 + "|" + P2 + "|" + verb2 + "|"+ L2 + "|" + PrP2 + ".");
				if ((L1+PrP1).endsWith(",") || (L1+PrP1).endsWith("."))
					ans = ans + (P1 + " " + verb1 + " " + (L1 + " " +PrP1).substring(0, (L1+" "+PrP1).length())) + "  " +(P2 + " " + verb2 + " "+ L2 + " "+ PrP2) + " ";
				else
					ans = ans + (P1 + " " + verb1 + " " + L1 + " " +PrP1) + ". " + (P2 + " " + verb2 +" "+ L2 + " " +PrP2) + " ";
				if (!(L2+PrP2).endsWith(",") && !(L2+PrP2).endsWith("."))
					ans = ans + ". ";
			
				System.err.println(ans);
	    	}
			else
				ans = ans + sentence.toString()+" ";
		}
		return ans;
	}
	public static void main(String[] args) {
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner,parse,dcoref");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		System.out.println(parse("Sandy has 10 books , Benny has 24 books , and Tim has 33 books .How many books do they have together ?",pipeline));
	}
	
}
