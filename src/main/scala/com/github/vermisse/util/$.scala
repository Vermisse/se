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
import java.nio.charset._

/**
 * 工具类
 * @author vermisse
 */
object $ {

  /**
   * 日期处理，所有参数都有默认值
   * @param format:输出格式，偏函数，默认值为yyyy-MM-dd HH:mm:ss
   * @param date:日期，偏函数，默认为当前系统时间
   * @param year:修改年，偏函数，默认为0
   * @param month:修改月，偏函数，默认为0
   * @param day:修改日，偏函数，默认为0
   * @param hour:修改时，偏函数，默认为0
   * @param minute:修改分，偏函数，默认为0
   * @param second:修改秒，偏函数，默认为0
   */
  def date(format: String = "yyyy-MM-dd HH:mm:ss",
           date: java.util.Date = new java.util.Date,
           year: Int = 0, month: Int = 0, day: Int = 0,
           hour: Int = 0, minute: Int = 0, second: Int = 0) = {
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
   * @param url:网址
   * @param exception:异常信息，高阶函数
   */
  def url(url: String)(exception: String => Unit) = {
    val result: StringBuilder = new StringBuilder

    //字符集过滤闭包
    val filter = (matcher: Matcher) =>
      if (!matcher.find)
        "UTF-8" //如果没有先默认设置为UTF-8
      else if (matcher.group(1).toUpperCase == "GB2312")
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
   * @param path:文件路径
   * @param line:每行内容，高阶函数
   */
  def file(path: String)(line: String => Unit) {
    val file = Source.fromFile(this.getClass.getClassLoader.getResource(path).getPath)
    try {
      file.getLines.foreach { line(_) }
    } catch {
      case ex: Exception => Unit
    } finally {
      file.close //自定义租赁模式，既使用后自动关闭，调用的时候无需考虑
    }
  }

  /**
   * JDBC查询
   * @param url:数据库连接
   * @param username:数据库账号，偏函数
   * @param password:数据库密码，偏函数
   * @param sql:查询语句
   * @param ps:PreparedStatement实例，高阶函数
   * @param rs:ResultSet实例，高阶函数
   */
  def select(url: String,
             username: String = null,
             password: String = null)(sql: String)(ps: PreparedStatement => Unit)(rs: ResultSet => Unit) {
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
   * JDBC执行
   * @param url:数据库连接
   * @param username:数据库账号，偏函数
   * @param password:数据库密码，偏函数
   * @param sql:查询语句
   * @param ps:PreparedStatement实例，高阶函数
   */
  def execute(url: String,
              username: String = null,
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
   * @param source:资源路径
   * @param key:键值
   */
  def prop(source: String)(key: String) = {
    try {
      val prop = new Properties
      prop.load(this.getClass.getClassLoader.getResourceAsStream(source))
      prop.getProperty(key)
    } catch {
      case ex: Exception => null
    }
  }

  /**
   * lucene索引保存
   * @param dir:资源路径
   * @param document:lucene文档，高阶函数
   */
  def iwriter(dir: String)(document: Document => Unit) {
    val analyzer = new StandardAnalyzer

    //将索引存储到硬盘上，使用下面的代码代替
    val directory = FSDirectory.open(Paths.get(dir))
    //如下想把索引存储到内存中
    //val directory: Directory = new RAMDirectory
    val config = new IndexWriterConfig(analyzer)
    val iwriter = new IndexWriter(directory, config)

    try {
      val doc = new Document
      document(doc)
      iwriter.addDocument(doc)
    } finally {
      iwriter.close
    }
  }

  /**
   * lucene查询索引(多条件分页)
   * @param dir:资源路径
   * @param queries:查询条件value集合
   * @param fields:查询条件key集合
   * @param clauses:逻辑关系(与、或、非)
   * @param pageSize:每页多少条
   * @param currentPage:当前第几页
   * @param document:查询结果
   * @param pageCount:总页数
   */
  def isearcher(dir: String,
                queries: scala.Array[String],
                fields: scala.Array[String],
                clauses: scala.Array[BooleanClause.Occur],
                pageSize: Int,
                currentPage: Int)(document: Document => Unit)(pageCount: Int => Unit) {
    val analyzer = new StandardAnalyzer

    //将索引存储到硬盘上，使用下面的代码代替
    val directory = FSDirectory.open(Paths.get(dir))
    //如下想把索引存储到内存中
    //val directory: Directory = new RAMDirectory

    //读取索引并查询
    val ireader = DirectoryReader.open(directory)
    val mreader = new MultiReader(ireader)

    try {
      val isearcher = new IndexSearcher(mreader)
      val query = MultiFieldQueryParser.parse(queries, fields, clauses, new StandardAnalyzer)
      val hits = isearcher.search(query, Integer.MAX_VALUE).scoreDocs

      val begin = pageSize * (currentPage - 1)
      val end = Math.min(begin + pageSize, hits.length) - 1

      pageCount {
        Math.ceil(hits.length / pageSize).asInstanceOf[Int]
      }

      //迭代输出结果
      begin to end foreach {
        i =>
          val hitDoc: Document = isearcher.doc(hits(i).doc)
          document(hitDoc)
      }
    } finally {
      mreader.close
      ireader.close
      directory.close
    }
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
   * @param html:页面源代码
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
        val tmp = (Math.random * 62).asInstanceOf[Int]
        sb.append((if (tmp < 26)
          tmp + 65
        else if (tmp < 52)
          tmp + 71
        else tmp - 4).toChar)
    }
    sb.toString
  }

  /**
   * null转换为空字符串
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