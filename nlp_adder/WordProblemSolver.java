package nlp_adder;

import java.util.Properties;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class WordProblemSolver {
	
	public static void main(String[] main) {
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner,parse,dcoref");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		solveWordProblems("While making desserts for a bake sale , Victor used 0.625 scoop of brown sugar as well as 0.25 scoop of white sugar . How much more brown sugar did Victor use ? ", pipeline);
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
