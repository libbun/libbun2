package org.libbun;

public class Debugger extends BunDriver {

	// Template Engine
	public void pushNode(PegObject node) {
		if(node.isFailure() || node.is("#error")) {
			String msg = node.textAt(0, "syntax error by peg " + node.createdPeg);
			this.report(node, "error", msg);
		}
		else {
			Main._PrintLine("Parsed: " + node.tag + "\n" + node + "\n");
		}
	}
	
	public void pushUnknownNode(PegObject node) {
		this.pushNode(node);
	}

	@Override
	public void initTable(Namespace gamma) {
	}

	@Override
	public void startTransaction(String fileName) {
	}

	@Override
	public void endTransaction() {
	}


	@Override
	public void pushGlobalName(PegObject node, String name) {
	}

	@Override
	public void pushLocalName(PegObject node, String name) {
	}

	@Override
	public void pushCode(String text) {
	}

	@Override
	public void pushType(BunType type) {
	}

	@Override
	public void pushUndefinedName(PegObject node, String name) {
	}

	@Override
	public boolean hasCommand(String cmd) {
		return false;
	}

	@Override
	public void pushApplyNode(PegObject node, String name) {
	}


}




