package org.libbun;

public abstract class BunDriver {

	public abstract void initTable(Namespace gamma);

	public abstract void startTransaction(String fileName);
	public abstract void endTransaction();
	
	public abstract void pushGlobalName(PegObject node, String name);
	public abstract void pushLocalName(PegObject node, String name);
	public abstract void pushUndefinedName(PegObject node, String name);
	public abstract void pushApplyNode(PegObject node, String name);

	public abstract void pushType(BunType type);

	// Template Engine
	public void pushNode(PegObject node) {
		if(node.matched == null) {
			SymbolTable gamma = node.getSymbolTable();
			gamma.tryMatch(node);
		}
		if(node.matched != null) {
			BunType t = node.getType(BunType.UntypedType);
			t.build(node, this);
		}
		else {
			this.pushUnknownNode(node);
		}
	}
	
	public void pushUnknownNode(PegObject node) {
		Main._PrintLine("Driver pushed unknown node: " + node.name + "\n" + node + "\n");
	}

	public void pushUpcastNode(BunType castType, PegObject node) {
		this.pushNode(node);
	}
	
	protected UniMap<DriverCommand> commandMap = new UniMap<DriverCommand>();
	
	public boolean hasCommand(String cmd) {
		return this.commandMap.hasKey(cmd);
	}

	public void addCommand(String name, DriverCommand c) {
		this.commandMap.put(name, c);
	}

	public void pushCommand(String cmd, PegObject node, String[] params) {
		DriverCommand command = this.commandMap.get(cmd);
		command.invoke(this, node, params);
	}

	public abstract void pushCode(String text);
	
	public void report(PegObject node, String errorType, String msg) {
		System.err.println(node.formatSourceMessage(errorType, msg));
	}

	public void performChecker(String checkerName, SymbolTable gamma, PegObject node) {
		return;
	}


}

abstract class SourceDriver extends BunDriver {

	private String fileName;
	protected UniStringBuilder builder;
	
	protected SourceDriver() {
		this.addCommand("push",      new PushCommand());
		this.addCommand("begin",     new OpenIndentCommand());
		this.addCommand("end",       new CloseIndentCommand());
		this.addCommand("textof",    new TextofCommand());
		this.addCommand("quote",     new QuoteCommand());
		this.addCommand("typeof",    new TypeofCommand());
		this.addCommand("statement", new StatementCommand());
		this.addCommand("list",      new ListCommand());
	}

	@Override
	public void startTransaction(String fileName) {
		this.fileName = fileName;
		this.builder = new UniStringBuilder();
	}

	@Override
	public void endTransaction() {
		this.builder.show();
		this.builder = null;
	}

	@Override
	public void pushCode(String text) {
		this.builder.append(text);
	}
	
	class PushCommand extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			driver.pushNode(node);
		}
	}

	class NewLineCommand extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			builder.appendNewLine();
		}
	}
	
	class OpenIndentCommand extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			builder.openIndent();
		}
	}

	class CloseIndentCommand extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			builder.closeIndent();
		}
	}

	
	class TextofCommand extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			driver.pushCode(node.getText());
		}
	}

	class QuoteCommand extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			String s = UniCharset._UnquoteString(node.getText());
			s = UniCharset._QuoteString("\"", s, "\"");
			driver.pushCode(s);
		}
	}

	class TypeofCommand extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			driver.pushType(node.getType(BunType.UntypedType));
		}
	}

	class StatementCommand extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			String Separator = "";
			if(param.length > 0) {
				Separator = param[0];
			}
			if(node.is("#block")) {
				for(int i = 0; i < node.size(); i++) {
					builder.appendNewLine();
					driver.pushNode(node.get(i));
					driver.pushCode(Separator);
				}
			}
			else {
				builder.appendNewLine();
				driver.pushNode(node);
				driver.pushCode(Separator);
			}
		}
	}

	class ListCommand extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			String Separator = ", ";
			if(param.length > 0) {
				Separator = param[0];
			}
			for(int i = 0; i < node.size(); i++) {
				if(i > 0) {
					driver.pushCode(Separator);
				}
				driver.pushNode(node.get(i));
			}
		}
	}
	

}