package org.wikipedia.miner.util.text;

public class UniversalStemmer extends TextProcessor {
	TextProcessor stemProcessor;

	public UniversalStemmer(String language) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		if(language == "turkish"){
			this.stemProcessor = new ZemberekStemmer();
		}
		else {
			this.stemProcessor = new SnowballStemmer(language);
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
