package com.siriforreq.speech;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.speech.RecognizerIntent;

public class SpeechInputController extends Activity{


	public static String getSpeechResultString(Intent intent){
		ArrayList<String> matches = intent
				.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
		StringBuilder sBuild = new StringBuilder();
		sBuild.append(matches.get(0));		
		return sBuild.toString();
	}
	
	public static Intent getSpeechIntent(){
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
				RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
				"Waiting for input...");

		return intent;
	}
	public static ArrayList<String> getStringArray(Intent intent){
		return intent
				.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
	}

	
}
