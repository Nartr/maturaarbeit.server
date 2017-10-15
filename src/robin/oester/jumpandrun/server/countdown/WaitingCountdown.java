package robin.oester.jumpandrun.server.countdown;

import robin.oester.jumpandrun.server.ServerStarter;
import robin.oester.jumpandrun.server.packets.CountdownPacket;
import robin.oester.jumpandrun.server.packets.MessagePacket;
import robin.oester.jumpandrun.server.tools.GameState;

public class WaitingCountdown extends Countdown {
	
	public WaitingCountdown() {
		super(60);																		//set the waiting countdown to 40 seconds
	}

	@Override
	public void start() {
		super.start();
		ServerStarter.setState(GameState.WAITING);										//on starting set the state to waiting
		ServerStarter.sendMessage("Waiting Countdown started");
	}

	@Override
	protected void second(int second) {
		super.second(second);
		CountdownPacket countPacket = new CountdownPacket();							//send a countdown packet to all players
		countPacket.seconds = second;
		ServerStarter.broadcastTCP(countPacket, true);
		switch (second) {
		case 300: case 180:																//send a message at 300, 180, 60, 30, 20, 10, 5, 4, 3, 2, 1 seconds left
			String msg1 = String.format("The game starts in %s", second / 60 + 
					" minutes");
			sendMessagePacket(msg1);
			break;
		case 60:
			String msg2 = "The game starts in 1 minute";
			sendMessagePacket(msg2);
			break;
		case 30: case 20: case 10: case 5: case 4: 
			case 3: case 2:
			String msg3 = String.format("The game starts in %s", second + " seconds");
			sendMessagePacket(msg3);
			break;
		case 1:
			String msg4 = "The game starts in 1 second";
			sendMessagePacket(msg4);
			break;
		case 0:
			String msg5 = "The game starts now";
			sendMessagePacket(msg5);
			ServerStarter.getStarting().start();										//start the starting countdown
			break;
		default:
			break;
		}
	}

	@Override
	public void interrupt() {
		super.interrupt();
		ServerStarter.sendMessage("The Waiting Countdown was interrupted");
	}
	
	private void sendMessagePacket(String msg) {
		MessagePacket packet = new MessagePacket();										//append a string to the current console
		packet.message = ServerStarter.getConsole().append(msg + "\n").toString();
		
		ServerStarter.broadcastTCP(packet, true);										//broadcast the new console to all players
		ServerStarter.sendMessage(msg);
	}
}
