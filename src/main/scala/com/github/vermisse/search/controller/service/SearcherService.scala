package com.github.vermisse.search.controller.service

import org.springframework.stereotype._
import com.github.vermisse.util._
import org.apache.lucene.search._
import scala.collection._
import org.apache.lucene.queryparser.classic._
import org.apache.lucene.queryparser.flexible.standard._
import java.util.ArrayList
import com.github.vermisse.search.model.dao._
import org.springframework.beans.factory.annotation._
import scala.collection.JavaConversions._
import org.apache.lucene.document._

/**
 * 搜索服务
 * @author vermisse
 */
@Service
class SearcherService {

  @Autowired
  private var mapper: HotMapper = null

  /**
   * 查询网页
   */
  def queryText(dir: String)
               (keywords: String, ip: String, pageSize: Int, currentPage: Int)
               (page: (ArrayList[Int], Int) => Unit) =
    query(dir, keywords, ip, pageSize, currentPage, "url")(page) {
      doc =>
        Map(
          "url" -> doc.get("url"),
          "title" -> doc.get("title"),
          "description" -> doc.get("description"),
          "content" -> doc.get("content"))
    }

  /**
   * 查询图片
   */
  def queryImage(dir: String)
                (keywords: String, ip: String, pageSize: Int, currentPage: Int)
                (page: (ArrayList[Int], Int) => Unit) =
    query(dir, keywords, ip, pageSize, currentPage, "img")(page) {
      doc =>
        Map(
          "url" -> doc.get("url"),
          "title" -> doc.get("title"),
          "description" -> doc.get("description"),
          "content" -> doc.get("content"),
          "image" -> doc.get("image"))
    }

  /**
   * 通用查询
   */
  private def query(dir: String, keywords: String, ip: String, pageSize: Int, currentPage: Int, tp: String)
                   (page: (ArrayList[Int], Int) => Unit)
                   (doc: Document => Map[String, String]) = {
    val espKeywords = QueryParserUtil.escape(keywords)
    val queries = Array(espKeywords, espKeywords, espKeywords, tp)
    val fields = Array("title", "description", "content", "type")
    val clauses = Array(
      BooleanClause.Occur.SHOULD, //表示or
      BooleanClause.Occur.SHOULD, //BooleanClause.Occur.MUST表示and
      BooleanClause.Occur.SHOULD, //BooleanClause.Occur.MUST_NOT表示not
      BooleanClause.Occur.MUST)
    val result = new ArrayList[java.util.Map[String, String]]
    val count = $.isearcher(dir, queries, fields, clauses, pageSize, currentPage) {
      x =>
        result.add(JavaConversions.mapAsJavaMap(doc(x)))
    }
    page(getRange(count, currentPage), count)
    mapper.saveKeywords($.randomText(15), ip, keywords, $.date("yyyy-MM-dd HH:mm:ss.SSS"))
    result
  }

  /**
   * 获取分页
   */
  private def getRange(cnt: Int, cur: Int) = {
    val list = new ArrayList[Int]
    val range = cnt match {
      //如果总页数比当前页大9页以上，从当前页数到9页以后
      case _ if (cnt > cur + 9)         => cur to cur + 9
      //否则如果比当前页大不到9页，但是超过10页，说明是最后几页，从最后一页-9开始计算
      case _ if (cnt > cur && cnt > 10) => cnt - 9 to cnt
      case _ if (cnt != 0)              => 1 to cnt
      case _                            => 0 to 0
    }
    range.foreach(list.add(_))
    list
  }

  /**
   * 查询热搜榜
   */
  def getTop = {
    val (all, list) = (mapper.getTop, new ArrayList[java.util.Map[String, Int]])

    0 to (if (all.size > 9) 9 else all.size - 1) foreach {
      i =>
        list.add(all.get(i))
    }
    list
  }
}