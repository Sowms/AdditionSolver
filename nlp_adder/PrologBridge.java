package nlp_adder;

import jpl.Compound;
import jpl.Query;
import jpl.Term;
import jpl.Variable;

public class PrologBridge {

	public static void main(String[] args) {
		Query q1 = new Query("consult('foo.pl')");
		System.out.println( "consult " + (q1.hasSolution() ? "succeeded" : "failed"));
		Query q2 = new Query("foo(a)");
		Boolean resp= q2.hasSolution();
		System.out.println("foo(a) is " + resp.toString());
		Query q3 = new Query("foo(c)");
		System.out.println("foo(c) is " + ( q3.hasSolution() ? "provable" : "not provable" ));
		Query q4 = new Query(new Compound("foo", new Term[] { new Variable("X")}));
		while (q4.hasMoreElements())
			System.out.println(q4.nextElement());
		}
}

