package com.mytest.dvita.server;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.servlet.http.HttpSession;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.mytest.dvita.client.WordService;
import com.mytest.dvita.shared.WordData;
import com.mytest.dvita.shared.WordEvolutionData;
 

@SuppressWarnings("serial")
public class WordServiceImpl extends RemoteServiceServlet implements WordService{ 

	
	@Override
	public WordEvolutionData getWordEvolution(Integer[] wordsIds, int topicID) throws IllegalArgumentException {
		try{
			Connection connection = ConnectionManager.getConnection();
			HttpSession httpSession = getThreadLocalRequest().getSession(true);
			ConfigTopicminingServer info2 = (ConfigTopicminingServer) httpSession.getAttribute("SetupInfo2");
			//ConfigRawdataServer info = (ConfigRawdataServer) httpSession.getAttribute("SetupInfo");
			
			/*for(int i=0;i<wordsIds.length;i++){
				System.out.println("IDvonWort: "+wordsIds[i]);
			}*/
			String sqlquery="SELECT intervalStart, intervalEND,ID FROM "+DVitaConfig.getSchemaDot()+info2.tablePrefix+ "_TOPICINTERVALS ";
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
			//System.out.println("TimeId:"+TimeId.length);
			
			TimeId = IntervalIDentries.toArray(TimeId);

		
			//System.out.println("Länge von WordIds "+wordsIds.length);
			//ArrayList<Integer> WordId= new ArrayList<Integer>();
			
			Double[][]wordsCount = new Double [wordsIds.length][TimeId.length];
			for(int i=0; i <wordsIds.length; i++ ){//für jedes Wort
				for (int k=0; k<TimeId.length;k++){// Zu jeder Zeit
				sqlquery="SELECT PROBABILITY FROM "+DVitaConfig.getSchemaDot()+info2.tablePrefix+"_DESCRIBEDBY WHERE WORDID="+wordsIds[i]+" AND INTERVALID="+TimeId[k]+" AND TOPICID="+topicID;
				statement = connection.createStatement();
				sql = statement.executeQuery(sqlquery);
				
				//System.out.println("sql"+sqlquery);
				//int j=0;
				while(sql.next()){//füge die 4 Wörter ein
					wordsCount[i][k]=sql.getDouble("PROBABILITY");
				}

				}
			}


			WordEvolutionData  worddata = new WordEvolutionData(); 
			worddata.intervalStartDate= new String [TimeId.length]; // hier werden die array angelegt
			worddata.intervalIDs= new Integer [TimeId.length];

			for(int i=0;i<TopicTime.length;i++){
				worddata.intervalIDs[i]=TimeId[i];
				worddata.intervalStartDate[i]=TopicTime[i];

			}
			
			worddata.relevanceAtTime=wordsCount;
			worddata.intervalIDs=TimeId;
			worddata.intervalStartDate=TopicTime;
			worddata.wordIDs = wordsIds;
			connection.close();
			return worddata;
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
	@Override
	public WordData bestWords(int topic, int Number, long topictime) {
		try{
			
			Connection connection = ConnectionManager.getConnection();
			HttpSession httpSession = getThreadLocalRequest().getSession(true);
			ConfigTopicminingServer info2 = (ConfigTopicminingServer) httpSession.getAttribute("SetupInfo2");
			ConfigRawdataServer info = (ConfigRawdataServer) httpSession.getAttribute("SetupInfo");
			
			ArrayList<Integer>WordIDs = new ArrayList<Integer>();
			ArrayList<Double>CountInTopic= new ArrayList<Double>();
			ArrayList<String>WordsName= new ArrayList<String>();
			
			WordData wordsOfTopics = new WordData(); 
			
			String sqlquery="select WORDID, PROBABILITY from "+DVitaConfig.getSchemaDot()+ info2.tablePrefix+ "_DESCRIBEDBY WHERE TOPICID="+topic+" AND INTERVALID="+topictime+" ORDER BY PROBABILITY DESC "+ConnectionManager.only(Number);
			Statement statement = connection.createStatement();
			ResultSet sql = statement.executeQuery(sqlquery);
			
			while(sql.next()){
			WordIDs.add(sql.getInt("WORDID"));
			CountInTopic.add(sql.getDouble("PROBABILITY"));
			}
			Integer[]IDsOfWords=new Integer[WordIDs.size()];
			IDsOfWords= WordIDs.toArray(IDsOfWords);
			Double[]CountOfWordsInTopic=new Double[CountInTopic.size()];
			CountOfWordsInTopic=CountInTopic.toArray(CountOfWordsInTopic);
			/*for(int i=0;i<IDsOfWords.length;i++){
				System.out.println(IDsOfWords[i]);
			}*/
			
			//MD
			//java.text.DecimalFormat df = new java.text.DecimalFormat(".##");
			
			for (int i=0;i<IDsOfWords.length;i++){
				sqlquery="select NAME from "+DVitaConfig.getSchemaDot()+ info.tablePrefix+ "_WORDS WHERE  ID="+IDsOfWords[i]+" ";
				statement = connection.createStatement();
				sql = statement.executeQuery(sqlquery);
		
				while (sql.next()){					
					WordsName.add(sql.getString("NAME"));
					
					//MD: display word probability in topic at the current time slice
					//WordsName.add(sql.getString("NAME") + " [" + df.format(CountOfWordsInTopic[i]) + "]" );
				}
			}
		
			String [] Words=new String[WordsName.size()];
			Words= WordsName.toArray(Words);
			
			
			//wordsOfTopics.WordsProportion=CountOfWordsInTopic;
			wordsOfTopics.words=Words;
			wordsOfTopics.wordIds=IDsOfWords;
			
			connection.close();
			return wordsOfTopics;
			
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

