package sample;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class NotAGoodNameTests {

    @InjectMocks
    private MeteoService service;

    @Mock
    private MeteoClient client;

    private AutoCloseable mockRef;

    @BeforeEach
    void setUp() {
        mockRef = MockitoAnnotations.openMocks(this);
    }

    @Test
    void country_parameter_is_case_insensitive() {
        when(client.getTemperature(anyInt())).thenReturn(45.2D);

        String temperature = service.getTemperature("frAnCe");

        assertThat(temperature).isEqualTo("45.2");
    }

    @Test
    void unknown_country_throws() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> service.getTemperature("unknown country"));
    }

    @AfterEach
    void tearDown() throws Exception {
        mockRef.close();
    }
}
