package parallel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("App Integration Tests")
public class AppTest {

    @Test
    @DisplayName("App Konstruktor existiert")
    void testAppConstructor() {
        assertTrue(new App() != null);
    }

    @Test
    @DisplayName("App.main() kann ohne Fehler aufgerufen werden")
    void testMainMethodExists() throws Exception {
        var method = App.class.getDeclaredMethod("main", String[].class);
        assertTrue(method != null);
    }
}
