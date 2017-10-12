package robin.oester.jumpandrun.server.countdown;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

import robin.oester.jumpandrun.server.ServerStarter;
import robin.oester.jumpandrun.server.packets.CountdownPacket;
import robin.oester.jumpandrun.server.packets.ServerChangeStatusPacket;
import robin.oester.jumpandrun.server.tools.GameState;
import robin.oester.jumpandrun.server.tools.Player;

public class StartingCountdown extends Countdown {
	
	private static final int WORLDS = 2;

	public StartingCountdown() {
		super(15);																		//set the starting countdown to 15 seconds
	}

	@Override
	public void start() {
		super.start();
		ServerStarter.sendMessage("Player got teleportet into arena");
		
		ServerStarter.setWorld(new Random().nextInt(WORLDS)	+ 1);						//select a random world
		
		ResultSet result = ServerStarter.getMysql().getResults(							//get the world record from the chosen map
				"SELECT time FROM records WHERE userid='0' AND "
				+ "worldid='" + ServerStarter.getWorld() + "'");
		long wRecord = Long.MAX_VALUE;													//if none exist, set the record to a high number
		try {
			if(result.next()) {
				wRecord = result.getInt("time");										//get the result and set the world record
			}
			result.close();																//close all resources
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		ServerStarter.setWorldRecord(wRecord);											//set the world record
		
		ServerChangeStatusPacket packet = new ServerChangeStatusPacket();				//create a new change status packet
		packet.newState = GameState.START;
		ServerStarter.setState(GameState.START);
		
		for(Player all : ServerStarter.getAllPlayers()) {								//go through all players and select their record on the map
			ResultSet r = ServerStarter.getMysql().getResults(
					"SELECT time FROM records WHERE userid='"
			+ all.getUserID() + "' AND worldid='"
			+ ServerStarter.getWorld() + "'");
			long record = Long.MAX_VALUE;
			try {
				if(r.next()) {
					record = r.getInt("time");											//if exist, set it to the time of the result else to a high
				}																		//number
				r.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			all.setRecord(record);														//save their record into the player array
			packet.input = ServerStarter.getWorld() + ":" + 							//set the input to worldID:worldrecord (in seconds):player
					ServerStarter.getWorldRecord() / 1000.0 + 							//record
					":" + record / 1000.0;
			
			ServerStarter.sendTCP(all.getId(), packet);									//send the packet to all players and add them to game players
			ServerStarter.getGamePlayers().add(all);
		}
	}
	
	@Override
	protected void second(int second) {
		super.second(second);
		if(second > 0) {
			ServerStarter.sendMessage("Game starts in " + second + " seconds");
			
			CountdownPacket packet = new CountdownPacket();								//send a countdown packet to all lobby players
			packet.seconds = second;
			
			for(Player lobby : ServerStarter.getLobbyPlayers()) 
			{
				ServerStarter.sendTCP(lobby.getId(), packet);
			}
		} else {
			ServerStarter.sendMessage("Game started");									//if the countdown finished, send a status change packet 
			ServerChangeStatusPacket packet = new ServerChangeStatusPacket();			//to all gaming players
			packet.newState = GameState.GAME;
			ServerStarter.broadcastTCP(packet, false);
		}
	}
	
	@Override
	public void interrupt() {
		super.interrupt();
		ServerStarter.sendMessage("Starting Countdown was interrputed");
	}
}
