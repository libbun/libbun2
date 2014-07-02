package org.libbun;

import java.util.HashSet;

public abstract class Peg {
	public final static boolean _BackTrack = true;

	int       flag     = 0;
	String    ruleName = null;
	boolean   debug    = false;
	boolean   hasLeftRecursion = false;

	PegSource source = null;
	int       sourcePosition = 0;
	
	Peg() {
	}

	protected abstract Peg clone(String ns);
	protected abstract void stringfy(UniStringBuilder sb, boolean debugMode);
	protected abstract void makeList(PegRuleSet parser, UniArray<String> list, UniMap<String> set);
	protected abstract PegObject simpleMatch(PegObject inNode, ParserContext context);
	protected abstract void verify(String ruleName, PegRuleSet rules);
	public abstract void accept(PegVisitor visitor);

	public int size() {
		return 0;
	}
	public Peg get(int index) {
		return this;  // to avoid NullPointerException
	}

	@Override public String toString() {
		UniStringBuilder sb = new UniStringBuilder();
		this.stringfy(sb, false);
		if(this.ruleName != null) {
			sb.append(" defined in ");
			sb.append(this.ruleName);
		}
		return sb.toString();
	}
	
	public boolean hasLeftRecursion() {
		return this.hasLeftRecursion;
	}
	
	public void setLeftRecursion(boolean lrExistense) {
		this.hasLeftRecursion = lrExistense;
	}

	public final PegChoice appendAsChoice(Peg e) {
		if(this instanceof PegChoice) {
			((PegChoice)this).extend(e);
			return ((PegChoice)this);
		}
		else {
			PegChoice choice = new PegChoice();
			choice.add(this);
			choice.extend(e);
			return choice;
		}
	}
	protected PegObject performMatch(PegObject inNode, ParserContext context) {
		if(this.debug) {
			PegObject node2 = this.simpleMatch(inNode, context);
			String msg = "matched";
			if(node2.isFailure()) {
				msg = "failed";
			}
			String line = context.formatErrorMessage(msg, this.toString());
			System.out.println(line + "\n\tnode #" + inNode + "# => #" + node2 + "#");
			return node2;
		}
		return this.simpleMatch(inNode, context);
	}
	public final String toPrintableString(String name, String Setter, String Choice, String SemiColon, boolean debugMode) {
		UniStringBuilder sb = new UniStringBuilder();
		sb.append(name);
		sb.append(Setter);
		if(this instanceof PegChoice) {
			for(int i = 0; i < this.size(); i++) {
				if(i > 0) {
					sb.append(Choice);
				}
				this.get(i).stringfy(sb, debugMode);
			}
		}
		else {
			this.stringfy(sb, debugMode);
		}
		sb.append(SemiColon);
		return sb.toString();
	}

	void setSource(PegSource source, int sourcePosition) {
		this.source = source;
		this.sourcePosition = sourcePosition;
	}
	protected void dump(String msg) {
		if(this.source != null) {
			System.out.println(this.source.formatErrorMessage("*", this.sourcePosition, msg));
		}
		else {
			System.out.println("unknown source: " + msg);
		}
	}
	protected void warning(String msg) {
		if(Main.PegDebuggerMode) {
			Main._PrintLine("PEG warning: " + msg);
		}
	}
	
}

abstract class PegAtom extends Peg {
	String symbol;
	public PegAtom (String symbol) {
		super();
		this.symbol = symbol;
	}
	@Override
	protected void stringfy(UniStringBuilder sb, boolean debugMode) {
		sb.append(this.symbol);
	}	
	@Override
	protected void verify(String ruleName, PegRuleSet rules) {
		this.ruleName = ruleName;
	}
	@Override
	public final int size() {
		return 0;
	}
	@Override
	public final Peg get(int index) {
		return this;  // just avoid NullPointerException
	}
	@Override
	protected void makeList(PegRuleSet parser, UniArray<String> list, UniMap<String> set) {
	}

}

