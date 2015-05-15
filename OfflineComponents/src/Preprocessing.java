import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.mytest.dvita.server.ConfigRawdataServer;
import com.mytest.dvita.server.ConnectionManager;
import com.mytest.dvita.server.DVitaConfig;
import com.mytest.dvita.server.Stemmer;

public class Preprocessing {

	ConfigRawdataServer info = null;

	/* After the connection to DB, the words in the docs are splited by split() */
	
	String schema = DVitaConfig.getSchemaDot();	
	
	// TODOMD add these settings to dvita.config
	 int minCountProDoc = 2; // pro dok soll das wort mindestens so oft vorkommen

	// ArrayList<Integer> wordID2OverallCount = new ArrayList<Integer>();
	
	
	 ArrayList< HashMap<String,Integer> > wordID2RealWorldQuantityMap = new ArrayList<HashMap<String,Integer>>();	
	
	public Preprocessing(ConfigRawdataServer info2) {
		info = info2;
	}
	
	
	public static void main(String[] args) {
		try {
			
			//args = new String[1];
			 //args[0]= "1111";
			
			 if(args.length!=1) { System.out.println("please specify connectionID"); System.exit(-1); }
			
		ConfigRawdataServer info = new ConfigRawdataServer();
		Connection connection = ConnectionManager.getConnection();
		// eingabe: welchze datenbanik soll preprocessed werden
		
		Statement statement = connection.createStatement();
		
		 // eintrag in spalte ID aus Tabelle config_rawdata
		 int id = Integer.parseInt(args[0]);
		
		info = com.mytest.dvita.server.ConfigReader.readConfigRawdata(id, statement);
		
		
		Preprocessing d = new Preprocessing(info);
		d.run();
	
		
		
		} catch (Exception e) {
			
			
			e.printStackTrace();
		}
	}
	
