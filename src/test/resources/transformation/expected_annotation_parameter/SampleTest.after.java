package sample;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class SampleTest {

    @Test
    public void some_test() {
        StringBuilder stringBuilder = new StringBuilder()
            .append(2)
            .append(" + ")
            .append(2);
        assertThatExceptionOfType(NumberFormatException.class)
            .isThrownBy(() -> Integer
                .parseInt(stringBuilder.toString()));
    }
}
