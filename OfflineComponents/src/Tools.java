import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.mytest.dvita.server.ConnectionManager;
import com.mytest.dvita.server.DVitaConfig;


public class Tools {
	
	public static Double[] getCurrent(int topic, Statement statement, String tablePrefix, int timesteps) throws SQLException {
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

	
	public static void DropTableIfExists(Statement statement, String table, String schema) throws SQLException {
    	
		if(schema.trim() != "" && !schema.endsWith("."))
			schema = schema + ".";
		
		if(ConnectionManager.getDbType() == 1){
			// mysql
			System.out.println("DROP TABLE IF EXISTS "+ schema+table);
			String sqltableclose = "DROP TABLE IF EXISTS "+ schema+table;	
			statement.executeUpdate(sqltableclose);
			return;
		}
		// mysql
		//String sqltableclose = "DROP TABLE IF EXISTS "+info.tablePrefix+"_WORDS";	
		//statement.executeUpdate(sqltableclose);
		
		// db2
		if(ConnectionManager.getDbType() == 2){
		//ResultSet res;
		try {
			System.out.println("SELECT 1 FROM "+schema+table);
			statement.executeQuery("SELECT 1 FROM "+schema+table);
		} catch (SQLException e) {
			//System.out.println(e.getSQLState());
			if(e.getSQLState().equals("42704") || e.getSQLState().equals("42883")) {
				// tabelle nicht vorhanden, alles ok
				return;
			}
			throw e;
		}
		
		//System.out.println("hoer");
		// tablle vorhanden, also löschen
		statement.executeUpdate("DROP TABLE " + schema+table);
		
		}
	}
		

	public static void copyFolder(File src, File dest)
		throws IOException{
	
		if(src.isDirectory()){
	
			//if directory not exists, create it
			if(!dest.exists()){
			   dest.mkdir();
			   //System.out.println("Directory copied from " 
	             //             + src + "  to " + dest);
			}
	
			//list all the directory contents
			String files[] = src.list();
	
			for (String file : files) {
			   //construct the src and dest file structure
			   File srcFile = new File(src, file);
			   File destFile = new File(dest, file);
			   //recursive copy
			   copyFolder(srcFile,destFile);
			}
	
		}else{
			//if file, then copy it
			//Use bytes stream to support all file types
			InputStream in = new FileInputStream(src);
		        OutputStream out = new FileOutputStream(dest); 
	
		        byte[] buffer = new byte[1024];
	
		        int length;
		        //copy the file content in bytes 
		        while ((length = in.read(buffer)) > 0){
		    	   out.write(buffer, 0, length);
		        }
	
		        in.close();
		        out.close();
		        //System.out.println("File copied from " + src + " to " + dest);
		}
	}
}
