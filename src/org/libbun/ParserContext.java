package org.libbun;

public abstract class ParserContext extends SourceContext {
//	public PegRuleSet ruleSet;
	public int objectCount = 0;

	public ParserContext(PegSource source, int startIndex, int endIndex) {
		super(source, startIndex, endIndex);
	}
	
	public abstract void setRuleSet(PegRuleSet ruleSet);

//	@Override
//	public SourceContext subContext(int startIndex, int endIndex) {
//		return new PegParserContext(this.parser, this.source, startIndex, endIndex);
//	}

	public boolean hasNode() {
		this.matchZeroMore(UniCharset.WhiteSpaceNewLine);
		return this.sourcePosition < this.endPosition;
	}

	public PegObject parseNode(String key) {
		return this.parsePegObject(new PegObject(BunSymbol.TopLevelFunctor), key);
	}

	public abstract void initMemo();
	public void removeMemo(int startIndex, int endIndex) {
	}

	protected abstract PegObject parsePegObject(PegObject inNode, String key);
	
	public final PegObject newPegObject(String name) {
		PegObject node = new PegObject(name, this.source, null, this.sourcePosition);
		this.objectCount = this.objectCount + 1;
		return node;
	}

	protected final PegObject foundFailureNode = new PegObject(null, this.source, null, 0);
	
	public final PegObject foundFailure(Peg created) {
		if(this.sourcePosition >= this.foundFailureNode.endIndex) {  // adding error location
			this.foundFailureNode.startIndex = this.sourcePosition;
			this.foundFailureNode.endIndex = this.sourcePosition;
			this.foundFailureNode.createdPeg = created;
		}
		return this.foundFailureNode;
	}

	public final PegObject refoundFailure(Peg created, int pos) {
		this.foundFailureNode.startIndex = pos;
		this.foundFailureNode.endIndex   = pos;
		this.foundFailureNode.createdPeg = created;
		return this.foundFailureNode;
	}

	public final Peg storeFailurePeg() {
		return this.foundFailureNode.createdPeg;
	}
	public final int storeFailurePosition() {
		return this.foundFailureNode.endIndex;
	}
	public final void restoreFailure(Peg created, int pos) {
		this.foundFailureNode.createdPeg = created;
		this.foundFailureNode.startIndex   = pos;
		this.foundFailureNode.endIndex   = pos;
	}

	public abstract int getStackPosition(Peg peg);
	public abstract void popBack(int stackPosition, boolean backtrack);
	public abstract void push(Peg peg, PegObject parentNode, int index, PegObject node);
	public abstract void addSubObject(PegObject newnode, int stack, int top);
	
	public abstract void showStatInfo(PegObject node);

	public boolean isVerifyMode() {
		return false;
	}

	public PegObject precheck(PegNewObject peg, PegObject in) {
		return null;  // if prechecked
	}
}
