package org.libbun;

public abstract class BunType  {
	protected int         typeId    = -1;
	protected Object      typeInfo  = null;  // used in Class<?> in JVM (for example)
	protected String      tag;
	BunType(String tag) {
		this.tag = tag;
		this.typeInfo = null;
	}
	protected final boolean isIssued() {
		return typeId != -1;
	}
	public final String getName() {
		UStringBuilder sb = new UStringBuilder();
		this.stringfy(sb);
		return sb.toString();
	}
	public final String toString() {
		UStringBuilder sb = new UStringBuilder();
		this.stringfy(sb);
		return sb.toString();
	}
	
	protected final void stringfy(UStringBuilder sb, String openToken, String delimToken, String closeToken) {
		sb.append(openToken);
		for(int i = 0; i < this.size(); i++) {
			if(i > 0) {
				sb.append(delimToken);
			}
			this.get(i).stringfy(sb);
		}
		sb.append(closeToken);		
	}
	
	public final PegObject peg() {
		BunType type = this.getRealType();
		PegObject o = new PegObject(type.tag, null, null, 0);
		for(int i = 0; i < type.size(); i++) {
			o.append(type.get(i).peg());
		}
		return o;
	}
	public final int getFuncParamSize() {
		BunType type = this.getRealType();
		if(type instanceof FuncType) {
			return type.size() - 1;
		}
		return 0;
	}
	public final BunType getReturnType() {
		BunType type = this.getRealType();
		if(type instanceof FuncType) {
			return type.get(type.size() - 1);
		}
		return type;
	}
	public final boolean hasNodeType() {
		BunType type = this.getRealType();
		for(int i = 0; i < type.size(); i++) {
			if(type.get(i).hasNodeType()) {
				return true;
			}
		}
		return type instanceof NodeType;
	}
	public final boolean hasVarType() {
		BunType type = this.getRealType();
		for(int i = 0; i < type.size(); i++) {
			if(type.get(i).hasVarType()) {
				return true;
			}
		}
		return type instanceof VarType;
	}
	public final int countGreekType() {
		BunType type = this.getRealType();
		if(type instanceof GreekType) {
			return ((GreekType)type).greekIndex + 1;
		}
		else {
			int greekCount = 0;
			for(int i = 0; i < type.size(); i++) {
				int count = type.get(i).countGreekType();
				if(count > greekCount) {
					greekCount = count;
				}
			}
			return greekCount;
		}
	}
	
	public abstract void    stringfy(UStringBuilder sb);
	public abstract int size();
	public abstract BunType get(int index);

	public abstract BunType getLangType(SymbolTable gamma);
	public abstract BunType getRealType();

	public abstract BunType transformGreekTypeToVarType(BunType[] buffer);
	public abstract boolean accept(SymbolTable gamma, PegObject node, boolean hasNextChoice);
	public abstract void typed(SymbolTable gamma, PegObject node, boolean flag);

	public boolean is(BunType nodeType) {
		nodeType = nodeType.getRealType();
		if(nodeType == this) {
			return true;
		}
		return false;
	}

	protected final boolean checkCoercion(SymbolTable gamma, PegObject node, BunType nodeType, boolean hasNextChoice) {
		String key = BunType.keyTypeRel("#cast", nodeType, this);
		Functor f = gamma.getSymbol(key);
		if(f != null) {
			if(Main.VerboseMode) {
				Main._PrintLine("found type coercion from " + nodeType + " to " + this);
			}
			node.typed = BunType.newTransType(key, nodeType, this, f);
			return true;
		}
		if(hasNextChoice) {
			return false;
		}
		return false;  // stupid cast
	}
	public void build(PegObject node, BunDriver driver) {
		node.matched.build(node, driver);
	}

	// ----------------------------------------------------------------------
	
	final static UList<BunType>   _TypeList;
	final static UMap<BunType>     _TypeNameMap;

	public final static BunType UntypedType = new AnyType("#Tuntyped", "untyped");
	public final static BunType AnyType = new AnyType("#Tany", "any");
	public final static BunType VoidType = new AnyType("#Tvoid", "void");
		
	static {
		_TypeList = new UList<BunType>(new BunType[128]);
		_TypeNameMap = new UMap<BunType>();
		issueType("untyped", UntypedType);
		issueType("any",     AnyType);
		issueType("void",    VoidType);
		setGenerics(new FuncType(0));
	}

