package org.libbun.drv;

import org.libbun.BunDriver;
import org.libbun.BunType;
import org.libbun.DriverCommand;
import org.libbun.Main;
import org.libbun.PegObject;
import org.libbun.UCharset;
import org.libbun.UList;
import org.libbun.UStringBuilder;

abstract class SourceDriver extends BunDriver {

	private String fileName;
	protected UStringBuilder builder;
	
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
		this.builder = new UStringBuilder();
	}

	@Override
	public void endTransaction() {
		UList<UStringBuilder> list = new UList<UStringBuilder>(new UStringBuilder[1]);
		list.add(this.builder);
		Main._WriteFile(fileName, list);
		this.builder = null;
	}

	@Override
	public void startTopLevel() {
		this.builder.appendNewLine();
	}

	@Override
	public void endTopLevel() {
	}
	
	@Override
	public void pushCode(String text) {
		this.builder.append(text);
	}
	
	public final void pushNodeList(String openToken, PegObject args, String comma, String closeToken) {
		this.builder.append(openToken);
		for(int i = 0; i < args.size(); i++) {
			if(i > 0) {
				this.builder.append(comma);
			}
			this.pushNode(args.get(i));
		}
		this.builder.append(closeToken);
	}

	@Override
	public void pushName(PegObject node, String name) {
		this.builder.append(name);
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
			String s = UCharset._UnquoteString(node.getText());
			s = UCharset._QuoteString("\"", s, "\"");
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