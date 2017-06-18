package org.ansj.library;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.ansj.dic.PathToStream;
import org.ansj.domain.KV;
import org.ansj.util.MyStaticValue;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.nlpcn.commons.lang.tire.domain.Forest;
import org.nlpcn.commons.lang.tire.domain.Value;
import org.nlpcn.commons.lang.tire.library.Library;
import org.nlpcn.commons.lang.util.IOUtil;
import org.nlpcn.commons.lang.util.StringUtil;
import org.nlpcn.commons.lang.util.logging.Log;
import org.nlpcn.commons.lang.util.logging.LogFactory;

public class DicLibrary {

	private static final Log LOG = LogFactory.getLog();

	public static final String DEFAULT = "dic";

	public static final String DEFAULT_NATURE = "userDefine";

	public static final Integer DEFAULT_FREQ = 1000;

	public static final String DEFAULT_FREQ_STR = "1000";
	
	// 记录上次发送请求时的远端自定义词典的快照 ADD by zkli
	public static List<String> previousRemoteDict = new ArrayList<>();

	// 用户自定义词典
	private static final Map<String, KV<String, Forest>> DIC = new HashMap<>();

	static {
		for (Entry<String, String> entry : MyStaticValue.ENV.entrySet()) {
			if (entry.getKey().startsWith(DEFAULT)) {
				put(entry.getKey(), entry.getValue());
			}
		}
		putIfAbsent(DEFAULT, "library/default.dic");

		Forest forest = get();
		if (forest == null) {
			put(DEFAULT, DEFAULT, new Forest());
		}

	}

	/**
	 * 如果确定远端词典有更新，则加载远程扩展词典到主词库表
	 * ADD by zkli
	 */
	public static void loadRemoteExtDict(String location) {
		LOG.info("[Dict Loading] " + location);
		List<String> currentRemoteDict = getRemoteWords(location);
		// 如果找不到扩展的字典，则忽略
		if (currentRemoteDict == null) {
			LOG.error("[Dict Loading] " + location + "加载失败");
		}
		
		// 判定此次修改哪些是增加的，哪些是删除的（修改可视为先删除，后增加）
		List<String> addRemoteDict = new ArrayList<>();
		List<String> delRemoteDict = new ArrayList<>();
		compareBetweenRemoteDictState(currentRemoteDict, addRemoteDict, delRemoteDict);
		
		for (String theWord : addRemoteDict) {
			if (theWord != null && !"".equals(theWord.trim())) {
				// 加载扩展词典数据到主内存词典中
				LOG.info("ADD " + theWord);
				DicLibrary.insert(DicLibrary.DEFAULT, theWord, "userDefine", 1000);
			}
		}
		for (String theWord : delRemoteDict) {
			if (theWord != null && !"".equals(theWord.trim())) {
				// 加载扩展词典数据到主内存词典中
				LOG.info("DEL " + theWord);
				DicLibrary.delete(DicLibrary.DEFAULT, theWord);
			}
		}
		
		previousRemoteDict.clear();
		previousRemoteDict.addAll(currentRemoteDict);
	}
	
	/**
	 * 判定此次修改哪些是增加的，哪些是删除的（修改可视为先删除，后增加）
	 * @param currentRemoteDict
	 * @param addRemoteDict
	 * @param delRemoteDict
	 */
	private static void compareBetweenRemoteDictState(List<String> currentRemoteDict, List<String> addRemoteDict,
			List<String> delRemoteDict) {
		List tmp = new ArrayList();
		tmp.addAll(previousRemoteDict);
		// 删了哪些词语
		tmp.removeAll(currentRemoteDict);
		delRemoteDict.addAll(tmp);

		tmp.clear();
		tmp.addAll(currentRemoteDict);
		// 增加了哪些词语
		tmp.removeAll(previousRemoteDict);
		addRemoteDict.addAll(tmp);
	}

