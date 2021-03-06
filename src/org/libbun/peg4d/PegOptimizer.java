package org.libbun.peg4d;

import org.libbun.Main;
import org.libbun.UCharset;
import org.libbun.UList;
import org.libbun.UMap;

class PegOptimizer extends PegTransformer {
	Grammar rules;
	UMap<Peg> pegCache;
	private int OptimizationLevel = 2;
	int statOptimizedPeg = 0;
	int statInlineCount = 0;
	int statChoice = 0;
	int statPredictableChoice = 0;
	
	PegOptimizer(Grammar ruleSet, UMap<Peg> cacheMap) {
		this.rules = ruleSet;
		this.pegCache = cacheMap;
		if(ruleSet == Grammar.PegGrammar) {
			this.OptimizationLevel = 2;
		}
		else {
			this.OptimizationLevel = Main.OptimizedLevel;
		}
	}
	
	public Peg transform(Peg e) {
		if(this.OptimizationLevel > 0) {
			if(e instanceof PegNonTerminal) {
				return this.extractInline(((PegNonTerminal) e));
			}
			if(e instanceof PegChoice) {
				return this.predictChoice(((PegChoice) e));
			}
			if(e instanceof PegNewObject) {
				return this.predictNewObject(((PegNewObject) e));
			}
			if(e instanceof PegUnary) {
				Peg inner = ((PegUnary) e).inner;
				if(e instanceof PegNot) {
					return peepNot(e, inner.clone(this));
				}
				if(e instanceof PegSetter) {
					return peepSetter(e, inner);
				}
			}
			return this.peepHoleTransform(e);
		}
		return null;
	}
	
	private final Peg extractInline(PegNonTerminal label) {
		String ruleName = label.symbol;
		Peg next = rules.getRule(ruleName);
		if(this.OptimizationLevel > 3) {
			label.predicted = next.getPrediction();  // super prediction
		}
		if(next.is(Peg.CyclicRule) || !isTextMatchOnly(next)) {
			return null;
		}
		Peg n = pegCache.get(ruleName);
		if(n == null) {
			n = next.clone(this);
			pegCache.put(ruleName, n);
		}				
		this.statInlineCount += 1;
		log(label, "inlining: " + ruleName);
		return n;
	}

	private final void appendChoiceList(UList<Peg> flatList, Peg e) {
		if(flatList.size() > 0 && (e instanceof PegString1 || e instanceof PegCharacter)) {
			Peg prev = flatList.ArrayValues[flatList.size()-1];
			if(prev instanceof PegString1) {
				PegCharacter c = new PegCharacter("");
				c.charset.append(((PegString1) prev).symbol);
				flatList.ArrayValues[flatList.size()-1] = c;
				prev = c;
			}
			if(prev instanceof PegCharacter) {
				UCharset charset = ((PegCharacter) prev).charset;
				if(e instanceof PegCharacter) {
					charset.append(((PegCharacter) e).charset);
					log(prev, "merged character: " + prev);
				}
				else {
					charset.append(((PegString1) e).symbol);
					log(prev, "merged character: " + prev);
				}
				return;
			}
		}
		flatList.add(e);
	}
	
	private final Peg predictChoice(PegChoice orig) {
		UList<Peg> flatList = new UList<Peg>(new Peg[orig.size()]);
		for(int i = 0; i < orig.size(); i++) {
			Peg sub = orig.get(i).clone(this);
			if(sub instanceof PegChoice) {
				log(orig, "flaten choice: " + sub);
				for(int j = 0; j < sub.size(); j++) {
					this.appendChoiceList(flatList, sub.get(j));
				}
			}
			else {
				this.appendChoiceList(flatList, sub);
			}
		}
		if(flatList.size() == 1) {
			log(orig, "removed choice: " + flatList.ArrayValues[0]);
			return flatList.ArrayValues[0];
		}
		PegChoice newChoice = new PegChoice(orig.flag);
		newChoice.list = flatList;
		Object predicted = newChoice.getPrediction();
		this.statPredictableChoice += newChoice.predictable;
		this.statChoice += newChoice.size();
		if(newChoice.predictable > 0) {
			if(this.OptimizationLevel >= 3 && newChoice.predictable == newChoice.size()) {
				if(newChoice.pretextSize > 1 && newChoice.predictable > 2) {
					PegMappedChoice choice = new PegMappedChoice(newChoice);
					log2(orig, "mapped choice: " + choice.map.keys());
					return choice;
				}
				if(newChoice.pretextSize == 1) {
					PegMappedCharacterChoice choice = new PegMappedCharacterChoice(newChoice);
					log2(orig, "mapped choice: " + choice.keyList);
					return choice;
				}
			}
			else {
				//System.out.println("@@ " + newChoice +"\n\t p=" + newChoice.predictable + "<" + newChoice.size() + " size=" + newChoice.pretextSize);
				if(this.OptimizationLevel >= 4 && newChoice.predictable > (newChoice.size() / 2)) {
					PegSelectiveChoice choice = new PegSelectiveChoice(newChoice);
					log2(orig, "selective choice: " + choice.predictable + ", " + choice.size());
				}
			}
		}
		return newChoice;
	}
	
