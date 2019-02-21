package exceptions;

public class NotTCPException extends Exception{
	/**
	 * 
	 */
	private static final long serialVersionUID = -6986111010476785878L;

	/**
	 * 
	 */

	public NotTCPException(){
        super("不是TCP包：");
    }
	
	public NotTCPException(String message) {
		super("不是TCP包："+message);
	}
	
}
