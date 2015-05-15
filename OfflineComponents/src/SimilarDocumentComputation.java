import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.mytest.dvita.server.ConfigRawdataServer;
import com.mytest.dvita.server.ConfigTopicminingServer;
import com.mytest.dvita.server.ConnectionManager;
import com.mytest.dvita.server.DVitaConfig;
import com.mytest.dvita.shared.DocumentInfo;


public class SimilarDocumentComputation {

	boolean overwriteTable = true;


	public SimilarDocumentComputation(ConfigRawdataServer info, ConfigTopicminingServer info2) {


		try {
			Connection connection = ConnectionManager.getConnection();
			Statement statement = connection.createStatement();

			HashMap<Integer,Double[]> docID2TopicProportions = new HashMap<Integer,Double[]>();
			
			HashMap<Integer,HashSet<Integer>> intervalID2Documents = new HashMap<Integer,HashSet<Integer>>();

			
			
			if(overwriteTable) {
				Tools.DropTableIfExists(statement, info2.tablePrefix+"_SIMILARDOCS",DVitaConfig.getSchema());
			}

			// erstmal die Tabllen erstellen
			String sqltable = "CREATE TABLE "+DVitaConfig.getSchemaDot()+info2.tablePrefix+"_SIMILARDOCS(DOCIDSOURCE INTEGER NOT NULL, DOCIDDESTINATION INTEGER NOT NULL, INTERVALID INTEGER NOT NULL, POSITION INTEGER NOT NULL, PRIMARY KEY(DOCIDSOURCE, DOCIDDESTINATION))";	
			statement.executeUpdate(sqltable);

			sqltable = "CREATE INDEX "+DVitaConfig.getSchemaDot()+info2.tablePrefix+"_E ON "+DVitaConfig.getSchemaDot()+info2.tablePrefix+"_SIMILARDOCS (DOCIDSOURCE, INTERVALID)";
			statement.executeUpdate(sqltable);
		
	

			Timer t2 = new Timer();
			t2.start();

			
			Timer t = new Timer();
			t.start();
			
			// obtain max interval id
			String sqlquery="select max(id) as MAXINTERVAL from " + DVitaConfig.getSchemaDot()+info2.tablePrefix+ "_TOPICINTERVALS";
			Statement maxInterval = connection.createStatement();
			ResultSet miRes = maxInterval.executeQuery(sqlquery);
			miRes.next();
			int maxIntervalID = miRes.getInt("MAXINTERVAL");
			
			
			// gehe durch alle dokumente
			sqlquery="select DISTINCT DOCID, INTERVALID from " + DVitaConfig.getSchemaDot()+info2.tablePrefix+ "_BELONGTO";
			Statement statementAllDocs = connection.createStatement();
			ResultSet sql2 = statementAllDocs.executeQuery(sqlquery);
			while(sql2.next()) {

				int docid = sql2.getInt("DOCID");
				int myintervalID = sql2.getInt("INTERVALID");

				System.out.println("Computing similarities for document with ID " + docid);
				
				int nrTopics = info2.NumberTopics;


				for(int intervalTime = Math.max(myintervalID - info2.similarDocsTimeShift, 0); 
					intervalTime <= Math.min(myintervalID + info2.similarDocsTimeShift, maxIntervalID); 
					intervalTime++) 
				{
					//<> heiﬂt ungleich
					HashSet<Integer> DocList= intervalID2Documents.get(intervalTime);
					
					if(DocList == null) {
							
						t.pause();
						
						DocList = new HashSet<Integer>();
					
					
						sqlquery="select DISTINCT DOCID from " + DVitaConfig.getSchemaDot()+info2.tablePrefix+ "_BELONGTO WHERE INTERVALID="+intervalTime;
						statement = connection.createStatement();
						//System.out.println(sqlquery);
						ResultSet sql = statement.executeQuery(sqlquery);
	
						while(sql.next()){
							DocList.add(sql.getInt("DOCID"));
							//System.out.println("DOCLIST"+DocList);
						}
						
						intervalID2Documents.put(intervalTime,DocList);
					
						t.start();
					}

					/**
					 * betrachte erst das angeklickte Dokument! 
					 *  speichere in TopicproportionOfResearchedDocid die id von topics
					 *  wo der angeklickte Doc in diesen Topics(Topicproportion) auftritt
					 */
					Double[] TopicproportionOfResearchedDocid= docID2TopicProportions.get(docid);

					if(TopicproportionOfResearchedDocid==null) {
						
						t.pause();
						
						TopicproportionOfResearchedDocid = new Double[nrTopics];
						for(int i=0; i<nrTopics; i++) {
							TopicproportionOfResearchedDocid[i] = 0.0;
						}
						sqlquery="select TOPICPROPORTION, TOPICID from " + DVitaConfig.getSchemaDot()+info2.tablePrefix+ "_BELONGTO WHERE DOCID="+docid+" " ;
						statement = connection.createStatement();
						//System.out.println(sqlquery);
						ResultSet sql = statement.executeQuery(sqlquery);
						while(sql.next()){
							TopicproportionOfResearchedDocid[sql.getInt("TOPICID")]=sql.getDouble("TOPICPROPORTION");
	
						}
						
						docID2TopicProportions.put(docid, TopicproportionOfResearchedDocid);
					
						t.start();
					}

					//ZU Jedem Doc muss die 10 ‰hnlichsten bereit gespeichert sein



					//Klasse Tupel unter Server!
					ArrayList<Tupel> tupel=new ArrayList<Tupel>();

					for (int otherDocId : DocList){
						
						if(otherDocId == docid) continue; // nicht mit sich selbst vergleichen
						
						
						Double[] TopicproportionsOtherDoc= docID2TopicProportions.get(otherDocId);
						
						if(TopicproportionsOtherDoc==null) {
							
							t.pause();
							
							TopicproportionsOtherDoc = new Double[nrTopics];
							for(int i=0; i<nrTopics; i++) {
								TopicproportionsOtherDoc[i] = 0.0;
							}
							/**
							 * Hier speichere in  TopicproportionsOtherDoc die ids von topics
							 * wo die verglichenen Docs in diesen Topics(Topicproportion) auftreten
							 */
							sqlquery="select TOPICPROPORTION, TOPICID from  " + DVitaConfig.getSchemaDot()+info2.tablePrefix+ "_BELONGTO WHERE DOCID="+otherDocId+"" ;
							//System.out.println(sqlquery);
							ResultSet sql = statement.executeQuery(sqlquery);
	
							//int numberNonZeroTopics = 0;
							while(sql.next()){
								TopicproportionsOtherDoc[sql.getInt("TOPICID")]=sql.getDouble("TOPICPROPORTION");
								//numberNonZeroTopics++;
							}
							
							docID2TopicProportions.put(otherDocId, TopicproportionsOtherDoc);
						
							t.start();
						}
						

						DocumentInfo docinfo = new DocumentInfo();

						docinfo.docID = otherDocId;
						// kopiere die oben bestimmten Porpoertions in das Array f¸r die Ausgabe
						// damit ¸bertragung schneller geht, speichere nur die nicht 0 topics
						docinfo.topicProportions = new Double[nrTopics];
						docinfo.topicIDs = new Integer[nrTopics];
						int k=0;
						for(int i=0; i<nrTopics; i++) {
							docinfo.topicProportions[k]=TopicproportionsOtherDoc[i];
							docinfo.topicIDs[k] = i;
							k++;
						}
						
						//System.out.println(TopicproportionOfResearchedDocid + " " +  TopicproportionsOtherDoc);

						/**
						 * F¸r die Suche nach ‰hnlichen Dokumenten zu dem angeklickten Dokument: 
						 */
						Double tmp=JensenShanonDivergenz.JSD(TopicproportionOfResearchedDocid, TopicproportionsOtherDoc);
						//System.out.println(tmp + " " + Arrays.toString(TopicproportionOfResearchedDocid) + " " + Arrays.toString(TopicproportionsOtherDoc));
						tupel.add(new Tupel(tmp,docinfo));
					}

					java.util.Collections.sort(tupel);

					
					int maxSimilar = Math.min(info2.similarDocsCount,tupel.size());

					//System.out.println(maxSimilar + " *** maxaenhliche");
					//if(maxSimilar==9) System.exit(-1);
					
					t.pause();
					
					// nehme nur die x-besten als ausgabe
					for(int i=0; i<maxSimilar; i++) {
						DocumentInfo result = tupel.get(i).theDoc;
				

		
						///////////////////////////INSERT IN TABLE////////////////////////////////////////////////////////////////////

						
						
								String sqlinsert = "INSERT INTO "+DVitaConfig.getSchemaDot()+info2.tablePrefix+"_SIMILARDOCS(DOCIDSOURCE, DOCIDDESTINATION, INTERVALID, POSITION) VALUES("+
										docid + ","+ result.docID+ "," + intervalTime+ ","+(i+1)+")";
								statement.executeUpdate(sqlinsert);

		
					}
					
					t.start();
					
				}
			}


			long runtime = t.stop();
			long runtime2 = t2.stop();
			
			System.out.println("Runtime without database connection: " + runtime + " ms");
			System.out.println("Overall runtime: " + runtime2 + " ms");
			connection.close();

		} catch (SQLException e) {
			
			e.printStackTrace();
		}



	}

	public static void main(String[] args) {

		try {
			
			
			//args = new String[1];
			//args[0]= "1111";
			
			 if(args.length < 1) { System.out.println("please specify topicMining ID"); System.exit(-1); }
					 
			ConfigRawdataServer info = new ConfigRawdataServer();
			ConfigTopicminingServer info2 = new ConfigTopicminingServer();
			Connection connection = ConnectionManager.getConnection();
			Statement statement = connection.createStatement();
			
			 // eintrag in spalte ID aus Tabelle config_topicmining
			 int id = Integer.parseInt(args[0]);


			info2 = com.mytest.dvita.server.ConfigReader.readConfigTopicmining(id, statement);
			info = com.mytest.dvita.server.ConfigReader.readConfigRawdata(info2.rawdataID, statement);

			statement.close();
			connection.close();
			
			if(args.length > 1) {
				info2.similarDocsTimeShift = Integer.parseInt(args[1]);
				System.out.println("Overriding number of time slices for similarity computation: "  + info2.similarDocsTimeShift);
			 }

			new SimilarDocumentComputation(info,info2);

		} catch (SQLException e) {
			
			e.printStackTrace();
		}


	}

}
