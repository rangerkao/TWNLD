import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


public class ContentProvider_extract {
	
	private static final String DB_URL = "oracle.jdbc.driver.OracleDriver://10.42.1.101:1521/s2tbsdev";
	private static final String DB_USER_NAME = "FOYADEV";
	private static final String DB_PWD = "foyadev";
	private static final String DB_DRIVER_PATH = "com.mysql.jdbc.Driver";
		
	//private static final String TARGET_URL = "http://ei-dev1v1.hk.chinamobile.com/mvno_api/MVNO_UPDATE_QOS";
	//20140926 更改target IP
	//on 101 Test IP
	private static final String TARGET_URL ="http://203.142.105.18";
	//on 192  IP
	//private static final String TARGET_URL ="http://203.142.105.91";
	
	private static final String TARGET_URL2="/mvno_api/MVNO_UPDATE_QOS";
	
	
	private static final String LOGGER_PATH = "/export/home/foya/S2T/log/CP.log";
	private static final String CLASS = "CP.class.name";
	private static final String AMPERSAND = "&";
	private static String VERSION;
	private static String MSISDN;
	private static String IMSI;
	private static String ACTION;
	private static String DATE_TIME;
	private static String VENDOR;
	private static String PLAN;
	
	private static Record rc=null;
	
	private static Logger logger = null;
	
	static Properties s2tconf = new Properties();
	public ProcessS2T.PS2T s2t = new ProcessS2T.PS2T();
	
	private String eMeg=""; 
	
	public ContentProvider_extract(String Spath,Record r) {
    	VERSION = "1";
    	MSISDN = "";
    	IMSI = "";
    	ACTION = "";
    	DATE_TIME = "";
    	VENDOR = "S";
    	PLAN = "1";
 
    	rc=r;
    	initLog(Spath);
    	
    	logger.debug("ContentProvider_extract Initial");
    }
	
	private void initLog(String Spath) {
		
		 Properties props=new Properties();
	        try {
	            s2tconf.load(new   FileInputStream( Spath +"/log4j.properties"));
	            PropertyConfigurator.configure(s2tconf);
	            logger=Logger.getLogger("ContentProvider_extract");
	        }
	       catch (IOException e) {
	            e.printStackTrace();
	        }
	}
	
	public String doProvider() throws UnsupportedEncodingException{
		logger.debug("doProvider");
		
		String response = "";
		String data = "";

		if(rc!=null){
			data=loadPostData();
			response = sendPost(data);
			logger.info("response:"+response);
		}else{
			eMeg="Record is null";
			Send_AlertMail();
		}
		return response;
	}
	