	private static void issueType(String key, BunType t) {
		t.typeId = _TypeNameMap.size();
		_TypeNameMap.put(key, t);
	}
	
	public final static BunType newValueType(String name, Object typeInfo) {
		BunType t = _TypeNameMap.get(name, null);
		if(t == null) {
			t = new ValueType("#type", name, typeInfo);
			issueType(name, t);
		}
		return t;
	}
	
	public static BunType newVarType() {
		return new VarType(null);
	}

	public static BunType newVarType(PegObject node) {
		return new VarType(node);
	}
	
	public final static void setGenerics(GenericType t) {
		issueType(t.baseName+"<>", t);
		t.genericId = t.typeId;
	}

	public final static BunType newGenericType(String baseName, UList<BunType> typeList) {
		GenericType gt = null;
		BunType t = _TypeNameMap.get(baseName+"<>", null);
		if(t instanceof GenericType) {
			gt = (GenericType)t;
		}
		else {
			gt = new GenericType("#Tgeneric", baseName, 0);
			setGenerics(gt);
		}
		return newGenericType(gt, typeList.ArrayValues, typeList.size());
	}

	public final static BunType newGenericType(GenericType baseType, BunType[] params) {
		return newGenericType(baseType, params, params.length);
	}
	
	public final static BunType newGenericType(GenericType baseType, BunType[] types, int size) {
		if(checkIssuedType(types, size)) {
			return baseType.newCloneType(compactArray(types, size));
		}
		else {
			String key = mangleTypes(baseType.baseName, types, size);
			BunType gt = _TypeNameMap.get(key, null);
			if(gt == null) {
				gt = baseType.newCloneType(compactArray(types, size));
				issueType(key, gt);
			}
			return gt;
		}
	}
	private static BunType[] compactArray(BunType[] t, int size) {
		if(t.length == size) {
			return t;
		}
		BunType[] newt = new BunType[size];
		System.arraycopy(t, 0, newt, 0, size);
		return newt;
	}
	public final static boolean checkIssuedType(BunType[] types, int size) {
		for(int i = 0; i < size; i++) {
			if(types[i].isIssued()) {
				return true;
			}
		}
		return false;
	}
	private final static String mangleTypes(String header, BunType[] t, int size) {
		UStringBuilder sb = new UStringBuilder();
		sb.append(header);
		for(int i = 0; i < size; i++) {
			sb.append("+");
			sb.appendInt(t[i].typeId);
		}
		return sb.toString();
	}

	public final static BunType newGenericType(String baseName, BunType type) {
		UList<BunType> typeList = new UList<BunType>(new BunType[1]);
		typeList.add(type);
		return newGenericType(baseName, typeList);
	}

	public static BunType newUnionType(UList<BunType> list) {
		// TODO Auto-generated method stub
		return null;
	}

	public final static FuncType newFuncType(UList<BunType> typeList) {
		BunType funcType = newGenericType(FuncType.FuncBaseName, typeList);
		if(funcType instanceof FuncType) {
			return (FuncType)funcType;
		}
		return null;
	}

	public final static FuncType newFuncType(BunType t, BunType r) {
		UList<BunType> typeList = new UList<BunType>(new BunType[2]);
		typeList.add(t);
		typeList.add(r);
		return BunType.newFuncType(typeList);
	}

	// =======================================================================
	
	public static final BunType newGreekType(int greekIndex) {
		String key = "$" + greekIndex;
		BunType t = _TypeNameMap.get(key, null);
		if(t == null) {
			t = new GreekType(greekIndex);
			issueType(key, t);
		}
		return t;
	}

	public final static String keyTypeRel(String head, BunType fromType, BunType toType) {
		return head + "+" + fromType.typeId + "+" + toType.typeId;
	}
	
	public final static BunType newTransType(String key, BunType sourceType, BunType targetType, Functor f) {
		BunType t = _TypeNameMap.get(key, null);
		if(t == null) {
			t = new TransType(sourceType, targetType, f);
			issueType(key, t);
		}
		return t;
	}
	
	public final static BunType[] emptyTypes = new BunType[0];

	public final static BunType[] addTypes (BunType[] types, BunType t) {
		for(int i = 0; i < types.length; i++) {
			if(types[i] == t) {
				return types;
			}
		}
		BunType[] newtypes = new BunType[types.length+1];
		System.arraycopy(types, 0, newtypes, 0, types.length);
		newtypes[types.length] = t;
		types = newtypes;
		sortTypes(types);
		return types;
	}
	
