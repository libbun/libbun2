package org.libbun;

public class SymbolTable {
	Namespace          root = null;
	PegObject          scope     = null;
	UMap<Functor>    symbolTable = null;

	public SymbolTable(Namespace namespace) {
		this.root = namespace;
	}
	SymbolTable(Namespace root, PegObject node) {
		this.root = root;
		this.setNode(node);
	}
	public void setNode(PegObject node) {
		node.gamma = this;
		this.scope = node;
	}
	
	public final Namespace getNamespace() {
		return this.root;
	}
	
	public final SymbolTable getParentTable() {
		if(this.scope != null) {
			PegObject Node = this.scope.parent;
			while(Node != null) {
				if(Node.gamma != null) {
					return Node.gamma;
				}
				Node = Node.parent;
			}
		}
		return null;
	}
	
	public final void setSymbol(String key, Functor f) {
		if(this.symbolTable == null) {
			this.symbolTable = new UMap<Functor>();
		}
		this.symbolTable.put(key, f);
		f.storedTable = this;
//		if(Main.EnableVerbose) {
//			Main._PrintLine("SET @" + this + ", name = " + key + " as " + f.funcType);
//		}
	}
	
	public final Functor getLocalSymbol(String name) {
		if(this.symbolTable != null) {
			return this.symbolTable.get(name, null);
		}
		return null;
	}
	public final Functor getSymbol(String name) {
		SymbolTable gamma = this;
		while(gamma != null) {
			Functor f = gamma.getLocalSymbol(name);
			//System.out.println("GET gamma " + gamma + ", name=" + name + " f=" + f);
			if(f != null) {
				return f;
			}
			gamma = gamma.getParentTable();
		}
		return null;
	}

	public void addFunctor(Functor f) {
		String key = f.key();
		Functor parent = this.getSymbol(key);
		f.nextChoice = parent;
		this.setSymbol(key, f);
		if(Main.VerboseMode) {
			Main._PrintLine("defined functor: " + f.name + ": " + f.funcType + " as " + key + " in " + this);
		}
	}
	
	public Functor getFunctor(PegObject node) {
		return this.getFunctor(node.tag, node.size());
	}

	public Functor getFunctor(String name, int size) {
		String key = name + ":" + size;
		Functor f = this.getSymbol(key);
		if(f != null) {
			return f;
		}
		key = name + "*";
		f = this.getSymbol(key);
//		if(f == null && Main.EnableVerbose) {
//			Main._PrintLine("SymbolTable.getFunctor(): not found " + node.name + ":" + node.size());
//		}
		return f;
	}
	
	public final PegObject typeErrorNode(PegObject errorNode, String message, boolean isStrongTyping) {
		if(isStrongTyping) {
			PegObject o = new PegObject("#error", errorNode.source, null, errorNode.startIndex);
			o.matched = Functor.ErrorFunctor;
			o.typed = BunType.newErrorType(message);
			if(Main.VerboseMode) {
				Main._PrintLine(errorNode.formatSourceMessage("debug", message));
			}
			return o;
		}
		errorNode.matched = null;
		errorNode.typed = null;
		return errorNode;
	}

	public final PegObject check(PegObject node, boolean isStrongTyping) {
		return this.root.checker.check(this, node, isStrongTyping);
	}

	public final PegObject tryMatch(PegObject node, boolean isStrongTyping) {
		if(node.matched == null) {
			return this.tryMatchImpl("tag", node.tag, this.getFunctor(node), node, isStrongTyping);
		}
		return node;
	}
	
	public final PegObject tryMatchImpl(String type, String name, Functor f, PegObject node, boolean isStrongTyping) {
		Functor cur = f;
		while(cur != null) {
			boolean isStrongTyping2 = isStrongTyping;
			if(cur.nextChoice != null) {
				isStrongTyping2 = false;
			}
			node = cur.matchSubNode(this, node, isStrongTyping2);
			if(node.matched != null) {
				return node;
			}
			cur = cur.nextChoice;
		}
		String errorMessage = (f == null) ? "undefined " : "mismatched ";
		return this.typeErrorNode(node, errorMessage + type + ": " + name, isStrongTyping);
	}
	
