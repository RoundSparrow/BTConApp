// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package org.thaliproject.p2p.btconapp;

import android.annotation.TargetApi;
import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.HashMap;
import java.util.Locale;

public class MyTextSpeech implements TextToSpeech.OnInitListener {
    private final TextToSpeech _tts;
    private HashMap<String, String> params = new HashMap<String, String>();
    public volatile boolean isReady = false;

    public MyTextSpeech(Context context) {
        // start with full volume.
        params.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, "1.0");
        _tts = new TextToSpeech(context, this);
    }


    public void stop() {
        if (_tts != null) {
            _tts.stop();
            _tts.shutdown();
        }
        isReady = false;
    }

    @Override
    public void onInit(final int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = _tts.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                //
            }
            else {
                String msg = "hi there, i'm ready";
                speak(msg);
                isReady = true;
            }
        }
    }

    public void setVolumeLevel(String inVolumeLevel)
    {
        params.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, inVolumeLevel);
    }

    /**  speak at a lower volume, for platform >= 13 */
    @TargetApi(13)
    public void speak(final String text) {

        _tts.speak(text, TextToSpeech.QUEUE_FLUSH, params);
    }
}
