package org.libbun;

public final class SourceBuilder extends UStringBuilder {
	SourceBuilder parent;

	public SourceBuilder(SourceBuilder Parent) {
		super();
		this.parent = Parent;
	}

	public final SourceBuilder pop() {
		this.AppendLineFeed();
		return this.parent;
	}

	public final void appendCode(String Source) {
		this.lastChar = '\0';
		int StartIndex = 0;
		int i = 0;
		while(i < Source.length()) {
			char ch = Main._GetChar(Source, i);
			if(ch == '\n') {
				if(StartIndex < i) {
					this.slist.add(Source.substring(StartIndex, i));
				}
				this.appendNewLine();
				StartIndex = i + 1;
			}
			if(ch == '\t') {
				if(StartIndex < i) {
					this.slist.add(Source.substring(StartIndex, i));
				}
				this.append(this.Tabular);
				StartIndex = i + 1;
			}
			i = i + 1;
		}
		if(StartIndex < i) {
			this.slist.add(Source.substring(StartIndex, i));
		}
	}

	public final int GetPosition() {
		return this.slist.size();
	}

	public final String CopyString(int BeginIndex, int EndIndex) {
		return Main._SourceBuilderToString(this, BeginIndex, EndIndex);
	}

}
