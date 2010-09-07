package org.wikipedia.tools;

import java.io.File;

import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.util.text.CaseFolder;
import org.wikipedia.miner.util.text.TextProcessor;
import org.wikipedia.miner.util.text.UniversalStemmer;

public class DatabaseWriter {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		if(args.length != 6){
			System.exit(1);
		}
		
		//connect to database
		Wikipedia wikipedia = Wikipedia.getInstanceFromArguments(args) ;
						
		//load cvs files
		File dataDirectory = new File(args[4]) ;
		wikipedia.getDatabase().loadData(dataDirectory, true) ;
		
		TextProcessor tp = new UniversalStemmer(args[5]) ;
						
		//prepare text processors
		wikipedia.getDatabase().prepareForTextProcessor(tp) ;
						
		//cache definitions (only worth doing if you will be using them a lot - will take a day or so)
		// wikipedia.getDatabase().summarizeDefinitions() ;

	}

}
