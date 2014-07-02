//package org.libbun;
//
//public class PegParserContext extends ParserContext {
//
//	final UniArray<Log> logStack = new UniArray<Log>(new Log[128]);
//	private int stackTop = 0;
//
//	private UniMap<Memo> memoMap = new UniMap<Memo>();
//
//	int memoHit = 0;
//	int memoMiss = 0;
//	int memoSize = 0;
//
//	public PegParserContext(PegSource source, int startIndex, int endIndex) {
//		super(source, startIndex, endIndex);
//	}
//
//	@Override
//	public SourceContext subContext(int startIndex, int endIndex) {
//		return new PegParserContext(this.source, startIndex, endIndex);
//	}
//	
//	public void initMemo() {
//		this.memoMap = new UniMap<Memo>();
//	}
//	
//	public UniMap<Memo> getMemoMap() {
//		return memoMap;
//	}
//
////	public boolean isLeftRecursion(String PatternName) {
////		Peg e = this.ruleSet.getRightJoinRule(PatternName);
////		return e != null;
////	}
//
//	public final PegObject parsePegNode(PegObject parentNode, String pattern) {
//		int pos = this.getPosition();
//		String key = pattern + ":" + pos;
//		Memo m = this.memoMap.get(key, null);
//		if(m == null) {
//			m = new Memo();
//			m.nextPosition = this.getPosition();
//			m.result = this.foundFailureNode;
//			this.memoMap.put(key, m);
//			this.memoMiss = this.memoMiss + 1;
//			Peg e = this.ruleSet.getRule(pattern);
//			PegObject ans = e.debugMatch(parentNode, this);
//			if(pos == this.getPosition() && !ans.isFailure()) {
//				this.memoMap.remove(key);
//				return ans;
//			}
//			else {
//				m.result = ans;
//				m.nextPosition = this.getPosition();
//				if(e.hasLeftRecursion()) {
//					return growLeftRecursion(pattern, pos, m, parentNode);
//				}
//				else {
//					return ans;
//				}
//			}
//		}
//		else {
//			this.memoHit = this.memoHit + 1;
//			this.sourcePosition = m.nextPosition;
//			if(m.result == null) {
//				return parentNode;
//			}
//			return m.result;
//		}
//	}
//	
//	public final PegObject growLeftRecursion(String pattern, int pos, Memo m, PegObject parentNode) {
//		while(true) {
//			this.setPosition(pos);
//			Peg e = this.ruleSet.getRule(pattern);
//			PegObject ans = e.debugMatch(parentNode, this);
//			if(ans.isFailure() || this.getPosition() <= m.nextPosition){
//				break;
//			}
//			m.result = ans;
//			m.nextPosition = this.getPosition();
//		}
//		this.setPosition(m.nextPosition);
//		return m.result;
//	}
//
//	public final PegObject parsePegNodeNon(PegObject parentNode, String pattern, boolean hasNextChoice) {
//		Peg e = this.ruleSet.getRule(pattern);
//		if(e != null) {
//			return e.debugMatch(parentNode, this);
//		}
//		Main._Exit(1, "undefined label " + pattern);
//		return this.foundFailureNode;
//	}
//
//	public final PegObject parseRightPegNode(PegObject left, String symbol) {
//		String key = this.ruleSet.nameRightJoinName(symbol);
//		Peg e = this.ruleSet.getRule(key);
//		if(e != null) {
//			PegObject right = e.debugMatch(left, this);
//			if(!right.isFailure()) {
//				left = right;
//			}
//		}
//		return left;
//	}
//
//	public final int getStackPosition(Peg trace) {
//		this.pushImpl(trace, null, '\0', null, 0, null);
//		return this.stackTop;
//	}
//
//	private void pushImpl(Peg trace, String msg, char type, PegObject parentNode, int index, PegObject childNode) {
//		Log log = null;
//		if(this.stackTop < this.logStack.size()) {
//			if(this.logStack.ArrayValues[this.stackTop] == null) {
//				this.logStack.ArrayValues[this.stackTop] = new Log();
//			}
//			log = this.logStack.ArrayValues[this.stackTop];
//		}
//		else {
//			log = new Log();
//			this.logStack.add(log);
//		}
//		log.trace = trace;
//		log.sourcePosition = this.sourcePosition;
//		log.msg = msg;
//		log.type = type;
//		log.parentNode = parentNode;
//		log.index = index;
//		log.childNode = childNode;
//		this.stackTop = this.stackTop + 1;
//	}
//
//	void pushLog(Peg trace, String msg) {
//		this.pushImpl(trace, msg, 'm', null, 0, null);
//	}
//
//	public void popBack(int stackPostion, boolean backtrack) {
//		this.stackTop = stackPostion-1;
//		Log log = this.logStack.ArrayValues[stackPostion-1];
//		if(backtrack) {
//			this.rollback(log.sourcePosition);
//		}
//	}
//
//	public void push(Peg trace, PegObject parentNode, int index, PegObject node) {
//		this.pushImpl(trace, "", 'p', parentNode, index, node);
//	}
//
//	@Override
//	public void addSubObject(PegObject newnode, int stack, int top) {
//		for(int i = stack; i < top; i++) {
//			Log log = this.logStack.ArrayValues[i];
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
//	public void showStatInfo(PegObject parsedObject) {
//		System.out.println("hit: " + this.memoHit + ", miss: " + this.memoMiss);
//		System.out.println("created_object=" + this.objectCount + ", used_object=" + parsedObject.count());
//		System.out.println("backtrackCount: " + this.backtrackCount + ", backtrackLength: " + this.backtrackSize);
//		System.out.println();
//	}
//
//}
//
//class Log {
//	int sourcePosition;
//	Peg trace;
//	String msg;
//	char type;
//	PegObject parentNode;
//	int index;
//	PegObject childNode;
//
//	@Override public String toString() {
//		return "" + this.sourcePosition + " " + this.msg;
//	}
//}
//
//class Memo {
//	PegObject result;
//	int nextPosition;
//}
