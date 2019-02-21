package parseFrames;

import java.util.Date;

import recvFrames.Listener;

public class flow {
	private Date startAt;
	private Date endAt;
	private int isAlive = 0;
	private int flowCounter = 0;
	public flow(EapolFrame f){
		startAt = new Date();
		flowCounter = Listener.flowCount;
		flowCounter++;
		init(f);
	} 
	/**
	 * 调用时这个流已经结束（检测到了FIN或RST包）
	 */
	public void end() {
		endAt = new Date();
		isAlive = 0;
	}
	public void init(EapolFrame f) {
		srcIP = f.getSrcIP();
		dstIP = f.getDstIP();
		srcPort = f.getSrcPort();
		dstPort = f.getDstPort();
	}
	
	private boolean isVideoStream = false;	//该变量用来判定本流是否视频流
	private int srcIP;
	private int dstIP;
	private int srcPort;
	private int dstPort;
	/**
	 * 判断一个数据包是否属于本数据流
	 * @param _si 调用数据包的源IP
	 * @param _di 调用数据包的目的IP
	 * @param _sp 调用数据包的源端口
	 * @param _dp 调用数据包的目的端口
	 * @return 是或者否
	 */
	public boolean isThisFlow(int _si, int _di, int _sp, int _dp) {
		if(srcIP!=_si)return false;
		if(dstIP!=_di)return false;
		if(srcPort!=_sp)return false;
		if(dstPort!=_dp)return false;
		return true;
	}
}
