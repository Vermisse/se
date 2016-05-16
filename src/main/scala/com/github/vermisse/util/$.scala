package com.github.vermisse.util

import scala.io._
import java.sql._
import java.text._
import java.util._
import scala.reflect.runtime._
import scala.collection.JavaConversions._
import org.apache.lucene.document._
import org.apache.lucene.analysis._
import org.apache.lucene.store._
import org.apache.lucene.analysis.standard._
import org.apache.lucene.index._
import java.nio.file._
import java.net._
import java.util.regex._
import scala.collection.mutable.StringBuilder
import org.htmlparser.beans._
import org.apache.lucene.search._
import org.apache.lucene.queryparser.classic._
import java.nio.charset.MalformedInputException

/**
 * 工具类
 * @author vermisse
 */
object $ {

  /**
   * 日期处理，所有参数都有默认值
   */
  def date(format: String = "yyyy-MM-dd HH:mm:ss",
           date: java.util.Date = new java.util.Date,
           year: Int = 0, month: Int = 0, day: Int = 0,
           hour: Int = 0, minute: Int = 0, second: Int = 0): String = {
    val sdf = new SimpleDateFormat(format)

    val c = Calendar.getInstance
    c.setTime(date)
    c.add(Calendar.YEAR, year)
    c.add(Calendar.MONTH, month)
    c.add(Calendar.DAY_OF_YEAR, day)
    c.add(Calendar.HOUR, hour)
    c.add(Calendar.MINUTE, minute)
    c.add(Calendar.SECOND, second)

    sdf.format(c.getTime)
  }

  /**
   * 读取url内容
   */
  def url(url: String)(exception: String => Unit): String = {
    val result: StringBuilder = new StringBuilder

    //字符集过滤
    val filter = (matcher: Matcher) =>
      if (!matcher.find)
        "UTF-8" //如果没有先默认设置为UTF-8
      else if (matcher.group(1) equalsIgnoreCase "GB2312")
        "GBK"
      else
        matcher.group(1).toUpperCase

    val _url: URL = new URL(url)
    var conn: BufferedSource = null
    try {
      val map: Map[String, List[String]] = _url.openConnection.getHeaderFields
      if (map == null)
        throw new Exception("无法获取Header")
      if (map.get("Content-Type") == null)
        throw new Exception("无法获取Content-Type")
      map.get("Content-Type").foreach {
        charset =>
          val matcher: Matcher = Pattern.compile(".*charset=([^;]*).*").matcher(charset)
          conn = Source.fromURL(url, filter(matcher))
          conn.getLines.foreach { result.append(_) }
      }
      result.toString
    } catch {
      case ex: MalformedInputException =>
        try {
          conn.close
          conn = Source.fromURL(url, "GBK") //如果读取失败用GBK重新读取一遍
          conn.getLines.foreach { result.append(_) }
          result.toString
        } catch {
          case ex: Exception =>
            exception(ex.getMessage)
            null
        }
      case ex: Exception =>
        exception(ex.getMessage)
        null
    } finally {
      if (conn != null) conn.close
    }
  }

  /**
   * 读取文件内容
   */
  def file(path: String)(line: String => Unit): Unit = {
    val file = Source.fromFile(this.getClass.getClassLoader.getResource(path).getPath)
    try {
      file.getLines.foreach { line(_) }
    } catch {
      case ex: Exception => null
    } finally {
      file.close //自定义租赁模式，既使用后自动关闭，调用的时候无需考虑
    }
  }

  /**
   * 查询
   */
  def select(url: String, username: String = null,
             password: String = null)(sql: String)(ps: PreparedStatement => Unit)(rs: ResultSet => Unit): Unit = {
    var conn: Connection = null
    var pstmt: PreparedStatement = null
    var rset: ResultSet = null
    try {
      if (username != null && password != null) conn = DriverManager.getConnection(url, username, password)
      else conn = DriverManager.getConnection(url)
      pstmt = conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
      ps(pstmt)
      rset = pstmt.executeQuery

      while (rset.next) rs(rset)
    } finally {
      if (rset != null) rset.close
      if (pstmt != null) pstmt.close
      if (conn != null) conn.close
    }
  }

