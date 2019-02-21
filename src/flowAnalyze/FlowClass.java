package flowAnalyze;

public class FlowClass {
	private int flowID = -1;				//数据流ID
	private int srcIP[] = new int[4];		//源IP
	private int dstIP[] = new int[4];		//目的IP
	private int srcPort = 0;
	private int dstPort = 0;
	
	private boolean isVideoStream = false;	//该流是视频流
	private String dpiUserAgent = "";		//如果有明文HTTP头，记录请求段User Agent字段
	private String dpiHost = "";			//如果有明文HTTP头，记录Host字段
	private String CAOwner = "";			//如果是SSL流，记录CA证书持有者
	FlowClass(int _srcIP[], int _dstIP[], int _srcPort, int _dstPort){
		//从redis获取flowID
		
		//复制四元组
		
	}
	
}