	public void Send_AlertMail(){
		logger.info("Send_AlertMail");
	      try{
	      String Smailserver=s2tconf.getProperty("mailserver");
	      String SFrom=s2tconf.getProperty("From");
	      String Sto=s2tconf.getProperty("RDGroup");
	      String SSubject,SmessageText;
	      SSubject="ContentProvider Error  "+s2t.Date_Format()+s2t.Date_Format_Time();
	      
	      SmessageText = "ContentProvider Error:"+
	    		  "<br>Time:"+s2t.Date_Format()+s2t.Date_Format_Time()+
	    		  "<br>MNOIMSI:"+rc.getMNOIMSI()+
	    		  "<br>MNOMSISDN:"+rc.getMNOMSISDN()+
	    		  "<br>ADDON_ACTION:"+rc.getADDON_ACTION()+
	    		  "<br>Error:"+eMeg;
	      				
	      
	        s2t.SendAlertMail(Smailserver, SFrom, Sto, SSubject, SmessageText);
	         logger.info("Send Mail Content:"+SmessageText);}
	      catch(Exception ex){
	        logger.error("JAVA Error:"+ex.toString());}
	    }
	
	
	public String sendPost(String parameter) throws UnsupportedEncodingException {
		logger.debug("sendPost");
		final String EMPTY = "";
		final String METHOD = "POST";
		final String PROPERTY_1 = "POST /mvno_api/MVNO_UPDATE_QOS";
		final String PROPERTY_2 = "HTTP/1.1";
		final String ACCEPT = "Accept";
		final String ACCEPT_FORMAT = "text/html";
		final String ACCEPT_LANG = "Accept-Language";
		final String LANGUAGE = "en-us";
		final String CONTENT_TYPE = "Content-Type";
		final String TYPE = "application/x-www-form-urlencoded";
		final String HOST = "Host";
		final String HOST_NAME = "wmvno.hk.chinamobile.com";
		final String UTF_8 = "UTF-8";
		final String TAB = "\r";
		
		HttpURLConnection connection = null;
		int timeOut = 12000;
		String errorCode = "0";
		
		try {
			URL url = new URL(TARGET_URL+TARGET_URL2);
			
			logger.debug("openConnection:"+TARGET_URL);
			
		    connection = (HttpURLConnection)url.openConnection();
		    connection.setDoOutput(true);
		    connection.setRequestMethod(METHOD);
		      
		    connection.setReadTimeout(timeOut);
		    connection.setConnectTimeout(timeOut);
		      
		    connection.setRequestProperty(PROPERTY_1, EMPTY);
		    connection.setRequestProperty(PROPERTY_2, EMPTY);
		    connection.setRequestProperty(ACCEPT, ACCEPT_FORMAT);
		    connection.setRequestProperty(ACCEPT_LANG, LANGUAGE);
		    connection.setRequestProperty(CONTENT_TYPE, TYPE);
		    connection.setRequestProperty(HOST, HOST_NAME);
		    
		    //Send Request
		    logger.debug("Send Request");
		    //20141212
		    //String postData = URLEncoder.encode(parameter, UTF_8);
		    String postData = parameter;
		    
		    OutputStream out = connection.getOutputStream();
		    logger.debug("Write PostData:"+postData);
		    out.write(postData.getBytes());
		    out.close();
		      
		    //Get Response
		    logger.debug("Get Response");
		    InputStream in = connection.getInputStream();
		    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		      
		    String line;
		    StringBuffer response = new StringBuffer();
		      
		    while((line = reader.readLine()) != null) {  
		         response.append(line);
		         response.append(TAB);
		    }
		   
		    reader.close();
		   
		    return response.toString();
		  } catch(MalformedURLException e) {
			  //send email here
			  logger.error("at sendPost got MalformedURLException:"+e.getMessage());
			  eMeg="at sendPost got MalformedURLException:"+e.getMessage();
			  Send_AlertMail();
		  } catch(IOException e) {
			  //send email here
			  logger.error("at sendPost got IOException:"+e.getMessage());
			  eMeg="at sendPost got IOException:"+e.getMessage();
			  Send_AlertMail();
		  } finally {
			  if(connection != null) {
				  connection.disconnect();
		      }
		  }
		   
		  return errorCode;
	}
	
	public static String loadPostData()  {
		logger.debug("loadPostData");
		IMSI = rc.getMNOIMSI();
		MSISDN = rc.getMNOMSISDN();
		ACTION = rc.getADDON_ACTION();;
		
		String postData = "VERSION=" + VERSION + AMPERSAND + "MSISDN=" + MSISDN + AMPERSAND + 
		              "IMSI=" + IMSI + AMPERSAND + "DATE_TIME=" + dString() + AMPERSAND +
		              "VENDOR=" + VENDOR  + AMPERSAND + "ACTION=" + ACTION + AMPERSAND +
		              "PLAN=" + PLAN;
		
		logger.debug("postData:"+postData);
		
		return postData;
	}
	
	private static String dString(){
		SimpleDateFormat sdf = new SimpleDateFormat("dd---yyyy.HH:mm:ss");
		String dString=sdf.format(new Date());
		int dm=Calendar.getInstance().get(Calendar.MONTH)+1;
		switch(dm){
			case 1:
				dString=dString.replaceAll("---", "-JAN-");//Jan, Feb, Mar, Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec
				break;
			case 2:
				dString=dString.replaceAll("---", "-FEB-");
				break;
			case 3:
				dString=dString.replaceAll("---", "-MAR-");
				break;
			case 4:
				dString=dString.replaceAll("---", "-APR-");
				break;
			case 5:
				dString=dString.replaceAll("---", "-MAY-");
				break;
			case 6:
				dString=dString.replaceAll("---", "-JUN-");
				break;
			case 7:
				dString=dString.replaceAll("---", "-JUL-");
				break;
			case 8:
				dString=dString.replaceAll("---", "-AUG-");
				break;
			case 9:
				dString=dString.replaceAll("---", "-SEP-");
				break;
			case 10:
				dString=dString.replaceAll("---", "-OCT-");
				break;
			case 11:
				dString=dString.replaceAll("---", "-NOV-");
				break;
			case 12:
				dString=dString.replaceAll("---", "-DEC-");
				break;
			default:
		}
		
		return dString;
	}
}
