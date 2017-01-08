package com.grenslair.glulx.ggasm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Assemble {
    public static final int majorVersion = 0;
    public static final int minorVersion = 5;
    public static final int patchVersion = 0;

	private ObjectFile asm;
	private String inputFile;
	private String filePath;
	private String fileContent;
	private int fileLength;
	private ArrayList<Token> tokenList;

	private int lexPos;
	private int lexerLast;
	private int lexerChar;
	private int lexerLine;

	private int curToken;

	public Assemble(ObjectFile asm, String filename) throws AsmException {
		tokenList      = new ArrayList<Token>();
		this.asm       = asm;
		this.inputFile = filename;

		try {
			Path path = Paths.get(inputFile);
			if (path.getParent() != null) {
				filePath = path.getParent().toString() + "/";
			} else {
				filePath = "./";
			}
			byte[] fileBytes;
			fileBytes = Files.readAllBytes(path);
			fileContent = new String(fileBytes, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new AsmException("IO Error: " + e.getMessage());
		}
		fileLength = fileContent.length();

		doLex();
		doParse();
	}

    /**
     * Replace escape characters within the provided text with their real
     * versions
     * @param text  the string to replace escape codes within
     * @return the string after escape evaluation is complete
     */
    public String doEscapes(String text) throws AsmException {
		StringBuilder sb = new StringBuilder();
		// TODO update for unicode
		for (int i = 0; i < text.length(); ++i) {
			if (text.charAt(i) == '\\') {
				++i;
				switch(text.charAt(i)) {
					case '0':  // nothing
						break;
					case '\\': // backslash
					case '"':  // quote
                    case '\'': // single quote
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
							throw new AsmException(inputFile+"("+lexerLine+"): Unexpected end of escape in \\xXX");
						}
						int start = i;
						while (i < text.length() && isHexDigit(text.charAt(i))) {
							++i;
						}
						int v = Integer.parseInt(text.substring(start,i), 16);
						System.err.println(v);
						sb.appendCodePoint(v);
						--i;
						break;
					default:   // unknown
						throw new AsmException(inputFile+"("+lexerLine+"): Unknown character escape \\" + text.charAt(i));
				}
			} else {
				sb.append(text.charAt(i));
			}
		}
        return sb.toString();
    }

	/**
	 * Parse one line from an assembly input file and return a list of tokens
	 * representing the content.
	 */
	public void doLex() throws AsmException {
		if (fileContent.isEmpty()) {
			return;
		}
		lexerChar = fileContent.codePointAt(0);

		lexPos = 0;
		lexerLine = 1;
		int start = 0, end;
		while (lexHasNext()) {

			// parse end of statement
			if (lexChar() == '\n') {
				lexNext();
				tokenList.add(new Token(inputFile, lexerLine, Token.Type.End));
				continue;
			}

			if (Character.isWhitespace(lexChar())) {
				while(lexHasNext() && lexChar() != '\n' && Character.isWhitespace(lexChar())) {
					lexNext();
				}
				continue;
			}

			// end-of-line comment
			if (lexChar() == ';') {
				tokenList.add(new Token(inputFile, lexerLine, "", Token.Type.End));
				while(lexHasNext() && lexChar() != '\n') {
					lexNext();
				}
				continue;
			}

            // parse character constants
			if (lexChar() == '\'') {
				lexNext();
				start = lexPos;
				while (lexHasNext() && (lexChar() != '\'' || lexPrev() == '\\')) {
					lexNext();
				}
				if (!lexHasNext()) {
					throw new AsmException(inputFile+"("+lexerLine+"): Unterminated string.");
				}
				end = lexPos;
				lexNext();
				String text = fileContent.substring(start,end);
                text = doEscapes(text);

                if (text.length() == 0) {
					throw new AsmException(inputFile+"("+lexerLine+"): empty character constant.");
                }
                if (text.length() > 1) {
					throw new AsmException(inputFile+"("+lexerLine+"): character constant has multiple characters.");
                }
				tokenList.add(new Token(inputFile, lexerLine, text.codePointAt(0)));
                continue;
            }

			// parse strings
			if (lexChar() == '"') {
				lexNext();
				start = lexPos;
				while (lexHasNext() && (lexChar() != '"' || lexPrev() == '\\')) {
					lexNext();
				}
				if (!lexHasNext()) {
					throw new AsmException(inputFile+"("+lexerLine+"): Unterminated string.");
				}
				end = lexPos;
				lexNext();
				String text = fileContent.substring(start,end);
                text = doEscapes(text);

				tokenList.add(new Token(inputFile, lexerLine, text, Token.Type.String));

				// parse decimal numbers
			} else if (lexChar() == '-' || Character.isDigit(lexChar())) {
				start = lexPos;
				boolean parseFloat = false;
				lexNext();
				while (lexHasNext() && (Character.isDigit(lexChar()) || lexChar() == '.')) {
					if (lexChar() == '.') {
						parseFloat = true;
					}
					lexNext();
				}
				String unparsedNumber = fileContent.substring(start,lexPos);
				if (parseFloat) {
					tokenList.add(new Token(inputFile, lexerLine, Float.parseFloat(unparsedNumber)));
				} else {
                    if (unparsedNumber.codePointAt(0) == '-') {
                        tokenList.add(new Token(inputFile, lexerLine, Integer.parseInt(unparsedNumber, 10)));
                    } else {
                        tokenList.add(new Token(inputFile, lexerLine, Integer.parseUnsignedInt(unparsedNumber, 10)));
					}
				}

				// parse hex numbers
			} else if (lexChar() == '$') {
				lexNext();
				start = lexPos;
				while (lexHasNext() && isHexDigit(lexChar())) {
					lexNext();
				}
				tokenList.add(new Token(inputFile, lexerLine, Integer.parseUnsignedInt(fileContent.substring(start,lexPos), 16)));

				// parse identifiers
			} else if (isIdentifier(lexChar(), true)) {
				start = lexPos;
				lexNext();
				while (lexHasNext() && isIdentifier(lexChar(), false)) {
					lexNext();
				}
				if (lexHasNext() && lexChar() == ':') {
					lexNext();
				}
				end = lexPos;
				String text = fileContent.substring(start,end);
				tokenList.add(new Token(inputFile, lexerLine, text, Token.Type.Identifier));

			} else {
				// unknown
				throw new AsmException(inputFile+"("+lexerLine+"): Lexer: unexpected " + new String(Character.toChars(lexChar())) + " (" + lexChar() + ")");
			}
		}
		tokenList.add(new Token(inputFile, lexerLine, "", Token.Type.End));
	}
	/**
	 * Return the value of the next character from the file. This will advance
	 * the lexer position to the next value.
	 * @return the next character
	 */
	private int lexNext() {
		lexPos += Character.charCount(lexerChar);
		lexerLast = lexerChar;
		if (lexerLast == '\n') {
			++lexerLine;
		}
		if (lexPos < fileLength) {
			lexerChar = fileContent.codePointAt(lexPos);
		} else {
			lexerChar = 0;
		}
		return lexerChar;
	}
	private int lexChar() {
		return lexerChar;
	}
	private int lexPrev() {
		return lexerLast;
	}
	/**
	 * Return whether or not there is another character
	 * @return true if there is another character from the file data, false
	 *           otherwise
	 */
	private boolean lexHasNext() {
		return (lexPos < fileLength);
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
	static public void lineMatches(List<Token> parts, boolean matchLength, Token.Type ...types)
			throws AsmException {
		if (matchLength && parts.size() != types.length + 1) {
			throw new AsmException(parts.get(0).getSource()+": Bad operand count for " + parts.get(0).getStringValue() +
					" expected " + types.length + ", but found " +
					(parts.size()-1) + ".");
		}
		for (int i = 0; i < parts.size() && i < types.length; ++i) {
			if (!parts.get(i+1).isType(types[i])) {
				throw new AsmException(parts.get(0).getSource()+": Operand " + i + " is " +
						parts.get(i+1).getType() + ", but " +
						types[i] + " was expected for " +
						parts.get(0).getStringValue() + ".");
			}
		}
	}

	/**
	 * Add the content of an assembly input file to the current gamefile.
	 * @return whether errors occurred during assembly
	 */
	public void doParse() throws AsmException {
		curToken = 0;
		while (hasNextToken()) {

			// get the next statement, restarting the loop if empty and
			// verifying it starts with an identifier.
			ArrayList<Token> stmt = getNextTokens();
			if (stmt.isEmpty()) {
				continue;
			}
			if (!stmt.get(0).isType(Token.Type.Identifier)) {
				throw new AsmException(stmt.get(0).getSource()+": Expected statement to begin with identifier.");
			}

			// check for directives
			if (stmt.get(0).equalTo("stackSize")) {
				lineMatches(stmt, true, Token.Type.Integer);
				int stackSize = ObjectFile.roundUp(stmt.get(1).getIntValue());
				if (stackSize != stmt.get(1).getIntValue()) {
                    System.err.println(stmt.get(0).getSource()+": stack size "
                        + stmt.get(1).getIntValue()
                        + " is not a multiple up 256; round up to "
                        + stackSize + ".");
				}
				asm.setStackSize(stackSize);
				continue;
			}
			if (stmt.get(0).equalTo("toROM")) {
				lineMatches(stmt, true);
				asm.addToRom(true);
				continue;
			}
			if (stmt.get(0).equalTo("endROM")) {
				lineMatches(stmt, true);
				asm.addToRom(false);
				continue;
			}
			if (stmt.get(0).equalTo("include")) {
				lineMatches(stmt, true, Token.Type.String);
				String includedFile = filePath + stmt.get(1).getStringValue();
				System.err.println("including \""+includedFile+"\" from \""+inputFile+"\".");
				new Assemble(asm, includedFile);
				continue;
			}
            if (stmt.get(0).equalTo("includeBinary")) {
                lineMatches(stmt, true, Token.Type.Identifier, Token.Type.Identifier, Token.Type.String);
                String includedFile = filePath + stmt.get(3).getStringValue();
                System.err.println("including \""+includedFile+"\" from \""+inputFile+"\" into glulx file.");
				asm.addLine(new AsmLabel(stmt.get(1).getStringValue()));
                try {
        			Path path = Paths.get(includedFile);
        			byte[] fileBytes;
        			fileBytes = Files.readAllBytes(path);
                    asm.addLine(new AsmData(fileBytes));
                    asm.addConstant(stmt.get(2).getStringValue(), fileBytes.length, 0); // TODO last arg is source line);
                } catch (IOException e) {
        			throw new AsmException("IO Error: " + e.getMessage());
                }
                continue;
            }
			if (stmt.get(0).equalTo("constant")) {
				lineMatches(stmt, true, Token.Type.Identifier, Token.Type.Integer);
				asm.addConstant(stmt.get(1).getStringValue(), stmt.get(2).getIntValue(), 0); // TODO last arg is line number
				continue;
			}

			// check for a label
			if (stmt.get(0).getStringValue().endsWith(":")) {
				String text = stmt.get(0).getStringValue();
				asm.addLine(new AsmLabel(text.substring(0,text.length()-1)));
				stmt.remove(0);
				if (stmt.isEmpty()) {
					continue;
				}
				if (!stmt.get(0).isType(Token.Type.Identifier)) {
					throw new AsmException(stmt.get(0).getSource()+": Expected statement to begin with identifier.");
				}
			}

			// check for function definitions
			if (stmt.get(0).equalTo("function")) {
				lineMatches(stmt, true, Token.Type.Identifier, Token.Type.Integer);
				AsmLabel label = new AsmLabel(stmt.get(1).getStringValue(), AsmLabel.Type.LocalFunction, stmt.get(2).getIntValue());
				//				label.setSource(inputFile, lineNo);  TODO
				asm.addLine(label);
				continue;
			}
			if (stmt.get(0).equalTo("stkfunction")) {
				lineMatches(stmt, true, Token.Type.Identifier, Token.Type.Integer);
				AsmLabel label = new AsmLabel(stmt.get(1).getStringValue(), AsmLabel.Type.StackFunction, stmt.get(2).getIntValue());
				//				label.setSource(inputFile, lineNo); TODO
				asm.addLine(label);			//			throw new AsmException("Unexpected token: " + here);
				continue;
			}

			// check for entries in the string table
			if (stmt.get(0).equalTo("addString")) {
				lineMatches(stmt, true, Token.Type.Identifier, Token.Type.String);
				asm.addString(stmt.get(1).getStringValue(), stmt.get(2).getStringValue());
				continue;
			}

			// check for data statements
			if (stmt.get(0).equalTo("string")) {
				lineMatches(stmt, true, Token.Type.Identifier, Token.Type.String);
				asm.addLine(new AsmLabel(stmt.get(1).getStringValue(), AsmLabel.Type.String));
				asm.addLine(new AsmData(stmt.get(2).getStringValue(), AsmData.StringType.Automatic));
				continue;
			}
			if (stmt.get(0).equalTo("basicString")) {
				lineMatches(stmt, true, Token.Type.Identifier, Token.Type.String);
				asm.addLine(new AsmLabel(stmt.get(1).getStringValue(), AsmLabel.Type.String));
				asm.addLine(new AsmData(stmt.get(2).getStringValue(), AsmData.StringType.Basic));
				continue;
			}
			if (stmt.get(0).equalTo("unicodeString")) {
				lineMatches(stmt, true, Token.Type.Identifier, Token.Type.String);
				asm.addLine(new AsmLabel(stmt.get(1).getStringValue(), AsmLabel.Type.String));
				asm.addLine(new AsmData(stmt.get(2).getStringValue(), AsmData.StringType.Unicode));
				continue;
			}
			if (stmt.get(0).equalTo("bytes")) {
				lineMatches(stmt, false, Token.Type.Identifier);
				asm.addLine(new AsmLabel(stmt.get(1).getStringValue(), AsmLabel.Type.Data));
				byte[] data = new byte[stmt.size()-2];
                System.out.println(data);
				buildBytes(data, stmt, 2);
				asm.addLine(new AsmData(data));
				continue;
			}
			if (stmt.get(0).equalTo("bytesFixed")) {
				lineMatches(stmt, false, Token.Type.Identifier, Token.Type.Integer);
				asm.addLine(new AsmLabel(stmt.get(1).getStringValue(), AsmLabel.Type.Data));
				if (stmt.get(2).getIntValue() < stmt.size() - 3) {
					throw new AsmException(stmt.get(0).getSource()+"): bytesFixed has size of " + stmt.get(2).getIntValue() + ", but " + (stmt.size() - 3) + " values.");
				}
				byte[] data = new byte[stmt.get(2).getIntValue()];
				buildBytes(data, stmt, 3);
				asm.addLine(new AsmData(data));
				continue;
			}
			// check for a shortcut mnemonic (_glk or _call)
			if (stmt.get(0).equalTo("_glk") || stmt.get(0).equalTo("_call")) {
                doShortcutCall(stmt);
				continue;
			}

			// otherwise it must be a mnemonic (or an error)
			if (Mnemonic.list.containsKey(stmt.get(0).getStringValue())) {
				// get the mnemonic
				Mnemonic m = Mnemonic.list.get(stmt.get(0).getStringValue());
				if (m.operands != stmt.size() - 1) {
					throw new AsmException(stmt.get(0).getSource()+": Bad operand count");
				}

				AsmInstruction ai = new AsmInstruction(m);
				for (int i = 1; i < stmt.size(); ++i) {
					ai.addOperand(new Operand(stmt.get(i), asm));
				}
				asm.addLine(ai);
			} else {
				throw new AsmException(stmt.get(0).getSource()+": Unknown mnemonic \"" + stmt.get(0).getStringValue() + "\"");
			}
		}
	}

	/**
	 * Perform a shortcut call for either the glk or call opcode.
	 * @param stmt  the list of tokens for this call
	 */
	private void doShortcutCall(ArrayList<Token> stmt) throws AsmException {
        boolean isCall = (stmt.get(0).equalTo("_call"));
        if (stmt.get(1).isType(Token.Type.Identifier) && (!isCall && stmt.get(1).isType(Token.Type.Integer))) {
            throw new AsmException(stmt.get(0).getSource()+": Invalid function name for " + stmt.get(0).getStringValue());
        }

        // check that we have enough arguments
        if (isCall && stmt.size() < 3) {
            throw new AsmException(stmt.get(0).getSource()+": insufficent operands for _call (min 2)");
        }
        if (!isCall && stmt.size() < 4) {
            throw new AsmException(stmt.get(0).getSource()+": insufficent operands for _glk (min 3)");
        }

        if (isCall && stmt.size() - 3 <= 3) {
            AsmInstruction ai;
            switch(stmt.size() - 3) {
                case 0:
                    ai = new AsmInstruction(Mnemonic.list.get("callf"));
                    break;
                case 1:
                    ai = new AsmInstruction(Mnemonic.list.get("callfi"));
                    break;
                case 2:
                    ai = new AsmInstruction(Mnemonic.list.get("callfii"));
                    break;
                case 3:
                    ai = new AsmInstruction(Mnemonic.list.get("callfiii"));
                    break;
                default:
                    throw new AsmException(stmt.get(0).getSource()+": error in assembler; tried to construct callf* opcode with bad operand count");
            }
            // operand for function address
            ai.addOperand(new Operand(stmt.get(1), asm));
            // general operands
            for (int i = 2; i < stmt.size() - 1; ++i) {
                ai.addOperand(new Operand(stmt.get(i), asm));
            }
            // operand for return value
            ai.addOperand(new Operand(stmt.get(stmt.size()-1), asm));
            asm.addLine(ai);
            return;
        }

        // add instructions to push args onto stack
        for (int i = stmt.size() - 2; i > 1; --i) {
            AsmInstruction ai = new AsmInstruction(Mnemonic.list.get("copy"));
            ai.addOperand(new Operand(stmt.get(i), asm));
            ai.addOperand(new Operand(-1, Operand.Mode.Variable));
            asm.addLine(ai);
        }
        // add actual GLK instruction
        AsmInstruction ai;
        if (isCall) {
            ai = new AsmInstruction(Mnemonic.list.get("call"));
        } else {
            ai = new AsmInstruction(Mnemonic.list.get("glk"));
        }
        ai.addOperand(new Operand(stmt.get(1), asm));
        ai.addOperand(new Operand(stmt.size() - 3));
        ai.addOperand(new Operand(stmt.get(stmt.size()-1), asm));
        asm.addLine(ai);
	}

	private void buildBytes(byte[] data, ArrayList<Token> stmt, int startPos) throws AsmException{
		for (int i = startPos; i < stmt.size(); ++i) {
			if (!stmt.get(i).isType(Token.Type.Integer)) {
				throw new AsmException(stmt.get(i).getSource()+"expected int value(s) for bytes statement");
			}
			int value = stmt.get(i).getIntValue();
			byte b = (byte)(value);
			if ( (((int)b)&0xFF) != value) {
				throw new AsmException(stmt.get(i).getSource()+"bytes requires 1 byte values");
			}
			data[i-startPos] = b;
		}
	}

	private ArrayList<Token> getNextTokens() {
		ArrayList<Token> list = new ArrayList<Token>();
		Token tok = getToken();
		while (!tok.isType(Token.Type.End) && hasNextToken()) {
			list.add(tok);
			tok = nextToken();
		}
		nextToken();
		return list;
	}

	private Token getToken() {
		if (curToken < tokenList.size()) {
			return tokenList.get(curToken);
		}
		return null;
	}
	private Token nextToken() {
		++curToken;
		return getToken();
	}
	private boolean hasNextToken() {
		return (curToken < tokenList.size());
	}
}