class PegString extends PegAtom {
	public PegString(String symbol) {
		super(symbol);
	}
	@Override
	protected Peg clone(String ns) {
		return this;
	}
	public final static String quoteString(String s) {
		char quote = '\'';
		if(s.indexOf("'") != -1) {
			quote = '"';
		}
		return UniCharset._QuoteString(quote, s, quote);
	}
	@Override
	protected void stringfy(UniStringBuilder sb, boolean debugMode) {
		sb.append(PegString.quoteString(this.symbol));
	}
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitPegString(this);
	}
	@Override
	public PegObject simpleMatch(PegObject inNode, ParserContext context) {
		if(context.match(this.symbol)) {
			return inNode;
		}
		return context.foundFailure(this);
	}
}

class PegAny extends PegAtom {
	public PegAny() {
		super(".");
	}
	@Override
	protected Peg clone(String ns) {
		return this;
	}
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitPegAny(this);
	}
	@Override
	public PegObject simpleMatch(PegObject inNode, ParserContext context) {
		if(context.hasChar()) {
			context.consume(1);
			return inNode;
		}
		return context.foundFailure(this);
	}
}

class PegCharacter extends PegAtom {
	UniCharset charset;
	public PegCharacter(String token) {
		super(token);
		this.charset = new UniCharset(token);
	}
	@Override
	protected Peg clone(String ns) {
		return this;
	}
	@Override
	protected void stringfy(UniStringBuilder sb, boolean debugMode) {
		sb.append("[" + this.symbol, "]");
	}
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitPegCharacter(this);
	}
	@Override
	public PegObject simpleMatch(PegObject inNode, ParserContext context) {
		char ch = context.getChar();
		if(!this.charset.match(ch)) {
			return context.foundFailure(this);
		}
		context.consume(1);
		return inNode;
	}
}

class PegLabel extends PegAtom {
	public PegLabel(String token) {
		super(token);
	}
	@Override
	protected Peg clone(String ns) {
		if(ns != null && ns.length() > 0) {
			return new PegLabel(ns + this.symbol);
		}
		return this;
	}
	@Override protected PegObject simpleMatch(PegObject parentNode, ParserContext context) {
		return context.parsePegObject(parentNode, this.symbol);
	}
	@Override
	protected void makeList(PegRuleSet parser, UniArray<String> list, UniMap<String> set) {
		if(!set.hasKey(this.symbol)) {
			Peg next = parser.getRule(this.symbol);
			list.add(this.symbol);
			set.put(this.symbol, this.symbol);
			next.makeList(parser, list, set);
		}
	}
	@Override
	protected void verify(String ruleName, PegRuleSet rules) {
		if(!rules.hasRule(this.symbol)) {
			Main._PrintLine(this.source.formatErrorMessage("error", this.sourcePosition, "undefined label: " + this.symbol));
			rules.foundError = true;
		}
	}
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitPegLabel(this);
	}
}

abstract class PegUnary extends Peg {
	Peg innerExpr;
	boolean prefix;
	public PegUnary(Peg e, boolean prefix) {
		super();
		this.innerExpr = e;
		this.prefix = prefix;
	}
	@Override
	public final int size() {
		return 1;
	}
	@Override
	public final Peg get(int index) {
		return this.innerExpr;
	}
	protected abstract String getOperator();
	@Override
	protected final void stringfy(UniStringBuilder sb, boolean debugMode) {
		if(this.prefix) {
			sb.append(this.getOperator());
		}
		if(this.innerExpr instanceof PegAtom || this.innerExpr instanceof PegNewObject) {
			this.innerExpr.stringfy(sb, debugMode);
		}
		else {
			sb.append("(");
			this.innerExpr.stringfy(sb, debugMode);
			sb.append(")");
		}
		if(!this.prefix) {
			sb.append(this.getOperator());
		}
	}
	@Override
	protected void makeList(PegRuleSet parser, UniArray<String> list, UniMap<String> set) {
		this.innerExpr.makeList(parser, list, set);
	}
	@Override
	protected void verify(String ruleName, PegRuleSet rules) {
		this.ruleName = ruleName;
		this.innerExpr.verify(ruleName, rules);
	}
}

