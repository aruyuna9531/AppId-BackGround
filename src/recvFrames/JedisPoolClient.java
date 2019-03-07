package recvFrames;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
public class JedisPoolClient
{
	private static JedisPool pool = null; 
	static
	{
		if(pool == null)
		{
			JedisPoolConfig config = new JedisPoolConfig();
			config.setMaxTotal(50);
			config.setMaxIdle(5); 
			config.setMaxWaitMillis(1000*100);
			config.setTestOnBorrow(true);
			config.setTestOnReturn(true); 
			pool = new JedisPool(config, "127.0.0.1", 6379, 2000, "619868");
		}
	}
	public static Jedis getJedis() { return pool.getResource(); }
	public static void returnResource(Jedis jedis) {
		if(jedis != null) { jedis.close();}
	}
}
