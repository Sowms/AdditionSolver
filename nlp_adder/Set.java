package nlp_adder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

public class Set {
	
	String name;
	String cardinality;
	String compute = "";
	HashMap<String,Set> components; 
	static final Set Empty = new Set();
	public Set() {
		name = "\u03D5";
		cardinality = "0";
		components = new HashMap<>();
		components.put(name,Empty);
		compute = name;
	}
	public boolean isEmpty() {
		return this.name.equals(Empty.name);
	}
	public Set(String name, String cardinality) {
		this.name = name;
		this.cardinality = cardinality;
		compute = name;
	}
	// because we're dealing with disjoint sets, A intersection B = 0
	public static Set union(Set a, Set b) {
		Set ans = new Set();
		ans.name = a.name + "U" + b.name;
		ans.cardinality = a.cardinality + "+" + b.cardinality;
		ans.components.put(a.name,a);
		ans.components.put(b.name,b);
		ans.compute = "+"+a.compute+b.compute;
		return ans;
	}
	public Set unknownComponent() {
		Iterator<Entry<String, Set>> it = components.entrySet().iterator(); 
		while (it.hasNext()) {
			Set s = it.next().getValue();
			if (s.cardinality.equals("x"))
				return s;
		}
		return Empty;
	}
	public static Set difference(Set a, Set b) {
		Set ans = new Set();
		ans.name = a.name + "-" + b.name;
		ans.cardinality = a.cardinality + "-" + b.cardinality;
		ans.components.put(a.name,a);
		ans.components.put(b.name,b);
		ans.compute = "-"+a.compute+b.compute;
		return ans;
	}
	private boolean isOperand(char check){
		return check != '-' && check != 'U' && check != '+';
	}
	public void computeCardinality() {
		ArrayList<Set> operandStack = new ArrayList<>();
		System.out.println(compute);
		for (int i = compute.length() - 1; i >=0; i--) {
			if (isOperand(compute.charAt(i))) {
				char operand = compute.charAt(i);
				operandStack.add(0,components.get(operand+""));
			}
			else {
				System.out.println(operandStack);
				Set operand1 = operandStack.remove(0);
				Set operand2 = operandStack.remove(0);
				System.out.println(operandStack);
				Set ans = new Set();
				char operator = compute.charAt(i);
				System.out.println(operand1.name+"|"+operand2.name+operand2.cardinality);
				if (operator == '-')
					ans = difference(operand1,operand2);
				else
					ans = union(operand1,operand2);
				System.out.println(ans.cardinality);
				operandStack.add(0,ans);
			}
		}
		System.out.println(operandStack.size());
		if (!isOperand(compute.charAt(0)))
			cardinality = operandStack.get(0).cardinality;
	}
}
