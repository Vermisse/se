package com.github.vermisse.listener.model.dao

/**
 * @author vermisse
 */
trait InitDao {
  
  /**
   * 探测数据库是否初始化
   */
  def queryExist
}