package com.grenslair.glulx.ggasm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * This class represents an entire glulx gamefile.
 */
public class ObjectFile {

	private List<AsmLine> romArea;
	private List<AsmLine> instructions;
	private HashMap<String,AsmLabel> symbols;
	private ByteBuffer bytecode;
	private int codeSize;
	private int romEnd;
	private boolean toROM;
	private HashMap<String,Constant> constants;
	private int stackSize;
	private StringTable strings;

	private class Constant {
		public int line;
		public int value;
		public Constant(int line, int value) {
			this.line = line;
			this.value = value;
		}
		@Override
		public String toString() {
			return "[line:"+line+" value:"+value+"]";
		}
	}

	/**
	 * Round a value up to the nearest multiple of 256.
	 * @param value The value to round up
	 * @return the result after rounding up
	 */
	static public int roundUp(int value) {
		while (value % 256 != 0) {
			++value;
		}
		return value;
	}


	/**
	 * Create a new object file.
	 */
	public ObjectFile() {
		instructions = new ArrayList<AsmLine>();
		romArea = new ArrayList<AsmLine>();
		symbols = new HashMap<String,AsmLabel>();
		constants = new HashMap<String,Constant>();
		strings = new StringTable();
		stackSize = 2048;

		addLine(new AsmLabel("_startOfRAM", AsmLabel.Type.BuiltIn));
	}
	/**
	 * Add a new asm line to the game file. This will add to either writable
	 * memory or to ROM according to the write area flag.
	 * @param i The AsmLine to add to this game file.
	 */
	public void addLine(AsmLine i) {
		if (toROM) {
			romArea.add(i);
		} else {
			instructions.add(i);
		}
		i.setObjectFile(this);
	}

	/**
	 * Toggle writing to the game file's ROM area
	 * @param toROM true if new lines should be added to ROM, false otherwise.
	 */
	public void addToRom(boolean toROM) {
		this.toROM = toROM;
	}
	/**
	 * Set the stack size for this game file.
	 * @param newSize The new stack size
	 */
	public void setStackSize(int newSize) {
		stackSize = newSize;
	}
	/**
	 * Return the current size of the game file in code. This is only valid after
	 * positionCode() has been called and only accurate once the byte code has been built.
	 * @return the current size of this game file in bytes.
	 */
	public int getCodeSize() {
		return codeSize;
	}
	/**
	 * Find the code position of all asm lines in the game file
	 */
	public void positionCode() {
		romArea.add(0, new AsmData(36));
		strings.toCode(romArea);

		int position = 0;

		for (AsmLine i : romArea) {
			i.setPosition(position);

			if (i instanceof AsmLabel) {
				AsmLabel l = (AsmLabel)i;

				if (symbols.containsKey(l.getName())) {
					System.err.println("Label \"" + l.getName() + "\" already defined. (Duplicate at " + l.getSourceFile()+":"+l.getSourceLine() + ")");
				} else {
					symbols.put(l.getName(), l);
				}
			}

			position += i.getSize();
		}

		romEnd = roundUp(position);
		romArea.add(new AsmData(romEnd - position));
		position = romEnd;

		for (AsmLine i : instructions) {
			i.setPosition(position);

			if (i instanceof AsmLabel) {
				AsmLabel l = (AsmLabel)i;

				if (symbols.containsKey(l.getName())) {
					System.err.println("Label \"" + l.getName() + "\" already defined. (Duplicate at " + l.getSourceFile()+":"+l.getSourceLine() + ")");
				} else {
					symbols.put(l.getName(), l);
				}
			}

			position += i.getSize();
		}
		codeSize = position;
	}

	public String addString(String text) {
		return strings.addString(text);
	}
	public void addString(String label, String text) throws AsmException {
		strings.addString(label, text);
	}

	public void addConstant(String name, int value, int line) {
		constants.put(name, new Constant(line, value));
	}
	public boolean isSymbolKnown(String symbolName) {
		if (constants.containsKey(symbolName)) {
			return true;
		} else if (symbols.containsKey(symbolName)) {
			return true;
		}
		return false;
	}
	public int getSymbolValue(String symbolName) {
		if (constants.containsKey(symbolName)) {
			return constants.get(symbolName).value;
		} else if (symbols.containsKey(symbolName)) {
			return symbols.get(symbolName).getPosition();
		}
		return 0;
	}
	public int getConstantValue(String constantName) {
		if (constants.containsKey(constantName)) {
			return constants.get(constantName).value;
		}
		return 0;
	}
	public boolean isConstantDefined(String constantName) {
		if (constants.containsKey(constantName)) {
			return true;
		}
		return false;
	}
	public void replaceSymbols() throws AsmException {
		for (AsmLine line : instructions) {
			line.replaceSymbols();
		}
	}

