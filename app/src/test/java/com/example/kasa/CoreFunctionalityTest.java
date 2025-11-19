package com.example.kasa;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.util.Log;

import com.example.Kasa.AudioRecorder;
import com.example.Kasa.TextTranslator;
import com.example.Kasa.TextTranslator.TranslationCallback;
import com.example.Kasa.PhoneCallHandler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

import androidx.test.core.app.ApplicationProvider;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class CoreFunctionalityTest {
    private Context context;
    private AudioRecorder recorder;

    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
        recorder = new AudioRecorder(context);
        ShadowToast.reset();
    }

    // AudioRecorder tests
    @Test
    public void audioRecorder_initialState_notRecording() {
        assertFalse("Recorder should not be recording initially", recorder.isRecording());
    }

    @Test
    public void audioRecorder_filePath_valid() {
        String path = recorder.getAudioFilePath();
        assertNotNull("Audio file path should not be null", path);
        assertFalse("Audio file path should not be empty", path.isEmpty());
    }

    @Test
    public void audioRecorder_stopWithoutStart_noException() {
        recorder.stopRecording();
        assertFalse("Recorder still should not be recording", recorder.isRecording());
    }

    // TextTranslator tests
    @Test
    public void textTranslator_emptyInput_triggersOnError() {
        AtomicBoolean errorCalled = new AtomicBoolean(false);
        AtomicReference<String> errorMsg = new AtomicReference<>();

        TextTranslator.translateText("", new TranslationCallback() {
            @Override
            public void onSuccess(String translation, String cls, String content) {
                fail("onSuccess should not be called for empty input");
            }

            @Override
            public void onError(String error) {
                errorCalled.set(true);
                errorMsg.set(error);
            }
        });

        assertTrue("onError should be called", errorCalled.get());
        assertEquals("No text to translate", errorMsg.get());
    }

    // PhoneCallHandler: test Levenshtein distance via reflection
    @Test
    public void phoneCallHandler_levenshteinDistance_correctValue() throws Exception {
        Method ld = PhoneCallHandler.class.getDeclaredMethod("levenshteinDistance", String.class, String.class);
        ld.setAccessible(true);
        int dist1 = (int) ld.invoke(null, "kitten", "sitting");
        assertEquals(3, dist1);

        int dist2 = (int) ld.invoke(null, "flaw", "lawn");
        assertEquals(2, dist2);
    }
}
