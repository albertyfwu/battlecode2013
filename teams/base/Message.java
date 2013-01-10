package base;

public class Message {
	public byte header;
	public short body;
	
	public Message(byte header, short body) {
		this.header = header;
		this.body = body;
	}
	
	// for default ints
	public Message(int header, int body) {
		this.header = (byte) header;
		this.body = (short) body;
	}
}