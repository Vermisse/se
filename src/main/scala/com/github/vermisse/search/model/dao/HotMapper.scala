package com.github.vermisse.search.model.dao

import org.apache.ibatis.annotations._
import java.util.{ List, Map }

/**
 * @author vermisse
 */
trait HotMapper {

  /**
   * 保存搜索记录(用于热搜榜)
   */
  def saveKeywords(@Param("id") id: String,
                   @Param("ip") ip: String,
                   @Param("keywords") keywords: String,
                   @Param("save_date") save_date: String)

  /**
   * 查询热搜榜
   */
  def getTop: List[Map[String, Int]]
}