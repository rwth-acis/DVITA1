package com.mytest.dvita.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.servlet.http.HttpSession;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.mytest.dvita.client.DocumentService;
import com.mytest.dvita.shared.DocumentData;
import com.mytest.dvita.shared.DocumentInfo;
import com.mytest.dvita.shared.SerializablePair;
//import com.google.gwt.user.server.rpc.core.java.util.Collections;


/**
 * The server side implementation of the RPC service.
 * @param <wordsOfTopics>
 */
@SuppressWarnings("serial")
public class DocumentServiceImpl extends RemoteServiceServlet implements DocumentService {

	
	public DocumentInfo[] documentSearch(String words){
		
		try {
			Connection connection = ConnectionManager.getConnection();
			HttpSession httpSession = getThreadLocalRequest().getSession(true);
			ConfigTopicminingServer info2 = (ConfigTopicminingServer) httpSession.getAttribute("SetupInfo2");
			ConfigRawdataServer info = (ConfigRawdataServer) httpSession.getAttribute("SetupInfo");
			Connection rawDataConnection = ConnectionManager.getRawDataConnection(info);
	
			
			String[] multipleWords = words.split(" ");
			ArrayList<Integer> wordIDList= new ArrayList<Integer>();
			Statement statement = connection.createStatement();
			
			
			for(int i=0;i<multipleWords.length;i++){
		
			Stemmer s = new Stemmer();
			s.add(multipleWords[i].toCharArray(),multipleWords[i].length());
			s.stem();
			String StemWordOfSearchedItem= s.toString();
			
			//System.out.println("StemWordOfSearchedItem "+StemWordOfSearchedItem);
			

			String sqlquery="SELECT ID from "+DVitaConfig.getSchemaDot()+info.tablePrefix+"_WORDS where STEMNAME='"+StemWordOfSearchedItem+"'";
			ResultSet sql = statement.executeQuery(sqlquery);
			while(sql.next()){
				wordIDList.add(sql.getInt("ID"));
			}
			}
			
			String listOfWordIDs;	
			
			if(wordIDList.isEmpty()) return new DocumentInfo[0];
			
			listOfWordIDs = wordIDList.get(0)+"";
			for(int i=1; i<wordIDList.size(); i++) {
				listOfWordIDs += ","+wordIDList.get(i);
			}
			
			
			ArrayList<Integer> docIDList= new ArrayList<Integer>();
			ArrayList<Integer> wordsQuantityInDoc= new ArrayList<Integer>();
			ResultSet sql;

			String sqlquery = "select distinct t1.DOCID as DOCID,t2.INTERVALID as TOPICTIME, t1.c, t1.q from (SELECT DOCID, count(*) as c, sum(QUANTITY) as q from "
					+ DVitaConfig.getSchemaDot()+info.tablePrefix+"_contains where WORDID in ("+listOfWordIDs+")  group by DOCID ORDER BY count(*) desc, sum(QUANTITY) DESC "+ConnectionManager.only(10)+") t1,"+DVitaConfig.getSchemaDot()+info2.tablePrefix+"_belongto t2 where t1.DOCID=t2.DOCID order by t1.c desc, t1.q desc";
			//System.out.println(sqlquery);
			statement = connection.createStatement();
			sql = statement.executeQuery(sqlquery);
			while(sql.next()){
				 docIDList.add(sql.getInt("DOCID"));
				 wordsQuantityInDoc.add(sql.getInt("TOPICTIME"));

			}
			
			Integer[]idOfDocs=new Integer[docIDList.size()];
			idOfDocs=docIDList.toArray(idOfDocs);
			Integer[]quantityOfWordInDoc=new Integer[wordsQuantityInDoc.size()];
			quantityOfWordInDoc=wordsQuantityInDoc.toArray(quantityOfWordInDoc);
			
			String where = "";
			if(info.whereClause != null && info.whereClause.length()>0) {
				where = info.whereClause + " AND ";
			}

			DocumentInfo [] p = new DocumentInfo[idOfDocs.length];
			Statement statement2 = rawDataConnection.createStatement();
			for(int i=0;i<idOfDocs.length;i++){
			String sqlquery2="select "+info.columnNameTitle+" as Title, "+info.columnNameDate+" as Datum from "+info.fromClause+" Where "+where+""+info.columnNameID+"="+idOfDocs[i]+"";
			sql = statement2.executeQuery(sqlquery2);
			System.out.println(sqlquery2);
		
			p[i]=new DocumentInfo();
			while(sql.next()){
				p[i].docTitle=sql.getString("Title");
				p[i].docDate= sql.getString("Datum");
				}
			}
			
			
			PreparedStatement ps = this.createStatementGetTopicPropotions(connection, info2);


			
			Integer[][]docsAndQuantity=new Integer[idOfDocs.length][quantityOfWordInDoc.length];
			for(int j=0;j<idOfDocs.length;j++){
				docsAndQuantity[j][0]=idOfDocs[j];
				p[j].docID=idOfDocs[j];
				p[j].intervalID=quantityOfWordInDoc[j];
				

				SerializablePair<Integer[], Double[]> res = getTopicProportions(ps,idOfDocs[j]);
				p[j].topicIDs = res.first;
				p[j].topicProportions = res.second;
			}
			
			statement2.close();
			statement.close();
			rawDataConnection.close();
			connection.close();
			return p;
		}
		catch (SQLException e) {
			
			e.printStackTrace();
			return null;
		}
		
	}
	public DocumentInfo [] relatedDocuments (int input,int limit,int topictime) throws IllegalArgumentException{

		try {
			Connection connection = ConnectionManager.getConnection();
			HttpSession httpSession = getThreadLocalRequest().getSession(true);
			//zugreifen auf infos bzgl der DB über getAttribute, die in SetupServise durch setAttribute in dem Session gespeichert wurde!
			ConfigTopicminingServer info2 = (ConfigTopicminingServer) httpSession.getAttribute("SetupInfo2");
			ConfigRawdataServer info = (ConfigRawdataServer) httpSession.getAttribute("SetupInfo");

			Connection rawDataConnection = connection;
			if(!info.dataOnHostServer) {
				rawDataConnection = ConnectionManager.getRawDataConnection(info);
			}


			String sqlquery="SELECT MAX(TOPICID) as x FROM "+DVitaConfig.getSchemaDot()+info2.tablePrefix+"_belongto";
			String sqlquery1="SELECT MIN(TOPICID) as y FROM "+DVitaConfig.getSchemaDot()+info2.tablePrefix+"_belongto";
			Statement statement = connection.createStatement();
			ResultSet sql = statement.executeQuery(sqlquery);
			
			sql.next(); 
			int MAXtopicID = sql.getInt("x");
			
			ResultSet sql1 = statement.executeQuery(sqlquery1);
			sql1.next();
			int MINtopicID= sql1.getInt("y");

			if (input < MINtopicID || input > MAXtopicID){
				return null;
			} else 
			{
				
				

				String sqlquery0= "select DOCID from "+DVitaConfig.getSchemaDot()+info2.tablePrefix+"_belongto WHERE TOPICID= "+input+" AND INTERVALID= "+topictime+" order by TOPICPROPORTION desc "+ConnectionManager.only(limit);
				
				statement = connection.createStatement();
				sql = statement.executeQuery(sqlquery0);

								
				ArrayList<Integer> entries= new ArrayList<Integer>();
				while(sql.next()){
					entries.add(sql.getInt("DOCID")); 	
				}
				System.out.println("Time: " +topictime);

				Integer[] DocID = new Integer[entries.size()];
				DocID = entries.toArray(DocID);

				//platziere die Ausgaben der SQL-Query in der Liste von Arryas

				//ArrayList<String> titelDocs= new ArrayList<String>();
				String DateDoc="";

				DocumentInfo[] newObjects= new DocumentInfo[DocID.length];
				String titelDoc=null;
				Statement statement2 = rawDataConnection.createStatement();
				
				PreparedStatement ps = this.createStatementGetTopicPropotions(connection, info2);

				
				for(int i=0;i<DocID.length;i++){

					//speichere die Objekte eines Dokumentes ja in dem erzeugten Arry für Dokumente =>   Doc2, Doc 4 , Doc 8
					//  Objekte:                                                                        Titel: PC,  Gen,   Handy
					//	Objekte:														     			TopicID: 2,   5,     4
					DocumentInfo resultscollection =new DocumentInfo();
					newObjects[i]=resultscollection;

					//gehe die einzelnen DocID's gespeichert in DocID[i] durch und suche nach deren Titel	
					String where = " WHERE ";
					if(info.whereClause != null && info.whereClause.length()>0) {
						where = " WHERE " + info.whereClause + " AND ";
					}
					String sqlquery2="Select "+info.columnNameTitle+" as Titel, "+info.columnNameDate+" as Datum FROM "+info.fromClause +where+" "+info.columnNameID+"="+DocID[i]+"";
					System.out.println(sqlquery2);

					ResultSet  sql2 = statement2.executeQuery(sqlquery2);
					//speichere die Ausgabe in der Liste der Arrays

					while(sql2.next()){
						titelDoc=sql2.getString("Titel");
						DateDoc=sql2.getString("Datum");
						//System.out.println(DateDoc);
					}
					
					SerializablePair<Integer[], Double[]> p = getTopicProportions(ps,DocID[i]);

					newObjects[i].docID=DocID[i];
					newObjects[i].docTitle= titelDoc;
					newObjects[i].topicIDs= p.first;
					newObjects[i].docDate=DateDoc;
					newObjects[i].topicProportions= p.second; 
					newObjects[i].intervalID=(int)topictime;

				}
				statement2.close();
				statement.close();
				rawDataConnection.close();
				connection.close();
				return newObjects;
				

			}

		} catch (SQLException e) {
			
			e.printStackTrace();
			return null;
		}

	}
	