class PegOptional extends PegUnary {
	public PegOptional(Peg e) {
		super(e, false);
	}
	@Override
	protected Peg clone(String ns) {
		Peg e = this.innerExpr.clone(ns);
		if(e != this) {
			return new PegOptional(e);
		}
		return this;
	}
	@Override
	protected String getOperator() {
		return "?";
	}
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitPegOptional(this);
	}
	@Override protected PegObject simpleMatch(PegObject innode, ParserContext context) {
		int stackPosition = context.getStackPosition(this);
		Peg errorPeg = context.storeFailurePeg();
		int errorPosition = context.storeFailurePosition();
		PegObject parsedNode = this.innerExpr.performMatch(innode, context);
		if(parsedNode.isFailure()) {
			context.popBack(stackPosition, Peg._BackTrack);
			context.restoreFailure(errorPeg, errorPosition);
			return innode;
		}
		return parsedNode;
	}
}

class PegOneMore extends PegUnary {
	public int atleast = 0; 
	protected PegOneMore(Peg e, int atLeast) {
		super(e, false);
		this.atleast = atLeast;
	}
	public PegOneMore(String ruleName, Peg e) {
		this(e, 1);
	}
	@Override
	protected Peg clone(String ns) {
		Peg e = this.innerExpr.clone(ns);
		if(e != this) {
			return new PegOneMore(this.ruleName, e);
		}
		return this;
	}
	@Override
	protected String getOperator() {
		return "+";
	}
	@Override
	public PegObject simpleMatch(PegObject parentNode, ParserContext context) {
		PegObject prevNode = parentNode;
		Peg errorPeg = context.storeFailurePeg();
		int errorPosition = context.storeFailurePosition();
		int count = 0;
		while(context.hasChar()) {
			int startPosition = context.getPosition();
			PegObject node = this.innerExpr.performMatch(prevNode, context);
			if(node.isFailure()) {
				break;
			}
			prevNode = node;
			if(!(startPosition < context.getPosition())) {
				break;
			}
			count = count + 1;
		}
		context.restoreFailure(errorPeg, errorPosition);
		if(count < this.atleast) {
			return context.foundFailure(this);
		}
		return prevNode;
	}

	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitOneMore(this);
	}
}

class PegZeroMore extends PegOneMore {
	public PegZeroMore(Peg e) {
		super(e, 0);
	}
	@Override
	protected Peg clone(String ns) {
		Peg e = this.innerExpr.clone(ns);
		if(e != this) {
			return new PegZeroMore(e);
		}
		return this;
	}
	@Override
	protected String getOperator() {
		return "*";
	}
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitZeroMore(this);
	}
}

class PegAnd extends PegUnary {
	PegAnd(Peg e) {
		super(e, true);
	}
	@Override
	protected Peg clone(String ns) {
		Peg e = this.innerExpr.clone(ns);
		if(e != this) {
			return new PegAnd(e);
		}
		return this;
	}
	@Override
	protected String getOperator() {
		return "&";
	}
	@Override
	protected PegObject simpleMatch(PegObject parentNode, ParserContext context) {
		PegObject node = parentNode;
		int stackPosition = context.getStackPosition(this);
		node = this.innerExpr.performMatch(node, context);
		context.popBack(stackPosition, Peg._BackTrack);
		return node;
	}
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitPegAnd(this);
	}
}

class PegNot extends PegUnary {
	PegNot(Peg e) {
		super(e, true);
	}
	@Override
	protected Peg clone(String ns) {
		Peg e = this.innerExpr.clone(ns);
		if(e != this) {
			return new PegNot(e);
		}
		return this;
	}
	@Override
	protected String getOperator() {
		return "!";
	}
	@Override
	protected PegObject simpleMatch(PegObject parentNode, ParserContext context) {
		PegObject node = parentNode;
		int stackPosition = context.getStackPosition(this);
		node = this.innerExpr.performMatch(node, context);
		context.popBack(stackPosition, Peg._BackTrack);
		if(node.isFailure()) {
			return parentNode;
		}
		return context.foundFailure(this);
	}
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitPegNot(this);
	}
}