	public final boolean checkTypeAt(PegObject node, int index, BunType type, boolean isStrongTyping) {
		//System.out.println("@@ " + node.get(index).tag + ", " + index + ", " + type + " untyped?" + (type == BunType.UntypedType));
		if(type == BunType.UntypedType) {  // UntypedType does not invoke tryMatch()
			return true;
		}
		if(index < node.size()) {
			PegObject subnode = node.get(index);
			subnode = this.tryMatch(subnode, isStrongTyping);
			node.AST[index] = subnode;
			subnode.parent = node;
			if(type.accept(this, subnode, isStrongTyping)) {
				return true;
			}
			return false;
		}
		else {
			BunType valueType = BunType.VoidType;
			return type.is(valueType);
		}
	}
		
	private BunType getTypeCoersion(BunType sourceType, BunType targetType, boolean hasNextChoice) {
		String key = BunType.keyTypeRel("#cast", sourceType, targetType);
		Functor f = this.getSymbol(key);
		if(f != null) {
			if(Main.VerboseMode) {
				Main._PrintLine("found type coercion from " + sourceType + " to " + targetType);
			}
			return BunType.newTransType(key, sourceType, targetType, f);
		}
		if(hasNextChoice) {
			return null;
		}
		return null;
	}

	class DefinedTypeFunctor extends Functor {
		public DefinedTypeFunctor(String name, BunType type) {
			super(Functor._SymbolFunctor, name, type);
		}
		@Override
		public void build(PegObject node, BunDriver driver) {
			driver.pushType(node.getType(BunType.UntypedType));
		}
	}
	
	public Functor setType(BunType type) {
		String name = type.getName();
		Functor defined = this.getSymbol(type.getName());
		if(defined != null) {
			System.out.println("duplicated name:" + name);
		}
		defined = new DefinedTypeFunctor(name, type);
		this.setSymbol(name, defined);
		return defined;
	}

	public BunType getType(String name, BunType defaultType) {
		Functor f = this.getSymbol(name);
		if(f != null) {
			return f.getReturnType(defaultType);
		}
//		if(f instanceof DefinedTypeFunctor) {
//			return f.getReturnType(defaultType);
//		}
		if(Main.VerboseMode) {
			Main._PrintLine("FIXME: undefined type: " + name);
		}
		return defaultType;
	}
	
	public void load(String fileName, BunDriver driver) {
		ParserContext context = Main.newParserContext(Main.loadSource(fileName));
		this.root.initParserRuleSet(context, null);
		while(context.hasNode()) {
			context.initMemo();
			PegObject node = context.parseNode("TopLevel");
			node.gamma = this;
			node = this.tryMatch(node, true);
			driver.pushNode(node);
		}
	}

	public final void report(PegObject node, String errorType, String msg) {
		if(this.root.driver != null) {
			this.root.driver.report(node, errorType, msg);
		}
	}

	// ======================================================================
	
	// #type

	class TypeFunctor extends Functor {
		public TypeFunctor(String name) {
			super(Functor._SymbolFunctor, name, null);
		}

//		@Override
//		protected void matchSubNode(PegObject node, boolean hasNextChoice) {
//			if(node.isEmptyToken()) {
//				node.typed = BunType.newVarType(node);
//			}
//			else {
//				String typeName = node.getText();
//				SymbolTable gamma = node.getSymbolTable();
//				node.typed = gamma.getType(typeName, null);
//			}
//			node.matched = this;
//		}

		@Override
		public void build(PegObject node, BunDriver driver) {
			driver.pushType(node.getType(BunType.UntypedType));
		}
	}

	public void loadBunModel(String fileName, BunDriver driver) {
		this.addFunctor(new TypeFunctor("#type"));
		this.addFunctor(new BunTypeDefFunctor("#bun.typedef"));
		this.addFunctor(new BunTemplateFunctor("#bun"));
		this.load(fileName, driver);
	}
	
	public void addUpcast(BunType fromType, BunType toType) {
		class UpcastFunctor extends Functor {
			public UpcastFunctor(BunType fromType, BunType toType) {
				super(0, "#cast", BunType.newFuncType(fromType, toType));
			}
			@Override
			public void build(PegObject node, BunDriver driver) {
				driver.pushUpcastNode(this.funcType.getReturnType(), node);
			}			
		}
		UpcastFunctor f = new UpcastFunctor(fromType, toType);
		this.addFunctor(f);
	}

}

