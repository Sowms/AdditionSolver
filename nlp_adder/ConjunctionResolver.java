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
	
	public static boolean containsVerb (List<CoreLabel> tokens) {
		 int index=1;
		 for (CoreLabel token: tokens) {
		   	String pos = token.get(PartOfSpeechAnnotation.class);
		   	////System.out.println(pos+token);
		   	if (pos.contains("VB") && tokens.indexOf(token)!=0 && !tokens.get(tokens.indexOf(token)-1).tag().contains("TO") && index < tokens.size() && !containsVerb(tokens.subList(index, tokens.size()-1)))
		   		return true;
		   	if (pos.contains("VB") && tokens.indexOf(token)==0) {
		   		if (tokens.size() > 1 && index < tokens.size() && !containsVerb(tokens.subList(index, tokens.size()-1)))
		   			return true;
		   		if (tokens.size()==1)
		   			return true;
		   	}
		   	index++;
	     }
	    return false;
	}
	
	public static boolean containsPrep (List<CoreLabel> tokens) {
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
	public static String getVerbPhrase (List<CoreLabel> tokens) {
		String verbPhrase = "";
		int index=1;
		for (CoreLabel token: tokens) {
			//if (token.originalText().equals(text.split(" ")[0]) && tokens.get(tokens.indexOf(token)+1).originalText().equals(text.split(" ")[1])) 
			//	begin = true;
			//if (!begin)
			//	continue;
	    	String pos = token.get(PartOfSpeechAnnotation.class);
	    	if (!verbPhrase.isEmpty() && !verbPhrase.endsWith(" "))
	    		verbPhrase = verbPhrase + " ";
	    	if (pos.equals("$") || pos.equals(","))
	    		verbPhrase = verbPhrase + token.originalText();
	    	else
	    		verbPhrase = verbPhrase + token.originalText() + " ";
	    	////System.out.println("vvvvv"+verbPhrase+pos);
	    	if ((pos.contains("VB") && !containsVerb(tokens.subList(index, tokens.size()-1))) || token.originalText().startsWith("ha"))
	    		return verbPhrase.replace(" '","'").replace(" ,",",").trim();
	    	if ((pos.contains("VB") && containsPrep(tokens.subList(index, tokens.size()-1))) || token.originalText().startsWith("ha"))
	    		return verbPhrase.replace(" '","'").replace(" ,",",").trim();
	    	index++;
     	}
		return verbPhrase.trim();
	}
	public static String getPrepPhrase (String text, List<CoreLabel> tokens) {
		boolean crossPrep = false, crossVerb = false;
		String prepPhrase = "";
	    //boolean begin = false;
	    for (CoreLabel token: tokens) {
			String pos = token.get(PartOfSpeechAnnotation.class);
			////System.out.println(token.originalText()+pos);
			if (pos.contains("VB"))
	    		crossVerb = true;
			if (containsVerb(tokens) && !crossVerb)
				continue;
			/*if (!prepPhrase.isEmpty() && !prepPhrase.endsWith(" ") && !prepPhrase.endsWith("$"))
	    		prepPhrase = prepPhrase + " ";*/
			if (!crossPrep && pos.contains("IN") && !token.originalText().equals("by") || pos.contains("TO")) {
	    		crossPrep = true;
	    		prepPhrase = prepPhrase + token.originalText() + " ";
	    		continue;
			}
			if (crossPrep && (pos.contains("IN") && !token.originalText().equals("by") || pos.contains("TO"))) {
				prepPhrase = "";
				//System.out.println("\twaka" + prepPhrase);
				prepPhrase = prepPhrase + token.originalText() + " ";
	    		continue;
			}
			if (crossPrep && !pos.equals("$"))
				prepPhrase = prepPhrase + token.originalText() + " ";
			else if (crossPrep)
				prepPhrase = prepPhrase + token.originalText();
    		
			
     	}
	    prepPhrase = prepPhrase.replace(" .",".");
	    prepPhrase = prepPhrase.replace(" ,",",");
	    //System.out.println("waka" + prepPhrase);
		
	    return prepPhrase.replace(" '","'").trim();
	}
	public static String parse(String input, StanfordCoreNLP pipeline) {
		String ans = "";
		input = input.replace(" .", ". ");
		input = input.replace(" ,", ", ");
		input = input.replace("$ ", "$").replace(" '","'").trim();;
		input = input.replaceAll("\\s+", " ").trim();
		input = input.trim();
		//System.out.println(input);
		Annotation document = new Annotation(input);
	    pipeline.annotate(document);
	    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for(CoreMap sentence: sentences) {
			boolean condition1 = sentence.toString().contains(" and ");
			boolean condition2 = sentence.toString().contains(" but ");
			boolean condition3 = sentence.toString().contains(" if ");
			boolean condition4 = sentence.toString().contains(" then ");
			//System.out.println(condition1);
			String splitString = "";
			if (condition1)
				splitString = " and ";
			else if (condition2)
				splitString = " but ";
			else if (condition3)
				splitString = " if ";
			else if (condition4)
				splitString = " then ";
			if (condition1 || condition2 || condition3 || condition4) {
				String firstPart = sentence.toString().split(splitString)[0];
				String secondPart = sentence.toString().split(splitString)[1];
				String VP1="", VP2="", PrP1="", PrP2="", L1="", L2="", P1="", P2="";
				//System.out.println(firstPart+"|"+secondPart);
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
				//System.out.println(firstPart+"|"+secondPart);
				if (!containsVerb(firstPartTokens)) {
					ans = ans + sentence.toString()+" ";
					continue;
				}
				VP1 = getVerbPhrase(firstPartTokens);
				////System.out.println("vp1"+VP1);
				String[] words = VP1.split(" ");
				String verb1 = words[words.length-1], verb2 = "";
				P1 = VP1.substring(0, VP1.length()-verb1.length()).trim();
				if (containsVerb(secondPartTokens)) {
					VP2 = getVerbPhrase(secondPartTokens);
					words = VP2.split(" ");
					verb2 = words[words.length-1];
					P2 = VP2.substring(0, VP2.length()-verb2.length()).trim();
				}
				if (containsPrep(firstPartTokens)) {
					PrP1 = getPrepPhrase(firstPart,firstPartTokens);
				}
				if (containsPrep(secondPartTokens)) {
					PrP2 = getPrepPhrase(secondPart,secondPartTokens);
				}
				//System.out.println("aa"+PrP2);
				//System.out.println("aa"+PrP1);
				//System.out.println("aa"+verb2);
				//System.out.println("aa"+P2);
				//System.out.println("aa"+VP2);
				
				
				
				if (verb2.isEmpty())
					verb2 = verb1;
				if (VP2.isEmpty())
					VP2 = P2 + " " + verb2;
				if (P2.trim().isEmpty()) {
					P2 = P1; 
				}
				if (PrP1.isEmpty() && !PrP2.startsWith("for"))
					PrP1 = PrP2;
				////System.out.println(VP1+"|"+VP2);
				L1 = firstPart.replace(VP1,"");
				L1 = L1.trim();
				L1 = L1.replace(PrP1,"");
				L1 = L1.trim();
				L2 = secondPart.replace(VP2,"");
				L2 = L2.trim();
				L2 = L2.replace(PrP2,"");
				L2 = L2.trim();
				//System.out.println(P1 + "|" + verb1 + "|" + L1 + "|" + PrP1);
				//System.out.println( P2 + "|" + verb2 + "|"+ L2 + "|" + PrP2);
				if ((L1+PrP1).trim().endsWith(",") || (L1+PrP1).endsWith("."))
					ans = ans + (P1 + " " + verb1 + " " + (L1 + " " +PrP1).substring(0, (L1+" "+PrP1).length())) + "  " +(P2 + " " + verb2 + " "+ L2 + " "+ PrP2) + " ";
				else
					ans = ans + (P1 + " " + verb1 + " " + L1 + " " +PrP1) + ". " + (P2 + " " + verb2 +" "+ L2 + " " +PrP2) + " ";
				if (!(L2+PrP2).trim().endsWith(",") && !(L2+PrP2).endsWith("."))
					ans = ans + ". ";
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
		//System.out.println(parse("The next day she went back and asked for another 0.5 inch to be cut off . How much hair did she have cut off in all ? ",pipeline));
	}
	
}
