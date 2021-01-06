package sample;

import org.junit.Assert.*;
import org.junit.Test;

class SampleTest {

    @Test
      void
        one_test() {
        int sumResult = 2 + 2;
        assertEquals("2 + 2 should be equal to 4", 4, sumResult);
    }

    @Test
    synchronized void two_test() {
        int multResult = 2 * 2;
        assertEquals("2 * 2 should be equal to 4", 4, multResult);
    }
}
