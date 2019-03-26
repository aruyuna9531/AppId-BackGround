package linkToRedis;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
public class JedisPoolClient
{
	GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
	private static JedisPool pool = null; 
	static
	{
		if(pool == null)
		{
			JedisPoolConfig config = new JedisPoolConfig();
			pool = new JedisPool(config, "ras.sysu.edu.cn", 9037, 3000, "smartap");
		}
	}
	public static Jedis getJedis() { return pool.getResource(); }
	public static void returnResource(Jedis jedis) {
		if(jedis != null) { jedis.close();}
	}
	public static void main(String args[]) {
		Jedis jedis = null;
		System.out.println(jedis.get("a"));
		if(jedis!=null)jedis.close();
	}
}
