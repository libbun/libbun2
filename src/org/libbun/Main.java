package org.libbun;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.text.DecimalFormat;

public class Main {
	public final static String  ProgName  = "libbun";
	public final static String  CodeName  = "yokohama";
	public final static int     MajorVersion = 2;
	public final static int     MinerVersion = 0;
	public final static int     PatchLevel   = 0;
	public final static String  Version = "2.0";
	public final static String  Copyright = "Copyright (c) 2013-2014, Konoha project authors";
	public final static String  License = "BSD-Style Open Source";

	// -l konoha.peg
	private static String LanguagePeg = "lib/peg/konoha.peg"; // default

	// -d driver
	private static String DriverName = "debug";  // default

	// -i
	private static boolean ShellMode = false;

	// -o
	private static String OutputFileName = null;

	//
	private static String InputFileName = null;

	// --parse-only
	public static boolean ParseOnly = false;

	// --verbose
	public static boolean EnableVerbose = false;

	// --verbose:ast
	public static boolean EnableVerboseAst = false;

	// --profile
	public static boolean ProfileMode = false;

	// --verbose:peg
	public static boolean PegDebuggerMode = false;

	// --disable-memo
	public static boolean NonMemoPegMode = false;

	private static void parseCommandArgument(String[] args) {
		int index = 0;
		while (index < args.length) {
			String argument = args[index];
			if (!argument.startsWith("-")) {
				break;
			}
			index = index + 1;
			if (argument.equals("--peg") && (index < args.length)) {
				LanguagePeg = args[index];
				PegDebuggerMode = true;
				index = index + 1;
			}
			else if (argument.equals("-l") && (index < args.length)) {
				LanguagePeg = args[index];
				index = index + 1;
			}
			else if ((argument.equals("-d") || argument.equals("--driver")) && (index < args.length)) {
				DriverName = args[index];
				index = index + 1;
			}
			else if ((argument.equals("-o") || argument.equals("--out")) && (index < args.length)) {
				OutputFileName = args[index];
				index = index + 1;
			}
			else if (argument.equals("-i")) {
				ShellMode = true;
			}
			else if (argument.equals("--disable-memo")) {
				NonMemoPegMode = true;
			}
			else if (argument.equals("--profile")) {
				ProfileMode = true;
			}
			else if (argument.equals("--parse-only")) {
				ParseOnly = true;
			}
			else if(argument.startsWith("--verbose")) {
				if(argument.equals("--verbose:ast")) {
					EnableVerboseAst = true;
				}
//				else if(argument.equals("--verbose:peg")) {
//					pegDebugger = true;
//				}
				else {
					EnableVerbose = true;
					EnableVerboseAst = true;
				}
			}
			else {
				ShowUsage("unknown option: " + argument);
			}
		}
		if (index < args.length) {
			InputFileName = args[index];
		}
		else {
			ShellMode = true;
		}
	}

	public final static void ShowUsage(String Message) {
		System.out.println(ProgName + " :");
		System.out.println("  --peg|-l  FILE          Language file");
		System.out.println("  --driver|-d  NAME       Driver");
		System.out.println("  --out|-o  FILE          Output filename");
		System.out.println("  --verbose               Printing Debug infomation");
		System.out.println("  --profile               Show memory usage and parse time");
		Main._Exit(0, Message);
	}

	private final static UniMap<Class<?>> driverMap = new UniMap<Class<?>>();
	static {
		driverMap.put("py", PythonDriver.class);
		driverMap.put("python", PythonDriver.class);
		driverMap.put("ll", LLVMDriver.class);
		driverMap.put("llvm", LLVMDriver.class);
		driverMap.put("jvm", JvmDriver.class);
		driverMap.put("jvm-debug", JvmDriver.DebuggableJvmDriver.class);
		driverMap.put("jvm-indy", JvmIndyDriver.class);
		driverMap.put("jvm-indy-debug", JvmIndyDriver.DebuggableJvmIndyDriver.class);
	}

	private static BunDriver loadDriver(String driverName) {
		if(PegDebuggerMode) {
			return new Debugger();
		}
		try {
			return (BunDriver) driverMap.get(driverName, PythonDriver.class).newInstance();
		}
		catch(Throwable t) {
			System.err.println("cannot load driver: " + driverName);
			System.err.println("instead load python driver");
		}
		return new PythonDriver();
	}

	public final static ParserContext newParserContext(PegSource source) {
		return new SimpleParserContext(source);
	}

	public final static void main(String[] args) {
		parseCommandArgument(args);
		BunDriver driver = loadDriver(DriverName);
		Namespace gamma = new Namespace(driver);
		gamma.loadPegFile("main", LanguagePeg);
		driver.initTable(gamma);
		if(InputFileName != null) {
			loadScript(gamma, driver, InputFileName);
		}
		else {
			ShellMode = true;
		}
		if(ShellMode) {
			performShell(gamma, driver);
		}
	}

