package com.example.quickstart;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.NumberPicker;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by evapeng on 4/27/17.
 */

public class CreateTask extends Activity implements EasyPermissions.PermissionCallbacks{
    private DatePicker datePicker;
    private Calendar calendar;
    private TextView dateView;
    private int year, month, day, hour, minute;
    private TimePicker timePicker1;
    private TextView time;
    private String format = "";
    MainActivity m;
    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 0;
    static public GoogleAccountCredential mCredential;
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    static final int AUTH_CODE_REQUEST_CODE = 1004;

    private static final String BUTTON_TEXT = "Call Google Calendar API";
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {CalendarScopes.CALENDAR_READONLY,CalendarScopes.CALENDAR};
    TextView taskDays;
    Button createButton;


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_task);

        dateView = (TextView) findViewById(R.id.taskDueDate);
        time = (TextView) findViewById(R.id.taskDueTime);

        calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);

        month = calendar.get(Calendar.MONTH);
        hour = calendar.get(Calendar.HOUR);
        minute = calendar.get(Calendar.MINUTE);
        day = calendar.get(Calendar.DAY_OF_MONTH);
        showDate(year, month+1, day);
        showTime(hour,minute);

        m = new MainActivity();

        NumberPicker np = (NumberPicker) findViewById(R.id.taskNumber);
        np.setMinValue(1);
        np.setMaxValue(10);
        np.setWrapSelectorWheel(false);

        NumberPicker np2 = (NumberPicker) findViewById(R.id.daysBeforeDue);
        np2.setMinValue(1);
        np2.setMaxValue(10);
        np2.setWrapSelectorWheel(false);

        NumberPicker np3 = (NumberPicker) findViewById(R.id.totalHours);
        np3.setMinValue(1);
        np3.setMaxValue(50);
        np3.setWrapSelectorWheel(false);

        taskDays = (TextView) findViewById(R.id.taskDays);
        createButton = (Button) findViewById(R.id.createButton);

        createButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                TextView taskName = (TextView) findViewById(R.id.taskName);
                int sessions = ((NumberPicker) findViewById(R.id.taskNumber)).getValue();
                int daysBefore = ((NumberPicker) findViewById(R.id.daysBeforeDue)).getValue();
                int hours = ((NumberPicker) findViewById(R.id.totalHours)).getValue();
                int weekends = ((RadioGroup) findViewById(R.id.workWeekends)).getCheckedRadioButtonId();
                RadioGroup radioButtonG = (RadioGroup) findViewById(R.id.workWeekends);

                View button = radioButtonG.findViewById(radioButtonG.getCheckedRadioButtonId());
                int idx = radioButtonG.indexOfChild(button); //0 = yes, 1 = no

                Calendar dueC = Calendar.getInstance();

                //SUBTRACT DAYS
                dueC.set(year,month,day);
                dueC.add(Calendar.DAY_OF_YEAR, -daysBefore);
                Date cur = calendar.getTime();
                Date due2 = dueC.getTime();
                long diff = due2.getTime() - cur.getTime();
                long totalDays = diff/(60*24*60*1000);

                if (idx == 1){
                    totalDays = totalDays - (totalDays * 2/7);
                }

                int sessionTimeHours = (int) Math.ceil(hours/sessions);

                for (int sess = 0; sess < sessions; sess++){
                    dueC = Calendar.getInstance();
                    //SUBTRACT DAYS
                    dueC.set(year,month - 1,day);

                    Log.e("DAY",dueC.getTime().toString());

                    Event event = new Event()
                            .setSummary(String.valueOf(taskName.getText()) + " Session " + String.valueOf(sessions - sess) + "/" + String.valueOf(sessions));

                    //now that we have days & sessions space it out
                    //spaced out max is 3
                    int between = 0;
                    if (totalDays/sessions >= 3) {
                        between = 3;
                    }
                    else if (totalDays/sessions == 2 || totalDays/sessions == 1) {
                        between = (int) totalDays/sessions;
                    }

                    if (between >= 1){

                        dueC.add(Calendar.DAY_OF_MONTH,-1-(sess*between));
                        if (idx == 1){
                            while ((dueC.get(Calendar.DAY_OF_WEEK)) == Calendar.SATURDAY || (dueC.get(Calendar.DAY_OF_WEEK)) == Calendar.SUNDAY){
                                dueC.add(Calendar.DAY_OF_MONTH,-1);
                            }
                        }
                        DateTime startDateTime = new DateTime(dueC.getTime());
                        EventDateTime start = new EventDateTime()
                                .setDateTime(startDateTime);
                        event.setStart(start);

                        Log.e("DAY",start.toString());
                        Log.e("HOURS",String.valueOf(sessionTimeHours));

                        Calendar dueC2 = (Calendar) dueC.clone();
                        dueC2.add(Calendar.HOUR_OF_DAY,sessionTimeHours);
                        DateTime endDateTime = new DateTime(dueC2.getTime());
                        EventDateTime end = new EventDateTime()
                                .setDateTime(endDateTime);

                        Log.e("DAY",end.toString());

                        String calendarId = "primary";
                        event.setEnd(end);

                        new MakeRequestTask2(mCredential,calendarId,event).execute();

                        Context context = getApplicationContext();
                        CharSequence text = sessions>1? String.valueOf(sessions) + " Events created" : "Event created";
                        int duration = Toast.LENGTH_SHORT;

                        Toast toast = Toast.makeText(context, text, duration);
                        toast.show();


                        //new MainActivity().new MakeRequestTask2(MainActivity.mCredential,calendarId,event,getApplicationContext()).execute();
                    }

                }


            }
        });
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        0);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    private void showDate(int year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;
        dateView.setText(new StringBuilder().append(month).append("/")
                .append(day).append("/").append(year));
    }

    @SuppressWarnings("deprecation")
    public void setDate(View view) {
        showDialog(999);
    }

    public void setTime(View view) {
        showDialog(998);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        // TODO Auto-generated method stub
        if (id == 999) {
            return new DatePickerDialog(this,
                    myDateListener, year, month, day);
        }
        if (id == 998) {
            return new TimePickerDialog(this,
                    myTimeListener, hour, minute, true);
        }
        return null;
    }

    private DatePickerDialog.OnDateSetListener myDateListener = new
            DatePickerDialog.OnDateSetListener() {
                @RequiresApi(api = Build.VERSION_CODES.N)
                @Override
                public void onDateSet(DatePicker arg0,
                                      int arg1, int arg2, int arg3) {
                    // TODO Auto-generated method stub
                    // arg1 = year
                    // arg2 = month
                    // arg3 = day
                    setNumberofdays(calendar.getTime(),arg1,arg2,arg3);
                    Log.v("DATE",String.valueOf(arg2));
                    showDate(arg1, arg2+1, arg3);

                }
            };
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void setNumberofdays(Date cur, int y, int m, int d){
        Calendar due = Calendar.getInstance();
        due.set(y,m,d);
        Date due2 = due.getTime();
        long diff = due2.getTime() - cur.getTime();
        taskDays.setText(String.valueOf(diff/(60*24*60*1000)) + " Days");
    }

    private TimePickerDialog.OnTimeSetListener myTimeListener = new
            TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker timePicker,
                                      int selectedHour, int selectedMinute) {
                    // TODO Auto-generated method stub
                    showTime(selectedHour, selectedMinute);
                }
            };

    public void showTime(int hour, int min) {
        if (hour == 0) {
            hour += 12;
            format = "AM";
        } else if (hour == 12) {
            format = "PM";
        } else if (hour > 12) {
            hour -= 12;
            format = "PM";
        } else {
            format = "AM";
        }

        if (min >= 10){
            time.setText(new StringBuilder().append(hour).append(":").append(min)
                    .append(" ").append(format));
        }
        else{
            time.setText(new StringBuilder().append(hour).append(":0").append(min)
                    .append(" ").append(format));
        }


    }

    private void getResultsFromApi() {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (! isDeviceOnline()) {
            Log.e("TEXT","No network connection available.");
        } else {
            //don't make request; disabled
            Intent myIntent = new Intent(this, CreateTask.class);
            startActivityForResult(myIntent, 0);
//            new MakeRequestTask(mCredential).execute();
        }
    }
    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    Log.e("TEXT",
                            "This app requires Google Play Services. Please install " +
                                    "Google Play Services on your device and relaunch this app.");
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                CreateTask.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    public class MakeRequestTask2 extends AsyncTask<Void, Void, Void> {
        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;
        String calendarId;
        Event event;
        Context c;


        MakeRequestTask2(GoogleAccountCredential credential, String calendarId, Event event) {
            this.calendarId = calendarId;
            this.event = event;


            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Calendar API Android Quickstart")
                    .build();
        }

        /**
         * Background task to call Google Calendar API.
         */
        @Override
        protected Void doInBackground(Void... params) {
            try {
                createEvents(calendarId,event);
                Log.e("CALENDAR","SUCCEED");


            } catch (UserRecoverableAuthIOException e) {
                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
            } catch (Exception e) {
                Log.e("CALENDAR","FAILED");
                Log.e("CALENDAR",e.toString());

                mLastError = e;
                Log.e("CALENDAR",Log.getStackTraceString(e));
                cancel(true);
            }

            return null;
        }

        /**
         * Fetch a list of the next 10 events from the primary calendar.
         * @return List of Strings describing returned events.
         * @throws IOException
         */
        public void createEvents(String calendarId, Event event) throws IOException {
            Log.e("WHAT", "HERE");
            mCredential.setSelectedAccountName("renatusair@gmail.com");
            mService.events().insert(calendarId, event).execute();
        }

    }


}