	private final static void sortTypes(BunType[] t) {
		for(int i = 0; i < t.length - 1; i++) {
			for(int j = i + 1; j < t.length; j++) {
				if(t[i].typeId > t[j].typeId) {
					BunType e = t[i];
					t[i] = t[j];
					t[j] = e;
				}
			}
		}
	}
	public static BunType newNotType(BunType type) {
		return new BunNotType(type);
	}

	public static BunType newAndType(UList<BunType> typeList) {
		BunAndType at = new BunAndType();
		at.types = typeList.compactArray();
		return at;
	}

	public static BunType newTokenType(String text) {
		String key = "`" + text;
		BunType t = _TypeNameMap.get(key, null);
		if(t == null) {
			t = new TokenType(text);
			issueType(key, t);
		}
		return t;
	}

	public static BunType newNodeType(String text) {
		String key = "#" + text;
		BunType t = _TypeNameMap.get(key, null);
		if(t == null) {
			t = new NodeType(text);
			issueType(key, t);
		}
		return t;
	}

	public static BunType newOptionalType(String text) {
		// TODO Auto-generated method stub
		return null;
	}
	
}

abstract class LangType extends BunType {
	protected LangType(String label) {
		super(label);
	}
	public boolean accept(SymbolTable gamma, PegObject node, boolean hasNextChoice) {
		BunType nodeType = node.getType(BunType.UntypedType);
		return this.is(nodeType) || this.checkCoercion(gamma, node, nodeType, hasNextChoice);
	}
	public void typed(SymbolTable gamma, PegObject node, boolean flag) {
		if(node.typed == null) {
			node.typed = this;
		}
	}
}

class ValueType extends LangType {
	protected String name;
	protected BunType[] superTypes;
	ValueType(String label, String name, Object typeInfo) {
		super(label);
		this.name = name;
		superTypes = BunType.emptyTypes;
	}
	@Override
	public void stringfy(UStringBuilder sb) {
		sb.append(this.name);
	}
	
	public void addSuperType(SymbolTable gamma, BunType t) {
		this.superTypes = BunType.addTypes(this.superTypes, t);
		gamma.addUpcast(this, t);
	}
	@Override
	public int size() {
		return 0;
	}
	@Override
	public BunType get(int index) {
		return null;
	}
	@Override
	public BunType getLangType(SymbolTable gamma) {
		return this;
	}
	@Override
	public BunType getRealType() {
		return this;
	}
	@Override
	public BunType transformGreekTypeToVarType(BunType[] buffer) {
		return this;
	}
}

class AnyType extends ValueType {
	AnyType(String label, String name) {
		super(label, name, null);
	}
	public boolean is(BunType nodeType) {
		return true;
	}
}

class VarType extends LangType {
	private BunType inferredType = null;
	protected  PegObject node;
	VarType(PegObject node) {
		super("#Tvar");
		this.node = node;
	}
	@Override
	public void stringfy(UStringBuilder sb) {
		if(this.inferredType != null) {
			sb.append("(?->");
			this.inferredType.stringfy(sb);
			sb.append(")");
			return;
		}
		sb.append("?");
	}
	@Override
	public final BunType getRealType() {
		if(this.inferredType != null) {
			return this.inferredType.getRealType();
		}
		return this;
	}
	public int size() {
		return 0;
	}
	@Override
	public BunType get(int index) {
		return null;
	}
	public final boolean is(BunType nodeType) {
		if(this == nodeType) {
			return true;
		}
		if(this.inferredType == null) {
			this.inferredType = nodeType;
			return true;
		}
		return this.inferredType.is(nodeType);
	}
	@Override
	public BunType getLangType(SymbolTable gamma) {
		return this;
	}
	@Override
	public BunType transformGreekTypeToVarType(BunType[] buffer) {
		return this;
	}
}

class GenericType extends LangType {
	protected int    genericId;
	protected String baseName;
	protected BunType[] typeParams;

	public GenericType(String label, String baseName, int genericId) {
		super(label);
		this.baseName = baseName;
		this.genericId = genericId;
		typeParams = BunType.emptyTypes;
	}
	
	public GenericType newCloneType(BunType[] typeParams) {
		GenericType gt = new GenericType(this.tag, this.baseName, this.genericId);
		gt.typeParams = typeParams;
		return gt;
	}

	@Override
	public void stringfy(UStringBuilder sb) {
		this.stringfy(sb, this.baseName + "<", ",", ">");
	}

