package com.siriforreq.activities;

import java.util.Random;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import com.siriforreq.main.R;
import com.siriforreq.speech.SpeechFeedback;

public class Alfred extends Activity {
	SpeechFeedback speech;
	ImageView mouth;
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.lolactivity);
		
		speech = new SpeechFeedback(this);
		ImageView alfred = (ImageView) findViewById(R.id.imageView1);
		mouth = (ImageView) findViewById(R.id.imageView2);
		mouth.setVisibility(View.INVISIBLE);
		alfred.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
//				speech.speak("Hey, I am Alfred. Let me tell you a story which happened long long time ago...\n" +
//						"It had been a cold winter in Gothenburg. I had worked as a butler at Systemite for about ten years. One beautiful " +
//						"day in February, two really handsome men entered the office. They began a master thesis which would revolutionize the world, hence defining" +
//						"my existance. I am Alfred. I do forget, I do forgive. Do not expect me");
//				speech.setPitch(2);
//				speech.speak("Something is happening. Something fishy is going down. But our friends are watching" +
//						"all day long. And they are in the neighbourhood, so if you want help, give them a call. Here comes Piff and Puff, they" +
//						"are jumping now. Here comes Piff and Puff, every time now. Here comes the best squad of saving. They solves the cases just for you!");
				speech.setPitch((float) 0.3);
				speech.speak("Luke, I am your father");
				Thread t = new Thread(new Runnable(){

					@Override
					public void run() {
						// TODO Auto-generated method stub
						while(speech.isSpeaking()){
							Message msg = handler.obtainMessage();
							msg.sendToTarget();
							
							try {
								Random r = new Random();
								
								
								int sleepTime = r.nextInt(160)+
										r.nextInt(150)+r.nextInt(150)+r.nextInt(150)+r.nextInt(150)+r.nextInt(150);
								System.out.println("int is: " +sleepTime);
								Thread.currentThread().sleep(sleepTime);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
						}
					}
					
				});
				t.start();
			}
		});
		
	}
	public void onDestroy(){
		super.onDestroy();
		speech.destroy();
	}
	private Handler handler = new Handler(){
		public void handleMessage(Message msg){
			if(mouth.getVisibility() == View.VISIBLE){
				mouth.setVisibility(View.INVISIBLE);
			}
			else
				mouth.setVisibility(View.VISIBLE);
		}
		
	};
}
