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
public class ExtractAttributes {
    static HashMap<String,String> procedureMap = new HashMap<String,String>();
    static HashMap<String,String> keywordMap = new HashMap<String,String>();
    static ArrayList<String> schemas = new ArrayList<String>();
	private static BufferedReader br;
    private static final String CHANGE_OUT = "changeOut";
    private static final String CHANGE_IN = "changeIn";
    private static final String COMPARE_PLUS = "comparePlus";
    private static final String COMPARE_MINUS = "compareMinus";
    private static final String INCREASE = "increase";
    private static final String REDUCTION = "reduction";
    private static final String ALTOGETHER_EQ = "altogetherEq";
    private static final String COMPARE_PLUS_EQ = "comparePlusEq";
    private static final String COMPARE_MINUS_EQ = "compareMinusEq";
		
    private static void loadProcedureLookup() {
		keywordMap.put("put", CHANGE_OUT);
		keywordMap.put("place", CHANGE_OUT);
		keywordMap.put("plant", CHANGE_OUT);
		keywordMap.put("stack", CHANGE_OUT);
		keywordMap.put("add", CHANGE_OUT);
		keywordMap.put("sell", CHANGE_OUT);
		keywordMap.put("give", CHANGE_OUT);
		keywordMap.put("load", CHANGE_OUT);
		keywordMap.put("pour", CHANGE_OUT);
		keywordMap.put("build", CHANGE_OUT);
		keywordMap.put("more than", COMPARE_PLUS);
		keywordMap.put("less than", COMPARE_MINUS);
		keywordMap.put("get", CHANGE_IN);
		keywordMap.put("buy", CHANGE_IN);
		keywordMap.put("pick", CHANGE_IN);
		keywordMap.put("cut", CHANGE_IN);
		keywordMap.put("take", CHANGE_IN);
		keywordMap.put("receive", CHANGE_IN);
		keywordMap.put("borrow", CHANGE_IN);
		keywordMap.put("lose", REDUCTION);
		keywordMap.put("use", REDUCTION);
		keywordMap.put("leave", REDUCTION);
		keywordMap.put("transfer", REDUCTION);
		keywordMap.put("spill", REDUCTION);
		keywordMap.put("leak", REDUCTION);
		keywordMap.put("produce", INCREASE);
		keywordMap.put("remove", REDUCTION);
		keywordMap.put("spend", REDUCTION);
		keywordMap.put("eat", REDUCTION);
		keywordMap.put("more", INCREASE);
		keywordMap.put("immigrate", INCREASE);
		keywordMap.put("increase", INCREASE);
		keywordMap.put("carry", INCREASE);
		keywordMap.put("saw", REDUCTION);
		keywordMap.put("taller", INCREASE);
		//keywordMap.put("find", INCREASE);
		keywordMap.put("decrease", REDUCTION);
		//keywordMap.put("break", REDUCTION);
		keywordMap.put("finish", REDUCTION);
		
		procedureMap.put(CHANGE_OUT, "[owner1]-[entity].[owner2]+[entity]");
		procedureMap.put(CHANGE_IN, "[owner1]+[entity].[owner2]-[entity]");
		procedureMap.put(COMPARE_PLUS, "[entity]+[owner2]");
		procedureMap.put(COMPARE_MINUS, "[owner2]-[entity]");
		procedureMap.put(REDUCTION, "[owner1]-[entity]");
		procedureMap.put(INCREASE, "[owner1]+[entity]");
		procedureMap.put(ALTOGETHER_EQ, "[owner1]+[owner2]=[entity]");
		procedureMap.put(COMPARE_PLUS_EQ, "[owner1] = [owner2]+[entity]");
		procedureMap.put(COMPARE_MINUS_EQ, "[owner1] = [owner2]-[entity]");
	}
    public static void main(String args[]) {
        Properties props = new Properties();
	props.put("annotators", "tokenize, ssplit, pos, lemma, ner,parse,dcoref");
	StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        String problem = "Joan has 3 apples. She gave 2 of them to Sam. How many apples does she have now?";
        String simplifiedProblem = Parser.parse(problem, pipeline);
        double avgLength = ((double) (problem.split(" ").length))/(problem.split("\\.").length);
        double numLength = 0, normaliser = 0;
        LinguisticInfo extractedInformation = (new SentencesAnalyzer()).extract(simplifiedProblem, pipeline);
        extractedInformation = OrderSteps.order(extractedInformation);
        System.out.println(extractedInformation.entities);
        System.out.println(extractedInformation.owners);
        for (LinguisticStep sentence : extractedInformation.sentences) {
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
        	if (sentence.procedureName != null)
                schemas.add(sentence.procedureName);
            Lexicon lexicon = Lexicon.getDefaultLexicon();
            NLGFactory nlgFactory = new NLGFactory(lexicon);
            Realiser realiser = new Realiser(lexicon);
            //NLGElement s1 = nlgFactory.createSentence("my dog is happy");
            //String output = realiser.realiseSentence(s1);
            SPhraseSpec p = nlgFactory.createClause();
            p.setSubject(sentence.owner1);
            p.setVerb(sentence.verbQual);
            if (!(sentence.entityValue == null && sentence.isQuestion)) {
            	p.setObject(sentence.entityValue + " " + sentence.entityName);
            	numLength = numLength + sentence.entityValue.length();
            	normaliser++;
            }
            else
            	p.setObject(sentence.entityName);
            if (!sentence.owner2.isEmpty()) {
            	PPPhraseSpec complement = nlgFactory.createPrepositionPhrase("to");
        		complement.setComplement(sentence.owner2);
        		p.setComplement(complement);
            }
            //p.setIndirectObject(sentence.owner2);
            if (sentence.isQuestion && !sentence.aggregator && !sentence.comparator && !sentence.difference && !sentence.setCompletor) {
            	p.setFeature(Feature.INTERROGATIVE_TYPE,InterrogativeType.HOW_MANY);
            	//p.setFeature(Feature.PASSIVE,true);
            }
            if (sentence.tense.equals("past"))
            	p.setFeature(Feature.TENSE, Tense.PAST);
            if (sentence.isQuestion) {
            	p = nlgFactory.createClause();
        		NPPhraseSpec subject = nlgFactory.createNounPhrase(sentence.entityName);
        		subject.setFeature(Feature.NUMBER, NumberAgreement.PLURAL);
        		p.setSubject(subject);
        		VPPhraseSpec verb = nlgFactory.createVerbPhrase("belong");
        		p.setVerb(verb);
        		PPPhraseSpec complement = nlgFactory.createPrepositionPhrase("to");
        		complement.setComplement(sentence.owner1);
        		p.setComplement(complement);
        		p.setFeature(Feature.INTERROGATIVE_TYPE, InterrogativeType.HOW_MANY);
            }
            //p.setFeature(Feature.PERFECT, true);
            String output2 = realiser.realiseSentence(p); // Realiser created earlier.
            System.out.println(output2);
     
        }
        numLength = numLength/normaliser;
        System.out.println("avgLength = " + avgLength);
        System.out.println("avgDigitLength = " + numLength);
        System.out.println(schemas);
    	/*String schemaGrammar = "";
    	try {
 			String sCurrentLine;
 			br = new BufferedReader(new FileReader("changeOut"));
 			while ((sCurrentLine = br.readLine()) != null) {
 				schemaGrammar = schemaGrammar + "\n" + sCurrentLine;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
    	RiGrammar grammar = new RiGrammar(schemaGrammar);
        String result = grammar.expandFrom("<start>");
        
        System.out.println(result);*/
              // System.out.println(output);
    }
    
}
