package recvFrames;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Set;

import exceptions.*;
import general.commonFunctions;
import linkToRedis.fetchAppIdentifyData;
import parseFrames.EapolFrame;
import parseFrames.flow;
import recvFrames.Listener;

public class SocketThread implements Runnable{
	Socket sin;
	Socket sout;
	fetchAppIdentifyData f;
	public SocketThread(Socket _s){
		sin=_s;
	}
	char toHex(int i) {
		if(i<10)return (char)(i+48);
		else return (char)(i+87);
	}
	
	void printPack(byte[] b, int catchLen) {
		char c[]=new char[3];
		c[2]=' ';
		for(int i=0;i<catchLen;i++) {
			if(i%16==0)System.out.println();
			int toInt = commonFunctions.byteConvert(b[i]);
			c[0]=toHex(toInt/16);
			c[1]=toHex(toInt%16);
			System.out.print(c);
		}
		System.out.println();
	}
	int strncmp(String a, String b, int len) {
		return a.substring(0, len).compareTo(b.substring(0, len));
	}
	int strlen(String str) {
		return str.length();
	}
	int getIntFromByte(byte[] s, int startAt) {
		int ret=0;
		for(int i=startAt; i<startAt+4;i++) {
			ret = ret<<8;
			ret += commonFunctions.byteConvert(s[i]);
		}
		return ret;
	}
	public EapolFrame pack_analysis(byte[] pktStr, int read_bytes) throws NotTCPException {
		// 用EapolFrame对象解析这个帧
		EapolFrame f = new EapolFrame(pktStr, read_bytes);
		//f.printFrame();
		if(f.videoFrameCharacter())System.out.println("Video flow detected.");
		if(f.streaming()<0)Listener.flowAcc_downstream += Math.abs(f.streaming());
		else Listener.flowAcc_upstream += f.streaming();
		return f;
	}
	public void run() {
		System.out.println("检测到新的连接");
		try {
			/**
			 * 1.15更新：发包到云后台 Part 1
			 */
			f = new fetchAppIdentifyData("ras.sysu.edu.cn", 9037, "smartap");	//从redis获取活动节点
			String[] sp = f.getOneCloudServerAddress().split(":");	//随机获得的活动节点
			sout = new Socket(sp[0], Integer.parseInt(sp[1]));	//与这个服务器建立连接
			System.out.println("成功连接到云服务器："+sp[0]+":"+sp[1]+"，发包准备就绪");
			DataOutputStream os = new DataOutputStream(sout.getOutputStream());
			
			//获取输入流
			InputStream inputStream = sin.getInputStream();
			int read_bytes=0;
			byte[] c_len = new byte[8];
			int pack_count=0;
			while(true) {
				/*读8字节的自定义包头（4字节包类型+4字节包长度）*/
				int headRead=0;
				while(headRead!=8) {
					int sred = inputStream.read(c_len, headRead, 8-headRead);
					if(sred == -1) throw new PacketReceiveFailedException("包头接收失败，可能路由器已经断开对本机的链接，或者路由器没有联网");
					headRead += sred; 
				}
				read_bytes = headRead;
				//前8个字节是包头
				pack_count++;
				/**
				这几行是打印自定义包头内容的，调试时用
				System.out.print("Head bytes(Length = "+read_bytes+"):");
				for(int tmp_c = 0; tmp_c<read_bytes;tmp_c++) {
					System.out.print(c_len[tmp_c]+",");
				}
				System.out.println();
				*/
				@SuppressWarnings("unused")
				int catchType = getIntFromByte(c_len, 0);			//暂时没有什么作用的前4字节（包类型），左边的灯泡真多余（
				int catchLen = getIntFromByte(c_len, 4);			//包长度（重要）
				/**
				 * 这行打印包头的解析结果
				System.out.println("cap:"+pack_count+", length="+ catchLen + ", type: "+ catchType);
				 */
				byte[] pktStr = new byte[catchLen];
				int buff=0;
				/*读这1个包的内容*/
				while(buff!=catchLen){
					int sget = inputStream.read(pktStr, buff, catchLen-buff);
					if(sget == -1)throw new PacketReceiveFailedException("包体接收失败（这也太扯淡了吧会有这个出现吗）");
					buff += sget;
				}
				read_bytes = buff;
				if(catchLen>1514) {
					//以太网MTU限制，IP头+TCP头+数据段最大是1500B,再加数字链路层头就是1514B，超过这个数字的都是非主流以太网包
					throw new PacketTooLongException(catchLen);
				}

				//打印包的信息（包括帧长度，MAC流向，IP流向，数据段内容。调试用。）
				EapolFrame f = pack_analysis(pktStr, read_bytes);
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.err.println("IO错误");
			e.printStackTrace();
		} catch (PacketTooLongException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PacketReceiveFailedException e) {
			e.printStackTrace();
			try {
				sout.close();
			} catch (IOException e1) {
					// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} catch (redisConnectFailedException e) {
			e.printStackTrace();
		} catch(Exception e) {
			System.err.println("未知错误（详细错误类型见下方，有必要的话在packet_analysis方法内解开f.printFrame()的注释参考调试）");
			e.printStackTrace();
		}
		finally {
			System.out.println("抓包出错，抓包线程终止");
		}
	}
}

