package robin.oester.jumpandrun.server.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import robin.oester.jumpandrun.server.ServerStarter;

public class MySQL {

	private String host, database, username, password;
	private int port;
	private Connection con;

	public MySQL(String host, int port, String database, String 
			username, String password) {
		this.host = host;
		this.port = port;
		this.database = database;
		this.username = username;
		this.password = password;
	}

	public void connect() {
		try {
			con = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + 	//tries to connect to the specific database
					"/" + database, username, password);
			ServerStarter.sendMessage("MySQL connection established");
		} catch (SQLException e) {
			e.printStackTrace();
			try {
				Thread.sleep(2000);
				System.exit(1);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	public boolean isConnected() {
		try {
			return con != null && con.isValid(1);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public void disconnect() {
		if(isConnected()) {
			try {
				con.close();															//close the connection on disabling the server
				System.out.println("Server >> MySQL: Disconnected");
			} catch (SQLException e) {
				System.err.println("Server >> Mysql: Error on closing the connection");
				e.printStackTrace();
			}
		}
	}
	
	public void process(String query) {													//process a query string often update, insert or delete
		if(isConnected()) {
			try {
				PreparedStatement stmt = con.prepareStatement(query);					//prepare a statement
				stmt.executeUpdate();													//execute it in the database
				stmt.close();															//close the statement after processing
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public ResultSet getResults(String query) {											//get the result often select
		try {
			if(isConnected()) {															//if database is connected then execute the query and return
				PreparedStatement stmt = con.prepareStatement(query);					//the result
				ResultSet rs = stmt.executeQuery();
				return rs;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
}
