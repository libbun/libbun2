//package org.libbun;
//
//public class PegParserParser extends SourceContext {
//	PegRuleSet parser;
//	boolean   isfirstSingleExpr = true;
////	public final static Peg RuleName = Peg.newOneMore(Peg.newCharacter("A-Za-z0-9_"));
////	public final static Peg WhiteSpace = Peg.newOneMore(Peg.newCharacter(" \t\r\n"));
//	
//	public PegParserParser(PegRuleSet parser, PegSource source, int startIndex, int endIndex) {
//		super(source, startIndex, endIndex);
//		this.parser = parser;
//	}
//	public PegParserParser(PegRuleSet parser, PegSource source) {
//		super(source, 0, source.sourceText.length());
//		this.parser = parser;
//	}
//	private PegParserParser subParser(int startIndex, int endIndex) {
//		return new PegParserParser(this.parser, this.source, startIndex, endIndex);
//	}
//	public boolean hasRule() {
//		this.skipComment(UniCharset.SemiColon);
//		return this.hasChar();
//	}
//	public void parseRule() {
//		if(this.match("import")) {
//			this.skipComment(UniCharset.WhiteSpaceNewLine);
//			this.parseImportFile();
//			return;
//		}
//		int startIndex = this.getPosition();
//		if(!this.match(UniCharset.Letter)) {
//			this.showErrorMessage("Is forgotten ; ?");
//		}
//		this.matchZeroMore(UniCharset.NameSymbol);
//		String label = this.substring(startIndex, this.getPosition());
//		this.skipComment(UniCharset.WhiteSpaceNewLine);
//		if(!this.match('=') && !this.match('<', '-')) {
//			this.showErrorMessage("Is forgotten ; ?");
//		}
//		Peg parsed = this.parsePegExpr(label);
//		this.parser.setRule(label, parsed);
//	}
//
//	private void parseImportFile() {
//		if(!this.match(UniCharset.Letter)) {
//			this.showErrorMessage("Is forgotten ; ?");
//		}
//		int startIndex = this.getPosition();
//		this.matchZeroMore(UniCharset.NameSymbol);
//		String label = this.substring(startIndex, this.getPosition());
//		this.skipComment(UniCharset.WhiteSpaceNewLine);
//		if(!this.match("from")) {
//			this.showErrorMessage("expected from");
//		}
//		this.skipComment(UniCharset.WhiteSpaceNewLine);
//		startIndex = this.getPosition();
//		this.matchZeroMore(UniCharset.NodeLabel);
//		String fileName = this.substring(startIndex, this.getPosition());
//		fileName = this.source.checkFileName(fileName);
//		this.parser.importRuleFromFile(label, fileName);
//	}
//
//	private int skipQuotedString(char endChar) {
//		for(; this.hasChar(); this.consume(1)) {
//			char ch = this.getChar();
//			if(ch == endChar) {
//				int index = this.getPosition();
//				this.consume(1);
//				return index;
//			}
//			if(ch == '\\') {
//				this.consume(1);  // skip next char;
//			}
//		}
//		return -1;
//	}
//
//	private int skipGroup(int openChar, int closeChar) {
//		int order = 1;
//		while(this.hasChar()) {
//			char ch = this.getChar();
//			this.consume(1);
//			if(ch == closeChar) {
//				order = order - 1;
//				if(order == 0) {
//					return this.getPosition() - 1;
//				}
//			}
//			if(ch == openChar) {
//				order = order + 1;
//			}
//			if(ch == '"' || ch == '\'') {
//				if(this.skipQuotedString(ch) == -1) {
//
//					return -1;
//				}
//			}
//			if(ch == '[') {
//				int pos = this.getPosition() - 1;
//				if(this.skipQuotedString(']') == -1) {
//					this.rollback(pos);
//					this.showErrorMessage("unclosed [");
//					return -1;
//				}
//			}
//		}
//		return -1;
//	}
//
//	private Peg parsePostfix(String leftName, Peg left) {
//		if(left != null) {
//			if(this.match('@')) {
//				int digit = this.getChar();
//				if(digit >= '0' && digit <= '9') {
//					this.consume(+1);
//					digit = digit - '0';
//				}
//				else {
//					digit = -1;
//				}
//				left = new PegSetter(leftName, left, digit);
//			}
//			if(this.match('*')) {
//				return new PegZeroMore(leftName, left);
//			}
//			if(this.match('+')) {
//				return new PegOneMore(leftName, left);
//			}
//			if(this.match('?')) {
//				return new PegOptional(leftName, left);
//			}
//		}
//		return left;
//	}
//
//	private Peg parseSingleExpr(String ruleName) {
//		Peg right = null;
//		this.skipComment(UniCharset.WhiteSpaceNewLine);
//		if(this.match(';')) {
//			this.consume(-1);
//			return null;
//		}
//		if(this.match("indent")) {
//			right = new PegIndent(ruleName);
//			return this.parsePostfix(ruleName, right);
//		}
//		if(this.match(UniCharset.Letter)) {
//			int startIndex = this.getPosition() - 1;
//			int endIndex = this.matchZeroMore(UniCharset.NameSymbol);
//			String s = this.source.substring(startIndex, endIndex);
//			right = new PegLabel(ruleName, s);
//			if(ruleName.equals(s)) {
//				right.setLeftRecursion(true);
//			}
//			right.setSource(this.source, startIndex);
//			return this.parsePostfix(ruleName, right);
//		}
//		if(this.match('.')) {
//			right = new PegAny(ruleName);
//			return this.parsePostfix(ruleName, right);
//		}
//		if(this.match('$')) {
//			right = this.parseSingleExpr(ruleName);
//			if(right != null) {
//				right = new PegSetter(ruleName, right, -1);
//			}
//			return right;
//		}
//		if(this.match('&')) {
//			right = this.parseSingleExpr(ruleName);
//			if(right != null) {
//				right = new PegAnd(ruleName, right);
//			}
//			return right;
//		}
//		if(this.match('!')) {
//			right = this.parseSingleExpr(ruleName);
//			if(right != null) {
//				right = new PegNot(ruleName, right);
//			}
//			return right;
//		}
//		if(this.match('#')) {
//			int startIndex = this.getPosition();
//			this.matchZeroMore(UniCharset.NodeLabel);
//			int endIndex = this.getPosition();
//			right = new PegObjectLabel(ruleName, this.substring(startIndex - 1, endIndex));
//			return right;
//		}
//		if(this.match('(')) {
//			int startIndex = this.getPosition();
//			int endIndex = this.skipGroup('(', ')');
//			if(endIndex == -1) {
//				this.showErrorMessage("unclosed ')'");
//				return null;
//			}
//			PegParserParser sub = this.subParser(startIndex, endIndex);
//			right = sub.parsePegExpr(ruleName);
//			if(right != null) {
//				right = this.parsePostfix(ruleName, right);
//			}
//			return right;
//		}
//		if(this.match('[')) {
//			int startIndex = this.getPosition();
//			int endIndex = this.skipQuotedString(']');
//			if(endIndex == -1) {
//				this.showErrorMessage("unclosed ']'");
//				return null;
//			}
//			right = new PegCharacter(ruleName, this.source.substring(startIndex, endIndex));
//			return this.parsePostfix(ruleName, right);
//		}
//		if(this.match('"')) {
//			int startIndex = this.getPosition();
//			int endIndex = this.skipQuotedString('"');
//			if(endIndex == -1) {
//				this.showErrorMessage("unclosed \"");
//				return null;
//			}
//			String s = this.source.substring(startIndex, endIndex);
//			right = new PegString(ruleName, UniCharset._UnquoteString(s));
//			return this.parsePostfix(ruleName, right);
//		}
//		if(this.match('\'')) {
//			int startIndex = this.getPosition();
//			int endIndex = this.skipQuotedString('\'');
//			if(endIndex == -1) {
//				this.showErrorMessage("unclosed '");
//				return null;
//			}
//			String s = this.source.substring(startIndex, endIndex);
//			right = new PegString(ruleName, UniCharset._UnquoteString(s));
//			return this.parsePostfix(ruleName, right);
//		}
//		if(this.match('{')) {
//			boolean leftJoin = false;
//			if(this.match('$', ' ') || this.match('$', '\n')) {
//				leftJoin = true;
//			}
//			int startIndex = this.getPosition();
//			int endIndex = this.skipGroup('{', '}');
//			if(endIndex == -1) {
//				this.rollback(startIndex);
//				this.showErrorMessage("unclosed '}'");
//				return null;
//			}
//			PegParserParser sub = this.subParser(startIndex, endIndex);
//			right = sub.parsePegExpr(ruleName);
//			right = new PegNewObject(ruleName, leftJoin, right);
//			right = this.parsePostfix(ruleName, right);
//			return right;
//		}
//		if(this.match("<<")) {
//			boolean leftJoin = false;
//			if(this.match('@', ' ') || this.match('@', '\n')) {
//				leftJoin = true;
//			}
//			int startIndex = this.getPosition();
//			int endIndex = this.skipGroup('<', '>');
//			if(endIndex == -1 || !this.match('>')) {
//				this.rollback(startIndex);
//				this.showErrorMessage("unclosed '>>'");
//				return null;
//			}
//			PegParserParser sub = this.subParser(startIndex, endIndex);
//			right = sub.parsePegExpr(ruleName);
//			right = new PegNewObject(ruleName, leftJoin, right);
//			right = this.parsePostfix(ruleName, right);
//			return right;
//		}
//		this.showErrorMessage("unexpected character '" + this.getChar() + "'");
//		return right;
//	}
//
//	private final Peg parseSequenceExpr(String ruleName) {
//		Peg left = this.parseSingleExpr(ruleName);
//		if(left == null) {
//			return left;
//		}
//		this.skipComment(UniCharset.WhiteSpaceNewLine);
//		if(this.hasChar()) {
//			this.skipComment(UniCharset.WhiteSpaceNewLine);
//			char ch = this.getChar();
//			if(ch == '/') {
//				this.consume(1);
//				this.skipComment(UniCharset.WhiteSpaceNewLine);
//				return left;
//			}
//			Peg right = this.parseSequenceExpr(ruleName);
//			if(right != null) {
//				boolean lrExistense = false;
//				if(left.hasLeftRecursion()) {
//					lrExistense = true;
//				}
//				left = left.appendAsSequence(right);
//				left.setLeftRecursion(lrExistense);
//			}
//		}
//		return left;
//	}
//
//	public final Peg parsePegExpr(String ruleName) {
//		Peg left = this.parseSequenceExpr(ruleName);
//		this.skipComment(UniCharset.WhiteSpaceNewLine);
//		if(this.match(';')) {
//			return left;
//		}
//		if(this.hasChar()) {
//			if(this.match("catch ") || this.match("catch\n")) {
//				this.skipComment(UniCharset.WhiteSpaceNewLine);
//				Peg right = this.parsePegExpr(ruleName);
//				right = new PegCatch(ruleName, right);
//				left = left.appendAsChoice(right);
//			}
//			else {
//				Peg right = this.parsePegExpr(ruleName);
//				if(right != null) {
//					boolean lrExistense = false;
//					if(left.hasLeftRecursion()) {
//						lrExistense = true;
//					}
//					left = left.appendAsChoice(right);
//					left.setLeftRecursion(lrExistense);
//				}
//			}
//		}
//		return left;
//	}
//
//	private final boolean match(char ch, char ch2) {
//		if(this.getChar(0) == ch && this.getChar(1) == ch2) {
//			this.consume(2);
//			return true;
//		}
//		return false;
//	}
//	
//
//}