	private static void loadScript(Namespace gamma, BunDriver driver, String fileName) {
		String startPoint = "TopLevel";
		PegSource source = Main.loadSource(fileName);
		parseLine(gamma, driver, startPoint, source);
	}

	private static void parseLine(Namespace gamma, BunDriver driver, String startPoint, PegSource source) {
		try {
			ParserContext context = Main.newParserContext(source);
			gamma.initParserRuleSet(context, "main");

			if(!ParseOnly) {
				driver.startTransaction(null);
			}

			ParseProfileStart();
			while(context.hasChar()) {
				PegObject node = context.parsePegObject(new PegObject(BunSymbol.TopLevelFunctor), startPoint);
				if(node.isFailure()) {
					node.tag = BunSymbol.PerrorFunctor;
				}
				gamma.set(node);
				if(PegDebuggerMode || EnableVerboseAst) {
					System.out.println("parsed:\n" + node.toString());
					if(context.hasChar()) {
						System.out.println("** uncosumed: '" + context + "' **");
					}
				}
				if(PegDebuggerMode || EnableVerbose) {
					System.out.println();
					context.showStatInfo(node);
				}
				if(!ParseOnly) {
					node = gamma.tryMatch(node, true);
					if(EnableVerbose) {
						System.out.println("Typed node: \n" + node + "\n:untyped: " + node.countUnmatched(0));
					}
					node.matched.build(node, driver);
				}
			}
			ParseProfileStop();

			if(!ParseOnly) {
				driver.endTransaction();
			}
		}
		catch (Exception e) {
			PrintStackTrace(e, source.lineNumber);
		}
	}

	static long Timer = 0;

	private static void ParseProfileStart() {
		if(ProfileMode) {
			Timer = System.currentTimeMillis();
		}
	}

	public static String getMemoryInfo() {
		String info = "";
		DecimalFormat format_mem =   new DecimalFormat("#,### KB");
		DecimalFormat format_ratio = new DecimalFormat("##.#");
		long free =  Runtime.getRuntime().freeMemory() / 1024;
		long total = Runtime.getRuntime().totalMemory() / 1024;
		long max =   Runtime.getRuntime().maxMemory() / 1024;
		long used =  total - free;
		double ratio = (used * 100 / (double)total);

		info += "Total   = " + format_mem.format(total);
		info += "\n";
		info += "Free    = " + format_mem.format(total);
		info += "\n";
		info += "use     = " + format_mem.format(used) + " (" + format_ratio.format(ratio) + "%)";
		info += "\n";
		info += "can use = " + format_mem.format(max);
		return info;
	}

	private static void ParseProfileStop() {
		if(ProfileMode) {
			System.out.println("Time    = " + (System.currentTimeMillis() - Timer) + " msec");
			System.gc();
			System.out.println(getMemoryInfo());
		}
	}

	public final static void performShell(Namespace gamma, BunDriver driver) {
		Main._PrintLine(ProgName + Version + " (" + CodeName + ") on " + Main._GetPlatform());
		Main._PrintLine(Copyright);
		int linenum = 1;
		String line = null;
		while ((line = readMultiLine(">>> ", "    ")) != null) {
			String startPoint = "TopLevel";
			if(PegDebuggerMode) {
				if(line.startsWith("?")) {
					int loc = line.indexOf(" ");
					if(loc > 0) {
						startPoint = line.substring(1, loc);
						line = line.substring(loc+1);
					}
					else {
						PegRuleSet p = gamma.root.getRuleSet("main");
						p.show(line.substring(1));
						startPoint = null;
					}
				}
			}
			if(startPoint != null) {
				PegSource source = new PegSource("(stdin)", linenum, line);
				parseLine(gamma, driver, startPoint, source);
			}
			linenum = linenum + 1;
		}
		Main._PrintLine("");
	}

	private static jline.ConsoleReader ConsoleReader = null;

