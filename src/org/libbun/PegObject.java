package org.libbun;

public class PegObject {
	public PegSource    source = null;
	int          startIndex = 0;
	int          endIndex = 0;
	Peg          createdPeg = null;
	public String       tag = null;
	PegObject    parent = null;
	PegObject    AST[] = null;
	SymbolTable  gamma = null;
	Functor      matched = null;
	BunType      typed   = null;

	PegObject(String tag) {
		this.tag = tag;
	}

	PegObject(String tag, PegSource source, Peg createdPeg, int startIndex) {
		this.tag        = tag;
		this.source     = source;
		this.createdPeg = createdPeg;
		this.startIndex = startIndex;
		this.endIndex   = startIndex;
	}

	public final boolean isFailure() {
		return (this.tag == null);
	}

	public final boolean is(String functor) {
		return this.tag.equals(functor);
	}

	public final void setSource(Peg createdPeg, PegSource source, int startIndex, int endIndex) {
		this.createdPeg = createdPeg;
		this.source     = source;
		this.startIndex = startIndex;
		this.endIndex   = endIndex;
	}

	public final String formatSourceMessage(String type, String msg) {
		return this.source.formatErrorMessage(type, this.startIndex, msg);
	}
	
	public final boolean isEmptyToken() {
		return this.startIndex == this.endIndex;
	}
	
	public final String getText() {
		if(this.source != null) {
			return this.source.substring(this.startIndex, this.endIndex);
		}
		return "";
	}

	// AST[]
	
	public final int size() {
		if(this.AST == null) {
			return 0;
		}
		return this.AST.length;
	}

	public final PegObject get(int index) {
		return this.AST[index];
	}

	public final PegObject get(int index, PegObject defaultValue) {
		if(index < this.size()) {
			return this.AST[index];
		}
		return defaultValue;
	}

	public final void set(int index, PegObject node) {
		if(!(index < this.size())){
			this.expandAstToSize(index+1);
		}
		this.AST[index] = node;
		node.parent = this;
	}
	
	public final void resizeAst(int size) {
		if(this.AST == null && size > 0) {
			this.AST = Main._NewPegObjectArray(size);
		}
		else if(this.AST.length != size) {
			PegObject[] newast = Main._NewPegObjectArray(size);
			if(size > this.AST.length) {
				Main._ArrayCopy(this.AST, 0, newast, 0, this.AST.length);
			}
			else {
				Main._ArrayCopy(this.AST, 0, newast, 0, size);
			}
			this.AST = newast;
		}
	}

	public final void expandAstToSize(int newSize) {
		if(newSize > this.size()) {
			this.resizeAst(newSize);
		}
	}

	public final void append(PegObject childNode) {
		int size = this.size();
		this.expandAstToSize(size+1);
		this.AST[size] = childNode;
		childNode.parent = this;
	}

	public final void insert(int index, PegObject childNode) {
		int oldsize = this.size();
		if(index < oldsize) {
			PegObject[] newast = Main._NewPegObjectArray(oldsize+1);
			if(index > 0) {
				Main._ArrayCopy(this.AST, 0, newast, 0, index);
			}
			newast[index] = childNode;
			childNode.parent = this;
			if(oldsize > index) {
				Main._ArrayCopy(this.AST, index, newast, index+1, oldsize - index);
			}
			this.AST = newast;
		}
		else {
			this.append(childNode);
		}
	}

	public final void removeAt(int index) {
		int oldsize = this.size();
		if(oldsize > 1) {
			PegObject[] newast = Main._NewPegObjectArray(oldsize-1);
			if(index > 0) {
				Main._ArrayCopy(this.AST, 0, newast, 0, index);
			}
			if(oldsize - index > 1) {
				Main._ArrayCopy(this.AST, index+1, newast, index, oldsize - index - 1);
			}
			this.AST = newast;
		}
		else {
			this.AST = null;
		}
	}
	
	
	public final int count() {
		int count = 1;
		for(int i = 0; i < this.size(); i++) {
			count = count + this.get(i).count();
		}
		return count;
	}

	public final void checkNullEntry() {
		for(int i = 0; i < this.size(); i++) {
			if(this.AST[i] == null) {
				this.AST[i] = new PegObject("#empty", this.source, null, this.startIndex);
				this.AST[i].parent = this;
			}
		}
	}

	public final String textAt(int index, String defaultValue) {
		if(index < this.size()) {
			return this.AST[index].getText();
		}
		return defaultValue;
	}
	
	public BunType typeAt(SymbolTable gamma, int index, BunType defaultType) {
		if(index < this.size()) {
			PegObject node = this.AST[index];
			if(node.typed != null) {
				return node.typed;
			}
			if(node.matched == null && gamma != null) {
				node = gamma.tryMatch(node, true);
			}
			if(node.matched != null) {
				return node.matched.getReturnType(defaultType);
			}
		}
		return defaultType;
	}

	@Override
	public String toString() {
		SourceBuilder sb = new SourceBuilder(null);
		this.stringfy(sb);
		return sb.toString();
	}

	final void stringfy(SourceBuilder sb) {
		if(this.isFailure()) {
			sb.append(this.formatSourceMessage("syntax error", "    " + this.info()));
		}
		else if(this.AST == null) {
			sb.appendNewLine(this.tag+ ": '''", this.getText(), "'''" + this.info());
		}
		else {
			sb.appendNewLine(this.tag);
			sb.openIndent("{" + this.info());
			for(int i = 0; i < this.size(); i++) {
				if(this.AST[i] != null) {
					this.AST[i].stringfy(sb);
				}
				else {
					sb.appendNewLine("@missing subnode at " + i);
				}
			}
			sb.closeIndent("}");
		}
	}

	private String info() {
		if(this.matched == null) {
			if(this.source != null && Main.VerbosePegMode) {
				return "         ## by peg : " + this.createdPeg;
			}
			return "";
		}
		else {
			return "      :: " + this.getType(null) + " by " + this.matched;
		}
	}

	public final PegObject findParentNode(String name) {
		PegObject node = this;
		while(node != null) {
			if(node.is(name)) {
				return node;
			}
			node = node.parent;
		}
		return null;
	}

	public final SymbolTable getSymbolTable() {
		PegObject node = this;
		while(node.gamma == null) {
			node = node.parent;
		}
		return node.gamma;
	}

	public final SymbolTable getLocalSymbolTable() {
		if(this.gamma == null) {
			SymbolTable gamma = this.getSymbolTable();
			gamma = new SymbolTable(gamma.getNamespace(), this);
			// NOTE: this.gamma was set in SymbolTable constructor
			assert(this.gamma != null);
		}
		return this.gamma;
	}
	
	public final BunType getType(BunType defaultType) {
		if(this.typed == null) {
			if(this.matched != null) {
				this.typed = this.matched.getReturnType(defaultType);
			}
			if(this.typed == null) {
				return defaultType;
			}
		}
		return this.typed;
	}
	
	public boolean isVariable() {
		// TODO Auto-generated method stub
		return true;
	}

	public void setVariable(boolean flag) {
	}

	public final int countUnmatched(int c) {
		for(int i = 0; i < this.size(); i++) {
			PegObject o = this.get(i);
			c = o.countUnmatched(c);
		}
		if(this.matched == null) {
			return c+1;
		}
		return c;
	}

	public final boolean isUntyped() {
		return this.typed == null || this.typed == BunType.UntypedType;
	}

}
