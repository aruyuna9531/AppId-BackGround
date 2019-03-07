package linkToRedis;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import exceptions.NotTCPException;
import exceptions.WrongParameterException;
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
					System.out.println("数据包编号="+packcounter+", 数据包类型="+f.pickFlag());
					commonFunctions.mainFunc(f);
				} catch (NotTCPException e) {
				} catch (redisConnectFailedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(readNums>=0 && packcounter>=readNums)break;
				}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		readPackByBytes("/home/aruyuna/data/youku-demo1.pcap", -1);
		//System.out.println(String.valueOf(-45135));
	}

}
