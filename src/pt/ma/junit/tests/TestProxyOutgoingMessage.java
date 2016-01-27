package pt.ma.junit.tests;

import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestProxyOutgoingMessage {

	@BeforeClass
	public static void globalSetup() {
		// prefix actions executed once before any test!

	}

	@AfterClass
	public static void globalTeardown() {
		// prefix actions executed once after all tests!

	}

	@Before
	public void perTestSetup() {
		// prefix actions executed before each test!

	}

	@After
	public static void perTestTeardown() {
		// actions executed after each test!

	}

	@Test
	public void test() {
		fail("Not yet implemented");
	}

}
