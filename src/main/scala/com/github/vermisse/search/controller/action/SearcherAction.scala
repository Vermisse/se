package com.github.vermisse.search.controller.action

import org.springframework.stereotype._
import com.github.vermisse.search.controller.service._
import org.springframework.beans.factory.annotation._
import org.springframework.web.bind.annotation._
import javax.servlet._
import org.springframework.web.servlet._
import java.util.ArrayList
import javax.servlet.http.HttpServletRequest
import com.github.vermisse.util._

/**
 * @author vermisse
 */
@Controller
class SearcherAction {

  @Autowired
  private var service: SearcherService = null

  @Autowired
  private var application: ServletContext = null

  @RequestMapping(Array { "/s" })
  def queryText(request: HttpServletRequest) = {
    //验证入参，如果keywords为空，赋值为空字符串
    val keywords = $.##(request.getParameter("keywords"))

    val cur = if ($.##(request.getParameter("currentPage")).matches($.regexNum)) //如果是数字，取当前页
      request.getParameter("currentPage").toInt
    else 1 //否则设置为1

    val pageSize = 10
    val query = service.queryText(application.getRealPath("/WEB-INF/lucene"))(_, _, _, _)
    val key = new String(keywords.getBytes("ISO-8859-1"), "UTF-8")

    val mav = new ModelAndView("text")
    mav.addObject("keywords", key)
    mav.addObject("currentPage", cur)

    if (key.trim != "") {
      var pageCount = 0

      val result = query(key, request.getRemoteAddr, pageSize, cur) {
        mav.addObject("page", _)
      }(pageCount = _)
      mav.addObject("result", result)

      if (cur != 1) mav.addObject("previous", cur - 1)
      if (cur != pageCount) mav.addObject("next", cur + 1)
    }

    mav.addObject("top", service.getTop)
    mav
  }

  @RequestMapping(Array { "/p" })
  def queryImage(request: HttpServletRequest) = {
    //验证入参，如果keywords为空，赋值为空字符串
    val keywords = $.##(request.getParameter("keywords"))

    val cur = if ($.##(request.getParameter("currentPage")).matches($.regexNum)) //如果是数字，取当前页
      request.getParameter("currentPage").toInt
    else 1 //否则设置为1

    val pageSize = 9
    val query = service.queryImage(application.getRealPath("/WEB-INF/lucene"))(_, _, _, _)
    val key = new String(keywords.getBytes("ISO-8859-1"), "UTF-8")

    val mav = new ModelAndView("image")
    mav.addObject("keywords", key)
    mav.addObject("currentPage", cur)

    if (key.trim != "") {
      var pageCount = 0

      val result = query(key, request.getRemoteAddr, pageSize, cur) {
        mav.addObject("page", _)
      }(pageCount = _)
      mav.addObject("result", result)

      if (cur != 1) mav.addObject("previous", cur - 1)
      if (cur != pageCount) mav.addObject("next", cur + 1)
    }

    mav.addObject("top", service.getTop)
    mav
  }
}