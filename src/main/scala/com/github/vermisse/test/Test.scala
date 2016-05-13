package com.github.vermisse.test

import org.apache.lucene.analysis._
import org.apache.lucene.analysis.standard._
import org.apache.lucene.store._
import java.nio.file._
import org.apache.lucene.index._
import org.apache.lucene.document._
import org.apache.lucene.search._
import org.apache.lucene.queryparser.classic._
import com.github.vermisse.util._
import org.htmlparser._

/**
 * @author vermisse
 */
object Test {

  def main(arr: Array[String]) {
    //柯里化配置文件
    val pro = $.prop("jdbc.properties")(_)

    //从配置文件中读取驱动字符串加载
    Class.forName(pro("derby.driver"))

    //柯里化查询和执行函数
    val query = $.select(pro("derby.url"))(_)
    query("select * from quene where isdown=0")(x => x) {
      rs =>
        println(rs.getString("url"))
    }
  }
}