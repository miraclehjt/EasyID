package com.gome.fup.easyid.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 缓存类
 * 自定义实现有效期控制
 * @author fupeng-ds
 */
public class Cache {

	private static Map<String, Long> expire = new ConcurrentHashMap<String, Long>();

	private static Map<String, Object> cache = new ConcurrentHashMap<String, Object>();

	/**
	 * 默认有效时间30分钟
	 */
	private static final long EXPIRE = 1800;

	public static void set(String key, Object value) {
		cache.put(key, value);
		expire.put(key, System.currentTimeMillis() + EXPIRE);
	}

	public static Object get(String key) {
		Long time = expire.get(key);
		if (null != time && System.currentTimeMillis() < time) {
			return cache.get(key);
		} else {
			expire.remove(key);
			cache.remove(key);
			return null;
		}
	}

	public static boolean hasKey(String key) {
		if (expire.containsKey(key)) {
			Long time = expire.get(key);
			if (null != time && System.currentTimeMillis() < time) {
				return cache.containsKey(key);
			}
		}
		return false;
	}

	public static void set(String key, Object value, long seconds) {
		cache.put(key, value);
		if (seconds != -1l) {
			expire.put(key, System.currentTimeMillis() + (seconds * 1000));
		}
	}

	public static void del(String key) {
		cache.remove(key);
		expire.remove(key);
	}
}
