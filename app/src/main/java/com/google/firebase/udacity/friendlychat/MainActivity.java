package com.google.firebase.udacity.friendlychat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.firebase.udacity.chat.BuildConfig;
import com.google.firebase.udacity.chat.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    public static final String FRIENDLY_MSG_LENGTH_KEY = "friendly_msg_length";

    public static final int RC_SIGN_IN = 1;
    private static final int RC_PHOTO_PICKER = 2;

    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;

    private String mUsername;

    // Firebase instance variables
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMessagesDatabaseReference;
    private ChildEventListener mChildEventListener;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mChatPhotosStorageReference;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    String onResumeCaller = "onCreate";
    boolean signedIn = false;
    boolean isUploadingPic = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsername = ANONYMOUS;

        // Initialize Firebase components
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        mMessagesDatabaseReference = mFirebaseDatabase.getReference().child("messages");
        mChatPhotosStorageReference = mFirebaseStorage.getReference().child("chat_photos");

        // Initialize references to views
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageListView = (ListView) findViewById(R.id.messageListView);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);

        mMessageEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                scrollMyListViewToBottom();
                return true;
            }
        });

        //findViewById(R.id.refresh).setVisibility(View.GONE);

        // Initialize message ListView and its adapter
        List<FriendlyMessage> friendlyMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, friendlyMessages);
        mMessageListView.setAdapter(mMessageAdapter);

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isUploadingPic = true;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
            }
        });

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FriendlyMessage friendlyMessage = new FriendlyMessage(mMessageEditText.getText().toString(), mUsername, System.currentTimeMillis() / 1000L, null);
                mMessagesDatabaseReference.push().setValue(friendlyMessage);

                // Clear input box
                mMessageEditText.setText("");
            }
        });

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    onSignedInInitialize(user.getDisplayName());
                    onResumeCaller = "";
                    signInNotif();
                } else {

                    //FriendlyMessage friendlyMessage = new FriendlyMessage("", mUsername + " is now offline", System.currentTimeMillis() / 1000L, null);

                    signedIn = false;
                    // User is signed out
                    onSignedOutCleanup();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setTheme(R.style.FirebaseUIAuth)
                                    .setProviders(
                                            AuthUI.EMAIL_PROVIDER,
                                            AuthUI.GOOGLE_PROVIDER)
                                    .build(),
                            RC_SIGN_IN);
                    getSupportActionBar().setTitle("FireChat Login");
                }
            }
        };

        // Create Remote Config Setting to enable developer mode.
        // Fetching configs from the server is normally limited to 5 requests per hour.
        // Enabling developer mode allows many more requests to be made per hour, so developers
        // can test different config values during development.
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        mFirebaseRemoteConfig.setConfigSettings(configSettings);

        // Define default config values. Defaults are used when fetched config values are not
        // available. Eg: if an error occurred fetching values from the server.
        Map<String, Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put(FRIENDLY_MSG_LENGTH_KEY, DEFAULT_MSG_LENGTH_LIMIT);
        mFirebaseRemoteConfig.setDefaults(defaultConfigMap);
        fetchConfig();
        mMessageListView.addFooterView(new TextView(MainActivity.this));
        mMessageListView.addHeaderView(new TextView(MainActivity.this));
        //pictureRefresh();
    }

    private void pictureRefresh() {
        final int index = mMessageListView.getFirstVisiblePosition();
        View v = mMessageListView.getChildAt(0);
        final int top = (v == null) ? 0 : (v.getTop() - mMessageListView.getPaddingTop());
        mMessageListView.invalidateViews();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                mMessageListView.setSelectionFromTop(index, top);
                pictureRefresh();
            }
        }, 5000);
    }

    private void signInNotif() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                if (!mUsername.equals("anonymous") && !signedIn) {
                    FriendlyMessage friendlyMessage = new FriendlyMessage("", mUsername + " is now online", System.currentTimeMillis() / 1000L, null);
                    mMessagesDatabaseReference.push().setValue(friendlyMessage);
//            mMessageAdapter.add(friendlyMessage);
//            scrollMyListViewToBottom();
                }
                signedIn = true;
            }
        }, 1000);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != RC_SIGN_IN) {
            if (requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK) {
                Uri selectedImageUri = data.getData();

                // Get a reference to store file at chat_photos/<FILENAME>
                assert selectedImageUri != null;
                StorageReference photoRef = mChatPhotosStorageReference.child(selectedImageUri.getLastPathSegment());

                // Upload file to Firebase Storage
                photoRef.putFile(selectedImageUri)
                        .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                // When the image has successfully uploaded, we get its download URL
                                Uri downloadUrl = taskSnapshot.getDownloadUrl();

                                // Set the download URL to the message box, so that the user can send it to the database
                                FriendlyMessage friendlyMessage = new FriendlyMessage("", mUsername, System.currentTimeMillis() / 1000L,  downloadUrl.toString());
                                mMessagesDatabaseReference.push().setValue(friendlyMessage);
                            }
                        });
            }
        } else {
            if (resultCode == RESULT_OK) {
                // Sign-in succeeded, set up the UI
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                // Sign in was canceled by the user, finish the activity
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        Log.e("onResume", onResumeCaller);
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
        if (!mUsername.equals("anonymous") && onResumeCaller.equals("") && !isUploadingPic) {
            attachDatabaseReadListener();
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    FriendlyMessage friendlyMessage = new FriendlyMessage("", mUsername + " is now online", System.currentTimeMillis() / 1000L, null);
                    mMessagesDatabaseReference.push().setValue(friendlyMessage);
                }
            }, 1000);
        }
        if (isUploadingPic) {
            isUploadingPic = false;
        }
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!mUsername.equals("anonymous") && !isUploadingPic) {
            FriendlyMessage friendlyMessage = new FriendlyMessage("", mUsername + " is now offline", System.currentTimeMillis() / 1000L, null);
            mMessagesDatabaseReference.push().setValue(friendlyMessage);
        }
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
    }

    @Override
    protected void onDestroy() {
        detachDatabaseReadListener();
        mMessageAdapter.clear();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                FriendlyMessage friendlyMessage = new FriendlyMessage("", mUsername + " is now offline", System.currentTimeMillis() / 1000L, null);
                mMessagesDatabaseReference.push().setValue(friendlyMessage);
                Log.e("Logout", mUsername);
                AuthUI.getInstance().signOut(this);
                return true;
//            case R.id.refresh:
//                super.onPause();
//                mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
//                super.onStop();
//                mMessageAdapter.clear();
//                List<FriendlyMessage> friendlyMessages = new ArrayList<>();
//                mMessageAdapter = new MessageAdapter(this, R.layout.item_message, friendlyMessages);
//                mMessageListView.setAdapter(mMessageAdapter);
//                super.onResume();
//                mFirebaseAuth.addAuthStateListener(mAuthStateListener);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onSignedInInitialize(String username) {
        mUsername = username;
        attachDatabaseReadListener();
    }

    private void onSignedOutCleanup() {
        mUsername = ANONYMOUS;
        mMessageAdapter.clear();
        detachDatabaseReadListener();
        getSupportActionBar().setTitle("FireChat Login");
    }

    private void scrollMyListViewToBottom() {
        mMessageListView.post(new Runnable() {
            @Override
            public void run() {
                // Select the last row so it will scroll into view...
                mMessageListView.invalidateViews();
                mMessageListView.setSelection(mMessageAdapter.getCount() - 1);
            }
        });
    }

    private void attachDatabaseReadListener() {
        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    FriendlyMessage friendlyMessage = dataSnapshot.getValue(FriendlyMessage.class);
                    mMessageAdapter.add(friendlyMessage);
                    scrollMyListViewToBottom();
                    mMessageListView.invalidateViews();
                }

                public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    Log.e("Removing...", "Removing...");
                    mMessageAdapter.clear();
                }
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                public void onCancelled(DatabaseError databaseError) {}
            };
            mMessagesDatabaseReference.addChildEventListener(mChildEventListener);
        }
    }

    private void detachDatabaseReadListener() {
        if (mChildEventListener != null) {
            mMessagesDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }

    // Fetch the config to determine the allowed length of messages.
    public void fetchConfig() {
        long cacheExpiration = 3600; // 1 hour in seconds
        // If developer mode is enabled reduce cacheExpiration to 0 so that each fetch goes to the
        // server. This should not be used in release builds.
        if (mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }
        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Make the fetched config available
                        // via FirebaseRemoteConfig get<type> calls, e.g., getLong, getString.
                        mFirebaseRemoteConfig.activateFetched();

                        // Update the EditText length limit with
                        // the newly retrieved values from Remote Config.
                        applyRetrievedLengthLimit();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // An error occurred when fetching the config.
                        Log.w(TAG, "Error fetching config", e);

                        // Update the EditText length limit with
                        // the newly retrieved values from Remote Config.
                        applyRetrievedLengthLimit();
                    }
                });
    }

    /**
     * Apply retrieved length limit to edit text field. This result may be fresh from the server or it may be from
     * cached values.
     */
    private void applyRetrievedLengthLimit() {
        Long friendly_msg_length = mFirebaseRemoteConfig.getLong(FRIENDLY_MSG_LENGTH_KEY);
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(friendly_msg_length.intValue())});
        Log.d(TAG, FRIENDLY_MSG_LENGTH_KEY + " = " + friendly_msg_length);
    }
}