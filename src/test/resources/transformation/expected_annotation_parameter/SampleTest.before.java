package sample;

import org.junit.Test;

public class SampleTest {

    @Test(expected = NumberFormatException.class)
    public void some_test() {
        StringBuilder stringBuilder = new StringBuilder()
            .append(2)
            .append(" + ")
            .append(2);
        Integer
            .parseInt(stringBuilder.toString());
    }
}
