package edu.asu.mc.group30;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static android.content.Context.MODE_PRIVATE;
import static java.sql.Types.NULL;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String DBNAME = "group30.db";
    LineChart drawGraph;

    Random random = new Random();
    int counter=0;
    boolean stop=false;
    boolean runClicked = false;
    boolean stopClicked = false;
    SensorManager mSensorManager;
    Sensor mSensor;
    StringBuilder builder;
    Thread thread;
    int SENSOR_DELAY_MEGA = 100000; //100000 = 100 millisecond
    boolean startDataEntry = false;
    Context context;
    int duration = Toast.LENGTH_LONG;
    boolean dbCreated = false;
    boolean uploaded = false;
    Toast toast = null;
    int count = 0;
    String main_sql="";
    long start_time = -1, end_time;
    double elapsedSeconds;
    boolean first_entry = true;
    int entry_count = 0;
    Integer selectedAction;
    TextView text;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Legend legend = drawGraph.getLegend();
        //XAxis xi = drawGraph.getXAxis();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mSensor , SENSOR_DELAY_MEGA); // for every 1/10 second collect a sample.
        toast = Toast.makeText(getApplicationContext(), "Enter details", duration);
        toast.show();
        startDataEntry = false;

    }
    @SuppressLint("WrongCall")
    protected void createGraph(View view) {
        createDB();
    }

    protected int runChecks()
    {
        /*doesn't do any thing for now but we can run checks like whether age is a number,
        name has been filled out and doesn't include numeric characters, ID is a number -- also need to check if we need to maintain
        a DB for this; check if a RadioButton option has been selected
         */
        return 0;
    }

    public void Train(View view)
    {
        svm_train t = new svm_train();
        String trainFilePath = Environment.getExternalStorageDirectory()+"/"+"output.txt";
        String modelFilePath = Environment.getExternalStorageDirectory()+"/"+"model.txt";
        String[] argument = {trainFilePath, modelFilePath};
        try {
            t.run(argument);
        }
        catch(Exception e)
        {

        }


    }
    public void Test(View view)
    {
        svm_predict t = new svm_predict();
        String testFilePath = Environment.getExternalStorageDirectory()+"/"+"test.txt";
        String modelFilePath = Environment.getExternalStorageDirectory()+"/"+"model.txt";
        String outputFilePath = Environment.getExternalStorageDirectory()+"/"+"tested.txt";
        String[] argument = {testFilePath, modelFilePath, outputFilePath};
        try {
            t.runFirst(argument);
        }
        catch(Exception e)
        {

        }

    }
    protected void createDB()
    {

        //please make sure that runChecks is called before this method.
        // create database
        RadioButton radioEating = (RadioButton)findViewById(R.id.radioButtonEating);
        RadioButton radioWalking = (RadioButton)findViewById(R.id.radioButtonWalking);
        RadioButton radioRunning = (RadioButton)findViewById(R.id.radioButtonRunning);
        text = (TextView) findViewById(R.id.steps);
        text.setText(count);
        if(radioEating.isChecked())
        {
            selectedAction = 1;
        }
        else if(radioWalking.isChecked())
        {
            selectedAction = 2;
        }
        else if(radioRunning.isChecked())
        {
            selectedAction = 3 ;
        }
        else
        {
            selectedAction = NULL;
        }
        SQLiteDatabase  patient_database= SQLiteDatabase.openOrCreateDatabase(Environment.getExternalStorageDirectory()+"/"+DBNAME,null);
        // create table
        String sql = "CREATE TABLE IF NOT EXISTS table1 "+"("+"ID INTEGER PRIMARY KEY AUTOINCREMENT,";
        String colnames = "";
        for(int i=0; i<50; i++)
        {
            sql = sql +"Xvalues"+i+" REAL, Yvalues"+i+" REAL, Zvalues"+i+" REAL,";
            colnames = colnames +"Xvalues"+i+", Yvalues"+i+", Zvalues"+i+",";
        }
        sql = sql + " ActivityLabel VARCHAR);";
        colnames = colnames + " ActivityLabel";
        System.out.println(sql);
        patient_database.execSQL(sql);
        //toast message to say that database and table have been created.
        CharSequence text = "Database and table have been created";
        main_sql = "INSERT INTO table1 ("+colnames+") values(";
        if (toast != null) toast.cancel();

//        toast = Toast.makeText(getApplicationContext(), text, duration);
//        toast.show();
        patient_database.close();
        //call the accelerometer class to make insertions into the table in parallel.
        startDataEntry = true;
        dbCreated = true;
        start_time = System.currentTimeMillis();

    }

    public void stopTheGraph() {
        if (!stopClicked) {
            startDataEntry = false;
            stopClicked = true;
            runClicked = false;
            if (thread != null) {
                thread.interrupt();
            }
            thread = null;
            stop = true;
            counter = 0;
        }
    }

    protected void stopGraph(View view)
    {
        if(dbCreated == false) {
            if (toast != null) toast.cancel();
            toast = Toast.makeText(getApplicationContext(), "Create database by filling details and pressing enter first", duration);
            toast.show();
            return;
        }
        stopTheGraph();
    }

    public void classify(View view) throws IOException
    {
        SQLiteDatabase  patient_database= SQLiteDatabase.openOrCreateDatabase(Environment.getExternalStorageDirectory()+"/"+DBNAME,null);
        String eat_sql = "SELECT * FROM table1 where ActivityLabel = 1;";
        String walk_sql = "SELECT * FROM table1 where ActivityLabel = 2;";
        String run_sql = "SELECT * FROM table1 where ActivityLabel = 3;";
        Cursor res = patient_database.rawQuery(eat_sql, null);
        res.moveToFirst();
        FileWriter fileWriter =  new FileWriter(Environment.getExternalStorageDirectory()+"/"+"output.txt");
        BufferedWriter writer = new BufferedWriter(fileWriter);
        int mini_count = 1;
        while (res.isAfterLast() == false &&mini_count <21)  {
            writer.write(res.getString(151));
            //writer.write("2");
            for(int i=1; i< 151; i++)
            {
                writer.write(" "+ i+":"+res.getString(i));
            }
            writer.write("\n");
            System.out.println(res.getString(0));
            System.out.println("Ezhudhitten");
            mini_count = mini_count + 1;
            res.moveToNext();
        }
        Cursor res1 = patient_database.rawQuery(walk_sql,null);
        res1.moveToFirst();
        mini_count = 1;
        while(res1.isAfterLast() == false && mini_count < 21)
        {
            writer.write(res1.getString(151));
            for(int i=1; i< 151; i++)
            {
                writer.write(" "+ i+":"+res1.getString(i));
            }
            writer.write("\n");
            System.out.println(res1.getString(0));
            System.out.println("Ezhudhitten");
            mini_count = mini_count + 1;
            res1.moveToNext();
        }
        Cursor res2 = patient_database.rawQuery(run_sql,null);
        res2.moveToFirst();
        mini_count = 1;
        while(res2.isAfterLast() == false && mini_count < 21)
        {
            writer.write(res2.getString(151));
            for(int i=1; i< 151; i++)
            {
                writer.write(" "+ i+":"+res2.getString(i));
            }
            writer.write("\n");
            System.out.println(res2.getString(0));
            System.out.println("Ezhudhitten");
            mini_count = mini_count + 1;
            res2.moveToNext();
        }

        writer.close();
        fileWriter.close();
        FileWriter fileWriter1 =  new FileWriter(Environment.getExternalStorageDirectory()+"/"+"test.txt");
        BufferedWriter writer1 = new BufferedWriter(fileWriter1);
        while (res.isAfterLast() == false)  {
            writer1.write(res.getString(151));
            //writer.write("2");
            for(int i=1; i< 151; i++)
            {
                writer1.write(" "+ i+":"+res.getString(i));
            }
            writer1.write("\n");
            System.out.println(res.getString(0));
            System.out.println("Ezhudhitten");
            res.moveToNext();
        }
        while(res1.isAfterLast() == false)
        {
            writer1.write(res1.getString(151));
            for(int i=1; i< 151; i++)
            {
                writer1.write(" "+ i+":"+res1.getString(i));
            }
            writer1.write("\n");
            System.out.println(res1.getString(0));
            System.out.println("Ezhudhitten");
            res1.moveToNext();
        }
        while(res2.isAfterLast() == false)
        {
            writer1.write(res2.getString(151));
            for(int i=1; i< 151; i++)
            {
                writer1.write(" "+ i+":"+res2.getString(i));
            }
            writer1.write("\n");
            System.out.println(res2.getString(0));
            System.out.println("Ezhudhitten");
            mini_count = mini_count + 1;
            res2.moveToNext();
        }
        writer1.close();

        fileWriter1.close();
        System.out.println("Mudinjiruchu");

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            runAccelerometer accelo = new runAccelerometer();
            accelo.execute(event);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    private class runAccelerometer extends AsyncTask<SensorEvent, Void, Void>
    {

        @Override
        protected  Void doInBackground(SensorEvent... my_events) {
            if (start_time != -1) {
                //get x,y,z values
                SensorEvent new_event = my_events[0];
                float x = new_event.values[0];
                float y = new_event.values[1];
                float z = new_event.values[2];
                end_time = System.currentTimeMillis();
                long total_time = end_time - start_time;
                elapsedSeconds = total_time / 1000;
                {
                    //get the timestamp
                    //if (builder != null && startDataEntry) {
                    if (!first_entry) main_sql = main_sql + ", ";
                    main_sql = main_sql + x + "," + y + "," + z;
                    first_entry = false;
                    entry_count ++;
                    //}

                }



                //if (elapsedSeconds < 5.3 && elapsedSeconds > 4.5) {
                if (entry_count == 50) {

                    SQLiteDatabase patient_database = SQLiteDatabase.openOrCreateDatabase(Environment.getExternalStorageDirectory() + "/" + DBNAME, null);
                    main_sql = main_sql + ","+selectedAction +");";
                    System.out.println(main_sql);
                    patient_database.execSQL(main_sql);
                    patient_database.close();
                    System.out.println("creating..");
                    if (count <= 100) {
                        System.out.println("One row created successfully");
                        entry_count = 0;
                        first_entry = true;
                        createDB();

                        count = count + 1;
                    }

                }

            }
            return null;
        }


    }
}