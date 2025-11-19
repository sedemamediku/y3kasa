package com.example.kasa;

import static org.junit.Assert.*;

import com.example.Kasa.AudioTranscriber;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class AudioTranscriberTest {
    @Test
    public void transcribeAudio_missingFile_triggersOnError() {
        AtomicBoolean errorCalled = new AtomicBoolean(false);
        AtomicReference<String> errorMsg = new AtomicReference<>();

        AudioTranscriber.transcribeAudio("/no/such/file.mp3", new AudioTranscriber.TranscriptionCallback() {
            @Override
            public void onSuccess(String transcription) {
                fail("Should not succeed when file missing");
            }
            @Override
            public void onError(String error) {
                errorCalled.set(true);
                errorMsg.set(error);
            }
        });

        assertTrue(errorCalled.get());
        assertEquals("Audio file not found", errorMsg.get());
    }
}