	public void run() throws Exception {
		
		boolean overwriteTable = true; 
		// verbindung zur DB wo die ergebnisse gespeichert wreden
		Connection connection = ConnectionManager.getConnection();
		//verbindung zu DB2(In Connection->Rohedaten drin)
		Connection rawDataConnection = ConnectionManager.getRawDataConnection(info);
				
		Timer stemmingEtc = new Timer();
		Timer writeWords = new Timer();
		Timer writeRealWords = new Timer();

		// Set<String> set = new HashSet<String>();
		Map<String, Integer> mapID = new HashMap<String, Integer>();

		HashSet<Integer> savedWords = new HashSet<Integer>();

		
		int WortID = 0;
		
		
		Statement statement = connection.createStatement();
		// Create a table for saving Words
        if(overwriteTable) {
        	Tools.DropTableIfExists(statement,info.tablePrefix+"_WORDS",schema);
        }
		String sqltable = "CREATE TABLE "+schema+info.tablePrefix+"_WORDS(STEMNAME VARCHAR (225) NOT NULL, NAME VARCHAR (225), ID INTEGER NOT NULL, PRIMARY KEY (ID))";
		System.out.println(sqltable);
		statement.executeUpdate(sqltable);

		// Create a table for Contains:
		if(overwriteTable) {
			Tools.DropTableIfExists(statement,info.tablePrefix+"_CONTAINS",schema);	
		}
		sqltable = "CREATE TABLE "+schema+info.tablePrefix+"_CONTAINS(WORDID INTEGER NOT NULL, DOCID INTEGER NOT NULL, QUANTITY INTEGER NOT NULL, PRIMARY KEY(WORDID, DOCID)) ";
		System.out.println(sqltable);
		statement.executeUpdate(sqltable);
		
		// weitere indexe (neben den primary key index) erzeugen
		sqltable = "CREATE INDEX "+schema+info.tablePrefix+"_A ON "+schema+info.tablePrefix+"_CONTAINS (WORDID)";
		statement.executeUpdate(sqltable);
		sqltable = "CREATE INDEX "+schema+info.tablePrefix+"_B ON "+schema+info.tablePrefix+"_CONTAINS (DOCID)";
		statement.executeUpdate(sqltable);
		
		

		HashSet<String> stopWrds = readStopwords(statement);
		
		//HashMap<String,Integer> bigramToCount = new HashMap<String,Integer>();
		//String oldTerm = "";


		//String Query = "Select ID, Text FROM rawdata";
		String where = "";
		if(info.whereClause != null && info.whereClause.length() > 0) { 
			where = " WHERE " + info.whereClause; 
		}
		String Query = "SELECT "+info.columnNameID+" AS ID, "+info.columnNameContent+" AS Text, "+info.columnNameTitle+" AS Titel FROM "+info.fromClause+where;
		System.out.println("Raw Data Query: " + Query);
		Statement statement2 = rawDataConnection.createStatement();
		ResultSet sql = statement2.executeQuery(Query);
		//gehe durch alle dokumente:
		while (sql.next()) {

			int ID = sql.getInt("ID");
			String Text = sql.getString("Text");
			if(Text == null) continue;
			String t = Text.toLowerCase();
			
			// nutze text und titel wird wort extraction
			String Titel = sql.getString("Titel");
			if(Titel != null) {
				t+= " "+Titel.toLowerCase();
			}
			
			stemmingEtc.start();
			
			// preprocessing : delete all punctuation mark from the text like
			// .,"!?... and replace the with a blank
			t = t.replaceAll("[^a-z]", " ");

			String[] individualWords = t.split(Pattern.quote(" "));
			
			Map<String, Integer> mapQuantity = new HashMap<String, Integer>();

			
			Stemmer s = new Stemmer();

			// for-loops to remove stopwords from the text

			for (int i = 0; i < individualWords.length; i++) {
				String currentWord = individualWords[i];

				// delete words from docs with a length shorter than 2

				if (individualWords[i].length() <= 2) {
					//oldTerm = ""; // für bigrams wichtig!!!
					continue;
				}

				
				if(stopWrds.contains(individualWords[i])) {
					//oldTerm = ""; // für bigrams wichtig!!!
					continue;
				}
		
				
				// hallo katze hund the maus hund
				// oldterm="";
				// oldterm="hallo";
				//->biram hallo katze
				// oldterm = "katze"
				// -> bigram katze hund

				// Words in text that are saved in "stemword" :

					char[] word = currentWord.toCharArray();
					s.add(word, word.length);
					s.stem();

					String stemword = s.toString();
					// set.add(stemword);

					
					// Quantity of words in each Docs, calculated by
					// mapQuantity:

					//Bilde Wort auf eine Zahl ab durch Hash mapQuantity:
					if (mapQuantity.get(stemword) == null) {
						mapQuantity.put(stemword, 1);
					}

					else {
						int Quantity = mapQuantity.get(stemword);
						Quantity++;
						mapQuantity.put(stemword, Quantity);

					}

					// Refers a WordID to each new word that is recognized in a
					// doc
					Integer stemwordID = mapID.get(stemword);
					if (stemwordID == null) {
						mapID.put(stemword, WortID);
						stemwordID = WortID;
						WortID += 1;
						
						wordID2RealWorldQuantityMap.add(new HashMap<String,Integer>());
		
					}
					
					if(mapQuantity.get(stemword)==minCountProDoc) {
						if(!savedWords.contains(stemwordID)) {
							String sqlinsert = "INSERT INTO "+schema+info.tablePrefix+"_WORDS(STEMNAME, ID ) VALUES('"
									+ stemword + "'," + stemwordID + ")";
							
							//System.out.println(sqlinsert);
							//int resultinsert = statement.executeUpdate(sqlinsert);
							statement.addBatch(sqlinsert);
							
							savedWords.add(stemwordID);
						}
						
					}
					
					
					//In  tabelle Word bis jetzt :  nur gestemmte wort, nun das original wort: 
					//wörter im dokument: computer, computer, copmute, computers
					// alle werden gestemt zu zu compt
					// compt -> wordID ist 3
					// wordID2RealWorldQuantityMap[3] ist eine Map die abbildet
					//	computer -> 2x
					// compute -> 1x
					// computers -> 1x
					
					
					HashMap<String,Integer> map = wordID2RealWorldQuantityMap.get(stemwordID);
					Integer c = map.get(currentWord);
					if(c==null) {
						map.put(currentWord,1);
					} else {
						map.put(currentWord,c+1);
					}
					

			}
			
			
			stemmingEtc.pause();
			writeWords.start();
			
			
			statement.executeBatch(); // die wörter die oben gesammelt wurden als batch
			// in die datenbank schreiben

			System.out.println("Processing document with ID " + ID);

			Set<String> Words = mapQuantity.keySet();
			
			
			insertWords(Words,mapQuantity,statement,ID,mapID);
			
			
			writeWords.pause();

		}
		
	
		
		writeRealWords.start();
		
		// schreibe real world representatives in die datei
		writeRealWorldRepresentatives(statement,wordID2RealWorldQuantityMap.size());
		
		writeRealWords.pause();
		
		
		long a = stemmingEtc.getTime();
		long b = writeWords.getTime();
		long c = writeRealWords.getTime();
		
		System.out.println("Stemming " + a + " ms");
		System.out.println("Writing word stems " + b + " ms");
		System.out.println("Writing full words " + c + " ms");

		sql.close();
		statement.close();
		statement2.close();
		connection.close();
		if(!info.dataOnHostServer) rawDataConnection.close();

	}

