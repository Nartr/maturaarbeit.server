package robin.oester.jumpandrun.server.countdown;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import robin.oester.jumpandrun.server.ServerStarter;
import robin.oester.jumpandrun.server.packets.CountdownPacket;
import robin.oester.jumpandrun.server.packets.MessagePacket;
import robin.oester.jumpandrun.server.packets.ServerChangeStatusPacket;
import robin.oester.jumpandrun.server.tools.GameState;
import robin.oester.jumpandrun.server.tools.Player;
import robin.oester.jumpandrun.server.tools.PlayerComerator;

public class FinishedCountdown extends Countdown {
	
	private boolean annoMode;															//"rank annunciation" mode
	
	public FinishedCountdown() {
		super(90);																		//90 seconds long
	}

	@Override
	protected void second(int second) {
		super.second(second);
		if(second > 0) {
			CountdownPacket packet = new CountdownPacket();								//every second send countdown packet to all lobby-clients
			packet.seconds = second;
			
			for(Player lobby : ServerStarter.getLobbyPlayers()) {
				ServerStarter.sendTCP(lobby.getId(), packet);
			}
			
			if(second < 10) {
				ServerStarter.sendMessage("Server will restart in " + 					//print a message in the server console
						second + " seconds");
			}
		} else {
			ServerChangeStatusPacket packet = new ServerChangeStatusPacket();			//if the "rank annunciation" has finished create a server
																						//changed status packet
			packet.newState = GameState.WAITING;										//set the new state to waiting
			ServerStarter.broadcastTCP(packet, false);									//broadcast the packet
			ServerStarter.getGamePlayers().clear();										//clear the list of playing players
			ServerStarter.setConsole(new StringBuilder());								//reset the console inside the waiting screen
			MessagePacket message = new MessagePacket();
			message.message = "Server restarted";										//broadcast the restarted message
			ServerStarter.broadcastTCP(message, true);
			ServerStarter.sendMessage("Server restarted");
			for(Player all : ServerStarter.getAllPlayers()) {							//reset the world record and the times of all players
				all.setTime(0);
				all.setRecord(0);
			}
			if(ServerStarter.getAllPlayers().size() > 0) {
				ServerStarter.getWaiting().start();										//start the waiting countdown if there are players
			}
		}
		if(second > 30 && !annoMode) {								
			boolean finished = true;
			for(Player game : ServerStarter.getGamePlayers()) {
				if(game.getTime() == 0) {
					finished = false;
				}
			}
			
			if(finished) {																//if annoMode is deactivated and all players finished then
				annoMode = true;														//setup the "rank annunciation"
				setTime(30);															//shorten the countdown to 30 seconds
				setupAnnunciation();
			}
		} else if(second < 30 && !annoMode) {											//if some players loose more than one minute, then start the
			annoMode = true;															//the countdown and put them without time on the last ranks
			setupAnnunciation();
		}
	}
	
	private void setupAnnunciation() {
		ServerChangeStatusPacket packet = new 							
				ServerChangeStatusPacket();
		
		List<Player> rankList = new ArrayList<>();										//create an empty rank list

		for(Player all : ServerStarter.getGamePlayers()) {
			if(all.getTime() != 0) {
				rankList.add(all);														//put all players with time in it
			}
		}
		
		Collections.sort(rankList, new PlayerComerator());								//sort according to their time
		
		for(Player all : ServerStarter.getGamePlayers()) {
			if(all.getTime() == 0) {
				rankList.add(all);														//put the players who did not finish on the last places
			}
		}
		
		packet.newState = GameState.END_GAME;
		for(int i = 0; i < rankList.size(); i++) {
			packet.input = String.valueOf(i + 1);
			ServerStarter.sendTCP(rankList.get(i).getId(), packet);						//set the new state to end game and flush the rank list
		}
		ServerStarter.setState(GameState.END_GAME);
	}

	@Override
	public void start() {
		super.start();
		annoMode = false;																//start the finished countdown and set the annoMode to false
		ServerStarter.sendMessage("Finished Countdown started");
	}
	
	@Override
	public void interrupt() {
		super.interrupt();
		ServerStarter.sendMessage("Finished Countdown was interrputed");
	}
}
