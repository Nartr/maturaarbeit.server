package robin.oester.jumpandrun.server.countdown;

public class Countdown implements Runnable {

	private Thread t;																	//The thread in which the countdown runs
	private boolean isRunning;
	private int seconds, timer;
	
	public Countdown(int seconds) {
		this.seconds = seconds;															//seconds defines the length of the countdown
	}
	
	public void start() {
		this.timer = seconds;															//on start, set the timer to wanted seconds
		this.isRunning = true;
		this.t = new Thread(this);														//create a new thread and start it
		t.start();
	}
	
	@Override
	public void run() {
		while(isRunning) {
			if(timer <= 0) {
				isRunning = false;														//if the timer is 0, stop the thread
			}
			
			second(timer);																//execute second-function
			timer--;																	//subtract one from timer
			try {
				Thread.sleep(1000);														//try to sleep one second
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void setTime(int seconds) {
		timer = seconds;																//set the time to shorten the countdown
	}
	
	protected void second(int second) {}												//fires every second of the countdown							
	
	public void interrupt() {
		isRunning = false;																//if the thread is interrupted then set running to false
	}
	
	public boolean isRunning() {
		return isRunning;
	}
}