	@Override
	public int size() {
		return this.typeParams.length;
	}

	@Override
	public BunType get(int index) {
		return this.typeParams[index];
	}

	@Override
	public BunType getLangType(SymbolTable gamma) {
		return this;
	}

	@Override
	public BunType getRealType() {
		if(!this.isIssued()) {
			boolean noVarType = true;
			for(int i = 0; i < typeParams.length; i++) {
				typeParams[i] = typeParams[i].getRealType();
				if(typeParams[i].hasVarType()) {
					noVarType = false;
				}
			}
			if(noVarType) {
				return BunType.newGenericType(this, this.typeParams);
			}
		}
		return this;
	}

	@Override
	public boolean is(BunType nodeType) {
		nodeType = nodeType.getRealType();
		if(this == nodeType) {
			return true;
		}
		if(nodeType instanceof GenericType) {
			GenericType gt = (GenericType)nodeType;
			if(gt.genericId == this.genericId && gt.typeParams.length == this.typeParams.length) {
				for(int i = 0; i < this.typeParams.length; i++) {
					if(!this.typeParams[i].is(gt.typeParams[i])) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}
	
	@Override
	public final BunType transformGreekTypeToVarType(BunType[] buffer) {
		if(buffer == null) {
			int greekCount = this.countGreekType();
			if(greekCount == 0) {
				return this;
			}
			buffer = new BunType[greekCount];
		}
		BunType[] newparams = new BunType[this.typeParams.length];
		for(int i = 0; i< this.typeParams.length; i++) {
			newparams[i] = this.typeParams[i].transformGreekTypeToVarType(buffer);
		}
		return this.newCloneType(newparams);
	}
}


class FuncType extends GenericType {
	public final static String FuncBaseName    = "Func";

	public FuncType(int genericId) {
		super("Tfunc", FuncBaseName, genericId);
	}

	@Override
	public GenericType newCloneType(BunType[] typeParams) {
		FuncType t = new FuncType(this.genericId);
		t.typeParams = typeParams;
		return t;
	}
}

class UnionType extends LangType {
	public BunType[] types;
	public UnionType() {
		super("#Tunion");
		this.types = BunType.emptyTypes;
	}
	@Override
	public int size() {
		return this.types.length;
	}
	@Override
	public BunType get(int index) {
		return this.types[index];
	}
	public void add(BunType t) {
		assert(this.typeId == -1);
		t = t.getRealType();
		if(t instanceof UnionType) {
			UnionType ut = (UnionType)t;
			for(int i = 0; i < ut.types.length; i++) {
				this.types = BunType.addTypes(this.types, ut.types[i]);
			}
		}
		else {
			this.types = BunType.addTypes(this.types, t);
		}
	}
	@Override
	public void stringfy(UStringBuilder sb) {
		this.stringfy(sb, "", "|", "");
	}
	@Override
	public BunType getRealType() {
		if(this.hasVarType()) {
			System.err.println("TODO: UnionType.getRealType()");
		}
		return this;
	}
	@Override
	public BunType getLangType(SymbolTable gamma) {
		if(this.hasNodeType()) {
			System.err.println("TODO: UnionType.getLangType()");
		}
		return this;
	}
	@Override
	public BunType transformGreekTypeToVarType(BunType[] buffer) {
		if(this.countGreekType() > 0) {
			System.err.println("TODO: UnionType.newVarGreekType()");
//			UnionType ut = new UnionType();
//			for(int i = 0; i < this.types.length; i++) {
//				ut.add(this.types[i].newVarGreekType(list, buffer));
//			}
//			return ut;
		}
		return this;
	}

	@Override
	public boolean is(BunType nodeType) {
		nodeType = nodeType.getRealType();
		if(this == nodeType) {
			return true;
		}
		if(nodeType instanceof UnionType) {
			UnionType ut = (UnionType)nodeType;
			for(int i = 0; i < ut.types.length; i++) {
				if(!this.is(ut.types[i])) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean accept(SymbolTable gamma, PegObject node, boolean hasNextChoice) {
		BunType nodeType = node.getType(BunType.UntypedType);
		if(this.is(nodeType)) {
			return true;
		}
//		for(int i = 0; i < types.length; i++) {
//			if(types[i].accept(node)) {
//				return true;
//			}
//		}
		return false;
	}

}

class TransType extends BunType {
	public BunType sourceType;
	public BunType targetType;
	public Functor  functor;
	public TransType(BunType sourceType, BunType targetType, Functor functor) {
		super("");
		this.sourceType = sourceType;
		this.targetType = targetType;
		this.functor = functor;
	}
	@Override
	public BunType getRealType() {
		return this.targetType;
	}
	@Override
	public void stringfy(UStringBuilder sb) {
		this.targetType.stringfy(sb);
		sb.append("(<-");
		this.sourceType.stringfy(sb);
		sb.append(")");
	}
	@Override
	public boolean is(BunType nodeType) {
		return this.targetType.is(nodeType);
	}
	public void build(PegObject node, BunDriver driver) {
		PegObject o = new PegObject(this.functor.name);
		o.append(node);
		o.matched = this.functor;
		o.typed = this.targetType;
		node.typed = this.sourceType;
		driver.pushNode(o);
		//this.functor.build(o, driver);
		node.typed = this;
	}

	@Override
	public int size() {
		return 0;
	}
	@Override
	public BunType get(int index) {
		return null;
	}
	@Override
	public BunType getLangType(SymbolTable gamma) {
		return this.targetType;
	}
	@Override
	public BunType transformGreekTypeToVarType(BunType[] buffer) {
		return this.targetType;
	}
	@Override
	public boolean accept(SymbolTable gamma, PegObject node, boolean hasNextChoice) {
		return this.targetType.accept(gamma, node, hasNextChoice);
	}
	@Override
	public void typed(SymbolTable gamma, PegObject node, boolean flag) {
		if(node.typed == null) {
			node.typed = this;
		}
	}
}

class GreekList {
	public String name;
	public BunType premiseType;
	public GreekList next;
	
	public GreekList(String name, BunType premiseType) {
		this.name = name;
		this.premiseType = premiseType;
		this.next = null;
	}
	public void append(GreekList entry) {
		GreekList cur = this;
		while(cur.next != null) {
			cur = cur.next;
		}
		cur.next = entry;
	}
	public int size() {
		int size = 0;
		GreekList cur = this;
		while(cur != null) {
			size = size + 1;
			cur = cur.next;
		}
		return size;
	}
	public BunType getGreekType(String name) {
		int size = 0;
		GreekList cur = this;
		while(cur != null) {
			if(cur.name.equals(name)) {
				return BunType.newGreekType(size);
			}
			size = size + 1;
			cur = cur.next;
		}
		return null;
	}
	public final BunType getPremiseType(int greekIndex) {
		int size = 0;
		GreekList list = this;
		while(list != null) {
			if(greekIndex == size) {
				return list.premiseType;
			}
			size = size + 1;
			list = list.next;
		}
		return null;
	}
}

class GreekType extends LangType {
	int greekIndex;
	public GreekType(int greekIndex) {
		super("#type");
		this.greekIndex = greekIndex;
	}
	@Override
	public void stringfy(UStringBuilder sb) {
		sb.append("$");
		sb.appendInt(this.greekIndex);
	}
	@Override
	public boolean is(BunType nodeType) {
		return false;
	}
	@Override
	public BunType transformGreekTypeToVarType(BunType[] buffer) {
		if(buffer[this.greekIndex] == null) {
			buffer[this.greekIndex] = BunType.newVarType(); //list.getPremiseType(this.greekIndex));
		}
		return buffer[this.greekIndex];
	}
	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public BunType get(int index) {
		return null;
	}
	@Override
	public BunType getLangType(SymbolTable gamma) {
		return null;
	}
	@Override
	public BunType getRealType() {
		return this;
	}
}


// Mutable<T>, Assignable<T> = T
// T|null

abstract class BunNodeType extends BunType {
	String symbol;
	BunNodeType(String label, String symbol) {
		super(label);
		this.symbol = symbol;
	}
	@Override
	public int size() {
		return 0;
	}
	@Override
	public BunType get(int index) {
		return null;
	}
	@Override
	public BunType getLangType(SymbolTable gamma) {
		return null;
	}
	@Override
	public BunType getRealType() {
		return this;
	}
	@Override
	public BunType transformGreekTypeToVarType(BunType[] buffer) {
		return this;
	}
	@Override
	public boolean is(BunType nodeType) {
		return false;
	}
//	public boolean accept(SymbolTable gamma, PegObject node, boolean hasNextChoice) {
//		if(this.symbol.equals(node.name)) {
//			return true;
//		}
//		return false;
//	}
//	@Override
//	public void typed(SymbolTable gamma, PegObject node, boolean flag) {
//	}

}

class NodeType extends BunNodeType {
	String symbol;
	NodeType(String symbol) {
		super("#Tbun.node", symbol);
	}
	@Override
	public void stringfy(UStringBuilder sb) {
		sb.append("#");
		sb.append(this.symbol);
	}
	public boolean accept(SymbolTable gamma, PegObject node, boolean hasNextChoice) {
		if(this.symbol.equals(node.tag)) {
			return true;
		}
		return false;
	}
	@Override
	public void typed(SymbolTable gamma, PegObject node, boolean flag) {
	}
}

class TokenType extends BunNodeType {
	TokenType(String symbol) {
		super("#Tbun.token", symbol);
	}
	@Override
	public void stringfy(UStringBuilder sb) {
		sb.append("'");
		sb.append(this.symbol);
		sb.append("'");
	}
	public boolean accept(SymbolTable gamma, PegObject node, boolean hasNextChoice) {
		//System.out.println("@@@@ matching " + this + "   "+ this.symbol + " " + node.getText() + " ... " + this.symbol.equals(node.getText()));
		if(this.symbol.equals(node.getText())) {
			return true;
		}
		return false;
	}
	@Override
	public void typed(SymbolTable gamma, PegObject node, boolean flag) {
	}
}

class SymbolType extends BunNodeType {
	SymbolType(String symbol) {
		super("#Tbun.symbol", symbol);
	}
	@Override
	public void stringfy(UStringBuilder sb) {
		sb.append("$");
		sb.append(this.symbol);
	}
	private BunType getSymbolType(SymbolTable gamma, PegObject node) {
		String symbol = this.symbol;
		if(symbol.length() == 0) {
			symbol = node.getText();
		}
		BunType t = gamma.getType(symbol, null);
		return t;
	}
	public boolean accept(SymbolTable gamma, PegObject node, boolean hasNextChoice) {
		BunType t = this.getSymbolType(gamma, node);
		if(t != null) {
			return t.accept(gamma, node, hasNextChoice);
		}
		return false;
	}
	@Override
	public void typed(SymbolTable gamma, PegObject node, boolean flag) {
		if(node.typed == null) {
			BunType t = this.getSymbolType(gamma, node);
			if(t != null) {
				node.typed = t;
			}
		}
	}
}

class VariableType extends BunNodeType {
	VariableType() {
		super("#Tbun.optinal", "@Variable");
	}
	@Override
	public void stringfy(UStringBuilder sb) {
		sb.append(this.symbol);
	}
	public boolean accept(SymbolTable gamma, PegObject node, boolean hasNextChoice) {
		return node.isVariable();
	}
	@Override
	public void typed(SymbolTable gamma, PegObject node, boolean flag) {
		node.setVariable(flag);
	}
}

class BunNotType extends BunNodeType {
	BunType innerType;
	BunNotType(BunType innerType) {
		super("#Tbun.not", null);
		this.innerType = innerType;
	}
	@Override
	public void stringfy(UStringBuilder sb) {
		sb.append("!");
		innerType.stringfy(sb);
	}
	public boolean accept(SymbolTable gamma, PegObject node, boolean hasNextChoice) {
		return !(this.innerType.accept(gamma, node, false));
	}
	public void typed(SymbolTable gamma, PegObject node, boolean flag) {
		this.innerType.typed(gamma, node, !flag);
	}
}

class BunAndType extends BunNodeType {
	BunType[] types;
	BunAndType() {
		super("Tbun.and", null);
		this.types = BunType.emptyTypes;
	}
	@Override
	public void stringfy(UStringBuilder sb) {
		this.stringfy(sb, "", " ", "");
	}
	@Override
	public int size() {
		return this.types.length;
	}
	@Override
	public BunType get(int index) {
		return this.types[index];
	}
	public BunType getLangType(SymbolTable gamma) {
		for(int i = 0; i < this.types.length; i++) {
			BunType t = this.types[i].getLangType(gamma);
			if(t != null) {
				return t;
			}
		}
		return BunType.AnyType;
	}
	public boolean accept(SymbolTable gamma, PegObject node, boolean hasNextChoice) {
		for(int i = 0; i < this.types.length; i++) {
			if(!this.types[i].accept(gamma, node, hasNextChoice)) {
				return false;
			}
		}
		return true;
	}
	public void typed(SymbolTable gamma, PegObject node, boolean flag) {
		for(int i = 0; i < this.types.length; i++) {
			this.types[i].typed(gamma, node, flag);
		}
	}
}
