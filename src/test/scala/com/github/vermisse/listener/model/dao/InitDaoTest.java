package com.github.vermisse.listener.model.dao;

import org.junit.*;
import org.springframework.beans.factory.annotation.*;

import com.github.vermisse.util.*;

public class InitDaoTest extends BaseTest {
	
	@Autowired
	private InitDao dao;
	
	@Test
	public void testQueryExist() {
		dao.queryExist();
	}
}