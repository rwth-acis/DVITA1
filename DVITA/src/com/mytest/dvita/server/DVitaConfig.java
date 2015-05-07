package com.mytest.dvita.server;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

// This class represents the connection data stored in the app directory
// in the configuration file "dvita.config"

// TODOMD: add Topic Mining settings to the file
public class DVitaConfig {
	private static String dbconnection = null;
	private static String dbdriver;
	private static String dbschema;
	private static String dbusername;
	private static String dbpassword;
	
	// PUBLIC //
	public static String getConnectionString() { return read("dbconnection");	}	
	public static String getDriver() { return read("dbdriver"); }
	public static String getUserName() { return read("dbusername"); }
	public static String getPassword() { return read("dbpassword"); }
	public static String getSchema() { return read("dbschema"); }
	
	public static String getSchemaDot() {
		// only add dot if schema isn't empty
		String schema = read("dbschema");
		if(schema.trim() == "")
			return schema;
		return schema + "."; 
	}
	
	// PRIVATE //	
	private DVitaConfig() {
	}	
	
	private static String presetPath = null;
	public static void presetPath(String path) {
		// this can be used when Paths.get("") does not deliver the correct path
		System.out.println("PRESET CONFIG FILE = " + path);
		presetPath = path;
	}
	
	private static String read(String what) {
		try {
			if(dbconnection == null) 
			{			
				// First call -- read all the info from dvita.config
				String path = presetPath;
				if(path == null) {
					System.out.println("CONFIG FILE = " + path);
					path = Paths.get("dvita.config").toAbsolutePath().toString();
				}
				
				// read the file				
				for(String line : Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8)) {
					int pos = line.indexOf("=");
					String field = line.substring(0, pos).trim().toLowerCase();
					String value = line.substring(pos + 1).trim();
					DVitaConfig.class.getDeclaredField(field).set(null, value);
					//System.out.println("  " + field + "=" + value);					
				}
			}
			
			Field f = DVitaConfig.class.getDeclaredField(what);			
			return f != null ? (String) f.get(null) : "";
		}
		catch(Exception e) 
		{
			System.out.println("ERROR READING CONFIG FILE dvita.config! " + e.getMessage());
			return "";
		}
	}
}