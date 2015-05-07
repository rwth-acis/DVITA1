package com.mytest.dvita.server;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;

import javax.servlet.ServletContextEvent;
import javax.servlet.http.HttpSession;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.mytest.dvita.client.SetupService;
import com.mytest.dvita.shared.ConfigRawdataShared;
import com.mytest.dvita.shared.ConfigTopicminingShared;
import com.mytest.dvita.shared.ConfigTopicminingShared.Granularity;
import com.mytest.dvita.shared.SerializablePair;
import com.sun.glass.ui.Application;



@SuppressWarnings("serial")
public class SetupServiceImpl extends RemoteServiceServlet implements SetupService{

	public ConfigTopicminingShared setUpSession(int analysisID, String username, String passwort) {
		
		// username und passwort sind = null wenn es sich um keine
		// remote connection handelt, d.h. die rawdaten auf dem gleichen server liegen
		// wie auch die topicAnalysis, ... daten
		// oder wenn man das Passwort woanders her lesen möchte
		
		 HttpSession httpSession = getThreadLocalRequest().getSession(true);
		 ConfigRawdataServer info = new ConfigRawdataServer();
		 ConfigTopicminingServer info2 = new ConfigTopicminingServer();
		
			Connection connection = ConnectionManager.getConnection();
			try {
				Statement statement = connection.createStatement();
				Statement statement2 = connection.createStatement();

				info2 = ConfigReader.readConfigTopicmining(analysisID, statement2);
				info = ConfigReader.readConfigRawdata(info2.rawdataID, statement);
				
				// wenn manuell ein username/passwort gegeben wurde, dann diese benutzen
				 if(username != null) {
					 info.user = username;
				 }
				 if(passwort != null) {
					 info.passwort = passwort;
				 }
				
			} catch (SQLException e) {
				
				e.printStackTrace();
			}
		 //infos über info2 und info über die DB was von User ausgewählt wird, wird in session gespcihert
		 httpSession.setAttribute("SetupInfo", info);
	     httpSession.setAttribute("SetupInfo2",info2);
	     
	     

		 try {
			connection.close();
		} catch (SQLException e) {
			
			e.printStackTrace();
		}	 	
		 
		 if(info2.NumberTopics==-1) { // es wurde gar nichts initialisiert
			 return null;
		 }
		 
		 ConfigTopicminingShared s = new ConfigTopicminingShared();
		 s.NumberTopics = info2.NumberTopics;
			s.metaDescription = info2.metaDescription; 
			 s.id=info2.id;
			 s.metaTitle=info2.metaTitle; 
			 s.rangeEnd=info2.rangeEnd;
			 s.rangeStart=info2.rangeStart;
		s.gran=	 info2.gran; 
		 
	     return s;
	}
	
	public LinkedList<SerializablePair<ConfigRawdataShared,LinkedList<ConfigTopicminingShared>>> getSetupInformation() {
		
		LinkedList<SerializablePair<ConfigRawdataShared,LinkedList<ConfigTopicminingShared>>>  res = new LinkedList<SerializablePair<ConfigRawdataShared,LinkedList<ConfigTopicminingShared>>>();
		
		String presetPath = getServletContext().getRealPath("/WEB-INF/dvita.config");		
		DVitaConfig.presetPath(presetPath);
		
		Connection connection = ConnectionManager.getConnection();
		try {
			String sqlquery="SELECT * FROM "+DVitaConfig.getSchemaDot()+"config_gui ORDER BY ID ASC";
			Statement statement = connection.createStatement();
			Statement statement2 = connection.createStatement();
			ResultSet sql = statement.executeQuery(sqlquery);
			
			while(sql.next()) {
				
				ConfigRawdataShared info = new ConfigRawdataShared();
				 /*info.content = sql.getString("columnNameContent");
				 info.date = sql.getString("columnNameDate");
				 info.title = sql.getString("columnNameTitle"); 
				 info.from = sql.getString("from"); 
				 info.where = sql.getString("where"); 
				 info.columnNameID = sql.getString("columnNameID"); 
				 info.tablePrefix = sql.getString("tablePrefix"); */
				// obige Infos haben den Client nicht zu interessieren
				 info.rawdataID = sql.getInt("ID");
				 info.metaTitle = sql.getString("title"); 
				 info.metaDescription = sql.getString("description");
				 
				 String[] miningIDs = sql.getString("miningIDs").split(" ");
				 
				 LinkedList<ConfigTopicminingShared> l = new LinkedList<ConfigTopicminingShared>();
				 
				 SerializablePair<ConfigRawdataShared,LinkedList<ConfigTopicminingShared>> p = new SerializablePair<ConfigRawdataShared,LinkedList<ConfigTopicminingShared>>();
				 p.first = info;
				 p.second = l;
				 
				 res.add(p);
				 
				 
				 for(String miningID : miningIDs) {
				
				String sqlquery2="SELECT * FROM "+DVitaConfig.getSchemaDot()+"config_topicmining WHERE ID="+miningID;
				System.out.println(sqlquery2);
				ResultSet sq2 = statement2.executeQuery(sqlquery2);
				
				while(sq2.next()) {
					ConfigTopicminingShared info2 = new ConfigTopicminingShared();
					info2.NumberTopics=sq2.getInt("numberTopics");;
					 info2.metaDescription = sq2.getString("meta_description"); 
					 info2.id = sq2.getInt("id");
					 info2.metaTitle = sq2.getString("meta_title"); 
					 info2.rangeEnd = sq2.getTimestamp("rangeEnd");
					 info2.rangeStart = sq2.getTimestamp("rangeStart");
					 
					 switch( sq2.getInt("granularity")) { 
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
					 
					 l.add(info2);
					 
					
				}
				
				 }
				
				
			}
			
			
		} catch (SQLException e) {
			
			e.printStackTrace();
		}
		
		try {
			connection.close();
		} catch (SQLException e) {
			
			e.printStackTrace();
		}
		return res;
	}
	
	
	
	
	

}

