package nlp_adder;

public class Set {
	
	String name;
	String entity;
	int cardinality;
	final Set Empty = new Set();
	public Set() {
		name = "";
		entity = "";
		cardinality = 0;
	}
	public Set(String name, String entity, int cardinality) {
		this.name = name;
		this.entity = entity;
		this.cardinality = cardinality;
	}
	public Set union(Set a, Set b) {
		Set ans = new Set();
		ans.name = "("+a.name+") U ("+b.name+")";
		ans.cardinality = a.cardinality + b.cardinality;
		return ans;
	}
	public Set difference(Set a, Set b) {
		Set ans = new Set();
		ans.name = "("+a.name+") - ("+b.name+")";
		ans.cardinality = a.cardinality - b.cardinality;
		return ans;
	}

}
