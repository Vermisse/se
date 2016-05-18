##项目介绍
* 运行环境
    * JDK 7
    * Scala 2.11
    * Tomcat 7及以上
* 组件
    * Derby (保存待爬取资源队列及热搜榜，项目第一次启动时自动创建)
    * Lucene (分词索引网页及图片，资源不下载到本地)
    * HtmlParser (分析页面内容)
    * SpringMVC (路由)
    * Spring (AOP和IOC管理)
    * Mybatis (持久化)
    * Velocity (搜索引擎页面)
    * Quartz (定时执行爬虫)
    * JQuery (页面Javascript框架)
* 开发工具
    * Scala IDE (需手动安装JST、WST插件)