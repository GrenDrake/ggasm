package com.grenslair.glulx.ggasm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Assemble {


	private Assemble() {
	}


	/**
	 * Parse one line from an assembly input file and return a list of tokens
	 * representing the content.
	 * @param line The input line to parse
	 * @return The resulting list of tokens.
	 */
	static public List<Token> parseLine(String line) throws AsmException {
		List<Token> parts = new ArrayList<Token>();
		if (line.isEmpty()) {
			return parts;
		}

		int pos = 0, start = 0, end;
		for (pos = 0; pos < line.length(); ++pos) {
			while (pos < line.length() && Character.isWhitespace(line.charAt(pos))) {
				++pos;
			}
			if (pos == line.length()) {
				return parts;
			}
			char here = line.charAt(pos);

			// end-of-line comment
			if (here == ';') {
				return parts;

				// parse strings
			} else if (here == '"') {
				++pos;
				start = pos;
				while (pos < line.length() && (line.charAt(pos) != '"' || line.charAt(pos-1) == '\\')) {
					++pos;
				}
				if (pos == line.length()) {
					throw new AsmException("Unterminated string.");
				}
				end = pos;
				String text = line.substring(start,end);
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < text.length(); ++i) {
					if (text.charAt(i) == '\\') {
						++i;
						switch(text.charAt(i)) {
						case '0':  // nothing
							break;
						case '\\': // backslash
						case '"':  // quote
							sb.append(text.charAt(i));
							break;
						case 'n':  // newline
							sb.append('\n');
							break;
						case 'r':  // return
							sb.append('\r');
							break;
						case 't':  // tab
							sb.append('\t');
							break;
						case 'x':  // character by hex code
							++i;
							if (i >= text.length() || !isHexDigit(text.charAt(i))) {
								throw new AsmException("Unexpected end of escape in \\xXX");
							}
							start = i;
							while (i < text.length() && isHexDigit(text.charAt(i))) {
								++i;
							}
							int v = Integer.parseInt(text.substring(start,i), 16);
							System.err.println(v);
							sb.appendCodePoint(v);
							--i;
							break;
						default:   // unknown
							throw new AsmException("Unknown character escape \\" + text.charAt(i));
						}
					} else {
						sb.append(text.charAt(i));
					}
				}

				parts.add(new Token(sb.toString(), Token.Type.String));

				// parse decimal and hex numbers
			} else if (Character.isDigit(line.charAt(pos))) {
				if (here == '0' && pos+2 < line.length() && line.charAt(pos+1) == 'x') {
					// hexadecimal number
					pos += 2;
					start = pos;
					while (pos < line.length() && isHexDigit(line.charAt(pos))) {
						++pos;
					}
					parts.add(new Token(Integer.parseInt(line.substring(start,pos), 16)));
				} else {
					start = pos;
					while (pos < line.length() && Character.isDigit(line.charAt(pos))) {
						++pos;
					}
					parts.add(new Token(Integer.parseInt(line.substring(start,pos), 10)));
				}

				// parse identifiers
			} else if (isIdentifier(here, true)) {
				start = pos;
				++pos;
				while (pos < line.length() && isIdentifier(line.charAt(pos), false)) {
					++pos;
				}
				if (pos != line.length() && line.charAt(pos) == ':') {
					++pos;
				}
				end = pos;
				String text = line.substring(start,end);
				parts.add(new Token(text, Token.Type.Identifier));

			} else {
				// unknown
				throw new AsmException("Unexpected " + line.charAt(pos));
			}
		}
		return parts;
	}
	/**
	 * Check to see if a character is valid for an identifier.
	 * @param what         the character to check
	 * @param initialChar  true if this is the first character of the
	 *                        (potential) identifier
	 * @return true if the character is valid, false otherwise
	 */
	static public boolean isIdentifier(int what, boolean initialChar) {
		if (initialChar) {
			return (Character.isAlphabetic(what) || what == '*' || what == '#' || what == '_');
		}
		return (Character.isAlphabetic(what) || Character.isDigit(what) || what == '_');
	}
	/**
	 * Check to see if a character is a valid hexadecimal digit.
	 * @param what  the character to check
	 * @return true if the character is a valid hexadecimal digit, false
	 *           otherwise
	 */
	static public boolean isHexDigit(int what) {
		if (Character.isDigit(what)) {
			return true;
		}
		int lower = Character.toLowerCase(what);
		if (lower >= 'a' && lower <= 'f') {
			return true;
		}
		return false;
	}


	/**
	 * Check to see if the tokens of an input line match what is expected and
	 * throw an error if not.
	 * @param parts A List of the actual tokens received
	 * @param matchLength Should we make sure the length matches?
	 * @param forWhat the name of the mnemonic this is for; displayed in error
	 *   messages
	 * @param types The types expected to be found
	 */
	static public void lineMatches(List<Token> parts, boolean matchLength, String forWhat, Token.Type ...types)
			throws AsmException {
		if (matchLength && parts.size() != types.length) {
			throw new AsmException("Bad operand count for " + forWhat +
					"expected " + types.length + ", but found " +
					parts.size() + ".");
		}
		for (int i = 0; i < parts.size() && i < types.length; ++i) {
			if (!parts.get(i).isType(types[i])) {
				throw new AsmException("Operand " + i + " is " +
						parts.get(i).getType() + ", but " +
						types[i] + " was expected for " +
						forWhat + ".");
			}
		}
	}

	/**
	 * Add the content of an assembly input file to the current gamefile.
	 * @param inputFile the name of the assembly file to add
	 * @return whether errors occurred during assembly
	 */
	static public boolean fromFile(ObjectFile asm, String inputFile) {
		List<String> lines;
		String filePath;
		try {
			Path path = Paths.get(inputFile);
			if (path.getParent() != null) {
				filePath = path.getParent().toString() + "/";
			} else {
				filePath = "./";
			}
			lines = Files.readAllLines(path, StandardCharsets.UTF_8);
		} catch (IOException e) {
			System.out.println(e);
			return true;
		}

		boolean foundErrors = false;
		int lineNo = 0;
		for (String line : lines) {
			++lineNo;

			try {
				List<Token> parts = parseLine(line.trim());
				if (parts.isEmpty()) {
					continue;
				}

				// check system configuration settings
				if (parts.get(0).equalTo("stackSize")) {
					asm.setStackSize(parts.get(1).getIntValue());
					continue;
				}

				if (parts.get(0).equalTo("toROM")) {
					asm.addToRom(true);
					continue;
				}
				if (parts.get(0).equalTo("endROM")) {
					asm.addToRom(false);
					continue;
				}

				// check to see if a label was indicated on this line
				if (parts.get(0).getStringValue().endsWith(":")) {
					String text = parts.get(0).getStringValue();
					asm.addLine(new AsmLabel(text.substring(0,text.length()-1)));
					parts.remove(0);
					if (parts.isEmpty()) {
						continue;
					}
				}

				// make sure we're starting with an identifier
				if (parts.get(0).getType() != Token.Type.Identifier) {
					throw new AsmException("Expected identifier at start of line, but found " + parts.get(0).getType());
				}
				String mnemonic = parts.get(0).getStringValue();
				parts.remove(0);

				// include another asm file
				if (mnemonic.equals("include")) {
					lineMatches(parts, true, mnemonic, Token.Type.String);
					String includedFile = filePath + parts.get(0).getStringValue();
					System.err.println("including \""+includedFile+"\" from \""+inputFile+"\".");
					fromFile(asm, includedFile);

					// define a constant symbol
				} else if (mnemonic.equals("constant")) {
					lineMatches(parts, true, mnemonic, Token.Type.Identifier, Token.Type.Integer);
					asm.addConstant(parts.get(0).getStringValue(), parts.get(1).getIntValue(), lineNo);

					// declare a new function
				} else if (mnemonic.equals("function")) {
					lineMatches(parts, true, mnemonic, Token.Type.Identifier, Token.Type.Integer);
					AsmLabel label = new AsmLabel(parts.get(0).getStringValue(), AsmLabel.Type.LocalFunction, parts.get(1).getIntValue());
					label.setSource(inputFile, lineNo);
					asm.addLine(label);

					// declare a new stack-based function
				} else if (mnemonic.equals("stkfunction")) {
					lineMatches(parts, true, mnemonic, Token.Type.Identifier, Token.Type.Integer);
					AsmLabel label = new AsmLabel(parts.get(0).getStringValue(), AsmLabel.Type.StackFunction, parts.get(1).getIntValue());
					label.setSource(inputFile, lineNo);
					asm.addLine(label);


					// perform a glk shortcut call
					// format: _glk function args+ dest
				} else if (mnemonic.equals("_glk")) {
					// make sure we don't have a string
					if (parts.get(0).isType(Token.Type.String)) {
						throw new AsmException("cannot pass string as identifier of _glk call");
					}

					// add instructions to push args onto stack
					for (int i = parts.size() - 2; i > 0; --i) {
						AsmInstruction ai = new AsmInstruction(Mnemonic.list.get("copy"));
						ai.addOperand(new Operand(parts.get(i), asm));
						ai.addOperand(new Operand(-1, Operand.Mode.Variable));
						asm.addLine(ai);
					}
					// add actual GLK instruction
					AsmInstruction ai = new AsmInstruction(Mnemonic.list.get("glk"));
					ai.addOperand(new Operand(parts.get(0), asm));
					ai.addOperand(new Operand(parts.size() - 2));
					ai.addOperand(new Operand(parts.get(parts.size()-1), asm));
					asm.addLine(ai);


					// perform a function shortcut call
					// format: _call function args+ dest
				} else if (mnemonic.equals("_call")) {
					// make sure we don't have a string
					if (parts.get(0).isType(Token.Type.String)) {
						throw new AsmException("cannot pass string as function identifier of _call call");
					}

					// add instructions to push args onto stack
					for (int i = parts.size() - 2; i > 0; --i) {
						AsmInstruction ai = new AsmInstruction(Mnemonic.list.get("copy"));
						ai.addOperand(new Operand(parts.get(i), asm));
						ai.addOperand(new Operand(-1, Operand.Mode.Variable));
						asm.addLine(ai);
					}
					// add actual GLK instruction
					AsmInstruction ai = new AsmInstruction(Mnemonic.list.get("call"));
					ai.addOperand(new Operand(parts.get(0), asm));
					ai.addOperand(new Operand(parts.size() - 2));
					ai.addOperand(new Operand(parts.get(parts.size()-1), asm));
					asm.addLine(ai);


					// declare a raw data segment
				} else if (mnemonic.equals("raw")) {
					lineMatches(parts, false, mnemonic, Token.Type.Identifier);
					asm.addLine(new AsmLabel(parts.get(0).getStringValue()));
					byte[] data = new byte[parts.size() - 1];
					for (int i = 0; i < data.length; ++i) {
						Token t = parts.get(i+1);
						if (!t.isType(Token.Type.Integer)) {
							throw new AsmException("data values must be integers");
						}
						data[i] = (byte)t.getIntValue();
					}
					asm.addLine(new AsmData(data));

					// declare a raw data segment of fixed length
				} else if (mnemonic.equals("rawlen")) {
					lineMatches(parts, false, mnemonic, Token.Type.Identifier);
					asm.addLine(new AsmLabel(parts.get(0).getStringValue()));

					int dataLength = 0;
					switch(parts.get(1).getType()) {
					case Identifier:
						String constantName = parts.get(1).getStringValue();
						if (asm.isConstantDefined(constantName)) {
							dataLength = asm.getConstantValue(constantName);
						} else {
							throw new AsmException("Length must be an integer or previously defined constant value.");
						}
						break;
					case Integer:
						dataLength = parts.get(1).getIntValue();
						break;
					default:
						throw new AsmException("Length must be an integer or previously defined constant value.");
					}

					byte[] data;
					if (parts.size() - 3 > 0) {
						data = new byte[parts.size() - 3];
						for (int i = 0; i < data.length; ++i) {
							data[i] = (byte)parts.get(i+3).getIntValue();
						}
					} else {
						data = new byte[0];
					}
					asm.addLine(new AsmData(dataLength, data));

					// declare a basic string; this will automatically be stored as
					// Unicode if necessary
				} else if (mnemonic.equals("string")) {
					lineMatches(parts, true, mnemonic, Token.Type.Identifier, Token.Type.String);
					asm.addLine(new AsmLabel(parts.get(0).getStringValue(),AsmLabel.Type.String));
					asm.addLine(new AsmData(parts.get(1).getStringValue()));

					// add a new instruction code
				} else {
					if (Mnemonic.list.containsKey(mnemonic)) {
						// get the mnemonic
						Mnemonic m = Mnemonic.list.get(mnemonic);
						if (m.operands != parts.size()) {
							throw new AsmException("Bad operand count");
						}

						AsmInstruction ai = new AsmInstruction(m);
						for (int i = 0; i < parts.size(); ++i) {
							ai.addOperand(new Operand(parts.get(i), asm));
						}
						asm.addLine(ai);
					} else {
						throw new AsmException("Unknown mnemonic \"" + mnemonic + "\"");
					}
				}
			} catch (AsmException e) {
				System.err.println(inputFile+"("+lineNo+"): " + e.getMessage());
				foundErrors = true;
			}
		}

		return foundErrors;
	}
}
