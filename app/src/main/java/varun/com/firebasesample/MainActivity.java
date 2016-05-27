package varun.com.firebasesample;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.io.File;
import java.util.UUID;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {

    private static final int RC_TAKE_PICTURE = 101;
    private static final int RC_STORAGE_PERMS = 102;

    private static final String KEY_FILE_URI = "key_file_uri";
    private static final String KEY_DOWNLOAD_URL = "key_download_url";

    private static final String TAG = "MainActivity";

    private static final String KEY = "key";

    FirebaseAnalytics firebaseAnalytics;
    FirebaseRemoteConfig firebaseRemoteConfig;

    Button subscribeButton, logTokenButton, clickPhotoButton, uploadButton, downloadButton;
    FloatingActionButton fab;
    TextView remoteConfigTextView;

    private Uri mDownloadUrl = null;
    private Uri mFileUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        firebaseAnalytics = FirebaseAnalytics.getInstance(MainActivity.this);
        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        final CheckBox catchCrashCheckBox = (CheckBox) findViewById(R.id.catchCrashCheckBox);

        remoteConfigTextView = (TextView) findViewById(R.id.remote_config);
        subscribeButton = (Button) findViewById(R.id.subscribeButton);
        logTokenButton = (Button) findViewById(R.id.logTokenButton);
        clickPhotoButton = (Button) findViewById(R.id.clickPhotoButton);
        uploadButton = (Button) findViewById(R.id.uploadButton);
        downloadButton = (Button) findViewById(R.id.downloadButton);
        fab = (FloatingActionButton) findViewById(R.id.fab);

        if (savedInstanceState != null) {
            mFileUri = savedInstanceState.getParcelable(KEY_FILE_URI);
            mDownloadUrl = savedInstanceState.getParcelable(KEY_DOWNLOAD_URL);
        }

        //REMOTE CONFIG
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        firebaseRemoteConfig.setConfigSettings(configSettings);

        firebaseRemoteConfig.setDefaults(R.xml.default_remote_config);

        fetchData();

        //CLOUD NOTIFICATION
        assert subscribeButton != null;
        subscribeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseMessaging.getInstance().subscribeToTopic("news");
                Log.e(TAG, "Subscribed to news topic");
            }
        });

        assert logTokenButton != null;
        logTokenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "InstanceID token: " + FirebaseInstanceId.getInstance().getToken());
            }
        });

        //CRASH ANALYTICS
        //ANALYTICS
        assert fab != null;
        fab.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        firebaseAnalytics.setUserProperty("user_name", "Varun");

                        FirebaseCrash.logcat(Log.INFO, TAG, "Crash button clicked");

                        if (catchCrashCheckBox.isChecked()) {
                            try {
                                throw new NullPointerException();
                            } catch (NullPointerException ex) {
                                FirebaseCrash.logcat(Log.ERROR, TAG, "NPE caught");
                                FirebaseCrash.report(ex);
                            }
                        } else {
                            throw new NullPointerException();
                        }
                    }
                });

        //STORAGE
        clickPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putParcelable(KEY_FILE_URI, mFileUri);
        out.putParcelable(KEY_DOWNLOAD_URL, mDownloadUrl);
    }

    @AfterPermissionGranted(RC_STORAGE_PERMS)
    private void launchCamera() {
        String perm = Manifest.permission.READ_EXTERNAL_STORAGE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !EasyPermissions.hasPermissions(MainActivity.this, perm)) {
            EasyPermissions.requestPermissions(this, "This app needs images from your phone",
                    RC_STORAGE_PERMS, perm);
            return;
        }

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Choose file storage location
        File file = new File(Environment.getExternalStorageDirectory(), UUID.randomUUID().toString() + ".jpg");
        mFileUri = Uri.fromFile(file);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mFileUri);

        // Launch intent
        startActivityForResult(takePictureIntent, RC_TAKE_PICTURE);
    }

    //REMOTE CONFIG
    private void fetchData() {

        remoteConfigTextView.setText(firebaseRemoteConfig.getString(KEY));

        long cacheExpiration = 3600; // 1 hour in seconds.
        if (firebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }

        firebaseRemoteConfig.fetch(cacheExpiration)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.e(TAG, "Fetch Succeeded");
                            firebaseRemoteConfig.activateFetched();
                        } else {
                            Log.e(TAG, "Fetch failed");
                        }
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
