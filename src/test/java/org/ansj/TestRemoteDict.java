package org.ansj;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.ansj.domain.Result;
import org.ansj.monitor.Monitor;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.ansj.util.MyStaticValue;

/**
 * ANSJ热更新词库测试类
 * 
 * @author: zkli  
 * @date: 2017年6月17日
 */
public class TestRemoteDict {
	// 配置文件 ansj_library.properties 中的key值
	String keyRemoteDict = "remote_dict"; 
	
	public static void main(String args[]) throws Exception {
		// 开始监控
		TestRemoteDict testRemoteDict = new TestRemoteDict();
		testRemoteDict.startRemoteMonitor();
		
		// 开始测试，两秒调用一次
		for (int i = 0; i < 100; i++) {
			Result terms = ToAnalysis.parse("明月几时有，把酒问青天");
			System.out.println(terms);
			Thread.sleep(2000);
		}
	}
	
	/**
	 * 设定监听程序，每隔60s启动一次monitor线程
	 */
	public synchronized void startRemoteMonitor() {
		ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
		if(MyStaticValue.ENV.containsKey(keyRemoteDict)){
			String location = MyStaticValue.ENV.get(keyRemoteDict);
			// 0 秒是初始延迟，可以修改的 5 秒是间隔时间 单位秒（实际建议60s或更长）
			pool.scheduleAtFixedRate(new Monitor(location), 0, 5, TimeUnit.SECONDS);
			// 0 秒是初始延迟，可以修改的 60 秒是间隔时间 单位秒
//			pool.scheduleAtFixedRate(new Monitor(location), 0, 60, TimeUnit.SECONDS);
		}
	}
}
