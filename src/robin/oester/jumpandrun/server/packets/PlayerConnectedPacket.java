package robin.oester.jumpandrun.server.packets;

import java.util.Map;

public class PlayerConnectedPacket {
	public String newPlayer;
	public int newPlayerColor;
	public Map<String, Integer> players;
}