	protected PreparedStatement createStatementGetTopicPropotions(Connection connection,ConfigTopicminingServer info2) throws SQLException {
		return connection.prepareStatement("select TOPICPROPORTION,TOPICID from "+DVitaConfig.getSchemaDot()+info2.tablePrefix+"_belongto WHERE DOCID=? order by TOPICPROPORTION "); // diese sortierung macht es nachher leichter!? oder wir nehmen hash map
	}
	
	protected SerializablePair<Integer[], Double[]> getTopicProportions(PreparedStatement ps, int docid) throws SQLException {
		
		//save the topic-proportions for visualising its result in a pei chart!!

		ps.setInt(1,docid);
		ResultSet sql3 = ps.executeQuery();

		ArrayList<Double> TOPICPROPORTION=new ArrayList<Double>();
		ArrayList<Integer> IDsTOPIC= new ArrayList<Integer>();

		while(sql3.next()){
			IDsTOPIC.add(sql3.getInt("TOPICID"));
			TOPICPROPORTION.add(sql3.getDouble("TOPICPROPORTION")); 


		}
		//System.out.println("objekt: " + TOPICPROPORTION);
		Integer [] TOPICIDs= new Integer[IDsTOPIC.size()];
		TOPICIDs = IDsTOPIC.toArray(TOPICIDs);
		Double [] TOPICsPROPORTION= new Double[ TOPICPROPORTION.size()];
		TOPICsPROPORTION = TOPICPROPORTION.toArray(TOPICsPROPORTION);

		SerializablePair<Integer[],Double[]> p = new SerializablePair<Integer[],Double[]>();
		p.first = TOPICIDs;
		p.second = TOPICsPROPORTION;
		return p;
	}


