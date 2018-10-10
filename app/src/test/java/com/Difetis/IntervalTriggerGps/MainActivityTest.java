package com.Difetis.IntervalTriggerGps;

import android.os.Bundle;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by kerfall on 12/01/2018.
 */
public class MainActivityTest {

    //@Mock Bundle savedInstanceState;

    //l'objet à tester
    MainActivity main;

    @Before
    public void setUp() throws Exception {
        //Context context = mock(Context.class);
        //MockitoAnnotations.initMocks(this); //créé tous les @Mock
        main = new MainActivity();

        Bundle savedInstanceState = new Bundle();

        main.onCreate(savedInstanceState);
    }

    @Test
    public void onCreate() throws Exception {
    }

    @Test
    public void displayTime() throws Exception {

        main.DisplayTime(1000, false);
    }

    @Test
    public void saveData() throws Exception {
    }

    @Test
    public void writeCsvFile() throws Exception {
    }

    @Test
    public void writeGpxFile() throws Exception {
    }

    @Test
    public void onItemClick() throws Exception {
    }

    @Test
    public void startChrono() throws Exception {
    }

    @Test
    public void newLap() throws Exception {
    }

    @Test
    public void onChronoClick() throws Exception {
    }

    @Test
    public void isExternalStorageWritable() throws Exception {
    }

}