package org.libbun;

public class Namespace extends SymbolTable {
	public UniMap<PegRuleSet> ruleMap;
	public UniArray<String>   exportSymbolList;
	public BunDriver  driver;

	public Namespace(BunDriver driver) {
		super(null);
		this.root = this;
		this.ruleMap = new UniMap<PegRuleSet>();
		PegRuleSet pegRule = new PegRuleSet();
		pegRule.loadPegRule();
		this.ruleMap.put("peg", pegRule);
//		this.ruleMap.put("main", ruleSet);
		this.driver = driver;
	}
	
	public final String toString() {
		return "root";
	}

	public void importFrom(Namespace ns) {
		for(int i = 0; i < ns.exportSymbolList.size(); i++) {
			String symbol = ns.exportSymbolList.ArrayValues[i];
			this.setSymbol(symbol, ns.getSymbol(symbol));
		}
	}
	

	public final PegRuleSet loadPegFile(String ruleNs, String fileName) {
		PegRuleSet rules = this.ruleMap.get(fileName);
		if(rules == null) {
			rules = new PegRuleSet();
			rules.loadPegFile(fileName);
			this.ruleMap.get(fileName);
		}
		if(ruleNs != null) {
			this.ruleMap.put(ruleNs, rules);
		}
		return rules;
	}
	
	public final PegRuleSet getRuleSet(String ruleNs) {
		PegRuleSet p = this.ruleMap.get(ruleNs);
		if(p == null) {
			p = new PegRuleSet();
			p.loadPegFile("lib/peg/" + ruleNs + ".peg");
			this.ruleMap.put(ruleNs, p);
		}
		return p;
	}

	public void initParserRuleSet(ParserContext context, String lang) {
		if(lang == null) {
			lang = this.guessLang(context.source.fileName, "bun");
		}
		PegRuleSet ruleSet = this.getRuleSet(lang);
		context.setRuleSet(ruleSet);
	}

	private String guessLang(String fileName, String defaultValue) {
		if(fileName != null) {
			int loc = fileName.lastIndexOf(".");
			if(loc > 0) {
				return fileName.substring(loc+1);
			}
		}
		return defaultValue;
	}




}
