package com.grenslair.glulx.ggasm;

import java.nio.ByteBuffer;

public abstract class AsmLine {
    private int position;
    private int sourceLine;
    private String sourceFile;
    private ObjectFile owner;

    public AsmLine() {
        position = 0;
    }
    public AsmLine(Token token) {
        position = 0;
        setSource(token);
    }
    public void setObjectFile(ObjectFile owner) {
        this.owner = owner;
    }
    public ObjectFile getObjectFile() {
        return owner;
    }
    public void setPosition(int pos) {
        position = pos;
    }
    public int getPosition() {
        return position;
    }

    public void setSource(Token token) {
        if (token != null) {
            sourceFile = token.getFile();
            sourceLine = token.getLine();
        }
    }
    public String getSource() {
        return sourceFile+"("+sourceLine+")";
    }
    public int getSourceLine() {
        return sourceLine;
    }
    public String getSourceFile() {
        return sourceFile;
    }

    public void replaceSymbols() throws AsmException {
    }


    abstract public void buildByteCode(ByteBuffer code);
    abstract public int getSize();
    abstract public String toString();
}
