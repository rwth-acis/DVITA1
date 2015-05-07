package com.mytest.dvita.shared;

import java.io.Serializable;
import java.sql.Timestamp;

// informationen fürs topic mining
public class ConfigTopicminingShared implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3014588681709576626L;


	public int id;
	
	public static enum Granularity {
	    /* 1 */ YEARLY,  
	    /* 2 */ MONTHLY, 
	    /* 3 */ WEEKLY, 
	    /* 4 */ DAYLY, 
	    /* 5 */ QUARTERYEAR, 
	    /* 6 */ HALFYEAR,
	    /* 7 */ FIVEYEARS,
	    /* 8 */ DECADE 
	}
	
	
	//GRAN \in {enum}
	public Granularity gran;
	
	public int NumberTopics = -1;
	
	public Timestamp rangeStart;
	public Timestamp rangeEnd;
	
	public String metaTitle;
	public String metaDescription;
	
	
}
