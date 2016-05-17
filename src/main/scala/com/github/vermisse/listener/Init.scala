package com.github.vermisse.listener

import com.github.vermisse.util._

/**
 * 项目启动监听，用于初始化数据库
 * @author vermisse
 */
object Init {

  /**
   * 创建数据库
   */
  def createdb {
    //柯里化配置文件
    val pro = $.prop("jdbc.properties")(_)

    //从配置文件中读取驱动字符串加载
    Class.forName(pro("derby.driver"))

    //柯里化查询和执行函数
    val query = $.select(pro("derby.url"))(_)
    val exec = $.execute(pro("derby.url"))(_)

    try {
      //查询队列表，如果异常说明数据库尚未初始化
      query("select count(*) from quene")(x => x)(x => x)
    } catch {
      case ex: Exception =>
        Array(
          """
            create table quene(
              url varchar(1024) primary key,
              isdown int,
              save_date varchar(10),
              type int
            )
          """,
          """
            create table hot(
              id varchar(15) primary key,
              ip varchar(20),
              keywords varchar(1024),
              save_date varchar(25)
            )
          """).foreach {
            //初始化数据库
            exec(_)(x => x)
          }
        //从init.txt中读取入口页面
        $.file("init.txt") {
          line =>
            //把入口页面保存到队列表
            exec("""
              insert into quene(url, isdown, save_date)
              values(?, ?, ?)
            """) {
              pstmt =>
                pstmt.setString(1, line)
                pstmt.setInt(2, 0)
                pstmt.setString(3, $.date("yyyy-MM-dd"))
            }
        }
    }
  }
}