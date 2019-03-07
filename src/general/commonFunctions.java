/**
 * 一些全局的处理函数
 */

package general;

import exceptions.WrongParameterException;
import exceptions.invalidNumberException;
import exceptions.redisConnectFailedException;
import linkToRedis.fetchAppIdentifyData;
import parseFrames.EapolFrame;
import recvFrames.Listener;

public class commonFunctions {
	private static fetchAppIdentifyData a= new fetchAppIdentifyData(Listener.redisserver_host,Listener.redisserver_port,Listener.redisserver_pass);;
	public static int byteConvert(byte b) {
		return b>=0?b:(256+b);
	}
	public static byte[] pickbytes(byte[] ori, int start, int end) {
		byte[] tmp=new byte[end-start];
		for(int i=0;i<end-start;i++)tmp[i]=ori[i+start];
		return tmp;
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
	 * @throws invalidNumberException null或者非法字符串（含有负号和数字以外的字符）
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

	private static char[] toLineArr(int lines) {
		int c[]=new int[3];
		c[0]=lines/16/16;
		lines%=16*16;
		c[1]=lines/16;
		lines%=16;
		c[2]=lines;
		char[] pp = new char[5];
		for(int i=0;i<3;i++)
			try {
				pp[i]=DecToHex(c[i]);
			} catch (WrongParameterException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		pp[3]='0';
		pp[4]=' ';
		return pp;
	}
	public static void printbytesformat(byte[] b) throws WrongParameterException {
		System.out.print("0000 ");
		int lcounter=0;
		for(int i=0;i<b.length;i++) {
			System.out.print(intToHex(byteConvert(b[i]))+" ");
			if(i%16==7)System.out.print(" ");
			if(i%16==15) {
				System.out.println();
				System.out.print(toLineArr(++lcounter));
			}
		}
		System.out.println();
	}
	public static void printbytesformat(String s) throws WrongParameterException {
		printbytesformat(s.getBytes());
	}
	public static void printbytesformat(StringBuffer s) throws WrongParameterException {
		printbytesformat(s.toString());
	}
	
	/**
	 * 接收数据包时调用，打印可提取的视频与终端信息。
	 * @param f 数据包的内容
	 * @throws redisConnectFailedException
	 */
	public static void mainFunc(EapolFrame f) throws redisConnectFailedException {
		//下一行是视频流量检测测试
		if(f.videoFrameCharacter()) System.out.println("检测到视频流量：");
		//移动应用识别测试（这里从redis缓存里拿数据）
		String dua = a.getFlowStatus(f.getSrcIP(), f.getSrcPort(), f.getDstIP(), f.getDstPort(), "dpiUserAgent");
		if(dua!=null)System.out.println("当前流量指向的终端信息："+dua);
		
		//server hello
		int ptr = 0;
		if(f.serverhello()) {
			//定位到Cert
			byte[] b = f.getEM();
			int shlen = b[3]*256+b[4];
			ptr = shlen+5;
			if(shlen+5<b.length)if(b[ptr+5]==11) {
				//certificate
				try {
					a.CAcertInit(f.getSrcIP(), f.getSrcPort(), f.getDstIP(), f.getDstPort(), (b[ptr+3]*256+b[ptr+4]));
					byte[] scert = new byte[b.length-ptr-5];
					for(int i=0;i<b.length-ptr-5;i++)scert[i]=b[i+ptr+5];
					
					a.writeCAcert(f.getSrcIP(), f.getSrcPort(), f.getDstIP(), f.getDstPort(), scert);
					System.out.println("IP组："+f.getDstIP()+":"+f.getDstPort()+":"+ f.getSrcIP()+":"+f.getSrcPort()+"注册了CA证书");
				} catch (invalidNumberException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		if(!f.serverhello() && a.checkHashExist(f.getSrcIP(), f.getSrcPort(), f.getDstIP(), f.getDstPort(), "CertWriting") && f.getSrcPort()==443 && f.getEM()!=null) {
			try {
				a.writeCAcert(f.getSrcIP(), f.getSrcPort(), f.getDstIP(), f.getDstPort(), f.getEM());
			} catch (invalidNumberException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}

