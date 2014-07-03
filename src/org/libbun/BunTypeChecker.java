package org.libbun;

public class BunTypeChecker {
	// SymbolTable
	private class DefinedNameFunctor extends Functor {
		PegObject defNode = null;
		public DefinedNameFunctor(int flag, String name, BunType type, PegObject defNode) {
			super(flag, name, type);
			this.defNode = defNode;
		}
		@Override
		public void build(PegObject node, BunDriver driver) {
			driver.pushLocalName(node, this.name);
		}
	}
	private DefinedNameFunctor getName(SymbolTable gamma, String name) {
		Functor f = gamma.getSymbol(name);
		if(f instanceof DefinedNameFunctor) {
			return (DefinedNameFunctor)f;
		}
		return null;
	}
	private Functor setName(SymbolTable gamma, int flag, PegObject nameNode, BunType type, PegObject defNode) {
		String name = nameNode.getText();
		Functor defined = gamma.getLocalSymbol(name);
		if(defined != null) {
			gamma.report(nameNode, "notice", "duplicated name: " + name);
		}
		type = type.getRealType();
//		if(type.hasVarType()) {
//			gamma.report(nameNode, "notice", "ambigious variable: " + name + " :" + type.getName());
//		}
		if(nameNode.matched == null) {
			nameNode.matched = gamma.getSymbol("#name:0");
		}
		if(nameNode.typed == null) {
			nameNode.typed = type;
		}
		DefinedNameFunctor f = new DefinedNameFunctor(flag, name, type, defNode);
		gamma.setSymbol(name, f);
		return f;
	}
	private Functor setName(SymbolTable gamma, int flag, String name, BunType type) {
		DefinedNameFunctor f = new DefinedNameFunctor(flag, name, type.getRealType(), null);
		gamma.setSymbol(name, f);
		return f;
	}
	private class DefinedFunctionFunctor extends DefinedNameFunctor {
		public DefinedFunctionFunctor(String name, BunType type, PegObject funcNode, Functor defined) {
			super(0, name, type, funcNode);
			this.nextChoice = defined;
		}
		@Override 
		public void build(PegObject node, BunDriver driver) {
			driver.pushApplyNode(node, name);
		}
	}
	private boolean checkReturnStatement(PegObject block) {
		for(int i = 0; i < block.size(); i++) {
			PegObject stmt = block.get(i);
			if(stmt.is("#return")) {
				return true;
			}
			if(stmt.is("#if") && stmt.size() == 3) {
				if(checkReturnStatement(stmt.get(1)) && checkReturnStatement(stmt.get(2))) {
					return true;
				}
			}
		}
		if(block.is("#return")) {
			return true;
		}
		return false;
	}
	private Functor setFunctionName(SymbolTable gamma, String name, BunType type, PegObject node) {
		Functor defined = gamma.getSymbol(name);
		type = type.getRealType();
		defined = new DefinedFunctionFunctor(name, type, node, defined);
		if(Main.EnableVerbose) {
			Main._PrintLine(name + " :" + type.getName() + " initValue=" + node);
		}
		gamma.setSymbol(name, defined);
		return defined;
	}

//
//class DefinedGlobalNameFunctor extends DefinedNameFunctor {
//	public DefinedGlobalNameFunctor(String name, BunType type, PegObject initValue) {
//		super(name, type, initValue);
//	}
//	@Override
//	public void build(PegObject node, BunDriver driver) {
//		driver.pushGlobalName(node, this.name);
//	}
//}
//
//
//
//public Functor setName(String name, BunType type, PegObject initValue) {
//	return this.setName(false, name, null, type, initValue);
//}
//
//public Functor setName(PegObject nameNode, BunType type, PegObject initValue) {
//	return this.setName(false, nameNode.getText(), nameNode, type, initValue);
//}
//
//public Functor setGlobalName(PegObject nameNode, BunType type, PegObject initValue) {
//	return this.setName(true, nameNode.getText(), nameNode, type, initValue);
//}
//
//
//