abstract class PegList extends Peg {
	protected UniArray<Peg> list;
	PegList() {
		super();
		this.list = new UniArray<Peg>(new Peg[2]);
	}
	public final int size() {
		return this.list.size();
	}
	public final Peg get(int index) {
		return this.list.ArrayValues[index];
	}
	public void add(Peg e) {
		this.list.add(e);
	}
	@Override protected void stringfy(UniStringBuilder sb, boolean debugMode) {
		for(int i = 0; i < this.size(); i++) {
			if(i > 0) {
				sb.append(" ");
			}
			Peg e = this.get(i);
			if(e instanceof PegChoice || e instanceof PegSequence) {
				sb.append("(");
				e.stringfy(sb, debugMode);
				sb.append(")");
			}
			else {
				e.stringfy(sb, debugMode);
			}
		}
	}
	@Override
	protected void makeList(PegRuleSet parser, UniArray<String> list, UniMap<String> set) {
		for(int i = 0; i < this.size(); i++) {
			this.get(i).makeList(parser, list, set);
		}
	}
	@Override
	protected void verify(String ruleName, PegRuleSet rules) {
		this.ruleName = ruleName;
		for(int i = 0; i < this.size(); i++) {
			Peg e  = this.get(i);
			e.verify(ruleName, rules);
		}
	}
}

class PegSequence extends PegList {
	PegSequence() {
		super();
	}
	@Override
	protected Peg clone(String ns) {
		boolean hasClone = false;
		for(int i = 0; i < this.list.size(); i++) {
			Peg e  = this.list.ArrayValues[i].clone(ns);
			if(e != this.list.ArrayValues[i]) {
				hasClone = true;
			}
		}
		if(hasClone) {
			PegList l = new PegSequence();
			for(int i = 0; i < this.list.size(); i++) {
				l.list.add(this.get(i).clone(ns));
			}
			return l;
		}
		return this;
	}
	@Override
	protected PegObject simpleMatch(PegObject innode, ParserContext context) {
		int stackPosition = context.getStackPosition(this);
		for(int i = 0; i < this.size(); i++) {
			Peg e  = this.get(i);
			PegObject parsedNode = e.performMatch(innode, context);
			if(parsedNode.isFailure()) {
				context.popBack(stackPosition, Peg._BackTrack);
				return parsedNode;
			}
//			if(innode != parsedNode) {
//				System.out.println("BEFORE: inputNode " + innode);
//				System.out.println("e: " + e.getClass().getSimpleName());
//				System.out.println("AFTER: parsedNode " + parsedNode);
//			}
			innode = parsedNode;
		}
		return innode;
	}
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitSequence(this);
	}
	public Peg cdr() {
		PegSequence seq = new PegSequence(); 
		for(int i = 1; i < this.size(); i++) {
			Peg e  = this.list.ArrayValues[i];
			seq.list.add(e);
		}
		return seq;
	}
}

class PegChoice extends PegList {
	boolean hasCatch = false;
	PegChoice() {
		super();
	}
	@Override
	protected Peg clone(String ns) {
		boolean hasClone = false;
		for(int i = 0; i < this.size(); i++) {
			Peg e  = this.get(i).clone(ns);
			if(e != this.get(i)) {
				hasClone = true;
			}
		}
		if(hasClone) {
			PegChoice l = new PegChoice();
			l.hasCatch = this.hasCatch;
			for(int i = 0; i < this.list.size(); i++) {
				l.list.add(this.get(i).clone(ns));
			}
			return l;
		}
		return this;
	}
	public void extend(Peg e) {
		if(e instanceof PegChoice) {
			for(int i = 0; i < e.size(); i++) {
				this.add(e.get(i));
			}
		}
		else if(e != null) {
			this.list.add(e);
//			if(e.hasLeftRecursion() == true) {
//				this.setLeftRecursion(true);
//			}
		}
	}
	@Override protected void stringfy(UniStringBuilder sb, boolean debugMode) {
		for(int i = 0; i < this.size(); i++) {
			if(i > 0) {
				sb.append(" / ");
			}
			Peg e = this.list.ArrayValues[i];
			e.stringfy(sb, debugMode);
		}
	}
	@Override
	protected PegObject simpleMatch(PegObject inNode, ParserContext context) {
		int stackPosition = context.getStackPosition(this);
		PegObject node = inNode;
		Peg errorPeg = context.storeFailurePeg();
		int errorPosition = context.storeFailurePosition();
		for(int i = 0; i < this.size(); i++) {
			Peg e  = this.get(i);
			if(e instanceof PegCatch) {
				node = context.newPegObject("#error");
				node.createdPeg = context.storeFailurePeg();
				node.startIndex = context.storeFailurePosition();
				node.endIndex = context.storeFailurePosition();
				if(Main.PegDebuggerMode) {
					Main._PrintLine(node.formatSourceMessage("error: " + this.ruleName, " by " + node.createdPeg));
				}
				context.restoreFailure(errorPeg, errorPosition);
				return e.performMatch(node, context);
			}
			node = e.performMatch(inNode, context);
			if(!node.isFailure()) {
				break;
			}
			context.popBack(stackPosition, Peg._BackTrack);
		}
		return node;
	}
	@Override
	protected void verify(String ruleName, PegRuleSet rules) {
		for(int i = 0; i < this.size(); i++) {
			Peg e  = this.get(i);
			e.verify(ruleName, rules);
			if(e instanceof PegCatch) {
				this.hasCatch = true;
			}
		}
	}
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitChoice(this);
	}
}

