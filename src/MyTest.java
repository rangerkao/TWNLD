import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class MyTest extends HttpServlet{
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		   response.getWriter().println("********************************");
		  // response.getWriter().println(res);
		   response.getWriter().println("********************************");
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {  
//    	Enumeration headerNames = request.getHeaderNames();
//    	String s = "<br><br>";
//    	
//    	while (headerNames.hasMoreElements()) {
//    		String key = (String) headerNames.nextElement();
//    		String value = request.getHeader(key);
//    		s += key + "     " + value;
//    		s += "<br>";
//    	}
    	
    //	 PrintWriter out = request.get
    	
    	String s = request.getParameter("IMSI");
    		
    	response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.println("<html>");
		out.println("<head><title>demo</title></head>");
		out.println("<body>");
		out.println(s);
		out.println("<p>My Test</p>");
		out.println("</body></html>");
		out.close();  
    }  
}