	/**
	 * Do the build process for this game file.
	 * @return true if the file was built successfully, false otherwise
	 */
	public boolean doBuild() {
		try {
			positionCode();
			replaceSymbols();
			buildByteCode();
		} catch (AsmException e) {
			System.err.println("ERROR " + e.getMessage());
			return false;
		}
		return true;
	}

	public void buildByteCode() {
		if (!symbols.containsKey("main")) {
			System.err.println("Could not find \"main\" symbol.");
			return;
		}

		// setup bytecode space
		while (codeSize % 256 != 0) {
			++codeSize;
		}
		bytecode = ByteBuffer.allocate(codeSize);
		bytecode.order(ByteOrder.BIG_ENDIAN);

		// write ROM data
		for (AsmLine line : romArea) {
			line.buildByteCode(bytecode);
		}

		////////////////////////////////////////////////////////////////////////
		// GLULX header ////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////////////////
		//  0 Glulx Magic Number (47 6C 75 6C or "Glul")
		bytecode.putInt( 0, 0x476C756C);
		// 04 Glulx version number: The upper 16 bits stores the major version
		//    number; the next 8 bits stores the minor version number; the low 8
		//    bits stores an even more minor version number, if any.
		//    GGASM builds version 3.1.2
		bytecode.putInt( 4, 0x00030102);
		// 08 RAMSTART: The first address which the program can write to.
		bytecode.putInt( 8, romEnd);
		// 12 EXTSTART: The end of the game-file's stored initial memory (and
		//    therefore the length of the game file.)
		bytecode.putInt(12, codeSize);
		// 16 ENDMEM: The end of the program's memory map.
		bytecode.putInt(16, codeSize);
		// 20 Stack size: The size of the stack needed by the program.
		bytecode.putInt(20, stackSize);
		// 24 Address of function to execute: Execution commences by calling
		//    this function. (this is the position of the main function)
		bytecode.putInt(24, symbols.get("main").getPosition());
		// 28 Address of string-decoding table: This table is used to decode
		//    compressed strings. See section 1.6.1.3, "Compressed strings".
		//    This may be zero, indicating that no compressed strings are to be
		//    decoded.
		bytecode.putInt(28, 0x00000000);
		// 32 Checksum: A simple sum of the entire initial contents of memory,
		//    considered as an array of big-endian 32-bit integers. The checksum
		//    should be computed with this field set to zero.
		bytecode.putInt(32, 0x00000000);
		////////////////////////////////////////////////////////////////////////
		// End Header //////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////////////////

		// write game file content
		for (AsmLine line : instructions) {
			line.buildByteCode(bytecode);
		}

		// pad game file
		while (bytecode.position() % 256 != 0) {
			bytecode.put((byte)0);
		}

		// calculate and write checksum
		int checksum = 0;
		for (int i = 0; i < codeSize; i += 4) {
			checksum += bytecode.getInt(i);
		}
		bytecode.putInt(32, checksum);
	}
	public void writeByteCodeToFile(String filename) {
		if (bytecode == null) {
			return;
		}

		try {
			File out = new File(filename);
			FileChannel channel = new FileOutputStream(out).getChannel();
			bytecode.flip();
			channel.write(bytecode);
			channel.close();
		} catch (FileNotFoundException e) {
			System.out.println(e);
		} catch (IOException e) {
			System.out.println(e);
		}
	}

	/**
	 * Produce a code dump of the content of this game file to a string. This
	 * is for debugging purposes and is not likely to be otherwise useful.
	 * @return The string containing the code dump.
	 */
	public String dumpCode() {
		StringBuilder sb = new StringBuilder();
		for (AsmLine line : instructions) {
			sb.append(line.getPosition() + "/" + line.getSize() + ": " + line+"\n");
		}
		return sb.toString();
	}
	/**
	 * Dump the current symbol table for this game file to a string.
	 * @return The string containing the symbol table.
	 */
	public String dumpSymbols() {
		StringBuilder sb = new StringBuilder();
		Formatter f = new Formatter(sb, Locale.US);

		String[] keys = symbols.keySet().toArray(new String[symbols.keySet().size()]);
		Arrays.sort(keys);
		for (String key : keys) {
			f.format("%08x  %-16s  %s\n", symbols.get(key).getPosition(), symbols.get(key).getType(), key);
		}
		f.close();
		return sb.toString();
	}
	/**
	 * Dump the current constant table for this game file to a string.
	 * @return The string containing the constant table.
	 */
	public String dumpConstants() {
		StringBuilder sb = new StringBuilder();
		Formatter f = new Formatter(sb, Locale.US);

		String[] keys = constants.keySet().toArray(new String[constants.keySet().size()]);
		Arrays.sort(keys);
		for (String key : keys) {
			f.format("%-32s  %s\n", key, constants.get(key).value);
		}
		f.close();
		return sb.toString();
	}
	/**
	 * Dump the current string table for this game file to a string.
	 * @return the string containig the string table.
	 */
	public String dumpStrings() {
		return strings.dump();
	}

}
