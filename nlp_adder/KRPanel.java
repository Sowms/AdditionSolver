package nlp_adder;

import java.awt.Graphics;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.swing.JLabel;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class KRPanel extends JPanel {
	HashMap<String,Owner> story = new HashMap<String,Owner>();
	public KRPanel(HashMap<String,Owner> story) {
		this.story = story;
	}
	public void paintComponent(Graphics g) {  
		GridLayout layout = new GridLayout(0,5);
		this.setLayout(layout);
		this.add(new JPanel());
		this.add(new JPanel());
		this.add(new JLabel("t0"));
		this.add(new JLabel("t1"));
		this.add(new JLabel("t2"));
		Iterator<Entry<String, Owner>> storyIterator = story.entrySet().iterator();
		while (storyIterator.hasNext()) {
		     Owner owner = storyIterator.next().getValue();
		     this.add(new JLabel(owner.name));
		     Iterator<Entry<String, ArrayList<TimeStamp>>> verbStoryIterator = owner.situation.entrySet().iterator();
		     int counter = 0;
			 while (verbStoryIterator.hasNext()) {
				Entry<String, ArrayList<TimeStamp>> newPairs = verbStoryIterator.next();
				if (counter != 0)
					this.add(new JPanel());
				counter++;
				this.add(new JLabel(newPairs.getKey()));
				ArrayList<TimeStamp> verbStory = newPairs.getValue();
				int time = 0;
				for (TimeStamp currentTimeStamp : verbStory) {
					this.add(new JLabel(currentTimeStamp.value + " " +currentTimeStamp.name));
					time++;
				}
				for (int k = time; k<=2; k++) {
					this.add(new JPanel());
				}
			} 
		}
	}
}
