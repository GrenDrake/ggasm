package com.grenslair.glulx.ggasm;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;

public class Build {
	private ObjectFile asm;

	public Build() {
		asm = new ObjectFile();
		asm.addToRom(true);
		asm.addLine(new AsmData(0x47474153, true));
		asm.addToRom(false);
		new HashSet<String>();
	}

	public ObjectFile getObjectFile() {
		return asm;
	}

	public void build(String outputFile) {
		if (asm.doBuild()) {
			asm.writeByteCodeToFile(outputFile);
		}
	}

	public boolean fromFile(String filename) {
		return Assemble.fromFile(asm, filename);
	}



	public static void main(String args[]) {
		Build a = new Build();

		if (args.length != 2) {
			System.err.println("USAGE: ggasm <infile> <outfile>");
			return;
		}
		String infile  = args[0];
		String outfile = args[1];

		if (!a.fromFile(infile)) {
			a.build(outfile);

			try( PrintWriter out = new PrintWriter("codedump.txt") ){
				out.println(a.getObjectFile().dumpCode());
			} catch (IOException e) {
				System.err.println(e);
			}

			try( PrintWriter out = new PrintWriter("symbols.txt") ){
				out.println(a.getObjectFile().dumpSymbols());
				out.println("\n");
				out.println(a.getObjectFile().dumpConstants());
			} catch (IOException e) {
				System.err.println(e);
			}
		} else {
			System.err.println("Errors occured during assembly.");
			System.exit(1);
		}
	}
}
