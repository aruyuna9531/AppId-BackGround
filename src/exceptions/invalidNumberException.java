package exceptions;

public class invalidNumberException extends Exception{
	/**
	 * 
	 */
	private static final long serialVersionUID = 8269391896753946290L;

	public invalidNumberException(){
        super("非法数字：");
    }
	
	public invalidNumberException(String message) {
		super("非法数字："+message);
	}
}
