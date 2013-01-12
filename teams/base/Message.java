package base;

/**
 * Message interface for sending broadcasts.
 * WARNING: Must ensure that only last 24 bits of body are used
 */
public class Message {
	
	public boolean isValid = false;
	public int body;
	
	public Message(int body, boolean isValid) {
		this.body = body;
		this.isValid = isValid;
	}
	
	public Message(boolean isValid) {
		this(0, isValid);
	}
}