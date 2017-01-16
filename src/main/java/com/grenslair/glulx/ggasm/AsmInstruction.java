package com.grenslair.glulx.ggasm;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class AsmInstruction extends AsmLine {
	public Mnemonic mnemonic;
	public int opcode;
	public List<Operand> operands;

	public AsmInstruction(Mnemonic mnemonic, Operand... operands) {
		super();
		this.mnemonic = mnemonic;
		this.opcode = mnemonic.opcode;
		this.operands = new ArrayList<Operand>();
		for (Operand op : operands) {
			this.operands.add(op);
		}
	}
	public void addOperand(Operand o) {
		if (o != null) {
			operands.add(o);
		}
	}
	@Override
	public void replaceSymbols() throws AsmException {
		for (Operand o : operands) {
			if (o.isSymbol()) {
				if (!getObjectFile().isSymbolKnown(o.getSymbol())) {
					throw new AsmException(getSource() + ": Undefined symbol \""+o.getSymbol()+"\"");
				}
				int value = getObjectFile().getSymbolValue(o.getSymbol());
				o.setValue(value);
			}
		}
	}
	@Override
	public void buildByteCode(ByteBuffer code) {
		if (opcode <= 0x7F) {
			code.put((byte)opcode);
		} else if (opcode <= 0x3FFF) {
			code.putShort((short)(opcode | 0x8000));
		} else {
			code.putInt(opcode | 0xC0000000);
		}

		int b = 0;
		for (int i = 0; i < operands.size(); ++i) {
			if (i % 2 == 0) {
				b = operands.get(i).getAddressMode();
			} else {
				b += operands.get(i).getAddressMode() << 4;
				code.put((byte)b);
				b = 0;
			}
		}
		if (operands.size() % 2 != 0) {
			code.put((byte)b);
		}

		Mnemonic m = Mnemonic.byOpcode(opcode);
		for (int i = 0; i < operands.size(); ++i) {
			Operand o = operands.get(i);
			int value = o.getValue();
			if (i == operands.size() - 1 && m != null && m.finalRelative) {
				// last operand
				value -= (this.getPosition() + this.getSize());
				value += 2;
			}

			switch(o.getSize()) {
			case 0:
				break;
			case 1:
				code.put((byte)value);
				break;
			case 2:
				code.putShort((short)value);
				break;
			case 4:
				code.putInt(value);
				break;
			default:
				System.out.println("Bad operand size " + o.getSize());
			}
		}
	}
	@Override
	public int getSize() {
		int size = 0;

		// opcode size
		if (opcode <= 0x7F) {
			size = 1;
		} else if (opcode <= 0x3FFF) {
			size = 2;
		} else {
			size = 4;
		}

		// addressing modes size
		size += operands.size() / 2;
		if (operands.size() % 2 != 0) {
			++size;
		}

		// operands size
		for (Operand o : operands) {
			size += o.getSize();
		}

		return size;
	}
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[INST ");
		sb.append(opcode);
		sb.append(":");
		for (Operand o : operands) {
			sb.append(o);
		}
		sb.append("]");
		return sb.toString();
	}
}
