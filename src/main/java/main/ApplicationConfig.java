package main;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApplicationConfig {

    // 3 layers is the most common, valid case (RGB), 4 occurs sometimes as well (RGBA)
    public static final ArrayList<Integer> VALID_LAYER_COUNTS = new ArrayList<>(Arrays.asList(3, 4));

    public static final double DIVERGENCE_TOLERANCE_FACTOR = 1.8;

    public static final int EXPECT_MAX_DUPLICATES = 4;

}
