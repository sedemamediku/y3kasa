package com.example.kasa;

import static org.junit.Assert.*;

import com.example.Kasa.TextTranslator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class TextTranslatorTest {
    @Test
    public void translateText_emptyInput_triggersOnError() {
        AtomicBoolean errorCalled = new AtomicBoolean(false);
        AtomicReference<String> errorMsg = new AtomicReference<>();

        TextTranslator.translateText("", new TextTranslator.TranslationCallback() {
            @Override
            public void onSuccess(String translation, String actionClass, String content) {
                fail("Should not succeed on empty input");
            }
            @Override
            public void onError(String error) {
                errorCalled.set(true);
                errorMsg.set(error);
            }
        });

        assertTrue(errorCalled.get());
        assertEquals("No text to translate", errorMsg.get());
    }
}
