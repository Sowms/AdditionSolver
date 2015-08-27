package nlp_adder;

import java.util.Properties;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class WordProblemSolver {
	
	public static void main(String[] main) {
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner,parse,dcoref");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		solveWordProblems("Sara 's high school played 12 basketball games this year . The team won most of their games . They were defeated during 4 games . How many games did they win ? ", pipeline);
	}

	public static String solveWordProblems(String problem, StanfordCoreNLP pipeline) {
		String simplifiedProblem = Parser.parse(problem, pipeline);
		System.out.println("ha"+simplifiedProblem);
	    LinguisticInfo extractedInformation = (new SentencesAnalyzer()).extract(simplifiedProblem, pipeline);
	    extractedInformation = OrderSteps.order(extractedInformation);
	    KnowledgeRepresenter.clear();
	    KnowledgeRepresenter.represent(extractedInformation, simplifiedProblem);
	    KnowledgeRepresenter.solve();
	    System.out.println(KnowledgeRepresenter.finalAns.replace(".0 ", " "));
	    return KnowledgeRepresenter.finalAns.replace(".0 ", "");
	}
}
