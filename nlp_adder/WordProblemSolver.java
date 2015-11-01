package nlp_adder;

import java.util.Properties;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class WordProblemSolver {
	
	public static void main(String[] main) {
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner,parse,dcoref");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		solveWordProblems("In Shannon 's apartment complex , 0.16666666666666666 the apartments are one-bedroom apartments and 0.3333333333333333 are two-bedroom apartments . What fraction of the apartments are either 1 - or two-bedroom apartments ? ", pipeline);
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