	private final Peg predictNewObject(PegNewObject orig) {
		PegNewObject ne = new PegNewObject(orig.leftJoin);
		ne.flag = orig.flag;
		int predictionIndex = -1;
		for(int i = 0; i < orig.size(); i++) {
			Peg sub = orig.get(i).clone(this);
			ne.list.add(sub);
			if(isNeedsObjectCreation(sub) && predictionIndex == -1) {
				predictionIndex = i;
			}
		}
		if(predictionIndex == -1) {
			predictionIndex=0;
		}
		if(this.OptimizationLevel >= 3 && predictionIndex > 0) {
			log(orig, "prediction index: " + predictionIndex);
			ne.predictionIndex = predictionIndex;
		}
		return ne;
	}
	
	private boolean isNeedsObjectCreation(Peg e) {
		if(e.is(Peg.HasNewObject) || e.is(Peg.HasSetter) || e.is(Peg.HasTagging) || e.is(Peg.HasMessage) || e.is(Peg.HasContext)) {
			return true;
		}
		return false;
	}
	
	public Peg peepHoleTransform(Peg e) {
		if(e instanceof PegString) {
			String symbol = ((PegString) e).text;
			if(symbol.length() == 1) {
				log(e, "string1: " + e);
				return new PegString1(e, symbol);
			}
			if(symbol.length() == 0) {
				log(e, "empty: " + e);
				return new PegEmpty(e);
			}
			return null;
		}
		if(e instanceof PegOptional) {
			Peg inner = ((PegOptional) e).inner;
			if(inner instanceof PegString) {
				log(e, "optional string: " + e);
				return new PegOptionalString(e, ((PegString) inner).text);
			}
			if(inner instanceof PegCharacter) {
				log(e, "optional character: " + e);
				return new PegOptionalCharacter(e, ((PegCharacter) inner).charset);
			}
		}
		if(e instanceof PegRepeat) {
			PegRepeat re = (PegRepeat)e;
			Peg inner = re.inner;
			if(inner instanceof PegCharacter) {
				UCharset charset = ((PegCharacter) inner).charset;
				Peg ne = null;
				if(re.atleast == 1) {
					log(e, "one more character: " + e);
					ne = new PegOneMoreCharacter(e, charset);
				}
				else {
					log(e, "zero more character: " + e);
					ne = new PegZeroMoreCharacter(e, charset);
				}
				return ne;
			}
			if(isTextMatchOnly(inner)) {
				inner = inner.clone(this);
				if(re.atleast == 1) {
					log(e, "one more text: " + e);
					return new PegOneMoreText(e, inner);
				}
				else {
					log(e, "zero more text: " + e);
					return new PegZeroMoreText(e, inner);
				}
			}
		}
		if(e instanceof PegSequence) {
			PegSequence seq = (PegSequence)e;
			if(seq.size() == 2 && seq.get(1) instanceof PegAny) {
				Peg ne = this.transform(seq.get(0));
				if(ne instanceof PegNotAtom) {
					log(e, "merged not and any: " + e);
					((PegNotAtom) ne).setNextAny(e);
					return ne;
				}

			}
		}
		return null;
	}
	
