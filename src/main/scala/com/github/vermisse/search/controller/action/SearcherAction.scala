package com.github.vermisse.search.controller.action

import org.springframework.stereotype._
import com.github.vermisse.search.controller.service._
import org.springframework.beans.factory.annotation._
import org.springframework.web.bind.annotation._
import javax.servlet._
import org.springframework.web.servlet._
import java.util.ArrayList
import javax.servlet.http._
import com.github.vermisse.util._

/**
 * 搜索Action
 * @author vermisse
 */
@Controller
class SearcherAction {

  @Autowired
  private var service: SearcherService = null

  @Autowired
  private var application: ServletContext = null

  @RequestMapping(Array { "/s" })
  def queryText(request: HttpServletRequest) = find(request, 10, "text")

  @RequestMapping(Array { "/p" })
  def queryImage(request: HttpServletRequest) = find(request, 9, "image")

  /**
   * 搜索
   */
  def find(request: HttpServletRequest, size: Int, forward: String) = {
    //柯里化查询
    val query = forward match {
      case "text" => service.queryText(application.getRealPath("/WEB-INF/lucene"))(_, _, _, _)
      case "image" => service.queryImage(application.getRealPath("/WEB-INF/lucene"))(_, _, _, _)
    }
    val param = $(request)(_)

    //初始化参数
    val (pageSize, keywords, currentPage) = (size, param("keywords"), param("currentPage"))

    //参数转换
    val key = new String($(keywords).getBytes("ISO-8859-1"), "UTF-8")
    val cur = if ($(currentPage).matches($.regexNum)) currentPage.toInt else 1 //如果是数字取当前页，否则设置为1

    //定义返回值
    val mav = new ModelAndView(forward)
    mav.addObject("keywords", key)
    mav.addObject("currentPage", cur)

    //如果查询条件不为空
    if (key.trim != "") {
      mav.addObject("result", query(key, request.getRemoteAddr, pageSize, cur) {
        (page, count) =>
          mav.addObject("page", page)
          if (cur != count) mav.addObject("next", cur + 1)
      })

      if (cur != 1) mav.addObject("previous", cur - 1)
    }

    //查询热搜
    mav.addObject("top", service.getTop)
    mav
  }
}