/**
 * 一些全局的处理函数
 */

package general;

import exceptions.WrongParameterException;
import exceptions.invalidNumberException;
import parseFrames.EapolFrame;
import recvFrames.Listener;

public class commonFunctions {
	public static int byteConvert(byte b) {
		return b>=0?b:(256+b);
	}
	
	private static char DecToHex(int x) throws WrongParameterException {
		if(x>=16)throw new WrongParameterException("DecToHex()：x="+x+">=16");
		if(x<=9)return (char)(x+'0');
		else return (char)(x-10+'a');
	}
	
	/**
	 * atoi 将字符串转为整数
	 * @param s 待转化的字符串
	 * @return 整数
	 * @throws invalidNumberException 非法字符串（含有负号和数字以外的字符）
	 */
	public static int atoi(String s) throws invalidNumberException {
		if(s==null || !(s.charAt(0)=='-' || s.charAt(0)>='0' && s.charAt(0)<='9'))throw new invalidNumberException(s);
		boolean minus = false;
		int x = 0;
		if(s.charAt(0)=='-')minus=true;
		else x+=s.charAt(0)-48;
		for(int i=1;i<s.length();i++) {
			if(s.charAt(i)>='0' && s.charAt(i)<='9') {
				x*=10;
				x+=s.charAt(i)-48;
			}
			else throw new invalidNumberException(s);
		}
		return minus?-x:x;
	}
	
	/**
	 * int转十六进制
	 * @param x 待转换的int
	 * @return 十六进制
	 * @throws WrongParameterException
	 */
	public static String intToHex(int x) throws WrongParameterException {
		if(x>=256)throw new WrongParameterException("intToHex()：x="+x+">=256");
		String s = "";
		s = s + DecToHex(x/16);
		s = s + DecToHex(x%16);
		return s;
	}
	
	/**
	 * 判断originStr里是否包含pattern子串
	 * @param pattern 待比对的子串内容
	 * @param originStr 原字符串
	 * @return true代表有，否则无
	 */
	public static boolean checkStr(String pattern, String originStr) {
		for(int i = 0; i <= originStr.length() - pattern.length(); i++) {
			if(originStr.substring(i, i+pattern.length()).equals(pattern))return true;
		}
		return false;
	}

	public static void printbytesformat(byte[] b) throws WrongParameterException {
		for(int i=0;i<b.length;i++) {
			System.out.print(intToHex(byteConvert(b[i]))+" ");
			if(i%16==7)System.out.print(" ");
			if(i%16==15)System.out.println();
		}
		System.out.println();
	}
}