	private final Peg peepNot(Peg orig, Peg inner) {
		if(inner instanceof PegString) {
			log(orig, "not string:" + inner);
			return new PegNotString(orig, ((PegString) inner).text);
		}
		if(inner instanceof PegString1) {
			log(orig, "not string1: " + inner);
			return new PegNotString1(orig, ((PegString1) inner).symbol);
		}
		if(inner instanceof PegCharacter) {
			log(orig, "not character: " + inner);
			return new PegNotCharacter(orig, ((PegCharacter) inner).charset);
		}
		PegNot ne = new PegNot(inner);
		ne.flag = orig.flag;
		return ne;
	}

	private final Peg peepSetter(Peg orig, Peg inner) {
		if(!inner.is(Peg.HasNewObject)) {
			log(orig, "removed stupid peg: " + orig);
			return inner.clone(this);
		}
		return null;
	}

	final void log(Peg orig, String msg) {
//		if(Main.VerbosePeg) {
//			System.out.println("optimized: " + msg);
//		}
		this.statOptimizedPeg += 1;
	}

	final void log2(Peg orig, String msg) {
//		if(Main.VerbosePeg) {
//			System.out.println("optimized: " + msg);
//		}
		this.statOptimizedPeg += 1;
	}

	private final boolean isTextMatchOnly(Peg e) {
		if(e.hasObjectOperation()) {
			return false;
		}
		if(this.OptimizationLevel < 2) {
			if(e.is(Peg.HasTagging) || e.is(Peg.HasPipe) || e.is(Peg.HasMessage) || e.is(Peg.HasContext)) {
				return false;
			}
		}
		return true;
	}

//	private final boolean isCharacter(Peg e) {
//		if(e instanceof PegCharacter || e instanceof PegString1) {
//			return true;
//		}
//		if(e instanceof PegString) {
//			if(((PegString) e).text.length() == 1) {
//				return true;
//			}
//		}
//		return false;
//	}
	
	class PegEmpty extends PegOptimized {
		public PegEmpty(Peg orig) {
			super(orig);
		}
		@Override
		public PegObject simpleMatch(PegObject left, ParserContext context) {
			return left;
		}
	}

	class PegString1 extends PegOptimized {
		char symbol;
		public PegString1(Peg orig, String token) {
			super(orig);
			this.symbol = token.charAt(0);
		}
		@Override
		public PegObject simpleMatch(PegObject left, ParserContext context) {
			long pos = context.getPosition();
			if(context.charAt(pos) == this.symbol) {
				context.consume(1);
				return left;
			}
			return context.foundFailure(this);
		}
		public Object getPrediction() {
			return ""+symbol;
		}
	}
	
	abstract class PegNotAtom extends PegOptimized {
		boolean nextAny = false;
		public PegNotAtom(Peg orig) {
			super(orig);
		}
		public final void setNextAny(Peg orig) {
			this.orig = orig;
			this.nextAny = true;
		}
		protected final PegObject matchNextAny(PegObject left, ParserContext context) {
			if(context.hasChar()) {
				context.consume(1);
				return left;
			}
			else {
				return context.foundFailure(this);
			}
		}
	}

	class PegNotString extends PegNotAtom {
		String symbol;
		public PegNotString(Peg orig, String token) {
			super(orig);
			this.symbol = token;
		}
		@Override
		public PegObject simpleMatch(PegObject left, ParserContext context) {
			long pos = context.getPosition();
			if(context.match(this.symbol)) {
				context.setPosition(pos);
				return context.foundFailure(this);
			}
			if(this.nextAny) {
				return this.matchNextAny(left, context);
			}
			return left;
		}
	}

	class PegNotString1 extends PegNotAtom {
		char symbol;
		public PegNotString1(Peg orig, char token) {
			super(orig);
			this.symbol = token;
		}
		@Override
		public PegObject simpleMatch(PegObject left, ParserContext context) {
			if(this.symbol == context.getChar()) {
				return context.foundFailure(this);
			}
			if(this.nextAny) {
				return this.matchNextAny(left, context);
			}
			return left;
		}
	}	
	
	class PegNotCharacter extends PegNotAtom {
		UCharset charset;
		public PegNotCharacter(Peg orig, UCharset charset) {
			super(orig);
			this.charset = charset;
		}
		@Override
		public PegObject simpleMatch(PegObject left, ParserContext context) {
			long pos = context.getPosition();
			if(context.match(this.charset)) {
				context.setPosition(pos);
				return context.foundFailure(this);
			}
			if(this.nextAny) {
				return this.matchNextAny(left, context);
			}
			return left;
		}
	}

