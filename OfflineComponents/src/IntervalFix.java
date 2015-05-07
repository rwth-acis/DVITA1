import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.mytest.dvita.server.ConfigRawdataServer;
import com.mytest.dvita.server.ConfigTopicminingServer;
import com.mytest.dvita.server.ConnectionManager;
import com.mytest.dvita.server.DVitaConfig;


// behebt nachträglich das Interval-Problem mit den Jahreszahlen
// nicht wirklich optimal, da so einige Dokumente beim Topic Mining vergessen werden

public class IntervalFix {


	public IntervalFix(ConfigRawdataServer info, ConfigTopicminingServer info2) {


		try {
			Connection connection = ConnectionManager.getConnection();
			Statement statement = connection.createStatement();

			String sqlquery="UPDATE "+DVitaConfig.getSchemaDot()+info2.tablePrefix+ "_topicintervals SET intervalStart=intervalStart+1 YEAR, intervalEnd=intervalEnd+1 YEAR";
			statement.executeUpdate(sqlquery);
			
			statement.close();
			connection.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}



	}

	public static void main(String[] args) {

		try {
			
			 //args = new String[1];
			 //	args[0] = "15";
			
			
			 if(args.length!=1) { System.out.println("please specify topicMining ID"); System.exit(-1); }
			
			ConfigRawdataServer info = new ConfigRawdataServer();
			ConfigTopicminingServer info2 = new ConfigTopicminingServer();
			Connection connection = ConnectionManager.getConnection();
			Statement statement = connection.createStatement();
			
			 // eintrag in spalte ID aus Tabelle config_topicmining
			 int id = Integer.parseInt(args[0]);


			info2 =  com.mytest.dvita.server.ConfigReader.readConfigTopicmining(id, statement);
			info =  com.mytest.dvita.server.ConfigReader.readConfigRawdata(info2.rawdataID, statement);

			statement.close();
			connection.close();

			new IntervalFix(info,info2);

		} catch (SQLException e) {
			
			e.printStackTrace();
		}


	}

}
