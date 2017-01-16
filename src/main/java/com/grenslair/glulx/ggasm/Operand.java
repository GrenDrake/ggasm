package com.grenslair.glulx.ggasm;

public class Operand {

    /**
    * The various addressing modes an operand mode can have. Note that
    * Property is not actually an addressing mode and is instead a reference
    * into the object property table. In addition, AfterRom is supported by
    * the Glulx virtual machine, but no method of specifying it is included.
    */
    public enum Mode {
        Constant,
        Indirect, // (contents of address)
        Variable, // local variable number; -1 = top of stack
        AfterRom  // (indirect, starting from end of rom)
    }

    private int size;
    private int value;
    public String symbolName;
    private Mode mode;

    /**
    * Create a new operand whose value is a specified integer and whose mode
    * is Constant.
    * @param value the value of the new operand
    */
    public Operand(int value) {
        this(value, Mode.Constant);
    }

    /**
    * Create a new operand whose value is a specified integer and mode.
    * @param value the value of the new operand
    * @param mode the mode of the new operand
    */
    public Operand(int value, Mode mode) {
        this.value = value;
        this.mode = mode;
        resize();
    }

    /**
    * Create a new operand whose value is a symbol reference and whose mode is
    * Constant
    * @param symbol the name of the symbol representing this operand's value
    */
    public Operand(String symbol) {
        this(symbol, Mode.Constant);
    }

    /**
    * Create a new operand whose value is a symbol reference and whose mode is
    * as specified.
    * @param symbol the name of the symbol representing this operand's value
    * @param mode the mode of the new operand
    */
    public Operand(String symbol, Mode mode) {
        symbolName = symbol;
        this.mode = mode;
        // we presume all references are four bytes
        size = 4;
    }

    /**
    * Create a new operand based on the content of a specified token. This
    * will automatically setup the operand according to the content and
    * addressing mode specified in the token.
    */
    public Operand(Token token, ObjectFile objFile) throws AsmException {
        switch(token.getType()) {
            case Integer:
                this.value = token.getIntValue();
                this.mode = Mode.Constant;
                break;
            case String:
                this.symbolName = objFile.addString(token.getStringValue());
                this.mode = Mode.Constant;
                break;
            case Float:
                this.value = Float.floatToRawIntBits(token.getFloatValue());
                this.mode = Mode.Constant;
                break;
            case Identifier:
                // handle stack reference
                if (token.getStringValue().equals("sp")) {
                    this.value = -1;
                    this.mode = Mode.Variable;
                    return;
                }
                // otherwise determine the addressing mode and (if neccesary)
                // remove it from the token text
                switch(token.getStringValue().charAt(0)) {
                    case '*':
                        this.mode = Operand.Mode.Indirect;
                        this.symbolName = token.getStringValue().substring(1);
                        break;
                    case '#':
                        this.mode = Operand.Mode.Variable;
                        this.symbolName = token.getStringValue().substring(1);
                        break;
                    default:
                        this.mode = Operand.Mode.Constant;
                        this.symbolName = token.getStringValue();
                }
                // check to see if the remaining text is an already defined
                // constant symbol
                if (objFile.isConstantDefined(this.symbolName)) {
                    this.value = objFile.getConstantValue(this.symbolName);
                    this.symbolName = null;
                } else {
                    // check to see if the token is actually a number rather than
                    // a label
                    try {
                        this.value = Integer.parseInt(this.symbolName);
                        this.symbolName = null;
                    } catch (NumberFormatException e) {
                        // it's a label after all!
                        // we don't actually need to do anything; it's already
                        // setup for this case
                    }
                }
                break;
            default:
                throw new AsmException(token.getSource() + ": Cannot create Operand from token type " + token.getType());
        }
        resize();
    }


    /**
    * Determine how many bytes it will require to store this operand. Operands
    * whose actual value is still unknown are presumed to occupy four bytes.
    */
    public void resize() {
        // if this value repersents a symbol, just set the size to four bytes
        if (isSymbol()) {
            size = 4;
            return;
        }
        // otherwise, find the size of this operand
        if (mode == Mode.Constant) {
            if (value == 0) {
                size = 0;
            } else if (value >= -128 && value <= 0x7F) {
                size = 1;
            } else if (value >= -32768 && value <= 0x7FFF) {
                size = 2;
            } else {
                size = 4;
            }
        } else {
            if (value == -1 && mode == Mode.Variable) {
                size = 0;
            } else if (value <= 0xFF) {
                size = 1;
            } else if (value <= 0xFFFF) {
                size = 2;
            } else {
                size = 4;
            }
        }
    }

    /**
    * Return the storage size of this operand in bytes.
    * @return The storage size of this operand in bytes.
    */
    public int getSize() {
        return size;
    }
    /**
    * Get the addressing mode of this operand.
    * @return The addrressing mode of this operand.
    */
    public Mode getMode() {
        return mode;
    }
    /**
    * Set the addressing mode of this operand.
    * @param mode the new mode for this operand
    */
    public void setMode(Mode mode) {
        this.mode = mode;
    }
    /**
    * Get the addressing mode/size half-byte for this operand.
    * @return A byte with the four byte addressing mode.
    */
    public int getAddressMode() {
        int result = 0;
        if (size == 4) result += 3;
        else           result += size;
        switch(mode) {
            case Constant:
                // add 0 to result
                break;
            case Indirect:
                result += 4;
                break;
            case Variable:
                result += 8;
                break;
            case AfterRom:
                result += 12;
                break;
            default:
                System.err.println("Unknown addressing mode.");
                return 0;
        }
        return result;
    }

    /**
    * Get the value of this operand.
    * @return the value of this operand.
    */
    public int getValue() {
        if (mode == Mode.Variable) {
            return value * 4;
        }
        return value;
    }
    /**
    * Set the value of this operand. This will also unmark the operand as
    * being a symbol.
    * @param newValue the new value of this operand
    */
    public void setValue(int newValue) {
        value = newValue;
        symbolName = null;
    }

    /**
    * Return whether this operand's value is currently being repersented by
    * a symbol.
    * @return whether this operand is repersented by a symbol
    */
    public boolean isSymbol() {
        return symbolName != null;
    }
    /**
    * Get the name of the symbol repersenting this operand's value. Returns an
    * empty string if this operand is not a symbol.
    * @return the name of the symbol for this operand, or an empty string if
    *   not a symbol.
    */
    public String getSymbol() {
        if (symbolName != null) {
            return symbolName;
        }
        return "";
    }

    /**
    * If this Operand is the name of a symbol, replace it. Otherwise, do nothing.
    * @param objFile The ObjectFile to draw symbol names and values from
    */
    public void replaceSymbol(ObjectFile objFile) throws AsmException {
        if (isSymbol()) {
            if (!objFile.isSymbolKnown(getSymbol())) {
                throw new AsmException("Undefined symbol \""+getSymbol()+"\"");
            }
            int value = objFile.getSymbolValue(getSymbol());
            setValue(value);
        }
    }

    /**
    * Get the value of this Operand as a string. If this is a symbol, return the 
    * symbol name; if it's an integer, convert it to a string.
    *
    * (this function is mostly intended for debugging)
    * @return the value of this oeprand as a string 
    */
    public String valueToString() {
        if (isSymbol()) {
            return getSymbol();
        }
        return "0x" + Integer.toString(value, 16);
    }

    /**
    * Convert this operand to a string.
    * @return this operand as a string
    */
    public String toString() {
        return "[value:"+value+" size:"+size+" name:"+symbolName+" mode:"+mode+"]";
    }
}
