package escada.tpc.common.args;

public class BooleanArg extends Arg {
	public boolean flag = false;

	public BooleanArg(String arg, String name, String desc) {
		super(arg, name, desc, false, false);
	}

	public BooleanArg(String arg, String name, String desc, ArgDB db) {
		super(arg, name, desc, false, false, db);
	}

	public BooleanArg(String arg, String name, String desc, boolean def) {
		super(arg, name, desc, false, true);
		flag = def;
	}

	public BooleanArg(String arg, String name, String desc, boolean def,
			ArgDB db) {
		super(arg, name, desc, false, true, db);
		flag = def;
	}

	// Customize to parse arguments.
	protected int parseMatch(String[] args, int a) throws Arg.Exception {
		if (a == args.length) {
			throw new Arg.Exception("Boolean argument missing value.", a);
		}

		char ch = args[a].charAt(0);

		switch (ch) {
		case '0': // zero
		case 'F': // false
		case 'f':
		case 'd': // disable
		case 'D':
		case 'n': // no
		case 'N':
			flag = false;
			break;
		case '1': // one
		case 'T': // true
		case 't':
		case 'e': // enable
		case 'E':
		case 'y': // yes
		case 'Y':
			flag = true;
			break;
		default:
			throw new Arg.Exception("Unable to parse flag (" + args[a] + ").",
					a);
		}

		return (a + 1);
	}

	public String value() {
		return ("" + flag);
	}
}

// arch-tag: 7391d710-e257-4432-8221-24e5e1f94cfa
