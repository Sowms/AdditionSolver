package nlp_adder;
import java.util.Properties;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import nlp_adder.WordProblemSolver;


public class Solve {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String input = "John has 3 apples. He gave 1 apple to Mary. How many does John have now?";
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner,parse,dcoref");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		System.out.println(WordProblemSolver.solveWordProblems(input, pipeline));

	}

}
