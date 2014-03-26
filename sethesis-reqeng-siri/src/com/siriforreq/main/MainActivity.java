package com.siriforreq.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.siriforreq.activities.Alfred;
import com.siriforreq.activities.NetworkActivity;
import com.siriforreq.activities.SettingsMainActivity;





public class MainActivity extends Activity implements OnClickListener{
	EditText userEditText, passwordEditText;
	SharedPreferences prefs;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		ImageView titleView = (ImageView) findViewById(R.id.image_title_speechweaver_logo);
		titleView.setOnClickListener(this);
		Button startNetwork = (Button) findViewById(R.id.button_login);
		startNetwork.setOnClickListener(this);
		
		prefs = getSharedPreferences("com.siriforreq.activities", Context.MODE_PRIVATE);
		String userName = prefs.getString("prefusername", null);
		String userPassword = prefs.getString("prefpassword", null);
		userEditText = (EditText) findViewById(R.id.edit_text_username);
		passwordEditText = (EditText) findViewById(R.id.edit_text_password);
		if(userName != null && userPassword != null){
			userEditText.setText(userName);
			passwordEditText.setText(userPassword);
		}
		
	}

	@Override
	public void onClick(View arg0) {
		
		Intent i = null;
		
		switch(arg0.getId()){
		
			case R.id.image_title_speechweaver_logo:
				i = new Intent(this, Alfred.class);
				startActivity(i);
				break;
			case R.id.button_login:
				i = new Intent(this, NetworkActivity.class);
				if(userEditText.getText().toString() != null && passwordEditText.getText().toString() != null){
					System.out.println();
					prefs.edit().putString("prefusername", userEditText.getText().toString().trim()).apply();
					prefs.edit().putString("prefpassword", passwordEditText.getText().toString()).apply();
				}
				startActivity(i);
		}
	
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInfl = getMenuInflater();
		menuInfl.inflate(R.menu.activity_main, menu);
		
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case R.id.main_settings:
			startActivity(new Intent(this, SettingsMainActivity.class));
			break;
		}
		return super.onOptionsItemSelected(item);
	}
}



