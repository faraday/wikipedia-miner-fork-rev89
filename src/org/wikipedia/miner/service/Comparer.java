/*
 *    Comparer.java
 *    Copyright (C) 2007 David Milne, d.n.milne@gmail.com
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.wikipedia.miner.service;

import java.sql.SQLException;
import java.util.*;

import org.w3c.dom.*;

import org.wikipedia.miner.model.*;
import org.wikipedia.miner.model.Anchor.RelatednessResult;
import org.wikipedia.miner.util.*;
import org.wikipedia.miner.util.text.*;


/**
 * This service measures the semantic relatedness between terms.
 * 
 *  @author David Milne
 */
public class Comparer {

	private WikipediaMinerServlet wms ;
	
	private boolean defaultShowDetails = false ;
	private int defaultMaxLinkCount = 250 ;
	
	private String language;
	
	private int[] linksIn1, linksIn2;
	private int[][] linksOut1, linksOut2;
	
	private HashMap<String, int[]> cacheLinksIn;
	private HashMap<String, int[][]> cacheLinksOut;

	/**
	 * Initializes a new Comparer
	 * @param wms the servlet that hosts this service
	 */
	public Comparer(WikipediaMinerServlet wms, String language) {
		this.wms = wms;
		this.language = language;
		
		cacheLinksIn = new HashMap<String, int[]>(10000);
		cacheLinksOut = new HashMap<String, int[][]>(10000);
	}
	
	/**
	 * @return false: the default behavior is to not show details of how terms are compared. 
	 */
	public boolean getDefaultShowDetails() {
		return defaultShowDetails ;
	}
	
	/**
	 * @return the default maximum number of links that are shown when providing details of how terms are compared.
	 */
	public int getDefaultMaxLinkCount() {
		return defaultMaxLinkCount ;
	}
	
