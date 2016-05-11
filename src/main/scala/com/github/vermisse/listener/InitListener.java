package com.github.vermisse.listener;

import javax.servlet.*;

public class InitListener implements ServletContextListener {
	
	/**
	 * 项目启动监听(scala类无法配置成监听)
	 */
	public void contextInitialized(ServletContextEvent application) {
		Init.createdb();
	}
	
	public void contextDestroyed(ServletContextEvent application) {
		
	}
}