import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import com.mytest.dvita.server.ConfigRawdataServer;
import com.mytest.dvita.server.ConfigTopicminingServer;
import com.mytest.dvita.server.ConnectionManager;
import com.mytest.dvita.server.DVitaConfig;
import com.mytest.dvita.shared.ConfigTopicminingShared.Granularity;


public class DynamicLDA {
	
	int sequenceMinIter = 100; // min und max iterationen
	int sequenceMaxIter  = 1000; // für den dynamic LDA
	int maxEMIter = 100; // iterationen für den initialen (static LDA)
	
	boolean linux = false;
	
// default runpath wenn unten nichts anderes angegeben
	public String runPath = System.getProperty("user.dir") + "\\DLDA";


	// ignoriere ein Topic für ein bestimmtes Dokument wenn dessen Anteil zu klein ist
	double minTopicProportion = 0.01; 

	// ignoreire ein Wort für ein bestimmtes Topic wenn dessen Anteil zu klein ist
	// (es ist relativ gesehen bzgl. des "besten" wortes aus dem topic)
	// d.h. wenn das beste wort von topic x eine probability von 0.3 hat, dann werden
	// z.b. alle woerter mit probability kleiner 0.003 entfernt (für dieses topic)
	double minRelativeWordProbability = 0.01; 

	// nur wörter mit hinreichend hoher gesamtzahl in datenbank berücksichtigt werden(von blei)
	int minWordCountOverall = 25; // blei hatte 25

	//  nur dokumente mit hinreichend hoher wortzahl betrachten
	// (anzahl wörter im dokument mindestens)
	int minWordsPerDocument = 10;
	
	
	ConfigTopicminingServer info2;
	ConfigRawdataServer info;

	// speichert alle frequent words bzw deren ID (vorder einträge)
	// und bildet diese auf die neuen IDs ab für das topic mining (zweite einträge)
	// dadurch sind beim topic mining nur aufsteigende IDs vorhanden und keine fehldenen einträge
	private HashMap<Integer, Integer> frequentWordsMap;
	// bildet die WordIDs von LDA auf die realen WordIDs ab
	// da die WordIDs von LDA aufsteigend sind kann man einfach array nehmen
	ArrayList<Integer> inverseFrequentWordsMap = new ArrayList<Integer>();


	private boolean overwriteTable;
	private ArrayList<Integer> fileDocID2DatabaseDocID = new ArrayList<Integer>();
	ArrayList<Integer> fileDocID2DatabaseIntervalID = new ArrayList<Integer>();


	public DynamicLDA(ConfigRawdataServer info3, ConfigTopicminingServer info22) {
		info = info3;
		info2 = info22;
	}

	void writeTopicProportions(double[] probs, int DOCID, int intervalID, Statement statement) throws SQLException {

		int nrTopics = probs.length;
		for(int t=0; t<nrTopics; t++) {

			if(probs[t]<this.minTopicProportion) continue;

			String sqlinsert = "INSERT INTO "+DVitaConfig.getSchemaDot()+info2.tablePrefix+"_BELONGTO(TOPICPROPORTION, DOCID, TOPICID, INTERVALID ) VALUES("
					+probs[t]+	"," + DOCID + "," + t + "," + intervalID + ")";
			statement.addBatch(sqlinsert);

			//System.out.print(probs[t] + "\t");

		}

		statement.executeBatch();

	}

	void writeWordProb(double[][] probOfwordXAtTimeY, int TopicID, Statement statement) throws SQLException 
	{

		String head = "INSERT INTO "+DVitaConfig.getSchemaDot()+info2.tablePrefix+"_DESCRIBEDBY (PROBABILITY, WORDID, TOPICID, INTERVALID) VALUES";
		String values = "";

		// speichere immer 200 wörter gleichzeitig
		// damit man nicht für jedes einzelen wort die datenabnk verbinden muss
		int count = 0;
		int max = 200;


		// BESTIMME WIE OFT EIN WORT MINDESTENS AUFTRETEN MUSS (IN DIESEM TOPIC)
		// DAMIT ES SPÄTER GESPEICHERT WIRD

		// bestimme zu jeden zeitpunkt das "beste" wort bzw. dessen probability
		double[] highestProbAtTimeY = new double[probOfwordXAtTimeY[0].length];
		for(int wordID=0; wordID<probOfwordXAtTimeY.length; wordID++) {
			for(int intervalID=0; intervalID<probOfwordXAtTimeY[0].length; intervalID++) {
				if(probOfwordXAtTimeY[wordID][intervalID]>highestProbAtTimeY[intervalID]) {
					highestProbAtTimeY[intervalID] = probOfwordXAtTimeY[wordID][intervalID];
				}
			}
		}


		// ENDE DER BESTIMMUNG


		for(int wordID=0; wordID<probOfwordXAtTimeY.length; wordID++) {
			for(int intervalID=0; intervalID<probOfwordXAtTimeY[0].length; intervalID++) {

				if(probOfwordXAtTimeY[wordID][intervalID]<this.minRelativeWordProbability*highestProbAtTimeY[intervalID]) continue;

				if(count == 0) {
					values += " (" +(probOfwordXAtTimeY[wordID][intervalID])+ "," + inverseFrequentWordsMap.get(wordID) + "," +TopicID+ "," + intervalID + ")";

				} else {
					values += ", (" +(probOfwordXAtTimeY[wordID][intervalID])+ "," + inverseFrequentWordsMap.get(wordID) + "," +TopicID+ "," + intervalID + ")";

				}

				count++;

				if(count == max) {
					//System.out.println(head+values);
					statement.executeUpdate(head+values);
					count = 0;
					values = "";
				}
			}
		}

		if(count > 0) {
			statement.executeUpdate(head+values);
		}

	}


