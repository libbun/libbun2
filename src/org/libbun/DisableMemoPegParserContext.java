//package org.libbun;
//
//public class DisableMemoPegParserContext extends ParserContext {
//
//	private UniMap<Peg>        pegCache;
//
//	final UniArray<SimpleLog> logStack = new UniArray<SimpleLog>(new SimpleLog[128]);
//	private int stackTop = 0;
//
//	class SimpleLog {
//		int sourcePosition;
//		Peg trace;
//		char type;
//		PegObject parentNode;
//		int index;
//		PegObject childNode;
//	}
//
//	public DisableMemoPegParserContext(PegRuleSet parser, PegSource source) {
//		this(parser, source, 0, source.sourceText.length());
//	}
//
//	public DisableMemoPegParserContext(PegRuleSet parser, PegSource source, int startIndex, int endIndex) {
//		super(parser, source, startIndex, endIndex);
//		this.loadPegDefinition(this.ruleSet.pegMap);
//	}
//
//	public final void loadPegDefinition(UniMap<Peg> pegMap) {
//		this.pegCache = new UniMap<Peg>();
//		UniArray<String> list = this.pegCache.keys();
//		for(int i = 0; i < list.size(); i++) {
//			String key = list.ArrayValues[i];
//			Peg e = this.pegCache.get(key, null);
//			if(Main.PegDebuggerMode) {
//				System.out.println(e.toPrintableString(key, "\n  = ", "\n  / ", "\n  ;", true));
//			}
//		}
//	}
//	@Override
//	public SourceContext subContext(int startIndex, int endIndex) {
//		return new DisableMemoPegParserContext(this.ruleSet, this.source, startIndex, endIndex);
//	}
//
//	public void initMemo() {
//	}
//
//	public final Peg getRightPattern(String name) {
//		return this.pegCache.get(this.ruleSet.nameRightJoinName(name), null);
//	}
//
//	public final PegObject parsePegNode(PegObject parentNode, String pattern) {
//		Peg e = this.ruleSet.getRule(pattern);
//		return e.debugMatch(parentNode, this);
//	}
//
//	public final int getStackPosition(Peg trace) {
//		this.pushImpl(trace, null, '\0', null, 0, null);
//		return this.stackTop;
//	}
//
//	private void pushImpl(Peg trace, String msg, char type, PegObject parentNode, int index, PegObject childNode) {
//		SimpleLog log = null;
//		if(this.stackTop < this.logStack.size()) {
//			if(this.logStack.ArrayValues[this.stackTop] == null) {
//				this.logStack.ArrayValues[this.stackTop] = new SimpleLog();
//			}
//			log = this.logStack.ArrayValues[this.stackTop];
//		}
//		else {
//			log = new SimpleLog();
//			this.logStack.add(log);
//		}
//		log.trace = trace;
//		log.sourcePosition = this.sourcePosition;
//		log.type = type;
//		log.parentNode = parentNode;
//		log.index = index;
//		log.childNode = childNode;
//		this.stackTop = this.stackTop + 1;
//	}
//
//	@Override
//	public void addSubObject(PegObject newnode, int stack, int top) {
//		for(int i = stack; i < top; i++) {
//			SimpleLog log = this.logStack.ArrayValues[i];
//			if(log.type == 'p' && log.parentNode == newnode) {
//				if(log.index == -1) {
//					newnode.append(log.childNode);
//				}
//				else {
//					newnode.set(log.index, log.childNode);
//				}
//			}
//		}
//	}
//
//	public void popBack(int stackPostion, boolean backtrack) {
//		this.stackTop = stackPostion-1;
//		SimpleLog log = this.logStack.ArrayValues[stackPostion-1];
//		if(backtrack) {
//			this.rollback(log.sourcePosition);
//		}
//	}
//
//	public void push(Peg trace, PegObject parentNode, int index, PegObject node) {
//		this.pushImpl(trace, "", 'p', parentNode, index, node);
//	}
//
//	public void showStatInfo(PegObject parsedObject) {
//		System.out.println("created_object=" + this.objectCount + ", used_object=" + parsedObject.count());
//		System.out.println("backtrackCount: " + this.backtrackCount + ", backtrackLength: " + this.backtrackSize);
//		System.out.println();
//	}
//
//}
