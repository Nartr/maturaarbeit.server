package robin.oester.jumpandrun.server.tools;

import robin.oester.jumpandrun.server.ServerStarter;

public class Player {
	
	private int id, colorID, userID;													//userID refers to players unique id inside the database
	private String name;
	private long time, record;															//the time, the player had for this parcour and his record

	public Player(int id, String name, int userID) {
		this.id = id;
		this.name = name;
		this.colorID = getNextColor();													//set the colorID to the next free color
		this.userID = userID;
	}
	
	private static int getNextColor() {													//due to the dependence of other players method has to be static
		boolean[] used = new boolean[ServerStarter.MAX_PLAYERS];						//create a boolean with the maximum of players
		for(Player players : ServerStarter.getAllPlayers()) {
			used[players.getColorID()] = true;											//go through all players and set their colorID to true if
		}																				//it's used
		for(int i = 0; i < used.length; i++) {
			if(!used[i]) {
				return i;																//go through the boolean array and return the next unused color
			}
		}
		return 0;
	}

	public int getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public int getColorID() {
		return colorID;
	}
	
	public boolean hasFinished() {
		return time != 0;
	}

	public void setTime(long time) {
		this.time = time;
	}
	
	public long getTime() {
		return time;
	}
	
	public int getUserID() {
		return userID;
	}
	
	public void setRecord(long record) {
		this.record = record;
	}
	
	public long getRecord() {
		return record;
	}
}
