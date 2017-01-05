package com.grenslair.glulx.ggasm;

/**
 * The Token class is used to store an individual token produced by the lexer
 * when scanning a source file.
 */
public class Token {
	private Type type;
	private String stringValue;
	private int intValue;
	private float floatValue;

	private String fromFile;
	private int fromLine;

	/**
	 * Specifies the various types of tokens that can occur during lexing.
	 */
	public enum Type {
		Identifier,
		String,
		Integer,
		Float,
		OpenBrace,
		CloseBrace,
		Comma,
		End,
		Empty
	}

	/**
	 * Create a new Token object with the specified value and type.
	 * @param value  the value of the new Token object
	 * @param type   the type of the new Token object
	 */
	public Token(String file, int line, String value, Type type) {
        this.fromFile = file;
        this.fromLine = line;
		this.stringValue = value;
		this.type = type;
	}
	/**
	 * Create a new Integer Token object with the specified value.
	 * @param value  the value of the new Token
	 */
	public Token(String file, int line, int value) {
        this.fromFile = file;
        this.fromLine = line;
		intValue = value;
		type = Type.Integer;
	}
	/**
	 * Create a new Float Token object with the specified value.
	 * @param value  the value of the new Token
	 */
	public Token(String file, int line, float value) {
        this.fromFile = file;
        this.fromLine = line;
		floatValue = value;
		type = Type.Float;
	}
    /**
     * Create a new token of the specified type with no value.
     * @param type  the type of Token to create
    */
    public Token(String file, int line, Type type) {
        this.fromFile = file;
        this.fromLine = line;
		this.type = type;
    }

    /**
     * Return the file that this token is from.
     * @return the name of the file this token is from
     */
	public String getFile() {
		return fromFile;
	}
    /**
     * Return the line that this token is from.
     * @return the line this token is from
     */
	public int getLine() {
		return fromLine;
	}
    /**
     * Return a formatted string containing the file and line that this Token 
     * originated from.
     * @return the name and line of this Token's source as a string
     */
    public String getSource() {
        return fromFile+"("+fromLine+")";
    }

	/**
	 * Get the Type of this Token.
	 * @return the Type of this token.
	 */
	public Type getType() {
		return type;
	}
	/**
	 * Determine if this Token is of the specified type.
	 * @param type  the Token type to check for
	 * @return true if the Token is the specified type, false otherwise
	 */
	public boolean isType(Type type) {
		return (this.type == type);
	}

	/**
	 * Get the value of this Token as an int. This is only meaningful for
	 * Integer type tokens.
	 * @return the value of this token as an int
	 */
	public int getIntValue() {
		return intValue;
	}
	/**
	 * Get the value of this Token as an int. This is only meaningful for
	 * Integer type tokens.
	 * @return the value of this token as an int
	 */
	public float getFloatValue() {
		return floatValue;
	}
	/**
	 * Get the value of this Token as a String. This is not meaningful if the
	 * Token is of the Integer or Float type.
	 * @return
	 */
	public String getStringValue() {
		return stringValue;
	}

	/**
	 * Is this token equal to the specified text value? This is always false
	 * for Integer or Float type tokens.
	 * @param text  the text to compare the Token to
	 * @return true if the Token is equal to the text, false otherwise
	 */
	public boolean equalTo(String text) {
		if (type == Type.Integer || type == Type.Float) {
			return false;
		}
		return stringValue.equals(text);
	}

	/**
	 * Return this Token as a string.
	 * @return this token as a String
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Token [type=");
		builder.append(type);
		builder.append(", stringValue=");
		builder.append(stringValue);
		builder.append(", intValue=");
		builder.append(intValue);
		builder.append(", floatValue=");
		builder.append(floatValue);
		builder.append("]");
		return builder.toString();
	}
}
