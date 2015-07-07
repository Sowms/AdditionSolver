package nlp_adder;

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
	public static boolean containsPrep (String text, StanfordCoreNLP pipeline) {
		Annotation document = new Annotation(text);
	    pipeline.annotate(document);
	    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	    for (CoreMap sentence: sentences) {
	     	for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
		    	String pos = token.get(PartOfSpeechAnnotation.class);
		    	if (pos.contains("IN") || pos.contains("TO"))
		    		return true;
	     	}
	    }
	    return false;
	}
	public static String getVerbPhrase (String text, CoreMap sentence) {
		String verbPhrase = "";
		boolean begin = false;
		List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
		for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
			if (token.originalText().equals(text.split(" ")[0]) && tokens.get(tokens.indexOf(token)+1).originalText().equals(text.split(" ")[1])) 
				begin = true;
			if (!begin)
				continue;
	    	String pos = token.get(PartOfSpeechAnnotation.class);
	    	verbPhrase = verbPhrase + token.originalText() + " ";
	    	System.out.println(verbPhrase+pos);
	    	if (pos.contains("VB"))
	    		return verbPhrase;
     	}
		return verbPhrase;
	}
	public static String getPrepPhrase (String text, CoreMap sentence) {
		String prepPhrase = "";
		boolean crossPrep = false, begin = false;
		System.out.println("waka" + text);
		List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
		for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
			if (token.originalText().equals(text.split(" ")[text.split(" ").length - 1]))
				return prepPhrase + token.originalText();
			if (token.originalText().equals(text.split(" ")[0]) && tokens.get(tokens.indexOf(token)+1).originalText().equals(text.split(" ")[1])) 
				begin = true;
			if (!begin)
				continue;
			String pos = token.get(PartOfSpeechAnnotation.class);
			System.out.println(prepPhrase+pos);
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
			String splitString = "";
			if (condition1)
				splitString = " and ";
			if (condition2)
				splitString = " but ";
			if (condition3)
				splitString = " if ";
			if (condition1 || condition2 || condition3) {
				String firstPart = sentence.toString().split(splitString)[0];
				String secondPart = sentence.toString().split(splitString)[1];
				String VP1="", VP2="", PrP1="", PrP2="", L1="", L2="", P1="", P2="";
				//System.out.println(firstPart+"|"+secondPart);
				VP1 = getVerbPhrase(firstPart,sentence);
				String[] words = VP1.split(" ");
				String verb1 = words[words.length-1], verb2 = "";
				P1 = VP1.replace(verb1, "").trim();
				if (containsVerb(secondPart,pipeline)) {
					VP2 = getVerbPhrase(secondPart,sentence);
					words = VP2.split(" ");
					verb2 = words[words.length-1];
					P2 = VP2.replace(verb2, "");
				}
				if (containsPrep(firstPart,pipeline)) {
					PrP1 = getPrepPhrase(firstPart,sentence);
				}
				if (containsPrep(secondPart,pipeline)) {
					PrP2 = getPrepPhrase(secondPart,sentence);
				}
				System.out.println(PrP2);
				if (verb2.isEmpty())
					verb2 = verb1;
				if (VP2.isEmpty())
					VP2 = P2 + " " + verb2;
				if (P2.trim().isEmpty()) {
					P2 = P1; 
				}
				if (PrP1.isEmpty())
					PrP1 = PrP2;
				System.out.println(VP1+"|"+VP2+"|"+P1 + "|" + verb1 + "|" + P2 + "|" + verb2 + "|"+ PrP1 + "|" + PrP2 + "|" + L1 + "|" + L2+".");
				L1 = firstPart.replace(VP1,"");
				L1 = L1.replace(PrP1,"");
				L2 = secondPart.replace(VP2,"");
				L2 = L2.replace(PrP2,"");
				System.out.println(L1+"|"+L2);
				if ((L1+PrP1).endsWith(",") || (L1+PrP1).endsWith("."))
					ans = ans + (P1 + " " + verb1 + " " + L1 + " " +PrP1) + " " +(P2 + " " + verb2 + " "+ L2 + " "+ PrP2) + " ";
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
		System.out.println(parse(" Dan's cat had kittens and 5 had spots . He gave 7 to Tim and 4 to Jason . He now has 5 kittens . How many kittens did he have to start with ?",pipeline));
	}
	
}
