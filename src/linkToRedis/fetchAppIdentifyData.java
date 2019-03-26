package linkToRedis;

import java.util.HashMap;
import java.util.Set;

import exceptions.WrongParameterException;
import exceptions.invalidNumberException;
import exceptions.redisConnectFailedException;
import general.commonFunctions;
import redis.clients.jedis.Jedis;
import ssl.CAcertification;

public class fetchAppIdentifyData {
	private int expire = 60;
	private Jedis jedis = null;
	private String server_host = "ras.sysu.edu.cn";
	private int server_port = 9037;
//	
//	public fetchAppIdentifyData(){
//		server_host="ras.sysu.edu.cn";
//		server_port=9037;
//		try {
//			connectToRedis("ras.sysu.edu.cn", 9037, "smartap");
//		} catch (redisConnectFailedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
//
//	public fetchAppIdentifyData(String _host, int _port){
//		server_host=_host;
//		server_port=_port;
//		try {
//			connectToRedis(server_host, server_port, null);
//		} catch (redisConnectFailedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
//	
//	public fetchAppIdentifyData(String _host, int _port, String _password){
//		server_host=_host;
//		server_port=_port;
//		try {
//			connectToRedis(server_host, server_port, _password);
//		} catch (redisConnectFailedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
//	
	/**
	 * 连接到云redis（私有方法，在构造函数使用）
	 * @param server_host 云redis的域名/IP
	 * @param server_port 云redis的端口
	 * @param auth_password 需要认证时提供的的密码（没有则为null）
	 * @throws redisConnectFailedException 连接失败抛出异常
	 */
//	private void connectToRedis() throws redisConnectFailedException {
//		/**
//		 * 连接到redis
//		 */
//		jedis = JedisPoolClient.getJedis();
//		/**
//		 * 如果redis使用了密码验证，那么需要调用auth()进行验证。
//		 * 没有进行验证会抛出NOAUTH Authentication required异常
//		 * 密码错误会抛出ERR invalid password异常
//		 */
//		try {
//			jedis.ping();
//		}catch(Exception e) {
//			System.err.println("连接到redis失败，从云抓取识别结果功能将不可用。可能的原因有以下1种或多种：\n"
//					+ "1.redis服务未开启，需要启动\n"
//					+ "2.redis相关初始化参数有误，需要检查");
//			System.err.println("3.密码错误，需要修改，如果不需要密码验证，请删除第3个参数。不知道密码的请咨询管理员");
//			System.err.println("4.redis设置了保护模式且没有密码验证，这是只能从云内部访问的Bug，仍然要咨询管理员");
//			if(jedis!=null){JedisPoolClient.returnResource(jedis);jedis=null;}
//			throw new redisConnectFailedException(server_host+":"+server_port);
//		}finally {
//			
//		}
//	}
	
	/**
	 * 连接到应用识别云后台获取数据
	 * @return 包含Mac：App的Map
	 */
	public HashMap<String, String> getResultFromCloud() {
		if(jedis==null)jedis = JedisPoolClient.getJedis();
		HashMap<String, String> AppIdContainers = new HashMap<String, String>();
		/**
		 * 尝试读写redis。keys()代表keys命令（取得拥有特定模式的键，*号代表任意字符串）
		 * 如果无法读写，可能启动了protected-mode，此时抛出DENIED Redis is running in protected mode
		 * 如果没抛出异常则可以读取
		 * 读取一个不存在的键会返回null
		 * 用get(key, value)从redis读键，smartAp后台不需要往redis写键
		 * */
		Set<String> MacList = jedis.keys("AppId:*");		//keys()返回值是Set<String>类型，需要引入Set包
		/**
		 * 用get(key, value)从redis读键，smartAp后台不需要往redis写键
		 */
		for(String s: MacList) {
			AppIdContainers.put(s.substring(6), jedis.get(s));
		}
		/**
		 * 关闭redis连接
		 */
		if(jedis!=null){
						JedisPoolClient.returnResource(jedis);jedis=null;}
		return AppIdContainers;
	}
	/**
	 * 从redis服务器中随机返回一个可用的云端接收节点地址
	 * @return 随机抽到的地址
	 */
	public String getOneCloudServerAddress() {
		if(jedis==null)jedis = JedisPoolClient.getJedis();
		String ret = jedis.srandmember("cloudServerList");		//获取活动服务器列表
		if(jedis!=null){
						JedisPoolClient.returnResource(jedis);jedis=null;}
		return ret;
	}
	