	/**
	 * @return an Element description of this service; what it does, and what parameters it takes.
	 */
	public Element getDescription() {
		
		Element description = wms.doc.createElement("Description") ;
		description.setAttribute("task", "compare") ;
		
		description.appendChild(wms.createElement("Details", "<p>This service measures the semantic relatedness between two terms or a set of page ids. From this you can tell, for example, that New Zealand has more to do with <a href=\"" + wms.context.getInitParameter("service_name") + "?task=compare&details=true&term1=New Zealand&term2=Rugby\">Rugby</a> than <a href=\"" + wms.context.getInitParameter("service_name") + "?task=compare&details=true&term1=New Zealand&term2=Soccer\">Soccer</a>, or that Geeks are more into <a href=\"" + wms.context.getInitParameter("service_name") + "?task=compare&details=true&term1=Geek&term2=Computer Games\">Computer Games</a> than the <a href=\"" + wms.context.getInitParameter("service_name") + "?task=compare&details=true&term1=Geek&term2=Olympic Games\">Olympic Games</a> </p>"
				+ "<p>The relatedness measures are calculated from the links going into and out of each page. Links that are common to both pages are used as evidence that they are related, while links that are unique to one or the other indicate the opposite. The relatedness measure is symmetric, so comparing <i>a</i> to <i>b</i> is the same as comparing <i>b</i> to <i>a</i>. </p>" )) ;
		
		Element group1 = wms.doc.createElement("ParameterGroup") ;
		description.appendChild(group1) ;
		
		Element paramTerm1 = wms.doc.createElement("Parameter") ;
		paramTerm1.setAttribute("name", "term1") ;
		paramTerm1.appendChild(wms.doc.createTextNode( "The first of two terms (or phrases) to compare.")) ;
		group1.appendChild(paramTerm1) ;
		
		Element paramTerm2 = wms.doc.createElement("Parameter") ;
		paramTerm2.setAttribute("name", "term2") ;
		paramTerm2.appendChild(wms.doc.createTextNode( "The second of two terms (or phrases) to compare.")) ;
		group1.appendChild(paramTerm2) ;
		
		Element group2 = wms.doc.createElement("ParameterGroup") ;
		description.appendChild(group2) ;
		
		Element paramIds = wms.doc.createElement("Parameter") ;
		paramIds.setAttribute("name", "ids") ;
		paramIds.appendChild(wms.doc.createTextNode("A set of page ids to compare, delimited by commas. For efficiency, the results will be returned in comma delimited form rather than xml, with one line for each comparison.")) ;
		group2.appendChild(paramIds) ;
		
		Element paramShowDetails = wms.doc.createElement("Parameter") ;
		paramShowDetails.setAttribute("name", "showDetails") ;
		paramShowDetails.setAttribute("optional", "true") ;
		paramShowDetails.appendChild(wms.doc.createTextNode("Specifies whether the details of a relatedness comparison (all of the senses and links that were considered) will be shown. This is much more expensive than merely showing the result of the comparison, so please only obtain the details if you will use them.")) ;
		paramShowDetails.setAttribute("default", String.valueOf(getDefaultShowDetails())) ; 
		description.appendChild(paramShowDetails) ;
		
		Element paramLinkCount = wms.doc.createElement("Parameter") ;
		paramLinkCount.setAttribute("name", "maxLinkCount") ;
		paramLinkCount.setAttribute("optional", "true") ;
		paramLinkCount.appendChild(wms.doc.createTextNode("The maximum number of page links to return when presenting the details of a relatedness comparison.")) ;
		paramLinkCount.setAttribute("default", String.valueOf(getDefaultMaxLinkCount())) ; 
		description.appendChild(paramLinkCount) ;
		
		return description ;
	}
	
	
	private double getRelatednessFromInLinks(ArrayList<Integer> group1, ArrayList<Integer> group2, int[] linksA, int[] linksB) throws SQLException{
		
		int linksBoth = 0 ;

		int indexA = 0 ;
		int indexB = 0 ;

		while (indexA < linksA.length && indexB < linksB.length) {

			long idA = linksA[indexA] ;
			long idB = linksB[indexB] ;

			if (idA == idB) {
				linksBoth ++ ;
				indexA ++ ;
				indexB ++ ;
			} else {
				
				if ((idA < idB && idA > 0)|| idB < 0) {
					
					if (group2.contains(idA)) 
						linksBoth ++ ;
					
					indexA ++ ;
				} else {
					
					if (group1.contains(idB)) 
						linksBoth ++ ;
					
					indexB ++ ;
				}
			}
		}

		double a = Math.log(linksA.length) ;
		double b = Math.log(linksB.length) ;
		double ab = Math.log(linksBoth) ;
		double m = Math.log(wms.wikipedia.getDatabase().getArticleCount()) ;

		double sr = (Math.max(a, b) -ab) / (m - Math.min(a, b)) ;

		if (Double.isNaN(sr) || Double.isInfinite(sr) || sr > 1)
			sr = 1 ;

		sr = 1-sr ;

		return sr ;
	}
	
	
	private double getRelatednessFromOutLinks(ArrayList<Integer> group1, ArrayList<Integer> group2, int[][] dataA, int[][] dataB) throws SQLException{

		int totalArticles = wms.wikipedia.getDatabase().getArticleCount() ;

		if (dataA.length == 0 || dataB.length == 0)
			return 0 ;

		int indexA = 0 ;
		int indexB = 0 ;

		Vector<Double> vectA = new Vector<Double>() ;
		Vector<Double> vectB = new Vector<Double>() ;

		while (indexA < dataA.length || indexB < dataB.length) {

			int idA = -1 ;
			int idB = -1 ;

			if (indexA < dataA.length)
				idA = dataA[indexA][0] ;

			if (indexB < dataB.length)
				idB = dataB[indexB][0] ;

			if (idA == idB) {
				double probability = Math.log((double)totalArticles/dataA[indexA][1]) ;
				vectA.add(probability) ;
				vectB.add(probability) ;

				indexA ++ ;
				indexB ++ ;
			} else {

				if ((idA < idB && idA > 0)|| idB < 0) {
					
					double probability = Math.log((double)totalArticles/dataA[indexA][1]) ;
					vectA.add(probability) ;
					if (group2.contains(idA))
						vectB.add(probability) ;
					else
						vectB.add(0.0) ;
				
					indexA ++ ;
				} else {
					
					double probability = Math.log((double)totalArticles/dataB[indexB][1]) ;
					vectB.add(new Double(probability)) ;
					if (group1.contains(idB))
						vectA.add(probability) ;
					else
						vectA.add(0.0) ;

					indexB ++ ;
				}
			}
		}

		// calculate angle between vectors
		double dotProduct = 0 ;
		double magnitudeA = 0 ;
		double magnitudeB = 0 ;

		for (int x=0;x<vectA.size();x++) {
			double valA = ((Double)vectA.elementAt(x)).doubleValue() ;
			double valB = ((Double)vectB.elementAt(x)).doubleValue() ;

			dotProduct = dotProduct + (valA * valB) ;
			magnitudeA = magnitudeA + (valA * valA) ;
			magnitudeB = magnitudeB + (valB * valB) ;
		}

		magnitudeA = Math.sqrt(magnitudeA) ;
		magnitudeB = Math.sqrt(magnitudeB) ;

		double sr = Math.acos(dotProduct / (magnitudeA * magnitudeB)) ;		
		sr = (Math.PI/2) - sr ; // reverse, so 0=no relation, PI/2= same
		sr = sr / (Math.PI/2) ; // normalize, so measure is between 0 and 1 ;				

		return sr ;
	}
	
	
	public double getRelatednessTo(ArrayList<Integer> group1, ArrayList<Integer> group2) throws SQLException{
		
		WikipediaDatabase database = wms.wikipedia.getDatabase();
		
		if (database.areOutLinksCached() && database.areInLinksCached()) {
			readLinks(group1,group2);
			return (getRelatednessFromInLinks(group1,group2,linksIn1,linksIn2) + getRelatednessFromOutLinks(group1,group2,linksOut1,linksOut2))/2 ;
		}
			
		if (database.areOutLinksCached()) {
			readLinks(group1,group2);
			return  getRelatednessFromOutLinks(group1,group2,linksOut1,linksOut2) ;
		}
		
		if (database.areInLinksCached()) {
			readLinks(group1,group2);
			return getRelatednessFromInLinks(group1,group2,linksIn1,linksIn2) ;
		}
		
		return (getRelatednessFromInLinks(group1,group2,linksIn1,linksIn2) + getRelatednessFromOutLinks(group1,group2,linksOut1,linksOut2))/2 ;
	}
	
	
	private void readLinks(ArrayList<Integer> group1, ArrayList<Integer> group2) throws SQLException{
		int u = 0;
		String key1 = "";
		for(int i : group1){
			key1 += i + "-";
		}
		
		String key2 = "";
		for(int i : group2){
			key2 += i + "-";
		}
		
		if(cacheLinksIn.containsKey(key1) && cacheLinksOut.containsKey(key1)){
			this.linksIn1 = cacheLinksIn.get(key1);
			this.linksOut1 = cacheLinksOut.get(key1);
		}
		else {
		
			TreeSet<Integer> tlinksIn1 = new TreeSet<Integer>() ;
			ArrayList<int[]> tlinksOut1 = new ArrayList<int[]>() ;
			for(int i : group1){
				Article art1 = new Article(wms.wikipedia.getDatabase(),i);
				for (Integer link:art1.getLinksInIds()) 
					tlinksIn1.add(link) ;
				for (int[] outLinkIdsAndCounts:art1.getLinksOutIdsAndCounts()) 
					tlinksOut1.add(outLinkIdsAndCounts) ;
			}
			
			this.linksIn1 = new int[tlinksIn1.size()];
			this.linksOut1 = new int[tlinksOut1.size()][2];
			
			u=0;
			for(int k : tlinksIn1){
				this.linksIn1[u++] = k;
			}
			u=0;
			for(int [] k : tlinksOut1){
				this.linksOut1[u++] = k;
			}
			
			cacheLinksIn.put(key1, this.linksIn1);
			cacheLinksOut.put(key1, this.linksOut1);
		
		}
		
		
		if(cacheLinksIn.containsKey(key2) && cacheLinksOut.containsKey(key2)){
			this.linksIn2 = cacheLinksIn.get(key2);
			this.linksOut2 = cacheLinksOut.get(key2);
		}
		else {
			TreeSet<Integer> tlinksIn2 = new TreeSet<Integer>() ;
			ArrayList<int[]> tlinksOut2 = new ArrayList<int[]>() ;
			for(int i : group2){
				Article art2 = new Article(wms.wikipedia.getDatabase(),i);
				for (Integer link:art2.getLinksInIds()) 
					tlinksIn2.add(link) ;
				for (int[] outLinkIdsAndCounts:art2.getLinksOutIdsAndCounts()) 
					tlinksOut2.add(outLinkIdsAndCounts) ;
			}
			
			this.linksIn2 = new int[tlinksIn2.size()];
			this.linksOut2 = new int[tlinksOut2.size()][2];
			
			
			u=0;
			for(int k : tlinksIn2){
				this.linksIn2[u++] = k;
			}
			
			u=0;
			for(int [] k : tlinksOut2){
				this.linksOut2[u++] = k;
			}
			
			cacheLinksIn.put(key2, this.linksIn2);
			cacheLinksOut.put(key2, this.linksOut2);
		}
		
	}
	

