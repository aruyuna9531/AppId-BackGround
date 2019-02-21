package recvFrames;


import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import recvFrames.SocketThread;
import linkToRedis.AppId_Thread;

public class Listener {
	//两个监控流量变化的变量（暂时没有使用）
	public static int flowAcc_upstream = 0;
	public static int flowAcc_downstream = 0;
	public static int flowCount = 0;
	
	//缓存基本信息
	public static String redisserver_host = "ras.sysu.edu.cn";
	public static int redisserver_port = 9037;
	public static String redisserver_pass = "smartap";
	
	//注册云上的计算机接包节点
	public static final String []defaultCloudServerList = {"ras.sysu.edu.cn:9035"};	//注册的节点表（不一定是当前能用的），有新的话可以往里加
		
	public static void background_Listener(){
		//线程池
		ExecutorService threadPool = Executors.newFixedThreadPool(100);	//100线程池容量，后面所有的线程都扔进去执行，每接收一个连接会占一个位置
		
		//统计流量功能。使用待定。
		//Runnable fac = new flow_counter();
		//threadPool.submit(fac);
		
		/**
		 * 从云服务器抓识别数据的线程，目前的功能是隔1秒从服务器抓1次识别结果并输出
		 * 如果要对抓到的识别结果作处理，需要点进AppId_Thread内补充run()方法的内容
		 * 构造函数一共有3种形式，详细见import类AppId_Thread上悬放指针时的解释
		 */
		Runnable IdThread = new AppId_Thread(redisserver_host, redisserver_port, redisserver_pass);
		threadPool.submit(IdThread);
		
		/**
		 * 从路由器接收数据包的线程
		 */
		try {
			int port=8084;			//本机接收包用的Socket端口
			ServerSocket server = new ServerSocket(port);
			// 等待链接到来
			System.out.println("已开启端口："+port+"，等待路由器的连接");
			
			while(true) {
				Socket socket = server.accept();
				Runnable runnable = new SocketThread(socket);
				threadPool.submit(runnable);
			}
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
				
		}
	}
	public static void main(String args[]){
		background_Listener();
	}
}

