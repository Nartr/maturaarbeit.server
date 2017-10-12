package robin.oester.jumpandrun.server.tools;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import robin.oester.jumpandrun.server.ServerStarter;
import robin.oester.jumpandrun.server.packets.MessagePacket;
import robin.oester.jumpandrun.server.packets.PlayerConnectedPacket;
import robin.oester.jumpandrun.server.packets.PlayerDisconnectedPacket;
import robin.oester.jumpandrun.server.packets.PlayerHitLine;
import robin.oester.jumpandrun.server.packets.PlayerLoginPacket;
import robin.oester.jumpandrun.server.packets.PlayerMovePacket;
import robin.oester.jumpandrun.server.packets.PlayerRespondPacket;
import robin.oester.jumpandrun.server.packets.Reason;

public class ServerListener extends Listener {
	
	@Override
	public void connected(Connection connection) {										//fired when a player connects but not important
		ServerStarter.sendMessage("ID " + connection.getID() + 
				" connected to the server");
	}

	@Override
	public void disconnected(Connection connection) 									//fired when a player disconnects
	{
		ServerStarter.sendMessage("ID " + connection.getID() + 
				" disconnected from the server");
		
		Player p = ServerStarter.getByID(connection.getID());
		if(p == null) {
			return;																		//if player is not logged in, it doesn't matter 
		}
		
		PlayerDisconnectedPacket packet = new PlayerDisconnectedPacket();				//else create a disconnected packet
		packet.player = p.getName();
		
		ServerStarter.getAllPlayers().remove(p);										//remove the player from the list
		
		if(ServerStarter.getGamePlayers().contains(p)) {								//if he was playing remove him from the gaming list
			ServerStarter.getGamePlayers().remove(p);
		}
		
		ServerStarter.broadcastTCP(packet, true);
		
		ServerStarter.getConsole().append(packet.player + " hat das Spiel verlassen\n");
		MessagePacket back = new MessagePacket();
		back.message = ServerStarter.getConsole().toString();
		
		for(Player all : ServerStarter.getLobbyPlayers()) {								//send the console to all players in the lobby
			ServerStarter.sendTCP(all.getId(), back);
		}
		
		if(ServerStarter.getState() > GameState.WAITING &&								//if a game is running
				ServerStarter.getGamePlayers().size() == 0 && 							//if size of game players is 0 then stop countdowns and clean
				ServerStarter.getAllPlayers().size() != 0) {							//console but if there are lobby players left then start
			ServerStarter.setConsole(new StringBuilder());								//waiting countdown
			if(ServerStarter.getStarting().isRunning()) {
				ServerStarter.getStarting().interrupt();
			}
			if(ServerStarter.getFinished().isRunning()) {
				ServerStarter.getFinished().interrupt();
			}
			ServerStarter.getWaiting().start();							
		}
		
		if(ServerStarter.getAllPlayers().size() == 0) {									//if all player left, then stop all countdowns and
			ServerStarter.setConsole(new StringBuilder());								//wait in login screen
			ServerStarter.setState(GameState.LOGIN);
			if(ServerStarter.getWaiting().isRunning()) {
				ServerStarter.getWaiting().interrupt();
			}
			if(ServerStarter.getStarting().isRunning()) {
				ServerStarter.getStarting().interrupt();
			}
			if(ServerStarter.getFinished().isRunning()) {
				ServerStarter.getFinished().interrupt();
			}
		}
	}

	@Override
	public void received(Connection connection, Object object) {
		if(object instanceof PlayerLoginPacket) {
			ServerStarter.sendMessage("Received Login Packet from " 					//handle log in in separate method
					+ connection.getID());
			
			PlayerLoginPacket packet = (PlayerLoginPacket) object;
			processLogin(connection, packet);
		}
		if(object instanceof PlayerMovePacket) {										//send the move packet to all other players than the sender
			PlayerMovePacket packet = (PlayerMovePacket) object;
			ServerStarter.sendUDP(connection.getID(), packet);
		}
		if(object instanceof PlayerHitLine) {
			PlayerHitLine packet = (PlayerHitLine) object;
			long time = packet.time;
			
			Player p = ServerStarter.getByID(connection.getID());
			p.setTime(time);															//if player hit line, set time to the time of the packet
			
			if(time < p.getRecord() && p.getRecord() != Long.MAX_VALUE) {				//if the personal record is beaten, then update the record
				ServerStarter.getMysql().process("UPDATE records SET time='" + 
						time + "' WHERE userid='" + p.getUserID() + 
						"' AND worldid='" + ServerStarter.getWorld() + "'");
			} else if(p.getRecord() == Long.MAX_VALUE) {								//if the personal record isn't set, then insert new record					
				ServerStarter.getMysql().process("INSERT INTO records "
						+ "(userid, worldid, time) VALUES ('" + p.getUserID() + 
						"', '" + ServerStarter.getWorld() + "', '"
						+ time + "')");
			}
			
			if(time < ServerStarter.getWorldRecord() && 								//the same for world record
					ServerStarter.getWorldRecord() != Long.MAX_VALUE) {
				ServerStarter.getMysql().process("UPDATE records SET time='" + 
					time + "' WHERE userid='0' AND worldid='" + 
						ServerStarter.getWorld() + "'");
				ServerStarter.setWorldRecord(time);										//update the world record in the field
			} else if(ServerStarter.getWorldRecord() == Long.MAX_VALUE) {
				ServerStarter.getMysql().process("INSERT INTO records "
						+ "(userid, worldid, time) VALUES ('0', '" + 
						ServerStarter.getWorld() + "', '" + time + "')");
				ServerStarter.setWorldRecord(time);
			}
			
			ServerStarter.sendMessage(connection.getID() + " succeded with " + 
					time / 1000.0 + " seconds");
			
			if(!ServerStarter.getFinished().isRunning()) {								//if player is first, start the finished countdown
				ServerStarter.getFinished().start();
			}
		}
		if(object instanceof MessagePacket) {
			ServerStarter.sendMessage("Received Message Packet from " + 
					connection.getID());
			MessagePacket packet = (MessagePacket) object;
			
			ServerStarter.sendMessage(ServerStarter.getByID(
					connection.getID()).getName() + ": " + packet.message);
			
			ServerStarter.getConsole().append(ServerStarter.getByID(					//if server receives a message packet, he adds the string to
					connection.getID()).getName() + ": " + 								//the console
					packet.message + "\n");
			
			MessagePacket back = new MessagePacket();
			back.message = ServerStarter.getConsole().toString();
			
			for(Player lobby : ServerStarter.getLobbyPlayers()) {
				ServerStarter.sendTCP(lobby.getId(), back);								//and sends it back to all players inside the lobby
			}
		}
	}

