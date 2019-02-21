package linkToRedis;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import exceptions.NotTCPException;
import exceptions.invalidNumberException;
import exceptions.redisConnectFailedException;
import general.commonFunctions;
import parseFrames.EapolFrame;
import recvFrames.Listener;
import ssl.CAcertification;

public class SingleTest {
	public static int packcounter=0;
	private static void printByteArray(byte []a) {
		for(int i=0;i<a.length;i++) {
			System.out.print((a[i]>=0?a[i]:256+a[i])+" ");
		}
		System.out.println();
	}
	/**
	 * 读文件
	 * @param fileName 文件名
	 * @param readNums 分析的数据帧数量，设为-1则读到文件底
	 */
	public static void readPackByBytes(String fileName, int readNums) {
		File file = new File(fileName);
		InputStream in = null;
		try {
			fetchAppIdentifyData a = new fetchAppIdentifyData(Listener.redisserver_host,Listener.redisserver_port,Listener.redisserver_pass);
			in = new FileInputStream(file);
			//头24字节
			byte [] pcapHeader = new byte[24];
			in.read(pcapHeader);
			//接下来每一段，开头16字节是包头（时间，大小）
			byte [] frameHeader = new byte[16];
			int byteread=0;
			while((byteread=in.read(frameHeader))!=-1) {
				packcounter++;
				int framebytes=0;
				framebytes = frameHeader[13] * 256 + (frameHeader[12]>=0?frameHeader[12]:(256+frameHeader[12]));
				byte [] frameTmp = new byte[framebytes];
				in.read(frameTmp);
				EapolFrame f;
				try {
					f = new EapolFrame(frameTmp, framebytes);
					//System.out.println("pack count="+packcounter+", type="+f.pickFlag());
					//下一行是视频流量检测测试
					if(f.videoFrameCharacter()) System.out.println("pack counter "+packcounter+": video stream detected.");
					//移动应用识别测试
					String dua = a.getFlowStatus(f.getSrcIP(), f.getSrcPort(), f.getDstIP(), f.getDstPort(), "dpiUserAgent");
					
					//System.out.println(dua);
					
					//server hello
					if(f.serverhello()) {
						//定位到Cert
						int shlen = f.getEM()[3]*256+f.getEM()[4];
						int ptr = shlen+5;
						if(shlen+5<f.getEM().length)if(f.getEM()[ptr+5]==11) {
							//certificate
							try {
								a.CAcertInit(f.getSrcIP(), f.getSrcPort(), f.getDstIP(), f.getDstPort(), (f.getEM()[ptr+3]*256+f.getEM()[ptr+4]));
								a.writeCAcert(f.getSrcIP(), f.getSrcPort(), f.getDstIP(), f.getDstPort(), new String(f.getEM()).substring(ptr));
								System.out.println("IP组："+f.getDstIP()+":"+f.getDstPort()+":"+ f.getSrcIP()+":"+f.getSrcPort()+"注册了CA证书");
							} catch (invalidNumberException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
					if(!f.serverhello() && a.checkHashExist(f.getSrcIP(), f.getSrcPort(), f.getDstIP(), f.getDstPort(), "CertWriting") && f.getSrcPort()==443 && f.getEM()!=null) {
						try {
							a.writeCAcert(f.getSrcIP(), f.getSrcPort(), f.getDstIP(), f.getDstPort(), new String(f.getEM()));
						} catch (invalidNumberException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				} catch (NotTCPException e) {
				}
				if(readNums>=0 && packcounter>=readNums)break;
				}
		}
		catch(IOException e) {
			e.printStackTrace();
		} catch (redisConnectFailedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		readPackByBytes("/home/aruyuna/data/bilibili-2.pcap", -1);
		//System.out.println(String.valueOf(-45135));
	}

}