class PegSetter extends PegUnary {
	public int index;
	public PegSetter(String ruleName, Peg e, int index) {
		super(e, false);
		this.innerExpr = e;
		this.index = index;
	}
	@Override
	protected Peg clone(String ns) {
		Peg e = this.innerExpr.clone(ns);
		if(e != this) {
			return new PegSetter(this.ruleName, e, this.index);
		}
		return this;
	}
	@Override
	protected String getOperator() {
		if(this.index != -1) {
			return "^" + this.index;
		}
		return "^";
	}
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitSetter(this);
	}
	@Override
	public PegObject simpleMatch(PegObject parentNode, ParserContext context) {
		PegObject node = this.innerExpr.performMatch(parentNode, context);
		if(!context.isVerifyMode()) {
			if(node.isFailure() || parentNode == node) {
				return node;
			}
			context.push(this, parentNode, this.index, node);
			return parentNode;
		}
		return node;
	}
}

class PegTag extends PegAtom {
	public PegTag(String ruleName, String objectLabel) {
		super(objectLabel);
	}
	@Override
	protected Peg clone(String ns) {
		return this;
	}
	@Override
	protected final void stringfy(UniStringBuilder sb, boolean debugMode) {
		if(debugMode) {
			sb.append(this.symbol);
		}
	}
	@Override
	public PegObject simpleMatch(PegObject inNode, ParserContext context) {
		if(!context.isVerifyMode()) {
			inNode.name = this.symbol;
		}
		return inNode;
	}
	@Override
	protected void verify(String ruleName, PegRuleSet rules) {
		this.ruleName = ruleName;
		rules.addObjectLabel(this.symbol);
	}
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitObjectLabel(this);
	}
}

class PegNewObject extends PegList {
	boolean leftJoin = false;
	String nodeName = "noname";
	public PegNewObject(String ruleName, boolean leftJoin) {
		super();
		this.leftJoin = leftJoin;
	}
	@Override
	protected Peg clone(String ns) {
		boolean hasClone = false;
		for(int i = 0; i < this.list.size(); i++) {
			Peg e  = this.list.ArrayValues[i].clone(ns);
			if(e != this.list.ArrayValues[i]) {
				hasClone = true;
			}
		}
		if(hasClone) {
			PegList l = new PegNewObject(this.ruleName, this.leftJoin);
			for(int i = 0; i < this.list.size(); i++) {
				l.list.add(this.get(i).clone(ns));
			}
			return l;
		}
		return this;
	}

	public void add(Peg e) {
		if(e instanceof PegSequence) {
			for(int i =0; i < e.size(); i++) {
				this.list.add(e.get(i));
			}
		}
		else {
			this.list.add(e);
		}
	}

