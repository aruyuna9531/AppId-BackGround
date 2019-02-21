package exceptions;

public class PacketReceiveFailedException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 363258353924576223L;

	public PacketReceiveFailedException(){
        super("包接收失败");
    }
	
	public PacketReceiveFailedException(String message) {
		super("包接收失败："+message);
	}
}
