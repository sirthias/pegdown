package org.pegdown;

import org.testng.Assert;

public class TestUtils {

    public static void assertEqualsMultiline(String actual, String expected) {
        Assert.assertEquals(
                actual.replace("\r\n", "\n"),
                expected.replace("\r\n", "\n")
        );
    }

}
