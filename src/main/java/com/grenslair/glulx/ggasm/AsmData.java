package com.grenslair.glulx.ggasm;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class AsmData extends AsmLine {
	private byte[] data;

	public enum StringType {
		Automatic,
		Basic,
		Unicode
	}

	/**
        Create a data segment of the specified length filled with zeros.
        @param length The size (in bytes) to make this data segment.
	 */
	public AsmData(int length) {
		super();
		data = new byte[length];
	}
	/**
        Create a data segment containing a single four-byte value.
        @param value   the value to store in the data segment
        @param isWord  flag value to overload function compared to AsmData(int)
	 */
	public AsmData(int value, boolean isWord) {
		super();
		data = new byte[4];
		ByteBuffer.wrap(data).putInt(value);
	}
	/**
        Create a data segment containing a string value. This will automatically
        decide whether to create a unicode or regular string.
        @param content  the string to store in the data segment
        @param type     the mode to save the string with
	 */
	public AsmData(String content, StringType type) {
		super();

		boolean needsUnicode = false;
		ArrayList<Integer> list = new ArrayList<Integer>();
		int length = content.length();
		for (int i = 0; i < length; ) {
			int codePoint = content.codePointAt(i);
			list.add(codePoint);
			if (codePoint > 0x7F) {
				needsUnicode = true;
			}
			i += Character.charCount(codePoint);
		}

		if (needsUnicode) {
			data = new byte[(content.length()+2)*4];
			ByteBuffer datab = ByteBuffer.wrap(data);
			datab.putInt(0xE2000000);
			for (int i : list) {
				datab.putInt(i);
			}
		} else {
			data = new byte[content.length()+2];
			data[0] = (byte)0xE0;
			int pos = 1;
			for (int i : list) {
				data[pos] = (byte)i;
				++pos;
			}
		}
	}
	/**
        Create a data segment based on an existing byte array.
        @param content  the byte array to create a data segment for
	 */
	public AsmData(byte[] content) {
		data = content;
	}
	public AsmData(int length, byte[] content) {
		if (length < content.length) {
			length = content.length;
		}
		data = Arrays.copyOf(content, length);
	}
	@Override
	public void buildByteCode(ByteBuffer code) {
		code.put(data);
	}
	@Override
	public int getSize() {
		return data.length;
	}
	@Override
	public String toString() {
		return "[DATA:"+Arrays.toString(data)+"]";
	}

}
