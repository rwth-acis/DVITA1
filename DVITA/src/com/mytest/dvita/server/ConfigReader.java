package com.mytest.dvita.server;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import com.mytest.dvita.shared.ConfigTopicminingShared.Granularity;


public class ConfigReader {
	// liest aus den entsprechenden Konfigurationstabellen die Daten aus und speichert
	// diese in die entsprechenden Java Objekte	
	
	public static ConfigTopicminingServer readConfigTopicmining(int analysisID, Statement statement) throws SQLException {
		ConfigTopicminingServer info2 = new ConfigTopicminingServer();
		String sqlquery2="SELECT * FROM "+ DVitaConfig.getSchemaDot() +"config_topicmining WHERE id="+analysisID;
		System.out.println(sqlquery2);
		ResultSet sq2 = statement.executeQuery(sqlquery2);

		
		while(sq2.next()) {
			info2.NumberTopics=sq2.getInt("numberTopics");;
			 info2.tablePrefix = sq2.getString("tablePrefix");
			 info2.metaDescription = sq2.getString("meta_description"); 
			 info2.id = sq2.getInt("id");
			 info2.metaTitle = sq2.getString("meta_title"); 
			 info2.rawdataID = sq2.getInt("rawdataID");
			 info2.rangeEnd = sq2.getTimestamp("rangeEnd");
			 info2.rangeStart = sq2.getTimestamp("rangeStart");
			 
			 // Zuordnung der granularity aus DB zu den Granularity aus der Menge{Yearly, Monthly ,...}
			 
			 int gran = sq2.getInt("granularity");
			 System.out.println("Time slice granularity = " + gran);
			 
			 switch(gran) {
			 	case 1: info2.gran = Granularity.YEARLY; break;
			 	case 2: info2.gran = Granularity.MONTHLY; break;
			 	case 3: info2.gran = Granularity.WEEKLY; break;
			 	case 4: info2.gran = Granularity.DAYLY; break;
			 	case 5: info2.gran = Granularity.QUARTERYEAR; break;
			 	case 6: info2.gran = Granularity.HALFYEAR; break;
			 	case 7: info2.gran = Granularity.FIVEYEARS; break;
			 	case 8: info2.gran = Granularity.DECADE; break;
			 	default:
			 		System.out.println("Unknown granularity value");
			 		info2.gran = Granularity.YEARLY; 
			 		break;
			 }
		}

		return info2;
	}
	
	public static ConfigRawdataServer readConfigRawdata(int rawdataID, Statement statement) throws SQLException {

			ConfigRawdataServer info = new ConfigRawdataServer();
			
			String sqlquery="SELECT * FROM "+DVitaConfig.getSchemaDot()+"config_rawdata WHERE id="+rawdataID;
			ResultSet sql = statement.executeQuery(sqlquery);
			
			sql.next();
				
				
				 info.columnNameContent = sql.getString("columnNameContent");
				 info.columnNameDate = sql.getString("columnNameDate");
				 info.columnNameURL = sql.getString("columnNameURL");
				 info.columnNameTitle = sql.getString("columnNameTitle"); 
				 info.fromClause = sql.getString("from"); 
				 info.whereClause = sql.getString("where"); 
				 info.columnNameID = sql.getString("columnNameID"); 
				 info.tablePrefix = sql.getString("tablePrefix"); 
				 info.rawdataID = sql.getInt("id");
				 info.metaTitle = sql.getString("meta_title"); 
				 info.metaDescription = sql.getString("meta_description"); 
				 Integer remote = sql.getInt("connectionID");
				 if(remote == null || remote == 0) {
					 info.dataOnHostServer = true;
				 } else {
					 info.dataOnHostServer = false;
					 // dann nun auch die remote connection daten holen
					 String sqlquery3="SELECT * FROM "+DVitaConfig.getSchemaDot()+"config_connection WHERE id="+remote;
					 ResultSet sq3 = statement.executeQuery(sqlquery3);
					 sq3.next();
					 info.type = sq3.getInt("type");
					 info.server = sq3.getString("server");
					 info.port = sq3.getInt("port");
					 info.databasename = sq3.getString("databasename");
					 
					 
					 
					 // für diese ID ist es in der Datenbank gespeichert
					 String user = sq3.getString("user");
					 if(user != null && user != "null") {
						 info.user = user;
						 info.passwort = sq3.getString("password");
					 }
				 } 
			
	
			return info;

		
	}


}
