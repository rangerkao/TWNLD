import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CP extends HttpServlet {
   private static URL url = null;
   private static HttpURLConnection connection = null;
   private static final String METHOD = "POST";
   private static final String AMPERSAND = "&";
   private static final String USER_AGENT = "Mozilla/5.0";

   
protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
   String res = doScan("", "");
   response.getWriter().println("********************************");
   response.getWriter().println(res);
   response.getWriter().println("********************************");
}

   
protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {  
	//String s = request.getHeader("Host");
	
	
	 
	
//	Enumeration headerNames = request.getHeaderNames();
//	//Enumeration headerNames = request.getAttributeNames();
//	String s = "<br><br>";
//	
//	while (headerNames.hasMoreElements()) {
//		String key = (String) headerNames.nextElement();
//		String value = request.getHeader(key);
//		s += key + "     " + value;
//		s += "<br>";
//	}
//	
//	
//	
//	Enumeration attrNames = request.getParameterNames();
//	s = "<br><br><br><br>";
//	
//	while (attrNames.hasMoreElements()) {
//		String key = (String) attrNames.nextElement();
//		String value = request.getParameter(key);
//		s += key + "     " + value;
//		s += "<br>";
//	}
	
	response.setContentType("text/html; charset=windows-1252");
    PrintWriter out = response.getWriter();
    out.println("<html>");
    out.println("<head><title>demo</title></head>");
    out.println("<body>");
    //out.println(s);
    out.println("<p>The servlet has received a POST. This is the reply.</p>");
    out.println("</body></html>");
    out.close();  
}  

public static String doScan(String targetURL, String urlParameters) {
   String data = urlParameters;
   String VERSION = "1";
   String MSISDN = "886972223344";
   String IMSI = "466923303444774";
   String DATE_TIME = "10-JUN-2014.14:38:40";
   String VENDOR = "S";
   String PLAN = "1";
   String ACTION = "A";
   
   data = "IMSI=" + IMSI;
  
//   data = "VERSION=" + VERSION + AMPERSAND + "MSISDN=" + MSISDN + AMPERSAND + 
//		  "IMSI=" + IMSI + AMPERSAND + "DATA_TIME=" + DATE_TIME + AMPERSAND +
//		  "VENDOR=" + VENDOR  + AMPERSAND + "ACTION=" + ACTION + AMPERSAND +
//		  "PLAN=" + PLAN;
   
  // targetURL = "http://localhost:8080/TWNLD/CP";
   //targetURL = "http://ei-dev1v1.hk.chinamobile.com/mvno_api/MVNO_UPDATE_QOS";
   targetURL = "http://localhost:8080/TWNLD/MyTest";
   
   //String result = "<br>" + data + "<br>";
   String result = "";
   
   try {
      url = new URL(targetURL);
      connection = (HttpURLConnection)url.openConnection();
      
      connection.setRequestMethod(METHOD);
      connection.setRequestProperty("POST /mvno_api/MVNO_UPDATE_QOS", "");
      connection.setRequestProperty("HTTP/1.1", "");
      connection.setRequestProperty("Accept:", "text/html");
      connection.setRequestProperty("Accept-Language:", "en-us");
      connection.setRequestProperty("Content-Type:", "application/-www-form-urlencoded");
     // connection.setRequestProperty("User-Agent", USER_AGENT);
      //connection.setRequestProperty("Host:", "localhost:8080");
      connection.setRequestProperty("Host", "wmvno.hk.chinamobile.com");
     // connection.setRequestProperty("Authorization", "en-US");
     // connection.setRequestProperty("Content-Length", Integer.toString(data.getBytes().length));
   
      connection.setUseCaches(false);
      connection.setDoInput(true);
      connection.setDoOutput(true);
      
      //Send Request
      DataOutputStream out = new DataOutputStream(connection.getOutputStream());
      out.writeBytes(data);
      out.flush();
      out.close();
      
      //Get Response
      InputStream in = connection.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(in));
      String line;
      StringBuffer response = new StringBuffer();
      
      while((line = reader.readLine()) != null) {  
         response.append(line);
         response.append('\r');
      }
   
      reader.close();
   
      return response.toString();
  } catch(MalformedURLException e) {
	  result += "URL Failed";
  } catch(IOException e) {
   //openConnection() failed
	  result += "openConnection Failed";
  } finally {
     if(connection != null) {
        connection.disconnect();
      }
  }
   
   return result;
}
 
}
