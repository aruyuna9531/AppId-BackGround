/**
 * 数据包太长异常
 */

package exceptions;

public class PacketTooLongException extends Exception{


	/**
	 * 
	 */
	private static final long serialVersionUID = -7252120356015163802L;

	public PacketTooLongException(){
        super("异常：数据包长度太大");
    }
	
	public PacketTooLongException(int length) {
		super("异常：数据包长度太大："+length);
	}
}
