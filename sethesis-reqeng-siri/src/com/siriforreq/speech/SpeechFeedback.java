package com.siriforreq.speech;

import java.util.Locale;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;

public class SpeechFeedback implements OnInitListener {
	private Context c;
	private TextToSpeech tts;
	boolean ready = false;
	
	public SpeechFeedback(Context c){
		this.c = c;
		tts = new TextToSpeech(this.c,this);
	}
	@Override
	public void onInit(int status) {
		if(status == TextToSpeech.SUCCESS) {	
			int result = tts.setLanguage(Locale.getDefault());
			if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
				System.out.println("did not work in SpeechFeedback");
			} 
			else {	
					ready = true;
			}
		} 
		else {
			System.out.println("failed o init");
			ready = false;
		}
	}
	public void setPitch(float value){
		tts.setPitch(value);
	}
	
	public void setSpeed(float value){
		tts.setSpeechRate(value);
	}
	public boolean isReady(){
		return this.ready;
	}
	public void speak(String input){
		tts.speak(input, TextToSpeech.QUEUE_ADD, null);
		
	}
	public boolean isSpeaking(){
		return tts.isSpeaking();
	}
    public void destroy() {        
	            tts.shutdown();
	               
    }

}