	//
	/**
	 * 输出内容到特定的哈希表（以flow:sip:sport:dip:dport的形式）
	 * @param sip 源ip（可能是目的Ip)
	 * @param sport 源端口
	 * @param dip 目的Ip
	 * @param dport 目的端口
	 * @param field 下属属性
	 * @param value 值
	 * @return 没有异常返回true
	 * @throws redisConnectFailedException redis链接失败
	 */
	public boolean writeFlowStatus(int sip, int sport, int dip, int dport, String field, String value) {
		if(jedis==null)jedis = JedisPoolClient.getJedis();
		
		if(jedis.exists("flow:"+sip+":"+sport+":"+dip+":"+dport) || jedis.exists("flow:"+dip+":"+dport+":"+sip+":"+sport)) {
			if(jedis.exists("flow:"+sip+":"+sport+":"+dip+":"+dport)) {
				jedis.hset("flow:"+sip+":"+sport+":"+dip+":"+dport, field, value);
				jedis.expire("flow:"+sip+":"+sport+":"+dip+":"+dport, expire);
				
			}
			else {
				jedis.hset("flow:"+dip+":"+dport+":"+sip+":"+sport, field, value);
				jedis.expire("flow:"+dip+":"+dport+":"+sip+":"+sport, expire);
			}
		}
		else {
			jedis.hset("flow:"+sip+":"+sport+":"+dip+":"+dport, field, value);
			jedis.expire("flow:"+dip+":"+dport+":"+sip+":"+sport, expire);
		}
		if(jedis!=null){
						JedisPoolClient.returnResource(jedis);jedis=null;}
		return true;
	}
	/**
	 * 写入数据流的请求信息
	 * @param sip 源ip
	 * @param sport 源端口
	 * @param dip
	 * @param dport
	 * @param message 请求信息体
	 * @return
	 * @throws redisConnectFailedException
	 */
	public boolean writeApplyMessage(int sip, int sport, int dip, int dport, String message){
		return writeFlowStatus(sip, sport, dip, dport, "applyMessage", message);
	}
	/**
	 * 获得一个流对应的get请求包
	 * @param sip
	 * @param sport
	 * @param dip
	 * @param dport
	 * @return get请求内容
	 * @throws redisConnectFailedException
	 */
	public String getApplyMessage(int sip, int sport, int dip, int dport) {
		return getFlowStatus(sip, sport, dip, dport, "applyMessage");
	}
	/**
	 * 后缀是否存在（视频的后缀）
	 * @param typename
	 * @return
	 * @throws redisConnectFailedException
	 */
	public boolean videoTypeVerify(String typename){
		if(jedis==null)jedis = JedisPoolClient.getJedis();
		boolean res = jedis.sismember("videoTypeList", typename);
		if(jedis!=null){
				JedisPoolClient.returnResource(jedis);jedis=null;
		}
		return res;
	}
	
	public String getIdByHost(String host){
		if(jedis==null)jedis = JedisPoolClient.getJedis();
		String res =  jedis.get("Host:"+host);
		if(jedis!=null){
			JedisPoolClient.returnResource(jedis);jedis=null;
		}
		return res;
	}
	
