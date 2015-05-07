import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

import com.mytest.dvita.server.ConfigRawdataServer;
import com.mytest.dvita.server.ConfigReader;
import com.mytest.dvita.server.ConfigTopicminingServer;
import com.mytest.dvita.server.ConnectionManager;
import com.mytest.dvita.server.DVitaConfig;
import com.mytest.dvita.shared.TopicLabels;

// vergeleiche die topics (d.h. deren wortverteilungen) von verschiedenen topics analysen
public class CompareWordDistribution {

	boolean overwriteTable = true;
	

	static public int nrWords = 10;


	private static TopicLabels res1;


	private static TopicLabels res2;


	private static Double[][] WordProbs;


	private static Double[][] WordProbs1;
	private static Double[][] WordProbs2;


	public static TopicLabels getWords(ConfigRawdataServer info, ConfigTopicminingServer info2) {


		
			try {
				Connection connection = ConnectionManager.getConnection();
				Statement statement = connection.createStatement();

				TopicLabels  meinRiver = new TopicLabels();
//		ThemeRiver2 meinRiver2 = new ThemeRiver2();

				String sqlquery="SELECT DISTINCT TOPICID FROM "+ DVitaConfig.getSchemaDot() +info2.tablePrefix+"_describedby ";
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
				WordProbs = new Double [TopicIds.length][nrWords];
				
				for(int i=0; i <TopicIds.length; i++ ){

					//sqlquery="SELECT DISTINCT WORDID FROM "+info2.tablePrefix+"_describedby WHERE TOPICID="+TopicIds[i]+" ORDER BY COUNT DESC  LIMIT 0 , "+nrWords;
//MYSQL:				sqlquery="SELECT WORDID, SUM( COUNT ) AS A FROM "+ConnectionManager.schema+info2.tablePrefix+"_describedby WHERE TOPICID = "+TopicIds[i]+" GROUP BY WORDID ORDER BY A DESC  LIMIT 0 , "+nrWords;
					sqlquery="SELECT WORDID, SUM( PROBABILITY ) AS A FROM "+DVitaConfig.getSchemaDot()+info2.tablePrefix+"_describedby WHERE TOPICID = "+TopicIds[i]+" GROUP BY WORDID ORDER BY A DESC "+ConnectionManager.only(nrWords);
					statement = connection.createStatement();
					sql = statement.executeQuery(sqlquery);
					//for(int j=0; j <WORDID.; j++){
					int j=0;
					while(sql.next()){
						WORDIDS[i][j]=(sql.getInt("WORDID"));
						WordProbs[i][j]=(sql.getDouble("A"));
						
						System.out.println("WORDIDS "  +i+ "und "+j+"ist:"+ WORDIDS[i][j]);

						String sqlquery1="SELECT NAME FROM "+DVitaConfig.getSchemaDot()+info.tablePrefix+"_WORDS WHERE ID="+WORDIDS[i][j]+" ";
						Statement statement1 = connection.createStatement();
						ResultSet sql1 = statement1.executeQuery(sqlquery1);
						//int k=0;
						sql1.next();
						WORDS[i][j]=sql1.getString("NAME");
						//System.out.println("WORDS "  +i+ "und "+j+"ist:"+ WORDS[i][j]);
						//k++;

						j++;
					}

				}
				meinRiver.wordIDs=WORDIDS;
				meinRiver.words=WORDS;
				meinRiver.topicIDs=TopicIds ;
				
				
				connection.close();
				
				return meinRiver;

			} catch (SQLException e) {
				
				e.printStackTrace();
			}
			return null;

	}

