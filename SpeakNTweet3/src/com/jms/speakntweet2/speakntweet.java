package com.jms.speakntweet2;

/**
* Implemented WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* 
* Contact: johnm@rga.com
* 
* HISTORY:
* 11/21/09 Created
* 12/22/09 ver0.1.6 simple status update
*/

/**
 * ---
 * To Publish:
 * $ keytool -genkey -v -keystore speakntweet-key.keystore -alias speakntweet -keyalg RSA -validity 10000
 * jarsigner -verbose -keystore /Users/johnmayo-smith/signed_apps/speakntweet-key.keystore /Users/johnmayo-smith/signed_apps/speakntweet.apk speakntweet
 * 
 * TEST: adb shell top -s vss -m 10c
 * 
 * right click on package name and select Android Tools --> export unsigned...
 * keystore and apk should be in the same directory
 * 
 * 
 * 
 *  Logo palette
 * 
 * 51,204,255 twitter blue
 * 18,138,178
 * 76,210,255
 * 178,91,0
 * 255,155,51
 */

//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.util.List;

//import org.apache.http.client.ClientProtocolException;
//import org.apache.http.client.methods.HttpGet;
//import org.apache.http.impl.client.DefaultHttpClient;
//import org.apache.http.params.BasicHttpParams;
//import org.apache.http.params.HttpConnectionParams;
//import org.apache.http.params.HttpParams;
//import twitter4j.AsyncTwitter;
//import twitter4j.http.AccessToken;
//import twitter4j.http.RequestToken;
//import android.content.DialogInterface.OnCancelListener;
//import android.text.Editable;
//import android.view.Window;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;


/*
 * class speakntweet
 * 
 * DESCRIPTION: main activity
 * 
 * HISTORY:
 * Created 12/22/2009
 */

public class speakntweet extends Activity {
/* For OAuth: 
	public static final String REQUEST_TOKEN_URI = "http://twitter.com/oauth/request_token";
	public static final String ACCESS_TOKEN_URI = "http://twitter.com/oauth/access_token";
	public static final String AUTHORIZE_TOKEN_URI = "http://twitter.com/oauth/authorize";
	public static final String CONSUMER_KEY = "UbeTJIFrs4IAOKfaEYFIbA"; //UbeTJIFrs4IAOKfaEYFIbA
	public static final String CONSUMER_SECRET = "jbrttpxVRhUp0xZyJpJN9EDPMiqCbB1kw6tNLlE8V8"; //jbrttpxVRhUp0xZyJpJN9EDPMiqCbB1kw6tNLlE8V8
	public static final String CALLBACK_URL = "snt://TestApp55";
*/	
	private static final int VOICE_RECOGNITION_REQUEST_CODE = 1235; //unique Recognizer Intent identifier

	//Twitter Messages
	public static final int T_SUCCESS = 1;
	public static final int T_MAIN = 2;
	public static final int T_EMPTY = 3;
	public static final int T_LOGIN = 4;

	public static final String T_SOURCE = "SpeaknTweet";

	public String matches = null;
	private ProgressDialog pd;
	protected Message Tmsg = null;

	private Handler handler = new Handler() {

		public void handleMessage(Message msg) {

			if(pd!=null)
				pd.dismiss();

			switch(msg.what){
			case T_EMPTY:
				MessageDialog("Nothing to tweet.");
				break;

			case T_SUCCESS:

				MessageDialog("Tweet Success!");

				break;

			case T_MAIN:
				MessageDialog("Ah, something's up. Please check connection, and double-check username and password.");
				break;

			case T_LOGIN:
				showTwitterLogIn();
				break;
			}

		}
	};


	/* *********************************************************************************
	 * Default Preferences
	 * *********************************************************************************/

	protected static Dialog dPref;
	public static final String PREFS_NAME = "SpeakNTweetPrefs2";

	//Preference file keys...
	static final private String PREF_U = "username"; 
	static final private String PREF_P = "password"; 
	static final private String PREF_HASH = "prepend";

	public static final int ABOUT_ID = 1;
	public static final int PREFS_ID = 2;

	//other
	protected String mUN=null;
	protected String mPW=null;
	protected String mPre=null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/**
		 * Landscape or Portrait
		 */

		if(getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE){ //landscape
			setContentView(R.layout.main_land);
		};

		if(getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT){ //portrait
			setContentView(R.layout.main);
		};

		((Button) findViewById(R.id.Tweet)).setOnClickListener(mButtons); 
		((Button) findViewById(R.id.Speak)).setOnClickListener(mButtons);

		initializeSettings();

