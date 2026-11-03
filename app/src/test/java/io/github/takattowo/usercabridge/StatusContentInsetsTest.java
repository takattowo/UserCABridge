package io.github.takattowo.usercabridge;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StatusContentInsetsTest {
    @Test
    public void systemBarInsetsAreAddedToBasePadding() {
        StatusContentInsets.Padding padding = StatusContentInsets.add(
                4, 5, 6, 7, 11, 13, 17, 19);

        assertEquals(15, padding.left);
        assertEquals(18, padding.top);
        assertEquals(23, padding.right);
        assertEquals(26, padding.bottom);
    }
}
