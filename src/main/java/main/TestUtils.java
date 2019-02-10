package main;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestUtils {

    // debug (unused)
    private boolean areClose(double a, double b, double delta) {
        if (a < b) {
            return (a + delta) > b;
        }

        if (b < a) {
            return (b + delta) > a;
        }

        return true;
    }

}
