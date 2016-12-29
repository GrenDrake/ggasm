package com.grenslair.glulx.ggasm;

import java.nio.ByteBuffer;

public class AsmLabel extends AsmLine {
    public enum Type {
        BuiltIn,
        General,
        Object,
        String,
        StackFunction,
        LocalFunction
    }

    private String name;
    private int localCount;
    private Type type;

    public AsmLabel(String name) {
        super();
        this.name = name;
        this.type = Type.General;
    }
    public AsmLabel(String name, Type type) {
        super();
        this.name = name;
        this.type = type;
    }
    public AsmLabel(String name, Type type, int localCount) {
        super();
        this.name = name;
        this.type = type;
        this.localCount = localCount;
    }

    public String getName() {
        return name;
    }
    public Type getType() {
        return type;
    }

    public void buildByteCode(ByteBuffer code) {
        if (type != Type.StackFunction && type != Type.LocalFunction) {
            return;
        }

        if (type == Type.StackFunction) {
            code.put((byte)0xC0);
        } else {
            code.put((byte)0xC1);
        }
        int locals = localCount;
        while (locals > 0) {
            code.put((byte)4);
            if (locals > 255) {
                code.put((byte)255);
            } else {
                code.put((byte)locals);
            }
            locals -= 255;
        }
        code.put((byte)0);
        code.put((byte)0);
    }
    public int getSize() {
        if (type != Type.StackFunction && type != Type.LocalFunction) {
            return 0;
        }

        int result = 3, locals = localCount;
        while (locals > 0) {
            result += 2;
            locals -= 256;
        }
        return result;
    }
    public String toString() {
        return "[LABEL:"+name+" type:"+type+" localCount:"+localCount+" @"+getSourceFile()+":"+getSourceLine()+"]";
    }

}