	abstract class TypeChecker {
		public abstract PegObject check(SymbolTable gamma, PegObject node, boolean isStrongTyping);
	}
	class TvarChecker extends TypeChecker {
		@Override
		public PegObject check(SymbolTable gamma, PegObject node, boolean isStrongTyping) {
			node.typed = BunType.newVarType();
			return node;
		}
	}
	class VarChecker extends TypeChecker {
		@Override
		public PegObject check(SymbolTable gamma, PegObject node, boolean isStrongTyping) {
			int flag = Functor._SymbolFunctor;
			BunType type = node.typeAt(gamma, 1, BunType.UntypedType);
			gamma.checkTypeAt(node, 2, type, true);
			if(node.findParentNode("#function") == null) {
			}
			else {
				PegObject block = node.findParentNode("#block");
				gamma = block.getLocalSymbolTable();
				flag |= Functor._ReadOnlyFunctor;
			}
			PegObject nameNode = node.get(0);
			setName(gamma, flag, nameNode, type, node);
//			nameNode.matched = gamma.getSymbol("#name:0");
//			nameNode.typed = type;
			return node;
		}
	}
	class NameChecker extends TypeChecker {
		@Override
		public PegObject check(SymbolTable gamma, PegObject node, boolean isStrongTyping) {
			String name = node.getText();
			DefinedNameFunctor f = getName(gamma, name);
			if(f != null) {
				node.matched = f;
				node.typed = f.getReturnType(BunType.UntypedType);
				return node;
			}
			return gamma.typeErrorNode(node, "undefined name: " + name, isStrongTyping);
		}
	}
	class ParamChecker extends TypeChecker {
		@Override
		public PegObject check(SymbolTable gamma, PegObject node, boolean isStrongTyping) {
			gamma.checkTypeAt(node, 1, BunType.AnyType, isStrongTyping);
			BunType type = node.typeAt(gamma, 1, BunType.UntypedType);
			PegObject nameNode = node.get(0);
			setName(gamma, Functor._SymbolFunctor|Functor._LocalFunctor, nameNode, type, node);
			return node;
		}
	}
	class FunctionChecker extends TypeChecker {
		@Override
		public PegObject check(SymbolTable gamma, PegObject node, boolean isStrongTyping) {
			PegObject block = node.get(3, null);
			if(block.gamma == null) {
				//System.out.println("**1 first time");
				SymbolTable blockgamma = block.getLocalSymbolTable();
				blockgamma.checkTypeAt(node, 1, BunType.VoidType, isStrongTyping);
				PegObject params = node.get(1, null);
				UniArray<BunType> typeList = new UniArray<BunType>(new BunType[params.size()+1]);
				for(int i = 0; i < params.size(); i++) {
					PegObject p = params.get(i);
					BunType ptype = p.typeAt(gamma, 1, null);
					typeList.add(ptype);
				}
				blockgamma.checkTypeAt(node, 2, BunType.AnyType, isStrongTyping);
				BunType returnType = node.typeAt(gamma, 2, null);
				typeList.add(returnType);
				node.typed = BunType.newFuncType(typeList);
				setName(blockgamma, Functor._SymbolFunctor|Functor._LocalFunctor, node.get(0), node.typed, node);
				setName(blockgamma, Functor._SymbolFunctor|Functor._LocalFunctor, "return", returnType);
				if(!checkReturnStatement(block)) {
					block.append(new PegObject("#return"));
				}
				block = blockgamma.tryMatch(block, false);
				node.set(3, block);
				setFunctionName(gamma, node.textAt(0, "f"), node.typed, node);
			}
			else {
				System.out.println("** second time");
				node.set(3, block.gamma.tryMatch(block, isStrongTyping));
			}
			return node;
		}
	}
	class Return0Checker extends TypeChecker {
		@Override
		public PegObject check(SymbolTable gamma, PegObject node, boolean isStrongTyping) {
			DefinedNameFunctor f = getName(gamma, "return");
			BunType type = f.funcType.getRealType();
			//System.out.println("return0 = " + type);
			gamma.checkTypeAt(node, 0, type, isStrongTyping);
			return node;
		}
	}
	class Return1Checker extends TypeChecker {
		@Override
		public PegObject check(SymbolTable gamma, PegObject node, boolean isStrongTyping) {
			DefinedNameFunctor f = getName(gamma, "return");
			BunType type = f.funcType.getRealType();
			//System.out.println("return1 = " + type);
			gamma.checkTypeAt(node, 0, type, isStrongTyping);
			return node;
		}
	}
	class BlockChecker extends TypeChecker {
		@Override
		public PegObject check(SymbolTable gamma, PegObject node, boolean isStrongTyping) {
			for(int i = 0; i < node.size(); i++) {
				gamma.checkTypeAt(node, i, BunType.VoidType, isStrongTyping);
			}
			return node;
		}
	}

	
	//	class ApplyChecker extends TypeChecker {
//		@Override
//		public void check(PegObject node) {
//			System.out.println("typeCheckApply: " + node);
////			SymbolTable gamma = node.getSymbolTable();
//		}
//	}
//	class FuncDecl extends TypeChecker {
//		@Override
//		public void check(PegObject node) {
//			//System.out.println("FuncDeclCommand node: " + node);
//			SymbolTable gamma = node.getSymbolTable();
//			for(int i = 0; i < node.size(); i++) {
//				gamma.checkTypeAt(node, i, BunType.VoidType, false);
//			}
//			DefinedNameFunctor f = gamma.getName("return");
//			BunType returnType = f.getReturnType(BunType.UntypedType);
//			System.out.println("returnType="+returnType);
//			gamma.checkTypeAt(node, 0, returnType, false);
//		}
//	}
//	class ApplyChecker extends TypeChecker {
//		@Override
//		public void check(PegObject node) {
//			public static void typeCheckReturn(PegObject node) {
//				//System.out.println("FuncDeclCommand node: " + node);
//				SymbolTable gamma = node.getSymbolTable();
//				DefinedNameFunctor f = gamma.getName("return");
//				BunType returnType = f.getReturnType(BunType.UntypedType);
//				System.out.println("returnType="+returnType);
//				gamma.checkTypeAt(node, 0, returnType, false);
//			}
//		}
//	}
	
