package org.libbun.drv;

import org.libbun.BunDriver;
import org.libbun.BunType;
import org.libbun.Main;
import org.libbun.Namespace;
import org.libbun.PegObject;

public class PegDumpper extends BunDriver {

	@Override
	public String getDesc() {
		return "PegDumpper 1.0 by Kimio Kuramitsu (YNU)";
	}

	// Template Engine
	public void pushNode(PegObject node) {
		Main._PrintLine(node);
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
	public void startTopLevel() {
		System.out.println();
	}

	@Override
	public void endTopLevel() {
	}



	@Override
	public void pushName(PegObject node, String name) {
	}

	@Override
	public void pushCode(String text) {
	}

	@Override
	public void pushType(BunType type) {
	}

	@Override
	public boolean hasCommand(String cmd) {
		return false;
	}

	@Override
	public void pushApplyNode(String name, PegObject args) {
	}



}




