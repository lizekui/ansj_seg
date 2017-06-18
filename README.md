Ansj中文分词(词库可热更新版)
==============================

## 研究目的
当前线上系统更新词库重启集群服务有风险，亟需一套词库热更新策略，保证在不停服务的前提下，增删用户自定义词库。


## 研究准备
* [Tomcat 7.x](http://tomcat.apache.org/download-70.cgi)
* [Ansj项目repo](https://github.com/NLPchina/ansj_seg)

## 研究思路
![dataflow overview](http://7xlhxb.com1.z0.glb.clouddn.com/dataflow.png)

## 实现步骤
1. 在远程服务器Tomcat上部署词典文件
2. 修改配合文件，写明远程词典地址
3. 在程序启动是，创建定时线程任务ScheduledExecutorService，对应线程类Monitor
4. 编写Monitor线程类，功能为发送HTTP请求并分析Last-Modify、ETags值是否变化
5. 在DicLibrary类里，编写Monitor类监听到词库更新后，本地不停服务下载词典并更新词库的过程

## 实验截图
1. 远程词典初始内容
![screenshot01](http://7xlhxb.com1.z0.glb.clouddn.com/screenshot1.png)

2. 启动主程序分析
* 分析“明月几时有，把酒问青天”
![screenshot02](http://7xlhxb.com1.z0.glb.clouddn.com/screenshot2.png)

3. 词典增删改效果
* 将“问青天”从远端词库删掉
![screenshot03](http://7xlhxb.com1.z0.glb.clouddn.com/screenshot3.png)
* 将“问青天”从远端词库增加
![screenshot04](http://7xlhxb.com1.z0.glb.clouddn.com/screenshot4.png)
* 将“问青天”修改为“问青”，由于不符合语言学模型，所以修改无效果
![screenshot05](http://7xlhxb.com1.z0.glb.clouddn.com/screenshot5.png)

## 涉及到的源码
1. [Modify] ansj_library.properties
2. [Add] src/test/java/ TestRemoteDict.java
3. [Modify] pom.xml
4. [Add] src/main/java/org/ansj/monitor/Monitor.java
5. [Modify] src/main/java/org/ansj/library/DicLibrary.java
