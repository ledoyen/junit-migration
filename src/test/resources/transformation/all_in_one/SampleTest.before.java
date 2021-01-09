package sample;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class NotAGoodNameTests {

    @InjectMocks
    private MeteoService service;

    @Mock
    private MeteoClient client;

    private AutoCloseable mockRef;

    @Before
    public void setUp() {
        mockRef = MockitoAnnotations.openMocks(this);
    }

    @Test
    public void country_parameter_is_case_insensitive() {
        when(client.getTemperature(anyInt())).thenReturn(45.2D);

        String temperature = service.getTemperature("frAnCe");

        assertThat(temperature).isEqualTo("45.2");
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknown_country_throws() {
        service.getTemperature("unknown country");
    }

    @After
    public void tearDown() throws Exception {
        mockRef.close();
    }
}
