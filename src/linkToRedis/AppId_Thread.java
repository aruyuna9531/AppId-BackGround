package linkToRedis;

import java.util.HashMap;

import exceptions.redisConnectFailedException;

/**
 * AppId_Thread
 * 读缓存线程，定期从redis中读识别结果数据
 * 对象定义时入参可以有3种形式：
 * （1）无参，使用默认的参数（实验用参数）
 * （2）2个参数（redis缓存所在的IP（或域名）和端口，此时被认为无密码验证）
 * （3）3个参数（除IP和端口外第3个参数为密码，用于有密码验证）
 * @author aruyuna
 *
 */
public class AppId_Thread implements Runnable{
	
	private String redis_host = "";
	private int redis_port = 0;
	private String password = "";
	
	public AppId_Thread() {
		redis_host="ras.sysu.edu.cn";
		redis_port=9037;
		password="smartap";
	}

	public AppId_Thread(String _host, int _port) {
		redis_host = _host;
		redis_port = _port;
		password = null;
	}
	
	public AppId_Thread(String _host, int _port, String _pass) {
		redis_host = _host;
		redis_port = _port;
		password = _pass;
	}
	
	public void run() {
			fetchAppIdentifyData IdCache = new fetchAppIdentifyData();
			HashMap<String, String> map; 
			while(true) {
				//进程每1秒动1次
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				map = IdCache.getResultFromCloud();
				//输出从redis读取的内容（测试）
				/*
				System.out.println("当前识别集合:[");
				for(String s: map.keySet()) {
					System.out.println("Mac="+s+", App="+map.get(s));
				}
				System.out.println("]");
				*/
				// TODO 
				/**
				 * 每1秒将会从redis读取1次数据（需要防止Key过多造成读取缓慢问题，这里应该用epoll方式，如果找到java实现epoll的方法，再改）
				 * 哈希表map变量保存了每次读取的终端Mac（作为Key）和识别结果（作为value）
				 * 如果数据要往外传，相关的代码写到这里
				 */
				
			}
	}
}
