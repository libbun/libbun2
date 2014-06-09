package org.libbun;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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

	// --verbose
	public static boolean EnableVerbose = false;
	
	// --verbose:peg
	public static boolean PegDebuggerMode = false;

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
			else if(argument.startsWith("--verbose")) {
				EnableVerbose = true;
//				if(argument.equals("--verbose:peg")) {
//					pegDebugger = true;
//				}
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
		Main._Exit(0, Message);
	}
	
	//	private final static CommonMap<Class<?>> ClassMap = new CommonMap<Class<?>>(null);
	//
	//	static {
	//		ClassMap.put("syntax::bun", libbun.lang.bun.BunGrammar.class);
	//		ClassMap.put("syntax::bun.extra", libbun.lang.bun.extra.BunExtraGrammar.class);
	//		ClassMap.put("syntax::bun.regex", libbun.lang.bun.regexp.RegExpGrammar.class);
	//		ClassMap.put("syntax::bun.shell", libbun.lang.bun.shell.ShellGrammar.class);
	//
	//		ClassMap.put("syntax::lisp",   libbun.lang.lisp.LispGrammar.class);
	//		//		ClassMap.put("syntax::konoha", libbun.lang.konoha.KonohaGrammar.class);
	//		ClassMap.put("syntax::python", libbun.lang.python.PythonGrammar.class);
	//
	//		// source code by file extension
	//		ClassMap.put("bash", libbun.encode.devel.BashGenerator.class);
	//		ClassMap.put("bun", libbun.encode.playground.BunGenerator.class);
	//		ClassMap.put("c",   libbun.encode.playground.CGenerator.class);
	//		ClassMap.put("cl",  libbun.encode.playground.CommonLispGenerator.class);
	//		ClassMap.put("cs",  libbun.encode.release.CSharpGenerator.class);
	//		ClassMap.put("csharp-playground",  libbun.encode.playground.CSharpGenerator.class);
	//		ClassMap.put("erl", libbun.encode.erlang.ErlangGenerator.class);
	//
	//		ClassMap.put("hs",  libbun.encode.haskell.HaskellSourceGenerator.class);
	//		ClassMap.put("java", libbun.encode.playground.JavaGenerator.class);
	//		ClassMap.put("js",  libbun.encode.release.JavaScriptGenerator.class);
	//		ClassMap.put("javascript-playground",  libbun.encode.playground.JavaScriptGenerator.class);
	//
	//		ClassMap.put("lua",  libbun.encode.devel.LuaGenerator.class);
	//
	//		ClassMap.put("pl",  libbun.encode.obsolete.PerlGenerator.class);
	//		ClassMap.put("py", libbun.encode.release.PythonGenerator.class);
	//		ClassMap.put("python-playground", libbun.encode.playground.PythonGenerator.class);
	//		ClassMap.put("r", libbun.encode.playground.RGenerator.class);
	//		ClassMap.put("rb", libbun.encode.devel.RubyGenerator.class);
	//		ClassMap.put("sml", libbun.encode.devel.SMLSharpGenerator.class);
	//
	//		ClassMap.put("vba", libbun.encode.devel.VBAGenerator.class);
	//
	//		//
	//		ClassMap.put("ssac", libbun.encode.devel.SSACGenerator.class);
	//
	//		// engine
	//		ClassMap.put("jvm", libbun.encode.jvm.AsmJavaGenerator.class);
	//		ClassMap.put("debug-jvm", libbun.encode.jvm.DebugAsmGenerator.class);
	//		ClassMap.put("dump-jvm", libbun.encode.jvm.ByteCodePrinter.class);
	//		ClassMap.put("ll", libbun.encode.llvm.LLVMSourceGenerator.class);
	//
	//	}
	//
	//	public final static boolean _LoadGrammar(LibBunGamma Gamma, String ClassName) {
	//		try {
	//			Class<?> GrammarClass =  ClassMap.GetOrNull(ClassName.toLowerCase());
	//			if(GrammarClass == null) {
	//				GrammarClass = Class.forName(ClassName);
	//			}
	//			Method LoaderMethod = GrammarClass.getMethod("LoadGrammar", LibBunGamma.class);
	//			LoaderMethod.invoke(null, Gamma);
	//			return true;
	//		} catch (Exception e) { // naming
	//			e.printStackTrace();
	//		}
	//		return false;
	//	}


	private static BunDriver loadDriver(String driverName) {
		if(PegDebuggerMode) {
			return new Debugger();
		}
		BunDriver driver = new PythonDriver();
		return driver;
	}

	public final static void main(String[] args) {
		parseCommandArgument(args);
		PegParser p = new PegParser();
		p.loadPegFile(LanguagePeg);
		BunDriver driver = loadDriver(DriverName);
		Namespace gamma = new Namespace(p, driver);
		driver.initTable(gamma);
		if(InputFileName == null) {
			performShell(gamma, driver);
		}
		else {
			evalScript(gamma, driver);
		}
	}

	public final static void evalScript(Namespace gamma, BunDriver driver) {
		InputStream stream = null;
		if(PegDebuggerMode) {
			File inputFile = new File(InputFileName);
			try {
				stream = new FileInputStream(inputFile);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			String buffer = "";
			try {
				int buflen = 4096;
				int readed = 0;
				char[] buf = new char[buflen];
				StringBuilder builder = new StringBuilder();
				while ((readed = reader.read(buf, 0, buflen)) >= 0) {
					builder.append(buf, 0, readed);
				}
				buffer = builder.toString();
			} catch (IOException e) {
				e.printStackTrace();
			}
			PegSource source = new PegSource(inputFile.getName(), 1, buffer);
			PegParserContext context =  gamma.root.newParserContext("main", source);
			PegObject node = context.parsePegNode(new PegObject(BunSymbol.TopLevelFunctor), "TopLevel");
			node.gamma = gamma;
			if(PegDebuggerMode) {
				System.out.println("parsed:\n" + node.toString());
				if(context.hasChar()) {
					System.out.println("** uncosumed: '" + context + "' **");
				}
				System.out.println("hit: " + context.memoHit + ", miss: " + context.memoMiss + ", object=" + context.objectCount + ", error=" + context.errorCount);
				System.out.println("backtrackCount: " + context.backtrackCount + ", backtrackLength: " + context.backtrackSize);
				System.out.println();
			}
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
						PegParser p = gamma.root.getParser("main");
						p.show(line.substring(1));
						startPoint = null;
					}
				}
			}
			if(startPoint != null) {
				try {
					PegSource source = new PegSource("(stdin)", linenum, line);
					PegParserContext context =  gamma.root.newParserContext("main", source);
					PegObject node = context.parsePegNode(new PegObject(BunSymbol.TopLevelFunctor), startPoint);
					if(node.isFailure()) {
						node.name = BunSymbol.PerrorFunctor;
					}
					gamma.set(node);
					if(PegDebuggerMode) {
						System.out.println("parsed:\n" + node.toString());
						if(context.hasChar()) {
							System.out.println("** uncosumed: '" + context + "' **");
						}
						System.out.println("hit: " + context.memoHit + ", miss: " + context.memoMiss + ", object=" + context.objectCount + ", error=" + context.errorCount);
						System.out.println("backtrackCount: " + context.backtrackCount + ", backtrackLength: " + context.backtrackSize);
						System.out.println();
					}
					if(driver != null) {
						driver.startTransaction(null);
						if(gamma.tryMatch(node)) {
							node.matched.build(node, driver);
						}
						else {
							driver.pushUnknownNode(node);
						}
						driver.endTransaction();
					}
				}
				catch (Exception e) {
					PrintStackTrace(e, linenum);
				}
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

	public final static MetaType[] _NewTypeArray(int size) {
		return new MetaType[size];
	}

	public final static PegObject[] _NewPegObjectArray(int size) {
		return new PegObject[size];
	}

	public final static void _ArrayCopy(Object src, int sIndex, Object dst, int dIndex, int length) {
		System.arraycopy(src, sIndex, dst, dIndex, length);
	}



}
