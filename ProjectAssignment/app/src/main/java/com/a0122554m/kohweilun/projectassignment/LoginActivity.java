package com.a0122554m.kohweilun.projectassignment;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;

public class LoginActivity extends Activity {
    CallbackManager callbackManager;
    ProfileTracker profileTracker;

    private static final String FB_SHAREDPREF_FOR_APP = "FbSharedPrefForApp";
    private String[] lesson_progress_titles;
    private int[] lesson_progress_last_seens;
    private int[] lesson_progress_furthests;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        SharedPreferences prefs = getSharedPreferences(FB_SHAREDPREF_FOR_APP, MODE_PRIVATE);
        String fb_email = prefs.getString("email", null);

        // Set up Login Button.
        LoginButton loginButton = findViewById(R.id.login_page_button);
        loginButton.setReadPermissions(Arrays.asList("public_profile", "email"));

        FacebookSdk.sdkInitialize(this.getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        profileTracker = new ProfileTracker() {
            @Override
            protected void onCurrentProfileChanged(Profile oldProfile, Profile currentProfile) {
                // App code
                //log in
                if (currentProfile != null){
                    Intent mainPage = new Intent(getApplicationContext(),MainActivity.class);
                    startActivity(mainPage);
                }
                //log out
                if (currentProfile == null){
                    Toast.makeText(LoginActivity.this, "FB Logout success!", Toast.LENGTH_SHORT).show();
                    Intent loginPage = new Intent(getApplicationContext(),LoginActivity.class);
                    startActivity(loginPage);
                    SharedPreferences.Editor editor = getSharedPreferences(FB_SHAREDPREF_FOR_APP, MODE_PRIVATE).edit();
                    editor.putString("email", null);
                    editor.apply();
                }
            }
        };

        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        // App code
                        Toast.makeText(LoginActivity.this, "FB Login success!", Toast.LENGTH_SHORT).show();
                        GraphRequest request = GraphRequest.newMeRequest(
                                loginResult.getAccessToken(),
                                new GraphRequest.GraphJSONObjectCallback() {
                                    @Override
                                    public void onCompleted(JSONObject object, GraphResponse response) {
                                        Log.v("LoginActivity", response.toString());

                                        // Application code
                                        try {
                                            String email = object.getString("email");
                                            String name = object.getString("name");
                                            GetUserAsyncTask getUserAsyncTask = new GetUserAsyncTask();
                                            getUserAsyncTask.execute("http://192.168.137.1:3000/api/user/login?email=" + email + "&name=" + name);
                                            SharedPreferences.Editor editor = getSharedPreferences(FB_SHAREDPREF_FOR_APP, MODE_PRIVATE).edit();
                                            editor.putString("email", email);
                                            editor.apply();
                                        } catch (JSONException exception) {
                                            Toast.makeText(LoginActivity.this, "There is an error accessing the attributes.", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                        Bundle parameters = new Bundle();
                        parameters.putString("fields", "id,name,email");
                        request.setParameters(parameters);
                        request.executeAsync();
                    }

                    @Override
                    public void onCancel() {
                        // App code
                        Toast.makeText(LoginActivity.this, "FB Login cancel!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        // App code
                        Toast.makeText(LoginActivity.this, "FB Login error!", Toast.LENGTH_SHORT).show();
                    }
                });

        if (fb_email != null){
            Intent mainPage = new Intent(getApplicationContext(),MainActivity.class);
            startActivity(mainPage);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        profileTracker.stopTracking();
    }

    private class GetUserAsyncTask extends AsyncTask<String, Void, String> {

        public String doInBackground(String... str) {
            URL url = convertToUrl(str[0]);
            HttpURLConnection httpURLConnection = null;
            int responseCode;
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            try {
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.connect();
                responseCode = httpURLConnection.getResponseCode();
                if (responseCode == httpURLConnection.HTTP_OK) {
                    InputStream inputStream = httpURLConnection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line);
                    }
                    inputStream.close();
                }
            } catch (Exception e) {
                System.out.println("Error : " + e.getMessage());
                e.printStackTrace();
            } finally {
                assert httpURLConnection != null;
                httpURLConnection.disconnect();
            }

            return stringBuilder.toString();
        }

        public void onPostExecute(String result) {
            try {
                JSONArray listOfLessonProgress = new JSONArray(result);
                int numOfLessonProgress = listOfLessonProgress.length();
                lesson_progress_titles = new String[numOfLessonProgress];
                lesson_progress_last_seens = new int[numOfLessonProgress];
                lesson_progress_furthests = new int[numOfLessonProgress];

                for (int i = 0; i < listOfLessonProgress.length(); i++) {
                    final JSONObject lessonProgressDetails = listOfLessonProgress.getJSONObject(i);
                    //do shared preference stuff
                }
            } catch (Exception e) {
                System.out.println("Error : " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // method from internet to handle url stuff
    private URL convertToUrl(String urlStr) {
        try {
            URL url = new URL(urlStr);
            URI uri = new URI(url.getProtocol(), url.getUserInfo(),
                    url.getHost(), url.getPort(), url.getPath(),
                    url.getQuery(), url.getRef());
            url = uri.toURL();
            return url;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
