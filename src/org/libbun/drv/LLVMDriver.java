package org.libbun.drv;

import java.util.HashMap;

import org.libbun.BunDriver;
import org.libbun.BunType;
import org.libbun.DriverCommand;
import org.libbun.Namespace;
import org.libbun.PegObject;
import org.libbun.UList;
import org.libbun.UStringBuilder;

public class LLVMDriver extends SourceDriver {
	private int UniqueNumber = 0;
	private final UList<UStringBuilder> StmtStack = new UList<UStringBuilder>(new UStringBuilder[100]);
	private final HashMap<PegObject, Integer> TempVarMap = new HashMap<PegObject, Integer>();

	@Override
	public String getDesc() {
		return "LLVM IR generator by Kouhei Moriya (YNU)";
	}

	@Override
	public void initTable(Namespace gamma) {
		this.addCommand("begin",      new LLVMOpenIndentCommand());
		this.addCommand("end",        new LLVMCloseIndentCommand());
		this.addCommand("startstmt",  new StartStatementCommand());
		this.addCommand("endstmt",    new EndStatementCommand());
		this.addCommand("reservenum", new ReserveNumberCommand());
		this.addCommand("getnum",     new GetNumberCommand());
		gamma.loadBunModel("lib/driver/llvm/konoha.bun", this);
		//gamma.loadBunModel("lib/driver/python/python_types.bun", this);
	}

	@Override
	public void startTransaction(String fileName) {
		super.startTransaction(fileName);
		this.UniqueNumber = 0;
		this.StmtStack.clear(0);
		this.TempVarMap.clear();
	}

	@Override
	public void pushName(PegObject node, String name) {
		this.pushCode(name);
	}

	@Override
	public void pushType(BunType type) {
		this.pushCode(type.getName());
	}

	@Override
	public void pushApplyNode(String name, PegObject args) {
		// TODO Auto-generated method stub
		
	}

	class LLVMOpenIndentCommand extends OpenIndentCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			builder.openIndent("{");
		}
	}

	class LLVMCloseIndentCommand extends CloseIndentCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			builder.closeIndent("}");
		}
	}

	class StartStatementCommand extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			StmtStack.add(builder);
			builder = new UStringBuilder();
		}
	}

	class EndStatementCommand extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			int stackSize = StmtStack.size();
			assert(stackSize > 0);
			StmtStack.ArrayValues[0].append(builder.toString());
			if(stackSize > 1) {
				StmtStack.ArrayValues[0].appendNewLine();
			}
			builder = StmtStack.ArrayValues[stackSize - 1];
			StmtStack.clear(stackSize - 1);
		}
	}

	class ReserveNumberCommand extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			if(param.length > 0) {
				TempVarMap.put(node, new Integer(UniqueNumber));
				UniqueNumber += (new Integer(param[0])).intValue();				
			}
		}
	}

	class GetNumberCommand extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			if(param.length > 0) {
				driver.pushCode("" + (TempVarMap.get(node).intValue() + (new Integer(param[0])).intValue()));
			}
		}
	}

	/* class ArgListCommand extends ListCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			for(int i = 0; i < node.size(); i++) {
				if(i > 0) {
					driver.pushCode(", ");
				}
				driver.pushType(node.getType(BunType.UntypedType));
				driver.pushCode(" ");
				driver.pushNode(node.get(i));
			}
		}
	} */
}
