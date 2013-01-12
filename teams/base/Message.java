package base;

public class Message {
	
	public boolean isValid = false;
	public byte header;
	public short body;
	
	public Message(byte header, short body, boolean isValid) {
		this.header = header;
		this.body = body;
		this.isValid = isValid;
	}
	
	public Message(byte header, short body) {
		this(header, body, true);
	}
	
	public Message(boolean isValid) {
		this(0, 0, isValid);
	}
	
	// for default ints
	public Message(int header, int body, boolean isValid) {
		this((byte)header, (short)body, isValid);
	}
	
	public Message(int header, int body) {
		this((byte)header, (short)body, true);
	}
}