package sample;

import org.junit.Assert.*;
import org.junit.Test;

public class SampleTest {

    @Test
      public void
        one_test() {
        int sumResult = 2 + 2;
        assertEquals("2 + 2 should be equal to 4", 4, sumResult);
    }

    @Test
    public synchronized void two_test() {
        int multResult = 2 * 2;
        assertEquals("2 * 2 should be equal to 4", 4, multResult);
    }
}
