package com.siriforreq.activities;

import java.util.InputMismatchException;
import java.util.Scanner;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import com.siriforreq.main.R;

public class SettingsMainActivity extends Activity {
	SharedPreferences sharedPref;
	EditText ipPortEditText;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings_list_view_item);
		ipPortEditText = (EditText) findViewById(R.id.settings_item_edit_text);
		sharedPref = getSharedPreferences("com.siriforreq.activities", Context.MODE_PRIVATE);
		
	}
	
	@Override protected void onPause(){
		super.onPause();
		System.out.println(ipPortEditText.getText().toString());
		if(ipPortEditText.getText().length() > 0){
			Scanner sc = new Scanner(ipPortEditText.getText().toString());
			sc.useDelimiter(":");
			String ip = "";
			try{
				if(sc.hasNext())
					ip = sc.next().trim();
				if(sc.hasNext()){
					int port = sc.nextInt();
					
					sharedPref.edit().putInt("portRef", port).apply();
				}
				sharedPref.edit().putString("ipRef", ip).apply();
				
			}catch(InputMismatchException e){
				Toast.makeText(this, "Port is not a number", Toast.LENGTH_LONG).show();
			}
		}
	}
	
	@Override
	protected void onResume(){
		super.onResume();

		String ipPortString = sharedPref.getString("ipRef", "") +":"+ sharedPref.getInt("portRef", -1);
		if(ipPortString.length() > 0){
			ipPortEditText.setText(ipPortString);
		}
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		
			
	}
}
