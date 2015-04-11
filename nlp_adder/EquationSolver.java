package nlp_adder;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;


public class EquationSolver {
	public static String answer = "";
	public static String getSolution(String input) {
		//http://stackoverflow.com/questions/3422673/evaluating-a-math-expression-given-in-string-form
		try {
			ScriptEngineManager mgr = new ScriptEngineManager();
			ScriptEngine engine = mgr.getEngineByName("JavaScript");
			if (!input.contains("x")) {
				answer = "" + engine.eval(input);
			} else {
				String[] components = input.split("=");
				String lhs = components[0];
				String rhs = components[1];
				String process = "";
				boolean isNegative = false;
				if (lhs.contains("x")) 
					process = lhs;
				else
					process = rhs;
				int xpos = lhs.indexOf('x');
				if (xpos > 0 && lhs.charAt(xpos - 1) == '-')
					isNegative = true;
				String newProcess = process.replaceAll("[\\+-]*x", "");
				double lhsValue, rhsValue, ans;
				if (process.equals(lhs)) {
					lhsValue = (double) engine.eval(newProcess);
				    rhsValue = (double) engine.eval(rhs);
				    ans = rhsValue - lhsValue;
				} else {
					rhsValue = (double) engine.eval(process);
				    lhsValue = (double) engine.eval(lhs);
				    ans = lhsValue - lhsValue;
				}
				if (isNegative)
					ans = -ans;
				answer = "" + ans;
			}
		} catch (ScriptException e) {
		e.printStackTrace();
		}
		return answer;
	}
}
