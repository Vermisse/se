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

/**
 * @author vermisse
 */
@Service
class SearcherService {

  @Autowired
  private var mapper: HotMapper = null

  def queryText(dir: String)(keywords: String,
                             ip: String,
                             pageSize: Int,
                             currentPage: Int)(page: java.util.ArrayList[Int] => Unit) = {
    val espKeywords = QueryParserUtil.escape(keywords)
    val queries = Array(espKeywords, espKeywords, espKeywords)
    val fields = Array("title", "description", "content")
    val clauses = Array(
      BooleanClause.Occur.SHOULD, //表示or
      BooleanClause.Occur.SHOULD, //BooleanClause.Occur.MUST表示and
      BooleanClause.Occur.SHOULD //BooleanClause.Occur.MUST_NOT表示not
      )
    val result = new java.util.ArrayList[java.util.Map[String, String]]
    $.isearcher(dir, queries, fields, clauses, pageSize, currentPage) {
      doc =>
        val map = JavaConversions.mapAsJavaMap(Map(
          "url" -> doc.get("url"),
          "title" -> doc.get("title"),
          "description" -> doc.get("description"),
          "content" -> doc.get("content")))
        result.add(map)
    } { pageCount =>
      val range = if (pageCount > currentPage + 9) currentPage to currentPage + 9
      else if (pageCount > currentPage && pageCount > 10) pageCount - 9 to pageCount else null
      if (range != null) {
        val list = new ArrayList[Int]
        range.foreach { list.add _ }
        page(list)
      }
    }
    mapper.saveKeywords($.randomText(15), ip, keywords, $.date("yyyy-MM-dd HH:mm:ss.SSS"))
    result
  }
  
  def getTop = mapper.getTop
}