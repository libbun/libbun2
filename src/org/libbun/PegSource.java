package org.libbun;

public class PegSource {
	public String fileName;
	int    lineNumber;
	public String  sourceText;

	public PegSource(String fileName, int lineNumber, String sourceText) {
		this.fileName = fileName;
		this.lineNumber = lineNumber;
		this.sourceText = sourceText;
	}

	public final char charAt(int n) {
		if(0 <= n && n < this.sourceText.length()) {
			return Main._GetChar(this.sourceText, n);
		}
		return '\0';
	}

	public final String substring(int startIndex, int endIndex) {
		return this.sourceText.substring(startIndex, endIndex);
	}

	public final int getLineNumber(int Position) {
		int LineNumber = this.lineNumber;
		int i = 0;
		while(i < Position) {
			char ch = Main._GetChar(this.sourceText, i);
			if(ch == '\n') {
				LineNumber = LineNumber + 1;
			}
			i = i + 1;
		}
		return LineNumber;
	}


	public final int getIndentSize(int fromPosition) {
		int startPosition = this.getLineStartPosition(fromPosition);
		int length = 0;
		String s = this.sourceText;
		for(;startPosition < s.length();startPosition=startPosition+1) {
			char ch = Main._GetChar(s, startPosition);
			if(ch == '\t') {
				length = length + 8;
			}
			else if(ch == ' ') {
				length = length + 1;
			}
			else {
				break;
			}
		}
		return length;

	}
	
	public final String getIndentText(int fromPosition) {
		int startPosition = this.getLineStartPosition(fromPosition);
		int i = startPosition;
		for(; i < fromPosition; i++) {
			char ch = this.charAt(i);
			if(ch != ' ' && ch != '\t') {
				break;
			}
		}
		return this.substring(startPosition, i);
	}


	public final int getLineStartPosition(int fromPostion) {
		String s = this.sourceText;
		int startIndex = fromPostion;
		if(!(startIndex < s.length())) {
			startIndex = s.length() - 1;
		}
		if(startIndex < 0) {
			startIndex = 0;
		}
		while(startIndex > 0) {
			char ch = Main._GetChar(s, startIndex);
			if(ch == '\n') {
				startIndex = startIndex + 1;
				break;
			}
			startIndex = startIndex - 1;
		}
		return startIndex;
	}

	public final String getLineTextAt(int pos) {
		String s = this.sourceText;
		int startIndex = this.getLineStartPosition(pos);
		int endIndex = startIndex;
		while(endIndex < s.length()) {
			char ch = Main._GetChar(s, endIndex);
			if(ch == '\n') {
				break;
			}
			endIndex = endIndex + 1;
		}
		return s.substring(startIndex, endIndex);
	}

	public final String getMakerLine(int pos) {
		String s = this.sourceText;
		int startIndex = this.getLineStartPosition(pos);
		String markerLine = "";
		int i = startIndex;
		while(i < pos) {
			char ch = Main._GetChar(s, i);
			if(ch == '\n') {
				break;
			}
			if(ch == '\t') {
				markerLine = markerLine + "\t";
			}
			else {
				markerLine = markerLine + " ";
			}
			i = i + 1;
		}
		return markerLine + "^";
	}

	public final String formatErrorHeader(String error, int pos, String message) {
		return "(" + this.fileName + ":" + this.getLineNumber(pos) + ") [" + error +"] " + message;
	}

	public final String formatErrorMessage(String errorType, int pos, String msg) {
		String Line = this.getLineTextAt(pos);
		String Delim = "\n\t";
		if(Line.startsWith("\t") || Line.startsWith(" ")) {
			Delim = "\n";
		}
		String Header = this.formatErrorHeader(errorType, pos, msg);
		String Marker = this.getMakerLine(pos);
		msg = Header + Delim + Line + Delim + Marker;
		return msg;
	}

	public String checkFileName(String fileName) {
		int loc = this.fileName.lastIndexOf("/");
		if(loc > 0) {
			return this.fileName.substring(0, loc+1) + fileName; 
		}
		return fileName;
	}
}
