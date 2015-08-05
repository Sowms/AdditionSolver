package nlp_adder;

//import java.io.BufferedReader;
//import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
//import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class DatasetTest {
	public static boolean checkAns(String sysAns, String ans) {
		if (sysAns.contains(ans))
			return true;
		Pattern wordPattern = Pattern.compile("\\d+\\.\\d+|\\d+");
		Matcher matcher = wordPattern.matcher(sysAns);
		if(matcher.find()) {
			double num1 = Math.round(Double.parseDouble(matcher.group()));
			double num2 = Math.round(Double.parseDouble(ans));
			return (num1 == num2);
		}
		return false;
	}
	public static void main(String[] args) {
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		int counter = 400;
		try {
			builder = builderFactory.newDocumentBuilder();
			Document document = builder.parse(new FileInputStream("TN.xml"));
			Element rootElement = document.getDocumentElement();
			NodeList nodes = rootElement.getElementsByTagName("Worksheet");
			Properties props = new Properties();
		    props.put("annotators", "tokenize, ssplit, pos, lemma, ner,parse,dcoref");
		    BufferedWriter br = new BufferedWriter(new FileWriter("tnoutput2"));
		    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		    int total = 0, count = 0, nocount = 0;
			for (int i = 0; i < nodes.getLength(); i++){
				Node node = nodes.item(i);
				if (node instanceof Element){
			    //a child element to process
					Element worksheet = (Element) node;
					Element table = (Element) worksheet.getChildNodes().item(1);
					NodeList tableChildren = table.getElementsByTagName("Row");
					for (int j = 0; j < tableChildren.getLength(); j++){
						Node tableChild = tableChildren.item(j);
						if (tableChild instanceof Element) {
							String ques = (tableChild.getChildNodes().item(1).getChildNodes().item(0).getTextContent());
							if (ques.equals("Question"))
								continue;
							total ++;
							//System.out.println(ques);
							String ans = (tableChild.getChildNodes().item(3).getChildNodes().item(0).getTextContent());
							//System.err.println(ans);
							String sysAns = "";
							try{
							sysAns = WordProblemSolver.solveWordProblems(ques,pipeline);
							}catch(Exception e) {
							}
							if (checkAns(sysAns,ans))
								count++;
							else{
								nocount++;
								System.err.println(sysAns+"|"+ans);
								br.write(nocount + ". " + ques);
								br.write("\n"+sysAns+"|"+ans+"\n");
							}
						}
					}
					if (total >= counter) {
						break;
					}
				}
			}
			br.close();
			System.out.println(count+"|"+total);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();  
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NullPointerException e) {
		// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
