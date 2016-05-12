package com.github.vermisse.util

import scala.io._
import java.sql._
import java.text._
import java.util._
import scala.reflect.runtime._
import org.apache.lucene.document._
import org.apache.lucene.analysis._
import org.apache.lucene.store._
import org.apache.lucene.analysis.standard._
import org.apache.lucene.index._
import java.nio.file._

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
    c.set(Calendar.YEAR, year)
    c.set(Calendar.MONTH, month)
    c.set(Calendar.DAY_OF_YEAR, day)
    c.set(Calendar.HOUR, hour)
    c.set(Calendar.MINUTE, minute)
    c.set(Calendar.SECOND, second)

    sdf.format(c.getTime)
  }

  /**
   * 读取url内容
   */
  def url(url: String): String = {
    var result = ""
    val conn = Source.fromURL(url)
    try {
      conn.getLines.foreach { result += _ }
      result
    } catch {
      case ex: Exception => null
    } finally {
      conn.close //自定义租赁模式，既使用后自动关闭，调用的时候无需考虑
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

  def iwriter(dir: String)(document: Document => Unit): Unit = {
    val analyzer: Analyzer = new StandardAnalyzer

    //将索引存储到硬盘上，使用下面的代码代替
    val directory: Directory = FSDirectory.open(Paths.get(dir))
    //如下想把索引存储到内存中
    //val directory: Directory = new RAMDirectory
    val config: IndexWriterConfig = new IndexWriterConfig(analyzer)
    val iwriter: IndexWriter = new IndexWriter(directory, config)

    try{
      val doc: Document = new Document
      document(doc)
      iwriter.addDocument(doc)
    }finally{
      iwriter.close
    }
  }
}