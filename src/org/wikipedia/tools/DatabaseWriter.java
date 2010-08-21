package org.wikipedia.tools;

import java.io.File;

import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.util.text.CaseFolder;

public class DatabaseWriter {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		//connect to database
		Wikipedia wikipedia = new Wikipedia("localhost", "wikiminer", "root", "123456") ;
						
		//load cvs files
		File dataDirectory = new File(args[0]) ;
		wikipedia.getDatabase().loadData(dataDirectory, true) ;
						
		//prepare text processors
		wikipedia.getDatabase().prepareForTextProcessor(new CaseFolder()) ;
						
		//cache definitions (only worth doing if you will be using them a lot - will take a day or so)
		// wikipedia.getDatabase().summarizeDefinitions() ;

	}

}