	public DocumentData getDocumentData(int docid) throws IllegalArgumentException{
		try {
			Connection connection = ConnectionManager.getConnection();
			HttpSession httpSession = getThreadLocalRequest().getSession(true);
			ConfigRawdataServer info = (ConfigRawdataServer) httpSession.getAttribute("SetupInfo");

			Connection rawDataConnection = connection;
			if(!info.dataOnHostServer) {
				rawDataConnection = ConnectionManager.getRawDataConnection(info);
			}



			{
				System.out.println("Fetching document: " + docid);
				String where = "";
				if(info.whereClause != null && info.whereClause.length()>0) {
					where = info.whereClause + " AND ";
				}
				String theurl = "";
				if(info.columnNameURL!=null) {
					theurl = " "+info.columnNameURL+" as URL, ";
				}
				String copyrightCol = "";
				if(info.columnNameCopyright != null) {
					copyrightCol = " " + info.columnNameCopyright + " as COPYRIGHT, ";
				}
				String textDisplayCol = "";
				if(info.columnNameTextDisplay != null) {
					textDisplayCol = " " + info.columnNameTextDisplay + " as TEXTDISPLAY, ";
				}
				String authorsCol = "";
				if(info.columnNameAuthors != null) {
					authorsCol = " " + info.columnNameAuthors + " as AUTHORS, ";
				}
				
				String sqlquery2 = "select "/*+info.columnNameContent+" as Text, "*/+
						info.columnNameTitle+" as Title, " + 
						theurl + 
						copyrightCol + 
						textDisplayCol +
						authorsCol +
						info.columnNameDate+" as Datum from " + info.fromClause +
						" Where " + where + " " + info.columnNameID + "=" + docid + "";
				
				System.out.println(sqlquery2);
				Statement statement2 = rawDataConnection.createStatement();
				ResultSet sql2 = statement2.executeQuery(sqlquery2);
				//speichere die Ausgabe in der Liste der Arrays

				DocumentData p = new DocumentData();
				while(sql2.next()){
					p.content = info.columnNameTextDisplay != null ? sql2.getString("TEXTDISPLAY") : "";
					p.title=sql2.getString("Title");
					p.date = sql2.getString("Datum");
					if(info.columnNameURL != null) {
						p.url = sql2.getString("URL");
					}
					if(info.columnNameCopyright != null) {
						p.copyright = sql2.getString("COPYRIGHT");
					}
					if(info.columnNameAuthors != null) {
						p.authors = sql2.getString("AUTHORS");
					}					
				}				
				
				rawDataConnection.close();
				connection.close();
				
				return p;
			}

		}
		catch (SQLException e) {
			
			e.printStackTrace();
			return null;
		}


	}