	private  HashSet<String> readStopwords(Statement statement) {
		
		HashSet<String> stopwords = new HashSet<String>();
		try {
			String Query = "Select Name FROM "+schema+"stopwords";
			ResultSet sql = statement.executeQuery(Query);
			while(sql.next()) {
				stopwords.add(sql.getString("Name"));
			}
		} catch (SQLException e) {
			
			e.printStackTrace();
		}
		
		return stopwords;
	}

	private  void writeRealWorldRepresentatives(Statement statement, int size) {
		
		System.out.println("Storing all word variants");
		
		try {
			
			for(int i=0; i<size; i++) {
				
				
				//wörter im dokument: computer, computer, copmute, computers
				// alle werden gestemt zu zu compt
				// compt -> wordID ist 3
				// wordID2RealWorldQuantityMap.get(3) ist eine Map die abbildet
				//	computer -> 2x
				// compute -> 1x
				// computers -> 1x
			
				
				HashMap<String, Integer> map = wordID2RealWorldQuantityMap.get(i);
				String real = "";
				Integer best = -1;
				for(String key : map.keySet()) {
					if(map.get(key)>best) {
						best = map.get(key);
						real = key;
					}
				}
				
				//  nun steht in real="copmuter" und in best=2
			
			String query = "UPDATE "+schema+info.tablePrefix+"_WORDS SET Name = '"+real+"' WHERE ID=" + i;
			
			
				statement.addBatch(query);
				
				if(i%10000==0) {
					statement.executeBatch();
				}
				
			}
			statement.executeBatch();
		} catch (SQLException e) {
			
			e.printStackTrace();
			
			Exception e2 = e.getNextException();
			while(e2 != null) {
				e2.printStackTrace();
				e2 = e.getNextException();	
			}
		}
		
	}
	
	/**
	 * Die Wörter+ gestemmte Wörter mit entsprechnde IDs in die Tabelle _Contain einfügen
	 * @param words words sind alle gestemmten wörter die in Document x vorkommen
	 * @param mapQuantity mapQuantity bildet gestemmtes wort auf aunzahl ab
	 * @param statement nötig für dei Ausführung der SQL Anfrage
	 * @param ID ist die DocID
	 * @param mapID  wort auf Id abbilden
	 * @throws SQLException
	 */
	private  void insertWords(Set<String> words,
			Map<String, Integer> mapQuantity, Statement statement, int ID, Map<String, Integer> mapID) {

		
		if(words.isEmpty()) return;
		
		String sqlcontain = "INSERT INTO "+schema+info.tablePrefix+"_CONTAINS(WORDID,DOCID, QUANTITY) VALUES";
		boolean first = true;
		boolean atLeastOne = false;
		
		for (String word : words) {
			int Quantity = mapQuantity.get(word);
			
			if(Quantity < minCountProDoc) { continue; }
			
			
			atLeastOne = true;
			int WID = mapID.get(word);
			//wordID2OverallCount.set(WID,wordID2OverallCount.get(WID)+Quantity); // overall count hochzählen
			
			
			if(!first) { sqlcontain += ", ("+ WID + "," + ID + "," + Quantity + ")"; }
			else { first = false; sqlcontain += " ("+ WID + "," + ID + "," + Quantity + ")"; }
		}
		
		if(!atLeastOne) return;

		
			try {
				statement.executeUpdate(sqlcontain);
			} catch (SQLException e) {
				
				e.printStackTrace();
				
				Exception e2 = e.getNextException();
				while(e2 != null) {
					e2.printStackTrace();
					e2 = e.getNextException();	
				}
			}
		
		
	}
	
	
	// WAR NUR ZUM TEST
	class ValueComparator implements Comparator<String> {

	    Map<String, Integer> base;
	    public ValueComparator(HashMap<String, Integer> bigramToCount) {
	        this.base = bigramToCount;
	    }

	    // Note: this comparator imposes orderings that are inconsistent with equals.    
	    public int compare(String a, String b) {
	        if (base.get(a) >= base.get(b)) {
	            return -1;
	        } else {
	            return 1;
	        } // returning 0 would merge keys
	    }
	}

}