	public String getFlowStatus(int sip, int sport, int dip, int dport, String field) {
		if(jedis==null)jedis = JedisPoolClient.getJedis();
		if(jedis.exists("flow:"+sip+":"+sport+":"+dip+":"+dport) || jedis.exists("flow:"+dip+":"+dport+":"+sip+":"+sport)) {
			if(jedis.exists("flow:"+sip+":"+sport+":"+dip+":"+dport)) {
				jedis.expire("flow:"+sip+":"+sport+":"+dip+":"+dport, expire);
				String res = jedis.hget("flow:"+sip+":"+sport+":"+dip+":"+dport, field);
				if(jedis!=null){
						JedisPoolClient.returnResource(jedis);jedis=null;}
				return res;
				
			}
			else {
				jedis.expire("flow:"+dip+":"+dport+":"+sip+":"+sport, expire);
				String res = jedis.hget("flow:"+dip+":"+dport+":"+sip+":"+sport, field);
				if(jedis!=null){
						JedisPoolClient.returnResource(jedis);jedis=null;}
				return res;
			}
		}
		else {
			if(jedis!=null){
						JedisPoolClient.returnResource(jedis);jedis=null;}
			return null;
		}
	}
	/**
	 * 遇到了FIN包，记录握手号
	 * @param sip
	 * @param sport
	 * @param dip
	 * @param dport
	 * @return
	 * @throws redisConnectFailedException 
	 */
	public boolean finalSequenceSet(int sip, int sport, int dip, int dport, int seq, int ack) {
		if(jedis==null)jedis = JedisPoolClient.getJedis();
		if(jedis.exists("flow:"+sip+":"+sport+":"+dip+":"+dport) || jedis.exists("flow:"+dip+":"+dport+":"+sip+":"+sport)) {
			if(!jedis.exists("flow:"+sip+":"+sport+":"+dip+":"+dport)) {
				int tmp=sip;
				sip=dip;
				dip=tmp;
				tmp=sport;
				sport=dport;
				dport=tmp;
			}
			jedis.hset("flow:"+sip+":"+sport+":"+dip+":"+dport, "finalSeq", String.valueOf(seq));
			jedis.hset("flow:"+sip+":"+sport+":"+dip+":"+dport, "finalAck", String.valueOf(ack));
			jedis.expire("flow:"+sip+":"+sport+":"+dip+":"+dport, expire);
			if(jedis!=null){
						JedisPoolClient.returnResource(jedis);jedis=null;}
			return true;
		}
		else {
			if(jedis!=null){
						JedisPoolClient.returnResource(jedis);jedis=null;}
			return false;
		}
	}
	/**
	 * 检查四次挥手是否完成，完成则删除流数据，否则不删除
	 * ps：第四次挥手ACK的发送方和挥手发起方是同一方，seq等于第一次挥手的seq+1，ack等于第一次挥手的ack+1
	 * @param sip
	 * @param sport
	 * @param dip
	 * @param dport
	 * @return
	 * @throws redisConnectFailedException 
	 * @throws invalidNumberException 
	 */
	public boolean finalHandshakeCompleteCheck(int sip, int sport, int dip, int dport, int seq, int ack) throws redisConnectFailedException, invalidNumberException {
		if(jedis==null)jedis = JedisPoolClient.getJedis();
		if(jedis.exists("flow:"+sip+":"+sport+":"+dip+":"+dport) || jedis.exists("flow:"+dip+":"+dport+":"+sip+":"+sport)) {
			if(jedis.exists("flow:"+sip+":"+sport+":"+dip+":"+dport)) {
				int fseq = 0, fack = 0;
				if(jedis.hexists("flow:"+sip+":"+sport+":"+dip+":"+dport, "finalSeq"))
					fseq = commonFunctions.atoi(jedis.hget("flow:"+sip+":"+sport+":"+dip+":"+dport, "finalSeq"));
				else{
					if(jedis!=null){
						JedisPoolClient.returnResource(jedis);jedis=null;}
					return false;
				}
				if(jedis.hexists("flow:"+sip+":"+sport+":"+dip+":"+dport, "finalAck"))
					fack = commonFunctions.atoi(jedis.hget("flow:"+sip+":"+sport+":"+dip+":"+dport, "finalAck"));
				else {
					if(jedis!=null){
						JedisPoolClient.returnResource(jedis);jedis=null;}
					return false;
				}
				if(seq == fseq + 1 && ack == fack + 1) {
					jedis.del("flow:"+sip+":"+sport+":"+dip+":"+dport);
					if(jedis!=null){
						JedisPoolClient.returnResource(jedis);jedis=null;}
					return true;
				}
				else {
					if(jedis!=null){
						JedisPoolClient.returnResource(jedis);jedis=null;}
					return false;
				}
			}
			else {
				int fseq = 0, fack = 0;
				if(jedis.hexists("flow:"+dip+":"+dport+":"+sip+":"+sport, "finalSeq"))
					fseq = commonFunctions.atoi(jedis.hget("flow:"+dip+":"+dport+":"+sip+":"+sport, "finalSeq"));
				else{
					if(jedis!=null){
						JedisPoolClient.returnResource(jedis);jedis=null;}
					return false;
				}
				if(jedis.hexists("flow:"+dip+":"+dport+":"+sip+":"+sport, "finalAck"))
					fack = commonFunctions.atoi(jedis.hget("flow:"+dip+":"+dport+":"+sip+":"+sport, "finalAck"));
				else{
					if(jedis!=null){
						JedisPoolClient.returnResource(jedis);jedis=null;}
					return false;
				}
				if(seq == fseq + 1 && ack == fack + 1) {
					jedis.del("flow:"+dip+":"+dport+":"+sip+":"+sport);
					if(jedis!=null){
						JedisPoolClient.returnResource(jedis);jedis=null;}
					return true;
				}
				else {
					if(jedis!=null){
						JedisPoolClient.returnResource(jedis);jedis=null;}
					return false;
				}
			}
		}
		else {
			if(jedis!=null){
						JedisPoolClient.returnResource(jedis);jedis=null;}
			return false;
		}
	}
	/**
	 * 删除一个流的数据（结束）
	 * @param sip
	 * @param sport
	 * @param dip
	 * @param dport
	 * @return
	 * @throws redisConnectFailedException
	 */
	public boolean deleteFlow(int sip, int sport, int dip, int dport) throws redisConnectFailedException {
		if(jedis==null)jedis = JedisPoolClient.getJedis();
		if(jedis.exists("flow:"+sip+":"+sport+":"+dip+":"+dport) || jedis.exists("flow:"+dip+":"+dport+":"+sip+":"+sport)) {
			if(jedis.exists("flow:"+sip+":"+sport+":"+dip+":"+dport)) {
				if(jedis.del("flow:"+sip+":"+sport+":"+dip+":"+dport)==1) {
					if(jedis!=null){
						JedisPoolClient.returnResource(jedis);jedis=null;}
					return true;
				}
				else{
					if(jedis!=null){
						JedisPoolClient.returnResource(jedis);jedis=null;}
					return false;
				}
			}
			else {
				if(jedis.del("flow:"+dip+":"+dport+":"+sip+":"+sport)==1) {
					if(jedis!=null){
						JedisPoolClient.returnResource(jedis);jedis=null;}
					return true;
				}
				else{
					if(jedis!=null){
						JedisPoolClient.returnResource(jedis);jedis=null;}
					return false;
				}
			}
		}
		else {
			if(jedis!=null){
						JedisPoolClient.returnResource(jedis);jedis=null;}
			return false;
		}
	}
	/**
	 * 哈希域是否存在
	 * @param sip
	 * @param sport
	 * @param dip
	 * @param dport
	 * @return
	 * @throws redisConnectFailedException 
	 */
	public boolean checkHashExist(int sip, int sport, int dip, int dport, String field) {
		if(jedis==null)jedis = JedisPoolClient.getJedis();
		if(jedis.exists("flow:"+sip+":"+sport+":"+dip+":"+dport) || jedis.exists("flow:"+dip+":"+dport+":"+sip+":"+sport)) {
			if(jedis.exists("flow:"+sip+":"+sport+":"+dip+":"+dport)) {
				boolean res = jedis.hexists("flow:"+sip+":"+sport+":"+dip+":"+dport, field);
				if(jedis!=null){
						JedisPoolClient.returnResource(jedis);jedis=null;}
				return res;
			}
			else {
				boolean res = jedis.hexists("flow:"+dip+":"+dport+":"+sip+":"+sport, field);
				if(jedis!=null){
						JedisPoolClient.returnResource(jedis);jedis=null;}
				return res;
			}
		}
		else {
			if(jedis!=null){
						JedisPoolClient.returnResource(jedis);jedis=null;}
			return false;
		}
	}
	/**
	 * 注册ca信息
	 * @param sip
	 * @param sport
	 * @param dip
	 * @param dport
	 * @param len
	 * @return
	 * @throws redisConnectFailedException 
	 */
	public boolean CAcertInit(int sip, int sport, int dip, int dport, int len) throws redisConnectFailedException {
		writeFlowStatus(sip, sport, dip, dport, "CertLength", String.valueOf(len));
		writeFlowStatus(sip, sport, dip, dport, "CertWritten", String.valueOf(0));
		writeFlowStatus(sip, sport, dip, dport, "CertWriting", "yes");
		return true;
	}
	/**
	 * 写入CA证书内容
	 * @param sip
	 * @param sport
	 * @param dip
	 * @param dport
	 * @param caPart
	 * @return
	 * @throws invalidNumberException
	 * @throws redisConnectFailedException
	 */
	/*public boolean writeCAcert(int sip, int sport, int dip, int dport, String caPart) throws invalidNumberException, redisConnectFailedException {
		if(jedis==null)jedis = JedisPoolClient.getJedis();
		if(jedis.exists("flow:"+sip+":"+sport+":"+dip+":"+dport) || jedis.exists("flow:"+dip+":"+dport+":"+sip+":"+sport)) {
			if(!jedis.exists("flow:"+sip+":"+sport+":"+dip+":"+dport)) {
				int tmp=sip;
				sip=dip;
				dip=tmp;
				tmp=sport;
				sport=dport;
				dport=tmp;
			}
			if(!jedis.hexists("flow:"+sip+":"+sport+":"+dip+":"+dport, "CertLength") 
					|| !jedis.hexists("flow:"+sip+":"+sport+":"+dip+":"+dport, "CertWritten") )
				throw new invalidNumberException("CA证书长度不存在");
			int len = commonFunctions.atoi(jedis.hget("flow:"+sip+":"+sport+":"+dip+":"+dport, "CertLength"));
			int written = commonFunctions.atoi(jedis.hget("flow:"+sip+":"+sport+":"+dip+":"+dport, "CertWritten"));
			if(len-written>caPart.length()) {
				if(written==0) {
					if(dip==-1952451012 && dport==443 && sip==-1062725884 && sport==53632) {
						System.out.println("即将写入数据库：\n");
						try {
							commonFunctions.printbytesformat(caPart);
						} catch (WrongParameterException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					writeFlowStatus(sip, sport, dip, dport, "CertMessage", caPart);
					jedis.hincrBy("flow:"+sip+":"+sport+":"+dip+":"+dport, "CertWritten", caPart.length());
					return true;
				}
				else {
					String ori = getFlowStatus(sip, sport, dip, dport, "CertMessage");
					StringBuffer ob = new StringBuffer(ori);
					if(dip==-1952451012 && dport==443 && sip==-1062725884 && sport==53632) {
					System.out.println("即将写入数据库：\n");
						try {
							commonFunctions.printbytesformat(caPart);
						} catch (WrongParameterException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					ob.append(caPart);
					writeFlowStatus(sip, sport, dip, dport, "CertMessage", ob.toString());
					jedis.hincrBy("flow:"+sip+":"+sport+":"+dip+":"+dport, "CertWritten", caPart.length());
					return true;
				}
			}
			else {
				String ori = getFlowStatus(sip, sport, dip, dport, "CertMessage");
				StringBuffer ob = new StringBuffer(ori);
				if(dip==-1952451012 && dport==443 && sip==-1062725884 && sport==53632) {
				System.out.println("即将写入数据库：\n");
					try {
						commonFunctions.printbytesformat(caPart.substring(0, len-written));
					} catch (WrongParameterException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				ob.append(caPart.substring(0, len-written));
				writeFlowStatus(sip, sport, dip, dport, "CertMessage", ob.toString());
				jedis.hincrBy("flow:"+sip+":"+sport+":"+dip+":"+dport, "CertWritten", len-written);
				jedis.hdel("flow:"+sip+":"+sport+":"+dip+":"+dport, "CertWriting");
				//CAcertification ca = new CAcertification(ob.toString());
				if(dip==-1952451012 && dport==443 && sip==-1062725884 && sport==53632) {
					System.out.println("总CA：");
					try {
						commonFunctions.printbytesformat(ob);
					} catch (WrongParameterException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				//ca.certificate_division();
				return true;
			}
		}
		else {
			return false;
		}
	}
	*/
	public boolean writeCAcert(int sip, int sport, int dip, int dport, byte[] caPart) throws invalidNumberException {
		if(jedis==null)jedis = JedisPoolClient.getJedis();
		if(jedis.exists("flow:"+sip+":"+sport+":"+dip+":"+dport) || jedis.exists("flow:"+dip+":"+dport+":"+sip+":"+sport)) {
			if(!jedis.exists("flow:"+sip+":"+sport+":"+dip+":"+dport)) {
				int tmp=sip;
				sip=dip;
				dip=tmp;
				tmp=sport;
				sport=dport;
				dport=tmp;
			}
			if(!jedis.hexists("flow:"+sip+":"+sport+":"+dip+":"+dport, "CertLength") 
					|| !jedis.hexists("flow:"+sip+":"+sport+":"+dip+":"+dport, "CertWritten") )
				throw new invalidNumberException("CA证书长度不存在");
			int len = commonFunctions.atoi(jedis.hget("flow:"+sip+":"+sport+":"+dip+":"+dport, "CertLength"));
			int written = commonFunctions.atoi(jedis.hget("flow:"+sip+":"+sport+":"+dip+":"+dport, "CertWritten"));
			if(len-written>caPart.length) {
				if(written==0) {
					writeFlowStatus(sip, sport, dip, dport, "CertMessage", caPart);
					//上面这个函数有jedis归还链接的操作，导致jedis=null，需要重新从连接池借一个jedis连接
					if(jedis==null)jedis = JedisPoolClient.getJedis();
					if(jedis!=null)System.out.println("test-pt4");
					jedis.hincrBy("flow:"+sip+":"+sport+":"+dip+":"+dport, "CertWritten", caPart.length);
					if(jedis!=null){
						JedisPoolClient.returnResource(jedis);
						jedis=null;
					}
					return true;
				}
				else {
					byte[] ori = jedis.hget(new String("flow:"+sip+":"+sport+":"+dip+":"+dport).getBytes(), "CertMessage".getBytes());
					byte []waitwrite = new byte[ori.length+caPart.length];
					for(int i=0;i<ori.length;i++)waitwrite[i]=ori[i];
					for(int i=0;i<caPart.length;i++)waitwrite[i+ori.length]=caPart[i];
					
					writeFlowStatus(sip, sport, dip, dport, "CertMessage", waitwrite);
					if(jedis==null)jedis = JedisPoolClient.getJedis();
					jedis.hincrBy("flow:"+sip+":"+sport+":"+dip+":"+dport, "CertWritten", caPart.length);
					if(jedis!=null){
						JedisPoolClient.returnResource(jedis);
						jedis=null;
					}
					return true;
				}
			}
			else {
				byte[] ori = jedis.hget(new String("flow:"+sip+":"+sport+":"+dip+":"+dport).getBytes(), "CertMessage".getBytes());
				byte []waitwrite = new byte[ori.length+len-written];
				for(int i=0;i<ori.length;i++)waitwrite[i]=ori[i];
				for(int i=0;i<len-written;i++)waitwrite[i+ori.length]=caPart[i];
				
				writeFlowStatus(sip, sport, dip, dport, "CertMessage", waitwrite);
				if(jedis==null)jedis = JedisPoolClient.getJedis();
				
				jedis.hincrBy("flow:"+sip+":"+sport+":"+dip+":"+dport, "CertWritten", len-written);
				jedis.hdel("flow:"+sip+":"+sport+":"+dip+":"+dport, "CertWriting");
				//CAcertification ca = new CAcertification(ob.toString());
				/*if(dip==-1952451012 && dport==443 && sip==-1062725884 && sport==53632) {
					System.out.println("总CA：");
					try {
						commonFunctions.printbytesformat(jedis.hget(new String("flow:"+sip+":"+sport+":"+dip+":"+dport).getBytes(), "CertMessage".getBytes()));
					} catch (WrongParameterException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}*/
				CAcertification ca = new CAcertification(jedis.hget(new String("flow:"+sip+":"+sport+":"+dip+":"+dport).getBytes(), "CertMessage".getBytes()));
				ca.certificate_division();
				if(jedis!=null){
						JedisPoolClient.returnResource(jedis);jedis=null;}
				return true;
			}
		}
		else {
			if(jedis!=null){
						JedisPoolClient.returnResource(jedis);jedis=null;}
			return false;
		}
	}
	/**
	 * 写流参数函数（通用）
	 * @param sip 源ip
	 * @param sport 源端口
	 * @param dip 目的ip
	 * @param dport 目的端口
	 * @param field 哈希key
	 * @param value 值
	 * @return
	 */
	public boolean writeFlowStatus(int sip, int sport, int dip, int dport, String field, byte[] value) {
		// TODO Auto-generated method stub
		if(jedis==null)jedis = JedisPoolClient.getJedis();
		
		if(jedis.exists("flow:"+sip+":"+sport+":"+dip+":"+dport) || jedis.exists("flow:"+dip+":"+dport+":"+sip+":"+sport)) {
			if(jedis.exists("flow:"+sip+":"+sport+":"+dip+":"+dport)) {
				jedis.hset(new String("flow:"+sip+":"+sport+":"+dip+":"+dport).getBytes(), field.getBytes(), value);
				jedis.expire("flow:"+sip+":"+sport+":"+dip+":"+dport, expire);
				
			}
			else {
				jedis.hset(new String("flow:"+dip+":"+dport+":"+sip+":"+sport).getBytes(), field.getBytes(), value);
				jedis.expire("flow:"+dip+":"+dport+":"+sip+":"+sport, expire);
			}
		}
		else {
			jedis.hset(new String("flow:"+sip+":"+sport+":"+dip+":"+dport).getBytes(), field.getBytes(), value);
			jedis.expire("flow:"+dip+":"+dport+":"+sip+":"+sport, expire);
		}
		if(jedis!=null){
			JedisPoolClient.returnResource(jedis);
			jedis=null;
		}
		return true;
	}
	
}
