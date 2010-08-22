package org.wikipedia.tools;

import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.util.text.CaseFolder;

public class PrepareProcessor {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		if(args.length != 5){
			System.exit(1);
		}
		
		//connect to database
		Wikipedia wikipedia = Wikipedia.getInstanceFromArguments(args) ;
						
		//prepare text processors
		wikipedia.getDatabase().prepareForTextProcessor(new CaseFolder()) ;
						
		//cache definitions (only worth doing if you will be using them a lot - will take a day or so)
		// wikipedia.getDatabase().summarizeDefinitions() ;

	}

}
