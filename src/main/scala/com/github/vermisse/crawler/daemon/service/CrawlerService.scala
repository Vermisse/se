package com.github.vermisse.crawler.daemon.service

import com.github.vermisse.crawler.model.dao._
import org.springframework.beans.factory.annotation._
import org.springframework.stereotype._
import com.github.vermisse.util._
import org.htmlparser._
import org.htmlparser.util._
import org.htmlparser.filters._
import org.htmlparser.tags._
import org.apache.lucene.document._

/**
 * 爬虫服务
 * @author vermisse
 */
@Service
class CrawlerService {

  /**
   * 队列操作
   */
  @Autowired
  private var queneMapper: QueneMapper = null

  /**
   * 查询所有未爬取的页面
   */
  def queryQuene = queneMapper.queryQuene(null)

  /**
   * 保存索引
   */
  def saveIndex(dir: String)(url: String): Unit = {
    var ex: String = null
    //读取url内容
    val html = $.url(url)(ex = _)
    //如果为空，调过
    if (ex != null) {
      queneMapper.indexQuene(url, $.date("yyyy-MM-dd"), 0)
      print("[索引失败][")
      print(ex)
      print("]")
      println(url)
      return
    }

    //因为要重复利用这个值，所以这里定义个常量
    val static = $.filterScript(html)
    val parser: Parser = new Parser
    parser.setInputHTML(static)

    var title: String = null
    var description: String = null
    //遍历一级节点
    while (parser.elements.hasMoreNodes) {
      //查询子节点
      val list = parser.elements.nextNode.getChildren
      //如果子节点不为空，递归
      if (list != null)
        saveIndex(list)(title = _)(description = _)
    }

    val sb = $.getStringBean
    parser.setInputHTML(static)
    parser.visitAllNodesWith(sb)
    queneMapper.indexQuene(url, $.date("yyyy-MM-dd"), 1)
    $.iwriter(dir) {
      doc =>
        doc.add(new Field("url", url, TextField.TYPE_STORED))
        doc.add(new Field("title", $.##(title), TextField.TYPE_STORED))
        doc.add(new Field("description", $.##(description), TextField.TYPE_STORED))
        doc.add(new Field("content", $.##(sb.getStrings), TextField.TYPE_STORED))
    }
    print("[索引完毕]")
    println(url)
  }

  def saveIndex(list: NodeList)(title: String => Unit)(description: String => Unit) {
    0 to list.size - 1 foreach {
      i =>
        val tag = list.elementAt(i)
        if (tag.isInstanceOf[LinkTag]) { //如果是a标签
          val link = tag.asInstanceOf[LinkTag].getLink
          if (link.matches($.regexUrl) && queneMapper.queryQuene(link).size == 0) {
            queneMapper.saveQuene(link, $.date("yyyy-MM-dd"), 0)
            print("[添加队列]")
            println(link)
          }
        } else if (tag.isInstanceOf[ImageTag] &&
          tag.asInstanceOf[ImageTag].getImageURL.matches($.regexUrl)) { //如果是图片
          //print("image:")
          //println(tag.asInstanceOf[ImageTag].getImageURL)
        } else if (tag.isInstanceOf[FrameTag]) { //如果是frame标签
          val location = tag.asInstanceOf[FrameTag].getFrameLocation
          if (location.matches($.regexUrl) && queneMapper.queryQuene(location).size == 0)
            queneMapper.saveQuene(location, $.date("yyyy-MM-dd"), 0)
        } else if (tag.isInstanceOf[TitleTag]) { //如果是title标签
          title(tag.asInstanceOf[TitleTag].getTitle)
        } else if (tag.isInstanceOf[MetaTag]) { //页面描述
          val meta = tag.asInstanceOf[MetaTag]
          val name = meta.getAttribute("name")
          if (name != null && name.toLowerCase == "description")
            description(meta.getMetaContent)
        } else if (tag.getChildren != null) { //递归
          saveIndex(tag.getChildren)(title)(description)
        }
    }
  }
}