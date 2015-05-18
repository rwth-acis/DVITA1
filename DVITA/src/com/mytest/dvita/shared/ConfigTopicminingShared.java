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
	    /* 8 */ DECADE,
	    /* 9 */ BIYEARLY,
	    /* 10 */ THREEYEARS,
	    /* 11 */ FOURYEARS,
	    /* 12 */ CENTURY
	}
	
	
	//GRAN \in {enum}
	public Granularity gran;
	
	public int NumberTopics = -1;
	
	public Timestamp rangeStart;
	public Timestamp rangeEnd;
	
	public String metaTitle;
	public String metaDescription;
	
	public void setGranularity(int dbValue) {
		switch(dbValue) { 
		 	case 1: this.gran = Granularity.YEARLY; break;
		 	case 2: this.gran = Granularity.MONTHLY; break;
		 	case 3: this.gran = Granularity.WEEKLY; break;
		 	case 4: this.gran = Granularity.DAYLY; break;
		 	case 5: this.gran = Granularity.QUARTERYEAR; break;
		 	case 6: this.gran = Granularity.HALFYEAR; break;
		 	case 7: this.gran = Granularity.FIVEYEARS; break;
		 	case 8: this.gran = Granularity.DECADE; break;
		 	case 9: this.gran = Granularity.BIYEARLY; break;
		 	case 10: this.gran = Granularity.THREEYEARS; break;
		 	case 11: this.gran = Granularity.FOURYEARS; break;
		 	case 12: this.gran = Granularity.CENTURY; break;
		 	default:
		 		System.out.println("Unknown granularity value");
		 		this.gran = Granularity.YEARLY; 
		 		break;
		}
	}
}