	private final static String readSingleLine(String prompt) {
		try {
			return ConsoleReader.readLine(prompt);
		}
		catch(IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private final static String readMultiLine(String prompt, String prompt2) {
		if(ConsoleReader == null) {
			try {
				ConsoleReader = new jline.ConsoleReader();
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		}
		String line = readSingleLine(prompt);
		if(line == null) {
			System.exit(0);
		}
		if(prompt2 != null) {
			int level = 0;
			while((level = CheckBraceLevel(line)) > 0) {
				String line2 = readSingleLine(prompt2);
				line += "\n" + line2;
			}
			if(level < 0) {
				line = "";
				Main._PrintLine(" .. canceled");
			}
		}
		ConsoleReader.getHistory().addToHistory(line);
		return line;
	}

	private static void PrintStackTrace(Exception e, int linenum) {
		StackTraceElement[] elements = e.getStackTrace();
		int size = elements.length + 1;
		StackTraceElement[] newElements = new StackTraceElement[size];
		int i = 0;
		for(; i < size; i++) {
			if(i == size - 1) {
				newElements[i] = new StackTraceElement("<TopLevel>", "TopLevelEval", "stdin", linenum);
				break;
			}
			newElements[i] = elements[i];
		}
		e.setStackTrace(newElements);
		e.printStackTrace();
	}

	private final static int CheckBraceLevel(String Text) {
		int level = 0;
		for(int i = 0; i < Text.length(); i++) {
			char ch = Text.charAt(i);
			if(ch == '{') {
				level++;
			}
			if(ch == '}') {
				level--;
			}
		}
		return level;
	}

	// file

	public final static PegSource loadSource(String fileName) {
		//ZLogger.VerboseLog(ZLogger.VerboseFile, "loading " + FileName);
		InputStream Stream = Main.class.getResourceAsStream("/" + fileName);
		if (Stream == null) {
			try {
				Stream = new FileInputStream(fileName);
			} catch (FileNotFoundException e) {
				Main._Exit(1, "file not found: " + fileName);
				return null;
			}
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(Stream));
		try {
			StringBuilder builder = new StringBuilder();
			String line = reader.readLine();
			while(line != null) {
				builder.append(line);
				builder.append("\n");
				line = reader.readLine();
			}
			return new PegSource(fileName, 1, builder.toString());
		}
		catch(IOException e) {
			e.printStackTrace();
			Main._Exit(1, "file error: " + fileName);
		}
		return null;
	}

	public final static String _GetPlatform() {
		return "Java JVM-" + System.getProperty("java.version");
	}

	public final static String _GetEnv(String Name) {
		return System.getenv(Name);
	}

	public final static void _Print(Object msg) {
		System.err.print(msg);
	}

	public final static void _PrintLine(Object msg) {
		System.err.println(msg);
	}

	public final static void _Exit(int status, String Message) {
		System.err.println("EXIT " + Main._GetStackInfo(3) + " " + Message);
		System.exit(status);
	}

	public static boolean DebugMode = false;

	public final static void _PrintDebug(String msg) {
		if(Main.DebugMode) {
			_PrintLine("DEBUG " + Main._GetStackInfo(3) + ": " + msg);
		}
	}

	private final static String _GetStackInfo(int depth) {
		String LineNumber = " ";
		Exception e =  new Exception();
		StackTraceElement[] Elements = e.getStackTrace();
		if(depth < Elements.length) {
			StackTraceElement elem = Elements[depth];
			LineNumber += elem;
		}
		return LineNumber;
	}

	public final static boolean _IsFlag(int flag, int flag2) {
		return ((flag & flag2) == flag2);
	}

	public final static int _UnsetFlag(int flag, int flag2) {
		return (flag & (~flag2));
	}

	public final static char _GetChar(String Text, int Pos) {
		return Text.charAt(Pos);
	}

	public final static String _CharToString(char ch) {
			return ""+ch;
	}

	public static String _SourceBuilderToString(UniStringBuilder sb) {
		return Main._SourceBuilderToString(sb, 0, sb.slist.size());
	}

	public static String _SourceBuilderToString(UniStringBuilder sb, int beginIndex, int endIndex) {
		StringBuilder jsb = new StringBuilder();
		for(int i = beginIndex; i < endIndex; i = i + 1) {
			jsb.append(sb.slist.ArrayValues[i]);
		}
		return jsb.toString();
	}

	public final static void _WriteTo(String FileName, UniArray<SourceBuilder> List) {
		if(FileName == null) {
			int i = 0;
			while(i < List.size()) {
				SourceBuilder Builder = List.ArrayValues[i];
				System.out.println(Builder.toString());
				Builder.clear();
				i = i + 1;
			}
		}
		else {
			try {
				BufferedWriter w = new BufferedWriter(new FileWriter(FileName));
				int i = 0;
				while(i < List.size()) {
					SourceBuilder Builder = List.ArrayValues[i];
					w.write(Builder.toString());
					w.write("\n\n");
					Builder.clear();
					i = i + 1;
				}
				w.close();
			}
			catch(IOException e) {
				_Exit(1, "cannot to write: " + e);
			}
		}
	}

	public final static String[] _GreekNames = {
		/*"Alpha"*/ "\u03B1", "\u03B2", "\u03B3"
	};

	public final static BunType[] _NewTypeArray(int size) {
		return new BunType[size];
	}

	public final static PegObject[] _NewPegObjectArray(int size) {
		return new PegObject[size];
	}

	public final static void _ArrayCopy(Object src, int sIndex, Object dst, int dIndex, int length) {
		System.arraycopy(src, sIndex, dst, dIndex, length);
	}




}
