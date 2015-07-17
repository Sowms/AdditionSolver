package nlp_adder;

import java.util.Properties;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class WordProblemSolver {
	
	public static void main(String[] main) {
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner,parse,dcoref");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		solveWordProblems(" Logan recorded the snowfall every day during a snowstorm . He recorded 0.3333333333333333 centimeter on Wednesday ,0.3333333333333333 centimeter on Thursday , and 0.2222222222222222 centimeter on Friday . How many total centimeters of snow did Logan record ?", pipeline);
	}

	public static String solveWordProblems(String problem, StanfordCoreNLP pipeline) {
		String simplifiedProblem = Parser.parse(problem, pipeline);
		System.out.println("ha"+simplifiedProblem);
	    LinguisticInfo extractedInformation = (new SentencesAnalyzer()).extract(simplifiedProblem, pipeline);
	    KnowledgeRepresenter.clear();
	    KnowledgeRepresenter.represent(extractedInformation, simplifiedProblem);
	    KnowledgeRepresenter.solve();
	    System.out.println(KnowledgeRepresenter.finalAns);
	    return KnowledgeRepresenter.finalAns;
	}
}
