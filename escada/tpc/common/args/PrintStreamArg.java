package escada.tpc.common.args;

import java.io.FileOutputStream;
import java.io.PrintStream;

public class PrintStreamArg extends Arg {
	public PrintStream s = null;

	public String fName = null;

	public PrintStreamArg(String arg, String name, String desc) {
		super(arg, name, desc, true, false);
	}

	public PrintStreamArg(String arg, String name, String desc, ArgDB db) {
		super(arg, name, desc, true, false, db);
	}

	// Customize to parse arguments.
	protected int parseMatch(String[] args, int a) throws Arg.Exception {
		if (a == args.length) {
			throw new Arg.Exception("File name missing.", a);
		}

		PrintStream oldS = s;
		try {
			s = new PrintStream(new FileOutputStream(args[a]));
			fName = args[a];
			if (oldS != null)
				oldS.close();
		} catch (java.io.IOException fnf) {
			throw new Arg.Exception("Unable to open file " + args[a] + ".", a);
		}

		return (a + 1);
	}

	public String value() {
		return (fName);
	}
}
// arch-tag: 761bd3e7-a33d-4700-969a-86235bcb6957
