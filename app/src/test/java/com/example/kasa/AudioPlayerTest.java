package com.example.kasa;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;
import android.app.Activity;

import com.example.Kasa.AudioPlayer;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class AudioPlayerTest {
    private Activity activity;
    private AudioPlayer player;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(Activity.class).setup().get();
        ShadowToast.reset();
        player = new AudioPlayer(activity);
    }

    @Test
    public void startPlaying_emptyPath_showsNoFileToast_andNotPlaying() {
        player.startPlaying("", null);
        assertEquals("No audio file found", ShadowToast.getTextOfLatestToast());
        assertFalse(player.isPlaying());
    }

    @Test
    public void stopPlaying_withoutStart_doesNothing_andNotPlaying() {
        // should not crash or change state
        player.stopPlaying();
        assertFalse(player.isPlaying());
    }
}
