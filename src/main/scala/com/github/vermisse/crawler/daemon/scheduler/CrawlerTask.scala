package com.github.vermisse.crawler.daemon.scheduler

import org.springframework.stereotype._
import com.github.vermisse.crawler.daemon.service._
import com.github.vermisse.util._
import org.springframework.beans.factory.annotation._
import scala.collection.JavaConversions._
import javax.servlet._

/**
 * 爬虫任务
 * @author vermisse
 */
@Component
class CrawlerTask {

  /**
   * 爬虫服务
   */
  @Autowired
  private var service: CrawlerService = null

  @Autowired
  private var application: ServletContext = null

  /**
   * 任务调度入口
   */
  def run {
    val saveIndex = service.saveIndex(application.getRealPath("/WEB-INF/lucene"))(_)
    val level = $.prop("crawler.properties")("crawler.level").toInt
    1 to level foreach {
      i =>
        service.queryQuene.foreach {
          quene =>
            saveIndex(quene.get("URL").asInstanceOf[String])
        }
        print("[第")
        print(i)
        println("层索引完毕]")
    }
  }
}