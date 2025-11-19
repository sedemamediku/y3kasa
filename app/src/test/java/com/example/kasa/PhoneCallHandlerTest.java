package com.example.kasa;

import static org.junit.Assert.*;

import com.example.Kasa.PhoneCallHandler;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.lang.reflect.Method;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PhoneCallHandlerTest {
    @Test
    public void levenshteinDistance_correctValues() throws Exception {
        Method m = PhoneCallHandler.class
                .getDeclaredMethod("levenshteinDistance", String.class, String.class);
        m.setAccessible(true);

        assertEquals(3, (int)m.invoke(null, "kitten", "sitting"));
        assertEquals(2, (int)m.invoke(null, "flaw",   "lawn"));
    }
}
