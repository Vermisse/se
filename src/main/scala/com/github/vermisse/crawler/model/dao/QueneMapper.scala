package com.github.vermisse.crawler.model.dao

import org.apache.ibatis.annotations._
import com.github.vermisse.util._

/**
 * 队列操作
 * @author vermisse
 */
trait QueneMapper {

  /**
   * 查询队列
   */
  def queryQuene(@Param("url") url: String = null)

  /**
   * 保存队列
   */
  def saveQuene(@Param("url") url: String,
                @Param("save_date") save_date: String,
                @Param("isdown") isdown: Int = 0)
}