package org.libbun;

public class PythonDriver extends SourceDriver {

	@Override
	public void initTable(Namespace gamma) {
		gamma.loadBunModel("lib/driver/python/konoha.bun", this);
		gamma.loadBunModel("lib/driver/python/python_types.bun", this);
	}

	@Override
	public void pushGlobalName(PegObject node, String name) {
		this.pushCode(name);
	}

	@Override
	public void pushLocalName(PegObject node, String name) {
		this.pushCode(name);
	}

	@Override
	public void pushUndefinedName(PegObject node, String name) {
		this.pushCode(name);
	}

	@Override
	public void pushType(BunType type) {
		this.pushCode(type.getName());
	}

	@Override
	public void pushApplyNode(PegObject node, String name) {
		// TODO Auto-generated method stub
		
	}
}
