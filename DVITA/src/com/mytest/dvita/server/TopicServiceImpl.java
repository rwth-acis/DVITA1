package com.mytest.dvita.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;

import javax.servlet.http.HttpSession;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.mytest.dvita.client.TopicService;
import com.mytest.dvita.shared.ThemeRiverData;
import com.mytest.dvita.shared.TopicLabels;


@SuppressWarnings("serial")
public class TopicServiceImpl extends RemoteServiceServlet implements TopicService{ 


	static int nrWords = 4;


	public TopicLabels getTopicList() throws IllegalArgumentException {
		try {
			Connection connection = ConnectionManager.getConnection();
			HttpSession httpSession = getThreadLocalRequest().getSession(true);
			ConfigTopicminingServer info2 = (ConfigTopicminingServer) httpSession.getAttribute("SetupInfo2");
			ConfigRawdataServer info = (ConfigRawdataServer) httpSession.getAttribute("SetupInfo");


			TopicLabels  tlabels = new TopicLabels();

			String sqlquery="SELECT DISTINCT TOPICID FROM "+DVitaConfig.getSchemaDot()+info2.tablePrefix+"_describedby ";
			Statement statement = connection.createStatement();
			ResultSet sql = statement.executeQuery(sqlquery);

			ArrayList<Integer> TopicIDs= new ArrayList<Integer>();
			while (sql.next()){
				TopicIDs.add(sql.getInt("TOPICID"));
			}
			Integer[]TopicIds = new Integer[TopicIDs.size()];
			TopicIds = TopicIDs.toArray(TopicIds);
			System.out.println("Länge von TopicIds "+TopicIds.length);
			//ArrayList<Integer> WordId= new ArrayList<Integer>();
			Integer[][]WORDIDS = new Integer [TopicIds.length][nrWords];
			String[][]WORDS = new String [TopicIds.length][nrWords];
			for(int i=0; i <TopicIds.length; i++ ){

				//MYSQL:				sqlquery="SELECT WORDID, SUM( PROBABILITY ) AS A FROM "+ConnectionManager.schema+info2.tablePrefix+"_describedby WHERE TOPICID = "+TopicIds[i]+" GROUP BY WORDID ORDER BY A DESC  LIMIT 0 , "+nrWords;
				sqlquery="SELECT WORDID, SUM( PROBABILITY ) AS A FROM "+DVitaConfig.getSchemaDot()+info2.tablePrefix+"_describedby WHERE TOPICID = "+TopicIds[i]+" GROUP BY WORDID ORDER BY A DESC "+ConnectionManager.only(nrWords);
				statement = connection.createStatement();
				sql = statement.executeQuery(sqlquery);

				int j=0;
				while(sql.next()){
					WORDIDS[i][j]=(sql.getInt("WORDID"));
					System.out.println("WORDIDS "  +i+ "und "+j+"ist:"+ WORDIDS[i][j]);

					String sqlquery1="SELECT NAME FROM "+DVitaConfig.getSchemaDot()+info.tablePrefix+"_WORDS WHERE ID="+WORDIDS[i][j]+" ";
					Statement statement1 = connection.createStatement();
					ResultSet sql1 = statement1.executeQuery(sqlquery1);
					//int k=0;
					sql1.next();
					WORDS[i][j]=sql1.getString("NAME");

					j++;
				}

			}
			tlabels.wordIDs=WORDIDS;
			tlabels.words=WORDS;
			tlabels.topicIDs=TopicIds ;
			connection.close();
			return tlabels;
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


	public Integer[] topicRanking(int buttonTyp) throws IllegalArgumentException {

		try {
			Connection connection = ConnectionManager.getConnection();
			HttpSession httpSession = getThreadLocalRequest().getSession(true);
			ConfigTopicminingServer info2 = (ConfigTopicminingServer) httpSession.getAttribute("SetupInfo2");


			String sqlquery="SELECT TOPICID FROM "+DVitaConfig.getSchemaDot()+info2.tablePrefix+ "_TRANKING WHERE RANKTYPE="+buttonTyp+" ORDER BY RANK ASC";
			Statement statement = connection.createStatement();
			ResultSet sql = statement.executeQuery(sqlquery);

			Integer[]TopicIds = new Integer[info2.NumberTopics];

			int pos = 0;
			while(sql.next()) {
				TopicIds[pos] = sql.getInt("TOPICID");
				pos++;
			}

			connection.close();
			return TopicIds;
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
	public Integer[] topicSearch(String textitem) throws IllegalArgumentException{

		try{
			Connection connection = ConnectionManager.getConnection();
			HttpSession httpSession = getThreadLocalRequest().getSession(true);
			ConfigTopicminingServer info2 = (ConfigTopicminingServer) httpSession.getAttribute("SetupInfo2");
			ConfigRawdataServer info = (ConfigRawdataServer) httpSession.getAttribute("SetupInfo");

			//System.out.println("Textinput "+textitem);
			String[] multipleWords = textitem.split(" ");
			ArrayList<Integer> TopicList= new ArrayList<Integer>();

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
				//System.out.println("SQLQUERY:"+sqlquery);
				while(sql.next()){
					wordIDList.add(sql.getInt("ID"));

				}
			}

			String listOfWordIDs;	
			if(wordIDList.size()>0) {
				listOfWordIDs = wordIDList.get(0)+"";
				for(int i=1; i<wordIDList.size(); i++) {
					listOfWordIDs += ","+wordIDList.get(i);
				}
			} else {
				return new Integer[0];
			}

			String sqlquery="select t.TOPICID as ID from "+DVitaConfig.getSchemaDot()+info2.tablePrefix+"_describedby t WHERE t.WORDID in ("+listOfWordIDs+") GROUP BY t.TOPICID ORDER BY count(*) DESC, sum(t.PROBABILITY) DESC";
			//System.out.println(sqlquery);
			ResultSet sql = statement.executeQuery(sqlquery);

			while(sql.next()){
				TopicList.add(sql.getInt("ID"));
			}

			Integer[]Topics= new Integer[TopicList.size()];
			Topics= TopicList.toArray(Topics);
			for(int i=0;i<Topics.length;i++){
			}

			connection.close();
			return Topics;
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

	public String [][] getTimeintervals()throws IllegalArgumentException{
		try{
			Connection connection = ConnectionManager.getConnection();
			HttpSession httpSession = getThreadLocalRequest().getSession(true);
			ConfigTopicminingServer info2 = (ConfigTopicminingServer) httpSession.getAttribute("SetupInfo2");
			//ConfigRawdataServer info = (ConfigRawdataServer) httpSession.getAttribute("SetupInfo");

			ArrayList<String> startOfTime= new ArrayList<String>();
			ArrayList<String> endOfTime= new ArrayList<String>();
			ArrayList<Integer> idOfTime= new ArrayList<Integer>();

			String sqlquery="SELECT intervalStart, intervalEND,ID FROM "+DVitaConfig.getSchemaDot()+info2.tablePrefix+ "_topicintervals Order by ID";
			Statement statement = connection.createStatement();
			ResultSet sql = statement.executeQuery(sqlquery);

			while(sql.next()){
				startOfTime.add(sql.getString("intervalStart"));
				endOfTime.add(sql.getString("intervalEnd"));
				idOfTime.add(sql.getInt("ID"));
			}			

			String[]startOfTimeIntervall= new String [startOfTime.size()];
			String[]endOfTimeIntervall= new String [endOfTime.size()];


			startOfTimeIntervall=startOfTime.toArray(startOfTimeIntervall);
			endOfTimeIntervall=endOfTime.toArray(endOfTimeIntervall);

			String[][] timeInterval= new String[startOfTimeIntervall.length][endOfTimeIntervall.length];

			int i=0;
			int k=1;	
			for(int j=0;j<startOfTimeIntervall.length;j++){
				timeInterval[j][i]=startOfTimeIntervall[j];	
				timeInterval[j][k]=endOfTimeIntervall[j];	
			}

			connection.close();	
			return timeInterval;

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


	public ThemeRiverData [] getTopicCurrent(Integer [] Topics) throws IllegalArgumentException { 

		try{
			Connection connection = ConnectionManager.getConnection();
			HttpSession httpSession = getThreadLocalRequest().getSession(true);
			ConfigTopicminingServer info2 = (ConfigTopicminingServer) httpSession.getAttribute("SetupInfo2");
			ConfigRawdataServer info = (ConfigRawdataServer) httpSession.getAttribute("SetupInfo");

			String sqlquery="SELECT intervalStart, intervalEND,ID FROM "+DVitaConfig.getSchemaDot()+info2.tablePrefix+ "_topicintervals ORDER BY ID ASC";
			Statement statement = connection.createStatement();
			ResultSet sql = statement.executeQuery(sqlquery);

			ArrayList<String> IntervalStartentries= new ArrayList<String>();
			ArrayList<Integer> IntervalIDentries= new ArrayList<Integer>();
			while(sql.next()){
				IntervalStartentries.add(sql.getString("intervalStart")); 	
				IntervalIDentries.add(sql.getInt("ID"));
			}
			String[] TopicTime = new String[IntervalStartentries.size()];
			TopicTime = IntervalStartentries.toArray(TopicTime);
			Integer [] TimeId = new Integer[IntervalIDentries.size()];
			System.out.println("TimeId:"+TimeId.length);

			TimeId = IntervalIDentries.toArray(TimeId);
			//int ArrayLength= TopicTime.length;

			System.out.println("Länge von TopicIds "+Topics.length);
			//ArrayList<Integer> WordId= new ArrayList<Integer>();
			Integer[]WORDIDS = new Integer[nrWords];
			ThemeRiverData [] riverdata= new ThemeRiverData[Topics.length];

			System.out.println("input" + Arrays.toString(Topics));


			PreparedStatement ps = connection.prepareStatement("SELECT WORDID FROM "+DVitaConfig.getSchemaDot()+info2.tablePrefix+"_describedby WHERE TOPICID=? AND INTERVALID=? ORDER BY PROBABILITY DESC "+ConnectionManager.only(nrWords)); 

			PreparedStatement ps2 = connection.prepareStatement("SELECT NAME FROM "+DVitaConfig.getSchemaDot()+info.tablePrefix+"_WORDS WHERE ID=?");


			for(int i=0; i <Topics.length; i++ ){//für jedes topic

				String[][]WORDS = new String[TimeId.length][nrWords];

				for (int k=0; k<TimeId.length;k++){// Zu jeder Zeit

					//sqlquery="SELECT WORDID FROM "+ConnectionManager.schema+info2.tablePrefix+"_describedby WHERE TOPICID="+Topics[i]+" AND INTERVALID="+TimeId[k]+" ORDER BY PROBABILITY DESC "+ConnectionManager.only(nrWords);
					ps.setInt(1, Topics[i]);
					ps.setInt(2, TimeId[k]);

					sql = ps.executeQuery();

					//System.out.println("sql"+sql);
					int j=0;
					while(sql.next()){//füge die 4 Wörter ein
						WORDIDS[j]=(sql.getInt("WORDID"));

						ps2.setInt(1,WORDIDS[j]);
						ResultSet sql1 = ps2.executeQuery();
						sql1.next();
						WORDS[k][j]=sql1.getString("NAME");

						j++;
					}


					sql.close();
					//System.out.println("words: " + Arrays.toString(WORDS[k]));

				}


				Double[] TopicLists = getCurrent(Topics[i],statement,info2.tablePrefix,TimeId.length);

				riverdata[i] =new ThemeRiverData(); 

				System.out.println("topics"+Topics[i]);
				riverdata[i].topicID=Topics[i];
				riverdata[i].relevanceAtTime=TopicLists;
				riverdata[i].wordsAtTime=WORDS;

				System.out.println("words " + Arrays.toString(WORDS[0]));


			}

			statement.close();
			ps.close();
			ps2.close();

			connection.close();
			return riverdata;


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

	protected static Double[] getCurrent(int topic, Statement statement, String tablePrefix, int timesteps) throws SQLException {
		Double[] TopicLists= new Double[timesteps];
		//ArrayList<Integer> TopicID= new ArrayList<Integer>();

		for(int j= 0; j<timesteps; j++){

			// bestimme dokumente pro zeitintervall
			// ist nötig für den average
			// (wir können unten NICHT den SQL avg nehmen, da wir Dokumente
			// mit zu geringen TopicPropoertions gepruned haben, das sind aber
			// für jedes Topic andere Dokumente)
			String sqlquery="SELECT count(DISTINCT DOCID) AS numberDocs FROM  "+DVitaConfig.getSchemaDot()+tablePrefix+ "_belongto WHERE INTERVALID ="+j+"";
			ResultSet sql = statement.executeQuery(sqlquery);
			sql.next();
			double numberDocs = sql.getDouble("numberDocs");

			// average oder sum??? evtl. sum umschalten
			// wenn zu jedem zeitpunkt sehr unterschiedeliche anzahl dokumente sind
			// dann koentte average besser sein
			// ACHTUNG: hier dennoch sum. average kommt gleich
			// liegt daran, dass wir pro topic unterschiedliche anzahl docs haben
			// daher ist der average nicht der normale average
			sqlquery="SELECT SUM(  TOPICPROPORTION ) AS y  FROM  " +DVitaConfig.getSchemaDot()+tablePrefix+ "_belongto WHERE TOPICID= "+topic+" AND  INTERVALID ="+j+"";
			sql = statement.executeQuery(sqlquery);
			sql.next();
			TopicLists[j]=sql.getDouble("y")/numberDocs; // nun ist der average korrekt berechnet!!!
			System.out.println("einträge für " +j+"von Topic"+topic+ "ist :"+ TopicLists[j]);



		}

		return TopicLists;
	}






}

