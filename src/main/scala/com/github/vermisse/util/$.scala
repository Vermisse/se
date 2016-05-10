package com.github.vermisse.util

import scala.io._
import java.sql._

/**
 * 工具类
 * @author vermisse
 */
object $ {
  classOf[org.apache.derby.jdbc.Driver42]

  private val conn_url = "jdbc:derby:/derby;create=true"

  def main(arr: scala.Array[String]) {
    val conn = DriverManager getConnection (conn_url)
    var pstmt: PreparedStatement = null
    var rset: ResultSet = null
    try {
      pstmt = conn prepareStatement ("select * from abab")
      rset = pstmt executeQuery

      while (rset next) println(rset)
    } finally {
      if (rset != null) rset.close
      if (pstmt != null) pstmt.close
      if (conn != null) conn.close
    }
  }

  def url(url: String): String = {
    var result = ""
    val conn = Source.fromURL(url)
    try {
      conn.getLines.foreach { result += _ }
      result
    } catch {
      case ex: Exception => ""
    } finally {
      conn.close //自定义租赁模式，既使用后自动关闭，调用的时候无需考虑
    }
  }
}