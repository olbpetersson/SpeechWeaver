package com.siriforreq.activities;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.siriforreq.helper.LimitedMimeTypeMap;
import com.siriforreq.helper.QueryHelper;
import com.siriforreq.main.R;
import com.siriforreq.speech.SocketNetworkHelper;
import com.siriforreq.speech.SpeechFeedback;
import com.siriforreq.speech.SpeechInputController;

public class NetworkActivity extends Activity implements
		OnClickListener{
	private static final int HANDLER_CONNECT_STATUS = 0,
			HANDLER_SHOW_PROGRESSDIALOG_CONNECT = 1,
			HANDLER_SHOW_PROGRESSDIALOG_TALKING = 2,
			HANDLER_SHOW_PROGRESSDIALOG_TRANSFER = 3,
			VOICE_INTENT_WRITE_ARRAY_REQUESTCODE = 4, 
			HANDLER_SHOW_VIEW_DIALOG = 5,
			HANDLER_SET_BUTTON_STATUS = 6,
			HANDLER_SET_REQUIREMENT_TITLE = 7, 
			HANDLER_SET_ANNOTATION_TITLE = 8,
			HANDLER_RECEIVED_ROOT_NODES = 9,
			HANDLER_RECEIVED_CONTEXT_NODES = 10,
			ATTACH_FILE_REQUEST = 11;
	private static final String  WAITING_FOR_TAG = "<WFT>", END_OF_LINE = "<EOL>", FAILED_TO_FIND_ITEM = "<FTFI>", CLOSEST_MATCHES = "<CMF>",
			FAILED_TO_EXECUTE = "<FTEC>", ID = "<ID>", SELECTION = "<SEL>", TAG = "<TAG>", 
			INFO = "<INFO>", ERROR = "<ERROR>", REPORT = "<RPGN>", ROOT="<ROOT>", CONTEXTNODE="<CTND>", TAGPLUS = "<TAGP>", FILETAG = "<FILE>", FILESIZETAG = "<FSIZE>";
	int port;
	private final LimitedMimeTypeMap mimeMap = LimitedMimeTypeMap.getSingleton();
	private String stringEdit, readStringInput, choiceMadeString, tagFlag, ip, username, password, fileName;
	private ByteArrayOutputStream byteArray;
	private SocketNetworkHelper socketNetworkHelper;
	private ArrayList<String> listValues = new ArrayList<String>();
	private TextView requirementTitle, annotationTitle, contextTitle, nameOfAttachedFile;
	private boolean runThread, hasContext = false, hasFile = false;
	private Button speakButton, generateButton, attachFileButton, clearAttachedFileButton;
	private static ProgressDialog progressDialog;
	private SpeechFeedback speech;
	private AudioManager mAudioManager;
	private ComponentName mMediaReceiverCompName;
	private BroadcastReceiver catchButtonEvent;
	public Context c;
	private SharedPreferences pref;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.alternative_layout_network);
		
		c = this;
		pref = getSharedPreferences("com.siriforreq.activities",Context.MODE_PRIVATE);
		username = pref.getString("prefusername", "");
		password = pref.getString("prefpassword", "");
		System.out.println("OnCreate()");
		mMediaReceiverCompName = new ComponentName(getPackageName(), MediaButtonReceiver.class.getName());
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		requirementTitle = (TextView) findViewById(R.id.requirement_title);
		annotationTitle = (TextView) findViewById(R.id.annotation_title);
		contextTitle = (TextView) findViewById(R.id.text_path_structure);
		contextTitle.setText(username+ "@");
		nameOfAttachedFile = (TextView) findViewById(R.id.attachedFileText);
		nameOfAttachedFile.setOnClickListener(this);
		
		speakButton = (Button) findViewById(R.id.bTapToTalk);
		speakButton.setOnClickListener(this);
		generateButton = (Button) findViewById(R.id.bGenerateReport);
		generateButton.setOnClickListener(this);
		attachFileButton = (Button) findViewById(R.id.bAttacheFiles);
		attachFileButton.setOnClickListener(this);
		clearAttachedFileButton = (Button) findViewById(R.id.attachedFileRemove);
		clearAttachedFileButton.setOnClickListener(this);
				
		readStringInput = "";
		runThread = true;
		speech = new SpeechFeedback(this);
		ip = pref.getString("ipRef", "");
		port = pref.getInt("portRef", -1);
		
		catchButtonEvent = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				if(speakButton.isEnabled())
					startTalkInteraction();			
			}		
		};
		sendMessageToHandler(HANDLER_SET_BUTTON_STATUS, false);
		startThread();
	}

	public void onStart(){
		super.onStart();
	}

	public void onResume() {
		super.onResume();
		System.out.println("onResume()");
		mAudioManager.registerMediaButtonEventReceiver(mMediaReceiverCompName);	
		socketNetworkHelper = SocketNetworkHelper.getSocketNetworkHelper(ip, port);
		setButtonStatus(true);
		IntentFilter btCommunicationFilter = new IntentFilter();
		btCommunicationFilter.setPriority(1000);
		btCommunicationFilter.addAction("com.siriforerq.activities.ACTION_LOL");
		registerReceiver(catchButtonEvent, btCommunicationFilter);
	}

	public void onPause() {
		super.onPause();
		if(progressDialog != null){
			progressDialog.dismiss();
		}
		unregisterReceiver(catchButtonEvent);
	}

	public void onStop(){
		super.onStop();
	}

	public void onDestroy(){
		super.onDestroy();
		runThread = false;
		System.out.println("OnDestroy");
		if (socketNetworkHelper != null) {
			
			socketNetworkHelper.close();
		}
		sleepWhileSpeeking();
		mAudioManager.unregisterMediaButtonEventReceiver(mMediaReceiverCompName);
		NotificationManager notificationManager = 
	    		(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.cancel(0);
		speech.destroy();
	}

	/**
	 * Starts the networkThread if it is not currently running
	 */
	public void startThread() {
		if (!networkThread.isAlive()) {
			runThread = true;
			networkThread.start();
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.bTapToTalk:
					startTalkInteraction();
				break;
			case R.id.bGenerateReport:
				sendMessageToHandler(HANDLER_SET_BUTTON_STATUS, false);
				if(runThread && socketNetworkHelper.isAlive())
					writeToSocket(REPORT);
				break;
			case R.id.bAttacheFiles:
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType("*/*");
				intent.addCategory(Intent.CATEGORY_OPENABLE);
				intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
				startActivityForResult(Intent.createChooser(intent, "Choose file from"),ATTACH_FILE_REQUEST);
				break;
			case R.id.attachedFileRemove:
				hasFile = false;
				nameOfAttachedFile.setText("No attached file at the moment");
				byteArray = null;
				clearAttachedFileButton.setEnabled(false);
				attachFileButton.setEnabled(true);
				break;
		}
	}

	/**
	 * Handle the results from the voice recognition activity.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == RESULT_OK){	
			if(requestCode == ATTACH_FILE_REQUEST){	
				 InputStream intentInputStream = null;
				 String tempName = data.getData().toString();
				 String tempMimeType = null;
				 Uri intentUri = data.getData();
	
				 tempName = QueryHelper.queryForColumn(c, intentUri, MediaStore.Images.Media.TITLE);
				 tempMimeType = QueryHelper.queryForColumn(c, intentUri, MediaStore.Images.Media.MIME_TYPE);
				 String fileEnding = mimeMap.getExtensionFromMimeType(tempMimeType);
				 if(tempName != null && !tempName.endsWith("."))
					 tempName += ".";
				 fileName = tempName.replace(" ", "");
				 if(fileEnding != null)
					 fileName += fileEnding;
				 
				 try {
					 boolean succeed = true;
					 intentInputStream = getContentResolver().openInputStream(data.getData());
					 BufferedInputStream bufferedInputStream = new BufferedInputStream(intentInputStream);	 
					 byte[] bytes = new byte[65535];
					 byteArray = new ByteArrayOutputStream();
					 
						while((bufferedInputStream.read(bytes)) != -1){
							 byteArray.write(bytes, 0, bytes.length);
							 if(byteArray.size() > 61440000){
								 Toast.makeText(c, "File to large. Max: 60 MB", Toast.LENGTH_LONG).show();
								 intentInputStream = null;
								 succeed = false;
								 break;
							 }	 
						 }
						if(succeed){
							hasFile = true;
							clearAttachedFileButton.setEnabled(true);
							nameOfAttachedFile.setText(fileName);
						}				
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
					Toast.makeText(this, "Error in file selection", Toast.LENGTH_LONG).show();
					speech.speak("Error in file selection");
				} catch (IOException e2){
					e2.printStackTrace();
					Toast.makeText(this, "Input/Output Error", Toast.LENGTH_LONG).show();
					speech.speak("Input/Output Error");
				}
	
			}
			else if(requestCode == VOICE_INTENT_WRITE_ARRAY_REQUESTCODE){
				sendMessageToHandler(HANDLER_SET_BUTTON_STATUS, false);
				
				listValues = SpeechInputController.getStringArray(data);
				if(tagFlag == ID){
					sendMessageToHandler(HANDLER_SET_REQUIREMENT_TITLE, listValues.get(0));
				}
				else if(tagFlag == TAG || tagFlag == TAGPLUS){
					sendMessageToHandler(HANDLER_SET_ANNOTATION_TITLE, listValues.get(0));
				}
				listValues.add(0, tagFlag);		
				writeToSocket(listValues);	
				if(tagFlag == TAGPLUS){
					writeToSocket(FILETAG+fileName+FILESIZETAG+byteArray.size(), byteArray.toByteArray());
					
					hasFile = false;
					nameOfAttachedFile.setText("No attached file at the moment");
					clearAttachedFileButton.setEnabled(false);
					attachFileButton.setEnabled(true);
				}
			}
			else{
				System.out.println("HELLO");
			}
		}
	}

	public void startTalkInteraction(){
		speech.speak("ID");
		Intent intent = SpeechInputController.getSpeechIntent();
		tagFlag = ID;
		sleepWhileSpeeking();
		sendMessageToHandler(HANDLER_SET_BUTTON_STATUS, false);
		startActivityForResult(intent, VOICE_INTENT_WRITE_ARRAY_REQUESTCODE);
	}
	
	/**
	 * Reconnects the socket if it is dead
	 */
	public void reconnectSocketHelper() {
		if (!socketNetworkHelper.isAlive()) {
			sendMessageToHandler(HANDLER_SHOW_PROGRESSDIALOG_CONNECT, true);
			try {
				socketNetworkHelper.connect(username,password);
			} catch (IOException e) {
				Toast.makeText(this, "ERROR! IOException!", Toast.LENGTH_LONG).show();
				speech.speak("ERROR! IOException!");
				e.printStackTrace();
			}	
			sendMessageToHandler(HANDLER_CONNECT_STATUS, socketNetworkHelper.isAlive());
		}
	}
	
	/**
	 * Writes to the socket established by the socketNetworkHelper
	 * 
	 * @param text
	 */
	public void writeToSocket(String text) {
		if(text.length() > 0){
			System.out.println("Write to socket String text");
			sendMessageToHandler(HANDLER_SHOW_PROGRESSDIALOG_TALKING, true);
			socketNetworkHelper.write(text);
		}
		else
			Toast.makeText(this, "No text to send", Toast.LENGTH_SHORT).show();
		
	}
	
	public void writeToSocket(String text, byte[] file) {
		if(text.length() > 0){
			System.out.println("Write to socket String text");
			socketNetworkHelper.write(text);
			sendMessageToHandler(HANDLER_SHOW_PROGRESSDIALOG_TALKING, false);
			sendMessageToHandler(HANDLER_SHOW_PROGRESSDIALOG_TRANSFER, true);
			Thread uploadThread = new Thread(new Runnable(){
				@Override
				public void run() {
					try {
						socketNetworkHelper.writeFile(byteArray.toByteArray());
					} catch (IOException e) {
						Toast.makeText(c, "Error! IOException error! Unable to send file",  Toast.LENGTH_LONG).show();
						speech.speak("Error! IOException error! Unable to send file");
						e.printStackTrace();
					}	
					byteArray = null;
				}		
			});
			uploadThread.start();
		}
		else
			Toast.makeText(this, "No text to send", Toast.LENGTH_SHORT).show();	
	}
	public void writeToSocket(ArrayList<String> text){
		System.out.println("Write to socket StringArray");
		sendMessageToHandler(HANDLER_SHOW_PROGRESSDIALOG_TALKING, true);
		socketNetworkHelper.writeStringArray(text , true);
	}

	/**
	 * Reads from the socket established by the socketNetworkHelper and
	 * puts what has been read into the instance variable readStringInput
	 * 
	 */
	public String readFromSocket() {
		System.out.println("ReadFromSocket()");
		return socketNetworkHelper.read();
	}

	/**
	 * Notifies the handler that a component needs to be updated by sending it a Message
	 * 
	 * @param id The ID of the case that needs to be handled (see static ints)
	 * @param value the value that should be sent to the handler
	 */
	public void sendMessageToHandler(int id, Object value){
		Message msg = handler.obtainMessage(id);
		msg.obj = value;
		msg.sendToTarget();
	}

	private void setNotification(String text){
		String time = new SimpleDateFormat("kk:mm").format(Calendar.getInstance().getTime()); 
		NotificationCompat.Builder noti = new NotificationCompat.Builder(c).setStyle(new NotificationCompat.BigTextStyle().bigText("Last message from server at "+time+":\n" +text+"\n")).setContentTitle("SpeechWeaver")
	    		.setSmallIcon(R.drawable.swlogo);
		noti.setAutoCancel(true);
	    NotificationManager notificationManager = 
	    		(NotificationManager) getSystemService(NOTIFICATION_SERVICE);   
	    notificationManager.notify(0, noti.build());	    
	}

	public void handleSocketInput(String input){
		System.out.println("HANDLED INPUT: "+input);
		 if(input.contains(WAITING_FOR_TAG)){
				sendMessageToHandler(HANDLER_SET_BUTTON_STATUS, false);
				speech.speak(input.replace(WAITING_FOR_TAG, ""));
				sleepWhileSpeeking();				
				Intent intent = SpeechInputController.getSpeechIntent();
				if(hasFile)
					tagFlag = TAGPLUS;
				else
					tagFlag = TAG;
				sleepWhileSpeeking();
				startActivityForResult(intent, VOICE_INTENT_WRITE_ARRAY_REQUESTCODE);
		}
		else if(input.contains(FAILED_TO_FIND_ITEM)){
			speech.speak(input.replace(FAILED_TO_FIND_ITEM, ""));
			sendMessageToHandler(HANDLER_SET_BUTTON_STATUS, true);
		}
		else if(input.contains(FAILED_TO_EXECUTE)){
			speech.speak(input.replace("FTEC", ""));
			sendMessageToHandler(HANDLER_SET_BUTTON_STATUS, true);
		}
		else if(input.contains(CLOSEST_MATCHES)){
			sendMessageToHandler(HANDLER_SET_BUTTON_STATUS, false);
			String[] receivedArray = input.replace(CLOSEST_MATCHES, "").split(END_OF_LINE);
			speech.speak("These where the closes matches");
			sendMessageToHandler(HANDLER_SHOW_VIEW_DIALOG, receivedArray);
		}
		else if(input.contains(INFO)){	
			setNotification(input.replace(INFO, ""));
			sendMessageToHandler(HANDLER_SHOW_PROGRESSDIALOG_TRANSFER, false);
			sendMessageToHandler(HANDLER_SHOW_PROGRESSDIALOG_TALKING, false);
			speech.speak(input.replace(INFO, ""));
			if(hasContext)
				sendMessageToHandler(HANDLER_SET_BUTTON_STATUS, true);		
		}
		else if(input.contains(ROOT)){		
			sendMessageToHandler(HANDLER_SHOW_PROGRESSDIALOG_TALKING, false);
			sendMessageToHandler(HANDLER_RECEIVED_ROOT_NODES, input.replace(ROOT, "").split(END_OF_LINE));
		}
		else if(input.contains(CONTEXTNODE)){
			sendMessageToHandler(HANDLER_RECEIVED_CONTEXT_NODES, input.replace(CONTEXTNODE, "").split(END_OF_LINE));
		}
		sleepWhileSpeeking();
	}

	public void setButtonStatus(boolean bool){	
		speakButton.setEnabled(bool);
		generateButton.setEnabled(bool);
		if(hasFile){
			attachFileButton.setEnabled(false);
			clearAttachedFileButton.setEnabled(true);
		}else{
			attachFileButton.setEnabled(true);
			clearAttachedFileButton.setEnabled(false);
		}		
	}
	
	public void sleepWhileSpeeking(){
		while(speech.isSpeaking()){
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}	
	}
	
	public String[] subStringArray(String[] originArray, int floor, int roof){
		String[] tempArray = new String[roof-floor];

		for(int i = floor; i < roof; i++){
			tempArray[i] =  originArray[i];
		}
		return tempArray;
	}
	
	private void createViewDialogForRootOrContext(final String TAG, final String[] nodes){	
		AlertDialog.Builder rootNodeDialogBuilder = new AlertDialog.Builder(NetworkActivity.this);
		rootNodeDialogBuilder.setTitle("Nodes");
		rootNodeDialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {		
			@Override
			public void onCancel(DialogInterface dialog) {
			}
		});
		rootNodeDialogBuilder.setItems(nodes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(TAG == ROOT)
					contextTitle.setText(contextTitle.getText()+nodes[which]+"/");
				else if(TAG == CONTEXTNODE){
					hasContext = true;
					contextTitle.setText(contextTitle.getText()+nodes[which]+"/");
					sendMessageToHandler(HANDLER_SET_BUTTON_STATUS, true);
				}
				writeToSocket(TAG + nodes[which]);
			}					
		});
		rootNodeDialogBuilder.show();
	}
	
	/*
	 * A handler for updating the UI. The handler is created in the UI-thread so
	 * that it can be called and update UI-components from other threads.
	 * 
	 * The handler should be updated through the sendMessageToHandler-method 
	 */
	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HANDLER_SHOW_PROGRESSDIALOG_CONNECT:
				boolean show = (Boolean) msg.obj;
				if (show && !NetworkActivity.this.isFinishing()) {
					progressDialog = ProgressDialog.show(
							NetworkActivity.this, "Connecting",
							"Trying to connect...");
					progressDialog.setCancelable(true);
					progressDialog.setCanceledOnTouchOutside(false);
				} else if (!show && progressDialog.isShowing()) {
					progressDialog.dismiss();
				}
				break;
			case HANDLER_SHOW_PROGRESSDIALOG_TALKING:
				show = (Boolean) msg.obj;
				if (show && !NetworkActivity.this.isFinishing() && !progressDialog.isShowing()) {
					progressDialog = ProgressDialog.show(
							NetworkActivity.this, "Talking with server",
							"Waiting...");
					progressDialog.setCancelable(true);
					progressDialog.setCanceledOnTouchOutside(false);
				} else if (!show && progressDialog.isShowing()) {
					progressDialog.dismiss();
				}
				break;
			case HANDLER_SHOW_PROGRESSDIALOG_TRANSFER:
				show = (Boolean) msg.obj;
				if (show && !NetworkActivity.this.isFinishing() && !progressDialog.isShowing()) {
					progressDialog = ProgressDialog.show(
							NetworkActivity.this, "Transferring file",
							"File: "+fileName );
					progressDialog.setCancelable(true);
					progressDialog.setCanceledOnTouchOutside(false);
				} else if (!show && progressDialog.isShowing()) {
					progressDialog.dismiss();
				}
				break;
			case HANDLER_CONNECT_STATUS:
				boolean isAlive = (Boolean) msg.obj;
				if (isAlive) {
					Toast.makeText(NetworkActivity.this,
							"Connected to " + ip + ":" + port,
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(NetworkActivity.this,
							"Disconnected", Toast.LENGTH_SHORT).show();
					setButtonStatus(false);
				}
				break;
			case HANDLER_SET_BUTTON_STATUS:
				setButtonStatus((Boolean) msg.obj);
				break;
			case HANDLER_SHOW_VIEW_DIALOG:
				final String[] choices = (String[]) msg.obj;
				int floor = 0, roof = choices.length;
				if(choices.length > 10){
					roof = 10;
				}
				String[] subChoices = subStringArray(choices, floor, roof);	
				AlertDialog.Builder choicesDialogBuilder = new AlertDialog.Builder(NetworkActivity.this);
				choicesDialogBuilder.setTitle("Found items");
				choicesDialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {		
					@Override
					public void onCancel(DialogInterface dialog) {
						sendMessageToHandler(HANDLER_SET_BUTTON_STATUS, true);
					}
				});
				choicesDialogBuilder.setItems(subChoices, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						System.out.println("OnClickDialog: "+which );						
						writeToSocket(SELECTION + choices[which]);
						requirementTitle.setText("Requirement: "+choices[which]);
					}					
				});
				choicesDialogBuilder.show();				
				break;
			case HANDLER_SET_REQUIREMENT_TITLE:
				String title = (String) msg.obj;
				requirementTitle.setText("Requirement: " +title);
				break;
			case HANDLER_SET_ANNOTATION_TITLE:
				String annot = (String) msg.obj;
				annotationTitle.setText("Annotation: " +annot);
				break;
			case HANDLER_RECEIVED_ROOT_NODES:
				createViewDialogForRootOrContext(ROOT, (String[]) msg.obj);
				break;	
			case HANDLER_RECEIVED_CONTEXT_NODES:
				createViewDialogForRootOrContext(CONTEXTNODE, (String[]) msg.obj);
				break;
			}				
		}
	};
	
	Thread networkThread = new Thread(new Runnable(){
		public void run(){		
			socketNetworkHelper = SocketNetworkHelper.getSocketNetworkHelper(ip,
					port);
			reconnectSocketHelper();
			if(!speech.isReady()){
				for(int i = 0; i < 5; i++){
					if(speech.isReady())
						break;
					try {
						Thread.currentThread().sleep(200);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}				
			}
			
			while (runThread && socketNetworkHelper.isAlive()) {
				readStringInput = readFromSocket();		
				if(readStringInput == null){
					sendMessageToHandler(HANDLER_SHOW_PROGRESSDIALOG_CONNECT, false);
					System.out.println("readInput = null");
					socketNetworkHelper.close();
					sendMessageToHandler(HANDLER_CONNECT_STATUS, socketNetworkHelper.isAlive());
				}
				else if(readStringInput.contains(ERROR)){
					sendMessageToHandler(HANDLER_SHOW_PROGRESSDIALOG_CONNECT, false);
					speech.speak(readStringInput.replace(ERROR, ""));			
				}
				else if (readStringInput.length() > 0) {
					sendMessageToHandler(HANDLER_SHOW_PROGRESSDIALOG_CONNECT, false);
					handleSocketInput(readStringInput);
				}					
			}
			speech.speak("Disconnected");
			sleepWhileSpeeking();
			finish();
		}
	});
	
}