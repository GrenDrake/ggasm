package com.grenslair.glulx.ggasm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class StringTable {

	private int nextId;
	private HashMap<String,String> table;
	private HashMap<String,ArrayList<String>> alias;


	public StringTable() {
		table = new HashMap<String,String>();
		alias = new HashMap<String,ArrayList<String>>();
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

	public void addString(String label, String text) throws AsmException {
		String realLabel = addString(text);

		if (!alias.containsKey(realLabel)) {
			alias.put(realLabel,  new ArrayList<String>());
		}
		alias.get(realLabel).add(label);
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
	public String dump() {
		StringBuilder sb = new StringBuilder();
		for (String key : table.keySet()) {
			sb.append(table.get(key));
			sb.append(": ");
			String outStr;
			if (key.length() > 10) {
				outStr = key.substring(0,10) + "...";
			} else {
				outStr = key;
			}
			outStr = outStr.replaceAll("[\n\r]", "_");
			sb.append(outStr);
			sb.append("\n");
		}
		sb.append("\n");
		for (String key : table.keySet()) {
			String label = table.get(key);
			if (alias.containsKey(label)) {
				sb.append(label);
				sb.append(":");
				ArrayList<String> al = alias.get(label);
				for (String s : al) {
					sb.append(" ");
					sb.append(s);
				}
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	public void toCode(List<AsmLine> code) {
		for (String key : table.keySet()) {
			String label = table.get(key);
			if (alias.containsKey(label)) {
				ArrayList<String> al = alias.get(label);
				for (String s : al) {
					code.add(new AsmLabel(s, AsmLabel.Type.String));
				}
			}
			code.add(new AsmLabel(table.get(key),AsmLabel.Type.String));
			code.add(new AsmData(key, AsmData.StringType.Automatic));
		}
	}
}
