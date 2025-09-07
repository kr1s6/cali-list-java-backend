package com.CalisthenicList.CaliList;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.springframework.test.context.ActiveProfiles;

@Suite
@SelectPackages("com.CalisthenicList.CaliList")
@ActiveProfiles("test")
public class AllTestsSuite {
}
