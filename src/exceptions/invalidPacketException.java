/**
 * 非法数据包异常
 */

package exceptions;

public class invalidPacketException extends Exception{
	/**
	 * 
	 */
	private static final long serialVersionUID = -8422590552954959579L;

	public invalidPacketException(){
        super("非法包：");
    }
	
	public invalidPacketException(String message) {
		super("非法包："+message);
	}
	
}
