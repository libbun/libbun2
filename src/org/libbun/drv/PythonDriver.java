package org.libbun.drv;

import org.libbun.BunType;
import org.libbun.Functor;
import org.libbun.Main;
import org.libbun.Namespace;
import org.libbun.PegObject;
import org.libbun.UMap;

public class PythonDriver extends SourceDriver {

	@Override
	public String getDesc() {
		return "Python source generator 1.0 by Kimio Kuramitsu (YNU)";
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
			this.pushCode("\nif __name__ == '__main__':\n   main()\n");
		}
	}

	/**
	 * Python does not support polymorphic function definitions.
	 * The rename methods rename in case of function name; 
	 */
	
	private UMap<Integer> FunctionNameMap = new UMap<Integer>();
	
	public String rename(int flag, String name) {
		if(!Main._IsFlag(flag, Functor._SymbolFunctor)) {
			if(name.equals("main")) {
				hasMainFunc = true;
			}
			Integer n = this.FunctionNameMap.get(name);
			if(n == null) {
				this.FunctionNameMap.put(name, 0);
			}
			else {
				n = n + 1;
				this.FunctionNameMap.put(name, n);
				name = name + "_r" + n;
			}
		}
		return name;
	}



}