	// schreibe die Informationen über die Dokumente (d.h. welche Wörter treten wie oft im jweiligen
	// Dokument auf) in die dateian data-mult.dat und data-seq.dat
	//
	// zusätzlich werden auch die Maps für die WordsIDs, DokumentIDs und IntervalIDs weggeschrieben
	int writeDocuments(Connection connection, File workingPath, String tmpData, String tmpDirectory) throws Exception {
		// sqlquery die den minimalen und maximalen monat/jahr in der Datenbank ausgibt
		//String sqlquerydate = "SELECT MONTH(min) as minMonth, YEAR(min) as minYear, MONTH(max) as maxMonth, YEAR(max) as maxYear FROM (SELECT MIN(Date) as min, MAX(Date) as max FROM `rawdata`) newTable";
		// sie vergibt den Intervallen eine ID und ein startDate (inklusive!!!) und endDate (exklusive)
		// anhand dieser Intervallen werden nacher die jeweiligen Dokumente des Zeitstempels ausgewählt!!



		fileDocID2DatabaseDocID = new ArrayList<Integer>();
		fileDocID2DatabaseIntervalID.clear();

		Log docFile = new Log(workingPath.getAbsolutePath()+"/"+tmpData+"-mult.dat",true,false,true);

		LinkedList<Integer> docsPerTimeStamp = new LinkedList<Integer>();

		System.out.println("Obtaining time slices.");

		String sqlIntervalRange = "SELECT ID, intervalStart, intervalEnd FROM "+DVitaConfig.getSchemaDot()+info2.tablePrefix+"_TOPICINTERVALS ORDER BY intervalStart ASC";
		Statement statement3 = connection.createStatement();
		ResultSet sql3 = statement3.executeQuery(sqlIntervalRange);

		System.out.println("Creating temporary document IDs table.");		
		Statement tempTable = connection.createStatement();				
		Tools.DropTableIfExists(tempTable, info2.tablePrefix + "_TEMPIDS", DVitaConfig.getSchema());
		tempTable.executeUpdate("CREATE TABLE " + DVitaConfig.getSchemaDot() + info2.tablePrefix + "_TEMPIDS(ID INTEGER NOT NULL, PRIMARY KEY (ID))");
		tempTable.close();		
		
		while(sql3.next()) {
			int intervalID = sql3.getInt("ID");
			String intervalStart = sql3.getString("intervalStart");
			String intervalEnd = sql3.getString("intervalEnd");
			
			System.out.println("Processing interval " + intervalID);

			// hole nur die DokumentIDs die SOWOHL in contains sind ALS AUCH im gültigen Intervallbereich
			String where = "";
			if(info.whereClause != null && info.whereClause.length()>0) {
				where = info.whereClause + " AND ";
			}
			
			// FIXED! this causes a SQL string too long/complex exception if we go beyond ~2000 words per time slice.
			// therefore during preprocessing we need to add an additional table x_docinfos(docid,date) with 
			// mapping from doc to date

			// bestimme documentIDS die im zeitintervall-bereich liegen
			String sqlquery= "Select DISTINCT DOCID FROM "+DVitaConfig.getSchemaDot()+info.tablePrefix+"_CONTAINS WHERE DOCID in (Select "+info.columnNameID+" as DOCID FROM "+info.fromClause+" WHERE "+where+""+info.columnNameDate+">='"+ intervalStart+"' AND "+info.columnNameDate+"<'" + intervalEnd+"')";
			boolean noDocs = false;
			if(!info.dataOnHostServer) {
				noDocs = true;
				// dann kann die kombinierte Anfrage oben NICHT ausgeführt werden
				Connection rawDataConnection = ConnectionManager.getRawDataConnection(info);
				String subquery = "Select "+info.columnNameID+" as DOCID FROM "+info.fromClause+" WHERE "+where+""+info.columnNameDate+">='"+ intervalStart+"' AND "+info.columnNameDate+"<'" + intervalEnd+"'";
				Statement state = rawDataConnection.createStatement();
				ResultSet sqlIds = state.executeQuery(subquery);				
				
				Statement tempIds = connection.createStatement();
				tempIds.executeUpdate("DELETE FROM " + DVitaConfig.getSchemaDot() + info2.tablePrefix + "_TEMPIDS");
				while(sqlIds.next()) {
					noDocs = false;
					tempIds.executeUpdate("INSERT INTO " + DVitaConfig.getSchemaDot() + info2.tablePrefix + "_TEMPIDS VALUES (" + sqlIds.getInt("DOCID") + ")");
				}				
				
				sqlIds.close();
				rawDataConnection.close();
				
				sqlquery = "SELECT DISTINCT DOCID "
						+ "FROM " + DVitaConfig.getSchemaDot() + info.tablePrefix + "_CONTAINS " 
						+ "WHERE DOCID IN (SELECT ID FROM " + DVitaConfig.getSchemaDot() + info2.tablePrefix + "_TEMPIDS)";
			}

			int docsInThisTimestamp = 0;

			if(!noDocs) {
				Statement statement = connection.createStatement();
				ResultSet sql = statement.executeQuery(sqlquery);
				Statement statement2 = connection.createStatement();
				
				// gehe durch alle dokumente (aus diesem Zeitintervall)
				while(sql.next()){

					int DOCID= sql.getInt("DOCID");
					// bestimme wörter (und deren anzhal) für das aktuelle dokument
					String sqlquery1= "SELECT QUANTITY,WORDID FROM "+DVitaConfig.getSchemaDot()+info.tablePrefix+"_CONTAINS WHERE DOCID = " + DOCID + " ORDER BY WORDID ASC";
					ResultSet resultsql = statement2.executeQuery(sqlquery1);

					String output = "";
					int distinctWordsPerDoc = 0;
					int overallWordsInDoc = 0;
					while(resultsql.next()) { // gehe durch alle wörter
						int wordid = resultsql.getInt("WORDID");
						if(this.frequentWordsMap.get(wordid)==null) { 
							// kein frequent word!! nächstes nehmen!
							continue;
						}
						int quantity = resultsql.getInt("QUANTITY");
						// seineWordID:quantity
						output += " " + frequentWordsMap.get(wordid)+ ":" + quantity;
						distinctWordsPerDoc++;
						overallWordsInDoc += quantity;
					}

					if(overallWordsInDoc<this.minWordsPerDocument) continue;

					//DOCIDS.add(DOCID);
					docsInThisTimestamp++;
					// write to file
					// distinctWordsPerDoc + output + "\n"
					// schreieb die entsprechende Zeile in die Datei
					docFile.log(distinctWordsPerDoc + output + "\n");
					fileDocID2DatabaseDocID.add(DOCID);
					fileDocID2DatabaseIntervalID.add(intervalID);
				}
			}
			
			System.out.println("  " + intervalStart+" - "+intervalEnd);
			System.out.println("  " + docsInThisTimestamp + "documents");
			if(docsInThisTimestamp==0){
				System.out.println("  NO DOCUMENTS IN THIS TIME SLICE! NEED TO EXIT, SORRY");
				System.exit(-1);
			}
			docsPerTimeStamp.add(docsInThisTimestamp);
			
			//System.exit(-1);
		}

		System.out.println("Dropping temporary document IDs table");
		tempTable = connection.createStatement();				
		Tools.DropTableIfExists(tempTable, info2.tablePrefix + "_TEMPIDS", DVitaConfig.getSchema());
		tempTable.close();

		docFile.close();

		// schreibe die Anzahl Dokumente per Timestamp in die Sequence Datei
		int nrTimesteps = docsPerTimeStamp.size();

		Log countFile = new Log(workingPath.getAbsolutePath()+"/"+tmpData+"-seq.dat",true,true,true);
		countFile.log(nrTimesteps + "\n");

		for(Integer count : docsPerTimeStamp) {
			countFile.log(count + "\n");
		}
		countFile.close();
		
		
		// ******** die InverseWOrdID speichern
		Log wordIDFile = new Log(workingPath.getAbsolutePath()+"/"+tmpDirectory+"/wordIDs.dat",true,false,true);

		// [Zur Kontrolle das nicht alles durcheinandergeht wird in die erste Zeile
		// die TopicMining ID geschrieben und in die zweite Zeile die Datebase ID]
		wordIDFile.log(info2.id + "\n");
		wordIDFile.log(info2.rawdataID + "\n");
		
		// nun kommen die eigentliche WortIDs
		for(int i=0; i<this.inverseFrequentWordsMap.size(); i++) {
			// seine ID (sind aufsteigened) und unsere ID
			wordIDFile.log(i + " " + inverseFrequentWordsMap.get(i) + "\n");
		}
		wordIDFile.close();

		// ********* die dokumentIDs speichern
		// (auch gleichzeitig deren interval ID)
		Log docIDFile = new Log(workingPath.getAbsolutePath()+"/"+tmpDirectory+"/docIDs.dat",true,false,true);
		
		
		// [Zur Kontrolle das nicht alles durcheinandergeht wird in die erste Zeile
		// die TopicMining ID geschrieben und in die zweite Zeile die Datebase ID]
		docIDFile.log(info2.id + "\n");
		docIDFile.log(info2.rawdataID + "\n");
		
		// nun die nrTimesteps
		docIDFile.log(nrTimesteps + "\n");
		
		// nun die eigentlichen DokIDs und Intervalle
		for(int i=0; i<fileDocID2DatabaseDocID.size(); i++) {
			// seine docID (sind aufsteigened) und unsere docID + unsere intervalID
			docIDFile.log(i + " " + fileDocID2DatabaseDocID.get(i) + " " + fileDocID2DatabaseIntervalID.get(i) + "\n");
		}
		docIDFile.close();

		
		

		return nrTimesteps;

	}