		if(mUN == "" || mPW =="")
			showTwitterLogIn();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		// create two menus. 
		menu.add(0, PREFS_ID, 0, R.string.Menu_1).setShortcut('1', 'x').setIcon(android.R.drawable.ic_menu_directions);
		menu.add(0, ABOUT_ID, 0, R.string.Menu_2).setShortcut('2', 'a').setIcon(android.R.drawable.ic_menu_info_details);    

		return true;
	}


	/*
	public static void oAuth(String args[]) throws Exception{
		Twitter twitter = new Twitter();
		twitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
		RequestToken requestToken = twitter.getOAuthRequestToken();
		AccessToken accessToken = null;

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while (null == accessToken) {
			System.out.println("Open the following URL and grant access to your account:"+REQUEST_TOKEN_URI);
			System.out.println(requestToken.getAuthorizationURL());
			System.out.print("Enter the PIN(if aviailable) or just hit enter.[PIN]:");
			String pin = br.readLine();
			try{
				if(pin.length() > 0){
					accessToken = twitter.getOAuthAccessToken(requestToken, pin);
				}else{
					accessToken = requestToken.getAccessToken();
				}
			} catch (TwitterException te) {
				if(401 == te.getStatusCode()){
					System.out.println("Unable to get the access token.");
				}else{
					te.printStackTrace();
				}
			}
		}
		//persist to the accessToken for future reference.
		storeAccessToken(twitter.verifyCredentials().getId() , accessToken);
		Status status = twitter.updateStatus(args[0]);
		System.out.println("Successfully updated status to [" + status.getText() + "].");
		System.exit(0);
	}
	 */


	/*
	 * OnClickListener()
	 * 
	 * DESCRIPTION: Main ClickListener for Speak and Tweet buttons 
	 * 
	 * HISTORY:
	 * Created 12/22/2009
	 */

	OnClickListener mButtons = new OnClickListener(){

		public void onClick(View v) {

			switch(v.getId()){

			case R.id.Tweet:

				pd = ProgressDialog.show( speakntweet.this, "Working" , "Contacting Twitter. Please wait ... ", true);

				new Thread(new Runnable() {
					public void run() {
						ATweet();
					}

					public void ATweet() {

						if(mUN != "" && mPW !=""){
							Twitter twitter = new Twitter(mUN,mPW);
							Status status = null;
							EditText et = (EditText) findViewById(R.id.TweetDisplay);
							String mTweet = mPre + " " + et.getText().toString();
							if(mTweet.length() > 0){
								try {

									twitter.setSource(T_SOURCE);
									status = twitter.updateStatus(mTweet);
									//	statusesList = twitter.getUserTimeline();

								} catch (TwitterException e) {

									if(status == null)

										Log.i("error", "main issue");
									handler.sendEmptyMessage(T_MAIN);
								}

								if(status!=null){
									Log.i("success", "status: " + status.getText());
									handler.sendEmptyMessage(T_SUCCESS);
								}
							}	
							else{
								Log.i("error", "nothing to tweet");
								handler.sendEmptyMessage(T_EMPTY);
							}
						}
						else{
							Log.i("error", "show LogIn");
							handler.sendEmptyMessage(T_LOGIN);
						}
					}
				}).start();

				break;


			case R.id.Speak:

				try {
					Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);//ACTION_WEB_SEARCH);
					//i.setPackage("com.google.android.voicesearch2");
					i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
							RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

					i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Preparing your Tweet..");
					startActivityForResult(i, VOICE_RECOGNITION_REQUEST_CODE);

				} catch (Exception e) {
					MessageDialog("Google speach recognition error");
					Log.e("Exception ","Exception RecognizerIntent"); 
					e.printStackTrace(); 
					return;
				}

				break;
			}
		}
	};


	/*
	 * onActivityResult()
	 * 
	 * DESCRIPTION: Speach Recognition Activity Result
	 * 
	 * HISTORY:
	 * Created 12/22/2009
	 */

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		String stemp=null;

		if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {

			// Fill the list view with the strings the recognizer thought it could have heard
			matches = String.valueOf(data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS));

			stemp = matches.replace("[", " "); //clean up [
			matches = stemp.replace("]", ""); //clean up ]			


		}   
		else
			matches = ""; 

		EditText et = (EditText) findViewById(R.id.TweetDisplay);
		et.setText(matches);

		super.onActivityResult(requestCode, resultCode, data);
	}


	/* *********************************************************************************
	 * Dialogs and Menu Interface
	 * *********************************************************************************/

	void showTwitterLogIn(){

		dPref = new Dialog(this); 
		dPref.setContentView(R.layout.setup); //set the view
		dPref.setTitle("Twitter Settings");
		((Button) dPref.findViewById(R.id.dLogin)).setOnClickListener(mLoginButton); 
		EditText et = (EditText) dPref.findViewById(R.id.SetupUN);
		et.setText(mUN);
		et = (EditText) dPref.findViewById(R.id.SetupPW);
		et.setText(mPW);
		et = (EditText) dPref.findViewById(R.id.EditTextPre);
		et.setText(mPre);
		dPref.show(); //show the dialog	
	}


	//About
	void AboutDialog()

	{
		new AlertDialog.Builder(this).
		setTitle(R.string.hello).setMessage("SpeaknTweet software is distributed on an AS IS BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.Copyright 2009 FDCC. Portions Copyright Yusuke Yamamoto and Google. Contact mayoinmotion@gmail.com").
		setPositiveButton("ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		}).show();
	}


	/*
	 * MessageDialog
	 * 
	 * DESCRIPTION: Creates a generic OK message dialog
	 * 
	 * HISTORY:
	 * Created 12/22/2009
	 */
	void MessageDialog(String msg){
		new AlertDialog.Builder(this).
		setTitle("Status").setMessage(msg).
		setPositiveButton("ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		}).show();
	}



	/*
	 * OnClickListener()
	 * 
	 * DESCRIPTION: Login Button ClickListener
	 * HISTORY:
	 * Created 12/22/2009
	 */
	OnClickListener mLoginButton = new OnClickListener() {

		public void onClick(View v) {
			switch(v.getId()){

			case R.id.dLogin:

				EditText vUN = (EditText) dPref.findViewById(R.id.SetupUN);
				EditText vPW = (EditText) dPref.findViewById(R.id.SetupPW);
				EditText vPre = (EditText) dPref.findViewById(R.id.EditTextPre);

				if(vUN.getText().toString() !="" &&
						vPW.getText().toString() !="" ){	 

					mUN = vUN.getText().toString(); //set Username
					mPW = vPW.getText().toString(); //set Password
					mPre = vPre.getText().toString(); //set Password

					SavePreferenceValues(); //save Preferences

					dPref.dismiss(); //close dialog
				}
				break;
			}
		}
	};

	/*
	 * SavePreferenceValues()
	 * 
	 * DESCRIPTION: Save Preference Values
	 * 
	 * HISTORY:
	 * Created 12/22/2009
	 */
	protected void SavePreferenceValues(){

		SharedPreferences settings = getSharedPreferences(PREFS_NAME,0); //access the preference file
		SharedPreferences.Editor editor = settings.edit();

		editor.clear();

		editor.putString(PREF_U, mUN);
		editor.putString(PREF_P, mPW);
		editor.putString(PREF_HASH, mPre);

		editor.commit();
	}


	/*
	 * SetPreferenceDialogDefaultValues()
	 * 
	 * DESCRIPTION: Set Preference Values
	 * 
	 * HISTORY:
	 * Created 12/22/2009
	 */
	protected void SetPreferenceDialogDefaultValues(Dialog d){  

		SharedPreferences settings = getSharedPreferences(PREFS_NAME,0);

		EditText et = (EditText) d.findViewById(R.id.EditTextUN);
		et.setText(String.valueOf(settings.getString(PREF_U, "")));
		et = (EditText) d.findViewById(R.id.EditTextPW);
		et.setText(String.valueOf(settings.getString(PREF_P, "")));
		et = (EditText) d.findViewById(R.id.EditTextPre);
		et.setText(String.valueOf(settings.getString(PREF_HASH, "#speakntweet ")));
	} 


	/*
	 * initializeSettings()
	 * 
	 * DESCRIPTION: Initialize Settings from Preference Dialog
	 * 
	 * HISTORY:
	 * Created 12/10/2009
	 */
	public void initializeSettings(){

		SharedPreferences settings = getSharedPreferences(PREFS_NAME,0);

		//Get username setting from preferences
		try {
			mUN = settings.getString(PREF_U, "" );
		} catch (Exception e) {
			Log.e("Preference Error","Username Error"); 
		}

		//Get pw setting from preferences
		try {
			mPW = settings.getString(PREF_P, "" );
		} catch (Exception e) {
			Log.e("Preference Error","Password Error"); 
		}
		//Get hashtag setting from preferences
		try {
			mPre = settings.getString(PREF_HASH, "#speakntweet " );
		} catch (Exception e) {
			Log.e("Preference Error","Hash Error"); 
		}
	}

	/**
	 * Called when a menu item is selected.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case ABOUT_ID:
			AboutDialog();
			return true;

		case PREFS_ID:
			showTwitterLogIn();

		}
		return super.onOptionsItemSelected(item);
	}

}