	private void processLogin(Connection con, PlayerLoginPacket packet) {
		ResultSet rs = ServerStarter.getMysql().getResults(
				"SELECT * FROM users WHERE username='" + packet.name + "'");			//first select all data from the players inside the database
		try {
			PlayerRespondPacket respond = new PlayerRespondPacket();					//create the empty respond packet
			if(rs.next()) {
				if(!packet.login) {														//if the user tries to register but player is already existing
					respond.reason = Reason.USER_EXIST;
					ServerStarter.sendMessage(con.getID() + 
							" tried to register existing user");
				} else {
					String password = rs.getString("password");
					if(packet.password.equals(password)) {
						if(ServerStarter.getAllPlayers().size() >= 						//check for password and players inside the game
								ServerStarter.MAX_PLAYERS) {
							respond.reason = Reason.TOO_MANY_PLAYERS;
							ServerStarter.sendMessage(con.getID() + " couldn't"
									+ " login because of too many players");
						} else {
							if(ServerStarter.getByName(packet.name) != null) {
								respond.reason = Reason.ALREADY_LOGGED_IN;
								ServerStarter.sendMessage(con.getID() + " tried"
										+ " to log in with playing username");
							} else {
								respond.reason = Reason.VALID;
								login(con.getID(), packet, rs.getInt("userid"));		//else login is valid then go through another method
							}
						}
					} else {
						respond.reason = Reason.WRONG_PASWORD;
						ServerStarter.sendMessage(con.getID() + 
								" used wrong password");
					}
				}
			} else {
				if(packet.login) {														//if the player wants to log in but username is not used
					respond.reason = Reason.INVALID_USERNAME;
					ServerStarter.sendMessage(con.getID() + " used invalid username");
				} else {
					if(ServerStarter.getAllPlayers().size() >= 							//check for online player size
							ServerStarter.MAX_PLAYERS) {
						respond.reason = Reason.TOO_MANY_PLAYERS;
						ServerStarter.sendMessage(con.getID() + 
								" couldn't login because of too many players");
					} else {															//if player is not used -> register and login
						respond.reason = Reason.VALID;
						ServerStarter.getMysql().process("INSERT INTO users "			//insert into database
								+ "(username, password) VALUES ('" + 
								packet.name + "', '" + packet.password + "')");
						ResultSet result = ServerStarter.getMysql().getResults(			//select his userID
								"SELECT userid FROM users WHERE username='" + 
								packet.name + "'");
						if(result.next()) {
							login(con.getID(), packet, result.getInt("userid"));		//get his userID
						}
					}
				}
			}
			ServerStarter.sendTCP(con.getID(), respond);								//send the respond packet
			
			rs.close();																	//close all resources
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void login(int id, PlayerLoginPacket packet, int userID) {
		ServerStarter.sendMessage(id + " logged in");
		
		Player newby = new Player(id, packet.name, userID);								//create a new player
		PlayerConnectedPacket conPacket = new PlayerConnectedPacket();
		conPacket.newPlayer = newby.getName();											//add his name and color to the packet
		conPacket.newPlayerColor = newby.getColorID();
		Map<String, Integer> players = new HashMap<>();
		for(Player all : ServerStarter.getAllPlayers()) {
			players.put(all.getName(), all.getColorID());								//go through all online players and add their color and name
		}
		conPacket.players = players;
		
		ServerStarter.getAllPlayers().add(newby);										//add to online player list
		ServerStarter.broadcastTCP(conPacket, true);									//send the connection packet to all players
		
		ServerStarter.getConsole().append(packet.name + 
				" hat das Spiel betreten\n");
		MessagePacket back = new MessagePacket();
		back.message = ServerStarter.getConsole().toString();							//create a new message packet and send it to all lobby players
		
		for(Player lobby : ServerStarter.getLobbyPlayers()) {
			ServerStarter.sendTCP(lobby.getId(), back);
		}
		
		if(ServerStarter.getAllPlayers().size() == 1 && 								//if he is the first one on the server then start waiting
				ServerStarter.getState() == GameState.LOGIN) {
			ServerStarter.getWaiting().start();
		}
	}
}
