package exceptions;

public class redisConnectFailedException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4591072816099159184L;

	public redisConnectFailedException(){
        super("云Redis连接失败");
    }
	
	public redisConnectFailedException(String message) {
		super("云Redis连接失败："+message);
	}
}
