import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;

import com.mytest.dvita.server.ConfigRawdataServer;
import com.mytest.dvita.server.ConfigTopicminingServer;
import com.mytest.dvita.server.ConnectionManager;
import com.mytest.dvita.server.DVitaConfig;

public class TopicRanking {

	boolean overwriteTable = true;
	ConfigTopicminingServer info2;
	ConfigRawdataServer info;

	public Integer[] topicSorting(int buttonTyp, Statement statement) throws IllegalArgumentException {
		
		try {
			
			
			String sqlquery="SELECT count(*) as timesteps FROM "+DVitaConfig.getSchemaDot()+info2.tablePrefix+ "_TOPICINTERVALS";
			ResultSet sql = statement.executeQuery(sqlquery);
			sql.next();
			int timesteps = sql.getInt("timesteps");
			
			ArrayList<Tupel2> list= new ArrayList<Tupel2>();
			
			for(int topicid=0; topicid<info2.NumberTopics; topicid++) {
			
				
				Double[] topicCurrent = Tools.getCurrent(topicid,statement,info2.tablePrefix,timesteps);
			
				if(buttonTyp==1){
					double sum=0;
					double add=0;
					double var;
					for (int i=0; i < topicCurrent.length-1; i++){
						sum= sum+topicCurrent[i];
					}
					double Mean= sum/topicCurrent.length;
					
					for (int i=0; i < topicCurrent.length-1; i++){
					add=add+Math.pow(topicCurrent[i]-Mean, 2);
					
					}
					var=(1.0/(topicCurrent.length-1.0))*add;
					
					Tupel2 t = new Tupel2(-var,topicid);
					list.add(t);
					
				}
				else if(buttonTyp==2){
					
					double g=0;
					for (int i=0; i < topicCurrent.length-1; i++){
						if(topicCurrent[i+1]-topicCurrent[i]<0 ){
							g= g+(topicCurrent[i+1]-topicCurrent[i]);
						}
					}
					Tupel2 t= new Tupel2(g, topicid);
					list.add(t);
				}
				
				else if(buttonTyp==3){
					double g=0;
					for (int i=0; i < topicCurrent.length-1; i++){
						if(topicCurrent[i+1]-topicCurrent[i]>0 ){
							g= g+topicCurrent[i+1]-topicCurrent[i];
						}
					}
					Tupel2 t= new Tupel2(-g, topicid);
					list.add(t);
				}
				else if(buttonTyp==4){
					// mean (here  equal to sum)
					double sum = 0;
					for(double val : topicCurrent) {
						sum += val;
					}
					// negative value to have large sums at the front
					Tupel2 t = new Tupel2(-sum,topicid);
					list.add(t);
					
				}
				else if(buttonTyp==5){
					double g=0;
					double time=0;
					double lambda = 0.1; // decay mit 0.1
					for (int i=topicCurrent.length-1; i > 0; i--){
						g += Math.exp(-lambda*time)*(topicCurrent[i]-topicCurrent[i-1]);
						time++;
					}
					Tupel2 t= new Tupel2(-g, topicid);
					list.add(t);
				}
			
			}
			
			Collections.sort(list);

			Integer[]TopicIds = new Integer[list.size()];
			
			for(int i=0; i<list.size(); i++) {
				System.out.println("  Value of topic " + list.get(i).ID + ": " + list.get(i).value);
				TopicIds[i] = list.get(i).ID;
			}
			
			//return meinRiver;
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

	

	public TopicRanking(ConfigRawdataServer info, ConfigTopicminingServer info2) {

		this.info = info;
		this.info2 = info2;
		
		
		try {
			Connection connection = ConnectionManager.getConnection();
			Statement statement = connection.createStatement();

			
			if(overwriteTable) {
				Tools.DropTableIfExists(statement, info2.tablePrefix+"_TRANKING",DVitaConfig.getSchema());
			}

			// erstmal die Tabllen erstellen
			String sqltable = "CREATE TABLE "+DVitaConfig.getSchemaDot()+info2.tablePrefix+"_TRANKING(RANKTYPE INTEGER NOT NULL, TOPICID INTEGER NOT NULL, RANK INTEGER NOT NULL, PRIMARY KEY(RANKTYPE, TOPICID))";	
			statement.executeUpdate(sqltable);

			for(int sorttype=1; sorttype<=5; sorttype++) {
				System.out.println("SORTING TOPICS (SORT TYPE 1)...");
			
				Integer[] order = topicSorting(sorttype,statement);
			
				for(int position=0; position<order.length; position++) {
				String sqlinsert = "INSERT INTO "+DVitaConfig.getSchemaDot()+info2.tablePrefix+"_TRANKING(RANKTYPE, TOPICID, RANK) VALUES("+
						sorttype + "," + order[position]+ ","+position+")";
					statement.addBatch(sqlinsert);
				}
				statement.executeBatch();
			}
			
			

			connection.close();

		} catch (SQLException e) {
			
			e.printStackTrace();
		}



	}

	public static void main(String[] args) {

		try {
			
			
			//args = new String[1];
			//args[0]= "1111";
			
			
			 if(args.length!=1) { System.out.println("please specify topicMining ID"); System.exit(-1); }
		
			
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

			new TopicRanking(info,info2);

		} catch (SQLException e) {
			
			e.printStackTrace();
		}

		}


}
