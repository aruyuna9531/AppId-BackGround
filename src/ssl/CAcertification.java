package ssl;

import exceptions.WrongParameterException;
import general.commonFunctions;

public class CAcertification {
	//Server Hello、Certificate、Server Key Exchange、 Server Hello Done四项揉到一组数据帧
	//其中Certificate段最长，包含CA证书内容，共计2000到7000字节不等，
	//而且Certificate直接跟在Server Hello的后面，并不单独开一个新的帧发送
	//Server Key Exchange和Server Hello Done也是直接接在Certificate后面，和Certificate的尾巴共用一个帧（除非中途超MTU）
	private int serverHelloBytes = 0;
	private int CertificateBytes = 0;
	private int CertificateBytesFilled = 0;
	private byte [] certificate;
	private int insertPointer = 0;
	public CAcertification(String s) {
		certificate = s.getBytes();
	}
	/**
	 * 往本流提供server hello段的一长串数据
	 * @param s TCP帧数据段（由服务器发往客户端的连续报文）
	 */
	public void addMessage(byte []s) {
		//此处s是不包含TCP头的TCP数据段（eapolMessage）
		if(serverHelloBytes==0) {
			//第一帧
			byte []shhead = new byte[5];
			//server hello 头部5字节，一般分别是16（22,代表SSL握手），03 03（将使用的SSL版本，此处代表SSL3.3，也就是TLS1.2），2字节的Server Hello段长度。
			for(int i=0;i<5;i++)shhead[i]=s[i];
			insertPointer += 5;
			serverHelloBytes = (shhead[3]>=0?shhead[3]:256+shhead[3])*256+(shhead[4]>=0?shhead[4]:256+shhead[4]);
			if(s.length>insertPointer+serverHelloBytes) {
				//如果s超出了server hello段
				insertPointer+=serverHelloBytes;
				byte[] cehead = new byte[5];							//可能有bug（s.length-insertPointer<5时）
				for(int i=0;i<5;i++)cehead[i]=s[i+insertPointer];
				insertPointer += 5;
				CertificateBytes = (cehead[3]>=0?cehead[3]:256+cehead[3])*256+(cehead[4]>=0?cehead[4]:256+cehead[4]);
				certificate = new byte[CertificateBytes];
				for(int i=0; i<s.length-insertPointer;i++) {
					certificate[i] = s[i+insertPointer];
				}
				CertificateBytesFilled += s.length-insertPointer;
			}
			else {
				
			}
		}
		else {
			//不是第一帧
			insertPointer = 0;
			if(CertificateBytes!=CertificateBytesFilled) {
				if(s.length+CertificateBytesFilled>CertificateBytes) {
					//该帧将会超出Certificate段，可以中途结束
					for(int i=0;i<CertificateBytes-CertificateBytesFilled; i++) {
						certificate[i+CertificateBytesFilled]=s[i];												//可能有bug（certificate为null时）
					}
					CertificateBytesFilled +=CertificateBytes-CertificateBytesFilled;
					certificate_division();
				}
				else {
					for(int i=0;i<s.length;i++) {
						certificate[i+CertificateBytesFilled]=s[i];	
					}
					CertificateBytesFilled +=s.length;
				}
			}
		}
	}
	//分解CA Certificate段
	public void printCA() {
		System.out.println(certificate);
	}
	public void certificate_division() {
		//找一下a0 03 02 01 02 02，CA证书标志点
		String castr = new String(certificate);
		try {
			commonFunctions.printbytesformat(certificate);
		} catch (WrongParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int start=0;
		for(int i=15;i<30;i++) {
			if(certificate[i]==0xa0
					&& certificate[i+1]==03
					&& certificate[i+2]==02
					&& certificate[i+3]==01
					&& certificate[i+4]==02
					&& certificate[i+5]==02
					) {
				start = i+6;
				break;
			}
		}
		//serial number
		start += certificate[start]+1;
		//signature
		start += certificate[start+1]+2;
		//issuer
		start += certificate[start+1]+2;
		//validity
		start += 32;
		//2B subject length（30 xx）
		start += 2;
		int subjectLength = certificate[start-1];
		int filled = 0;
		//子项（31 xx）
		while(filled<subjectLength) {
			int sslen = certificate[start+filled+1];
			if(certificate[start+filled+8]==10) {
				//organizationName
				System.out.println("CA证书持有单位："+castr.substring(start+filled+8+3, start+filled+8+3+certificate[start+filled+10]));
			}
			else if(certificate[start+filled+8]==3) {
				//commonName
				System.out.println("CA证书域名："+castr.substring(start+filled+8+3, start+filled+8+3+certificate[start+filled+10]));
			}
			filled += 2+sslen;
		}
	}
}
