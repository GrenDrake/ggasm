package com.grenslair.glulx.ggasm;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class AsmVarData extends AsmLine {

	private Operand itemCount;
	private ArrayList<Operand> items;

	public AsmVarData() {
		items = new ArrayList<Operand>();
	}

	public void addItem(Operand newItem) {
		if (newItem != null) {
			items.add(newItem);
		}
	}

	@Override
	public void buildByteCode(ByteBuffer code) {
	    for (Operand item : items) {
	        if (item.isSymbol()) {
	            int v = getObjectFile().getSymbolValue(item.getSymbol());
	            code.putInt(v);
	        } else {
	            code.putInt(item.getValue());
	        }
	    }
	}

	@Override
	public int getSize() {
		if (itemCount == null) {
			return items.size() * 4;
		}
		return 0;
	}

	@Override
    public String toString() {
        return "AsmVarData [itemCount=" + itemCount + ", items=" + items + ", getPosition()=" + getPosition()
                + ", getSourceLine()=" + getSourceLine() + ", getSourceFile()=" + getSourceFile() + "]";
    }

}
