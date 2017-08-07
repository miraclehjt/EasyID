package com.gome.fup.easyid.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 缓存类
 * 单例
 * @author fupeng-ds
 */
public class Cache {

	private static LoadingCache<String, Object> loadingCache;

	static {
		loadingCache = CacheBuilder
				.newBuilder()
				.maximumSize(1000)
				.expireAfterWrite(60, TimeUnit.SECONDS)		//设值缓存有效时间，60秒
				.build(new CacheLoader<String, Object>() {

					@Override
					public Object load(String key) throws Exception {
						return loadingCache.get(key);
					}
				});
	}
	
	public static void set(String key, Object value) {
		loadingCache.put(key, value);
	}
	
	public static Object get(String key) {
		Object result = null;
		try {
			result = loadingCache.get(key);
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return result;
	}

	public static boolean hasKey(String key) {
		Set<String> keySet = loadingCache.asMap().keySet();
		if (keySet.contains(key)) {
			return true;
		}
		return false;
	}

}