  /**
   * 执行
   */
  def execute(url: String, username: String = null,
              password: String = null)(sql: String)(ps: PreparedStatement => Unit) = {
    var conn: Connection = null
    var pstmt: PreparedStatement = null
    try {
      if (username != null && password != null) conn = DriverManager.getConnection(url, username, password)
      else conn = DriverManager.getConnection(url)
      pstmt = conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY)
      ps(pstmt)

      pstmt.executeUpdate
    } finally {
      if (pstmt != null) pstmt.close
      if (conn != null) conn.close
    }
  }

  /**
   * 读取properties配置文件
   */
  def prop(source: String)(key: String): String = {
    try {
      val prop = new Properties
      prop.load(this.getClass.getClassLoader.getResourceAsStream(source))
      prop.getProperty(key)
    } catch {
      case ex: Exception => null
    }
  }

  /**
   * 索引保存
   */
  def iwriter(dir: String)(document: Document => Unit): Unit = {
    val analyzer: Analyzer = new StandardAnalyzer

    //将索引存储到硬盘上，使用下面的代码代替
    val directory: Directory = FSDirectory.open(Paths.get(dir))
    //如下想把索引存储到内存中
    //val directory: Directory = new RAMDirectory
    val config: IndexWriterConfig = new IndexWriterConfig(analyzer)
    val iwriter: IndexWriter = new IndexWriter(directory, config)

    try {
      val doc: Document = new Document
      document(doc)
      iwriter.addDocument(doc)
    } finally {
      iwriter.close
    }
  }

  /**
   * 查询索引
   */
  def isearcher(dir: String,
                queries: scala.Array[String],
                fields: scala.Array[String],
                clauses: scala.Array[BooleanClause.Occur],
                pageSize: Int,
                currentPage: Int)(document: Document => Unit)(pageCount: Int => Unit) {
    val analyzer: Analyzer = new StandardAnalyzer

    //将索引存储到硬盘上，使用下面的代码代替
    val directory: Directory = FSDirectory.open(Paths.get(dir))
    //如下想把索引存储到内存中
    //val directory: Directory = new RAMDirectory

    //读取索引并查询
    val ireader: DirectoryReader = DirectoryReader.open(directory)
    val mreader: MultiReader = new MultiReader(ireader)
    val isearcher: IndexSearcher = new IndexSearcher(mreader)

    val query: Query = MultiFieldQueryParser.parse(queries, fields, clauses, new StandardAnalyzer)
    val hits: scala.Array[ScoreDoc] = isearcher.search(query, Integer.MAX_VALUE).scoreDocs

    val begin = pageSize * (currentPage - 1)
    val end = Math.min(begin + pageSize, hits.length) - 1

    pageCount(Math.ceil(hits.length / pageSize).asInstanceOf[Int])

    //迭代输出结果
    begin to end foreach {
      i =>
        val hitDoc: Document = isearcher.doc(hits(i).doc)
        document(hitDoc)
    }
    ireader.close
    directory.close
  }

  /**
   * 获取HtmlParser格式化
   */
  def getStringBean = {
    val sb = new StringBean
    sb.setLinks(false)
    sb.setReplaceNonBreakingSpaces(true)
    sb.setCollapse(true)
    sb
  }

  /**
   * 过滤脚本标签
   */
  def filterScript(html: String): String = {
    val style = html.toLowerCase.indexOf("<style")
    val script = html.toLowerCase.indexOf("<script")
    var endStyle = html.toLowerCase.indexOf("</style>")
    var endScript = html.toLowerCase.indexOf("</script>")
    if (style != -1 && endStyle != -1) {
      while (endStyle < style) //有些标签不是对称的，为了防止这种情况，如果结束标签早于开始标签，重新计算结束标签
        endStyle += html.substring(endStyle + 1).toLowerCase.indexOf("</style>") + 1
      filterScript(html.substring(0, style) + html.substring(endStyle + 8))
    } else if (script != -1 && endScript != -1) {
      while (endScript < script)
        endScript += html.substring(endScript + 1).toLowerCase.indexOf("</script>") + 1
      filterScript(html.substring(0, script) + html.substring(endScript + 9))
    } else html
  }

  /**
   * 创建随机字符串(包含数字及大小写字母)
   */
  def randomText(length: Int) = {
    val sb: StringBuilder = new StringBuilder
    0 to length - 1 foreach {
      _ =>
        val tmp = (Math.random() * 62).asInstanceOf[Int]
        sb.append((if (tmp < 26)
          tmp + 65
        else if (tmp < 52)
          tmp + 71
        else tmp - 4).toChar)
    }
    sb.toString
  }

  /**
   * null转换
   */
  def ##(text: String) = if (text == null) "" else text

  /**
   * url正则表达式验证规则
   */
  def regexUrl = "^(http|https|ftp)\\://([a-zA-Z0-9\\.\\-]+(\\:[a-zA-Z0-9\\.&%\\$\\-]+)*@)?((25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9])\\.(25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[0-9])|([a-zA-Z0-9\\-]+\\.)*[a-zA-Z0-9\\-]+\\.[a-zA-Z]{2,4})(\\:[0-9]+)?(/[^/][a-zA-Z0-9\\.\\,\\?\\'\\/\\+&%\\$#\\=~_\\-@]*)*$"

  /**
   * 数字正则表达式验证规则
   */
  def regexNum = "^[0-9]+$"
}