package com.Difetis.IntervalTimer;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import java.io.File;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;


public class MainActivity extends Activity implements AdapterView.OnItemClickListener {

    // chrono datas
    public final static String APP_PATH_SD_CARD = "/csvIntervalTimer";
    Button stop;
    Long MillisecondTime = 0L, StartTime = 0L,LapTime = 0L;
    Handler chronoHandler;
    Handler timeHandler;
    float lastscreenBrightness = 0;
    ListView lapList;

    ArrayList<Long> ListTimeLaps;
    ArrayList<Double> ListDistanceLaps;
    ArrayList<String> ListElementsArrayList;
    ArrayList<String> ListGpsArrayList;
    ArrayList<String> ListTcxArrayList;
    ArrayList<String> ListTcxLapArrayList;
    ArrayAdapter<String> adapter;
    private TextView chrono;
    private TextView actualTime;
    private TextView instantSpeed;
    private TextView lapLength;
    private Vibrator vibrator;
    String _currentDate;
    String _startTime;

    // GPSTracker service class
    GPSTracker gps;

    // callback class to update chrono
    public Runnable runnable = new Runnable() {

        public void run() {

            // update data every second while initializing gps
            int postDelay = 1000;

            // if chrono not started, then we are maybe waiting for gps
            if (StartTime == 0L) {

                if (gps != null) {

                    ListElementsArrayList.clear();
                    String latitude = String.format("Latitude %f", gps.getLatitude());
                    String longitude = String.format("Longitude %f", gps.getLongitude());
                    String altitude = String.format("Altitude %f", gps.getAltitude());
                    //String speed = String.format("Speed %f", gps.getSpeed());
                    //String satellites = String.format("Speed %d", gps.getSatellites());

                    // add gps information to list
                    ListElementsArrayList.add(latitude);
                    ListElementsArrayList.add(longitude);
                    ListElementsArrayList.add(altitude);
                    //ListElementsArrayList.add(speed);
                    //ListElementsArrayList.add(satellites);

                    adapter.notifyDataSetChanged();

                    if (gps.isGPSReady()) {
                        chrono.setText("Ready");

                    } else {
                        // add an animation effect on waiting for gps
                        String message = chrono.getText().toString();

                        if (message.startsWith("Search") && message.length() < 9) {
                            message += ".";
                        } else {
                            message = "Search";
                        }
                        chrono.setText(message);

                    }
                } else {

                    chrono.setText("GPS Out");
                }
            } else {

                // if GPS service is launched and has changed, we record a trackpoint
                if (gps != null && gps.isGPSUpdated()) {
                    String currentTime = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());

                    String gpsTrckpt = String.format("<trkpt lat=\"%.7f\" lon=\"%.7f\"><ele>%f</ele><time>%sT%sZ</time></trkpt>\n",
                            gps.getLatitude(),
                            gps.getLongitude(),
                            gps.getAltitude(),
                            _currentDate,
                            currentTime);

                    String tcxTrckpt = String.format("<Trackpoint>\n" +
                                    "            <Time>%sT%sZ</Time>\n" +
                                    "            <Position>\n" +
                                    "              <LatitudeDegrees>%.7f</LatitudeDegrees>\n" +
                                    "              <LongitudeDegrees>%.7f</LongitudeDegrees>\n" +
                                    "            </Position>\n" +
                                    "            <AltitudeMeters>%.7f</AltitudeMeters>\n" +
                                    "            <DistanceMeters>%.2f</DistanceMeters>\n" +
                                    "            <HeartRateBpm>\n" +
                                    "              <Value></Value>\n" +
                                    "            </HeartRateBpm>\n" +
                                    "            <Extensions>\n" +
                                    "              <ns3:TPX>\n" +
                                    "                <ns3:Speed>%.2f</ns3:Speed>\n" +
                                    "                <ns3:RunCadence>80</ns3:RunCadence>\n" +
                                    "              </ns3:TPX>\n" +
                                    "            </Extensions>\n" +
                                    "          </Trackpoint>",
                            _currentDate,
                            currentTime,
                            gps.getLatitude(),
                            gps.getLongitude(),
                            gps.getAltitude(),
                            gps.getLength(),
                            gps.getSpeed());

                    if (ListGpsArrayList != null) {
                        ListGpsArrayList.add(gpsTrckpt);
                    }

                    if (ListTcxArrayList != null) {
                        ListTcxArrayList.add(tcxTrckpt);
                    }

                    instantSpeed.setText(String.format("%.1f\nkm/h",gps.getSpeed()));

                    if(gps.getLength() > 1000){
                        lapLength.setText(String.format("%.2f\nkm",gps.getLength() / 1000));
                    }else{
                        lapLength.setText(String.format("%.0f\nm",gps.getLength()));
                    }


                    // don't write the same point twice
                    gps.setGPSUpdated(false);
                }
                MillisecondTime = SystemClock.uptimeMillis() - LapTime;

                chrono.setText(DisplayTime(MillisecondTime, false));

            }


            chronoHandler.postDelayed(this, postDelay);
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Settings.System.putInt(this.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, 0);

        setContentView(R.layout.activity_main);
        chrono = (TextView) findViewById(R.id.Chrono);
        instantSpeed = (TextView) findViewById(R.id.InstantSpeed);
        lapLength = (TextView) findViewById(R.id.Length);
        stop = (Button) findViewById(R.id.buttonStop);
        lapList = (ListView) findViewById(R.id.lapList);

        lapList.setOnItemClickListener(this);
        actualTime = (TextView) findViewById(R.id.ActualTime);

        ListElementsArrayList = new ArrayList<String>();

        ListGpsArrayList = new ArrayList<String>();
        ListTcxArrayList = new ArrayList<String>();
        ListTcxLapArrayList = new ArrayList<String>();
        ListDistanceLaps = new ArrayList<Double>();

        ListTimeLaps = new ArrayList<Long>();
        timeHandler = new Handler(getMainLooper());
        timeHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                String displayTime = new SimpleDateFormat("HH:mm").format(new Date());

                // launch a timer that updates every seconds
                actualTime.setText(displayTime);

                // at midnight, udpate current date
                if (_currentDate == null || _currentDate.isEmpty() || displayTime == "00:00") {
                    _currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                }

                timeHandler.postDelayed(this, 1000);
            }
        }, 1000);

        vibrator = (Vibrator) this.getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);

        chronoHandler = new Handler();


        adapter = new ArrayAdapter<String>(MainActivity.this,
                R.layout.listlayout,
                ListElementsArrayList
        );

        lapList.setAdapter(adapter);

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // if the chrono stopped, we reset it
                if (StartTime == 0L) {

                    LapTime = 0L;
                    MillisecondTime = 0L;

                    chrono.setText("00:00:00");
                    instantSpeed.setText("0\nkm/h");
                    lapLength.setText("0\nm");

                    // save laps if any
                    AsyncWriteFile save=new AsyncWriteFile();
                    save.execute();

                } else { // otherwise we stop it

                    // save the last lap
                    //NewLap(false);
                    WriteNewLap newlap = WriteNewLap();
                    newlap.Execute(false);

                    // display total time
                    MillisecondTime = SystemClock.uptimeMillis() - StartTime;
                    DisplayTime(MillisecondTime, false);

                    StartTime = 0L;
                    stop.setText("Reset");

                    // restoring brightness to default
                    WindowManager.LayoutParams lp = getWindow().getAttributes();
                    lp.screenBrightness = lastscreenBrightness;
                    getWindow().setAttributes(lp);

                }

                // short vibration to assert the click was recorded
                vibrator.vibrate(250);


            }
        });


        // create gps service
        gps = new GPSTracker(MainActivity.this);

        // update diplay
        chronoHandler.postDelayed(runnable, 0);

        // keep screen on for gps search
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    public String  DisplayTime(long MillisecondTime, boolean formatCenti) {


        int Seconds, Minutes, Hours, MilliSeconds,  CentiSeconds;

        Seconds = (int) (MillisecondTime / 1000);
        Minutes = Seconds / 60;
        Hours = 0;

        if(Minutes >= 60)
        {
            Hours = Minutes / 60;
            Minutes = Minutes % 60;
        }

        Seconds = Seconds % 60;

        if(formatCenti)
        {
            MilliSeconds = (int) (MillisecondTime % 1000);
            CentiSeconds = MilliSeconds / 10;

            return String.format("%02d:%02d:%02d", Minutes,Seconds,CentiSeconds );
        }
        else{

            return String.format("%02d:%02d:%02d", Hours, Minutes,Seconds);
        }


    }

    public void SaveData() {

        if (!ListElementsArrayList.isEmpty()) {
            // generate the path to save csv files
            String csvPath = Environment.getExternalStorageDirectory().getAbsolutePath() + APP_PATH_SD_CARD;

            // create it if it's missing
            File csvDir = new File(csvPath);
            if (!csvDir.exists()) {
                csvDir.mkdirs();
            }

            // save the csv file
            if (csvDir != null) {
                WriteCsvFile(csvDir);
                if (ListGpsArrayList != null && !ListGpsArrayList.isEmpty()) {
                    WriteGpxFile(csvDir);
                }

                if (ListTcxLapArrayList != null && !ListTcxLapArrayList.isEmpty()) {
                    WriteTcxFile(csvDir);
                }
            }

            Toast.makeText(getApplicationContext(), "File saved", Toast.LENGTH_LONG).show();

            // then empty lap list
            if(ListDistanceLaps != null)
            {
                ListDistanceLaps.clear();
            }
            if(ListTimeLaps != null)
            {
                ListTimeLaps.clear();

            }

            if(ListElementsArrayList != null)
            {
                ListElementsArrayList.clear();
            }

            if(ListGpsArrayList != null)
            {
                ListGpsArrayList.clear();
            }

            if(ListTcxArrayList != null){
                ListTcxArrayList.clear();
            }

            if(ListTcxLapArrayList != null){
                ListTcxLapArrayList.clear();
            }

            if(adapter != null)
            {
                adapter.notifyDataSetChanged();
            }

        }

    }

    public void WriteCsvFile(File sd) {
        if (sd != null) {

            // creating a unique name
            String formattedDate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String csvFilePath = "run" + formattedDate + ".csv";

            // creating the file
            File csvFile = new File(sd.getAbsolutePath(), csvFilePath);

            try {

                if (csvFile != null) {

                    int i = 0;
                    String body = "lap;time;distance\n";
                    String distance = "";
                    String line = "";
                    // foreach lap we add a line
                    if(ListTimeLaps != null && !ListTimeLaps.isEmpty()) {
                        for (Long lap :
                                ListTimeLaps) {

                            if (ListDistanceLaps != null && ListDistanceLaps.size() > i) {
                                distance = String.format("%.2f", ListDistanceLaps.get(i));
                            } else {
                                distance = "0";
                            }
                            i++;
                            line = String.valueOf(i) + ";" + DisplayTime(lap, true) + ";" + distance + "\n";
                            body += line;
                        }
                    }


                    FileWriter writer = new FileWriter(csvFile);
                    if (writer != null) {
                        writer.append(body);
                        writer.flush();
                        writer.close();
                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    public void WriteGpxFile(File sd) {

        if (sd != null) {

            // creating a unique name
            String formattedDate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String gpxFilePath = "run" + formattedDate + ".gpx";

            // creating the file
            File gpxFile = new File(sd.getAbsolutePath(), gpxFilePath);

            try {

                if (gpxFile != null) {

                    String body = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n";
                    body += "<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" creator=\"IntervalTriggerGps\" version=\"1.1\" \n";
                    body += "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n";
                    body += "xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n";
                    body += "<metadata>\n";
                    body += "<name>" + gpxFilePath + "</name>\n";
                    body += "<link href=\"\">\n";
                    body += "<text>IntervalTriggerGps</text>\n";
                    body += "</link>\n";
                    body += String.format("<time>%s</time>\n", _startTime);
                    body += "</metadata>\n";
                    body += "<trk>\n";
                    body += "<trkseg>\n";
                    // foreach Trackpoint we add a line
                    if(ListGpsArrayList != null && !ListGpsArrayList.isEmpty())
                    {
                        for (String gpsTrkpt :
                                ListGpsArrayList) {
                            body += gpsTrkpt;
                        }
                    }



                    body += " </trkseg>\n";
                    body += "</trk>\n";
                    body += "</gpx>";

                    FileWriter writer = new FileWriter(gpxFile);
                    if (writer != null) {
                        writer.append(body);
                        writer.flush();
                        writer.close();
                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }


    public void WriteTcxFile(File sd) {

        if (sd != null) {

            // creating a unique name
            String formattedDate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String gpxFilePath = "run" + formattedDate + ".tcx";

            // creating the file
            File gpxFile = new File(sd.getAbsolutePath(), gpxFilePath);

            try {

                if (gpxFile != null) {

                    String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
                     "<TrainingCenterDatabase\n"+
                     "xsi:schemaLocation=\"http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2 http://www.garmin.com/xmlschemas/TrainingCenterDatabasev2.xsd\"\n"+
                     "xmlns:ns5=\"http://www.garmin.com/xmlschemas/ActivityGoals/v1\"\n"+
                     "xmlns:ns3=\"http://www.garmin.com/xmlschemas/ActivityExtension/v2\"\n"+
                     "xmlns:ns2=\"http://www.garmin.com/xmlschemas/UserProfile/v2\"\n"+
                     "xmlns=\"http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2\"\n"+
                     "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ns4=\"http://www.garmin.com/xmlschemas/ProfileExtension/v1\">\n"+
                     "<Activities>\n"+
                     "<Activity Sport=\"Running\">\n"+
                     String.format("<Id>%s</Id>\n", _startTime);

                    // foreach Trackpoint we add a line
                    if(ListTcxLapArrayList != null && !ListTcxLapArrayList.isEmpty())
                    {
                        for (String lapTrkpt :
                                ListTcxLapArrayList) {
                            body += lapTrkpt;
                        }
                    }



                    body += "  <Creator xsi:type=\"Device_t\">\n" +
                            "        <Name>Amazfit Pace</Name>\n" +
                            "        <UnitId></UnitId>\n" +
                            "        <ProductID></ProductID>\n" +
                            "        <Version>\n" +
                            "          <VersionMajor>1</VersionMajor>\n" +
                            "          <VersionMinor>3</VersionMinor>\n" +
                            "          <BuildMajor>5</BuildMajor>\n" +
                            "          <BuildMinor>0</BuildMinor>\n" +
                            "        </Version>\n" +
                            "      </Creator>\n" +
                            "    </Activity>\n" +
                            "  </Activities>\n" +
                            "  <Author xsi:type=\"Application_t\">\n" +
                            "    <Name>IntervalTriggerGps</Name>\n" +
                            "    <Build>\n" +
                            "      <Version>\n" +
                            "        <VersionMajor>1</VersionMajor>\n" +
                            "        <VersionMinor>0</VersionMinor>\n" +
                            "        <BuildMajor>0</BuildMajor>\n" +
                            "        <BuildMinor>0</BuildMinor>\n" +
                            "      </Version>\n" +
                            "    </Build>\n" +
                            "    <LangID>fr</LangID>\n" +
                            "    <PartNumber></PartNumber>\n" +
                            "  </Author>\n" +
                            "</TrainingCenterDatabase>\n";

                    FileWriter writer = new FileWriter(gpxFile);
                    if (writer != null) {
                        writer.append(body);
                        writer.flush();
                        writer.close();
                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }


    public void onItemClick(AdapterView<?> l, View v, int position, long id) {

        // if chrono is stopped and listview contains data we save it
        if (StartTime == 0L && !ListElementsArrayList.isEmpty()) {
            //SaveData();
        } else { // otherwise we start a new lap
            //NewLap(true);
        }

        // vibration to assert the click was recorded
        vibrator.vibrate(250);

    }

    public void StartChrono() {

        // put brightness to minimum, will be restored on stop
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lastscreenBrightness = lp.screenBrightness;
        lp.screenBrightness = 0;
        getWindow().setAttributes(lp);

        // keep screen on until we stop
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // clear any data present in list
        ListDistanceLaps.clear();
        ListTimeLaps.clear();
        ListElementsArrayList.clear();
        ListGpsArrayList.clear();
        adapter.notifyDataSetChanged();

        // record start time timestamp
        StartTime = SystemClock.uptimeMillis();
        // laptime and starttime are equal on first lap
        LapTime = StartTime;

        // reset length data
        if (gps != null) {
            gps.setLength(0);
            String currentTime = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());

            _startTime = String.format("%sT%sZ", _currentDate, currentTime);
        }

        // launching callback
        chronoHandler.postDelayed(runnable, 10);
    }

    public void NewLap(boolean restartChrono) {

        // stop callback time to reset the lap
        chronoHandler.removeCallbacks(runnable);

        String displayLapInfo = "";
        String displayLapSpeed = "";
        String displayLapDistance = "";
        String displayLapTime = DisplayTime(MillisecondTime, true);
        double runningDistance = 0;
        double lapSpeed = 0;

        // display laptime in toast
        Toast.makeText(getApplicationContext(), displayLapTime, Toast.LENGTH_LONG).show();


        // add time and distance to list
        if(ListTimeLaps != null)
        {
            ListTimeLaps.add(MillisecondTime);
        }


        if(gps != null){

            runningDistance = gps.getLength();
            // reset distance
            gps.setLength(0);

            if(ListDistanceLaps != null)
            {
                ListDistanceLaps.add(runningDistance);
            }

            if(runningDistance > 0 && MillisecondTime > 0) {
                lapSpeed = (double)(3600 * runningDistance) / (double)MillisecondTime;
                displayLapSpeed = String.format("%.2f km/h", lapSpeed);
            }

            if(runningDistance > 1000) {
                displayLapDistance = String.format("%.2f km", runningDistance / 1000);
            }else{
                displayLapDistance = String.format("%.1f m", runningDistance);
            }

            displayLapInfo = String.format("%d %s %s %s",ListTimeLaps.size(),displayLapTime,displayLapDistance, displayLapSpeed);
        }

        String currentTime = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());

        String TcxLap = String.format("<Lap StartTime=\"%sT%sZ\">\n" +
                "        <TotalTimeSeconds>%.1f</TotalTimeSeconds>\n" +
                "        <DistanceMeters>%.2f</DistanceMeters>\n" +
                "        <MaximumSpeed></MaximumSpeed>\n" +
                "        <Calories></Calories>\n" +
                "        <AverageHeartRateBpm>\n" +
                "          <Value></Value>\n" +
                "        </AverageHeartRateBpm>\n" +
                "        <MaximumHeartRateBpm>\n" +
                "          <Value></Value>\n" +
                "        </MaximumHeartRateBpm>\n" +
                "        <Intensity>Active</Intensity>\n" +
                "        <TriggerMethod>Manual</TriggerMethod>\n" +
                "        <Track>",
                _currentDate,
                currentTime,
                (double)(MillisecondTime / 1000),
                runningDistance);

        // foreach Trackpoint we add a line
        if(ListTcxArrayList != null && !ListTcxArrayList.isEmpty())
        {
            for (String lapTrkpt :
                    ListTcxArrayList) {
                TcxLap += lapTrkpt;
            }
        }

        TcxLap += String.format("</Track>\n"+
                "        <Extensions>\n" +
                "          <ns3:LX>\n" +
                "            <ns3:AvgSpeed>%.2f</ns3:AvgSpeed>\n" +
                "            <ns3:AvgRunCadence></ns3:AvgRunCadence>\n" +
                "            <ns3:MaxRunCadence></ns3:MaxRunCadence>\n" +
                "          </ns3:LX>\n" +
                "        </Extensions>\n" +
                "      </Lap>",lapSpeed);

        if(ListTcxArrayList != null && !ListTcxArrayList.isEmpty())
        {
            ListTcxArrayList.clear();

        }

        if(ListTcxLapArrayList != null) {
            ListTcxLapArrayList.add(TcxLap);
        }

        // last lap is added first
        ListElementsArrayList.add(0, displayLapInfo);
        adapter.notifyDataSetChanged();

        if(restartChrono == true) {
            // resetting laptime and restart callback
            LapTime = SystemClock.uptimeMillis();
            chronoHandler.postDelayed(runnable, 0);
        }
    }

    public void onChronoClick(View v) {


        // check if GPS enabled
        if (gps.canGetLocation()) {

            // if gps is ready we can start running
            if (gps.isGPSReady()) {
                // if chrono is off we start it
                if (StartTime == 0L) {
                    StartChrono();
                } else { // otherwise it's a lap start
                    //NewLap(true);
                    WriteNewLap newlap = new WriteNewLap();
                    newlap.Execute(true);
                }
            }


        } else {
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            gps.showSettingsAlert();
        }


        // vibration to assert the click was recorded
        vibrator.vibrate(250);

        stop.setText("Stop");
    }

    /* Checks if external storage is available for read and write */
    // move this class to a static utilities class
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    @Override
    protected void onDestroy() {

        gps.stopUsingGPS();
        gps.onDestroy();

        // we have to remove callbacks before exiting
        chronoHandler.removeCallbacks(runnable);
        timeHandler.removeCallbacks(null);
        super.onDestroy();


    }

    private class WriteFile extends AsyncTask<Void, Void, Void>
    {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(getApplicationContext(), "Start Saving File", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected Void doInBackground(Void... arg0) {

            SaveData();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Toast.makeText(getApplicationContext(), "File Saved", Toast.LENGTH_SHORT).show();
        }
    }

    private class WriteNewLap extends AsyncTask<Boolean, Void, Void>
    {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(getApplicationContext(), "Start Saving Lap", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected Void doInBackground(Boolean... arg) {

            NewLap(arg[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Toast.makeText(getApplicationContext(), "Lap Saved", Toast.LENGTH_SHORT).show();
        }
    }
}
