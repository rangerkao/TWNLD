/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * 
 * @author Administrator
 */
public class PostO extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String ReqDate = "", TicketNumber = "", TWNLDIMSI = "", TWNLDMSISDN = "",
			S2TIMSI = "", S2T_MSISDN="";
	String ReqStatus = "", VLNCountry = "", GPRSStatus = "", IPP = "";

	/**
	 * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
	 * methods.
	 * 
	 * @param request
	 *            servlet request
	 * @param response
	 *            servlet response
	 * @throws ServletException
	 *             if a servlet-specific error occurs
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected void processRequest(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html;charset=UTF-8");
		PrintWriter pw = response.getWriter();
		// pw.println("<?xml version='1.0' encoding='UTF-8'?>");
		try {
			ReqDate = request.getParameter("Req_DateTime");
			TicketNumber = request.getParameter("Ticket_Number");
			TWNLDIMSI = request.getParameter("SGPSH_IMSI");
			TWNLDMSISDN = request.getParameter("SGPSH_MSISDN");
			S2TIMSI = request.getParameter("S2T_IMSI");
			S2T_MSISDN = request.getParameter("S2T_MSISDN");
			ReqStatus = request.getParameter("Req_Status");
			VLNCountry = request.getParameter("VLN_Country");
			GPRSStatus = request.getParameter("GPRS_Status");
			IPP = request.getParameter("IPP");
			URL url = new URL("http://" + IPP + "/SGPSH/SGPSHprovision");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestMethod("POST");
			conn.setUseCaches(false);
			conn.setAllowUserInteraction(true);
			HttpURLConnection.setFollowRedirects(true);
			conn.setInstanceFollowRedirects(true);
			String msg = "<?xml version='1.0' encoding='UTF-8'?>"
					+ "<ActivationRsp>";
			msg = msg + "<Req_DateTime>" + ReqDate + "</Req_DateTime>";
			msg = msg + "<Ticket_Number>" + TicketNumber + "</Ticket_Number>";
			msg = msg + "<SGPSH_IMSI>" + TWNLDIMSI + "</SGPSH_IMSI>";
			msg = msg + "<SGPSH_MSISDN>" + TWNLDMSISDN + "</SGPSH_MSISDN>";
			msg = msg + "<S2T_IMSI>" + S2TIMSI + "</S2T_IMSI>";
			msg = msg + "<S2T_MSISDN>" + S2T_MSISDN + "</S2T_MSISDN>";
			msg = msg + "<Req_Status>" + ReqStatus + "</Req_Status>";
			msg = msg + "<VLN_Country>" + VLNCountry + "</VLN_Country>";
			msg = msg + "<GPRS_Status>" + GPRSStatus + "</GPRS_Status>";
			msg = msg + "</ActivationRsp>";
			
			// pw.println(msg);
					
			java.io.DataOutputStream dos = new java.io.DataOutputStream(
					conn.getOutputStream());
			dos.writeBytes(msg);

			java.io.BufferedReader rd = new java.io.BufferedReader(
					new java.io.InputStreamReader(conn.getInputStream(),
							"UTF-8"));
			String line;
			while ((line = rd.readLine()) != null) {
				pw.println(line);
			}

			rd.close();
			pw.flush();
			/*
			 * TODO output your page here out.println("<html>");
			 * out.println("<head>");
			 * out.println("<title>Servlet Post</title>");
			 * out.println("</head>"); out.println("<body>");
			 * out.println("<h1>Servlet Post at " + request.getContextPath () +
			 * "</h1>"); out.println("</body>"); out.println("</html>");
			 */
		} finally {
			pw.close();
		}
	}

	// <editor-fold defaultstate="collapsed"
	// desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
	/**
	 * Handles the HTTP <code>GET</code> method.
	 * 
	 * @param request
	 *            servlet request
	 * @param response
	 *            servlet response
	 * @throws ServletException
	 *             if a servlet-specific error occurs
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		processRequest(request, response);
	}

	/**
	 * Handles the HTTP <code>POST</code> method.
	 * 
	 * @param request
	 *            servlet request
	 * @param response
	 *            servlet response
	 * @throws ServletException
	 *             if a servlet-specific error occurs
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		processRequest(request, response);
	}

	/**
	 * Returns a short description of the servlet.
	 * 
	 * @return a String containing servlet description
	 */
	@Override
	public String getServletInfo() {
		return "Short description";
	}// </editor-fold>

}
