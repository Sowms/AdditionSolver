package nlp_adder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class TextDatasetTester {
	public static boolean checkAns(String sysAns, String ans) {
		if (sysAns.contains(ans))
			return true;
		Pattern wordPattern = Pattern.compile("\\d+\\.\\d+|\\d+");
		Matcher matcher = wordPattern.matcher(sysAns);
		if(matcher.find()) {
			double num1 = Math.round(Double.parseDouble(matcher.group())*100)/100.0;
			double num2 = Math.round(Double.parseDouble(ans)*100)/100.0;
			System.out.println(num1+"|"+num2);
			return (num1 == num2);
		}
		return false;
	}
	public static void main(String[] args) {
		BufferedReader br1 = null, br2 = null;
		BufferedWriter br = null;
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner,parse,dcoref");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    int count = 0, total = 0;
		try {
 			String sCurrentLine;
 			br1 = new BufferedReader(new FileReader("q2.txt"));
 			br2 = new BufferedReader(new FileReader("ans2.txt"));
 			br = new BufferedWriter(new FileWriter("output5"));
 			while ((sCurrentLine = br1.readLine()) != null) {
 				String sysAns = "", ques = sCurrentLine, ans = br2.readLine();
				try{
				sysAns = WordProblemSolver.solveWordProblems(ques,pipeline);
				}catch(Exception e) {
				}
				if (checkAns(sysAns,ans))
					count++;
				else
					br.write(ques+"\n"+sysAns+"|"+ans+"\n");
				total++;
			}
 			System.out.println(count+"|"+total);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br1 != null)
					br1.close();
				if (br2 != null)
					br2.close();
				br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
}