	class PegOptionalString extends PegOptimized {
		String symbol;
		public PegOptionalString(Peg orig, String token) {
			super(orig);
			this.symbol = token;
		}
		@Override
		public PegObject simpleMatch(PegObject left, ParserContext context) {
			context.match(this.symbol);
			return left;
		}
	}

	class PegOptionalCharacter extends PegOptimized {
		UCharset charset;
		public PegOptionalCharacter(Peg orig, UCharset charset) {
			super(orig);
			this.charset = charset;
		}
		@Override
		public PegObject simpleMatch(PegObject left, ParserContext context) {
			context.match(this.charset);
			return left;
		}
	}
	
	class PegOneMoreText extends PegOptimized {
		Peg repeated;
		public PegOneMoreText(Peg orig, Peg repeated) {
			super(orig);
			this.repeated = repeated;
		}
		@Override
		public PegObject simpleMatch(PegObject left, ParserContext context) {
			long pos = context.getPosition();
			PegObject node = this.repeated.simpleMatch(left, context);
			if(node.isFailure()) {
				context.setPosition(pos);
				return node;
			}
			left = node;
			while(context.hasChar()) {
				pos = context.getPosition();
				node = this.repeated.simpleMatch(left, context);
				if(node.isFailure() || pos == context.getPosition()) {
					context.setPosition(pos);
					break;
				}
				left = node;
			}
			return left;
		}
	}

	class PegZeroMoreText extends PegOptimized {
		Peg repeated;
		public PegZeroMoreText(Peg orig, Peg repeated) {
			super(orig);
			this.repeated = repeated;
		}
		@Override
		public PegObject simpleMatch(PegObject left, ParserContext context) {
			while(context.hasChar()) {
				long pos = context.getPosition();
				PegObject node = this.repeated.simpleMatch(left, context);
				if(node.isFailure() || pos == context.getPosition()) {
					context.setPosition(pos);
					break;
				}
				left = node;
			}
			return left;
		}
	}

	
	class PegOneMoreCharacter extends PegOptimized {
		UCharset charset;
		public PegOneMoreCharacter(Peg orig, UCharset charset) {
			super(orig);
			this.charset = charset;
		}
		@Override
		public PegObject simpleMatch(PegObject left, ParserContext context) {
			long pos = context.getPosition();
			char ch = context.charAt(pos);
			if(!this.charset.match(ch)) {
				return context.foundFailure(this);
			}
			pos++;
			for(;context.hasChar();pos++) {
				ch = context.charAt(pos);
				if(!this.charset.match(ch)) {
					break;
				}
			}
			context.setPosition(pos);
			return left;
		}
	}

	class PegZeroMoreCharacter extends PegOptimized {
		UCharset charset;
		public PegZeroMoreCharacter(Peg orig, UCharset charset) {
			super(orig);
			this.charset = charset;
		}
		@Override
		public PegObject simpleMatch(PegObject left, ParserContext context) {
			long pos = context.getPosition();
			for(;context.hasChar();pos++) {
				char ch = context.charAt(pos);
				if(!this.charset.match(ch)) {
					break;
				}
			}
			context.setPosition(pos);
			return left;
		}
	}
	
	class PegMappedChoice extends PegChoice {
		private UMap<Peg> map = new UMap<Peg>();

		PegMappedChoice(PegChoice choice) {
			super(choice.flag);
			this.list = choice.list;
			this.getPrediction();
			this.map = new UMap<Peg>();
			for(int i = 0; i < this.list.size(); i++) {
				Peg sub = this.get(i);
				String token = sub.getPrediction().toString().substring(0, this.pretextSize);
				Peg.addAsChoice(this.map, token, sub);
			}
		}

		@Override
		public PegObject simpleMatch(PegObject left, ParserContext context) {
			long pos = context.getPosition();
			String token = context.substring(pos, pos + this.pretextSize);
			Peg e = this.map.get(token);
			if(e != null) {
				PegObject node2 = e.simpleMatch(left, context);
				if(node2.isFailure()) {
					context.setPosition(pos);
				}
				return node2;
			}
			//System.out.println("failure: " + pos + " token='"+token+"' in " + this.map.keys());
			return context.foundFailure(this);
		}
		
