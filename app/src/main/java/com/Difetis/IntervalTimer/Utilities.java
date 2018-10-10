package com.Difetis.IntervalTimer;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import android.content.res.Resources;


public class Utilities {
 //public static String APP_DIR = Main.SDCARD+"/Logfile";
 //public static String LOG_FILE_PATH = APP_DIR+"/ folome_log.txt";
 /*
//Creating App Directory For Log File  
public static void InitLog() {  
	
	deleteLog();
	createAppDirectory();
}
 

//Creating App Directory For Log File  
 public static void createAppDirectory() {  
	File dir1 = new File(APP_DIR);  
	if (!dir1.exists()) {  
	 dir1.mkdir();  
	}  
}  

// Deleting App Directory which is used for Log File  
public static void deleteLog() {  

	File log = new File(LOG_FILE_PATH);  
	if (log.exists()) {  
	 log.delete();  
	}  
}  
 
 public static void writeIntoLog(String data) 
 {
   
  FileWriter fw = null;    
  try {
   
   fw = new FileWriter(LOG_FILE_PATH , true);
   BufferedWriter buffer = new BufferedWriter(fw);   
   buffer.append(data+"\n");
    
   buffer.close();

  } catch (Exception e) { 
   e.printStackTrace();
  }
   
 } 

 public static String GetUnit(Resources res, int iUnit){
	  // on calcule le nombre de metre repr�sentant l'intervalle propos�
 String csUnit;
 
 String[] _Unit = res.getStringArray(R.array.LengthUnit);
 
	csUnit = _Unit[iUnit];
	
	return csUnit;
}      
 
 public static String GetSpeedUnit(Resources res, int iUnit){
 	  // on calcule le nombre de metre repr�sentant l'intervalle propos�
   String csUnit;
   
   String[] _Unit = res.getStringArray(R.array.SpeedUnit);
   
 	csUnit = _Unit[iUnit];
 	
 	return csUnit;
 }       
*/

 public static double MetreToDisplay(int iLengthUnit, double dValue){
 	  switch(iLengthUnit){
 	case 0: // m, pas de conversion
 		break;

 	case 1 : // km
 		dValue /= 1000; 
 		break;
 	case 2: // ml
 		dValue /= 3.2808399;
     	break;
 	case 3: // ft
 		dValue /= 1000; 
 		dValue /= 1.609344;
 		break;
 	default:
 		// inconnue, on renvoie la m�me chose
 	}
 	
 	return dValue;
 }       
 
 public static double DisplayToMetre(int iLengthUnit, double dValue){
	  switch(iLengthUnit){
	case 0: // m, pas de conversion
		break;
	case 1 : // km
		dValue *= 1000; 
		break;
	case 2: // ml
		dValue *= 3.2808399;
   	break;
	case 3: // ft
		dValue *= 1000; 
		dValue *= 1.609344;
		break;
	default:
		// inconnue, on renvoie la m�me chose
	}
	
	return dValue;
 }
}         


