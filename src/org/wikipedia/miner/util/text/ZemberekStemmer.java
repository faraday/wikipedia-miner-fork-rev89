package org.wikipedia.miner.util.text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.zemberek.erisim.Zemberek;
import net.zemberek.islemler.KokBulucu;
import net.zemberek.tr.yapi.TurkiyeTurkcesi;

public class ZemberekStemmer extends TextProcessor {
    private int repeat;
	private Cleaner cleaner ;
	private KokBulucu stemmer;
    private String language;
    
    private String current;
    
    private String delim = "[^`~!@#$%^&*()_=+|\\[\\];{},?<>:'\\\\\"]+";
    private Pattern reDelim;
	
    public ZemberekStemmer(){
    	final Zemberek z = new Zemberek(new TurkiyeTurkcesi());
    	
    	this.cleaner = new Cleaner();
        this.stemmer = z.kokBulucu();
        this.language = "turkish";
        this.repeat = 1;
        
        this.reDelim = Pattern.compile(delim);
    }
	
    /**
	 * Returns the name of the current SnowballStemmer (includes language information).
	 *
	 * @return	the name of this TextProcessor.
	 */
    @Override
    public String getName(){
        return this.getClass().getSimpleName() + "_" + this.language;
    }
    
    /**
	 * Returns a string that provides complete information to setup the current configuration of the
     * text processor.
	 *
	 * @return	a textual description of the TextProcessor.
	 */
    @Override
    public String toString(){
        return super.toString() + "<string<" + TextProcessor.encodeParam(this.language);
    }
    
    private String stem(String word)
    {
    	String[] stems = stemmer.stringKokBul(word);
        if (stems != null && stems.length > 0)
        {
            return stems[0];
        }
        else
        {
            return null;
        }
    }
    
    /**
	 * Returns the processed version of the argument string.
	 * 
	 * @param	text	the string to be processed
	 * @return the processed string
	 */	
	@Override
	public String processText(String text) {
		StringBuffer processedText = new StringBuffer() ;
		String[] terms = text.toLowerCase().split("\\s+") ;
		String tmp;
		
		for(int i=0;i<terms.length; i++)
        {
            if(terms[i].length() > 0)
            {
            	Matcher m = reDelim.matcher(terms[i]);
            	if(m.find()){
            		current = m.group();
            	}
            	else {
            		current = terms[i];
            	}
            	
                for (int j = this.repeat; j != 0; j--) {
                    tmp = this.stem(current);
                    if(tmp == null)
                    	break;
                    else
                    	current = tmp;
                }
				processedText.append(cleaner.processText(current));
				processedText.append(" ");
            }
		}

		return processedText.toString().trim() ;
	}

}
