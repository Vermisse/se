package com.github.vermisse.search.controller.action

import org.springframework.stereotype._
import com.github.vermisse.search.controller.service._
import org.springframework.beans.factory.annotation._
import org.springframework.web.bind.annotation._
import javax.servlet._
import org.springframework.web.servlet._
import java.util.ArrayList

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
  def queryText(keywords: String, currentPage: Int) = {
    val pageSize = 10
    val query = service.queryText(application.getRealPath("/WEB-INF/lucene"))(_, _, _)
    val key = new String(keywords.getBytes("ISO-8859-1"), "UTF-8")

    val mav = new ModelAndView("text")
    mav.addObject("keywords", key)
    mav.addObject("currentPage", currentPage)

    if (key.trim != "") {
      var pageCount: Int = 0

      val result = query(key, pageSize, currentPage) {
        mav.addObject("page", _)
      }
      mav.addObject("result", result)

      if (currentPage != 1) mav.addObject("previous", currentPage - 1)
      if (currentPage != pageCount) mav.addObject("next", currentPage + 1)
    }
    mav
  }
}