package robin.oester.jumpandrun.server.tools;

import java.util.Comparator;

public class PlayerComerator implements Comparator<Player> {							//Comparator to compare the player

	@Override
	public int compare(Player o1, Player o2) {
		return (int) (o1.getTime() - o2.getTime());										//returns the time difference, if big, then player two is better 
	}																					//inside the ranking
}