	public static void main(String[] args) {

		try {
			
			int topicMiningIDa = 16;
			int topicMiningIDb = 16; // 25;
			CompareWordDistribution.nrWords = 10;

			

			{
			ConfigRawdataServer info = new ConfigRawdataServer();
			ConfigTopicminingServer info2 = new ConfigTopicminingServer();
			Connection connection = ConnectionManager.getConnection();
			Statement statement = connection.createStatement();
			
			
			info2 = ConfigReader.readConfigTopicmining(topicMiningIDa, statement);
			info = ConfigReader.readConfigRawdata(info2.rawdataID, statement);

			statement.close();
			connection.close();

			res1 = CompareWordDistribution.getWords(info,info2);
			WordProbs1 = WordProbs;
			}
			
			{
				ConfigRawdataServer info = new ConfigRawdataServer();
				ConfigTopicminingServer info2 = new ConfigTopicminingServer();
				Connection connection = ConnectionManager.getConnection();
				Statement statement = connection.createStatement();
				
					info2 = ConfigReader.readConfigTopicmining(topicMiningIDb, statement);
				info = ConfigReader.readConfigRawdata(info2.rawdataID, statement);

				statement.close();
				connection.close();

				res2 = CompareWordDistribution.getWords(info,info2);
				WordProbs2= WordProbs;
				}
			
			
			
			// nun irgendwas mit den Ergebnissen machen
			// erstmal einfach ausgeben
			System.out.println("\n\nTopicMiningID: " + topicMiningIDa);
			for(int t=0; t<res1.topicIDs.length; t++)  {
				System.out.println("  Topic " + t + ": ");
				System.out.print("    ");
				for(int j=0; j<res1.words[t].length; j++)  {
					System.out.print(res1.words[t][j] + "(" + (WordProbs1[t][j])+ ") ");
				}
				System.out.println("");
			}
			
			System.out.println("TopicMiningID: " + topicMiningIDb);
			for(int t=0; t<res2.topicIDs.length; t++)  {
				System.out.println("  Topic " + t + ": ");
				System.out.print("    ");
				for(int j=0; j<res2.words[t].length; j++)  {
					System.out.print(res2.words[t][j] + "(" + (WordProbs2[t][j])+ ") ");
				}
				System.out.println("");
			}
			
			
			HashSet<Integer> wortIDs = new HashSet<Integer>();
			
			LinkedList<Integer> sort = new LinkedList<Integer>();
			String bestout = "";
			
			for(int t=0; t<res1.topicIDs.length; t++)  {
				double best = Double.MAX_VALUE;
				int index = 0;
				System.out.print("Exp1-t"+t);
			for(int t2=0; t2<res2.topicIDs.length; t2++)  {

				for(int w : res1.wordIDs[t]) { wortIDs.add(w); }
				for(int w : res2.wordIDs[t2]) { wortIDs.add(w); }

				
					double dist = jsd(res2.wordIDs[t2],WordProbs2[t2],res1.wordIDs[t],WordProbs[t]);
				//System.out.println("Exp1-t"+t+"+Exp2-t"+t2+": " +dist);
					System.out.print("\t" +dist);
					if(dist<best) {
						best = dist;
						index = t2;
					}
				}
			bestout += "\nBestMatch: Exp1-t"+t+"+Exp2-t"+index+": " +best;	
			if(!sort.contains(index)) {
				sort.add(index);
			}
			System.out.println("");
			}
			
			System.out.println(bestout);
			
			
			for(int t2=0; t2<res2.topicIDs.length; t2++)  {
				if(!sort.contains(t2)) {
					sort.add(t2);
				}
			}
			
			// nochmal sortiert ausgeben
			
			for(int t2: sort) {
				System.out.print("\tExp2-t"+t2);
			}
			System.out.println("");
			
			for(int t=0; t<res1.topicIDs.length; t++)  {
				double best = Double.MAX_VALUE;
				
				System.out.print("Exp1-t"+t);
			for(int t2 : sort)  {

				for(int w : res1.wordIDs[t]) { wortIDs.add(w); }
				for(int w : res2.wordIDs[t2]) { wortIDs.add(w); }

				
					double dist = jsd(res2.wordIDs[t2],WordProbs2[t2],res1.wordIDs[t],WordProbs[t]);
				//System.out.println("Exp1-t"+t+"+Exp2-t"+t2+": " +dist);
					System.out.print("\t" +dist);
					if(dist<best) {
						best = dist;
					
					}
				}
			System.out.println("");
			}
			
			// ENDE sortierung
			
			
			
			
			
			System.out.print("\n\nWORT");
			for(int t=0; t<res1.topicIDs.length; t++)  {
				System.out.print("\tTopic"+t);
			}
			for(int t2=0; t2<res2.topicIDs.length; t2++)  {
				System.out.print("\tTopic"+t2);
			}
			System.out.println("");

			
			
			for(int word : wortIDs) {
				
				String theWord = "";
				String out = "";
				for(int t=0; t<res1.topicIDs.length; t++)  {
					int pos = find(res1.wordIDs[t],word);
					if(pos == -1) {
						out += "\t0";
					} else {
						out += "\t"+WordProbs1[t][pos];
						theWord = res1.words[t][pos];
					}
				}
				for(int t2=0; t2<res2.topicIDs.length; t2++)  {
					int pos = find(res2.wordIDs[t2],word);
					if(pos == -1) {
						out += "\t0";
					} else {
						out += "\t"+WordProbs2[t2][pos];
						theWord = res2.words[t2][pos];
					}
				}

				System.out.println(theWord + out);
			}
			
			
			
		
		} catch (SQLException e) {
			
			e.printStackTrace();
		}


	}
	
	
	private static double jsd(Integer[] integers, Double[] doubles,
			Integer[] integers2, Double[] doubles2) {

		// da nicht beidesmal die gleichen worte vorkommen müssen ist es etwas komplizierter
		// sammle erst die Indizes die vorkommmen
		HashSet<Integer> indizes = new HashSet<Integer>();
		for(int i : integers) { indizes.add(i); }
		for(int i : integers2) { indizes.add(i); }
		
		double sum1 = 0;
		double sum2 = 0;
		for(double d : doubles) {
			sum1 += d;
		}
		for(double d : doubles2) {
			sum2 += d;
		}
		
		Double array1[]  = new Double[indizes.size()];
		Double array2[]  = new Double[indizes.size()];

		// nun durch alle indizes gehen und deren probes vergleichen
		//double dist = 0;
		//double distMax = 0;
		int pos = 0;
		for(int i : indizes) {
			// finde entsprechende position in array
			int pos1 = find(integers,i);
			int pos2 = find(integers2,i);
			if(pos1 != -1) {
				array1[pos] = doubles[pos1]/sum1;
			} else {
				array1[pos] = 0.0;
			}
			
			if(pos2 != -1) {
				array2[pos] = doubles2[pos2]/sum2;
			} else {
				array2[pos] = 0.0;
				
			}
			pos++;
		}
		
		return JensenShanonDivergenz.JSD(array1, array2);
		
	}


	private static int find(Integer[] integers, int i) {
		for(int j=0; j<integers.length; j++) {
			if(integers[j]==i) return j;
		}
		return -1;
	}

}
