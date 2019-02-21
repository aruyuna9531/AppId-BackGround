/**
 * 错误参数异常
 */

package exceptions;

public class WrongParameterException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9185171103447004952L;

	public WrongParameterException(){
        super("参数错误");
    }
	
	public WrongParameterException(String message) {
		super("参数错误："+message);
	}
	
	
}
