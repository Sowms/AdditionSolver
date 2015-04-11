package nlp_adder;

import java.util.Properties;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class WordProblemSolver {
	
	public static void main(String[] main) {
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner,parse,dcoref");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		solveWordProblems("Veronica owns 12 cats and 17 dogs. Maurice had 16 dogs. He sells 3 dogs to Veronica. How many cats are there in all if Maurice owns 1 cat?", pipeline);
	}

	public static String solveWordProblems(String problem, StanfordCoreNLP pipeline) {
		String simplifiedProblem = Parser.parse(problem, pipeline);
		System.out.println(simplifiedProblem);
	    LinguisticInfo extractedInformation = (new SentencesAnalyzer()).extract(simplifiedProblem, pipeline);
	    KnowledgeRepresenter.clear();
	    KnowledgeRepresenter.represent(extractedInformation);
	    KnowledgeRepresenter.solve();
	    System.out.println(KnowledgeRepresenter.finalAns);
	    return KnowledgeRepresenter.finalAns;
	}
}
