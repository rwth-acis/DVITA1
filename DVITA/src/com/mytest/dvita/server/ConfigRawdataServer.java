package com.mytest.dvita.server;

import com.mytest.dvita.shared.ConfigRawdataShared;

// dieser Informationen muss nur der Server kennen und nicht der Client
public class ConfigRawdataServer extends ConfigRawdataShared {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public String columnNameID;
	public String columnNameContent;
	public String columnNameTitle;
	public String columnNameDate;
	public String columnNameURL;
	public String columnNameCopyright;
	public String columnNameTextDisplay;
	public String columnNameAuthors;
	public String fromClause;
	public String whereClause;
	
	public String tablePrefix; // welches prefix soll für die tabellen genutzt werden nachdem preprocessed
	
	// rawdata datenbank server informationen
	public String server;
	public int port;
	public String databasename;
	public int type = 3; // 1 = MYSQL, 2 = DB2, 3=ORACLE

	
	public String user;
	public String passwort;
	
	public boolean dataOnHostServer = true;

	
}
