import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.log4j.Logger;

public class SMSThread {

	public static List<Map<String,String>> delaySMS = new ArrayList<Map<String,String>>();
	public static Properties s2tconf = new Properties();
	private static Logger logger;//,sconf
	static{
		
		new SMSThread();
	}
	SMSThread(){
		SMSThread.logger = TWNLDprovision.logger;
		SMSThread.s2tconf = TWNLDprovision.s2tconf;
		Thread t = new Thread(new ThreadExample2());
		t.start();
		System.out.println("Initial SMSThread");
	}
	class ThreadExample2 implements Runnable {
	    public void run() { // implements Runnable run()
	    	SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
	    	Date now = null;
			while(true){
				try {
					Thread.sleep(60*1000);
					now=new Date();
					//直接使用list.remove()會產生error，必須使用iterator，當iterator執行時會同步兩個參數的數值
					Iterator<Map<String, String>> it = delaySMS.iterator();
					while(it.hasNext()){
						Map<String,String> m = it.next();
						String msg=m.get("VARREALMSG");
						msg = new String(msg.getBytes("BIG5"), "ISO-8859-1");
						String phone=m.get("TWNLDMSISDN");
						String sendtime=m.get("sendtime");
						String logid = m.get("SMSLOGID");
						
						if(now.after(sdf.parse(sendtime))){
							send_SMS(msg,phone,logid);
							it.remove();
						}
					}
				}catch (InterruptedException e){
					ErrorHandle("InterruptedException",e);
				} catch (ParseException e) {
					ErrorHandle("ParseException",e);
				} catch (UnsupportedEncodingException e) {
					ErrorHandle("UnsupportedEncodingException",e);
				}
			}
	    }
	}
	public void send_SMS(String msg,String phone,String logid){
		if("true".equals(s2tconf.getProperty("TestMod"))){
			phone=s2tconf.getProperty("TestPhoneNumber");
		}
		String res;
		try {
			res = setSMSPostParam(msg, phone);
			logger.debug("delay send "+logid+" sms result : " + res);
			if(res.indexOf("Message Submitted")==-1){
				throw new Exception("Sendding SMS Error!<br>"
						+ "cTWNLDMSISDN="+phone+"<br>"
						+ "VARREALMSG="+msg);
			}
		} catch (IOException e) {
			ErrorHandle("IOException",e);
		} catch (Exception e) {
			ErrorHandle(e);
		}
	}
	public void ErrorHandle(Exception e){
		StringWriter s = new StringWriter();
		e.printStackTrace(new PrintWriter(s));
		logger.error(e);
		Send_AlertMail(s.toString());
	}
	public void ErrorHandle(String cont,Exception e){
		StringWriter s = new StringWriter();
		e.printStackTrace(new PrintWriter(s));
		logger.error(cont,e);
		Send_AlertMail(cont+"<br>"+s);
	}
	public void Send_AlertMail(String content){    	
		String Sto=s2tconf.getProperty("RDGroup");
		String SSubject,SmessageText;
		SSubject="TWNLD_ERROR";
		SmessageText = content;   
		try{
			String [] cmd=new String[3];
			cmd[0]="/bin/bash";
			cmd[1]="-c";
			cmd[2]= "/bin/echo \""+SmessageText+"\" | /bin/mailx -s \""+SSubject+"\" -r TWNLD_ALERT "+Sto;

			Process p = Runtime.getRuntime().exec (cmd);
			p.waitFor();
			logger.info("send mail cmd:"+cmd);
		}catch (Exception e){
			logger.error("send mail fail:"+SmessageText+"。",e);
		}
	}
	 private String setSMSPostParam(String msg,String phone) throws IOException{
			//StringBuffer sb=new StringBuffer ();

			String PhoneNumber=phone,Text=msg,charset="big5",InfoCharCounter=null,PID=null,DCS=null;
			String param =
					"PhoneNumber=+{{PhoneNumber}}&"
					+ "Text={{Text}}&"
					+ "charset={{charset}}&"
					+ "InfoCharCounter={{InfoCharCounter}}&"
					+ "PID={{PID}}&"
					+ "DCS={{DCS}}&"
					+ "Submit=Submit";
			
			if(PhoneNumber==null)PhoneNumber="";
			if(Text==null)Text="";
			//if(charset==null)charset="";
			if(InfoCharCounter==null)InfoCharCounter="";
			if(PID==null)PID="";
			if(DCS==null)DCS="";
			param=param.replace("{{PhoneNumber}}",PhoneNumber );
			param=param.replace("{{Text}}",Text.replaceAll("/+", "%2b") );
			param=param.replace("{{charset}}",charset );
			param=param.replace("{{InfoCharCounter}}",InfoCharCounter );
			param=param.replace("{{PID}}",PID );
			param=param.replace("{{DCS}}",DCS );

			return HttpPost("http://192.168.10.125:8800/Send%20Text%20Message.htm", param,"");
		}
	    public String HttpPost(String url,String param,String charset) throws IOException{
			URL obj = new URL(url);
			
			if(charset!=null && !"".equals(charset))
				param=URLEncoder.encode(param, charset);
			
			
			HttpURLConnection con =  (HttpURLConnection) obj.openConnection();
	 
			//add reuqest header
			/*con.setRequestMethod("POST");
			con.setRequestProperty("User-Agent", USER_AGENT);
			con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");*/
	 
			// Send post request
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(param);
			wr.flush();
			wr.close();
	 
			int responseCode = con.getResponseCode();
			System.out.println("\nSending 'POST' request to URL("+new Date()+") : " + url);
			System.out.println("Post parameters : " + new String(param.getBytes("ISO8859-1")));
			System.out.println("Response Code : " + responseCode);

			BufferedReader in = new BufferedReader(
			        new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
	 
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
	 
			//print result
			return(response.toString());
		}
	
}

