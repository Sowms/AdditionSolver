package nlp_adder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class Driver {

		public static void main (String[] args) {
			BufferedReader br = null;
			Properties props = new Properties();
		    props.put("annotators", "tokenize, ssplit, pos, lemma, ner,parse,dcoref");
		    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	 		try {
	 			String sCurrentLine;
	 			br = new BufferedReader(new FileReader("questions"));
	 			while ((sCurrentLine = br.readLine()) != null) {
					String ques = sCurrentLine;
					if(ques.isEmpty())
						break;
				WordProblemSolver.solveWordProblems(ques,pipeline);
					System.err.println("-----------------------");
				}
	 		} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (br != null)br.close();
				} catch (IOException ex) {
					ex.printStackTrace();
			}
		}
	}
}
