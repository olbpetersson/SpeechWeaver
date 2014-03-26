package com.siriforreq.activities;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
public class MediaButtonReceiver extends BroadcastReceiver{
	private boolean gotMessage = false;

	public MediaButtonReceiver(){
		super();
	}
	
	public boolean isMessage(){
		return this.gotMessage;
	}
	@Override
	public void onReceive(Context context, Intent intent) {
		System.out.println("IN ON RECEIVE1");
		 if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {			 
			 	System.out.println("GOT MEDIA BUTTON");
			 	Intent startBroadcastIntent = new Intent();
			 	startBroadcastIntent.setAction("com.siriforerq.activities.ACTION_LOL");
			 	startBroadcastIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
			 	System.out.println("...sending broadcast");
			 	context.sendBroadcast(startBroadcastIntent);
	     }
	}
	

}