	//TODO: add a method for comparing a set of article ids.
	
	/**
	 * Measures the relatedness between two sets of article ids 
	 * 
	 * @param group1 the first group of ids to compare
	 * @param group2 the second group of ids to compare
	 * @return an Element message of how the two terms relate to each other
	 * @throws Exception
	 */
	public Element getGroupRelatedness(ArrayList<Integer> group1, ArrayList<Integer> group2) throws Exception {

		Element response = wms.doc.createElement("RelatednessResponse") ;
		
		if (group1 == null || group2 == null) {
			response.setAttribute("unspecifiedParameters", "true") ;
			return response ;
		}
		
		Collections.sort(group1);
		Collections.sort(group2);
						
		double sr = getRelatednessTo(group1, group2);

		response.setAttribute("relatedness", wms.df.format(sr)) ;

		return response ;

	}
	
	
	/**
	 * Measures the relatedness between a term and a set of article ids 
	 * 
	 * @param term the term to compare
	 * @param group the group of ids to compare
	 * @return an Element message of how the two terms relate to each other
	 * @throws Exception
	 */
	public Element getLabelRelatedness(String term, ArrayList<Integer> group) throws Exception {

		Element response = wms.doc.createElement("RelatednessResponse") ;
		
		if (term == null || group == null) {
			response.setAttribute("unspecifiedParameters", "true") ;
			return response ;
		}
		
		// TextProcessor tp = new CaseFolder() ;
		TextProcessor tp = new UniversalStemmer(language) ;
		
		Anchor anchor1 = new Anchor(term, tp, wms.wikipedia.getDatabase()) ;
		SortedVector<Anchor.Sense> senses1 = anchor1.getSenses() ; 

		if (senses1.size() == 0) {
			response.setAttribute("unknownTerm", term) ; 
			return response ;
		}
		
		double maxSr = 0, sr;
		for(Anchor.Sense sense : senses1){
			ArrayList<Integer> termGroup = new ArrayList<Integer>(1);
			termGroup.add(sense.getId());
			
			sr = getRelatednessTo(termGroup, group);
			
			if(sr > maxSr)
				maxSr = sr;
		}
						
		response.setAttribute("relatedness", wms.df.format(maxSr)) ;

		return response ;

	}
	
	
	
