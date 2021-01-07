package sample;

import org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SampleTest {

    @BeforeClass
    public static void staticSetUp() {
        // Do
        //   something
    }

    @Before
    public void setUp() {
        // Do
        //   something
        //  before each test method
    }

    @Test
    public void a_test() {
        // test something
    }

    @After
    public void tearDown() {
        // Do
        //   something
        //  after each test method
    }

    @AfterClass
    public static void staticTearDown() {
        // Do
        //   something
        //      else
    }
}
