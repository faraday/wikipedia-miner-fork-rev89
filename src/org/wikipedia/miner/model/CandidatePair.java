package org.wikipedia.miner.model;

import org.wikipedia.miner.model.Anchor.Sense;

public class CandidatePair implements Comparable<CandidatePair> {
	
	public Sense senseA ;
	public Sense senseB ;
	public double relatedness ;
	public double obviousness ;
	
	/**
	 * initializes a new pair of candidate senses when disambiguating two anchors against each other
	 * 
	 * @param senseA the candidate sense of the first anchor
	 * @param senseB the candidate sense of the seccond anchor
	 * @param relatedness the amount that these senses relate to each other
	 * @param obviousness the average prior probability of the two senses
	 */
	public CandidatePair(Sense senseA, Sense senseB, double relatedness, double obviousness) {
		this.senseA = senseA ;
		this.senseB = senseB ;
		this.relatedness = relatedness ;
		this.obviousness = obviousness ;			
	}
	
	public int compareTo(CandidatePair cp) {
		return new Double(cp.obviousness).compareTo(obviousness) ;
	}
	
	public String toString() {
		return senseA + "," + senseB + ",r=" + relatedness + ",o=" + obviousness ;
	}
}
