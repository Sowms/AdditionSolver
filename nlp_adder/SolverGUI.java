package nlp_adder;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.Properties;

import javax.swing.*;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

class ProblemFrame extends JFrame {

	private static final long serialVersionUID = 1L;
	private JTextArea questionBox = new JTextArea();
	private JTextField answerBox = new JTextField();
	private JLabel questionLabel = new JLabel("Question");
	private JLabel answerLabel = new JLabel("Answer");
	private JButton solveButton = new JButton("Solve");
	private StanfordCoreNLP pipeline;
	
	public ProblemFrame(StanfordCoreNLP pipeline) {
		try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
          
        }
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
		setSize(1000,400);
		setLocation(150,200);
		setTitle("Word Problem Solver");
		initComponents();
		this.pipeline = pipeline;
	}
	private void initComponents() {
		
		
		GridLayout layout = new GridLayout(7,3);
		setLayout(layout);

		add(new JPanel());
		JPanel questionLabelPanel = new JPanel();
		questionLabelPanel.add(questionLabel);
		add(questionLabelPanel);
		add(new JPanel());

		add(new JPanel());
		questionBox.setLineWrap(true);
		JScrollPane scroll = new JScrollPane(questionBox);
	    scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
	    scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
	    add(scroll);
		add(new JPanel());

		add(new JPanel());
		JPanel answerLabelPanel = new JPanel();
		answerLabelPanel.add(answerLabel);
		add(answerLabelPanel);
		add(new JPanel());

		add(new JPanel());
		add(answerBox);
		add(new JPanel());
		
		add(new JPanel());
		add(new JPanel());
		add(new JPanel());
		
		add(new JPanel());
		add(solveButton);
		add(new JPanel());
		
		add(new JPanel());
		add(new JPanel());
		add(new JPanel());
		
		solveButton.addActionListener(new java.awt.event.ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String problem = questionBox.getText(); 		
				answerBox.setText(WordProblemSolver.solveWordProblems(problem, pipeline));
			}
			
		});
	}
	
}
public class SolverGUI {
	
	public static void main(String[] args) {
		
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner,parse,dcoref");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		new ProblemFrame(pipeline);
	}

}