	/**
	 * 如果确定远端词典有更新，
	 * 从远程服务器上，通过HTTP请求的方法下载自定义词条
	 * ADD by zkli
	 */
	private static List<String> getRemoteWords(String location) {

		List<String> buffer = new ArrayList<String>();
		RequestConfig rc = RequestConfig.custom().setConnectionRequestTimeout(10 * 1000).setConnectTimeout(10 * 1000)
				.setSocketTimeout(60 * 1000).build();
		CloseableHttpClient httpclient = HttpClients.createDefault();
		CloseableHttpResponse response;
		BufferedReader in;
		HttpGet get = new HttpGet(location);
		get.setConfig(rc);
		try {
			response = httpclient.execute(get);
			if (response.getStatusLine().getStatusCode() == 200) {

				String charset = "UTF-8";
				// 获取编码，默认为utf-8
				if (response.getEntity().getContentType().getValue().contains("charset=")) {
					String contentType = response.getEntity().getContentType().getValue();
					charset = contentType.substring(contentType.lastIndexOf("=") + 1);
				}
				in = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), charset));
				String line;
				while ((line = in.readLine()) != null) {
					buffer.add(line);
				}
				in.close();
				response.close();
				return buffer;
			}
			response.close();
		} catch (ClientProtocolException e) {
			LOG.error("getRemoteWords "+location+" error", e);
		} catch (IllegalStateException e) {
			LOG.error("getRemoteWords "+location+" error", e);
		} catch (IOException e) {
			LOG.error("getRemoteWords "+location+" error", e);
		}
		return buffer;
	}
	
	/**
	 * 关键词增加
	 *
	 * @param keyword 所要增加的关键词
	 * @param nature 关键词的词性
	 * @param freq 关键词的词频
	 */
	public static void insert(String key, String keyword, String nature, int freq) {
		Forest dic = get(key);
		String[] paramers = new String[2];
		paramers[0] = nature;
		paramers[1] = String.valueOf(freq);
		Value value = new Value(keyword, paramers);
		Library.insertWord(dic, value);
	}

	/**
	 * 增加关键词
	 *
	 * @param keyword
	 */
	public static void insert(String key, String keyword) {

		insert(key, keyword, DEFAULT_NATURE, DEFAULT_FREQ);
	}

	/**
	 * 删除关键词
	 */
	public static void delete(String key, String word) {

		Forest dic = get(key);
		if (dic != null) {
			Library.removeWord(dic, word);
		}
	}

	/**
	 * 将用户自定义词典清空
	 */
	public static void clear(String key) {
		get(key).clear();
	}

	public static Forest get() {
		if (!DIC.containsKey(DEFAULT)) {
			return null;
		}
		return get(DEFAULT);
	}

	/**
	 * 根据模型名称获取crf模型
	 * 
	 * @param modelName
	 * @return
	 */
	public static Forest get(String key) {

		KV<String, Forest> kv = DIC.get(key);

		if (kv == null) {
			if (MyStaticValue.ENV.containsKey(key)) {
				putIfAbsent(key, MyStaticValue.ENV.get(key));
				return get(key);
			}
			LOG.warn("dic " + key + " not found in config ");
			return null;
		}
		Forest forest = kv.getV();
		if (forest == null) {
			forest = init(key, kv, false);
		}
		return forest;

	}

	/**
	 * 根据keys获取词典集合
	 * 
	 * @param keys
	 * @return
	 */
	public static Forest[] gets(String... keys) {
		Forest[] forests = new Forest[keys.length];
		for (int i = 0; i < forests.length; i++) {
			forests[i] = get(keys[i]);
		}
		return forests;
	}

	/**
	 * 根据keys获取词典集合
	 * 
	 * @param keys
	 * @return
	 */
	public static Forest[] gets(Collection<String> keys) {
		return gets(keys.toArray(new String[keys.size()]));
	}

	/**
	 * 用户自定义词典加载
	 * 
	 * @param key
	 * @param path
	 * @return
	 */

	private synchronized static Forest init(String key, KV<String, Forest> kv, boolean reload) {
		Forest forest = kv.getV();
		if (forest != null) {
			if (reload) {
				forest.clear();
			} else {
				return forest;
			}
		} else {
			forest = new Forest();
		}
		try {

			LOG.debug("begin init dic !");
			long start = System.currentTimeMillis();
			String temp = null;
			String[] strs = null;
			Value value = null;
			try (BufferedReader br = IOUtil.getReader(PathToStream.stream(kv.getK()), "UTF-8")) {
				while ((temp = br.readLine()) != null) {
					if (StringUtil.isNotBlank(temp)) {
						temp = StringUtil.trim(temp);
						strs = temp.split("\t");
						strs[0] = strs[0].toLowerCase();
						// 如何核心辞典存在那么就放弃
						if (MyStaticValue.isSkipUserDefine && DATDictionary.getId(strs[0]) > 0) {
							continue;
						}
						if (strs.length != 3) {
							value = new Value(strs[0], DEFAULT_NATURE, DEFAULT_FREQ_STR);
						} else {
							value = new Value(strs[0], strs[1], strs[2]);
						}
						Library.insertWord(forest, value);
					}
				}
			}
			LOG.info("load dic use time:" + (System.currentTimeMillis() - start) + " path is : " + kv.getK());
			kv.setV(forest);
			return forest;
		} catch (Exception e) {
			LOG.error("Init dic library error :" + e.getMessage() + ", path: " + kv.getK());
			DIC.remove(key);
			return null;
		}
	}

	/**
	 * 动态添加词典
	 * 
	 * @param dicDefault
	 * @param dicDefault2
	 * @param dic2
	 */
	public static void put(String key, String path, Forest forest) {
		DIC.put(key, KV.with(path, forest));
		MyStaticValue.ENV.put(key, path);
	}

	/**
	 * 动态添加词典
	 * 
	 * @param dicDefault
	 * @param dicDefault2
	 * @param dic2
	 */
	public static void putIfAbsent(String key, String path) {

		if (!DIC.containsKey(key)) {
			DIC.put(key, KV.with(path, (Forest) null));
		}
	}

	/**
	 * 动态添加词典
	 * 
	 * @param dicDefault
	 * @param dicDefault2
	 * @param dic2
	 */
	public static void put(String key, String path) {
		put(key, path, null);
	}

	/**
	 * 动态添加词典
	 * 
	 * @param <T>
	 * @param <T>
	 * 
	 * @param dicDefault
	 * @param dicDefault2
	 * @param dic2
	 */
	public static synchronized Forest putIfAbsent(String key, String path, Forest forest) {

		KV<String, Forest> kv = DIC.get(key);
		if (kv != null && kv.getV() != null) {
			return kv.getV();
		}
		put(key, path, forest);
		return forest;
	}

	public static KV<String, Forest> remove(String key) {
		KV<String, Forest> kv = DIC.get(key);
		if (kv != null && kv.getV() != null) {
			kv.getV().clear();
		}
		MyStaticValue.ENV.remove(key) ;
		return DIC.remove(key);
	}

	public static Set<String> keys() {
		return DIC.keySet();
	}

	public static void reload(String key) {
		if (!MyStaticValue.ENV.containsKey(key)) { //如果变量中不存在直接删掉这个key不解释了
			remove(key);
		}

		putIfAbsent(key, MyStaticValue.ENV.get(key));

		KV<String, Forest> kv = DIC.get(key);

		init(key, kv, true);
	}

}