	void resumeLDA(String tmpDirectory, boolean ignoreID) throws SQLException {
		
		Connection connection = ConnectionManager.getConnection();
		Statement statement = connection.createStatement();
		
		if(!loadWordIDsFile(tmpDirectory,ignoreID)) {
			System.out.println("ERROR! Cannot load Word IDs file.");
			System.exit(-1);
		}
		int nrTimesteps = loadDocIDsFile(tmpDirectory,ignoreID);
		
		if(nrTimesteps == -1) {
			System.out.println("ERROR! Cannot obtain documents and time slices.");
			System.exit(-1);
		}
		
		int nrWords = inverseFrequentWordsMap.size();	
		
		long runtimePostLDA = runPostLDA(tmpDirectory,statement,nrTimesteps,nrWords);
		System.out.println("Runtime post LDA = " + runtimePostLDA);
	}


	private int loadDocIDsFile(String tmpDirectory, boolean ignoreID) {
		
		int nrTimesteps = -1;
		
		try {


			// Open the file
			FileInputStream fstream = new FileInputStream(runPath + "/" + tmpDirectory + "/docIDs.dat");
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));

			String firstLine = br.readLine();
			// muss die topicMining ID sein
			if(Integer.parseInt(firstLine) != info2.id) {
				System.out.println("ATTENTION: TopicMiningID is different!");
				if(!ignoreID)System.exit(-1);
			
				// kopiere dann die interval tabelle
				{
				Connection connection = ConnectionManager.getConnection();
				Statement s = connection.createStatement();
				ConfigTopicminingServer res = com.mytest.dvita.server.ConfigReader.readConfigTopicmining(Integer.parseInt(firstLine), s);
				
				if(overwriteTable) {
					Tools.DropTableIfExists(s, res.tablePrefix+"_TOPICINTERVALS", DVitaConfig.getSchema());
				}

				
				
				//String sqltable = "CREATE TABLE "+schema+res.tablePrefix+"_TOPICINTERVALS(ID INTEGER NOT NULL, intervalStart TIMESTAMP NOT NULL, intervalEnd TIMESTAMP NOT NULL, PRIMARY KEY(ID))";	
				//s.executeUpdate(sqltable);

				String sqltable = "CREATE TABLE "+DVitaConfig.getSchemaDot()+res.tablePrefix+"_TOPICINTERVALS AS (SELECT ID, intervalStart, intervalEnd FROM "+DVitaConfig.getSchemaDot()+res.tablePrefix+"_TOPICINTERVALS);";	
				s.executeUpdate(sqltable);
				
				s.close();
				connection.close();
				}
			}

