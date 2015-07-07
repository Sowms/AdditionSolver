package nlp_adder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;

public class KRDriver {

		public static void main (String[] args) {
			BufferedReader br = null;
			ArrayList<LinguisticStep> steps = new ArrayList<LinguisticStep>();
			LinguisticStep s = new LinguisticStep();
			int counter = 0;
			try {
	 			String sCurrentLine;
	 			br = new BufferedReader(new FileReader("KR"));
	 			while ((sCurrentLine = br.readLine()) != null) {
					String ques = sCurrentLine;
					if(ques.isEmpty())
						break;
					if (ques.contains("owner"))
						continue;
					if (ques.contains("---")) {
						LinguisticInfo extractedInformation = new LinguisticInfo();
						extractedInformation.sentences = steps;
						extractedInformation.entities = new LinkedHashSet<String>();
						KnowledgeRepresenter.clear();
						KnowledgeRepresenter.represent(extractedInformation,"");
						KnowledgeRepresenter.solve();
						System.out.println(KnowledgeRepresenter.finalAns);
						System.err.println("-----------------------");
						steps = new ArrayList<LinguisticStep>();
						counter++;
						continue;
					}
					String[] quantities = ques.split("\t");
					quantities = replaceBlanks(quantities);
					s = new LinguisticStep();
					s.owner1 = quantities[0];
					s.owner2 = quantities[1];
					s.isQuestion = Boolean.parseBoolean(quantities[2]);
					s.tense = quantities[3];
					s.entityName = quantities[4];
					s.entityValue = quantities[5];
					s.procedureName = quantities[6];
					s.keyword = quantities[7];
					s.nounQual = quantities[8];
					s.verbQual = quantities[9];
					s.aggregator = Boolean.parseBoolean(quantities[10]);
					s.comparator = Boolean.parseBoolean(quantities[11]);
					s.difference = Boolean.parseBoolean(quantities[12]);
					steps.add(s);
				}
	 			System.out.println(counter);
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

		private static String[] replaceBlanks(String[] quantities) {
			for (int i = 0; i < quantities.length; i++)
				if (quantities[i].equals("blank"))
					quantities[i] = "";
			return quantities;
		}
}
