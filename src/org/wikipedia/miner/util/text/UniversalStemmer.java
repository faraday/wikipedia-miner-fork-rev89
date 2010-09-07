package org.wikipedia.miner.util.text;

public class UniversalStemmer extends TextProcessor {
	TextProcessor stemProcessor;
	
	public UniversalStemmer(){
		try {
			this.stemProcessor = new SnowballStemmer("english");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public UniversalStemmer(String language) {
		if(language == "turkish"){
			this.stemProcessor = new ZemberekStemmer();
		}
		else {
			try {
				this.stemProcessor = new SnowballStemmer(language);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
    public String getName(){
        return this.stemProcessor.getName();
    }
	
	@Override
	public String toString(){
		return this.stemProcessor.toString();
	}
	
	@Override
	public String processText(String text) {
		return this.stemProcessor.processText(text);
	}

}
