package com.github.vermisse.search.controller.service

import org.springframework.stereotype._
import com.github.vermisse.util._
import org.apache.lucene.search._
import scala.collection._
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.queryparser.flexible.standard.QueryParserUtil
import java.util.ArrayList
import com.github.vermisse.search.model.dao._
import org.springframework.beans.factory.annotation._
import scala.collection.JavaConversions._

/**
 * @author vermisse
 */
@Service
class SearcherService {

  @Autowired
  private var mapper: HotMapper = null

  /**
   * 查询网页
   */
  def queryText(dir: String)(keywords: String,
                             ip: String,
                             pageSize: Int,
                             currentPage: Int)(page: java.util.ArrayList[Int] => Unit)(
                               pageCount: Int => Unit) = {
    val espKeywords = QueryParserUtil.escape(keywords)
    val queries = Array(espKeywords, espKeywords, espKeywords, "url")
    val fields = Array("title", "description", "content", "type")
    val clauses = Array(
      BooleanClause.Occur.SHOULD, //表示or
      BooleanClause.Occur.SHOULD, //BooleanClause.Occur.MUST表示and
      BooleanClause.Occur.SHOULD, //BooleanClause.Occur.MUST_NOT表示not
      BooleanClause.Occur.MUST)
    val result = new java.util.ArrayList[java.util.Map[String, String]]
    $.isearcher(dir, queries, fields, clauses, pageSize, currentPage) {
      doc =>
        val map = JavaConversions.mapAsJavaMap {
          Map(
            "url" -> doc.get("url"),
            "title" -> doc.get("title"),
            "description" -> doc.get("description"),
            "content" -> doc.get("content"))
        }
        result.add(map)
    } {
      pageCnt =>
        pageCount(pageCnt)
        val range = getRange(pageCnt, currentPage)
        if (range != null) {
          val list = new ArrayList[Int]
          range.foreach { list.add _ }
          page(list)
        }
    }
    mapper.saveKeywords($.randomText(15), ip, keywords, $.date("yyyy-MM-dd HH:mm:ss.SSS"))
    result
  }

  /**
   * 查询图片
   */
  def queryImage(dir: String)(keywords: String,
                              ip: String,
                              pageSize: Int,
                              currentPage: Int)(page: java.util.ArrayList[Int] => Unit)(
                                pageCount: Int => Unit) = {
    val espKeywords = QueryParserUtil.escape(keywords)
    val queries = Array(espKeywords, espKeywords, espKeywords, "img")
    val fields = Array("title", "description", "content", "type")
    val clauses = Array(
      BooleanClause.Occur.SHOULD, //表示or
      BooleanClause.Occur.SHOULD, //BooleanClause.Occur.MUST表示and
      BooleanClause.Occur.SHOULD, //BooleanClause.Occur.MUST_NOT表示not
      BooleanClause.Occur.MUST)
    val result = new java.util.ArrayList[java.util.Map[String, String]]
    $.isearcher(dir, queries, fields, clauses, pageSize, currentPage) {
      doc =>
        val map = JavaConversions.mapAsJavaMap {
          Map(
            "url" -> doc.get("url"),
            "title" -> doc.get("title"),
            "description" -> doc.get("description"),
            "content" -> doc.get("content"),
            "image" -> doc.get("image"))
        }
        result.add(map)
    } {
      pageCnt =>
        pageCount(pageCnt)
        val range = getRange(pageCnt, currentPage)
        if (range != null) {
          val list = new ArrayList[Int]
          range.foreach { list.add _ }
          page(list)
        }
    }
    mapper.saveKeywords($.randomText(15), ip, keywords, $.date("yyyy-MM-dd HH:mm:ss.SSS"))
    result
  }

  /**
   * 获取分页
   */
  def getRange(pageCnt: Int, currentPage: Int) = if (pageCnt > currentPage + 9)
    currentPage to currentPage + 9 //如果总页数比当前页大9页以上，从当前页数到9页以后
  else if (pageCnt > currentPage && pageCnt > 10)
    pageCnt - 9 to pageCnt //否则如果比当前页大不到9页，但是超过10页，说明是最后几页，从最后一页-9开始计算
  else if (pageCnt != 0)
    1 to pageCnt
  else
    null

  /**
   * 查询热搜榜
   */
  def getTop = {
    val all = mapper.getTop
    val list = new java.util.ArrayList[java.util.Map[String, Int]]

    0 to (if (all.size > 9) 9 else all.size - 1) foreach {
      i =>
        list.add(all.get(i))
    }
    list
  }
}