		public PegObject simpleMatchDebug(PegObject left, ParserContext context) {
			long pos = context.getPosition();
			PegObject node = super.simpleMatch(left, context);
			context.setPosition(pos);
			PegObject node2 = this.simpleMatch(left, context);
			if(!node.equals2(node2)) {
				context.setPosition(pos);
				context.showPosition("mismatched: " + this + "  size=" + this.list.size());
			}
			return node;
		}
	}

	class PegAlwaysFailure extends PegOptimized {
		public PegAlwaysFailure(Peg orig) {
			super(orig);
			
		}
		@Override
		public PegObject simpleMatch(PegObject left, ParserContext context) {
			return context.foundFailure(this);
		}
	}

	class PegMappedCharacterChoice extends PegChoice {
		Peg[] caseOf;
		boolean foundUnicode = false;
		Peg   alwaysFailure;
		UList<String> keyList;
		PegMappedCharacterChoice(PegChoice choice) {
			super(choice.flag);
			this.list = choice.list;
			this.getPrediction();
			this.keyList = new UList<String>(new String[16]);
			this.caseOf = new Peg[128];
			for(int i = 0; i < this.list.size(); i++) {
				Peg sub = this.get(i);
				Object p = sub.getPrediction();
				if(p instanceof String) {
					int ch = ((String) p).charAt(0);
					if(ch < 128) {
						if(this.caseOf[ch] == null) {
							this.keyList.add(Main._CharToString((char)ch));
						}
						this.caseOf[ch] = Peg.appendAsChoice(this.caseOf[ch], sub);
					}
					else {
						this.foundUnicode = true;
					}
				}
				if(p instanceof UCharset) {
					UCharset u = ((UCharset) p);
					for(int ch = 0; ch < 128; ch ++) {
						if(u.hasChar((char)ch)) {
							if(this.caseOf[ch] == null) {
								this.keyList.add(Main._CharToString((char)ch));
							}
							this.caseOf[ch] = Peg.appendAsChoice(this.caseOf[ch], sub);
						}
					}
					if(u.hasUnicode()) {
						this.foundUnicode = true;
					}
				}
			}
		}

		@Override
		public PegObject simpleMatch(PegObject left, ParserContext context) {
			char ch = context.getChar();
			if(ch < 128) {
				if(caseOf[ch] != null) {
					return caseOf[ch].simpleMatch(left, context);
				}
			}
			else {
				if(this.foundUnicode) {
					return super.simpleMatch(left, context);
				}
			}
			return context.foundFailure(this);
		}
	}
	
	class PegSelectiveChoice extends PegChoice {
		Peg[] caseOf;
		Peg   alwaysFailure;
		PegSelectiveChoice(PegChoice c) {
			super(c.flag);
			this.list = c.list;
			this.predictable = c.predictable;
			this.predicted = c.predicted;
			this.pretextSize = c.pretextSize;
			this.caseOf = new Peg[128];
			this.alwaysFailure = new PegAlwaysFailure(c);
		}
		
		@Override
		public PegObject simpleMatch(PegObject left, ParserContext context) {
			char ch = context.getChar();
			if(ch < 128) {
				if(caseOf[ch] == null) {
					caseOf[ch] = selectPeg(ch);
				}
				long pos = context.getPosition();
				PegObject node = caseOf[ch].simpleMatch(left, context);
				context.setPosition(pos);
				PegObject node2 = super.simpleMatch(left, context);
				if(node.equals(node2)) {
					System.out.println("!! ch = '" + ch + "'");
				}
				return node2;
			}
			return super.simpleMatch(left, context);
		}
		
		private Peg selectPeg(char ch) {
			Peg e = null;
			for(int i = 0; i < this.list.size(); i++) {
				Peg sub = this.get(i);
				if(sub.isPredictablyAcceptable(ch)) {
					e = Peg.appendAsChoice(e, sub);
				}
			}
			if(e == null) {
				e = this.alwaysFailure;
			}
			System.out.println("selected: '" + ch + "': " + e.size() + " ** " + e);
			return e;
		}
	}

}