			String secondLine = br.readLine();
			// muss die database ID sein
			if(Integer.parseInt(secondLine) != info2.rawdataID) {
				System.out.println("ERROR 2");
				System.exit(-1);
			}
			
			String thirdLine = br.readLine();
			// ist die Anzahl der TimeSteps
			nrTimesteps = Integer.parseInt(thirdLine);

			// wenn alles korrekt lese die DocIDs und Interval IDs ein
			this.fileDocID2DatabaseDocID.clear();
			this.fileDocID2DatabaseIntervalID.clear();
			int count = 0;
			String strLine;
			strLine = br.readLine();
			while(strLine != null) {
				String[] pair = strLine.split(" ");
				if(Integer.parseInt(pair[0]) != count) {
					// IDs von LDA sind aufsteigend
					System.out.println("ERROR 3");
					System.exit(-1);
				}
				
				fileDocID2DatabaseDocID.add(Integer.parseInt(pair[1]));
				fileDocID2DatabaseIntervalID.add(Integer.parseInt(pair[2]));
				
				count++;
				strLine = br.readLine();
			}
			

			//Close the input stream
			in.close();
		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
			return -1;
		}

		return nrTimesteps;
		
	}

	private boolean loadWordIDsFile(String tmpDirectory, boolean ignoreID) {

		try {



			// Open the file
			FileInputStream fstream = new FileInputStream(runPath + "/" + tmpDirectory + "/wordIDs.dat");
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));

			String firstLine = br.readLine();
			// muss die topicMining ID sein
			if(Integer.parseInt(firstLine) != info2.id) {
				System.out.println("ATTENTION: TopicMINING ID is different!");
				if(!ignoreID) System.exit(-1);
			}

			String secondLine = br.readLine();
			// muss die database ID sein
			if(Integer.parseInt(secondLine) != info2.rawdataID) {
				System.out.println("ERROR 2");
				System.exit(-1);
			}

			// wenn alles korrekt lese die WordIDs ein
			this.inverseFrequentWordsMap.clear();
			int count = 0;
			String strLine;
			strLine = br.readLine();
			while(strLine != null) {
				String[] pair = strLine.split(" ");
				if(Integer.parseInt(pair[0]) != count) {
					// IDs von LDA sind aufsteigend
					System.out.println("ERROR 3");
					System.exit(-1);
				}
				
				inverseFrequentWordsMap.add(Integer.parseInt(pair[1]));
				
				count++;
				strLine = br.readLine();
			}
			

			//Close the input stream
			in.close();
		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
			return false;
		}
		
		return true;
		
		
	}

	void readResult(String args) {

				int nrTopics = info2.NumberTopics;


		try {
			
			Timer lda = new Timer();
			Timer preLDA = new Timer();
			
			Connection connection = ConnectionManager.getConnection();
			Statement statement = connection.createStatement();
			createTopicIntervalTable(statement);
			
			preLDA.start();			

			Path runpathp = FileSystems.getDefault().getPath(runPath);
			// Pfad wo die Ergebnisse gespeichert werden
			Path p2 = java.nio.file.Files.createTempDirectory(runpathp,"LDA");
			System.out.println("LDA folder " + p2.getFileName());
			File workingPath = new File(runPath);
			String tmpData = p2.getFileName()+"/data";
			String tmpDirectory = p2.getFileName().toString();

			
			determineFrequentWords(connection);
			int nrWords = inverseFrequentWordsMap.size();	
			int nrTimesteps = writeDocuments(connection,workingPath,tmpData,tmpDirectory);
			fileDocID2DatabaseDocID.size();
			
			connection.close();

			preLDA.pause();
			
			int curr = 0;
			
			// moeglichkeit zum zwischenspeichern der ergebnisse nach weniger itereationen
			HashSet<Integer> listeItertations = new HashSet<Integer>();
			//listeItertations.add(50);
	
			
//			System.exit(-1);
			
			try {
				String line;
				//System.out.println(runCommend);
				
				lda.start();
				
				Process p = null;
				System.out.println("Starting LDA:");
				if(linux) {
					String runCommend = runPath + "/main -ntopics " + nrTopics + " -mode fit -rng_seed 0 -initialize_lda true -corpus_prefix " + tmpData + " -outname "+p2.getFileName()+" -top_chain_var 0.005 -alpha 0.01 -lda_sequence_min_iter "+this.sequenceMinIter+" -lda_sequence_max_iter "+this.sequenceMaxIter+" -lda_max_em_iter "+this.maxEMIter;					
					System.out.println(runCommend);
					// export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:~/usr/lib
					p = Runtime.getRuntime().exec(runCommend,null,workingPath);			
				} else {
					// starte den Topic Mining Algroithms (den C++ Code von Blei)
					String runCommend = runPath + "\\main.exe -ntopics " + nrTopics + " -mode fit -rng_seed 0 -initialize_lda true -corpus_prefix " + tmpData + " -outname "+p2.getFileName()+" -top_chain_var 0.005 -alpha 0.01 -lda_sequence_min_iter "+this.sequenceMinIter+" -lda_sequence_max_iter "+this.sequenceMaxIter+" -lda_max_em_iter "+this.maxEMIter;
					System.out.println(runCommend);
					p = Runtime.getRuntime().exec("cmd /c "+runCommend,null,workingPath);
				}
				

				BufferedReader bri = new BufferedReader
						(new InputStreamReader(p.getInputStream()));
				BufferedReader bre = new BufferedReader
						(new InputStreamReader(p.getErrorStream()));
				while ((line = bre.readLine()) != null) {
					//System.out.println(line);
					if(line.contains("EM iter")) {
						System.out.println("  Iteration " + curr);
						
						if(listeItertations.contains(curr)) {
							Tools.copyFolder(new File(runPath + "/"+ tmpDirectory),new File(runPath + "/"+ tmpDirectory+"-"+curr));
						}
						curr++;
						
					}
				}
				bre.close();
				while ((line = bri.readLine()) != null) {
					//System.out.println(line);
				}
				bri.close();


				p.waitFor();
				System.out.println("LDA Completed.");
				lda.pause();
			}
			catch (Exception err) {
				err.printStackTrace();
			}

			connection = ConnectionManager.getConnection();
			statement = connection.createStatement();

			long runtimePostLDA = runPostLDA(tmpDirectory,statement,nrTimesteps,nrWords);
			
			Log l = new Log("runtime"+args+".txt",true,true,true);
			l.log("Runtime in millisecs:\n");
			l.log("pre LDA " + preLDA.getTime() + "\n");
			l.log("LDA " + lda.getTime() + "\n");
			l.log("post LDA " + runtimePostLDA + "\n");
			l.close();
			
			connection.close();

			/*
			// lösche alle einträge aus dem temp verzeichnis
			// und danach das temp verzeichnis selbst
			DirectoryStream<Path> ds = Files.newDirectoryStream(p2);
			for (Path child : ds) {
			
				java.nio.file.Files.delete(child);
			}
			java.nio.file.Files.delete(p2);
			*/
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e1) {
			e1.printStackTrace();
		}



	}

	private long runPostLDA(String tmpDirectory, Statement statement, int nrTimesteps, int nrWords) throws SQLException {
		
		String path = runPath + "/"+tmpDirectory+"/";



		if(overwriteTable) {
			Tools.DropTableIfExists(statement, info2.tablePrefix+"_DESCRIBEDBY", DVitaConfig.getSchema());
			Tools.DropTableIfExists(statement, info2.tablePrefix+"_BELONGTO", DVitaConfig.getSchema());

		}

		// erstmal die Tabllen erstellen
		String sqltable = "CREATE TABLE "+DVitaConfig.getSchemaDot()+info2.tablePrefix+"_DESCRIBEDBY(PROBABILITY DOUBLE NOT NULL,  WORDID INTEGER NOT NULL, TOPICID INTEGER NOT NULL, INTERVALID INTEGER NOT NULL, PRIMARY KEY(WORDID, TOPICID, INTERVALID))";	
		statement.executeUpdate(sqltable);
		sqltable = "CREATE TABLE "+DVitaConfig.getSchemaDot()+info2.tablePrefix+"_BELONGTO(TOPICPROPORTION FLOAT NOT NULL, DOCID INTEGER NOT NULL, TOPICID INTEGER NOT NULL, INTERVALID INTEGER NOT NULL, PRIMARY KEY(DOCID, TOPICID) )";	
		statement.executeUpdate(sqltable);
		
		// weitere indexe (neben den primary key index) erzeugen
		sqltable = "CREATE INDEX "+DVitaConfig.getSchemaDot()+info2.tablePrefix+"_C ON "+DVitaConfig.getSchemaDot()+info2.tablePrefix+"_DESCRIBEDBY (TOPICID, INTERVALID)";
		statement.executeUpdate(sqltable);
		sqltable = "CREATE INDEX "+DVitaConfig.getSchemaDot()+info2.tablePrefix+"_D ON "+DVitaConfig.getSchemaDot()+info2.tablePrefix+"_BELONGTO (TOPICID, INTERVALID)";
		statement.executeUpdate(sqltable);
		
		// wenn linux dann ist der pfad noch einen tiefer
		if(linux) {
			path += "lda-seq/";
		}

		Timer postLDA = new Timer();
		postLDA.start();

		// zunächst für jedes Dokument die Topic Proportions auslesen
		// in gam steht für jedes dokument die topic porpotration
		String topicDistributionFile = path+"gam.dat";
		ArrayList<double[]> allTopicProportions = new ArrayList<double[]>();


		try {



			// Open the file that is the first 
			// command line parameter
			FileInputStream fstream = new FileInputStream(topicDistributionFile);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			
			int nrDocs = fileDocID2DatabaseDocID.size();

			// lies für jedes daokument sein topic porportions
			for(int i=0; i<nrDocs; i++) {
				int realDocID = fileDocID2DatabaseDocID.get(i);
				double[] topicProportions = new double[info2.NumberTopics];
				double sum = 0;
				for(int j=0; j<info2.NumberTopics; j++) {
					strLine = br.readLine(); // lies nächste zeile aus datei
					topicProportions[j] = Double.parseDouble(strLine); // unnormalisiert!!
					sum += topicProportions[j];
				} 
				for(int j=0; j<info2.NumberTopics; j++) {
					topicProportions[j] /= sum;
				} 
				//System.out.println(Arrays.toString(topicProportions));
				allTopicProportions.add(topicProportions);
				int interval = fileDocID2DatabaseIntervalID.get(i);
				writeTopicProportions(topicProportions,realDocID,interval,statement);
			}

			//Close the input stream
			in.close();
		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}


		// nun für jedes Topic seine Wortverteilungen

		for(int ttopic=0; ttopic<info2.NumberTopics; ttopic++) {
			DecimalFormat df = new DecimalFormat("000");
			// jede datei stellt ein topic  dar
			// in der datei ist zu jedem wort und jedem zeitpunkt die anzahl/wahrschenlihckeit dass es auftreitt
			String wordDistributionFile = path+"topic-" + df.format(ttopic) + "-var-e-log-prob.dat";

			double[][] probOfwordXAtTimeY = new double[nrWords][nrTimesteps]; // nur für dieses eine Topic!!!

			try{
				// Open the file that is the first 
				// command line parameter
				FileInputStream fstream = new FileInputStream(wordDistributionFile);
				// Get the object of DataInputStream
				DataInputStream in = new DataInputStream(fstream);
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				String strLine;

				// ACHTUNG: in der Datei wird für jedes wort zunächst über alle zeitpunkte gegangen
				for(int w=0; w<nrWords; w++) {

					for(int ti=0; ti<nrTimesteps; ti++) {
						strLine = br.readLine();
						probOfwordXAtTimeY[w][ti] = Math.exp(Double.parseDouble(strLine)); // es waren log werte gespeichert
					}

					//System.out.println(Arrays.toString(probOfwordXAtTimeY[w]));

				}

				writeWordProb(probOfwordXAtTimeY,ttopic,statement);


				//Close the input stream
				in.close();
			}catch (Exception e){//Catch exception if any
				System.err.println("Error: " + e.getMessage() + " bei file " + wordDistributionFile);
			}

		}
		
		postLDA.pause();
		
		return postLDA.stop();
		
	}

	public static Timestamp addOne(Timestamp date, Granularity g) {
		Timestamp calculatedDate = null;

		if (date != null) {
			final GregorianCalendar calendar = new GregorianCalendar();
			calendar.setTime(date);
			switch(g)  {
			case DAYLY: calendar.add(Calendar.DAY_OF_MONTH, 1); break;
			case WEEKLY: calendar.add(Calendar.WEEK_OF_YEAR,1); break;
			case MONTHLY: calendar.add(Calendar.MONTH, 1); break;
			case QUARTERYEAR: calendar.add(Calendar.MONTH, 3); break;
			case HALFYEAR: calendar.add(Calendar.MONTH, 6); break;
			case YEARLY: calendar.add(Calendar.YEAR, 1); break;
			case FIVEYEARS: calendar.add(Calendar.YEAR, 5); break;
			case DECADE: calendar.add(Calendar.YEAR, 1); break;	
			case BIYEARLY: calendar.add(Calendar.YEAR, 2); break;
			case THREEYEARS: calendar.add(Calendar.YEAR, 3); break;
			case FOURYEARS: calendar.add(Calendar.YEAR, 4); break;
			case CENTURY: calendar.add(Calendar.YEAR, 100); break;
			}
			calculatedDate = new Timestamp(calendar.getTime().getTime());
		}

		return calculatedDate;
	}




	private void createTopicIntervalTable(Statement statement) throws SQLException {
		System.out.println("Writing Topic Intervals. Granularity: " + info2.gran.toString());		

		if(overwriteTable) {
			Tools.DropTableIfExists(statement, info2.tablePrefix+"_TOPICINTERVALS", DVitaConfig.getSchema());
		}

		String sqltable = "CREATE TABLE "+DVitaConfig.getSchemaDot()+info2.tablePrefix+"_TOPICINTERVALS(ID INTEGER NOT NULL, intervalStart TIMESTAMP NOT NULL, intervalEnd TIMESTAMP NOT NULL, PRIMARY KEY(ID))";	
		statement.executeUpdate(sqltable);

		Timestamp old = null;
		Timestamp current = info2.rangeStart;

		int id = 0;
		while(current.before(info2.rangeEnd)) {

			if(old != null) {
				System.out.println(old + " - " + current);


				String sqlinsert = "INSERT INTO "+DVitaConfig.getSchemaDot()+info2.tablePrefix+"_TOPICINTERVALS(ID, intervalStart, intervalEnd) VALUES("
						+id+	",'" + old + "','" + current + "')";
				System.out.println(sqlinsert);
				//statement.executeUpdate(sqlinsert);
				statement.addBatch(sqlinsert);
				id++;


			}

			old = current;
			current = addOne(current,info2.gran);




		}


		String sqlinsert = "INSERT INTO "+DVitaConfig.getSchemaDot()+info2.tablePrefix+"_TOPICINTERVALS(ID, intervalStart, intervalEnd) VALUES("
				+id+	",'" + old + "','" + info2.rangeEnd + "')";
		statement.addBatch(sqlinsert);
		

		statement.executeBatch();


	}

	private void determineFrequentWords(Connection connection) {
		try {
			// hier sollte der range auch noch berücksichtigt werden
			String frequentQordQuery = "SELECT WORDID FROM "+DVitaConfig.getSchemaDot()+info.tablePrefix+"_CONTAINS GROUP BY WORDID HAVING sum(quantity)>="+this.minWordCountOverall;
			Statement statement3 = connection.createStatement();
			ResultSet sql3 = statement3.executeQuery(frequentQordQuery);

			frequentWordsMap = new HashMap<Integer,Integer>();
			inverseFrequentWordsMap = new ArrayList<Integer>();
			int newID = 0;
			while(sql3.next()) {
				// deine WordIDs -> seine WordIDs
				frequentWordsMap.put(sql3.getInt("WORDID"),newID);
				// seine WordID -> deine WordID
				inverseFrequentWordsMap.add(sql3.getInt("WORDID"));
				newID++;
			}


			

		} catch (SQLException e) {
			e.printStackTrace();
		}
		

	}



	/**
	 * @param args
	 */
	public static void main(String[] args) {


		/////////////////////////////////////////////////////////////////

		//aus den 3 tabellen alles eingelesen!!!
		///////////////////////////////////////////////////////////////

		ConfigRawdataServer info = new ConfigRawdataServer();
		ConfigTopicminingServer info2 = new ConfigTopicminingServer();
		Connection connection = ConnectionManager.getConnection();
		try{
			
			//args = new String[2];
			//args = new String[3]; // wenn 3, dann wird altes ergebnis eingelesen
			//args[0]= "1111";
			//args[1]= ""; // so wird es in ./DLDA ausgeführt
			
			// Auf der lokale Festplatte und nicht auf H:\ da zu langsam! 
			 //args[1]="C:/DLDA";
			 
			// wird nur genutzt, wenn 3 argumente angegeben
			 boolean ignoreID = false; // wenn true, dann kopiere einfach ein anderes Ergebnis ohne Sicherheitscheck (achtugn: sollte lieber false sein)
			//args[2] = "LDA4840017260647270770-2"; // LDA ergebnis auslesen (wenn z.b. vorher abgebrochen)

			
			 if(args.length!=2 && args.length!=3) { System.out.println("Please specify topicMining ID and DTM runpath"); System.exit(-1); }
		
			 
			Statement statement = connection.createStatement();
			
			// eintrag in spalte ID aus Tabelle config_topicmining
			int id = Integer.parseInt(args[0]);
			info2 = com.mytest.dvita.server.ConfigReader.readConfigTopicmining(id, statement);
			info = com.mytest.dvita.server.ConfigReader.readConfigRawdata(info2.rawdataID, statement);
			


			DynamicLDA d = new DynamicLDA(info,info2);
			if(d.linux) { 
				d.runPath = args[1];
			} else if(args[1]!="")  {
				System.out.println("Using user specified path to LDA binary");
				d.runPath = args[1];
			}
			
			d.overwriteTable = true;

			
			if(args.length==2) {
				// LDA von vorne beginnen
				d.readResult(args[0]);
			} else if(args.length==3) {
				// LDA aus den existierenden Datein laden
				d.resumeLDA(args[2],ignoreID);
			} 
		}
		
		catch (SQLException e) {
			e.printStackTrace();
		}
try {
	connection.close();
} catch (SQLException e) {
	e.printStackTrace();
}
	}

}
