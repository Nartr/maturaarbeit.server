package robin.oester.jumpandrun.server;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Server;

import robin.oester.jumpandrun.server.countdown.FinishedCountdown;
import robin.oester.jumpandrun.server.countdown.StartingCountdown;
import robin.oester.jumpandrun.server.countdown.WaitingCountdown;
import robin.oester.jumpandrun.server.mysql.MySQL;
import robin.oester.jumpandrun.server.packets.CountdownPacket;
import robin.oester.jumpandrun.server.packets.MessagePacket;
import robin.oester.jumpandrun.server.packets.PlayerConnectedPacket;
import robin.oester.jumpandrun.server.packets.PlayerDisconnectedPacket;
import robin.oester.jumpandrun.server.packets.PlayerHitLine;
import robin.oester.jumpandrun.server.packets.PlayerLoginPacket;
import robin.oester.jumpandrun.server.packets.PlayerMovePacket;
import robin.oester.jumpandrun.server.packets.PlayerRespondPacket;
import robin.oester.jumpandrun.server.packets.ServerChangeStatusPacket;
import robin.oester.jumpandrun.server.tools.GameState;
import robin.oester.jumpandrun.server.tools.Player;
import robin.oester.jumpandrun.server.tools.ServerListener;

public class ServerStarter {
	
	public static final int MAX_PLAYERS = 5;
	
	private static final int TCP_PORT = 6000, UDP_PORT = 6001;
	
	private static Calendar cal;
	private static SimpleDateFormat format;
	
	private static Server server;
	private static Kryo kryo;
	private static MySQL mysql;
	
	private static List<Player> allPlayers;
	private static List<Player> gamePlayers;
	
	private static StringBuilder console;
	
	private static int state;
	
	private static WaitingCountdown waiting;
	private static StartingCountdown starting;
	private static FinishedCountdown finished;
	
	private static int world;
	private static long worldRecord;

	public static void main(String[] args) {
		ServerStarter.cal = Calendar.getInstance();										//open calendar
		ServerStarter.format = new SimpleDateFormat("HH:mm:ss");						//create format for console
		
		ServerStarter.server = new Server();											//create server to connect
		
		sendMessage("Starting server...");
		server.start();																	//start server thread asynchronously
		
		try {
			server.bind(TCP_PORT, UDP_PORT);											//bind on TCP and UDP port
			sendMessage("bound to port " + TCP_PORT + ", " + UDP_PORT);
		} catch (IOException e) {
			sendMessage("Port already occupied");										//if port is occupied, then shutdown the system
			sendMessage("Prepering for shutdown...");
			try {
				Thread.sleep(2000);
				System.exit(1);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		
		setupKryo();																	//setup the packet manager
		
		server.addListener(new ServerListener());										//add a packet listener
		
		sendMessage("Connecting to MySQL...");
		mysql = new MySQL("localhost", 3306, "jump_and_run", "root", "07A.ciq3"); 		//connect to mysql server with specific database
		mysql.connect();
		
		ServerStarter.allPlayers = new ArrayList<>();
		ServerStarter.gamePlayers = new ArrayList<>();
		ServerStarter.console = new StringBuilder();
		ServerStarter.state = GameState.LOGIN;											//set state to login and wait for clients to connect
		ServerStarter.waiting = new WaitingCountdown();
		ServerStarter.starting = new StartingCountdown();
		ServerStarter.finished = new FinishedCountdown();
	}
	
	public static void sendMessage(String msg) {
		System.out.println("[" + format.format(cal.getTime()) + "]: " + msg);			//method to print a server message
	}
	
	public static void stop() {															//if server is stopped then close the connections
		mysql.disconnect();
		server.stop();
	}
	
	private static void setupKryo() {
		ServerStarter.kryo = server.getKryo();											//register all classes/packets
		
		kryo.register(HashMap.class);
		kryo.register(PlayerLoginPacket.class);
		kryo.register(PlayerRespondPacket.class);
		kryo.register(PlayerConnectedPacket.class);
		kryo.register(PlayerDisconnectedPacket.class);
		kryo.register(MessagePacket.class);
		kryo.register(CountdownPacket.class);
		kryo.register(ServerChangeStatusPacket.class);
		kryo.register(PlayerHitLine.class);
		kryo.register(PlayerMovePacket.class);
	}
	
	public static void sendUDP(int id, Object o) {
		for(Player all : gamePlayers) {
			if(all.getId() != id) {
				server.sendToUDP(all.getId(), o);
			}
		}
	}
	
	public static void sendTCP(int connectionID, Object o) {							//send packet per TCP to userID
		server.sendToTCP(connectionID, o);
	}
	
	public static synchronized void broadcastTCP(Object o, boolean all) {				//broadcast a packet per TCP either to all or only to
		broadcastTCP(o, -1, all);														//in-game players
	}

	public static synchronized void broadcastTCP(Object o, int id, boolean all) {		//broadcast a packet per TCP except one player
		if(all) {
			for(Player p : allPlayers) {
				if(p.getId() != id) {
					sendTCP(p.getId(), o);
				}
			}
		} else {
			for(Player p : gamePlayers) {
				if(p.getId() != id) {
					sendTCP(p.getId(), o);
				}
			}
		}
	}
	
	public static MySQL getMysql() {
		return mysql;
	}
	
	public static List<Player> getAllPlayers() {
		return allPlayers;
	}
	
	public static List<Player> getGamePlayers() {
		return gamePlayers;
	}
	
	public static List<Player> getLobbyPlayers() {										//gets all player who are not in-game or log in
		List<Player> lobby = new ArrayList<>();
		for(Player p : allPlayers) {
			if(!gamePlayers.contains(p)) {
				lobby.add(p);
			}
		}
		return lobby;
	}
	
	public static synchronized Player getByName(String name) {							//returns a player by its name
		for(Player p : allPlayers) {
			if(p.getName().equalsIgnoreCase(name)) {
				return p;
			}
		}
		return null;
	}
	
	public static synchronized Player getByID(int id) {									//returns a player by its id
		for(Player p : allPlayers) {
			if(p.getId() == id) {
				return p;
			}
		}
		return null;
	}
	
	public static StringBuilder getConsole() {
		return console;
	}
	
	public static int getState() {
		return state;
	}
	
	public static void setState(int state) {
		ServerStarter.state = state;
	}
	
	public static WaitingCountdown getWaiting() {
		return waiting;
	}

	public static StartingCountdown getStarting() {
		return starting;
	}
	
	public static void setWorldRecord(long worldRecord) {
		ServerStarter.worldRecord = worldRecord;
	}
	
	public static long getWorldRecord() {
		return worldRecord;
	}
	
	public static FinishedCountdown getFinished() {
		return finished;
	}

	public static void setConsole(StringBuilder builder) {
		console = builder;
	}
	
	public static int getWorld() {
		return world;
	}
	
	public static void setWorld(int world) {
		ServerStarter.world = world;
	}
}
