package parseFrames;

import exceptions.NotTCPException;
import exceptions.WrongParameterException;
import exceptions.invalidNumberException;
import exceptions.redisConnectFailedException;
import general.commonFunctions;
import linkToRedis.fetchAppIdentifyData;
import recvFrames.Listener;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Stack;

public class EapolFrame {
	private int packetLength = 0;	// 包的长度
	private long srcMac = 0;		// 源Mac
	private long dstMac = 0;		// 目的Mac
	private int IPHeadLength = 20;	// IP头长度
	private int Protocol = 0;		// 协议类型（6=TCP，11=UDP）
	private int srcIP = 0;			// 源IP
	private int dstIP = 0;			// 目的IP
	private int TCPHeadLength = 0;	// TCP头长度
	private int srcPort = 0;		// 源端口
	private int dstPort = 0;		// 目的端口
	private byte[] eapolMessage;	// 数据段
	
	//帧标记
	private boolean ACK = false;
	private boolean PSH = false;
	private boolean SYN = false;
	private boolean FIN = false;
	private boolean RST = false;
	private int seqNum = 0;
	private int ackNum = 0;
	
	public EapolFrame(byte[] pack, int read_bytes) throws NotTCPException {
		if(pack[23]!=6)throw new NotTCPException();
		packetLength = read_bytes;
		IPHeadLength = (commonFunctions.byteConvert(pack[14])%16) << 2;
		Protocol = pack[14+9];
		TCPHeadLength = commonFunctions.byteConvert(pack[46]) >> 2; 
		commonFunctions.realtimeBytes += packetLength;
		byte Flag = pack[47];
		if((Flag & 0x10) != 0)ACK = true;
		if((Flag & 0x8) != 0)PSH = true;
		if((Flag & 0x4) != 0)RST = true;
		if((Flag & 0x2) != 0)SYN = true;
		if((Flag & 0x1) != 0)FIN = true;
		
		seqNum = commonFunctions.byteConvert(pack[0x26]) * 256 * 256 * 256 
				+ commonFunctions.byteConvert(pack[0x27]) * 256 * 256
				+ commonFunctions.byteConvert(pack[0x28]) * 256
				+ commonFunctions.byteConvert(pack[0x29]);

		ackNum = commonFunctions.byteConvert(pack[0x2a]) * 256 * 256 * 256 
				+ commonFunctions.byteConvert(pack[0x2b]) * 256 * 256
				+ commonFunctions.byteConvert(pack[0x2c]) * 256
				+ commonFunctions.byteConvert(pack[0x2d]);
		
		
		try {
			dstMac = macTransfer(pack, 0);
			srcMac = macTransfer(pack, 6);
			srcIP = ipTransfer(pack, 14+12);
			dstIP = ipTransfer(pack, 14+16);
			srcPort = portTransfer(pack, 14+20);
			dstPort = portTransfer(pack, 14+22);
		} catch (WrongParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(packetLength-(14+IPHeadLength+TCPHeadLength)>0) {
			eapolMessage = new byte[packetLength-(14+IPHeadLength+TCPHeadLength)];
			for(int i=0; i<packetLength-(14+IPHeadLength+TCPHeadLength); i++)
				eapolMessage[i]=pack[14+IPHeadLength+TCPHeadLength+i];
		}
		else eapolMessage=null;
		
		if(SYN) {
				fetchAppIdentifyData f = new fetchAppIdentifyData();
				//记录基础seq，ack用于跟踪数据流
				if(!ACK)f.writeFlowStatus(srcIP, srcPort, dstIP, dstPort, "basicClientSeq", String.valueOf(seqNum));
				else f.writeFlowStatus(srcIP, srcPort, dstIP, dstPort, "basicServerSeq", String.valueOf(seqNum));
			
		}
		
		autoSaveGet();
		appIdentificationFromHTTP();
	}
	private long macTransfer(byte[] pack, int startAt) throws WrongParameterException {
		// 取得帧的mac：startAt～+6Bytes
		if(startAt>=14)throw new WrongParameterException("mac取址"+startAt);
		long res = 0;
		for(int i=startAt;i<startAt+6;i++) {
			res = res << 8;
			res += commonFunctions.byteConvert(pack[i]);
		}
		return res;
	}
	private int ipTransfer(byte[] pack, int startAt) throws WrongParameterException {
		if(startAt<14 || startAt>=14+IPHeadLength)throw new WrongParameterException("ip取址"+startAt);
		int res = 0;
		for(int i=startAt;i<startAt+4;i++) {
			res = res << 8;
			res += commonFunctions.byteConvert(pack[i]);
		}
		return res;
	}
	private int portTransfer(byte[] pack, int startAt) throws WrongParameterException {
		if(startAt<14+IPHeadLength || startAt>=14+IPHeadLength+TCPHeadLength)throw new WrongParameterException("端口取址"+startAt);
		int res = 0;
		for(int i=startAt;i<startAt+2;i++) {
			res = res << 8;
			res += commonFunctions.byteConvert(pack[i]);
		}
		return res;
	}
	private String mac_StringVer(long val) throws WrongParameterException {
		// mac：bytesFlag=6， ip：bytesFlag=4， port： bytesFlag=2
		String s = "";
		Stack<Integer> str = new Stack<Integer>();
		for(int i=0;i<6;i++) {
			str.push((int)(val % 256));
			val = val / 256;
		}
		boolean first = true;
		while(str.isEmpty()!=true) {
			if(!first)s = s + ":" + commonFunctions.intToHex(str.pop()); 
			else{
				s = s + commonFunctions.intToHex(str.pop());
				first = false;
			}
			
		}
		return s;
	}
	private void autoSaveGet() {
		if(eapolMessage==null || eapolMessage.length<3)return;
		if(eapolMessage[0]=='G' && eapolMessage[1]=='E' && eapolMessage[2]=='T') {
				fetchAppIdentifyData f = new fetchAppIdentifyData();
				f.writeApplyMessage(srcIP, srcPort, dstIP, dstPort, new String(eapolMessage));
		}
	}
	/**
	 * 打印这个帧
	 */
	public void printFrame() {
		try {
			commonFunctions.dateLog();
			System.out.println("帧长度："+packetLength+"（IP头："+IPHeadLength+"，TCP头："+TCPHeadLength+"）"+"\n");
			System.out.println("MAC流向："+ mac_StringVer(srcMac) + "->" + mac_StringVer(dstMac)+"\n");
			System.out.println("IP流向："+
					(srcIP>>>24)+"."+
					((srcIP & 0x00FF0000)>>>16)+"."+
					((srcIP & 0x0000FF00)>>>8)+"."+
					(srcIP & 0x000000FF)+":"+srcPort
					+ "->" +
					(dstIP>>>24)+"."+
					((dstIP & 0x00FF0000)>>>16)+"."+
					((dstIP & 0x0000FF00)>>>8)+"."+
					((dstIP & 0x000000FF))+":"+dstPort
					+"\n");
			if(eapolMessage!=null) {
				System.out.println("eapol信息：\n");
				for(int i=0;i<eapolMessage.length;i++) {
					if(i%32==0)System.out.print("\nLine:"+(i/32+1)+" ");
					System.out.print(commonFunctions.intToHex(commonFunctions.byteConvert(eapolMessage[i]))+" ");
				}
				System.out.println();
			}
			
		} catch (WrongParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/**
	 * 取flag
	 * @return 第一次握手返回tcpHS1，第二次返回tcpHS2，第三次返回tcpHS3，数据传输PSH，四次挥手finalHandshake（1,3,4。2比较麻烦不作处理），ack就ack，其他类型unknown
	 */
	public String pickFlag() {
		//SYN
		if(SYN && !ACK) return "tcpHS1";
		if(SYN && ACK) return "tcpHS2";
		if(RST) {
			fetchAppIdentifyData f;
			try {
				f = new fetchAppIdentifyData();
				f.deleteFlow(srcIP, srcPort, dstIP, dstPort);
			} catch (redisConnectFailedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return "reset";
		}
		if(FIN) {
				fetchAppIdentifyData f = new fetchAppIdentifyData();
				if(!f.checkHashExist(srcIP, srcPort, dstIP, dstPort, "finalSeq")) {
					f.finalSequenceSet(srcIP, srcPort, dstIP, dstPort, seqNum, ackNum);
					return "finalHandshake1";
				}
				else {
					return "finalHandshake3";
				}
		}
		if(ACK) {
			try {
				fetchAppIdentifyData f = new fetchAppIdentifyData();
				int bseq = commonFunctions.atoi(f.getFlowStatus(srcIP, srcPort, dstIP, dstPort, "basicClientSeq"));
				int back = commonFunctions.atoi(f.getFlowStatus(srcIP, srcPort, dstIP, dstPort, "basicServerSeq"));
				if(seqNum-bseq==1 && ackNum-back==1) {
					if(!PSH)
					{
						f.writeFlowStatus(srcIP, srcPort, dstIP, dstPort, "handshakeComplete", "yes");
						return "tcpHS3";
					}
					else {
						//建立三次握手后的第一帧，可以确定是HTTP，SSL或者其他流
						//SSL（16）
						String emsg = new String(eapolMessage);
						if(eapolMessage[0]==0x16) {
							f.writeFlowStatus(srcIP, srcPort, dstIP, dstPort, "flowType", "SSL");
							return "firstFrame(SSL)";
						}
						else for(int i = 0; i < HTTPrequests.length; i++) {
							if(emsg.substring(0, HTTPrequests[i].length()).compareTo(HTTPrequests[i])==0) {
								f.writeFlowStatus(srcIP, srcPort, dstIP, dstPort, "flowType", "HTTP");
								return "firstFrame(HTTP)-"+HTTPrequests[i];
							}
						}
						return "firstFrame(others)";
					}
				}
				else{
					if(f.finalHandshakeCompleteCheck(srcIP, srcPort, dstIP, dstPort, seqNum, ackNum)) {
						System.err.println("flow over.");
						return "finalHandshake4";
					}
					return "ACK";
				}
			} catch (redisConnectFailedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return "ACK";
			} catch (invalidNumberException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return "ACK";
			}
		}
		return "unknown";
	}
	
	// TODO: 其他的帧处理函数
	/**
	 * 从本数据帧内可以判断出是视频流的帧
	 * @return 成功写入返回yes，否则no
	 */
	private boolean videoStreamVerified() {
			fetchAppIdentifyData f = new fetchAppIdentifyData();
			f.writeFlowStatus(srcIP, srcPort, dstIP, dstPort, "isVideoStream", "yes");
			return true;
	}
	
	/**
	 * HTTP视频流识别（DPI）
	 * @return 是返回true，同时把视频流识别结果写到redis，否返回false
	 */
	public boolean videoFrameCharacter() {
		if(eapolMessage==null)return false;
		fetchAppIdentifyData f = new fetchAppIdentifyData();
		String emsg = new String(eapolMessage);
		String s = "HTTP/1.1 206 Partial Content";
		String t = "Content-Type: ";
		int ptrpos = 0;					//字符串比较定位光标
		if(emsg.length()<s.length()+t.length())return false;
		//比对开头是否符合字符串s
		if(emsg.substring(0, s.length()).compareTo(s)==0) {
			for(int i=s.length();i<emsg.length()-t.length();i++) {
				//定位到Content-Type位置
				if(emsg.substring(i, i+t.length()).compareTo(t)==0) {
					//此时定位到了i处，i就是Content-Type的C位置。取得Content-Type内容
					ptrpos = i+t.length();
					break;
				}
				
			}
			//此时ptrpos定位到Content-Type的第一个字节
			if(emsg.substring(ptrpos, ptrpos+5).compareTo("video")==0) {
				//Content-Type是video，可以确定为视频流
				videoStreamVerified();
				//此时该帧的目标mac地址为终端mac地址，更新信息
				try {
					f.writeVideoStreamByMac(this.mac_StringVer(dstMac));
				} catch (WrongParameterException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return true;
			}
			//不是video，查找octet-stream
			if(emsg.substring(ptrpos, ptrpos+"application/octet-stream".length()).compareTo("application/octet-stream")==0) {
				//是octet-stream，需要返回客户端的GET请求分析
					String getstr = f.getApplyMessage(srcIP, srcPort, dstIP, dstPort);
					if(getstr==null) {
						return false;
					}
					else if(getstr.substring(0, 3).compareTo("GET")==0) {
						//是一个get请求，提取资源字段（文件名）
						String []url = getstr.split(" ");
						//资源主体以问号结束，或者没有问号，则一整串都是主体
						String []urlsplit = url[1].split("[?]");
						//此时urlsplit[0]是url主体
						//剔除多余路径，保留文件名（以/分隔）
						String []docsplit = urlsplit[0].split("[/]");
						//此时docsplit的最后一项是文件名
						//分解出后缀（以.分隔）
						String []dtypesplit = docsplit[docsplit.length-1].split(".");
						if(dtypesplit.length==0)return false;
						else
						//dtypesilit的最后一项是后缀名，比对后缀库
						if(f.videoTypeVerify(dtypesplit[dtypesplit.length-1])) {
							videoStreamVerified();
							try {
								f.writeVideoStreamByMac(this.mac_StringVer(dstMac));
							} catch (WrongParameterException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							return true;
						}
					}
			}
			//不是octet-stream，不是视频流量
		}
		return false;
	}
	
	/**
	 * 单帧显性识别应用类型，并标记该流，后续该流的数据包都会归为这个app（DPI）
	 * @return 应用名字
	 */
	private String HTTPrequests[] = {"GET","HEAD","POST","PUT","DELETE","CONNECT","OPTIONS","TRACE"};
	public String appIdentificationFromHTTP() {
		if(eapolMessage==null)return null;
		String emsg = new String(eapolMessage);
		String need1 = "User-Agent: ";
		String need2 = "Host: ";
		for(int i=0;i<emsg.length()-need1.length();i++) {
			if(emsg.substring(i, i+need1.length()).compareTo(need1)==0) {
				String []ua = emsg.substring(i+need1.length()).split("\r\n");
					fetchAppIdentifyData f = new fetchAppIdentifyData();
					f.writeFlowStatus(srcIP, srcPort, dstIP, dstPort, "dpiUserAgent", ua[0]);
					try {
						if(!ua[0].substring(0, 6).equals("Dalvik"))f.writeAppId(this.mac_StringVer(srcMac), ua[0]);
					} catch (WrongParameterException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				return ua[0];
			}
		}
		for(int i=0;i<emsg.length()-need2.length();i++) {
			if(emsg.substring(i, i+need2.length()).compareTo(need2)==0) {
				String []spl = emsg.split("\r\n");
					fetchAppIdentifyData f = new fetchAppIdentifyData();
					f.writeFlowStatus(srcIP, srcPort, dstIP, dstPort, "dpiHost", spl[0]);
					return f.getIdByHost(spl[0]);
			}
		}
		return null;
	}

	public boolean serverhello() {
		if(eapolMessage!=null)return (srcPort == 443 && eapolMessage[0]==0x16 && eapolMessage[5]==0x02);
		return false;
	}
	/**
	 * 统计流量（暂时没什么用）
	 * @return
	 */
	public int streaming() {
		if(eapolMessage==null)return 0;
		if(srcPort<1024)/*downStreaming*/return -eapolMessage.length;
		else /*upStreaming*/return eapolMessage.length;
	}
	
	
	/*以下getter和setter*/
	public int getSrcIP() {
		// TODO Auto-generated method stub
		return srcIP;
	}
	public int getDstIP() {
		// TODO Auto-generated method stub
		return dstIP;
	}
	public int getSrcPort() {
		// TODO Auto-generated method stub
		return srcPort;
	}
	public int getDstPort() {
		// TODO Auto-generated method stub
		return dstPort;
	}
	public byte[] getEM() {
		return eapolMessage;
	}
	/**
	 * 把帧中可识别的状态写到缓存
	 */
	public void statusWrite() {
		
	}
}
