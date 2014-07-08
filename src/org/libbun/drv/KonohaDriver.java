package org.libbun.drv;

import org.libbun.BunType;
import org.libbun.Namespace;
import org.libbun.PegObject;

public class KonohaDriver extends SourceDriver {

	@Override
	public String getDesc() {
		return "Konoha source generator 1.0 by Kimio Kuramitsu (YNU)";
	}

	@Override
	public void initTable(Namespace gamma) {
		gamma.loadBunModel("lib/driver/python/common.bun", this);
		gamma.loadBunModel("lib/driver/python/pytypes.bun", this);
	}

	@Override
	public void pushType(BunType type) {
		this.pushCode(type.getName());
	}

	public void pushApplyNode(String name, PegObject args) {
		this.pushCode(name);
		this.pushNodeList("(", args, ", ", ")");
	}

	public void generateMain() {  // Override
		if(this.hasMainFunction()) {
			this.pushCode("\nif __name__ == '__main__':\n\tmain()\n");
		}
	}

}
