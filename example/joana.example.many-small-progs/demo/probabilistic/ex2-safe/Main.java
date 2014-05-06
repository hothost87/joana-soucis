public class Main {

	static int l, h;
	
	public static void main(String[] argv) throws InterruptedException {
		new Thread_1().start();
		new Thread_2().start();
	}

	static class Thread_1 extends Thread {
		
		public void run() {
			Main.l = 0;
			Main.h = Main.inputPIN();
		}
	
	}
	
	static class Thread_2 extends Thread {
		
		public void run() {
//			Main.l = Main.h;
			Main.print(Main.l);
			Main.l = Main.h;
		}
	
	}
	
	//@Source(Level.HIGH)
	public static int inputPIN() { return 42; }
	//@Sink(Level.LOW)
	public static void print(int i) {}

}