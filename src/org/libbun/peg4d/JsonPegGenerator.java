package org.libbun.peg4d;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.libbun.Main;
import org.libbun.peg4d.PegObject;
import org.libbun.UMap;

public class JsonPegGenerator {
	
	int arrayCount = 0;
	UMap<Integer> classNameMap = new UMap<Integer>();
	
	public final StringSource generate(StringSource source, PegObject node, int index) {
		int count = 0;
		int arrayCount = 0;
		for(int i = 0; i < node.AST.length; i++) {
			switch (node.AST[i].tag) {
			case "#class":
				index = this.classNameMap.get(node.AST[i].AST[0].getText());
					source.sourceText += "Object"+ index +" = << BeginObject Value" + index + "@ EndObject #object>>;\n\n";
					source = generate(source,node.AST[i], index);
				break;
			
			case "#main":
				source.sourceText += "Object0 = BeginObject << Member0@  #object >> EndObject;\n\n";
				source.sourceText += "Member0 = << Object0_0@ #member>>;\n\n";
				source.sourceText += "Object0" + "_" + count + " = << QuotationMark Name0@ QuotationMark NameSeparator BeginObject Value0@ EndObject #object >>;\n\n";
				source = generate(source,node.AST[i], 0);
				break;
				
			case "#name":
				source.sourceText += "Name" + index + " = << \"" + node.AST[i].getText() + "\" #string >>;\n\n";
				break;
				
			case "#member":
				source = generate(source, node.AST[i], index);
				source.sourceText += "Value" + index + " =";
				for(int j = 0; j < node.AST[i].AST.length - 1; j++) {
					source.sourceText += " Member" + index + "_" + j + "@ ValueSeparator";
				}
				source.sourceText += " Member" + index + "_" + (node.AST[i].AST.length - 1) + "@;\n\n";
				source.sourceText += "";
				break;
				
			case "#string":
				source.sourceText += "Member" + index + "_" + count + " = << Key" + index + "_" + count + "@ NameSeparator String@ #member >>;\n\n"
									+ "Key" + index + "_" + count + " = << QuotationMark " + node.AST[i].getText() + " QuotationMark #key >>;\n\n";
				count++;
				break;
				
			case "#number":
				source.sourceText += "Member" + index + "_" + count + " = << Key" + index + "_" + count + "@ NameSeparator Number@ #member >>;\n\n"
									+ "Key" + index + "_" + count + " = << QuotationMark " + node.AST[i].getText() + " QuotationMark #key >>;\n\n";
				count++;
				break;
				
			case "#Class":
				source.sourceText += "Member" + index + "_" + count + " = <<Key" + index + "_" + count + "@ NameSeparator Object" + this.classNameMap.get(node.AST[i].AST[0].getText()) + "@ #member >>;\n\n"
									+ "Key" + index + "_" + count + " = << QuotationMark \"" + node.AST[i].AST[0].getText() + "\" QuotationMark #key >>;\n\n";
				count++;
				break;
				
			case "#array":
				if(node.AST[i].AST[0].AST == null && this.arrayCount == 0) {
					source.sourceText += "Member" + index + "_" + count + " = <<Key" + index + "_" + count + "@ NameSeparator Array"+ index + "_" + arrayCount +"@ #member >>;\n\n";
					source.sourceText += "Array" + index + "_" + arrayCount + " = BeginArray << ( " + node.AST[i].AST[0].getText() + "@ ( ValueSeparator " + node.AST[i].AST[0].getText() + "@ )* )? #array >> EndArray;\n\n"
										+ "Key" + index + "_" + count + " = << QuotationMark " + node.AST[i].AST[1].getText() + " QuotationMark #key >>;\n\n";
					arrayCount++;
				}
				else if(node.AST[i].AST.length == 1) {
					arrayCount = this.arrayCount;
					source.sourceText += "Array" + index + "_" + arrayCount + " = BeginArray << ( " + node.AST[0].getText() + "@ ( ValueSeparator " + node.AST[0].getText() + "@ )* )? #array >> EndArray;\n\n";
					this.arrayCount++;
					i++;
				}
				else if(node.AST[i].AST[0].tag.equals("#Class")) {
					source.sourceText += "Member" + index + "_" + count + " = <<Key" + index + "_" + count + "@ NameSeparator Array"+ index + "_" + arrayCount +"@ #member >>;\n\n";
					source.sourceText += "Array" + index + "_" + arrayCount + " = BeginArray << Object" + this.classNameMap.get(node.AST[i].AST[0].AST[0].getText()) + "@ ( ValueSeparator Object" + this.classNameMap.get(node.AST[i].AST[0].AST[0].getText()) +"@)* #array >> EndArray;\n\n"
										+ "Key" + index + "_" + count + " = << QuotationMark " + node.AST[i].AST[1].getText() + " QuotationMark #key >>;\n\n";
					arrayCount++;
				}
				else {
					source.sourceText += "Member" + index + "_" + count + " = <<Key" + index + "_" + count + "@ NameSeparator Array"+ index + "_" + arrayCount +"@ #member >>;\n\n";
					source.sourceText += "Array" + index + "_" + arrayCount + " = BeginArray << ( Array" + index + "_" + (count + 1) + "@ ( ValueSeparator Array" + index + "_" + (count + 1) + "@ )* )? #array >> EndArray;\n\n"
										+ "Key" + index + "_" + count + " = << QuotationMark " + node.AST[i].AST[1].getText() + " QuotationMark #key >>;\n\n";
					arrayCount++;
					this.arrayCount = arrayCount;
					source = generate(source, node.AST[i], index);
					arrayCount = this.arrayCount;
				}
				count++;
				break;
				
			case "#boolean":
				source.sourceText += "Member" + index + "_" + count + " = << Key" + index + "_" + count + "@ NameSeparator (True@ / False@) #member >>;\n\n"
						+ "Key" + index + "_" + count + " = << QuotationMark " + node.AST[i].getText() + " QuotationMark #key >>;\n\n";
				count++;
				break;
				

			default:
				break;
			}
		}
		return source;
	}
	
	public final String generateJsonPegFile(PegObject node) {
		StringSource source = (StringSource) Main.loadSource("sample/rootJson.peg");
		this.checkClassLevel(node);
		this.generate(source, node, 0);
		String LanguageJsonPeg = "sample/generatedJson.peg";
		File newfile = new File(LanguageJsonPeg);
		try{
		    newfile.createNewFile();
		    File file = new File(LanguageJsonPeg);
		    FileWriter fileWriter = new FileWriter(file);
		    fileWriter.write(source.sourceText);
		    fileWriter.close();
		}catch(IOException e){
		    System.out.println(e);
		}
		return LanguageJsonPeg;
	}
	
	public final void checkClassLevel(PegObject node) {
		for(int i = 0; i < node.AST.length; i++) {
			if(node.AST[i].tag.equals("#main")) {
				this.classNameMap.put(node.AST[i].AST[0].getText(), 0);
			}
			else {
				this.classNameMap.put(node.AST[i].AST[0].getText(), i+1);
			}
		}
	}
}
