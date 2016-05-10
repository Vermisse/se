package com.github.vermisse.util;

import org.junit.runner.*;
import org.springframework.test.context.*;
import org.springframework.test.context.transaction.*;
import org.springframework.transaction.annotation.*;

@RunWith(JUnit4ClassRunner.class)
@ContextConfiguration("/spring.xml")
@Transactional  
@TransactionConfiguration(transactionManager = "transactionManager", defaultRollback = false) 
public class BaseTest {

}