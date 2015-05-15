package com.mytest.dvita.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.ibm.db2.jcc.DB2DataSource;

public class ConnectionManager {
	// 1 = mysql, 2=db2, 3=oracle
	public static int getDbType() { 
		return Integer.parseInt(DVitaConfig.getDbType()); 
	}
		
	public static Connection getConnection() {		
		Connection connection = null;
		try {
			
			// verbindung zum host system aufbauen //MDTODO
			// hier sind die informationen bislang hard-coded
			// optinal kann man sie natürlich auch aus anderen dateien auslesen
			Class.forName(DVitaConfig.getDriver()); 
			connection = DriverManager.getConnection(
					DVitaConfig.getConnectionString(), 
					DVitaConfig.getUserName(),
					DVitaConfig.getPassword());
		
		} catch (SQLException e) {
			
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			
			e.printStackTrace();
		}
	
	    return connection;
		
	}
	
	
	
	public static Connection getRawDataConnection(ConfigRawdataServer info) {
		
		if(info.dataOnHostServer) return getConnection();
		
		// verbindung zu den rohdaten aufbauen
		// informationen sind in ConfigRawdataServer info gesepeichert
		
		Connection connection = null;
		try {
			if(info.type==1) { // mysql
				Class.forName("com.mysql.jdbc.Driver");
				connection = DriverManager.getConnection(
				"jdbc:mysql://"+info.server+":"+info.port+"/"+info.databasename, info.user,info.passwort);
			} else if(info.type == 2) { // DB2 
				 DB2DataSource datasource = new DB2DataSource();
			        datasource.setServerName(info.server);
			        datasource.setUser(info.user);
			        datasource.setPassword(info.passwort);
			        datasource.setDriverType(4); //Type 4 pure Java JDBC Driver 
			        datasource.setPortNumber(info.port);
			        datasource.setDatabaseName(info.databasename);
			        connection = datasource.getConnection();

			} else if (info.type == 3){
				String url = "jdbc:oracle:thin:@"+info.server+":"+info.port+":"+info.databasename;
				 String dcn="oracle.jdbc.driver.OracleDriver";
				    Class.forName(dcn);
				    connection =DriverManager.getConnection(url,info.user,info.passwort);
				
			}else {
				// unkown
				System.err.println("err unknown database type");
				System.exit(-1);
			}
		} catch (ClassNotFoundException e) {
			
			e.printStackTrace();
		} catch (SQLException e) {
			
			e.printStackTrace();
		}
	
	    return connection;
		
	}
	
	public static String only(int AnzahlZeilen){

		switch(getDbType()) {
		case 1: return "LIMIT 0,"+AnzahlZeilen;
		case 2: return "FETCH FIRST "+AnzahlZeilen+" ROWS ONLY";
		default: return "";
		}
	}	
}