	private UniMap<TypeChecker> checkerMap = null;
	
	public BunTypeChecker() {
		if(this.checkerMap == null) {
			this.checkerMap = new UniMap<TypeChecker>();
			this.set("#Tvar:0", new TvarChecker());
			this.set("#var:3",  new VarChecker());
			this.set("#let:3",  new VarChecker());
			this.set("#name:0", new NameChecker());
			
			this.set("#function:4", new FunctionChecker());
			this.set("#param:2", new ParamChecker());
			this.set("#return:0", new Return0Checker());
			this.set("#return:1", new Return1Checker());
			this.set("#block:*",  new BlockChecker());
			this.set("#params:*",  this.checkerMap.get("#block:*"));
		}
	}

	public final void set(String nodeKey, TypeChecker c) {
		this.checkerMap.put(nodeKey, c);
	}

	public final PegObject check(SymbolTable gamma, PegObject node, boolean isStrongTyping) {
		String nodeKey = node.tag + ":" + node.size();
		TypeChecker c = this.checkerMap.get(nodeKey, null);
		if(c == null) {
			c = this.checkerMap.get(node.tag+":*", null);
		}
		if(c != null) {
			if(Main.EnableVerbose) {
				System.out.println("Using extended type checker: " + nodeKey);
			}
			return c.check(gamma, node, isStrongTyping);
		}
		if(Main.EnableVerbose) {
			System.out.println("Unusing extended type checker: '" + nodeKey + "' c =" + c);
		}
		return node;
	}
	
}
