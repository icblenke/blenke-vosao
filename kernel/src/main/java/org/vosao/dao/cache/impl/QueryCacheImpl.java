/**
 * Vosao CMS. Simple CMS for Google App Engine.
 * Copyright (C) 2009 Vosao development team
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * email: vosao.dev@gmail.com
 */

package org.vosao.dao.cache.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.cache.Cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vosao.dao.cache.CacheStat;
import org.vosao.dao.cache.QueryCache;
import org.vosao.global.SystemService;

public class QueryCacheImpl implements QueryCache, Serializable {

	protected static final Log logger = LogFactory.getLog(
			QueryCacheImpl.class);

	private SystemService systemService;
	private long calls;
	private long hits;
	
	public QueryCacheImpl() {
		calls = 0;
		hits = 0;
	}

	public SystemService getSystemService() {
		return systemService;
	}

	public void setSystemService(SystemService systemService) {
		this.systemService = systemService;
	}
	
	private Cache getCache() {
		return systemService.getCache();
	}

	private String getQueryMapKey(Class clazz) {
		return "queries:" + clazz.getName();
	}
	
	private Map<String, Set<String>> getQueryMap(Class clazz) {
		Map<String, Set<String>> result = (Map<String, Set<String>>) getCache()
				.get(getQueryMapKey(clazz));
		if (result == null) {
			result = new HashMap<String, Set<String>>();
		}
		return result;
	}
	
	private void updateQueryMap(Class clazz, Map<String, Set<String>> map) {
		getCache().put(getQueryMapKey(clazz), map);
	}

	private String getQueryKey(Class clazz, String query, Object[] params) {
		StringBuffer result = new StringBuffer(clazz.getName());
		result.append(query);
		if (params != null) {
			for (Object param : params) {
				result.append(param != null ? param.toString() : "null"); 
			}
		}
		return result.toString();
	}

	@Override
	public Object getQuery(Class clazz, String query, Object[] params) {
		calls++;
		Map<String, Set<String>> map = getQueryMap(clazz);
		Set<String> set = map.get(query);
		String key = getQueryKey(clazz, query, params);
		if (set != null && set.contains(key) && getCache().containsKey(key)) {
			Object result = getCache().get(key);
			if (result != null) {
				hits++;
			}
			return result;
		}
		return null;
	}

	@Override
	public void putQuery(Class clazz, String query, Object[] params, 
			Object value) {
		Map<String, Set<String>> map = getQueryMap(clazz);
		Set<String> set = map.get(query);
		String key = getQueryKey(clazz, query, params);
		if (set == null) {
			set = new HashSet<String>();
			map.put(query, set);
		}
		set.add(key);
		getCache().put(key, value);
		updateQueryMap(clazz, map);
	}

	@Override
	public void removeQueries(Class clazz) {
		updateQueryMap(clazz, new HashMap<String, Set<String>>());
	}

	@Override
	public CacheStat getStat() {
		return new CacheStat(calls, hits);
	}
	
}
