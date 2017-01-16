package com.grenslair.glulx.ggasm;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class AsmVarData extends AsmLine {

	private int itemCount;
	private ArrayList<Operand> items;

    public AsmVarData() {
        items = new ArrayList<Operand>();
        itemCount = -1;
    }
    public AsmVarData(int size) {
        items = new ArrayList<Operand>();
        itemCount = size;
    }

	public void addItem(Operand newItem) throws AsmException {
		if (newItem != null) {
		    if (itemCount > 0 && items.size() >= itemCount) {
		        throw new AsmException("Too many items for wordsFixed; only " + itemCount + " expected."); 
		    }
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
	    for (int i = items.size(); i < itemCount; ++i) {
	        code.putInt(0);
	    }
	}

	@Override
	public int getSize() {
		if (itemCount <= 0) {
			return items.size() * 4;
		}
		return itemCount * 4;
	}

	@Override
    public String toString() {
        return "AsmVarData [itemCount=" + itemCount + ", items=" + items + ", getPosition()=" + getPosition()
                + ", getSourceLine()=" + getSourceLine() + ", getSourceFile()=" + getSourceFile() + "]";
    }

}
