package com.grenslair.glulx.ggasm;

import java.util.HashMap;
import java.util.List;

public class StringTable {

	private int nextId;
	private HashMap<String,String> table;


	public StringTable() {
		table = new HashMap<String,String>();
	}


	public String addString(String text) {
		if (table.containsKey(text)) {
			return table.get(text);
		}
		String label = "_string"+nextId;
		++nextId;
		table.put(text, label);
		return label;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("["+table.size());
		for (String key : table.keySet()) {
			sb.append(";"+table.get(key)+"="+key);
		}
		sb.append("]");
		return sb.toString();
	}

	public void toCode(List<AsmLine> code) {
		for (String key : table.keySet()) {
			code.add(new AsmLabel(table.get(key),AsmLabel.Type.String));
			code.add(new AsmData(key));
		}
	}
}
