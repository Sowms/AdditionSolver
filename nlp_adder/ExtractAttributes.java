/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nlp_adder;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;




import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import rita.RiGrammar;
import simplenlg.framework.*;
import simplenlg.lexicon.*;
import simplenlg.realiser.english.*;
import simplenlg.phrasespec.*;
import simplenlg.features.*;

/**
 *
 * @author admin
 */
class Attributes {
	double avgLength;
    double numLength;
    boolean extraInfo;
    boolean extraNo;
    boolean isDecimal;
    boolean isAggregator;
    ArrayList<String> schemas;
    ArrayList<String> keywords;
    LinguisticInfo extractedInformation;
}
public class ExtractAttributes {
    static ArrayList<String> schemas = new ArrayList<String>();
    static ArrayList<String> keywords = new ArrayList<String>();	
    
    public static Attributes extract(String problem) {
    	schemas = new ArrayList<String>();
    	keywords = new ArrayList<String>();
    	Attributes attributes = new Attributes();
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma, ner,parse,dcoref");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
    	String simplifiedProblem = Parser.parse(problem, pipeline);
        double avgLength = ((double) (problem.split(" ").length))/(problem.split("\\.").length);
        double numLength = 0, normaliser = 0;
        double isQuestionCounter = 0;
        boolean isDecimal = false;
        attributes.extractedInformation = (new SentencesAnalyzer()).extract(simplifiedProblem, pipeline);
        attributes.extractedInformation = OrderSteps.order(attributes.extractedInformation);
        for (LinguisticStep sentence : attributes.extractedInformation.sentences) {
            /*System.out.print(sentence.aggregator+"|");
            System.out.print(sentence.comparator+"|");
            System.out.print(sentence.difference+"|");
            System.out.print(sentence.isQuestion+"|");
            System.out.print(sentence.keyword+"|");
            System.out.print(sentence.owner1+"|");
            System.out.print(sentence.owner2+"|");
            System.out.print(sentence.entityName+"|");
            System.out.print(sentence.entityValue+"|");
            System.out.print(sentence.procedureName+"|");
            System.out.print(sentence.setCompletor+"|");
            System.out.print(sentence.verbQual+"\n");*/
        	if (sentence.procedureName != null) {
                schemas.add(sentence.procedureName);
                keywords.add(sentence.keyword);
        	}
        	if (sentence.entityValue != null && !sentence.entityValue.equals("some")) {
        		numLength = numLength + sentence.entityValue.split("\\.")[0].length();
        		if (sentence.entityValue.contains("."))
        			isDecimal = true;
        		normaliser++;
        	}
        	if (sentence.aggregator && sentence.isQuestion)
        		attributes.isAggregator = true;
            //NLGElement s1 = nlgFactory.createSentence("my dog is happy");
            //String output = realiser.realiseSentence(s1);
            
     
        }
        numLength = numLength/normaliser;
        attributes.extraInfo = (isQuestionCounter > 1);
        attributes.extraNo = (normaliser > schemas.size() + 1);
        attributes.avgLength = avgLength;
        attributes.numLength = numLength;
        attributes.schemas = schemas;
        attributes.isDecimal = isDecimal;
        attributes.keywords = keywords;
    	return attributes;
    }
    public static void main(String args[]) {
        String problem = "There are 7 crayons in the drawer and 6.5 crayons on the desk . Sam placed 4 crayons and 8 scissors on the desk . How many crayons are now there in total ? ";
        Attributes a = extract(problem);
        System.out.println("extraNo = " + a.extraNo);
        System.out.println("extraInfo = " + a.extraInfo);
        System.out.println("avgLength = " + a.avgLength);
        System.out.println("avgDigitLength = " + a.numLength);
        System.out.println("isDecimal = " + a.isDecimal);
        System.out.println(a.schemas);
        System.out.println(a.keywords);
   }
    
}
