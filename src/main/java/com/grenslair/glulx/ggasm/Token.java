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
		Empty
	}

	/**
	 * Create a new Token object with the specified value and type.
	 * @param value  the value of the new Token object
	 * @param type   the type of the new Token object
	 */
	public Token(String value, Type type) {
		this.stringValue = value;
		this.type = type;
	}
	/**
	 * Create a new Integer Token object with the specified value.
	 * @param value  the value of the new Token
	 */
	public Token(int value) {
		intValue = value;
		type = Type.Integer;
	}
	/**
	 * Create a new Float Token object with the specified value.
	 * @param value  the value of the new Token
	 */
	public Token(float value) {
		floatValue = value;
		type = Type.Float;
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
