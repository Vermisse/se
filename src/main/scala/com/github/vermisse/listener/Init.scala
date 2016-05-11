package com.github.vermisse.listener

import scala.reflect.runtime._
import javax.servlet._
import java.util._
import java.sql._

/**
 * 项目启动监听，用于初始化数据库
 * @author vermisse
 */
object Init {
  
  def createdb {
    val pro = this.prop("jdbc.properties")(_)
    println(pro("derby.driver"))
    
    Class.forName(pro("derby.driver"))
    val query = select(pro("derby.url"))(_)

    query("select count(*) from quene") {
      println _
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
   * 查询
   */
  def select(url: String, username: String = null, password: String = null)(sql: String)(rs: ResultSet => Unit): Unit = {
    var conn: Connection = null
    var pstmt: PreparedStatement = null
    var rset: ResultSet = null
    try {
      if (username != null && password != null) conn = DriverManager.getConnection(url, username, password)
      else conn = DriverManager.getConnection(url)
      pstmt = conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
      rset = pstmt.executeQuery

      while (rset.next) rs(rset)
    } finally {
      if (rset != null) rset.close
      if (pstmt != null) pstmt.close
      if (conn != null) conn.close
    }
  }
}