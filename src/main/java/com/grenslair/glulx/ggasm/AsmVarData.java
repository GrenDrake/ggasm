package com.grenslair.glulx.ggasm;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class AsmVarData extends AsmLine {

	private int itemCount;
	private ArrayList<Operand> items;

	/**
	 * Create a variable sized data segment
	 */
    public AsmVarData() {
        items = new ArrayList<Operand>();
        itemCount = -1;
    }
    /**
     * Create a fixed size data segment/
     * @param size  the size of the data segment in words (4 bytes)
     */
    public AsmVarData(int size) {
        items = new ArrayList<Operand>();
        itemCount = size;
    }

    /**
     * Add a new item to the data segment. If this is a fixed size data 
     * segment and the addition would take us over the fixed size, throw an AsmException. 
     * @param newItem  the item to be added to the data segment
     */
	public void addItem(Operand newItem) throws AsmException {
		if (newItem != null) {
		    if (itemCount > 0 && items.size() >= itemCount) {
		        throw new AsmException(getSource() + ": Too many items for wordsFixed; only " + itemCount + " expected."); 
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

	/**
	 * Get the size of this data segment in bytes. If a size has been provided, it is that size, otherwise it is the number of items * 4 bytes.
	 * @return the number of bytes required for this object
	 */
	@Override
	public int getSize() {
		if (itemCount <= 0) {
			return items.size() * 4;
		}
		return itemCount * 4;
	}

	@Override
    public String toString() {
	    StringBuilder sb = new StringBuilder();
	    sb.append("[VARDATA:");
	    for (Operand item : items) {
            sb.append(" ");
	        sb.append(item);
	    }
	    for (int i = items.size(); i < itemCount; ++i) {
	        sb.append(" 0");
	    }
	    sb.append("]");
	    return sb.toString();
    }

}