	@Override
	protected final void stringfy(UniStringBuilder sb, boolean debugMode) {
		if(debugMode) {
			if(this.leftJoin) {
				sb.append("8<^ ");
			}
			else {
				sb.append("8< ");
			}
		}
		else {
			sb.append("( ");
		}
		super.stringfy(sb, debugMode);
		if(debugMode) {
			sb.append(" >8");
		}
		else {
			sb.append(" )");
		}
	}
	@Override
	public PegObject simpleMatch(PegObject inNode, ParserContext context) {
		PegObject checkedNode = context.precheck(this, inNode);
		if(checkedNode == null) {
			PegObject leftNode = inNode;
			int pos = context.getPosition();
			int stack = context.getStackPosition(this);
			PegObject newnode = context.newPegObject(this.nodeName);
			newnode.setSource(this, context.source, pos, context.getPosition());
			if(this.leftJoin) {
				context.push(this, newnode, 0, leftNode);
			}
			for(int i = 0; i < this.size(); i++) {
				Peg e = this.get(i);
				PegObject node = e.performMatch(newnode, context);
				if(node.isFailure()) {
					//System.out.println("** failed[" + pos + "] " + this);
					context.popBack(stack, true);
					return node;
				}
				//			if(node != newnode) {
				//				this.warning("dropping @" + newnode.name + " " + node);
				//			}
			}
			int top = context.getStackPosition(this);
			context.addSubObject(newnode, stack, top);
			newnode.setSource(this, context.source, pos, context.getPosition());
			newnode.checkNullEntry();
			//System.out.println("** created[" + pos + "] " + newnode);
			context.removeMemo(pos+1, context.getPosition());
			checkedNode = newnode;
		}
		return checkedNode;
	}

	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitNewObject(this);
	}
}

// (e / catch e)

class PegCatch extends PegUnary {
	PegCatch (String ruleName, Peg first) {
		super(first, true);
	}
	@Override
	protected Peg clone(String ns) {
		Peg e = this.innerExpr.clone(ns);
		if(e != this) {
			return new PegCatch(this.ruleName, e);
		}
		return this;
	}
	@Override
	protected String getOperator() {
		return "catch ";
	}
	@Override
	protected PegObject simpleMatch(PegObject inNode, ParserContext context) {
		this.innerExpr.performMatch(inNode, context);
		return inNode;
	}
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitCatch(this);
	}
}

class PegIndent extends PegAtom {
	PegIndent(String ruleName) {
		super("indent");
	}
	@Override
	protected Peg clone(String ns) {
		return this;
	}
	@Override
	protected PegObject simpleMatch(PegObject inNode, ParserContext context) {
		if(inNode.source != null) {
			String indent = inNode.source.getIndentText(inNode.startIndex);
			//System.out.println("###" + indent + "###");
			if(context.match(indent)) {
				return inNode;
			}
		}
		return inNode;
	}
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitIndent(this);
	}
}

class PegCType extends PegAtom {
	static public HashSet<String> typedefs = new HashSet<String>();
	boolean AddType = false;
	PegCType(String leftLabel, boolean AddType) {
		super(AddType ? "addtype" : "ctype");
		this.AddType = AddType;
	}

	@Override
	protected Peg clone(String ns) {
		return this;
	}
	@Override
	protected PegObject simpleMatch(PegObject inNode, ParserContext context) {
		if(inNode.source != null) {
			if(AddType) {
				if(inNode.name.equals("#DeclarationNoAttribute") && inNode.AST.length >= 2) {
					// inNode.AST = [typedef struct A, StructA]
					PegObject first = inNode.AST[0];
					if(first.AST.length >= 2) {
						String firstText = first.AST[0].getText().trim();
						// first is "typedef"
						if(first.AST[0].name.equals("#storageclassspecifier") && firstText.equals("typedef")) {
							PegObject second = inNode.AST[1];
							for (int i = 0; i < second.AST.length; i++) {
								PegObject decl = second.get(i);
								if(decl.name.equals("#declarator")) {
									// "typedef struct A StructA;"
									// add new typename StructA
									System.out.println(decl.get(decl.AST.length - 1).getText());
									typedefs.add(decl.get(decl.AST.length - 1).getText());
								}
							}
							return inNode;
						}
					}
				}
			}
			else {
				String name = inNode.getText().trim();
				if (!typedefs.contains(name)) {
					return new PegObject(null); //not match
				}
			}
		}
		return inNode;
	}

	@Override
	public void accept(PegVisitor visitor) {
		// TODO Auto-generated method stub
	}

}