	/**
	 * Measures the relatedness between two terms, and 
	 * 
	 * @param term1 the first term to compare
	 * @param term2 the second term to compare
	 * @param details true if the details of a relatedness comparison (all of the senses and links that were considered) are needed, otherwise false.
	 * @param linkLimit the maximum number of page links to return when presenting the details of a relatedness comparison.
	 * @return an Element message of how the two terms relate to each other
	 * @throws Exception
	 */
	public Element getRelatedness(String term1, String term2, boolean details, int linkLimit) throws Exception {

		Element response = wms.doc.createElement("RelatednessResponse") ;
		
		if (term1 == null || term2 == null) {
			response.setAttribute("unspecifiedParameters", "true") ;
			return response ;
		}
				
		// TextProcessor tp = new CaseFolder() ;
		TextProcessor tp = new UniversalStemmer(language) ;			

		Anchor anchor1 = new Anchor(term1, tp, wms.wikipedia.getDatabase()) ;
		SortedVector<Anchor.Sense> senses1 = anchor1.getSenses() ; 

		if (senses1.size() == 0) {
			response.setAttribute("unknownTerm", term1) ; 
			return response ;
		}

		Anchor anchor2 = new Anchor(term2, tp, wms.wikipedia.getDatabase()) ;
		SortedVector<Anchor.Sense> senses2 = anchor2.getSenses() ; 

		if (senses2.size() == 0) {
			response.setAttribute("unknownTerm", term2) ; 
			return response ;
		}
		
		response.setAttribute("term1", term1) ;
		response.setAttribute("term2", term2) ;

		RelatednessResult relResult = anchor1.getRelatednessTo(anchor2) ;

		response.setAttribute("relatedness", wms.df.format(relResult.sr)) ;

		if (!details)
			return response ;

		CandidatePair bestSenses = relResult.cp;
		
		Article art1 = bestSenses.senseA ;
		
		Element xmlSense1 = wms.doc.createElement("Sense1");
		
		
		xmlSense1.setAttribute("title", art1.getTitle()) ;
		xmlSense1.setAttribute("id", String.valueOf(art1.getId())) ;		
		xmlSense1.setAttribute("candidates", String.valueOf(senses1.size())) ;
		
		String firstSentence = null;
		try { 
			firstSentence = art1.getFirstSentence(null, null) ;
			firstSentence = wms.definer.formatDefinition(firstSentence, Definer.FORMAT_HTML, Definer.LINK_TOOLKIT) ;
		} catch (Exception e) {} ;
		
		if (firstSentence != null) 
			xmlSense1.appendChild(wms.createElement("FirstSentence", firstSentence)) ;

		response.appendChild(xmlSense1) ;
		
		
		Article art2 = bestSenses.senseB ;
		
		Element xmlSense2 = wms.doc.createElement("Sense2");
		xmlSense2.setAttribute("title", art2.getTitle()) ;
		xmlSense2.setAttribute("id", String.valueOf(art2.getId())) ;
		xmlSense2.setAttribute("candidates", String.valueOf(senses2.size())) ;
		
		firstSentence = null;
		try { 
			firstSentence = art2.getFirstSentence(null, null) ;
			firstSentence = wms.definer.formatDefinition(firstSentence, Definer.FORMAT_HTML, Definer.LINK_TOOLKIT) ;
		} catch (Exception e) {} ;
		
		if (firstSentence != null) 
			xmlSense2.appendChild(wms.createElement("FirstSentence", firstSentence)) ;
		
		response.appendChild(xmlSense2) ;
		
		
		//details of links coming in to these articles
		
		TreeSet<Integer> linksIn1 = new TreeSet<Integer>() ;
		for (Integer link:art1.getLinksInIds()) 
			linksIn1.add(link) ;
		
		TreeSet<Integer> linksIn2 = new TreeSet<Integer>() ;
		for (Integer link:art2.getLinksInIds()) 
			linksIn2.add(link) ;
		
		TreeSet<Integer> linksInShared = new TreeSet<Integer>() ;
		for (Integer id: linksIn1) {
			if (linksIn2.contains(id))
				linksInShared.add(id) ;
		}
		
		for (Integer id: linksInShared) {
			linksIn1.remove(id) ;
			linksIn2.remove(id) ;
		}
		
		Element xmlLinksIn = wms.doc.createElement("LinksIn") ; 
		xmlLinksIn.appendChild(getLinkListElement(linksInShared, "SharedLink", linkLimit)) ;
		xmlLinksIn.appendChild(getLinkListElement(linksIn1, "Link1", linkLimit)) ;
		xmlLinksIn.appendChild(getLinkListElement(linksIn2, "Link2", linkLimit)) ;
		response.appendChild(xmlLinksIn) ;
		
		//details of links going out from these articles
		
		TreeSet<Integer> linksOut1 = new TreeSet<Integer>() ;
		for (Integer link:art1.getLinksOutIds()) 
			linksOut1.add(link) ;
		
		TreeSet<Integer> linksOut2 = new TreeSet<Integer>() ;
		for (Integer link:art2.getLinksOutIds()) 
			linksOut2.add(link) ;
		
		TreeSet<Integer> linksOutShared = new TreeSet<Integer>() ;
		for (Integer id: linksOut1) {
			if (linksOut2.contains(id))
				linksOutShared.add(id) ;
		}
		
		for (Integer id: linksOutShared) {
			linksOut1.remove(id) ;
			linksOut2.remove(id) ;
		}
		
		Element xmlLinksOut = wms.doc.createElement("LinksOut") ; 
		xmlLinksOut.appendChild(getLinkListElement(linksOutShared, "SharedLink", linkLimit)) ;
		xmlLinksOut.appendChild(getLinkListElement(linksOut1, "Link1", linkLimit)) ;
		xmlLinksOut.appendChild(getLinkListElement(linksOut2, "Link2", linkLimit)) ;
		response.appendChild(xmlLinksOut) ;
		
		
		
		return response ;
	}
	
	private Element getLinkListElement(Collection<Integer> links, String tag, int linkLimit) {
		
		Element xmlLinks = wms.doc.createElement(tag + "List") ;
		xmlLinks.setAttribute("size", String.valueOf(links.size())) ;
		
		int count = 0 ;
		for (Integer link: links) {
			
			if (count++ >= linkLimit) break ;
			
			try {
				Article art = new Article(wms.wikipedia.getDatabase(), link) ;
				
				Element xmlLink = wms.doc.createElement(tag) ;
				xmlLink.setAttribute("id", String.valueOf(art.getId())) ;
				xmlLink.setAttribute("title", art.getTitle()) ;
				
				xmlLinks.appendChild(xmlLink) ;
			} catch (Exception e) {} ;
		}
		return xmlLinks ;
	}
	
}
