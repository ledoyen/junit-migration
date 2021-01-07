package sample;

import org.junit.Assert.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SampleTest {

    @BeforeAll
    public static void staticSetUp() {
        // Do
        //   something
    }

    @BeforeEach
    public void setUp() {
        // Do
        //   something
        //  before each test method
    }

    @Test
    public void a_test() {
        // test something
    }

    @AfterEach
    public void tearDown() {
        // Do
        //   something
        //  after each test method
    }

    @AfterAll
    public static void staticTearDown() {
        // Do
        //   something
        //      else
    }
}
