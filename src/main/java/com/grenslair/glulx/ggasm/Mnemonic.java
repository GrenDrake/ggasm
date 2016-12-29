package com.grenslair.glulx.ggasm;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Mnemonic {
    public String text;
    public int opcode;
    public int operands;
    public boolean finalRelative;

    static final public Map<String,Mnemonic> list;
    static {
        HashMap<String,Mnemonic> aList = new HashMap<String,Mnemonic>();
        aList.put("nop",           new Mnemonic("nop",           0x00, 0));
        // integer math
        aList.put("add",           new Mnemonic("add",           0x10,  3));
        aList.put("sub",           new Mnemonic("sub",           0x11,  3));
        aList.put("mul",           new Mnemonic("mul",           0x12,  3));
        aList.put("div",           new Mnemonic("div",           0x13,  3));
        aList.put("mod",           new Mnemonic("mod",           0x14,  3));
        aList.put("neg",           new Mnemonic("neg",           0x15,  2));
        // floating point math and conversions
        aList.put("numtof",        new Mnemonic("numtof",        0x190,  2));
        aList.put("ftonumz",       new Mnemonic("ftonumz",       0x191,  2));
        aList.put("ftonumn",       new Mnemonic("ftonumn",       0x192,  2));
        aList.put("ceil",          new Mnemonic("ceil",          0x198,  2));
        aList.put("floor",         new Mnemonic("floor",         0x199,  2));
        aList.put("fadd",          new Mnemonic("fadd",          0x1A0,  3));
        aList.put("fsub",          new Mnemonic("fsub",          0x1A1,  3));
        aList.put("fmul",          new Mnemonic("fmul",          0x1A2,  3));
        aList.put("fdiv",          new Mnemonic("fdiv",          0x1A3,  3));
        aList.put("fmod",          new Mnemonic("fmod",          0x1A4,  4));
        aList.put("sqrt",          new Mnemonic("sqrt",          0x1A8,  2));
        aList.put("exp",           new Mnemonic("exp",           0x1A9,  2));
        aList.put("log",           new Mnemonic("log",           0x1AA,  2));
        aList.put("pow",           new Mnemonic("pow",           0x1AB,  3));
        aList.put("sin",           new Mnemonic("sin",           0x1B0,  2));
        aList.put("cos",           new Mnemonic("cos",           0x1B1,  2));
        aList.put("tan",           new Mnemonic("tan",           0x1B2,  2));
        aList.put("asin",          new Mnemonic("asin",          0x1B3,  2));
        aList.put("acos",          new Mnemonic("acos",          0x1B4,  2));
        aList.put("atan",          new Mnemonic("atan",          0x1B5,  2));
        aList.put("atan2",         new Mnemonic("atan2",         0x1B6,  3));
        // bitwise operations
        aList.put("bitand",        new Mnemonic("bitand",        0x18,  3));
        aList.put("bitor",         new Mnemonic("bitor",         0x19,  3));
        aList.put("bitxor",        new Mnemonic("bitxor",        0x1A,  3));
        aList.put("bitnot",        new Mnemonic("bitnot",        0x1B,  3));
        aList.put("shiftl",        new Mnemonic("shiftl",        0x1C,  3));
        aList.put("sshiftr",       new Mnemonic("sshiftr",       0x1D,  3));
        aList.put("ushiftr",       new Mnemonic("ushiftr",       0x1E,  3));
        // jumps (most take relative addresses)
        aList.put("jump",          new Mnemonic("jump",          0x20,  1, true));
        aList.put("jz",            new Mnemonic("jz",            0x22,  2, true));
        aList.put("jnz",           new Mnemonic("jnz",           0x23,  2, true));
        aList.put("jeq",           new Mnemonic("jeq",           0x24,  3, true));
        aList.put("jne",           new Mnemonic("jne",           0x25,  3, true));
        aList.put("jlt",           new Mnemonic("jlt",           0x26,  3, true));
        aList.put("jge",           new Mnemonic("jge",           0x27,  3, true));
        aList.put("jgt",           new Mnemonic("jgt",           0x28,  3, true));
        aList.put("jle",           new Mnemonic("jle",           0x29,  3, true));
        aList.put("jltu",          new Mnemonic("jltu",          0x2A,  3, true));
        aList.put("jgeu",          new Mnemonic("jgeu",          0x2B,  3, true));
        aList.put("jgtu",          new Mnemonic("jgtu",          0x2C,  3, true));
        aList.put("jleu",          new Mnemonic("jleu",          0x2D,  3, true));
        aList.put("jumpabs",       new Mnemonic("jumpabs",       0x104, 1));
        aList.put("jfeq",          new Mnemonic("jfeq",          0x1C0, 4, true));
        aList.put("jfne",          new Mnemonic("jfne",          0x1C1, 4, true));
        aList.put("jflt",          new Mnemonic("jflt",          0x1C2, 3, true));
        aList.put("jfle",          new Mnemonic("jfle",          0x1C3, 3, true));
        aList.put("jfgt",          new Mnemonic("jfgt",          0x1C4, 3, true));
        aList.put("jfge",          new Mnemonic("jfge",          0x1C5, 3, true));
        aList.put("jisnan",        new Mnemonic("jisnan",        0x1C8, 2, true));
        aList.put("jisinf",        new Mnemonic("jisinf",        0x1C9, 2, true));
        // function calls
        aList.put("call",          new Mnemonic("call",          0x30,  3));
        aList.put("return",        new Mnemonic("return",        0x31,  1));
        aList.put("catch",         new Mnemonic("catch",         0x33,  2));
        aList.put("throw",         new Mnemonic("throw",         0x34,  2));
        aList.put("tailcall",      new Mnemonic("tailcall",      0x35,  2));
        aList.put("callf",         new Mnemonic("callf",         0x35,  2));
        aList.put("callfi",        new Mnemonic("callfi",        0x35,  3));
        aList.put("callfii",       new Mnemonic("callfii",       0x35,  4));
        aList.put("callfiii",      new Mnemonic("callfiii",      0x35,  5));
        // moving data
        aList.put("copy",          new Mnemonic("copy",          0x40,  2));
        aList.put("copys",         new Mnemonic("copys",         0x41,  2));
        aList.put("copyb",         new Mnemonic("copyb",         0x42,  2));
        aList.put("sexs",          new Mnemonic("sexs",          0x44,  2));
        aList.put("sexb",          new Mnemonic("sexb",          0x45,  2));
        aList.put("aload",         new Mnemonic("aload",         0x48,  2));
        aList.put("aloads",        new Mnemonic("aloads",        0x49,  2));
        aList.put("aloadb",        new Mnemonic("aloads",        0x4A,  2));
        aList.put("aloadbit",      new Mnemonic("aloads",        0x4B,  2));
        aList.put("astore",        new Mnemonic("aloads",        0x4C,  2));
        aList.put("astores",       new Mnemonic("aloads",        0x4D,  2));
        aList.put("astoreb",       new Mnemonic("aloads",        0x4E,  2));
        aList.put("astorebit",     new Mnemonic("aloads",        0x4F,  2));
        // stack operations
        aList.put("stkcount",      new Mnemonic("stkcount",      0x50,  1));
        aList.put("stkpeek",       new Mnemonic("stkpeek",       0x51,  2));
        aList.put("stkswap",       new Mnemonic("stkswap",       0x52,  0));
        aList.put("stkroll",       new Mnemonic("stkroll",       0x53,  2));
        aList.put("stkcopy",       new Mnemonic("stkcopy",       0x54,  1));
        // output operations
        aList.put("streamchar",    new Mnemonic("streamchar",    0x70,  1));
        aList.put("streamnum",     new Mnemonic("streamnum",     0x71,  1));
        aList.put("streamstr",     new Mnemonic("streamstr",     0x72,  1));
        aList.put("streamunichar", new Mnemonic("streamunichar", 0x73,  1));
        // other
        aList.put("gestalt",       new Mnemonic("gestalt",       0x100, 3));
        aList.put("debugtrap",     new Mnemonic("debugtrap",     0x101, 1));
        aList.put("getmemsize",    new Mnemonic("getmemsize",    0x102, 1));
        aList.put("setmemsize",    new Mnemonic("setmemsize",    0x103, 2));
        aList.put("random",        new Mnemonic("random",        0x110, 2));
        aList.put("setrandom",     new Mnemonic("setrandom",     0x111, 1));
        aList.put("quit",          new Mnemonic("quit",          0x120, 0));
        aList.put("verify",        new Mnemonic("verify",        0x121, 1));

        aList.put("setiosys",      new Mnemonic("setiosys",      0x149, 2));
        aList.put("glk",           new Mnemonic("glk",           0x130, 3));


/*
instruction_code_t CodeInstruction::instructionCodes[] = {
	{ "restart",		0x122,	0 },
	{ "save",			0x123,	2 },
	{ "restore",		0x124,	2 },
	{ "saveundo",		0x125,	1 },
	{ "restoreundo",	0x126,	1 },
	{ "protect",		0x127,	2 },
	{ "glk",			0x130,	3 },
	{ "getstringtbl",	0x140,	1 },
	{ "setstringtbl",	0x141,	1 },
	{ "getiosys",		0x148,	2 },
	{ "setiosys",		0x149,	2 },
	{ "linearsearch",	0x150,	8 },
	{ "binarysearch",	0x151,	8 },
	{ "linkedsearch",	0x152,	7 },
	{ "mzero",			0x170,	2 },
	{ "mcopy",			0x171,	3 },
	{ "malloc",			0x178,	2 },
	{ "mfree",			0x179,	1 },
	{ "accelfunc",		0x180,	2 },
	{ "accelparam",		0x181,	2 },
*/

        list = Collections.unmodifiableMap(aList);
    }

    public static Mnemonic byOpcode(int opcode) {
        for (String s : Mnemonic.list.keySet()) {
            Mnemonic m = Mnemonic.list.get(s);
            if (m.opcode == opcode) {
                return m;
            }
        }
        return null;
    }

    Mnemonic(String text, int opcode, int operands) {
        this(text,opcode,operands,false);
    }
    Mnemonic(String text, int opcode, int operands, boolean finalRelative) {
        this.text = text;
        this.opcode = opcode;
        this.operands = operands;
        this.finalRelative = finalRelative;
    }
}