		public DocumentInfo[] similarDocuments(int docid, int topictime) throws IllegalArgumentException {
		// liefert zum gegeben docID die 10 ähnlichsten Dokumente
		// aus dem Zeitpunkt topictime!!!
		// das dokument selbst muss nicht zu topictime vorliegen, aber die anderen dokumente

		try{
			Connection connection = ConnectionManager.getConnection();
			HttpSession httpSession = getThreadLocalRequest().getSession(true);
			ConfigTopicminingServer info2 = (ConfigTopicminingServer) httpSession.getAttribute("SetupInfo2");
			ConfigRawdataServer info = (ConfigRawdataServer) httpSession.getAttribute("SetupInfo");

			Connection rawDataConnection = ConnectionManager.getRawDataConnection(info);

			Statement statement = connection.createStatement();
			
			ArrayList<Integer> DocList= new ArrayList<Integer>();
				String sqlquery="select DOCIDDESTINATION from "+DVitaConfig.getSchemaDot()+info2.tablePrefix+ "_similardocs WHERE INTERVALID="+topictime+" AND DOCIDSOURCE="+docid+" ORDER BY position ASC";
				statement = connection.createStatement();
				System.out.println(sqlquery);
				ResultSet sql = statement.executeQuery(sqlquery);

				while(sql.next()){
					DocList.add(sql.getInt("DOCIDDESTINATION"));
					System.out.println("DOCLIST"+DocList);
				}
				
				
			
						
			
			Statement statement1= rawDataConnection.createStatement();
			PreparedStatement ps = this.createStatementGetTopicPropotions(connection, info2);
			
			// nehme nur die x-besten als ausgabe
			DocumentInfo[] result = new DocumentInfo[DocList.size()];
			for(int i=0; i<DocList.size(); i++) {
				result[i]= new DocumentInfo();
				result[i].docID = DocList.get(i);
				

				SerializablePair<Integer[], Double[]> res = getTopicProportions(ps,result[i].docID);
				result[i].topicIDs = res.first;
				result[i].topicProportions = res.second;
				
				
				String where = "";
				if(info.whereClause != null && info.whereClause.length()>0) {
					where = info.whereClause + " AND ";
				}
				sqlquery="select "+info.columnNameDate+" as Datum, "+info.columnNameTitle+" as Title from "+info.fromClause+" Where "+where+" "+info.columnNameID+"="+result[i].docID+"";
				System.out.println(sqlquery);
				
				sql = statement1.executeQuery(sqlquery);
				
				while(sql.next()){
					result[i].docTitle=sql.getString("Title");
					result[i].docDate=sql.getString("Datum");
					result[i].intervalID=topictime;
				}
				
				
				
			}
			
			rawDataConnection.close();
			connection.close();
			return result; 
			
		}

		catch (SQLException e) {
			
			e.printStackTrace();
			return null;
		}
		catch(Exception e) {
			e.printStackTrace();
			return null;
		}


	}
}
