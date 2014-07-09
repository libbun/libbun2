package org.libbun;

public abstract class BunDriver {

	public abstract String getDesc();
	public abstract void initTable(Namespace gamma);

	public abstract void startTransaction(String fileName);
	public abstract void endTransaction();
	public abstract void startTopLevel();
	public abstract void endTopLevel();

	public abstract void pushType(BunType type);   // deprecated
	public abstract void pushName(PegObject nameNode, String name);
	public abstract void pushApplyNode(String name, PegObject args);

	// Template Engine
	public void pushNode(PegObject node) {
		if(node.matched == null) {
			SymbolTable gamma = node.getSymbolTable();
			node = gamma.tryMatch(node, true);
		}
		if(node.matched != null) {
			BunType t = node.getType(BunType.UntypedType);
			t.build(node, this);
		}
		else {
			this.pushUnknownNode(node);
		}
	}

	public void pushUpcastNode(BunType castType, PegObject node) {
		this.pushNode(node);
	}
	
	public void pushErrorNode(PegObject node) {
		if(node.optionalToken == null) {
			node.optionalToken = "syntax error";
		}
		this.report(node, "error", node.optionalToken);
	}

	public void pushUnknownNode(PegObject node) {
		this.report(node, "error", "undefined tag: " + node.tag);
		if(node.size() > 0) {
			for(int i = 0; i < node.size(); i++) {
				this.pushNode(node.get(i));
			}
		}
		else {
			this.pushCode(node.getText());
		}
	}

	public abstract void pushCode(String text);

	protected UMap<DriverCommand> commandMap = new UMap<DriverCommand>();
	
	public boolean hasCommand(String cmd) {
		return this.commandMap.hasKey(cmd);
	}

	public void addCommand(String name, DriverCommand c) {
		this.commandMap.put(name, c);
	}

	public void pushCommand(String cmd, PegObject node, String[] params) {
		DriverCommand command = this.commandMap.get(cmd);
		if(command != null) {
			command.invoke(this, node, params);
		}
		else {
			Main._Exit(1, "unknown command: " + cmd);
		}
	}

	public String rename(int flag, String name) {
		if(!Main._IsFlag(flag, Functor._SymbolFunctor) && name.equals("main")) {
			hasMainFunc = true;
		}
		return name;
	}
	
	protected boolean hasMainFunc = false;
	protected final boolean hasMainFunction() {
		return hasMainFunc;
	}

	public void generateMain() {  // Override
	}
	
	public void report(PegObject node, String errorType, String msg) {
		System.err.println(node.formatSourceMessage(errorType, msg));
	}





}