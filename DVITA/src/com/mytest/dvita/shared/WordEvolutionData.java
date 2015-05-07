package com.mytest.dvita.shared;
import java.io.Serializable;
public class WordEvolutionData  implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -7897650599712054415L;
	
	public Integer [] wordIDs;
	public Double [][]relevanceAtTime;
	
	
	public Integer [] intervalIDs;
	public String [] intervalStartDate;
	
}
