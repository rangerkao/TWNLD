﻿/*
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 * 2010/12/14 新增功能 SMS延遲,修正發信的JAVA錯誤
 * 2011/03/24 新增功能 狀態05,17 的Type_Code87
 * 2011/03/25 狀態03,05 模組Check_TWN_Msisdn_Status 移除
 * 2011/03/29 更改MNOSubCode=960,Check_Pair_IMSI_status錯誤代碼改112
 * 2011/03/30 狀態97,98 不發錯誤信
 * 2011/04/07 新增回傳XML的LOG
 * 2011/04/11 修正SMS 出現亂碼
 * 2011/05/05 新憎rework 00,01,02,03,05,07,17,99及07時CHA不換VLN
 * 2011/05/12 修正rework
 * 2011/05/30 修正iError
 * 2011/06/01 修正 Check_S2T_Msisdn_UNused
 * 2011/08/12 修正 nvl(serviceid,'0') as ab from imsi 錯誤
 * 2011/11/09 修正SMS 因為雙引號出現亂碼
 * 2012/09/04 修正SMS 訊息內容修改
 * 2012/09/18 新增checking Follow Me to Home and SMS Follow Me to Home.(05,17)
 * 2012/09/18 新增泰國(THA)VLN
 * 2012/09/21 新增FORWARD_TO_HOME_NO,S_FORWARD_TO_HOME_NO 狀態05,17 type=202判斷
 * 2012/11/13 修正FORWARD_TO_HOME_NO,S_FORWARD_TO_HOME_NO 狀態05 type=202判斷 帶新中華門號
 * 2013/02/27 新增印尼(IDN)VLN
 * 2013/09/28 Remove SWE VLN and changed to CamGSM - by Duke
 * 2014/09/23 將Request17 Rerun17 驗證與操作table分開，運行18後只呼叫操作table部分
 * 2014/09/24 取消18後呼叫Rerun17
 * 2014/09/24 加入CMHK ContentProvider部分程式
 * 2014/10/07 新增錯誤訊息601 DBconnection Error
 */

/**
 * Program History
 * 20140918 程式接手
 * 20140919 調整Log內容，帶出Error Msg、SQL
 * 20140919 修改，當ActionCode為sx000時，不取資料
 * 20141007 新增狀態601，DB Connection Error
 * 20141211 調整當req18時，沒有Action Code與action時，不做任何事傳回000
 * 20141215 調整回傳訊息，移除重複的VLN，增加AddonCode、AddonAction、ReqDateTime
 * 20141216 調整簡訊內容
 * 20150206 將AddonCode、AddonAction改為List，Request18以loop執行
 */

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
//import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


/**
 *
 * @author Administrator
 */
public class TWNLDprovision extends HttpServlet {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public Statement Tempsm=null;
    public static Logger logger;//,sconf
    public ProcessS2T.PS2T s2t = new ProcessS2T.PS2T();
    static ResultSet Temprs=null;
    static ResultSet TempRtA=null;
    static String cFileName="",c910SEQ="",cFileID="",cVLNCountryOLD = "",cVLNSGPOLD = "",cVLNTHAOLD = "";
    static String cWorkOrderNBR="",cServiceOrderNBR="",sMNOName="",sXML="",cRCode="",sreturnXml="";
    static String sSubCode="", sStepNo="", sDATE="", sCount="",sMap = "",sCMHKLOGID="";
    static String sTypeCode="", sDataType="", sValue = "",sSql="",Sdate="",sSTATUS="",cMSISDNOLD = "";
    static String sMNOSubCode="950",cM205OT="",cMVLNOLD="",cMVLN="",sWSFStatus="",sWSFDStatus="";
    static String dReqDate="",cS2TIMSI="",cReqStatus="",cVLNCountry="",cVLNSGP="",cVLNTHA="",sAllVln="";
    static String cTicketNumber="",cTWNLDIMSI="",cTWNLDMSISDN="",Sparam="",Se="",smsi="",sError="",sM_CTYPE="";
    static String sVln="",cS2TMSISDN="",cCountryName="",cVLNc="",cGPRSStatus="",csta="",desc="",cGPRS="",cOldTWNLDMSISDN="",cOLDS2TMSISDN="";
    static String sOld_SERVICE_ORDER_NBR="",sOld_WORK_ORDER_NBR="",sOld_step_no="",TempSparam="";
    static String sFMTH="",sFMTHa="",sSFMTH="",sSFMTHa="",sFORWARD_TO_HOME_NO="",sS_FORWARD_TO_HOME_NO="";
    static Vector<String> vln=new Vector<String>();
    static Vector<String> Tmpvln=new Vector<String>();
    
    static List<Map<String,String>> cAddonItem = new ArrayList<Map<String,String>>();
    
    static String cAddonCode="", cAddonAction="";   //****************************************
    //static FileWriter fw = null;
    //static BufferedWriter bw = null;
    ResultSet TempRt=null;
    static Runtime runtime = Runtime.getRuntime();
    static String Run_Shell="",Process_Code=null,Old_result_flag=null,Ssubsidiaryid="59";
    public static Properties s2tconf = new Properties();
    int i,n=0,y,l,f,iVLN,iError=0,iCut=0,iCountA=0;
    boolean  ba,bb,bc,bd,sessionDebug = true;
    public java.util.Properties props = System.getProperties();
    
    //20150520 add
    List<Map<String,String>> delaySMS = new ArrayList<Map<String,String>>();
    
    //20140919 Add RecordErrorSQL
    String sErrorSQL="",iErrorMsg="";
    
    
    int SMS_Delay_Time = 5*60*1000;
    
    static //20141121 add for customer service number
    Connection conn2 = null ; 
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */

    private static void Load_Properties(PrintWriter outD,String Spath){
           // Properties props=new Properties();
        try {
            s2tconf.load(new   FileInputStream( Spath +"/log4j.properties"));
            PropertyConfigurator.configure(s2tconf);
            logger=Logger.getLogger("PRISMprovision");  
            connectDB2();
        }
       catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
		e.printStackTrace();
	}
    }
    
    protected static void connectDB2() {
    	//logger.info("connectDB2...");
		String url=s2tconf.getProperty("mBOSS.URL")
				.replace("{{Host}}", s2tconf.getProperty("mBOSS.Host"))
				.replace("{{Port}}", s2tconf.getProperty("mBOSS.Port"))
				.replace("{{ServiceName}}", (s2tconf.getProperty("mBOSS.ServiceName")!=null?s2tconf.getProperty("mBOSS.ServiceName"):""))
				.replace("{{SID}}", (s2tconf.getProperty("mBOSS.SID")!=null?s2tconf.getProperty("mBOSS.SID"):""));
		try {
			conn2=connDB(logger, s2tconf.getProperty("mBOSS.DriverClass"), url, 
					s2tconf.getProperty("mBOSS.UserName"), 
					s2tconf.getProperty("mBOSS.PassWord")
					);
			if(conn2==null){
				logger.error("Connection for MBOSS is null !");
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			logger.error("connectDB2 Exception : "+e.getMessage());
		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("connectDB2 Exception : "+e.getMessage());
		}
			

	}
    public static Connection connDB(Logger logger, String DriverClass, String URL,
			String UserName, String PassWord) throws ClassNotFoundException, SQLException {
		//logger.debug("Start to connect DB ");

		Connection conn = null;

			Class.forName(DriverClass);
			conn = DriverManager.getConnection(URL, UserName, PassWord);
			//logger.debug("Finished to connect DB ");

		return conn;
	}
    
    private boolean checkAddonCode(String code) {
    	String regex = "^sx\\d{3}$";
    	
    	Pattern p = Pattern.compile(regex);
    	Matcher m = p.matcher(code.toLowerCase());
    	
    	return m.find();
    }
    
    private boolean Query_AddonStatus(PrintWriter out18) {
    	logger.info("Check Addon Service");
    	
    	String addonCode = "";
    	String addonAction = "";

    	try {
			for(Map<String,String> m : cAddonItem){
				addonCode = m.get("AddonCode");
				addonAction = m.get("AddonAction");
				
				if(!checkAddonCode(addonCode)) {
					logger.debug("Addon Service Code is incorrect");
					Query_PreProcessResult(out18,"423");
					return false;
				}
				
				if("A".equals(addonAction.toUpperCase())) {
	    			
	    			sSql="SELECT COUNT(*) AS ab from ADDONSERVICE WHERE ADDONCODE='"+ 
	    			     addonCode + "' AND MNOIMSI= '" + cTWNLDIMSI + "'";
	    			TempRt = s2t.Query(sSql);
	             
	                while (Temprs.next()) {
	                	if("0".equals(Temprs.getString("ab"))) {
	                		logger.debug("Addon Service Already Existed");
	    					Query_PreProcessResult(out18,"425");
	    					return false;
	                	}
	                }
	                             
	                    
	                logger.info("Check Addon Code:" + sSql);
	    			
	    		} else if("D".equals(addonAction.toUpperCase())) {
	    			
	    		}
			}
			
			
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
    	return true;
    }
    
    /*
    private void Query_AddonStatus(PrintWriter out18, Document doc) {
    	logger.info("Check Addon Service");
    	
    	String SERVICE_TAG = "Addon_Service";
    	String ITEM_TAG = "Addon_Item";
    	String CODE_TAG = "Addon_Code";
    	String ACTION_TAG = "Addon_Action";
    	String addonCode = "";
    	String addonAction = "";
    	
    	NodeList serviceNode = doc.getElementsByTagName(SERVICE_TAG);
    	
    	try {
    		if(serviceNode == null || serviceNode.getLength() == 0) {
    			logger.debug("No Add-on service tag");
    			Query_PreProcessResult(out18,"421");
    		}
    		
    		NodeList itemNode = doc.getElementsByTagName(ITEM_TAG);
    		
    		if(itemNode == null || itemNode.getLength() == 0) {
    			logger.debug("No Add-on service code & action");
    			Query_PreProcessResult(out18,"422");
    		}
    		
    		for(int i = 0; i < itemNode.getLength(); i++) {
    			Node node = itemNode.item(i);
    			
    			if(node.getNodeType() == Node.ELEMENT_NODE) {
    				Element e = (Element)node;
    				NodeList codeList = e.getElementsByTagName(CODE_TAG);
    				Element codeElement = (Element)codeList.item(0);
    				NodeList c = codeElement.getChildNodes();
    				addonCode = ((Node)c.item(0)).getNodeValue().trim();
    				
    				if(!checkAddonCode(addonCode)) {
    					logger.debug("Addon Service Code is incorrect");
    	    			Query_PreProcessResult(out18,"423");
    				}
    				
    				NodeList actionList = e.getElementsByTagName(ACTION_TAG);
    				Element actionElement = (Element)actionList.item(0);
    				NodeList a = actionElement.getChildNodes();
    				addonAction = ((Node)a.item(0)).getNodeValue().trim();
    				
    				if(addonAction == null || addonAction.length() == 0) {
    					logger.debug("No Addon Service Action");
    					Query_PreProcessResult(out18,"424");
    				}
    				
    			}
    		}
    		
    		if("A".equals(addonAction.toUpperCase())) {
    			
    			sSql="SELECT COUNT(*) AS ab from ADDONSERVICE WHERE ADDONCODE='"+ 
    			     addonCode + "' AND MNOIMSI= '" + cTWNLDIMSI + "'";
    			TempRt = s2t.Query(sSql);
             
                while (Temprs.next()) {
                	if("0".equals(Temprs.getString("ab"))) {
                		logger.debug("Addon Service Already Existed");
    					Query_PreProcessResult(out18,"425");
                	}
                }
                             
                    
                logger.info("Check Addon Code:" + sSql);
    			
    		} else if("D".equals(addonAction.toUpperCase())) {
    			
    		}	
		} catch (SQLException e) {
			e.printStackTrace();
			sErrorSQL+=sSql;
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }*/
    
    private String parseDocumentToXML(Document doc) throws TransformerException{
    	DOMSource domSource = new DOMSource(doc);
    	StringWriter writer = new StringWriter();
    	StreamResult result = new StreamResult(writer);
    	TransformerFactory tf = TransformerFactory.newInstance();
    	Transformer transformer = tf.newTransformer();
    	transformer.transform(domSource, result);
    	return writer.toString();
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException, SQLException, InterruptedException, Exception {
    	
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        //synchronized 同步程式區塊可以避免多位使用者同時存取實例變數
        //synchronized(this){
        //int iCount,ivln,j,k;//
        //ResultSet Trt=null;
        String cSubscrId="";//,sFile="",VARREALMSG=""sMd="",
        Sparam = "";
        sAllVln="";
        dReqDate="";
        cTicketNumber="";
        cTWNLDIMSI="";
        cTWNLDMSISDN="";
        cS2TMSISDN="";
        cS2TIMSI="";
        cReqStatus="";
        cVLNCountry="";
        cGPRSStatus="";
        sWSFStatus="";
        sWSFDStatus="";
        //VARREALMSG="";
        sError="";
        cRCode="";
        cOldTWNLDMSISDN="";
        cOLDS2TMSISDN="";
        sM_CTYPE="";
        sreturnXml="";
        iError=0;
        sFMTH="";
        sFMTHa="";
        sSFMTH="";
        sSFMTHa="";
        sFORWARD_TO_HOME_NO="";
        sS_FORWARD_TO_HOME_NO="";
        
        sErrorSQL="";
        iErrorMsg="";
        delaySMS.clear();
		
        //20150529 add
        desc="";
        /*********************************************************/
        cAddonCode = "";
        cAddonAction = "";
        cAddonItem.clear();
		/*********************************************************/
        
        Load_Properties(out,getServletContext().getRealPath("/"));

        SMS_Delay_Time =Integer.parseInt(s2tconf.getProperty("SMS_Delay_Time", "30"))*60*1000;
        logger.info("SMS_Delay_Time="+SMS_Delay_Time);
        /*logger.info("The received XML content: ");
    	logger.info(request.getReader().readLine()); 
    	
    	request.getReader().reset();
    	logger.info("request.getReader().reset()");*/
    	
    	
        logger.info("Procedure Start");
        
        out.println("<?xml version='1.0' encoding='UTF-8'?>");
        out.println("<ActivationRsp>");
        sreturnXml = "<?xml version='1.0' encoding='UTF-8'?><ActivationRsp>";
        //20141211 add
      try {
      	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
          DocumentBuilder builder = dbf.newDocumentBuilder();
          Document doc1 = builder.parse(new InputSource(request.getReader()));
	   
          //******************************************************************************************************
          //Document doc1 = builder.parse(new File("C://Users/Administrator/workspace/TWNLD/src/test.xml"));
          //************************************************************************************************
          
          logger.info("The received XML content: ");
      	logger.info(parseDocumentToXML(doc1)); 
      	
          doc1.normalize();
          read_xml(doc1);
          
          ba = Connect_DB();
          logger.info("Connect Database:" + ba);
        
          if((ba == true) && (!cReqStatus.equals(""))) {
             if(!dReqDate.equals("")){
                Sdate = dReqDate.substring(4,6)+"/"+dReqDate.substring(6,8)+"/"+dReqDate.substring(0,4)+" "+dReqDate.substring(8,10)+":"+dReqDate.substring(10,12)+":"+dReqDate.substring(12,14);
                Write_ProvisionLog();
                vln.removeAllElements();
                TempRtA = null;
                
                sSql = "Select subscr_id as ab,nvl(result_flag,'0') as cd from S2T_TB_TYPB_WO_SYNC_FILE_DTL"+
                " where subscr_id ='"+cTicketNumber+"'";
                
                logger.debug("Check TicketNumber:"+sSql);
                TempRtA = s2t.Query(sSql);
              
                while(TempRtA.next()) {
                   cSubscrId = TempRtA.getString("ab");
                   Old_result_flag=TempRtA.getString("cd"); 
                }
                
                //20141211 add
                /*if("18".equals(cReqStatus) && "".equals(cAddonCode)&& "".equals(cAddonAction)){
              	  	logger.debug("AddonCode or AddonAction is ineffective!");
              	  	Query_PreProcessResult(out,"000");
           	       return;
           		 	
                }*/
                //20150506 mod
                if("18".equals(cReqStatus) && cAddonItem.size()==0){
              	  	logger.debug("AddonCode or AddonAction is ineffective!");
              	  	Query_PreProcessResult(out,"000");
           	       return;
           		 	
                }
                
           
                if(!cSubscrId.equals(cTicketNumber)) {
              	  switch(Integer.parseInt(cReqStatus)) {
                      case 0:
                         ReqStatus_00(out);
                         break;
                       case 1:
                          ReqStatus_01(out);
                          break;
                       case 2:
                          ReqStatus_02(out);
                          break;
                       case 3:
                          ReqStatus_03(out);
                          break;
                       case 5:
                          ReqStatus_05(out);
                          break;
                       case 7:
                          ReqStatus_07(out);
                          break; 
                       case 17:
                          ReqStatus_17(out);
                          break;
                       case 18:
                    	   ReqStatus_18(out);	
                    	   break;
                       case 19:
                          ReqStatus_19(out);
                          break;//}
                       case 97:
                      	 //else if (cReqStatus.equals("97")){
                         {
                            sWSFStatus = "O";
                            Process_SyncFile(sWSFStatus);
                            Query_ByPartnerIMSI(out);
                         }
                          break; //}
                       case 98:
                      	 // else if (cReqStatus.equals("98")){
                         {
                            sWSFStatus = "O";
                            Process_SyncFile(sWSFStatus);
                            Query_ByPartnerMSISDN(out);
                         }
                          break;//}
                       case 99:
                          ReqStatus_99(out);
                          break;
                          // else {iError=1;}
                       default:  
                      	 iError=1;
                      	 
                      	 if(!"".equals(iErrorMsg))
                      		 iErrorMsg+=",";
                      	 iErrorMsg+="ReqStatus is incorrect!";
                      	 
                      	 break;
                    }
              	  //}else {Query_PreProcessResult(out,"402");}
				   logger.info("iError:" + iError+", cRCode:" + cRCode);
				   
				   if((iError == 0) && (cRCode.equals("000"))) {
					  RCode_000(out);    
				   }
				   //501 S
				   if ((iError!=0)&&(!cRCode.equals("000"))){
					   S501(out,cRCode);
				   } //501 E
                  } else if (cSubscrId.equals(cTicketNumber) && !Old_result_flag.equals("000")){
                  	switch(Integer.parseInt(cReqStatus)){                	
	                case 0:
	                  ReRunStatus_00(out);
	                  break;
	                case 1:
	                  ReRunStatus_01(out);
	                  break;
	                case 2:
	                  ReRunStatus_02(out);
	                  break;
	                case 3:
	                  ReRunStatus_03(out);
	                  break;
	                case 5:
	                  ReRunStatus_05(out);
	                  break;
	                case 7:
	                  ReRunStatus_07(out);
	                  break;
	                case 17:
	                  ReRunStatus_17(out);
	                  break;
	                case 18:
	                	ReRunStatus_18(out);
	                	break;
	                case 99:
	                	
		                ReRunStatus_99(out);
		                break;
	                default:
						iError = 1;

						if (!"".equals(iErrorMsg))
							iErrorMsg += ",";
						iErrorMsg += "ReqStatus is incorrect!";
                  	}

		              logger.info("iError:"+iError+",cRCode:"+cRCode);
		              if ((iError==0)&&(cRCode.equals("000"))){
		                 RCode_000(out);
		               }
		              //501 S
		              if ((iError!=0)&&(!cRCode.equals("000"))){
		                S501(out,cRCode);
		              } //501 E
                  } else {
                  	Query_PreProcessResult(out,"107");
              	}
             } else{ 
          	   Query_PreProcessResult(out,"110");
      	   }
          } else {
        	  //20150504 add
        	  String s = "DB Error";
              logger.error(s);
                 sSql="update PROVLOG set javaerrmsg='"+s.toString()+"' where LOGID="+ sCMHKLOGID;
                 s2t.Update(sSql);
              sErrorSQL+=sSql;
              Query_PreProcessResult(out,"601");
        	  
              //20150504 mod
        	  /*cRCode="601";
              out.println("<Return_Code>");
              out.println("601");
              out.println("</Return_Code>");
              out.println("<Return_DateTime>");
              out.println(s2t.Date_Format()+s2t.Date_Format_Time());
              out.println("</Return_DateTime>");
              desc="DBconnection error";
              sreturnXml=sreturnXml+"<Return_Code>601</Return_Code><Return_DateTime>"+
                      s2t.Date_Format()+s2t.Date_Format_Time()+"</Return_DateTime>";*/
          }
      }catch(SQLException ex){
        StringWriter   s   =   new   StringWriter();
        ex.printStackTrace(new PrintWriter(s));
        logger.error("JAVA Error:"+s.toString());
           sSql="update PROVLOG set javaerrmsg='"+s.toString()+"' where LOGID="+ sCMHKLOGID;
           s2t.Update(sSql);
        Query_PreProcessResult(out,"600");
      }catch(Exception ex){
        StringWriter s = new StringWriter();
        ex.printStackTrace(new PrintWriter(s));
        logger.error("JAVA Error:"+s.toString());
         sSql="update PROVLOG set javaerrmsg='"+s.toString()+"' where LOGID="+
                 sCMHKLOGID;
         s2t.Update(sSql);
         Query_PreProcessResult(out,"600");
      }finally {
      	
        logger.info("Procedure End");
        out.println("<Return_MSG>");
        out.println(desc);
        out.println("</Return_MSG>");
        out.println("</ActivationRsp>");
        sreturnXml=sreturnXml+"<Return_MSG>"+desc+"</Return_MSG></ActivationRsp>";
        logger.info(sreturnXml);
        //20141211 add
        if(!"18".equals(cReqStatus) || cAddonItem.size()>0){
      	  if ((!cReqStatus.equals("97"))||(!cReqStatus.equals("98"))) {
              try {
					runtime.exec(s2tconf.getProperty("Run_Shell"));
				} catch (Exception e) {
					e.printStackTrace();
				}
  				  //Send False Mail
  				 if ((!Process_Code.equals("000"))||(!cRCode.equals("000"))){
  					Send_AlertMail();
  				 }
  			}
        } 
        
          out.close();
          vln.clear();
          Tmpvln.clear();
          s2tconf.clear();
          s2t.Close();
  
          //20150520 add  
          //delay 30 minute to send
          /*Thread t = new Thread(new ThreadExample2(delaySMS) );
		   
		  t.setDaemon(true);
		  t.start();*/ 
          
      }
        //} //synchronized end
      
      
    }
    
    public class ThreadExample2 implements Runnable {
    	List<Map<String,String>> delaySMS =new ArrayList<Map<String,String>>();
    	public ThreadExample2(List<Map<String,String>> delaySMS){
    		this.delaySMS=delaySMS;
    	}
        public void run() { // implements Runnable run()
        	
        	 try {
				Thread.sleep(SMS_Delay_Time);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
             send_dalay_SMS(delaySMS);
        }
    }


    public void ReqStatus_00(PrintWriter out0) throws SQLException, ClassNotFoundException, IOException, Exception{
                //if (cReqStatus.equals("00")){
                csta=Check_Pair_IMSI(cTWNLDIMSI,cS2TIMSI);
                if (csta.equals("1")){
                    csta="";
                csta=Check_Pair_IMSI_status(cTWNLDIMSI,cS2TIMSI);
                if (csta.equals("0")){
                    csta="";
                csta=Validate_TicketNumber();
                if (csta.equals("000")){
                //Check S2T IMSI
                  Temprs=null;
                  smsi="";
                   sSql="select count(*) as ab from imsi  where homeimsi='"+
                           cTWNLDIMSI+"' and status=0";
                          logger.info("Check S2T IMSI:"+sSql);
                  Temprs=s2t.Query(sSql);
                          while (Temprs.next()){
                               smsi=Temprs.getString("ab");
                               }
                  if (smsi.equals("1")){bb=true;} else {bb=false;}
                if (bb==true){
                //Check CHT MSISDN
                bc=Validate_PartnerMSISDNRange(cTWNLDMSISDN);
                if (bc==true){
                   csta="0";
                   while (csta.equals("0")){
                   Find_AvailableS2TMSISDN();
                   if (!cS2TMSISDN.equals("null")){
                   csta=Check_S2T_Msisdn_UNused();}
                   else {csta="1";}
                   }
                    if (!cS2TMSISDN.equals("null")){
                        sError="0";
                        if (!cVLNCountry.equals("")){
                          sError=Process_VLNString( cVLNCountry);}
                        if (sError.equals("0")){
                            smsi=Query_PartnerMSISDNStatus();
                          if (smsi.equals("0")){
                        //Clean Vector ,spec:JAPAN,81XXXX,JPN
                          sSql="delete followmedata where followmenumber =" +
       "(select followmenumber from followmedata where followmenumber='"+
                                  cTWNLDMSISDN+"')";
                          logger.debug("DELETE followmenumber:"+sSql);
                          s2t.Delete(sSql);
                       sWSFStatus="V";
                       sWSFDStatus="V";
                       Process_SyncFile(sWSFStatus);
                       Process_SyncFileDtl(sWSFDStatus);
                       Process_ServiceOrder();
                       Process_WorkSubcode();
                       sSql="update S2T_TB_SERVICE_ORDER set STATUS='N' where "+
                               "SERVICE_ORDER_NBR='"+cServiceOrderNBR+"'";
                       s2t.Update(sSql);
                        logger.debug("update SERVICE_ORDER:"+sSql);
                       Query_PreProcessResult(out0,"000");
                      Query_GPRSStatus();
        out0.println("<S2T_MSISDN>");
        out0.println(cS2TMSISDN);
        out0.println("</S2T_MSISDN>");
        sreturnXml=sreturnXml+"<S2T_MSISDN>"+cS2TMSISDN+"</S2T_MSISDN>";
                          }
                          else {Query_PreProcessResult(out0,"210");}
                    }
                        else
                        {Query_PreProcessResult(out0,"402");}
                    }
                    else {
                    	iError = 1;
                    	Query_PreProcessResult(out0, "203");
                    	if (!"".equals(iErrorMsg))
                    		iErrorMsg += ",";
                    	iErrorMsg += "Error Code 203!";
                    	}
                  } else {iError=1;
                      Query_PreProcessResult(out0,"109");
                      if (!"".equals(iErrorMsg))
                    	  iErrorMsg += ",";
                      iErrorMsg += "Error Code 109!";}
                } else {
                iError=1;
                Query_PreProcessResult(out0,"108");
                if (!"".equals(iErrorMsg))
					iErrorMsg += ",";
				iErrorMsg += "Error Code 108!";}
                }
           else{
                iError=1;
                Query_PreProcessResult(out0,"107");
                if (!"".equals(iErrorMsg))
					iErrorMsg += ",";
				iErrorMsg += "Error Code 107!";}
                }else {Query_PreProcessResult(out0,"112");}
                }else{Query_PreProcessResult(out0,"111");}
    }

    public void ReRunStatus_00(PrintWriter out0) throws SQLException, Exception{
       String sV="",sM="",scountryname="",sVLN="";//sE="",
       String Scut=null;
       logger.debug("ReRunStatus_00");
       //Temprs=null;
      csta=Check_Pair_IMSI(cTWNLDIMSI,cS2TIMSI);
        if (csta.equals("1")){
       iCut=Sparam.indexOf(",");
       if (iCut>0){
       TempSparam=Sparam.substring(iCut+1, Sparam.length());
       iCountA=Check_Tag(TempSparam);
       /*sSql="select count(CONTENT) as ab from PROVLOG"+
            " where substr(CONTENT,29,length(CONTENT)-28)='"+TempSparam+"'";
       logger.debug("Check TAG:"+sSql);
       TempRtA=s2t.Query(sSql);
       while (TempRtA.next()){
         iCountA=TempRtA.getInt("ab");
       }*/
       if (iCountA>1){
           Find_Old_ORDER_NBR();
           Find_Old_step_no();
       /*TempRtA=null;
       sSql="Select max(SERVICE_ORDER_NBR) as ab,max(WORK_ORDER_NBR) as cd,S2T_MSISDN from "+
       "S2T_TB_TYPB_WO_SYNC_FILE_DTL where subscr_id ='"+cTicketNumber+"' "+
               "group by S2T_MSISDN";
       logger.debug("Check Old_SERVICE_ORDER_NBR:"+sSql);
       TempRtA=s2t.Query(sSql);
       while (TempRtA.next()){
                sOld_SERVICE_ORDER_NBR=TempRtA.getString("ab");
                sOld_WORK_ORDER_NBR=TempRtA.getString("cd");
                cS2TMSISDN=TempRtA.getString("S2T_MSISDN");
            }*/
       /*TempRtA=null;
       sSql="select nvl(min(step_no),'0') as ab from S2T_TB_SERVICE_ORDER_ITEM "+
        "where SERVICE_ORDER_NBR="+sOld_SERVICE_ORDER_NBR+" and status <>'Y'";
       logger.debug("Check step_no:"+sSql);
       TempRtA=s2t.Query(sSql);
       while (TempRtA.next()){
                sOld_step_no=TempRtA.getString("ab");
            }*/
       //if (!sOld_step_no.equals("0")){
       Scut=cVLNCountry;
           while (Scut.length()>0){
       iCut=0;
       iCut=Scut.indexOf(",");
      if (iCut>0) {
       sV=Scut.substring(0, iCut);
       Scut=Scut.substring(iCut+1, Scut.length());
       sM=sV.substring(sV.length()-1,sV.length());
       sV=sV.substring(0,sV.length()-1);}
       else{sM=Scut.substring(Scut.length()-1,Scut.length());
       sV=Scut.substring(0,Scut.length()-1);
          Scut="";}
       TempRtA=null;
       sSql="Select countryname from Countryinitial where "+
             "countryinit='"+sV+"'";
      logger.debug("Check Countryinitial:"+sSql);
      TempRtA=s2t.Query(sSql);
      while (TempRtA.next()){
        scountryname=TempRtA.getString("countryname");
      }
      TempRtA=null;
      sSql="select vln_"+scountryname+" as ab from S2T_TB_TYPB_WO_SYNC_FILE_DTL "+
           "where subscr_id ='"+cTicketNumber+"'";
      logger.debug("Check Vln_Number:"+sSql);
      TempRtA=s2t.Query(sSql);
      while (TempRtA.next()){
        sVLN=TempRtA.getString("ab");
      }
      vln.add(scountryname+","+sVLN+","+sV+","+sM);
           }

            sWSFStatus = "V";
            sWSFDStatus = "V";
            Process_SyncFile(sWSFStatus);
            Process_SyncFileDtl(sWSFDStatus);
            Process_ServiceOrder();
            reProcess_WorkSubcode(sOld_step_no);
            sSql = "update S2T_TB_SERVICE_ORDER set STATUS='N' where SERVICE_ORDER_NBR='" + cServiceOrderNBR + "'";
            s2t.Update(sSql);
            logger.debug("update SERVICE_ORDER:" + sSql);
            Query_PreProcessResult(out0, "000");
            Query_GPRSStatus();
            out0.println("<S2T_MSISDN>");
            out0.println(cS2TMSISDN);
            out0.println("</S2T_MSISDN>");
       sreturnXml=sreturnXml+"<S2T_MSISDN>"+cS2TMSISDN+"</S2T_MSISDN>";
       }
        //}
       else{Query_PreProcessResult(out0,"201");}
       }
        }else{Query_PreProcessResult(out0,"111");}
    }

    public void ReqStatus_01(PrintWriter out1) throws SQLException, IOException, InterruptedException, Exception{
      //else if (cReqStatus.equals("01")){
            csta=Check_Pair_IMSI(cTWNLDIMSI,cS2TIMSI);
                if (csta.equals("1")){
                    csta="";
             csta=Check_TWN_Msisdn_Status(cTWNLDIMSI,cS2TIMSI);
                if (!"0".equals(csta)){
                    csta="";
                csta=Query_ServiceStatus();
                if (csta.equals("")){csta="0";}
                switch (Integer.parseInt(csta)){
                    case 3:
                        iError=1;
                        Query_PreProcessResult(out1,"206");
                        if (!"".equals(iErrorMsg))
							iErrorMsg += ",";
						iErrorMsg += "Error Code 206!";
                        break;
                    case 4:
                    case 10:
                        iError=1;
                        Query_PreProcessResult(out1,"201");
                        if (!"".equals(iErrorMsg))
							iErrorMsg += ",";
						iErrorMsg += "Error Code 201!";
                        break;
                    default:
                       //Check S2T IMSI
                      bb=Validate_IMSIRange(cS2TIMSI);
                      if (bb==true){
                        //Check CHT MSISDN
                        bc=Validate_PartnerMSISDNRange(cTWNLDMSISDN);
                        if (bc==true){
                          Get_GurrentS2TMSISDN();
                          if (!cS2TMSISDN.equals("")){
                          smsi=Query_PartnerMSISDNStatus();
                          if (!smsi.equals("0")){
                            sWSFStatus="V";
                            sWSFDStatus="V";
                            Process_SyncFile(sWSFStatus);
                            Process_SyncFileDtl(sWSFDStatus);
                            Process_ServiceOrder();
                            Process_WorkSubcode();
                       sSql="update S2T_TB_SERVICE_ORDER set STATUS='N' where "+
                               "SERVICE_ORDER_NBR='"+cServiceOrderNBR+"'";
                       s2t.Update(sSql);
                       logger.debug("update SERVICE_ORDER:"+sSql);
                            Query_PreProcessResult(out1,"000");}
                          else {Query_PreProcessResult(out1,"211");}
                          }
                          else {iError=1;
                         Query_PreProcessResult(out1,"108");
                         if (!"".equals(iErrorMsg))
								iErrorMsg += ",";
							iErrorMsg += "Error Code 108!";}
                        }
                        else {iError=1;
                         Query_PreProcessResult(out1,"109");
                         if (!"".equals(iErrorMsg))
								iErrorMsg += ",";
							iErrorMsg += "Error Code 109!";}
                      }
                      else {
                        iError=1;
                        Query_PreProcessResult(out1,"101");
                        if (!"".equals(iErrorMsg))
							iErrorMsg += ",";
						iErrorMsg += "Error Code 101!";}
                        break;
                }
                }else {Query_PreProcessResult(out1,"211");}
                }else {Query_PreProcessResult(out1,"111");}
    }

    public void ReRunStatus_01(PrintWriter reout1) throws SQLException, IOException, InterruptedException, Exception{
      //else if (cReqStatus.equals("01")){
        logger.debug("ReRunStatus_01");
            csta=Check_Pair_IMSI(cTWNLDIMSI,cS2TIMSI);
                if (csta.equals("1")){
                    csta="";
             csta=Check_TWN_Msisdn_Status(cTWNLDIMSI,cS2TIMSI);
                if (!"0".equals(csta)){
                    csta="";
                csta=Query_ServiceStatus();
                if (csta.equals("")){csta="0";}
                switch (Integer.parseInt(csta)){
                    case 3:
                        iError=1;
                        Query_PreProcessResult(reout1,"206");
                        if (!"".equals(iErrorMsg))
							iErrorMsg += ",";
						iErrorMsg += "Error Code 206!";
                        break;
                    case 4:
                    case 10:
                        iError=1;
                        Query_PreProcessResult(reout1,"201");
                        if (!"".equals(iErrorMsg))
							iErrorMsg += ",";
						iErrorMsg += "Error Code 201!";
                        break;
                    default:
                       //Check S2T IMSI
                      bb=Validate_IMSIRange(cS2TIMSI);
                      if (bb==true){
                        //Check CHT MSISDN
                        bc=Validate_PartnerMSISDNRange(cTWNLDMSISDN);
                        if (bc==true){
                          Get_GurrentS2TMSISDN();
                          if (!cS2TMSISDN.equals("")){
                          smsi=Query_PartnerMSISDNStatus();
                          if (!smsi.equals("0")){
                             iCut=Sparam.indexOf(",");
                             if (iCut>0){                                 
                               TempSparam=Sparam.substring(iCut+1, Sparam.length());
                               iCountA=Check_Tag(TempSparam);
                             if (iCountA>1){
                               Find_Old_ORDER_NBR();
                               Find_Old_step_no();
                             //if (!sOld_step_no.equals("0")){
                            sWSFStatus="V";
                            sWSFDStatus="V";
                            Process_SyncFile(sWSFStatus);
                            Process_SyncFileDtl(sWSFDStatus);
                            Process_ServiceOrder();
                            reProcess_WorkSubcode(sOld_step_no);
                       sSql="update S2T_TB_SERVICE_ORDER set STATUS='N' where "+
                               "SERVICE_ORDER_NBR='"+cServiceOrderNBR+"'";
                       s2t.Update(sSql);
                       logger.debug("update SERVICE_ORDER:"+sSql);
                            Query_PreProcessResult(reout1,"000");}//}
       else{Query_PreProcessResult(reout1,"201");}
                             }}
                          else {Query_PreProcessResult(reout1,"211");}
                          }
                          else {iError=1;
                         Query_PreProcessResult(reout1,"108");
                         if (!"".equals(iErrorMsg))
								iErrorMsg += ",";
							iErrorMsg += "Error Code 108!";}
                        }
                        else {iError=1;
                         Query_PreProcessResult(reout1,"109");
                         if (!"".equals(iErrorMsg))
								iErrorMsg += ",";
							iErrorMsg += "Error Code 109!";}
                      }
                      else {
                        iError=1;
                        Query_PreProcessResult(reout1,"101");
                        if (!"".equals(iErrorMsg))
							iErrorMsg += ",";
						iErrorMsg += "Error Code 101!";}
                        break;
                }
                }else {Query_PreProcessResult(reout1,"211");}
                }else {Query_PreProcessResult(reout1,"111");}
    }

    public void ReqStatus_02(PrintWriter out2) throws SQLException, IOException, InterruptedException, Exception{
      //else if (cReqStatus.equals("02")){
              csta=Check_Pair_IMSI(cTWNLDIMSI,cS2TIMSI);
                if (csta.equals("1")){
                    csta="";
              csta=Check_TWN_Msisdn_Status(cTWNLDIMSI,cS2TIMSI);
                if (!"0".equals(csta)){
                    csta="";
              csta=Query_ServiceStatus();
                if (csta.equals("")){csta="0";}
                switch (Integer.parseInt(csta)){
                    case 1:
                        iError=1;
                        Query_PreProcessResult(out2,"207");
                        if (!"".equals(iErrorMsg))
							iErrorMsg += ",";
						iErrorMsg += "Error Code 207!";
                        break;
                    case 4:
                    case 10:
                        iError=1;
                        Query_PreProcessResult(out2,"201");
                        if (!"".equals(iErrorMsg))
							iErrorMsg += ",";
						iErrorMsg += "Error Code 201!";
                        break;
                    default:
                        //Check S2T IMSI
                      bb=Validate_IMSIRange(cS2TIMSI);
                      if (bb==true){
                        //Check CHT MSISDN
                        bc=Validate_PartnerMSISDNRange(cTWNLDMSISDN);
                        if (bc==true){
                          Get_GurrentS2TMSISDN();
                          if (!cS2TMSISDN.equals("")){
                          smsi=Query_PartnerMSISDNStatus();
                          if (!smsi.equals("0")){
                            sWSFStatus="V";
                            sWSFDStatus="V";
                            Process_SyncFile(sWSFStatus);
                            Process_SyncFileDtl(sWSFDStatus);
                            Process_ServiceOrder();
                            Process_WorkSubcode();
                       sSql="update S2T_TB_SERVICE_ORDER set STATUS='N' where "+
                               "SERVICE_ORDER_NBR='"+cServiceOrderNBR+"'";
                       s2t.Update(sSql);
                       logger.debug("update SERVICE_ORDER:"+sSql);
                            Query_PreProcessResult(out2,"000");}
                          else {Query_PreProcessResult(out2,"211");}
                          }
                          else {iError=1;
                         Query_PreProcessResult(out2,"108");
                         if (!"".equals(iErrorMsg))
								iErrorMsg += ",";
							iErrorMsg += "Error Code 108!";}
                        }
                        else {iError=1;
                         Query_PreProcessResult(out2,"109");
                         if (!"".equals(iErrorMsg))
								iErrorMsg += ",";
							iErrorMsg += "Error Code 109!";}
                      }
                      else {
                        iError=1;
                        Query_PreProcessResult(out2,"101");
                        if (!"".equals(iErrorMsg))
							iErrorMsg += ",";
						iErrorMsg += "Error Code 101!";}
                        break;
                }
                }else {Query_PreProcessResult(out2,"211");}
                }else {Query_PreProcessResult(out2,"111");}
    }

    public void ReRunStatus_02(PrintWriter out2) throws SQLException, IOException, InterruptedException, Exception{
      //else if (cReqStatus.equals("02")){
        logger.debug("ReRunStatus_02");
              csta=Check_Pair_IMSI(cTWNLDIMSI,cS2TIMSI);
                if (csta.equals("1")){
                    csta="";
              csta=Check_TWN_Msisdn_Status(cTWNLDIMSI,cS2TIMSI);
                if (!"0".equals(csta)){
                    csta="";
              csta=Query_ServiceStatus();
                if (csta.equals("")){csta="0";}
                switch (Integer.parseInt(csta)){
                    case 1:
                        iError=1;
                        Query_PreProcessResult(out2,"207");
                        if (!"".equals(iErrorMsg))
									iErrorMsg += ",";
								iErrorMsg += "Error Code 207!";
                        break;
                    case 4:
                    case 10:
                        iError=1;
                        Query_PreProcessResult(out2,"201");
                        if (!"".equals(iErrorMsg))
							iErrorMsg += ",";
						iErrorMsg += "Error Code 201!";
                        break;
                    default:
                        //Check S2T IMSI
                      bb=Validate_IMSIRange(cS2TIMSI);
                      if (bb==true){
                        //Check CHT MSISDN
                        bc=Validate_PartnerMSISDNRange(cTWNLDMSISDN);
                        if (bc==true){
                          Get_GurrentS2TMSISDN();
                          if (!cS2TMSISDN.equals("")){
                          smsi=Query_PartnerMSISDNStatus();
                          if (!smsi.equals("0")){
                              iCut=Sparam.indexOf(",");
                               TempSparam=Sparam.substring(iCut+1, Sparam.length());
                               iCountA=Check_Tag(TempSparam);
                             if (iCountA>1){
                               Find_Old_ORDER_NBR();
                               Find_Old_step_no();
                             //if (!sOld_step_no.equals("0")){
                            sWSFStatus="V";
                            sWSFDStatus="V";
                            Process_SyncFile(sWSFStatus);
                            Process_SyncFileDtl(sWSFDStatus);
                            Process_ServiceOrder();
                            reProcess_WorkSubcode(sOld_step_no);
                       sSql="update S2T_TB_SERVICE_ORDER set STATUS='N' where "+
                               "SERVICE_ORDER_NBR='"+cServiceOrderNBR+"'";
                       s2t.Update(sSql);
                       logger.debug("update SERVICE_ORDER:"+sSql);
                            Query_PreProcessResult(out2,"000");}//}
       else{Query_PreProcessResult(out2,"201");}
                             }
                          else {Query_PreProcessResult(out2,"211");}
                          }
                          else {iError=1;
                         Query_PreProcessResult(out2,"108");
                         if (!"".equals(iErrorMsg))
								iErrorMsg += ",";
							iErrorMsg += "Error Code 108!";}
                        }
                        else {iError=1;
                         Query_PreProcessResult(out2,"109");
                         if (!"".equals(iErrorMsg))
								iErrorMsg += ",";
							iErrorMsg += "Error Code 109!";}
                      }
                      else {
                        iError=1;
                        Query_PreProcessResult(out2,"101");
                        if (!"".equals(iErrorMsg))
							iErrorMsg += ",";
						iErrorMsg += "Error Code 101!";}
                        break;
                }
                }else {Query_PreProcessResult(out2,"211");}
                }else {Query_PreProcessResult(out2,"111");}
    }

    public void ReqStatus_03(PrintWriter out3) throws SQLException, ClassNotFoundException, IOException, Exception{
      //else if (cReqStatus.equals("03")){
                        //Check S2T IMSI
                                  Temprs=null;
                  smsi="";
               csta=Check_Pair_IMSI(cTWNLDIMSI,cS2TIMSI);
               
               if (csta.equals("1")){
            	   //csta="";
            	   //csta=Check_TWN_Msisdn_Status(cTWNLDIMSI,cS2TIMSI);
            	   //if (cTWNLDMSISDN.equals("0")){
            	   
            	   csta="";

            	   sSql="select count(*) as ab from imsi  where homeimsi='"+cTWNLDIMSI+"'";
            	   logger.info("Check S2T IMSI:"+sSql);
            	   
            	   Temprs=s2t.Query(sSql);
            	   while (Temprs.next()){
            		   smsi=Temprs.getString("ab");
            	   }
            	   
            	   if (smsi.equals("1")){
            		   bb=true;
            	   } else {
            		   bb=false;
            	   }
            	   
                   if (bb==true){
                	   //Check CHT MSISDN
                	   bc=Validate_PartnerMSISDNRange(cTWNLDMSISDN);
                	   
                	   if (bc==true){
                		   Get_GurrentS2TMSISDN();
                		   
                		   if (cS2TMSISDN.equals("")){
                			   Temprs=null;
                			   
                			   sSql="select s2tmsisdn from availablemsisdn where partnermsisdn='"+cTWNLDMSISDN+"'";
                			   logger.info("Find_OLDS2TMSISDN:"+sSql);
                			   Temprs=s2t.Query(sSql);
                			   
                			   while (Temprs.next()){
                				   cOLDS2TMSISDN=Temprs.getString("s2tmsisdn");
                               }
                			   
                			   if (!cOLDS2TMSISDN.equals("")){
                				   cS2TMSISDN=cOLDS2TMSISDN;
                				   Temprs=null;
                				   sSql="select count(*) as ab FROM imsi WHERE homeimsi = '"+cTWNLDIMSI+"' and status=0";
                				   
                				   logger.info("count(homeimsi):"+sSql);
                				   Temprs=s2t.Query(sSql);
                				   
                				   while (Temprs.next()){
                					   smsi=Temprs.getString("ab");
                				   }
                				   
                				   if (smsi.equals("1")){
                					   sWSFStatus="V";
                					   sWSFDStatus="V";
                					   Process_SyncFile(sWSFStatus);
                					   Process_SyncFileDtl(sWSFDStatus);
                					   Process_ServiceOrder();
                					   Process_WorkSubcode();
                					   
                					   sSql="update S2T_TB_SERVICE_ORDER set STATUS='N' "+
                							   "where SERVICE_ORDER_NBR='"+cServiceOrderNBR+"'";
                					   s2t.Update(sSql);
                					   logger.debug("update SERVICE_ORDER:"+sSql);
                					   Query_PreProcessResult(out3,"000");
            					   }else {
            						   Query_PreProcessResult(out3,"108");
        						   }
                			   }else{
                				   Query_PreProcessResult(out3,"211");
            				   }
                		   }else {
                			   iError=1;
                			   Query_PreProcessResult(out3,"200");
                			   
                			   if (!"".equals(iErrorMsg))
                				   iErrorMsg += ",";
                			   
                			   iErrorMsg += "Error Code 200!";
            			   }
                	   }else {
                		   iError=1;
                		   Query_PreProcessResult(out3,"109");
                		   
                		   if (!"".equals(iErrorMsg))
								iErrorMsg += ",";
							iErrorMsg += "Error Code 109!";
						}
                   }else {
                	   
                        iError=1;
                        Query_PreProcessResult(out3,"108");
                        if (!"".equals(iErrorMsg))
							iErrorMsg += ",";
						iErrorMsg += "Error Code 108!";
					}
               } //}else{Query_PreProcessResult(out3,"211");}
                else{Query_PreProcessResult(out3,"111");
            }
    }

    public void ReRunStatus_03(PrintWriter out3) throws SQLException, ClassNotFoundException, IOException, Exception{
      //else if (cReqStatus.equals("03")){
             logger.debug("ReRunStatus_03");
             
             Temprs=null;
             smsi="";
             
             csta=Check_Pair_IMSI(cTWNLDIMSI,cS2TIMSI);
             if (csta.equals("1")){
            	 //csta="";
            	 //csta=Check_TWN_Msisdn_Status(cTWNLDIMSI,cS2TIMSI);
            	 //if (cTWNLDMSISDN.equals(csta)){
            	 csta="";
            	 sSql="select count(*) as ab from imsi  where homeimsi='"+cTWNLDIMSI+"'";
            	 logger.info("Check S2T IMSI:"+sSql);
            	 Temprs=s2t.Query(sSql);
            	 while (Temprs.next()){
            		 smsi=Temprs.getString("ab");
        		 }
            	 
            	 if (smsi.equals("1")){
            		 bb=true;
        		 } else {
        			 bb=false;
    			 }
            	 
            	 if (bb==true){
            		 //Check CHT MSISDN
            		 bc=Validate_PartnerMSISDNRange(cTWNLDMSISDN);
            		 if (bc==true){
            			 Get_GurrentS2TMSISDN();
            			 if (cS2TMSISDN.equals("")){
            				 Temprs=null;
            				 sSql="select s2tmsisdn from availablemsisdn where partnermsisdn='"+
            				 cTWNLDMSISDN+"'";
            				 logger.info("Find_OLDS2TMSISDN:"+sSql);
            				 Temprs=s2t.Query(sSql);
            				 while (Temprs.next()){
            					 cOLDS2TMSISDN=Temprs.getString("s2tmsisdn");
        					 }
            				 
            				 if (!cOLDS2TMSISDN.equals("")){
            					 cS2TMSISDN=cOLDS2TMSISDN;
            					 Temprs=null;
            					 sSql="select count(*) as ab FROM imsi WHERE homeimsi = '"+cTWNLDIMSI+"' and status=0";
            					 logger.info("count(homeimsi):"+sSql);
            					 Temprs=s2t.Query(sSql);
            					 while (Temprs.next()){
            						 smsi=Temprs.getString("ab");
        						 }
            					 if (smsi.equals("1")){
            						 iCut=Sparam.indexOf(",");
            						 TempSparam=Sparam.substring(iCut+1, Sparam.length());
            						 iCountA=Check_Tag(TempSparam);
            						 
            						 if (iCountA>1){
            							 Find_Old_ORDER_NBR();
            							 Find_Old_step_no();
            							 //if (!sOld_step_no.equals("0")){
            							 sWSFStatus="V";
            							 sWSFDStatus="V";
            							 Process_SyncFile(sWSFStatus);
            							 Process_SyncFileDtl(sWSFDStatus);
            							 Process_ServiceOrder();
            							 reProcess_WorkSubcode(sOld_step_no);
            							 sSql="update S2T_TB_SERVICE_ORDER set STATUS='N' "+
			                                "where SERVICE_ORDER_NBR='"+cServiceOrderNBR+"'";
            							 s2t.Update(sSql);
            							 logger.debug("update SERVICE_ORDER:"+sSql);
            							 Query_PreProcessResult(out3,"000");
        							 }//}
            						 else{
            							 Query_PreProcessResult(out3,"201");
        							 }
            					 }else {
            						 Query_PreProcessResult(out3,"108");
        						 }
            				 }else{
            					 Query_PreProcessResult(out3,"211");
        					 }
            			 }else {
            				 iError=1;
            				 Query_PreProcessResult(out3,"200");
            				 if (!"".equals(iErrorMsg))
            					 iErrorMsg += ",";
            				 iErrorMsg += "Error Code 200!";
        				 }
            		 }else {
            			 iError=1;
                         Query_PreProcessResult(out3,"109");
                         if (!"".equals(iErrorMsg))
								iErrorMsg += ",";
							iErrorMsg += "Error Code 109!";
					}
            	 }else {
                        iError=1;
                        Query_PreProcessResult(out3,"108");
                        if (!"".equals(iErrorMsg))
							iErrorMsg += ",";
						iErrorMsg += "Error Code 108!";}
                //}else{Query_PreProcessResult(out3,"111");}
                }else{Query_PreProcessResult(out3,"111");}
    }

    public void ReqStatus_05(PrintWriter out5) throws SQLException, ClassNotFoundException, IOException, Exception{
      //else if (cReqStatus.equals("05")){
             csta=Check_Pair_IMSI(cTWNLDIMSI,cS2TIMSI);
                if (csta.equals("1")){
                  //csta="";
               //csta=Check_TWN_Msisdn_Status(cTWNLDIMSI,cS2TIMSI);
                //if (cTWNLDMSISDN.equals(csta)){
                    csta="";
                    //Check S2T IMSI
                      bb=Validate_IMSIRange(cS2TIMSI);
                      if (bb==true){
                        //Check CHT MSISDN
                        bc=Validate_PartnerMSISDNRange(cTWNLDMSISDN);
                        if (bc==true){
                         TempRt=null;
                         sSql="SELECT followmenumber as ab FROM followmedata "+
                                   "WHERE serviceid=(SELECT MAX(Serviceid) "+
                                   "FROM imsi WHERE homeimsi = '"+cTWNLDIMSI+"')";
                         logger.debug("Find_cOldTWNLDMSISDN:"+sSql);
                           TempRt=s2t.Query(sSql);
                           while (TempRt.next()){cOldTWNLDMSISDN=TempRt.getString("ab");}
                           if (!cOldTWNLDMSISDN.equals("")){
                          Get_GurrentS2TMSISDN();
                          if (!cS2TMSISDN.equals("")){
                          smsi=Query_PartnerMSISDNStatus();
                          if (smsi.equals("0")){
                            Check_Type_Code_87_MAP_VALUE(cS2TMSISDN);
                            //20150522 add
                            Query_GPRSStatus();
                            
                            sWSFStatus="V";
                            sWSFDStatus="V";
                            Process_SyncFile(sWSFStatus);
                            Process_SyncFileDtl(sWSFDStatus);
                            Process_ServiceOrder();
                            //Process_WorkSubcode();
                            Process_WorkSubcode_05_17(cS2TIMSI,cTWNLDIMSI,cReqStatus,cTWNLDMSISDN);
                       sSql="update S2T_TB_SERVICE_ORDER set STATUS='N' where "+
                               "SERVICE_ORDER_NBR='"+cServiceOrderNBR+"'";
                       s2t.Update(sSql);
                       logger.debug("update SERVICE_ORDER:"+sSql);
                            Query_PreProcessResult(out5,"000");
                          }
                          else {Query_PreProcessResult(out5,"210");}
                          }
                        else {iError=1;
                         Query_PreProcessResult(out5,"108");
                         if (!"".equals(iErrorMsg))
								iErrorMsg += ",";
							iErrorMsg += "Error Code 108!";}
                        }else {Query_PreProcessResult(out5,"211");}
                        }else {iError=1;
                         Query_PreProcessResult(out5,"109");
                         if (!"".equals(iErrorMsg))
								iErrorMsg += ",";
							iErrorMsg += "Error Code 109!";}
                      }
                      else {
                        iError=1;
                        Query_PreProcessResult(out5,"101");
                        if (!"".equals(iErrorMsg))
							iErrorMsg += ",";
						iErrorMsg += "Error Code 101!";}
                //}else{Query_PreProcessResult(out5,"111");}
                }else{Query_PreProcessResult(out5,"111");}
    }

    public void ReRunStatus_05(PrintWriter out5) throws SQLException, ClassNotFoundException, IOException, Exception{
      //else if (cReqStatus.equals("05")){
        logger.debug("ReRunStatus_05");
             csta=Check_Pair_IMSI(cTWNLDIMSI,cS2TIMSI);
                if (csta.equals("1")){
                  //csta="";
               //csta=Check_TWN_Msisdn_Status(cTWNLDIMSI,cS2TIMSI);
                //if (cTWNLDMSISDN.equals(csta)){
                    csta="";
                    //Check S2T IMSI
                      bb=Validate_IMSIRange(cS2TIMSI);
                      if (bb==true){
                        //Check CHT MSISDN
                        bc=Validate_PartnerMSISDNRange(cTWNLDMSISDN);
                        if (bc==true){
                         TempRt=null;
                         sSql="SELECT followmenumber as ab FROM followmedata "+
                                   "WHERE serviceid=(SELECT MAX(Serviceid) "+
                                   "FROM imsi WHERE homeimsi = '"+cTWNLDIMSI+"')";
                         logger.debug("Find_cOldTWNLDMSISDN:"+sSql);
                           TempRt=s2t.Query(sSql);
                           while (TempRt.next()){cOldTWNLDMSISDN=TempRt.getString("ab");}
                           if (!cOldTWNLDMSISDN.equals("")){
                          Get_GurrentS2TMSISDN();
                          if (!cS2TMSISDN.equals("")){
                          smsi=Query_PartnerMSISDNStatus();
                          if (smsi.equals("0")){
                              iCut=Sparam.indexOf(",");
                               TempSparam=Sparam.substring(iCut+1, Sparam.length());
                               iCountA=Check_Tag(TempSparam);
                             if (iCountA>1){
                            	 //20150522 add
                            	 Query_GPRSStatus();
                               
                            	 Find_Old_ORDER_NBR();
                               Find_Old_step_no();
                             //if (!sOld_step_no.equals("0")){
                            Check_Type_Code_87_MAP_VALUE(cS2TMSISDN);
                            sWSFStatus="V";
                            sWSFDStatus="V";
                            Process_SyncFile(sWSFStatus);
                            Process_SyncFileDtl(sWSFDStatus);
                            Process_ServiceOrder();
                            //reProcess_WorkSubcode(sOld_step_no);
                            reProcess_WorkSubcode_05_17(cTWNLDIMSI,cS2TIMSI,sOld_step_no,cReqStatus,cTWNLDMSISDN);
                       sSql="update S2T_TB_SERVICE_ORDER set STATUS='N' where "+
                               "SERVICE_ORDER_NBR='"+cServiceOrderNBR+"'";
                       s2t.Update(sSql);
                       logger.debug("update SERVICE_ORDER:"+sSql);
                       Query_PreProcessResult(out5,"000");}//}
                          else{Query_PreProcessResult(out5,"201");}
                             }
                          else {Query_PreProcessResult(out5,"210");}
                          }
                        else {iError=1;
                         Query_PreProcessResult(out5,"108");
                         if (!"".equals(iErrorMsg))
								iErrorMsg += ",";
							iErrorMsg += "Error Code 108!";}
                        }else {Query_PreProcessResult(out5,"211");}
                        }else {iError=1;
                         Query_PreProcessResult(out5,"109");
                         if (!"".equals(iErrorMsg))
								iErrorMsg += ",";
							iErrorMsg += "Error Code 109!";}
                      }
                      else {
                        iError=1;
                        Query_PreProcessResult(out5,"101");
                        if (!"".equals(iErrorMsg))
							iErrorMsg += ",";
						iErrorMsg += "Error Code 101!";}
                //}else{Query_PreProcessResult(out5,"111");}
                }else{Query_PreProcessResult(out5,"111");}
    }

    public void ReqStatus_07(PrintWriter out7) throws SQLException, ClassNotFoundException, IOException, Exception{
      //else if (cReqStatus.equals("07")){
              //Check S2T IMSI
            csta=Check_Pair_IMSI(cTWNLDIMSI,cS2TIMSI);
                if (csta.equals("1")){
                   csta="";
               csta=Check_TWN_Msisdn_Status(cTWNLDIMSI,cS2TIMSI);
                //if (cTWNLDMSISDN.equals(csta)){
               if (!"0".equals(csta)){
                    csta="";
              bb=Validate_IMSIRange(cS2TIMSI);
              if (bb==true){
                        //Check CHT MSISDN
              bc=Validate_PartnerMSISDNRange(cTWNLDMSISDN);
              if (bc==true){
                 Get_GurrentS2TMSISDN();
                 if (!cS2TMSISDN.equals("")){
                   smsi=Query_PartnerMSISDNStatus();
                   if (!smsi.equals("0")){
                     sError="";
                     sError=Check_VLNStatus(cVLNCountry);
                     if (sError.equals("0")){
                     if (!cVLNCountry.equals("")){
                         sError="";
                          sError=Process_VLNString( cVLNCountry);}
                        if (sError.equals("0")){
                             sWSFStatus="V";
                             sWSFDStatus="V";
                //Update_VLNNumber();
                Process_SyncFile(sWSFStatus);
                Process_SyncFileDtl(sWSFDStatus);
                Process_ServiceOrder();
                Process_WorkSubcode_07();
                sSql="update S2T_TB_SERVICE_ORDER set STATUS='N' where "+
                        "SERVICE_ORDER_NBR='"+cServiceOrderNBR+"'";
                s2t.Update(sSql);
                logger.debug("update SERVICE_ORDER:"+sSql);
                Query_PreProcessResult(out7,"000");
                Update_VLNNumber();}
                      else {Query_PreProcessResult(out7,"402");}
                 } else if (sError.equals("330")){Query_PreProcessResult(out7,"330");}
                   else if (sError.equals("331")){Query_PreProcessResult(out7,"331");}
                 }else {Query_PreProcessResult(out7,"211");}}
              else {iError=1;
                         Query_PreProcessResult(out7,"108");
                         if (!"".equals(iErrorMsg))
								iErrorMsg += ",";
							iErrorMsg += "Error Code 108!";}
              }
                        else {iError=1;
                         Query_PreProcessResult(out7,"109");
                         if (!"".equals(iErrorMsg))
								iErrorMsg += ",";
							iErrorMsg += "Error Code 109!";}
                      }
                      else {
                        iError=1;
                        Query_PreProcessResult(out7,"101");
                        if (!"".equals(iErrorMsg))
							iErrorMsg += ",";
						iErrorMsg += "Error Code 101!";}
                }else{Query_PreProcessResult(out7,"211");}
                }else{Query_PreProcessResult(out7,"111");}
    }

    public void ReRunStatus_07(PrintWriter out7) throws SQLException, ClassNotFoundException, IOException, Exception{
      //else if (cReqStatus.equals("07")){
              logger.debug("ReRunStatus_07");
            csta=Check_Pair_IMSI(cTWNLDIMSI,cS2TIMSI);
                if (csta.equals("1")){
                   csta="";
               csta=Check_TWN_Msisdn_Status(cTWNLDIMSI,cS2TIMSI);
                if (!"0".equals(csta)){
                    csta="";
              bb=Validate_IMSIRange(cS2TIMSI);
              if (bb==true){
                        //Check CHT MSISDN
              bc=Validate_PartnerMSISDNRange(cTWNLDMSISDN);
              if (bc==true){
                 Get_GurrentS2TMSISDN();
                 if (!cS2TMSISDN.equals("")){
                   smsi=Query_PartnerMSISDNStatus();
                   if (!smsi.equals("0")){
                     sError="";
                     //sError=reCheck_VLNStatus(cVLNCountry);
                     //if (sError.equals("0")){
                     if (!cVLNCountry.equals("")){
                         sError="";
                          sError=Process_VLNString( cVLNCountry);}
                        if (sError.equals("0")){
                            iCut=Sparam.indexOf(",");
                        TempSparam=Sparam.substring(iCut+1, Sparam.length());
                               iCountA=Check_Tag(TempSparam);
                             if (iCountA>1){
                               Find_Old_ORDER_NBR();
                               Find_Old_step_no();
                             //if (!sOld_step_no.equals("0")){
                             sWSFStatus="V";
                             sWSFDStatus="V";
                //Update_VLNNumber();
                Process_SyncFile(sWSFStatus);
                Process_SyncFileDtl(sWSFDStatus);
                Process_ServiceOrder();
                reProcess_WorkSubcode_07(sOld_step_no);
                sSql="update S2T_TB_SERVICE_ORDER set STATUS='N' where "+
                        "SERVICE_ORDER_NBR='"+cServiceOrderNBR+"'";
                s2t.Update(sSql);
                logger.debug("update SERVICE_ORDER:"+sSql);
                Query_PreProcessResult(out7,"000");
                Update_VLNNumber();}//}
                          else{Query_PreProcessResult(out7,"201");}
                        }else {Query_PreProcessResult(out7,"402");}
                // } //else if (sError.equals("330")){Query_PreProcessResult(out7,"330");}
                   //else if (sError.equals("331")){Query_PreProcessResult(out7,"331");}
                 }else {Query_PreProcessResult(out7,"211");}}
              else {iError=1;
                         Query_PreProcessResult(out7,"108");
                         if (!"".equals(iErrorMsg))
								iErrorMsg += ",";
							iErrorMsg += "Error Code 108!";}
              }
                        else {iError=1;
                         Query_PreProcessResult(out7,"109");
                         if (!"".equals(iErrorMsg))
								iErrorMsg += ",";
							iErrorMsg += "Error Code 109!";}
                      }
                      else {
                        iError=1;
                        Query_PreProcessResult(out7,"101");
                        if (!"".equals(iErrorMsg))
							iErrorMsg += ",";
						iErrorMsg += "Error Code 101!";}
                }else{Query_PreProcessResult(out7,"211");}
                }else{Query_PreProcessResult(out7,"111");}
    }

    
    
    public void ReqStatus_17(PrintWriter out17) throws SQLException, IOException, ClassNotFoundException, Exception{
      //else if (cReqStatus.equals("17")){
    	logger.debug("ReqStatus_17");
            csta=Check_Pair_IMSI(cTWNLDIMSI,cS2TIMSI);
                if (csta.equals("1")){
                  csta="";
               csta=Check_TWN_Msisdn_Status(cTWNLDIMSI,cS2TIMSI);
                if (!"0".equals(csta)){
                    csta="";
                csta=Update_GPRSStatus();
                switch (Integer.parseInt(csta)){
                    case 0:
            {//Check S2T IMSI
                      bb=Validate_IMSIRange(cS2TIMSI);
                      if (bb==true){
                        //Check CHT MSISDN
                        bc=Validate_PartnerMSISDNRange(cTWNLDMSISDN);
                        if (bc==true){
                          Get_GurrentS2TMSISDN();
                          if (!cS2TMSISDN.equals("")){
                        	  ReqStatus_17_Act(out17);
                          }
                        else {iError=1;
                         Query_PreProcessResult(out17,"108");
                         if (!"".equals(iErrorMsg))
								iErrorMsg += ",";
							iErrorMsg += "Error Code 108!";}
                        }
                        else {iError=1;
                         Query_PreProcessResult(out17,"109");
                         if (!"".equals(iErrorMsg))
								iErrorMsg += ",";
							iErrorMsg += "Error Code 109!";}
                      }
                      else {
                        iError=1;
                        Query_PreProcessResult(out17,"101");
                        if (!"".equals(iErrorMsg))
							iErrorMsg += ",";
						iErrorMsg += "Error Code 101!";}}
                        break;
                    case 107:
                        iError=1;
                        Query_PreProcessResult(out17,"107");
                        if (!"".equals(iErrorMsg))
							iErrorMsg += ",";
						iErrorMsg += "Error Code 107!";
                        break;
                    default:
                        break;}
                }else{Query_PreProcessResult(out17,"211");}
                }else{Query_PreProcessResult(out17,"111");}
    }
    
    public void ReqStatus_17_Act(PrintWriter out17) throws SQLException, IOException, ClassNotFoundException, Exception{
    	logger.debug("ReqStatus_17_Act");

	    	Check_Type_Code_87_MAP_VALUE(cS2TMSISDN);
	        sWSFStatus="V";
	        sWSFDStatus="V";
	        Process_SyncFile(sWSFStatus);
	        Process_SyncFileDtl(sWSFDStatus);
	        Process_ServiceOrder();
	        //Process_WorkSubcode();
	        Process_WorkSubcode_05_17(cS2TIMSI,cTWNLDIMSI,cReqStatus,cTWNLDMSISDN);
	        sSql="update S2T_TB_SERVICE_ORDER set STATUS='N' where "+
	        		"SERVICE_ORDER_NBR='"+cServiceOrderNBR+"'";
	        s2t.Update(sSql);
	        logger.debug("update SERVICE_ORDER:"+sSql);
	        Query_PreProcessResult(out17,"000");
	        Query_GPRSStatus();
	        
    }

    public void ReRunStatus_17(PrintWriter out17) throws SQLException, IOException, ClassNotFoundException, Exception{
      //else if (cReqStatus.equals("17")){
        logger.debug("ReRunStatus_17");
            csta=Check_Pair_IMSI(cTWNLDIMSI,cS2TIMSI);
                if (csta.equals("1")){
                  csta="";
               csta=Check_TWN_Msisdn_Status(cTWNLDIMSI,cS2TIMSI);
                if (!"0".equals(csta)){
                    csta="";
                csta=Update_GPRSStatus();
                switch (Integer.parseInt(csta)){
                    case 0:
            {//Check S2T IMSI
                      bb=Validate_IMSIRange(cS2TIMSI);
                      if (bb==true){
                        //Check CHT MSISDN
                        bc=Validate_PartnerMSISDNRange(cTWNLDMSISDN);
                        if (bc==true){
                          Get_GurrentS2TMSISDN();
                          if (!cS2TMSISDN.equals("")){
                              iCut=Sparam.indexOf(",");
                            TempSparam=Sparam.substring(iCut+1, Sparam.length());
                            iCountA=Check_Tag(TempSparam);
                             if (iCountA>1){
                            	 ReRunStatus_17_Act(out17);
                               }//}
                          else{Query_PreProcessResult(out17,"201");}
                             }
                        else {iError=1;
                         Query_PreProcessResult(out17,"108");
                         if (!"".equals(iErrorMsg))
								iErrorMsg += ",";
							iErrorMsg += "Error Code 108!";}
                        }
                        else {iError=1;
                         Query_PreProcessResult(out17,"109");
                         if (!"".equals(iErrorMsg))
								iErrorMsg += ",";
							iErrorMsg += "Error Code 109!";}
                      }
                      else {
                        iError=1;
                        Query_PreProcessResult(out17,"101");
                        if (!"".equals(iErrorMsg))
							iErrorMsg += ",";
						iErrorMsg += "Error Code 101!";}}
                        break;
                    case 107:
                        iError=1;
                        Query_PreProcessResult(out17,"107");
                        if (!"".equals(iErrorMsg))
							iErrorMsg += ",";
						iErrorMsg += "Error Code 107!";
                        break;
                    default:
                        break;}
                }else{Query_PreProcessResult(out17,"211");}
                }else{Query_PreProcessResult(out17,"111");}
    }
    public void ReRunStatus_17_Act(PrintWriter out17)throws SQLException, IOException, ClassNotFoundException, Exception{
    	logger.debug("ReRunStatus_17_Act");

	    	Find_Old_ORDER_NBR();
	        Find_Old_step_no();
	        //if (!sOld_step_no.equals("0")){
	        Check_Type_Code_87_MAP_VALUE(cS2TMSISDN);
	        sWSFStatus="V";
	        sWSFDStatus="V";
	        Process_SyncFile(sWSFStatus);
	        Process_SyncFileDtl(sWSFDStatus);
	        Process_ServiceOrder();
	        //reProcess_WorkSubcode(sOld_step_no);
	        reProcess_WorkSubcode_05_17(cTWNLDIMSI,cS2TIMSI,sOld_step_no,cReqStatus,cTWNLDMSISDN);
	        sSql="update S2T_TB_SERVICE_ORDER set STATUS='N' where "+
	        		"SERVICE_ORDER_NBR='"+cServiceOrderNBR+"'";
	        s2t.Update(sSql);
	        logger.debug("update SERVICE_ORDER:"+sSql);
	        Query_PreProcessResult(out17,"000");
	        Query_GPRSStatus();
	        //Query_PreProcessResult(out17,"000");

    }
    
    public void ReqStatus_18(PrintWriter out18) throws SQLException, IOException, ClassNotFoundException, Exception {
    	
    	
    	if(!Query_AddonStatus(out18))
    		return;
    	
    	csta = Check_Pair_IMSI(cTWNLDIMSI, cS2TIMSI);
    	logger.info("ADDON START");
    	
    	if(csta.equals("1")) {
    		csta = "";	 
    		csta = Check_TWN_Msisdn_Status(cTWNLDIMSI, cS2TIMSI);
    	
    		if(!"0".equals(csta)) {
    			csta = Update_GPRSStatus();
    			
                switch(Integer.parseInt(csta)) {
                   case 0:
                   {
                	   bb = Validate_IMSIRange(cS2TIMSI);
                	     
                	   if(bb) {
                		   bc = Validate_PartnerMSISDNRange(cTWNLDMSISDN);
                		 
                		   if(bc) {
                			   Get_GurrentS2TMSISDN();
                			 
                			   if(!cS2TMSISDN.equals("")) {
                				   sSql = "";
                				   sWSFStatus = "V";
                                   sWSFDStatus = "V";
                                   
                                   for(Map<String,String> m: cAddonItem){
                                	   cAddonCode = m.get("AddonCode");
                                	   cAddonAction = m.get("AddonAction");
                       				
                                	   
                                	   Process_SyncFile(sWSFStatus);
                                       Process_SyncFileDtl(sWSFDStatus);
                                       Process_ServiceOrder();
                                       Process_WorkSubcode();
                      
                                       sSql="INSERT INTO ADDONSERVICE (REQUESTDATETIME," +
                                               "MNOSUBCODE,MNOIMSI,MNOMSISDN,S2TIMSI,S2TMSISDN," + 
                                    		   "ADDONCODE,ADDONACTION,SENDDATETIME,DONEDATETIME" +
                                               ") VALUES (SYSDATE" + ",'950','" + cTWNLDIMSI + "','" + cTWNLDMSISDN + "','"
                                    		   + cS2TIMSI + "','" + cS2TMSISDN + "','" + cAddonCode + "','" + cAddonAction 
                                    		   + "',null,null)";
                                       
                                       s2t.Inster(sSql);
                                       logger.debug("Adding Addon_Service:" + sSql);
                                   }
                				   Query_PreProcessResult(out18, "000");
                				   
                                  // Query_AddonStatus();	   
                                   
                			   } else {
                				   iError = 1;
                                   Query_PreProcessResult(out18,"108");
                                   if (!"".equals(iErrorMsg))
   									iErrorMsg += ",";
   								iErrorMsg += "Error Code 108!";
                			   }
                			   
                		   } else {
                			   iError = 1;
                               Query_PreProcessResult(out18,"109");
                               if (!"".equals(iErrorMsg))
									iErrorMsg += ",";
								iErrorMsg += "Error Code 109!";
                		   }
                	   } else {
                		   iError = 1;
                           Query_PreProcessResult(out18,"101");
                           if (!"".equals(iErrorMsg))
								iErrorMsg += ",";
							iErrorMsg += "Error Code 101!";
                	   }
                   }
                   break;
                   case 107:
                      iError=1;
                      Query_PreProcessResult(out18,"107");
                      if (!"".equals(iErrorMsg))
							iErrorMsg += ",";
						iErrorMsg += "Error Code 107!";
                   break;
                   default:
                   break;
                 }
    			 
    		 } else {
    			 Query_PreProcessResult(out18,"211");
    		 }
         } else {
        	 Query_PreProcessResult(out18,"111");
         }
    }
    
public void ReRunStatus_18(PrintWriter out18) throws SQLException, IOException, ClassNotFoundException, Exception {
	logger.debug("ReRunStatus_18");
	
    csta = Check_Pair_IMSI(cTWNLDIMSI,cS2TIMSI);
        
    if(csta.equals("1")) {
    	csta = "";
        csta = Check_TWN_Msisdn_Status(cTWNLDIMSI,cS2TIMSI);
        
        if(!"0".equals(csta)) {
        	csta = "";
            bb = Validate_IMSIRange(cS2TIMSI);
            
            if(bb) {
                //Check CHT MSISDN
            	bc = Validate_PartnerMSISDNRange(cTWNLDMSISDN);
            	
            	if(bc) {		
            		Get_GurrentS2TMSISDN();
         
            		if(!cS2TMSISDN.equals("")) {
            			smsi = Query_PartnerMSISDNStatus();
           
            			if(!smsi.equals("0")) {
            				//20141212 mark
            				/*sError = "";
            				if(!cVLNCountry.equals("")) {
            					sError = "";
            					sError = Process_VLNString(cVLNCountry);
            				}*/
            				sError="0";
            				
                
            				if(sError.equals("0")) {
            					iCut = Sparam.indexOf(",");
            					TempSparam = Sparam.substring(iCut + 1, Sparam.length());
            					iCountA = Check_Tag(TempSparam);
                     
            					if (iCountA > 1) {
            						Find_Old_ORDER_NBR();
            						Find_Old_step_no();
                     
            						sWSFStatus = "V";
            						sWSFDStatus = "V";
       
            						Process_SyncFile(sWSFStatus);
            						Process_SyncFileDtl(sWSFDStatus);
            						Process_ServiceOrder();
            						reProcess_WorkSubcode_07(sOld_step_no);
        
            						sSql="update S2T_TB_SERVICE_ORDER set STATUS='N' where "+
            								"SERVICE_ORDER_NBR='"+cServiceOrderNBR+"'";
        
            						s2t.Update(sSql);		
            						logger.debug("update SERVICE_ORDER:"+sSql);
            						Query_PreProcessResult(out18,"000");
        
            						//Update_VLNNumber();
            					} else {
            						Query_PreProcessResult(out18,"201");
            					}
            				} else {
            					Query_PreProcessResult(out18,"402");
            				}
            			} else {
            				Query_PreProcessResult(out18,"211");
            			}
            		} else {
            			iError = 1;
            			Query_PreProcessResult(out18,"108");
            			if (!"".equals(iErrorMsg))
							iErrorMsg += ",";
						iErrorMsg += "Error Code 108!";
            		}
            	} else {
            		iError = 1;
                    Query_PreProcessResult(out18,"109");
                    if (!"".equals(iErrorMsg))
						iErrorMsg += ",";
					iErrorMsg += "Error Code 109!";}
              	} else {
              		iError=1;
              		Query_PreProcessResult(out18,"101");
              		if (!"".equals(iErrorMsg))
						iErrorMsg += ",";
					iErrorMsg += "Error Code 101!";
              	}
        	} else {
        		Query_PreProcessResult(out18,"211");
        	}
        } else {
        	Query_PreProcessResult(out18,"111");
        }
      }

    public void ReqStatus_19(PrintWriter out19) throws SQLException, IOException, ClassNotFoundException, Exception{
    }

    public void ReqStatus_99(PrintWriter out99) throws SQLException, ClassNotFoundException, IOException, Exception{
      //else if (cReqStatus.equals("99")){
            //Check S2T IMSI
             csta=Check_Pair_IMSI(cTWNLDIMSI,cS2TIMSI);
                if (csta.equals("1")){
                   csta="";
               csta=Check_TWN_Msisdn_Status(cTWNLDIMSI,cS2TIMSI);
                if (!"0".equals(csta)){
                    csta="";
                      bb=Validate_IMSIRange(cS2TIMSI);
                      if (bb==true){
                        //Check CHT MSISDN
                        bc=Validate_PartnerMSISDNRange(cTWNLDMSISDN);
                        if (bc==true){
                          Get_GurrentS2TMSISDN();
                          if (!cS2TMSISDN.equals("")){
                         smsi=Query_PartnerMSISDNStatus();
                          if (!smsi.equals("0")){
                            sWSFStatus="V";
                             sWSFDStatus="V";
                            Process_SyncFile(sWSFStatus);
                            Process_SyncFileDtl(sWSFDStatus);
                            Process_ServiceOrder();
                            Process_WorkSubcode();
                       sSql="update S2T_TB_SERVICE_ORDER set STATUS='N' where "+
                               "SERVICE_ORDER_NBR='"+cServiceOrderNBR+"'";
                       s2t.Update(sSql);
                       logger.debug("update SERVICE_ORDER:"+sSql);
                            Query_PreProcessResult(out99,"000");}
                          else {Query_PreProcessResult(out99,"211");}
                          }
                        else {iError=1;
                         Query_PreProcessResult(out99,"108");
                         if (!"".equals(iErrorMsg))
								iErrorMsg += ",";
							iErrorMsg += "Error Code 108!";}
                        }
                        else {iError=1;
                         Query_PreProcessResult(out99,"109");
                         if (!"".equals(iErrorMsg))
								iErrorMsg += ",";
							iErrorMsg += "Error Code 109!";}
                      }
                      else {
                        iError=1;
                        Query_PreProcessResult(out99,"101");
                        if (!"".equals(iErrorMsg))
							iErrorMsg += ",";
						iErrorMsg += "Error Code 101!";}
                      }else{Query_PreProcessResult(out99,"211");}
                      }else{Query_PreProcessResult(out99,"111");}
    }

    public void ReRunStatus_99(PrintWriter out99) throws SQLException, ClassNotFoundException, IOException, Exception{
      //else if (cReqStatus.equals("99")){
            logger.debug("ReRunStatus_99");
             csta=Check_Pair_IMSI(cTWNLDIMSI,cS2TIMSI);
                if (csta.equals("1")){
                   csta="";
               csta=Check_TWN_Msisdn_Status(cTWNLDIMSI,cS2TIMSI);
                if (!"0".equals(csta)){
                    csta="";
                      bb=Validate_IMSIRange(cS2TIMSI);
                      if (bb==true){
                        //Check CHT MSISDN
                        bc=Validate_PartnerMSISDNRange(cTWNLDMSISDN);
                        if (bc==true){
                          Get_GurrentS2TMSISDN();
                          if (!cS2TMSISDN.equals("")){
                         smsi=Query_PartnerMSISDNStatus();
                          if (!smsi.equals("0")){
                              iCut=Sparam.indexOf(",");
                            TempSparam=Sparam.substring(iCut+1, Sparam.length());
                            iCountA=Check_Tag(TempSparam);
                             if (iCountA>1){
                               Find_Old_ORDER_NBR();
                               Find_Old_step_no();
                             //if (!sOld_step_no.equals("0")){
                            sWSFStatus="V";
                             sWSFDStatus="V";
                            Process_SyncFile(sWSFStatus);
                            Process_SyncFileDtl(sWSFDStatus);
                            Process_ServiceOrder();
                            reProcess_WorkSubcode(sOld_step_no);
                       sSql="update S2T_TB_SERVICE_ORDER set STATUS='N' where "+
                               "SERVICE_ORDER_NBR='"+cServiceOrderNBR+"'";
                       s2t.Update(sSql);
                       logger.debug("update SERVICE_ORDER:"+sSql);
                            Query_PreProcessResult(out99,"000");}//}
                          else{Query_PreProcessResult(out99,"201");}
                             }
                          else {Query_PreProcessResult(out99,"211");}
                          }
                        else {iError=1;
                         Query_PreProcessResult(out99,"108");
                         if (!"".equals(iErrorMsg))
								iErrorMsg += ",";
							iErrorMsg += "Error Code 108!";}
                        }
                        else {iError=1;
                         Query_PreProcessResult(out99,"109");
                         if (!"".equals(iErrorMsg))
								iErrorMsg += ",";
							iErrorMsg += "Error Code 109!";}
                      }
                      else {
                        iError=1;
                        Query_PreProcessResult(out99,"101");
                        if (!"".equals(iErrorMsg))
							iErrorMsg += ",";
						iErrorMsg += "Error Code 101!";}
                      }else{Query_PreProcessResult(out99,"211");}
                      }else{Query_PreProcessResult(out99,"111");}
    }

    public void RCode_000(PrintWriter outA) throws ClassNotFoundException, IOException, Exception{
    	
    	//20141216 add
    	boolean smsCho=true;
    	
    	String sMd = "";
    	String VARREALMSG = "";
	    String PACKAGE = "";
	    String PAYMENT = "";
                
	    if(cReqStatus.equals("18")) {
	    	//20150518 modify sms for multi-actionItem
	    	for(Map<String,String> m: cAddonItem){
	    	   VARREALMSG = "";
         	   cAddonCode = m.get("AddonCode");
         	   cAddonAction = m.get("AddonAction");
         	   
         	  if(cAddonCode.equals("SX001")) {
    	          PACKAGE = "香港上網包";
    		      PAYMENT = "NTD599";
    	       } else if(cAddonCode.equals("SX002")) {
    		      PACKAGE = "香港+大陸上網包";
    		      PAYMENT = "NTD999";
    	       }

    	       if(cAddonAction.equals("A")) {
    	          /*VARREALMSG = "《開通通知簡訊》 "; 
    	          VARREALMSG += PACKAGE + " -->";
    	          VARREALMSG += "addon_code=\'" + cAddonCode + "\'";
    	          VARREALMSG += " and addon_action=\'" + cAddonAction + "\'";*/
                  VARREALMSG += "親愛的中華電信用戶，您加選的環球卡" + PACKAGE; 
                  VARREALMSG += "月租服務已經開通囉，優惠期間每月只要" + PAYMENT;
    	          VARREALMSG += "提醒您手機須先將數據漫遊功能開啟，並請在網路的APN欄位輸入\"CMHK\"。環球卡感謝您！";
    	          
    	          if(smsCho)send_SMS(VARREALMSG,new SimpleDateFormat("yyyyMMddHHmm").format(new Date(new Date().getTime()+SMS_Delay_Time)));
                  else send_SMS1(VARREALMSG,new SimpleDateFormat("yyyyMMddHHmm").format(new Date(new Date().getTime()+SMS_Delay_Time)));
                  smsCho=!smsCho;
     	       } else {
                  /*VARREALMSG = "《退租通知簡訊》"; 
                  VARREALMSG += PACKAGE + " -->";
    	          VARREALMSG += "addon_code=\'" + cAddonCode + "\'";
    	          VARREALMSG += " and addon_action=\'" + cAddonAction + "\'";*/
    	          VARREALMSG += "《溫馨提醒》親愛的環球卡用戶，您所選的" + PACKAGE;
                  VARREALMSG += "服務已經依您選擇完成退租。日後如有需要歡迎隨時加選。環球卡感謝您！";
                  
                  if(smsCho)send_SMS(VARREALMSG,new SimpleDateFormat("yyyyMMddHHmm").format(new Date(new Date().getTime()+SMS_Delay_Time)));
                  else send_SMS1(VARREALMSG,new SimpleDateFormat("yyyyMMddHHmm").format(new Date(new Date().getTime()+SMS_Delay_Time)));
                  smsCho=!smsCho;
    	       }
    	       
    	      

	    	}
	       
	       
	       //send_SMS(VARREALMSG);

	       /*****************   Query starts here  ********************/
	       /* sSql = "update xxx set xxx ='" + cAddonAction + "' where xxx = ''";   ****/
	       /***********************************************************/
	       
	       //20140919 Add 
	      /* Query_GPRSStatus(); //query GPRS Status store in cGPRS
	       if(cGPRS.equals("0") && cAddonAction.equals("A")){//if GPRS is enable and action is "Add"
	    	   
	    	   logger.info("Running Req17 after Req18 to enable GPRS...");
	    	   
	    	   cGPRSStatus="1";		//Set Request GPRSStatus is 1
	    	   cReqStatus = "17";	//Set Request Status is 17
		       ReqStatus_17_Act(outA);	//
		       //ReRunStatus_17_Act(outA); //20140924 決定不呼叫
	       }else if(cGPRS.equals("1") && cAddonAction.equals("D")){//if GPRS is disable and action is "Delete"
	    	   logger.info("Running Req17 after Req18 to disable GPRS...");
	    	   
	    	   cGPRSStatus="0";		//Set Request GPRSStatus is 0
	    	   cReqStatus = "17";	//Set Request Status is 17
		       ReqStatus_17_Act(outA);	//
		       //ReRunStatus_17_Act(outA);//20140924 決定不呼叫
	       }*/
	       
	       /*cReqStatus = "17";
	       ReqStatus_17(outA);	
	       ReRunStatus_17(outA);*/
	       
	       //20140924 add
	       //20141215 cancel
	       /*logger.info("Run ContentProvider_extract");
	       Record rc=new Record(cS2TIMSI,cS2TMSISDN.substring(3,cS2TMSISDN.length()),cAddonAction);
	       
	       ContentProvider_extract ce=new ContentProvider_extract(getServletContext().getRealPath("/"),rc);
	       String response=ce.doProvider();
	       
	       logger.info("End ContentProvider_extract");
	       
	       if(!"200".equals(response)){
	    	   Send_AlertMail2("Addon Error : IMSI="+cS2TIMSI+" MSISDN:"+cS2TMSISDN.substring(3,cS2TMSISDN.length())+
	    			   " Action="+cAddonAction+" response="+response);
	       }*/
	       
        }

                
    	if(cReqStatus.equals("00")) {
    	   outA.println("<GPRS_Status>");
    	   outA.println(cGPRS);
    	   outA.println("</GPRS_Status>");
         
    	   VARREALMSG="";
    	   VARREALMSG+="親愛的中華客戶：您開通的「環球卡」香港副號為+"+cS2TMSISDN;
    	   VARREALMSG+="。在您抵達香港重新開機後，副號將顯示在手機上。請務必觀看環球卡撥號方式說明影片： http://goo.gl/sUSCHa。";
    	   VARREALMSG+="香港/大陸無限上網包上線囉，每月只要NTD599/999，歡迎加選，環球卡感謝您！如需諮詢請電客服{{customerService}}。";
    	   VARREALMSG=replaceCSPhone(VARREALMSG);
    	   
    	   //20141205 add
    	   //send_SMS(VARREALMSG);
    	   
    	   if(smsCho)send_SMS(VARREALMSG,new SimpleDateFormat("yyyyMMddHHmm").format(new Date(new Date().getTime()+SMS_Delay_Time)));
	    	else send_SMS1(VARREALMSG,new SimpleDateFormat("yyyyMMddHHmm").format(new Date(new Date().getTime()+SMS_Delay_Time)));
	    	smsCho=!smsCho;
    	   
    	   outA.println("<VLN>");
    	   sreturnXml=sreturnXml+"<GPRS_Status>"+cGPRS+"</GPRS_Status><VLN>";
        
    	   if(vln.size() > 0) {
    	      sAllVln = "";
    		  vln.firstElement();
          
    		  for(n = 0; n < vln.size(); n++) {
    		     sVln=vln.get(n);
    			 y = sVln.indexOf(",");
    			 sVln = sVln.substring(y+1,sVln.length());
    			 y = sVln.indexOf(",");
    			 cVLNc = sVln.substring(0, y);
    			 sMd = sVln.substring(sVln.length()-1,sVln.length());
    			 sVln=sVln.substring(y+1,sVln.length()-2);
    			 sAllVln=sAllVln+sVln+cVLNc+",";
    			
    			 if(sMd.equals("A")) {
    				 
    			    iVLN=0;
    				sSql="update availableVLN set Status='U',lastupdatetime=sysdate where "+
    						"VLNNUMBER='"+cVLNc+"'";
    				
    				logger.debug("availableVLN[A]:"+sSql);
    				s2t.Update(sSql);
             
    				//20141205 add
    				String cV="";
    				
    				
    				if (sVln.equals("CHN")) {
    					//20141205 mark
    					//iVLN=iVLN+1;
    					//VARREALMSG=VARREALMSG+"中國副號碼為 +"+cVLNc+"，";
    					cV="中國";
    			    } else if (sVln.equals("SGP")) {
    			    	//20141205 mark
    				   //iVLN=iVLN+1;
    				   //VARREALMSG=VARREALMSG+"新加坡副號碼為 +"+cVLNc+"，";
    			    	cV="新加坡";
    			    } else if (sVln.equals("SWE")) {
    			    	//20141205 mark
    			    	//iVLN=iVLN+1;
    				    //VARREALMSG=VARREALMSG+"瑞典副號碼為 +"+cVLNc+"，";
    			    	cV="瑞典";
    			    	
    			    	//20141216 add
    			    }else if (sVln.equals("THA")){
    					cV="泰國";
    				}else if (sVln.equals("IDN")){
    					cV="印尼";
    				}
    				
    				if(!"".equals(cV)){
    					VARREALMSG="親愛的中華客戶：您開通的「環球卡」"+cV+"副號為+"+cVLNc+"。"
    							+ "在您抵達"+cV+"重新開機後，副號將顯示在手機上。香港/大陸無限上網包上線囉，每月只要NTD599/999，歡迎加選，環球卡感謝您！如需諮詢請電客服{{customerService}}。";
    					VARREALMSG=replaceCSPhone(VARREALMSG);
    					//send_SMS(VARREALMSG);
    					if(smsCho)send_SMS(VARREALMSG,new SimpleDateFormat("yyyyMMddHHmm").format(new Date(new Date().getTime()+SMS_Delay_Time)));
    			    	else send_SMS1(VARREALMSG,new SimpleDateFormat("yyyyMMddHHmm").format(new Date(new Date().getTime()+SMS_Delay_Time)));
    			    	smsCho=!smsCho;
    				}
    			 }else if(sMd.equals("D")) {
    			    sSql="update availableVLN set Status='T',lastupdatetime=sysdate,s2tmsisdn=''"+
    					" where VLNNUMBER='"+cVLNc+"'";
    			    s2t.Update(sSql);
    		     }
    		  }
    		
    		  sAllVln=sAllVln.substring(0, sAllVln.length()-1);
    		  outA.println(sAllVln);
    		  sreturnXml=sreturnXml+sAllVln;
    	  }
    	   
    	   sSql="update availableMSISDN set status='U',lastupdatetime=sysdate,"+
              "partnermsisdn='"+cTWNLDMSISDN+"' Where mnosubcode='"+sMNOSubCode+"' And "+
              "s2tmsisdn='"+cS2TMSISDN+"'" ;
    	   
    	   logger.debug("ReqStatus[00-MSISDN]:"+sSql);
    	   s2t.Update(sSql);
         
         
	         //20141205 mark
	        /* if(iVLN > 0) {
	        	 VARREALMSG=VARREALMSG+"您現在就可以通知您當地的朋友打這個電話與您聯絡了！";
	         } else {
	        	 VARREALMSG=VARREALMSG+"您現在就可以通知您香港的朋友打這個電話與您聯絡了！";
	         }
	         
	         VARREALMSG=VARREALMSG+"副號碼會在您抵達該國時顯示在手機螢幕上，環球卡感謝您！";
	         send_SMS(VARREALMSG);*/
         
    	   VARREALMSG="提醒您：抵達當地國後請確認關閉飛航模式並重新開機，依照螢幕指示按下\"確定\"，環球卡將切換為當地門號。";
         
    	   //send_SMS1(VARREALMSG);
    	   if(smsCho)send_SMS(VARREALMSG,new SimpleDateFormat("yyyyMMddHHmm").format(new Date(new Date().getTime()+SMS_Delay_Time)));
	    	else send_SMS1(VARREALMSG,new SimpleDateFormat("yyyyMMddHHmm").format(new Date(new Date().getTime()+SMS_Delay_Time)));
	    	smsCho=!smsCho;
	    	
	       //20141205 mark
	         /*VARREALMSG="若您使用的是Symbian作業系統手機(例如NOKIA的N系列或E系列手機)搭配環球卡，"+
	                 "請主動更改SIM卡型態設定：功能表>工具>環球卡>初始設定>改為Type1。";
	         send_SMS(VARREALMSG);*/
	        
	         outA.println("</VLN>");
    	
	         if (cGPRS.equals("1")) {
	        	 VARREALMSG="親愛的中華客戶：您已開通環球卡數據服務，請先將手機數據漫遊功能開啟，在行動網路設定APN 為\"CMHK\"。"
	        	 		+ "除中國、香港、澳門、日本、韓國、印尼有每日收費上限外，其餘國家按實際用量收費，不提供吃到飽方案，請謹慎使用。另有香港/大陸月租上網吃到飽服務，歡迎加選。";
	        
	        	 //send_SMS(VARREALMSG);
	        	 if(smsCho)send_SMS(VARREALMSG,new SimpleDateFormat("yyyyMMddHHmm").format(new Date(new Date().getTime()+SMS_Delay_Time)));
	 	    	else send_SMS1(VARREALMSG,new SimpleDateFormat("yyyyMMddHHmm").format(new Date(new Date().getTime()+SMS_Delay_Time)));
	 	    	smsCho=!smsCho;
	        	 
	        	 VARREALMSG="親愛的環球卡用戶，本公司為協助您控管漫遊上網費用，漫遊上網服務每月費用超過NTD5,000時將自動關閉。"
	        	 		+ "如您不希望因超出使用金額而被關閉上網服務，請至www.sim2travel.com/chtm/5k.pdf "
	        	 		+ "下載約定書，或輸入以下內容「請將本人排除此規定」回覆至+886972900154，本公司將為您列入VIP名單，不會在超過額度時自動斷網。"
	        	 		+ "如需諮詢請電客服{{customerService}}。";
	        	 
	        	 VARREALMSG=replaceCSPhone(VARREALMSG);
	         
	         	 //send_SMS1(VARREALMSG);
	        	 if(smsCho)send_SMS(VARREALMSG,new SimpleDateFormat("yyyyMMddHHmm").format(new Date(new Date().getTime()+SMS_Delay_Time)));
	 	    	else send_SMS1(VARREALMSG,new SimpleDateFormat("yyyyMMddHHmm").format(new Date(new Date().getTime()+SMS_Delay_Time)));
	 	    	smsCho=!smsCho;
	         
	         }
    	 }
        
    	if ((cReqStatus.equals("97"))||(cReqStatus.equals("98"))) {
    		outA.println("<GPRS_Status>");
    		outA.println(cGPRS);
    		outA.println("</GPRS_Status>");
    		outA.println("<VLN>");
    		sreturnXml=sreturnXml+"<GPRS_Status>"+cGPRS+"</GPRS_Status><VLN>";
    		
    		if (vln.size()>0){
    			sAllVln="";
    			vln.firstElement();
    			
    			for (n=0; n<vln.size();n++){
    				sVln=vln.get(n);
    				y=sVln.indexOf(",");
    				sVln=sVln.substring(y+1,sVln.length());
    				y=sVln.indexOf(",");
    				cVLNc=sVln.substring(0, y);
    				sMd=sVln.substring(sVln.length()-1,sVln.length());
    				sVln=sVln.substring(y+1,sVln.length()-2);
    				sAllVln=sAllVln+sVln+cVLNc+",";
    			}
    			
    			sAllVln=sAllVln.substring(0, sAllVln.length()-1);
    			outA.println(sAllVln);
    			sreturnXml=sreturnXml+sAllVln;
    		}

	        outA.println("</VLN>");
	        outA.println("<Addon_Service>");
	        outA.println("</Addon_Service>");
	        sreturnXml=sreturnXml+"</VLN><Addon_Service></Addon_Service>";
        }
    	
    	if (cReqStatus.equals("99")){
    		sSql="update availableVLN set Status='Z',lastupdatetime=sysdate, "
    				+ "s2tmsisdn='' where s2tmsisdn='"+cS2TMSISDN+"'";
    		logger.info("ReqStatus[99-VLN]:"+sSql);
    		
    		s2t.Update(sSql);
    		
            sSql="update availablemsisdn set Status='Z',lastupdatetime="+
                    "sysdate,partnermsisdn=''"+
             " where s2tmsisdn='"+cS2TMSISDN+"'";
            
            logger.info("ReqStatus[99-VLN]:"+sSql);
            
            s2t.Update(sSql);
        }
    	
    	
        if (cReqStatus.equals("07")) {
        	
        	if (vln.size()>0){
        		vln.firstElement();
        		
        		for (n=0; n<vln.size();n++){
        			sVln=vln.get(n);
        			y=sVln.indexOf(",");
        			sVln=sVln.substring(y+1,sVln.length());
        			y=sVln.indexOf(",");
        			cVLNc=sVln.substring(0, y);
        			sMd=sVln.substring(sVln.length()-1,sVln.length());
        			sVln=sVln.substring(y+1,sVln.length()-2);
        			
        			if (sMd.equals("A")) {
        				sSql="update availableVLN set Status='U',lastupdatetime=sysdate where "
        						+ "VLNNUMBER='"+cVLNc+"'";
        				
        				s2t.Update(sSql);
        				
        				String cV="";
        				
        				if (sVln.equals("CHN")){
        					cV="中國";
        					//VARREALMSG=VARREALMSG+cV+"副號碼為 +"+cVLNc+"。";
        				}else if (sVln.equals("SGP")){
        					cV="新加坡";
        					//VARREALMSG=VARREALMSG+cV+"副號碼為 +"+cVLNc+"。";
        				}else if (sVln.equals("SWE")){
        					cV="瑞典";
        					//VARREALMSG=VARREALMSG+cV+"副號碼為 +"+cVLNc+"。";
        					
        					//20141216 add
        				}else if (sVln.equals("THA")){
        					cV="泰國";
        				}else if (sVln.equals("IDN")){
        					cV="印尼";
        				}
        				
        				if(!"".equals(cV)){
        					VARREALMSG="親愛的中華客戶：您開通的「環球卡」"+cV+"副號為+"+cVLNc+"。"
        							+ "在您抵達中國重新開機後，副號將顯示在手機上。香港/大陸無限上網包上線囉，每月只要NTD599/999，歡迎加選，環球卡感謝您！如需諮詢請電客服{{customerService}}。";
        					VARREALMSG=replaceCSPhone(VARREALMSG);
        					//send_SMS(VARREALMSG);
        					if(smsCho)send_SMS(VARREALMSG,new SimpleDateFormat("yyyyMMddHHmm").format(new Date(new Date().getTime()+SMS_Delay_Time)));
        			    	else send_SMS1(VARREALMSG,new SimpleDateFormat("yyyyMMddHHmm").format(new Date(new Date().getTime()+SMS_Delay_Time)));
        			    	smsCho=!smsCho;
        				}
        				
        			}else if (sMd.equals("D")) {
        				sSql="update availableVLN set Status='Z',lastupdatetime=sysdate,s2tmsisdn='' "
        						+ "where VLNNUMBER='"+cVLNc+"'";
        				
        				s2t.Update(sSql);
        			}
        		}
        	}
        	
	        //20141205 mark
	        /*if (VARREALMSG.length()>0){
	        VARREALMSG="親愛的環球卡客戶：您新開通的 "+VARREALMSG+"您現在就可以通知您當地的朋友"+
	                "打這個電話與您聯絡了！副號碼會在您抵達當地重新開機後，副號將顯示在手機上。香港/大陸無限上網包上線囉，每月只要NTD599/999，歡迎加選，環球卡感謝您！";
	         send_SMS(VARREALMSG);
	        }*/
        	
        	TempRt=null;
        	
	        sSql="SELECT b.countryinit||a.vln as ab FROM vlnnumber a, " +
	                "COUNTRYINITIAL b WHERE a.vplmnid=b.vplmnid "
	                + "AND a.serviceid = (SELECT MAX(Serviceid) FROM imsi WHERE homeimsi = '"+cTWNLDIMSI+"')";
	        
	        TempRt=s2t.Query(sSql);
	        
	        while (TempRt.next()){
	        	sVln=TempRt.getString("ab");
	        	sAllVln=sAllVln+sVln+",";
	        }
	        
	        if (sAllVln.length()>2){
	        	sAllVln=sAllVln.substring(0, sAllVln.length()-1);
	        }else {
	        	sAllVln="";
	        }
	        
	        outA.println("<VLN>");
	        outA.println(sAllVln);
	        outA.println("</VLN>");
	        
	        sreturnXml=sreturnXml+"<VLN>"+sAllVln+"</VLN>";
	     
        }
        
        
        if (cReqStatus.equals("05")) {
        	sSql="update availablemsisdn set partnermsisdn='"+cTWNLDMSISDN+
        		"',lastupdatetime=sysdate where partnermsisdn='"+cOldTWNLDMSISDN+"'";
        	
        	logger.debug("ReqStatus[05]:"+sSql);
        	s2t.Update(sSql);
        }
        
        if (cReqStatus.equals("17")){
        	if (cGPRS.equals("1")){
		        /*VARREALMSG="出國使用環球卡數據服務功能時，須先將數據漫遊功能開啟，並且在網路的APN"+
		                "欄位輸入[peoples.net[。漫遊數據服務依照用量計價，並無收費上限與吃到飽方案，"+
		                "請斟酌使用。";*/

        		VARREALMSG="親愛的中華客戶：您已開通環球卡數據服務，請先將手機數據漫遊功能開啟，在行動網路設定APN 為\"CMHK\"。"
        				+ "除中國、香港、澳門、日本、韓國、印尼有每日收費上限外，其餘國家按實際用量收費，不提供吃到飽方案，請謹慎使用。"
        				+ "另有香港/大陸月租上網吃到飽服務，歡迎加選。";
        		
        		//send_SMS(VARREALMSG);
        		if(smsCho)send_SMS(VARREALMSG,new SimpleDateFormat("yyyyMMddHHmm").format(new Date(new Date().getTime()+SMS_Delay_Time)));
    	    	else send_SMS1(VARREALMSG,new SimpleDateFormat("yyyyMMddHHmm").format(new Date(new Date().getTime()+SMS_Delay_Time)));
    	    	smsCho=!smsCho;
		        
		        
        		VARREALMSG="親愛的環球卡用戶，本公司為協助您控管漫遊上網費用，漫遊上網服務每月費用達約NTD5,000時將自動關閉。"
        	  		+ "如您不希望因超出使用金額而被關閉上網功能，影響您的商務進行，請至www.sim2travel.com/chtm/5k.pdf"
        	  		+ "下載約定書，或輸入以下內容回覆本簡訊「請將本人排除此規定」，本公司將為您列入VIP名單，不會在超過額度時自動斷網。"
        	  		+ "如需諮詢請電客服{{customerService}}。";
        		
        		VARREALMSG=replaceCSPhone(VARREALMSG);
        		
        		//send_SMS1(VARREALMSG);
        		if(smsCho)send_SMS(VARREALMSG,new SimpleDateFormat("yyyyMMddHHmm").format(new Date(new Date().getTime()+SMS_Delay_Time)));
    	    	else send_SMS1(VARREALMSG,new SimpleDateFormat("yyyyMMddHHmm").format(new Date(new Date().getTime()+SMS_Delay_Time)));
    	    	smsCho=!smsCho;
        	}
        }
        //20150625 add
        if (cReqStatus.equals("99")){
        	send_OTA_SMS();
        }
    }
    
    
    
    private String replaceCSPhone(String msg){
    	String cphone=null;
    	  
    	  if(conn2!=null){
    		  try {
  				cphone=queryCustmerServicePhone(cS2TIMSI);
  			} catch (SQLException e) {
  				e.printStackTrace();
  			}
    	  }

    	  if(cphone==null)cphone="";
    	  msg=msg.replace("{{customerService}}", cphone);
    	  
    	  return msg;
    }
    //Query Customer Service Phone number
    private String queryCustmerServicePhone(String imsi) throws SQLException{
		String cphone=null;
		String VLN=queryVLR(imsi);
		
		if(VLN!=null && !"".equals(VLN)){
			Map<String,String> tadigMap = queryTADIG();
			
			if(tadigMap.size()>0){
				String tadig=null;
				for(int i=VLN.length();i>0;i--){
					tadig=tadigMap.get(VLN.substring(0,i));
					if(tadig!=null &&!"".equals(tadig)){
						break;
					}
				}
				if(tadig!=null &&!"".equals(tadig)){
					String mccmnc=queryMccmnc(tadig);
					if(mccmnc!=null &&!"".equals(mccmnc)){
						cphone=queryCustomerServicePhone(mccmnc);
					}
				}
			}
		}
		if(cphone==null || "".equals(cphone))
			cphone=queryCustomerServicePhone("466000");
		
		return cphone;
	}
    public String queryVLR(String imsi) throws SQLException{
		String VLN=null;
		
		sSql="SELECT VLR_NUMBER FROM UTCN.BASICPROFILE WHERE IMSI='"+imsi+"'";
		logger.debug("Execute sql: "+sSql);
		ResultSet rs=conn2.createStatement().executeQuery(sSql);
		
		while(rs.next()){
			VLN=rs.getString("VLR_NUMBER");
		}
		rs.close();
		
		return VLN;
	}
	
	public Map<String,String> queryTADIG() throws SQLException{
		Map<String,String> map = new HashMap<String,String>();
		
		sSql=" SELECT B.REALMNAME TADIG, A.CHARGEAREACODE VLR FROM CHARGEAREACONFIG A, REALM B "
				+ "WHERE A.AREAREFERENCE=B.AREAREFERENCE ";
		logger.debug("Execute sql: "+sSql);
		ResultSet rs=conn2.createStatement().executeQuery(sSql);
		
		while(rs.next()){
			map.put(rs.getString("VLR"), rs.getString("TADIG"));
		}
		rs.close();
		return map;
	}
	public String queryMccmnc(String tadig) throws SQLException{
		String mccmnc=null;
				
		sSql=" SELECT MCCMNC FROM HUR_MCCMNC WHERE TADIG='"+tadig+"'";
		logger.debug("Execute sql: "+sSql);
		ResultSet rs=s2t.Query(sSql);

		while(rs.next()){
			mccmnc=rs.getString("MCCMNC");
		}		
				
		rs.close();

		return mccmnc;		
	}
	
	public String queryCustomerServicePhone(String mccmnc) throws SQLException{
		String cPhone=null;
		String subcode=mccmnc.substring(0,3);
		sSql=" SELECT PHONE FROM HUR_CUSTOMER_SERVICE_PHONE A WHERE A.CODE ='"+subcode+"'";
		logger.debug("Execute sql: "+sSql);
		ResultSet rs=s2t.Query(sSql);
		
		while(rs.next()){
			cPhone=rs.getString("PHONE");
		}		
		
		rs.close();
		return cPhone;		
	}


    
    

    public void S501(PrintWriter outA,String sRCode){
        logger.debug("sRCode:"+sRCode);
        if ((!sRCode.equals("500"))||(!sRCode.equals("501"))){
			try {
			    sWSFStatus = "O";
			sWSFDStatus = "I";
			    Process_SyncFile(sWSFStatus);
			    Process_SyncFileDtl(sWSFDStatus);
			} catch (SQLException ex) {
			    sErrorSQL+=sSql;
			    ErrorHandle(ex);
			} catch (Exception ex) {
				ErrorHandle(ex);
			}
        }
          if (cReqStatus.equals("00")) {
         outA.println("<GPRS_Status>");
         outA.println(cGPRS);
         outA.println("</GPRS_Status>");
        outA.println("<VLN>");
        outA.println("</VLN>");
        sreturnXml=sreturnXml+"<GPRS_Status>"+cGPRS+"</GPRS_Status><VLN></VLN>";
        }
        if ((cReqStatus.equals("97"))||(cReqStatus.equals("98"))) {
         outA.println("<GPRS_Status>");
         outA.println(cGPRS);
         outA.println("</GPRS_Status>");
        outA.println("<VLN>");
        outA.println("</VLN>");
        outA.println("<Addon_Service>");
        outA.println("</Addon_Service>");
        sreturnXml=sreturnXml+"<GPRS_Status>"+cGPRS+"</GPRS_Status><VLN></VLN>"+
                "<Addon_Service></Addon_Service>";
        }
        if (cReqStatus.equals("07")) {
        outA.println("<VLN>");
        outA.println("</VLN>");
         sreturnXml=sreturnXml+"<VLN></VLN>";
        }
    }

    public String Query_PartnerMSISDNStatus() throws SQLException, IOException{
    String imsisdn="";
    Temprs=null;
    sSql="select count(*) as ab from availableMSISDN where partnermsisdn='"+
            cTWNLDMSISDN+"' and status='U'";
logger.debug("Query_PartnerMSISDNStatus:"+sSql);
    Temprs=s2t.Query(sSql);
    while (Temprs.next()){
       imsisdn=Temprs.getString("ab");
     }
    return imsisdn;
}

    public void Process_SyncFile(String sSFStatus) throws SQLException, Exception{
                   //格式為YYYYMMDDXXX
        sDATE=s2t.Date_Format();
           c910SEQ=sDATE+sCount;
           cFileName="S2TCI"+c910SEQ+".950";
            cFileID="";
            Temprs=null;
            sSql="select S2T_SQ_FILE_CNTRL.NEXTVAL as ab from dual";
            Temprs=s2t.Query(sSql);
            while (Temprs.next()){
                cFileID=Temprs.getString("ab");
            }
            sSql="INSERT INTO S2T_TB_TYPEB_WO_SYNC_FILE (FILE_ID,"+
                    "FILE_NAME,FILE_SEND_DATE,FILE_SEQ,CMCC_BRANCH_ID,"+
                    "FILE_CREATE_DATE,STATUS) VALUES ("+cFileID+",'"+ cFileName+
                    "','"+ dReqDate.substring(0, 8)+
                    "','"+ c910SEQ.substring(8,11)+"','950',sysdate,'"+sSFStatus+"')";
            logger.debug("Process_SyncFile:"+sSql);
             s2t.Inster(sSql);
    }

     public void Process_SyncFileDtl(String sSFDStatus) throws SQLException, IOException{
        int iv,ix=0;
        String sVl="",sC,sH;
        cWorkOrderNBR="";
            Temprs=s2t.Query("select S2T_SQ_WORK_ORDER.nextval as ab from dual");
            while (Temprs.next()){
                cWorkOrderNBR=Temprs.getString("ab");
            }
        Temprs=null;
            cServiceOrderNBR="";
            Temprs=s2t.Query("select S2T_SQ_SERVICE_ORDER.nextval as ab from dual");
            while (Temprs.next()){
                cServiceOrderNBR=Temprs.getString("ab");
            }
      sSql="INSERT INTO S2T_TB_TYPB_WO_SYNC_FILE_DTL (WORK_ORDER_NBR,"+
           "WORK_TYPE, FILE_ID, SEQ_NO, CMCC_OPERATIONDATE, ORIGINAL_CMCC_IMSI,"+
           "ORIGINAL_CMCC_MSISDN, S2T_IMSI, S2T_MSISDN, FORWARD_TO_HOME_NO, "+
           "FORWARD_TO_S2T_NO_1, IMSI_FLAG, STATUS, SERVICE_ORDER_NBR, SUBSCR_ID)"+
           " VALUES ("+cWorkOrderNBR+",'"+ cReqStatus+"',"+ cFileID+",'"+
           c910SEQ+"',to_date('"+Sdate+"','MM/dd/yyyy HH24:mi:ss'),'"+
           cTWNLDIMSI+"','+"+ cTWNLDMSISDN+"','"+ cS2TIMSI+"','"+ cS2TMSISDN+"','+"+
           cTWNLDMSISDN+"','"+cTWNLDMSISDN+"', '2', '"+sSFDStatus+"','"+
           cServiceOrderNBR+"','"+cTicketNumber+"')";
      logger.debug("Process_SyncFileDtl:"+sSql);
      s2t.Inster(sSql);
      if (vln.size()>0){
         vln.firstElement();
          for (iv=0; iv<vln.size();iv++){
            sVl=vln.get(iv);
             ix=sVl.indexOf(",");
             sC=sVl.substring(0, ix);
             sVl=sVl.substring(ix+1, sVl.length());
             ix=sVl.indexOf(",");
             sH=sVl.substring(0, ix);
             sSql="update S2T_TB_TYPB_WO_SYNC_FILE_DTL set VLN_"+sC+"='"+sH+
             "' where WORK_ORDER_NBR="+cWorkOrderNBR+" and SERVICE_ORDER_NBR='"+
             cServiceOrderNBR+"'";
             s2t.Update(sSql); }}
    }

	public void Process_ServiceOrder() throws SQLException, IOException{
	   sSql = "INSERT INTO S2T_TB_SERVICE_ORDER (SERVICE_ORDER_NBR, "+
	          "WORK_TYPE, S2T_MSISDN, SOURCE_TYPE, SOURCE_ID, STATUS, "+
	          "CREATE_DATE) "+ "VALUES ('"+cServiceOrderNBR+"','"+ cReqStatus+"','"+ cS2TMSISDN+
	          "',"+"'B_TYPE',"+ cWorkOrderNBR+", '', sysdate)";
	                
	   logger.info("Process_ServiceOrder[1]:" + sSql);
	   Temprs = s2t.Query(sSql);
	   s2t.Inster(sSql);
	   Temprs = null;
	                
	   sSql="Select MNO_NAME from S2T_TB_MNO_COMPANY "+
	        "Where MNO_SUB_CODE='"+sMNOSubCode+"'";
	     
	   logger.debug("Process_ServiceOrder[2]:"+sSql);
	   Temprs = s2t.Query(sSql);
	               
	   while(Temprs.next()) {
	      sMNOName = Temprs.getString("MNO_NAME");
	   }
	}

     public void Process_ServiceOrderItem() throws SQLException, IOException{
          sSql="Insert into S2T_TB_SERVICE_ORDER_ITEM (SERVICE_ORDER_NBR,"+
               "STEP_NO, SUB_CODE, IDENTIFIER, STATUS, SEND_DATE) "+
               "Values ("+cServiceOrderNBR+","+ sStepNo+",'"+ sSubCode+"',"+
               " S2T_SQ_SERVICE_ORDER_ITEM.nextval, 'N', sysdate)";
          logger.debug("Process_ServiceOrderItem:"+sSql);
                s2t.Inster(sSql);
     }

      public void Process_ServiceOrderItemDtl() throws SQLException, IOException{
                     sSql="Insert into S2T_TB_SERVICE_ORDER_ITEM_DTL "+
                        "(SERVICE_ORDER_NBR, STEP_NO, TYPE_CODE, DATA_TYPE, VALUE) "+
                        "VALUES ("+cServiceOrderNBR+","+ sStepNo+","+ sTypeCode+","+
                        sDataType+",'"+ sValue+"')";
                     logger.debug("Process_ServiceOrderItemDtl:"+sSql);
                s2t.Inster(sSql);
     }

     public void Process_DefValue() throws SQLException, IOException{
         ResultSet TeRt=null;
                sSql="Select TYPE_CODE, DATA_TYPE, DEF_VALUE "+
                           "From S2T_TB_SUBCODE_TYPECODE "+
                           "Where subcode='"+sSubCode +
                            "' And work_type='"+cReqStatus +
                            "' And MNO_NAME='"+sMNOName +
                            "' And DEF_VALUE is not null";
                logger.debug("Process_DefValue:"+sSql);
                TeRt=s2t.Query(sSql);
                while (TeRt.next()){
                sTypeCode=TeRt.getString("TYPE_CODE");
                sDataType=TeRt.getString("DATA_TYPE");
                sValue=TeRt.getString("DEF_VALUE");
                Process_ServiceOrderItemDtl();
                }
    }

     public void Process_MapValue() throws SQLException, IOException{
         ResultSet TeRtA=null;
                sSql="Select TYPE_CODE, DATA_TYPE, MAP_VALUE "+
                           "From S2T_TB_SUBCODE_TYPECODE "+
                           "Where subcode='"+sSubCode +
                            "' And work_type='"+cReqStatus +
                            "' And MNO_NAME='"+sMNOName +
                            "' And MAP_VALUE is not null";
                logger.debug("Process_MapValue:"+sSql);
                TeRtA=s2t.Query(sSql);
                while (TeRtA.next()){
                sTypeCode=TeRtA.getString("TYPE_CODE");
                sDataType=TeRtA.getString("DATA_TYPE");
                sMap="";
                sMap=TeRtA.getString("MAP_VALUE");
               if ("S2T_MSISDN".equals(TeRtA.getString("MAP_VALUE"))){sValue=cS2TMSISDN;}
               else if ("S2T_IMSI".equals(TeRtA.getString("MAP_VALUE"))){sValue=cS2TIMSI;}
               else if ("TWNLD_MSISDN".equals(TeRtA.getString("MAP_VALUE"))){sValue=cTWNLDMSISDN;}
               else if ("TWNLD_IMSI".equals(TeRtA.getString("MAP_VALUE"))){sValue= cTWNLDIMSI;}
               else if ("S2T_MSISDN_OLD".equals(TeRtA.getString("MAP_VALUE"))){sValue=cMSISDNOLD;}
               else if ("M_205_OT".equals(TeRtA.getString("MAP_VALUE"))){sValue=cM205OT;}
               else if ("M_VLN".equals(TeRtA.getString("MAP_VALUE"))){sValue=cMVLN;}
               else if ("M_GPRS".equals(TeRtA.getString("MAP_VALUE"))){sValue=cGPRSStatus;}
               //20150522 add
               else if ("cGPRS".equals(TeRtA.getString("MAP_VALUE"))){sValue=cGPRS;}
               
               else if ("M_CTYPE".equals(TeRtA.getString("MAP_VALUE"))){sValue=sM_CTYPE;}
               else if ("FMTH".equals(TeRtA.getString("MAP_VALUE"))){sValue=sFMTH;}
               else if ("FMTH_A".equals(TeRtA.getString("MAP_VALUE"))){sValue=sFMTHa;}
               else if ("SFMTH".equals(TeRtA.getString("MAP_VALUE"))){sValue=sSFMTH;}
               else if ("SFMTH_A".equals(TeRtA.getString("MAP_VALUE"))){sValue=sSFMTHa;}
               else if ("FORWARD_TO_HOME_NO".equals(TeRtA.getString("MAP_VALUE"))){sValue=sFORWARD_TO_HOME_NO;}
               else if ("S_FORWARD_TO_HOME_NO".equals(TeRtA.getString("MAP_VALUE"))){sValue=sS_FORWARD_TO_HOME_NO;}
               logger.debug("MAP_VALUE:"+sMap+"="+sValue+",StepNo:"+sStepNo+",DataType:"+sDataType+",TypeCode:"+sTypeCode);
               if (sTypeCode.equals("1909")&&(sValue.equals("0"))){
                   logger.debug("Follow Me To Home did not work");
               }
               else if (sTypeCode.equals("1911")&&(sValue.equals(""))){
                   logger.debug("Follow Me To Home did not Active");
               }
               else if (sTypeCode.equals("1942")&&(sValue.equals("0"))){
                   logger.debug("SMS Follow Me To Home did not work");
               }
               else if (sTypeCode.equals("1944")&&(sValue.equals(""))){
                   logger.debug("SMS Follow Me To Home did not Active");
               }
               else{
                  Process_ServiceOrderItemDtl();
               }
               }
    }

     public void Process_WorkSubcode() throws SQLException, IOException{
                Temprs=null;
                sSql="Select subcode, step_no from S2T_TB_WORK_SUBCODE Where MNO_NAME='"+
                      sMNOName+"' And work_type='"+cReqStatus+"' Order by step_no";
                logger.info("Process_WorkSubcode:"+sSql);
                Temprs=s2t.Query(sSql);
                while (Temprs.next()){
                sSubCode=Temprs.getString("subcode");
                sStepNo=Temprs.getString("step_no");
                if (sSubCode.equals("205")){
                 if (vln.size()>0){
                 vln.firstElement();
                 for (n=0; n<vln.size();n++){
                 sVln=vln.get(n);
                 y=sVln.indexOf(",");
                 sVln=sVln.substring(y+1,sVln.length()-1);
                 y=sVln.indexOf(",");
                 cVLNc=sVln.substring(0, y);
                 sVln=sVln.substring(y+1,sVln.length()-1);
                 logger.info("Country:"+sVln+",StepNo:"+sStepNo+",VLN:"+cVLNc);
                 if ((sVln.equals("CHN")) && (sStepNo.equals("2"))){
                   cMVLN=cVLNc;
                   Process_ServiceOrderItem();
                   Process_DefValue();
                   Process_MapValue();}
                 else if ((sVln.equals("SGP")) && (sStepNo.equals("3"))){
                   cMVLN=cVLNc;
                   Process_ServiceOrderItem();
                   Process_DefValue();
                   Process_MapValue();}
                 else if ((sVln.equals("KHM")) && (sStepNo.equals("4"))){
                   cMVLN=cVLNc;
                   Process_ServiceOrderItem();
                   Process_DefValue();
                   Process_MapValue();}
                 else if ((sVln.equals("THA")) && (sStepNo.equals("5"))){
                   cMVLN=cVLNc;
                   Process_ServiceOrderItem();
                   Process_DefValue();
                   Process_MapValue();}
                 else if ((sVln.equals("IDN")) && (sStepNo.equals("6"))){
                   cMVLN=cVLNc;
                   Process_ServiceOrderItem();
                   Process_DefValue();
                   Process_MapValue();}
                 }
                 }
                }
                else if(!sSubCode.equals("205")) {
                   Process_ServiceOrderItem();
                   Process_DefValue();
                   Process_MapValue();}
    }
                sSql="update PROVLOG " +
                "set STEP='"+sStepNo+"' "+
                " where LOGID="+sCMHKLOGID;
                s2t.Update(sSql);
    }

     public void reProcess_WorkSubcode(String sStep_No) throws SQLException, IOException{
                       Temprs=null;
                       if (sStep_No.equals("0")){sStep_No="1";}
    sSql="Select subcode, step_no from S2T_TB_WORK_SUBCODE Where MNO_NAME='"+
    sMNOName+"' And work_type='"+cReqStatus+"' and step_no>="+sStep_No +
            " Order by step_no";
                logger.info("reProcess_WorkSubcode:"+sSql);
                Temprs=s2t.Query(sSql);
                while (Temprs.next()){
                sSubCode=Temprs.getString("subcode");
                sStepNo=Temprs.getString("step_no");
                if (sSubCode.equals("205")){
                 if (vln.size()>0){
                 vln.firstElement();
                 for (n=0; n<vln.size();n++){
                 sVln=vln.get(n);
                 y=sVln.indexOf(",");
                 sVln=sVln.substring(y+1,sVln.length()-1);
                 y=sVln.indexOf(",");
                 cVLNc=sVln.substring(0, y);
                 sVln=sVln.substring(y+1,sVln.length()-1);
                 logger.info("Country:"+sVln+",StepNo:"+sStepNo+",VLN:"+cVLNc);
                 if ((sVln.equals("CHN")) && (sStepNo.equals("2"))){
                   cMVLN=cVLNc;
                   Process_ServiceOrderItem();
                   Process_DefValue();
                   Process_MapValue();}
                 else if ((sVln.equals("SGP")) && (sStepNo.equals("3"))){
                   cMVLN=cVLNc;
                   Process_ServiceOrderItem();
                   Process_DefValue();
                   Process_MapValue();}
                 else if ((sVln.equals("KHM")) && (sStepNo.equals("4"))){
                   cMVLN=cVLNc;
                   Process_ServiceOrderItem();
                   Process_DefValue();
                   Process_MapValue();}
                 else if ((sVln.equals("THA")) && (sStepNo.equals("5"))){
                   cMVLN=cVLNc;
                   Process_ServiceOrderItem();
                   Process_DefValue();
                   Process_MapValue();}
                 else if ((sVln.equals("IDN")) && (sStepNo.equals("6"))){
                   cMVLN=cVLNc;
                   Process_ServiceOrderItem();
                   Process_DefValue();
                   Process_MapValue();}
                 }
                 }
                }
                else if(!sSubCode.equals("205")) {
                   Process_ServiceOrderItem();
                   Process_DefValue();
                   Process_MapValue();}
    }
                sSql="update PROVLOG " +
                "set STEP='"+sStepNo+"' "+
                " where LOGID="+sCMHKLOGID;
                s2t.Update(sSql);
     }

     public void Process_WorkSubcode_07() throws SQLException, IOException{
    Temprs=null;
                String cMd="";
              sSql="Select subcode, step_no from S2T_TB_WORK_SUBCODE Where MNO_NAME='"+
                      sMNOName+"' And work_type='"+cReqStatus+"' Order by step_no";
                logger.debug("Process_WorkSubcode_07:"+sSql);
                Temprs=s2t.Query(sSql);
                while (Temprs.next()){
                sSubCode=Temprs.getString("subcode");
                sStepNo=Temprs.getString("step_no");
                if (sSubCode.equals("205")){
                 if (vln.size()>0){
                 vln.firstElement();
                 for (n=0; n<vln.size();n++){
                 sVln=vln.get(n);
                 y=sVln.indexOf(",");
                 sVln=sVln.substring(y+1,sVln.length());
                 y=sVln.indexOf(",");
                 cVLNc=sVln.substring(0, y);
                 cMd=sVln.substring(sVln.length()-1,sVln.length());
                 sVln=sVln.substring(y+1,sVln.length()-2);
                 if (cMd.equals("A")){cM205OT="1";}
                 else if (cMd.equals("D")){cM205OT="3";
                 }
                 if ((sVln.equals("CHN")) && (sStepNo.equals("1"))){
                   cMVLN=cVLNc;
                   Process_ServiceOrderItem();
                   Process_DefValue();
                   Process_MapValue();}
                 else if ((sVln.equals("SGP")) && (sStepNo.equals("2"))){
                   cMVLN=cVLNc;
                   Process_ServiceOrderItem();
                   Process_DefValue();
                   Process_MapValue();}
                 else if ((sVln.equals("KHM")) && (sStepNo.equals("3"))){
                   cMVLN=cVLNc;
                   Process_ServiceOrderItem();
                   Process_DefValue();
                   Process_MapValue();}
                 else if ((sVln.equals("THA")) && (sStepNo.equals("4"))){
                   cMVLN=cVLNc;
                   Process_ServiceOrderItem();
                   Process_DefValue();
                   Process_MapValue();}
                else if ((sVln.equals("IDN")) && (sStepNo.equals("5"))){
                   cMVLN=cVLNc;
                   Process_ServiceOrderItem();
                   Process_DefValue();
                   Process_MapValue();}
                 }
                 }
                }
    }
                sSql="update PROVLOG " +
                "set STEP='"+sStepNo+"' "+
                " where LOGID="+sCMHKLOGID;
                s2t.Update(sSql);
    }

     public void reProcess_WorkSubcode_07(String sStep_No) throws SQLException, IOException{
    Temprs=null;
                String cMd="";
                if (sStep_No.equals("0")){sStep_No="1";}
              sSql="Select subcode, step_no from S2T_TB_WORK_SUBCODE Where MNO_NAME='"+
                      sMNOName+"' And work_type='"+cReqStatus+"' and step_no>="+sStep_No +
            " Order by step_no";
                logger.debug("Process_WorkSubcode_07:"+sSql);
                Temprs=s2t.Query(sSql);
                while (Temprs.next()){
                sSubCode=Temprs.getString("subcode");
                sStepNo=Temprs.getString("step_no");
                if (sSubCode.equals("205")){
                 if (vln.size()>0){
                 vln.firstElement();
                 for (n=0; n<vln.size();n++){
                 sVln=vln.get(n);
                 y=sVln.indexOf(",");
                 sVln=sVln.substring(y+1,sVln.length());
                 y=sVln.indexOf(",");
                 cVLNc=sVln.substring(0, y);
                 cMd=sVln.substring(sVln.length()-1,sVln.length());
                 sVln=sVln.substring(y+1,sVln.length()-2);
                 if (cMd.equals("A")){cM205OT="1";}
                 else if (cMd.equals("D")){cM205OT="3";
                 }
                 if ((sVln.equals("CHN")) && (sStepNo.equals("1"))){
                   cMVLN=cVLNc;
                   Process_ServiceOrderItem();
                   Process_DefValue();
                   Process_MapValue();}
                 else if ((sVln.equals("SGP")) && (sStepNo.equals("2"))){
                   cMVLN=cVLNc;
                   Process_ServiceOrderItem();
                   Process_DefValue();
                   Process_MapValue();}
                 else if ((sVln.equals("KHM")) && (sStepNo.equals("3"))){
                   cMVLN=cVLNc;
                   Process_ServiceOrderItem();
                   Process_DefValue();
                   Process_MapValue();}
                 else if ((sVln.equals("THA")) && (sStepNo.equals("4"))){
                   cMVLN=cVLNc;
                   Process_ServiceOrderItem();
                   Process_DefValue();
                   Process_MapValue();}
                 else if ((sVln.equals("IDN")) && (sStepNo.equals("5"))){
                   cMVLN=cVLNc;
                   Process_ServiceOrderItem();
                   Process_DefValue();
                   Process_MapValue();}
                 }
                 }
                }
    }
                sSql="update PROVLOG " +
                "set STEP='"+sStepNo+"' "+
                " where LOGID="+sCMHKLOGID;
                s2t.Update(sSql);
    }

     public void Process_WorkSubcode_05_17(String S2TImsiB,String TWNImsiB,String sReqStatus,String sTWNLDMSISDN) throws SQLException, IOException{
       Temprs=null;
       String Ssvrid="";//,cMd=""
       sSql="select nvl(serviceid,'0') as ab from imsi "+
             " where imsi = '"+S2TImsiB+"' and homeimsi='"+TWNImsiB+
             "'";
       logger.info("Get_Serviceid:"+sSql);
       Temprs=s2t.Query(sSql);
       while (Temprs.next()){
         Ssvrid=Temprs.getString("ab");
       }
       if (!Ssvrid.equals("0")){
         Temprs=null;
             sSql="select count(serviceid) as ab from serviceparameter where "+
                  "parameterid=3792 and serviceid='"+Ssvrid+"'";
             logger.info("Check_Follow_Me_To_Home(有1表示有申請, 0表示未申請):"+sSql);
             Temprs=s2t.Query(sSql);
             while (Temprs.next()){ //(有1表示有申請, 0表示未申請)
               sFMTH=Temprs.getString("ab");
             }

             if (sFMTH.equals("1")){
                 Temprs=null;
                 sSql="select nvl(value,'2') as ab From parametervalue where "+
                      "parametervalueid=3793 and serviceid='"+Ssvrid+"'";
                 logger.info("Check_Follow_Me_To_Home_Status(Value=1: active, Value=0: inactive, 若未申請, 則2):"+sSql);
                 Temprs=s2t.Query(sSql);
                 while (Temprs.next()){ //(Value=1: active, Value=0: inactive, 若未申請, 則NULL)
                   sFMTHa=Temprs.getString("ab");
                 }
             }
             Temprs=null;
             sSql="select count(serviceid) as ab from serviceparameter where "+
                  "parameterid=3748 and serviceid='"+Ssvrid+"'";
             logger.info("Check_SMS_Follow_Me_To_Home(有1表示有申請, 0表示未申請):"+sSql);
             Temprs=s2t.Query(sSql);
             while (Temprs.next()){ //(有1表示有申請, 0表示未申請)
               sSFMTH=Temprs.getString("ab");
             }

             if (sSFMTH.equals("1")){
                 Temprs=null;
                 sSql="select nvl(value,'2') as ab From parametervalue where "+
                      "parametervalueid=3752 and serviceid='"+Ssvrid+"'";
                 logger.info("Check_SMS_Follow_Me_To_Home_Status(Value=1: active, Value=0: inactive, 若未申請, 則2):"+sSql);
                 Temprs=s2t.Query(sSql);
                 while (Temprs.next()){ //(Value=1: active, Value=0: inactive, 若未申請, 則NULL)
                   sSFMTHa=Temprs.getString("ab");
                 }
             }
             if (sReqStatus.equals("17")){
             Temprs=null;
             sSql="select nvl(value,'0') as ab from parametervalue where parametervalueid=3792 "+
                  "and serviceid='"+Ssvrid+"'";
             logger.info("Check_FORWARD_TO_HOME_NO:"+sSql);
                 Temprs=s2t.Query(sSql);
                 while (Temprs.next()){
                   sFORWARD_TO_HOME_NO=Temprs.getString("ab");
                 }
             if (sFORWARD_TO_HOME_NO.equals('0')){sFORWARD_TO_HOME_NO=null;}
              Temprs=null;
             sSql="select nvl(value,'0') as ab from parametervalue where parametervalueid=3748 "+
                  "and serviceid='"+Ssvrid+"'";
             logger.info("Check_S_FORWARD_TO_HOME_NO:"+sSql);
                 Temprs=s2t.Query(sSql);
                 while (Temprs.next()){
                   sS_FORWARD_TO_HOME_NO=Temprs.getString("ab");
                 }
                 if (sS_FORWARD_TO_HOME_NO.equals('0')){sS_FORWARD_TO_HOME_NO=null;}
           }
             else{
               sFORWARD_TO_HOME_NO=sTWNLDMSISDN;
               sS_FORWARD_TO_HOME_NO=sTWNLDMSISDN;
               sTWNLDMSISDN=null;
             }
       }

              sSql="Select subcode, step_no from S2T_TB_WORK_SUBCODE Where MNO_NAME='"+
                      sMNOName+"' And work_type='"+cReqStatus+"' Order by step_no";
               logger.debug("Process_WorkSubcode_05_17:"+sSql);
               Temprs=s2t.Query(sSql);
                while (Temprs.next()){
                  sSubCode=Temprs.getString("subcode");
                  sStepNo=Temprs.getString("step_no");
                  Process_ServiceOrderItem();
                  Process_DefValue();
                  Process_MapValue();
                }
                sSql="update PROVLOG " +
                "set STEP='"+sStepNo+"' "+
                " where LOGID="+sCMHKLOGID;
                s2t.Update(sSql);
    }

     public void reProcess_WorkSubcode_05_17(String S2TImsiB,String TWNImsiB,String sStep_No,String sReqStatus,String sTWNLDMSISDN) throws SQLException, IOException{
    Temprs=null;
       String Ssvrid="";//,cMd=""
       sSql="select nvl(serviceid,'0') as ab from imsi "+
             " where imsi = '"+S2TImsiB+"' and homeimsi='"+TWNImsiB+
             "'";
       logger.info("Get_Serviceid:"+sSql);
       Temprs=s2t.Query(sSql);
       while (Temprs.next()){
         Ssvrid=Temprs.getString("ab");
       }
       if (!Ssvrid.equals("0")){
         Temprs=null;
             sSql="select count(serviceid) as ab from serviceparameter where "+
                  "parameterid=3792 and serviceid='"+Ssvrid+"'";
             logger.info("Check_Follow_Me_To_Home:"+sSql);
             Temprs=s2t.Query(sSql);
             while (Temprs.next()){ //(有1表示有申請, 0表示未申請)
               sFMTH=Temprs.getString("ab");
             }

             if (sFMTH.equals("1")){
                 Temprs=null;
                 sSql="select nvl(value,'2') as ab From parametervalue where "+
                      "parametervalueid=3793 and serviceid='"+Ssvrid+"'";
                 logger.info("Check_Follow_Me_To_Home_Status:"+sSql);
                 Temprs=s2t.Query(sSql);
                 while (Temprs.next()){ //(Value=1: active, Value=0: inactive, 若未申請, 則NULL)
                   sFMTHa=Temprs.getString("ab");
                 }
             }
             Temprs=null;
             sSql="select count(serviceid) as ab from serviceparameter where "+
                  "parameterid=3748 and serviceid='"+Ssvrid+"'";
             logger.info("Check_SMS_Follow_Me_To_Home:"+sSql);
             Temprs=s2t.Query(sSql);
             while (Temprs.next()){ //(有1表示有申請, 0表示未申請)
               sSFMTH=Temprs.getString("ab");
             }

             if (sSFMTH.equals("1")){
                 Temprs=null;
                 sSql="select case when count(value)=0 then '2' else value "+
            "end ab From parametervalue where "+
                      "parametervalueid=3752 and serviceid='"+Ssvrid+"'";
                 logger.info("Check_SMS_Follow_Me_To_Home_Status:"+sSql);
                 Temprs=s2t.Query(sSql);
                 while (Temprs.next()){ //(Value=1: active, Value=0: inactive, 若未申請, 則NULL)
                   sSFMTHa=Temprs.getString("ab");
                 }
             }
             if (sReqStatus.equals("17")){
             Temprs=null;
             sSql="select followmenumber  as ab from followmedata where serviceid= "+
                 "(select serviceid from imsi where imsi='"+S2TImsiB+"')";
             logger.info("Check_FORWARD_TO_HOME_NO:"+sSql);
                 Temprs=s2t.Query(sSql);
                 while (Temprs.next()){
                   sFORWARD_TO_HOME_NO=Temprs.getString("ab");
                 }

                            Temprs=null;
             sSql="select nvl(value,'0') as ab from parametervalue where parametervalueid=3792 "+
                  "and serviceid='"+Ssvrid+"'";
             logger.info("Check_FORWARD_TO_HOME_NO:"+sSql);
                 Temprs=s2t.Query(sSql);
                 while (Temprs.next()){
                   sFORWARD_TO_HOME_NO=Temprs.getString("ab");
                 }
             if (sFORWARD_TO_HOME_NO.equals('0')){sFORWARD_TO_HOME_NO=null;}
              Temprs=null;
             sSql="select nvl(value,'0') as ab from parametervalue where parametervalueid=3748 "+
                  "and serviceid='"+Ssvrid+"'";
             logger.info("Check_S_FORWARD_TO_HOME_NO:"+sSql);
                 Temprs=s2t.Query(sSql);
                 while (Temprs.next()){
                   sS_FORWARD_TO_HOME_NO=Temprs.getString("ab");
                 }
                 if (sS_FORWARD_TO_HOME_NO.equals('0')){sS_FORWARD_TO_HOME_NO=null;}
           }
           else{
               sFORWARD_TO_HOME_NO=sTWNLDMSISDN;
               sS_FORWARD_TO_HOME_NO=sTWNLDMSISDN;
               sTWNLDMSISDN=null;
             }
       
       }

                if (sStep_No.equals("0")){sStep_No="1";}
              sSql="Select subcode, step_no from S2T_TB_WORK_SUBCODE Where MNO_NAME='"+
                      sMNOName+"' And work_type='"+cReqStatus+"' and step_no>="+sStep_No +
            " Order by step_no";
                logger.debug("reProcess_WorkSubcode_05_17:"+sSql);
                Temprs=s2t.Query(sSql);
                while (Temprs.next()){
                  sSubCode=Temprs.getString("subcode");
                  sStepNo=Temprs.getString("step_no");
                  Process_ServiceOrderItem();
                  Process_DefValue();
                  Process_MapValue();
                }
                sSql="update PROVLOG " +
                "set STEP='"+sStepNo+"' "+
                " where LOGID="+sCMHKLOGID;
                s2t.Update(sSql);
    }

    public boolean Connect_DB() throws SQLException, ClassNotFoundException{
       boolean bconn = false;
       try{
        s2t.IP = s2tconf.getProperty("DBIp");
        s2t.PORT = s2tconf.getProperty("DBPort");
        s2t.SID = s2tconf.getProperty("DBName");
        s2t.un = s2tconf.getProperty("DBUserId");
        s2t.pw = s2tconf.getProperty("DBPassword");
        bconn=s2t.ConnDB();
        if (bconn==true){bconn=true;}
        else {bconn=false;}
   }catch(Exception ex){
        //20141004 新增 601 DBconnection Error
        logger.error(" DBconnection Error",ex);
       bconn=false;
       }

    return bconn;
    
    }

     public String Process_VLNString(String Scut)throws Exception{
        int iCut=0;
        String sV="",sM="",str1="",str="",sE="";
     while (Scut.length()>0){
    	 iCut=Scut.indexOf(",");
    	 if (iCut>0) {
    		 sV=Scut.substring(0, iCut);
    		 Scut=Scut.substring(iCut+1, Scut.length());
    		 sM=sV.substring(sV.length()-1,sV.length());
    		 sV=sV.substring(0,sV.length()-1);
		 } else{
			 sM=Scut.substring(Scut.length()-1,Scut.length());
			 sV=Scut.substring(0,Scut.length()-1);
			 Scut="";
		 }
    	 //countryname+vln+countryinit+A or D
    	 if (cReqStatus.equals("07")){
    		 str1=reGet_VLNNumber(sV,sM);
    		 str=str1;
		 }else {
			 str="Error";
		 }
    	 
    	 if (str.equals("Error")){
    		 str=Get_VLNNumber(sV,sM);
		 }
    	 
    	 if (!str.equals("Error")){
    		 logger.info("Process_VLNString:"+str+","+sM);
    		 vln.add(str+","+sM);
    		 sE="0";
		 }else { 
			 sE="1";
			 break;
		 }
    	 
  }   return sE;
    }
      //Check_S2T_IMSI
     public boolean Validate_IMSIRange(String s2timsi) throws SQLException, ClassNotFoundException, IOException{
    String minvalue="",maxvalue="",TmpSql="",sR="",sR1="";
    Temprs=null;
    int iL=0;
    sSql="SELECT minvalue, maxvalue FROM numbervalidation WHERE mnosubcode='"+
            sMNOSubCode +"' AND checktype='I'";
    Temprs=s2t.Query(sSql);
    while (Temprs.next()){
      minvalue=Temprs.getString("minvalue");
      maxvalue=Temprs.getString("maxvalue");
      TmpSql=TmpSql+" SELECT 'OK' result FROM dual WHERE '"+cTWNLDIMSI+
              "' BETWEEN '"+minvalue+"' AND '"+maxvalue+"' UNION ";
    }
    Temprs=null;
    if (TmpSql.length()>0){
    iL=TmpSql.lastIndexOf("UNION");
    TmpSql=TmpSql.substring(0, iL);
            logger.info("Validate_IMSIRange:"+sSql);
           Temprs=s2t.Query(sSql);
        Temprs=s2t.Query(TmpSql);
    while (Temprs.next()){
      sR=Temprs.getString("result");
      }
    }
        Temprs=null;
    if (!sR.equals("OK")){return false;}
    else {TmpSql="";
    sSql="SELECT minvalue, maxvalue FROM numbervalidation WHERE mnosubcode='000'" +
            " AND checktype='I'";
    Temprs=s2t.Query(sSql);
    while (Temprs.next()){
      minvalue=Temprs.getString("minvalue");
      maxvalue=Temprs.getString("maxvalue");
      TmpSql=TmpSql+" SELECT 'OK' result FROM dual WHERE '"+s2timsi+
              "' BETWEEN '"+minvalue+"' AND '"+maxvalue+"' UNION ";
    }
    Temprs=null;
    if (TmpSql.length()>0){
    iL=TmpSql.lastIndexOf("UNION");
    TmpSql=TmpSql.substring(0, iL);
    logger.debug("Check_s2timsi:"+TmpSql);
        Temprs=s2t.Query(TmpSql);
    while (Temprs.next()){
      sR1=Temprs.getString("result");
      }
    }
    if (sR1.equals("OK")){return true;}
    else {return false;}}

}
    //Check_CHT_MSISDN
     public boolean Validate_PartnerMSISDNRange(String sChtMsisdn) throws SQLException, ClassNotFoundException{
    String minvalue="",maxvalue="",TmpSql="",sR="";
    Temprs=null;
    int iL=0;
    sSql="SELECT minvalue, maxvalue FROM numbervalidation WHERE mnosubcode='"+
            sMNOSubCode +"' AND checktype='M'";
    Temprs=s2t.Query(sSql);
    while (Temprs.next()){
      minvalue=Temprs.getString("minvalue");
      maxvalue=Temprs.getString("maxvalue");
      TmpSql=TmpSql+" SELECT 'OK' result FROM dual WHERE '"+sChtMsisdn+
              "' BETWEEN '"+minvalue+"' AND '"+maxvalue+"' UNION ";
    }
    Temprs=null;
    if (TmpSql.length()>0){
    iL=TmpSql.lastIndexOf("UNION");
    TmpSql=TmpSql.substring(0, iL);
    logger.info("Validate_PartnerMSISDNRange:"+sSql);
        Temprs=s2t.Query(TmpSql);
    while (Temprs.next()){
      sR=Temprs.getString("result");
      }
    }
    if (sR.equals("OK")){return true;}
    else {return false;}

}
    // Select S2T_MSISDN
public void Find_AvailableS2TMSISDN() throws SQLException, IOException{
   Temprs=null;
    
   sSql="Select case when count(min(s2tmsisdn))=0 then 'null' else min(s2tmsisdn) "+
            "end as ab From availableMSISDN Where mnosubcode='"+sMNOSubCode+
            "' And status='F' group by s2tmsisdn";
   logger.debug("selc_S2Tmsisdn:" + sSql);
   Temprs = s2t.Query(sSql);
   
    while (Temprs.next()){
    cS2TMSISDN=Temprs.getString("ab");
    }
    
    if (!cS2TMSISDN.equals("null")){
    sSql="update availableMSISDN set status='B',lastupdatetime=sysdate," +
            "partnermsisdn='"+cTWNLDMSISDN+"' Where mnosubcode='"+sMNOSubCode+
            "' And s2tmsisdn='"+cS2TMSISDN+"'" ;
    logger.debug("Find_AvailableS2TMSISDN:"+sSql);
    s2t.Update(sSql);
    }
    }

    public  void Get_GurrentS2TMSISDN() throws SQLException, IOException{
       Temprs = null;
       sSql = "SELECT a.servicecode as ab FROM service a,IMSI b WHERE a.serviceid = "+
              "(SELECT MAX(Serviceid) FROM imsi WHERE homeimsi = '"+cTWNLDIMSI+"') AND "+
              "a.serviceid=b.serviceid ";
       logger.debug("Get_GurrentS2TMSISDN:" + sSql);
       Temprs = s2t.Query(sSql);
       
       while(Temprs.next()) {
          cS2TMSISDN = Temprs.getString("ab");
       }
    }

     public  void Get_GurrentS2TMSISDN_03() throws SQLException, IOException{
    Temprs=null;
    sSql="SELECT a.servicecode as ab FROM service a,IMSI b WHERE a.serviceid = "+
         "(SELECT MAX(Serviceid) FROM imsi WHERE homeimsi = '"+cTWNLDIMSI+"') AND "+
         "a.serviceid=b.serviceid ";
    logger.debug("Get_GurrentS2TMSISDN:"+sSql);
    Temprs=s2t.Query(sSql);
    while (Temprs.next()){
    cS2TMSISDN=Temprs.getString("ab");
    }
    }

    public String Check_VLNStatus(String ScutA) throws IOException, SQLException{
       int iACut=0,iEQ=0;
        String sVA="",sMA="",sCNN,SQLa="",sEQ="";//,strA="",sEA=""
        while (ScutA.length()>0){
       iACut=ScutA.indexOf(",");
      if (iACut>0) {
       sVA=ScutA.substring(0, iACut);
       ScutA=ScutA.substring(iACut+1, ScutA.length());
       sMA=sVA.substring(sVA.length()-1,sVA.length());
       sVA=sVA.substring(0,sVA.length()-1);}
       else{sMA=ScutA.substring(ScutA.length()-1,ScutA.length());
       sVA=ScutA.substring(0,ScutA.length()-1);
          ScutA="";}
       Temprs=null;
            sSql="Select countrycode from Countryinitial where countryinit='"+sVA+"'";
            Temprs=s2t.Query(sSql);
            while (Temprs.next()){
                sCNN=Temprs.getString("countrycode");
                TempRtA=null;
                if (sMA.equals("A")){
                TempRtA=null;
                SQLa="select count(*) as ab from availablevln where "+
                     "S2TMSISDN='"+cS2TMSISDN+"' and mnosubcode='"+sMNOSubCode+
                     "' and countrycode='"+sCNN+"'";
                logger.debug("Check_VLNStatus(A):"+SQLa);
                TempRtA=s2t.Query(SQLa);
                while (TempRtA.next()){
                sEQ=TempRtA.getString("ab");
                }
                if (!sEQ.equals("0")){
                  iEQ=330;
                  break;
                }}
               if (sMA.equals("D")){
                SQLa="select count(*) as ab from availablevln where "+
                     "S2TMSISDN='"+cS2TMSISDN+"' and mnosubcode='"+sMNOSubCode+
                     "' and countrycode='"+sCNN+"'";
                logger.debug("Check_VLNStatus(D):"+SQLa);
                TempRtA=s2t.Query(SQLa);
                while (TempRtA.next()){
                sEQ=TempRtA.getString("ab");
                }
                if (sEQ.equals("0")){
                  iEQ=331;
                  break;
                }}
            }

        }
        return Integer.toString(iEQ);
   }
    
    public String reCheck_VLNStatus(String ScutA) throws IOException, SQLException{
       int iACut=0,iEQ=0;
        String sVA="",sMA="",sCNN,SQLa="",sEQ="";//,strA="",sEA=""
        while (ScutA.length()>0){
       iACut=ScutA.indexOf(",");
      if (iACut>0) {
       sVA=ScutA.substring(0, iACut);
       ScutA=ScutA.substring(iACut+1, ScutA.length());
       sMA=sVA.substring(sVA.length()-1,sVA.length());
       sVA=sVA.substring(0,sVA.length()-1);}
       else{sMA=ScutA.substring(ScutA.length()-1,ScutA.length());
       sVA=ScutA.substring(0,ScutA.length()-1);
          ScutA="";}
       Temprs=null;
            sSql="Select countrycode from Countryinitial where countryinit='"+sVA+"'";
            Temprs=s2t.Query(sSql);
            while (Temprs.next()){
                sCNN=Temprs.getString("countrycode");
                TempRtA=null;
                if (sMA.equals("A")){
                 SQLa="select count(*) as ab from availablevln where "+
                     "S2TMSISDN='"+cS2TMSISDN+"' and mnosubcode='"+sMNOSubCode+
                     "' and countrycode='"+sCNN+"'  and status='B'";
                logger.debug("reCheck_VLNStatus(A-B):"+SQLa);
                TempRtA=s2t.Query(SQLa);
                while (TempRtA.next()){
                sEQ=TempRtA.getString("ab");
                }
                /*if (sEQ.equals("0")){
                TempRtA=null;
                SQLa="select count(*) as ab from availablevln where "+
                     "S2TMSISDN='"+cS2TMSISDN+"' and mnosubcode='"+sMNOSubCode+
                     "' and countrycode='"+sCNN+"'";
                logger.debug("reCheck_VLNStatus(A):"+SQLa);
                TempRtA=s2t.Query(SQLa);
                while (TempRtA.next()){
                sEQ=TempRtA.getString("ab");
                }
                }*/if (!sEQ.equals("0")){
                  iEQ=330;
                  break;
                }}
               if (sMA.equals("D")){
                SQLa="select count(*) as ab from availablevln where "+
                     "S2TMSISDN='"+cS2TMSISDN+"' and mnosubcode='"+sMNOSubCode+
                     "' and countrycode='"+sCNN+"'";
                logger.debug("reCheck_VLNStatus(D):"+SQLa);
                TempRtA=s2t.Query(SQLa);
                while (TempRtA.next()){
                sEQ=TempRtA.getString("ab");
                }
                if (sEQ.equals("0")){
                  iEQ=331;
                  break;
                }}
            }

        }
        return Integer.toString(iEQ);
   }

    public String Get_VLNNumber(String sCN,String sMo)throws SQLException, IOException{
       String cCountryCode="",iVPLMNID="",cVLNNUMBER="null",sNumId="";

           Temprs=null;
            sSql="Select vplmnid, countrycode,countryname from Countryinitial " +
                    "where countryinit='"+sCN+"'";
            logger.debug("MODE:"+sMo+",Get_VLNNumber:"+sSql);
            Temprs=s2t.Query(sSql);
            while (Temprs.next()){
                iVPLMNID=Temprs.getString("vplmnid");
                cCountryCode=Temprs.getString("countrycode");
                cCountryName=Temprs.getString("countryname");
            }
           if (sMo.equals("A")){
            Temprs=null;
            cVLNNUMBER="null";
            sSql= "select case when count(numid)=0 then 0 else numid "+
            "end as ab from availableMSISDN where s2tmsisdn='"+cS2TMSISDN+
                    "' group by numid";
            Temprs=s2t.Query(sSql);
            while (Temprs.next()){
                sNumId=Temprs.getString("ab");
            }
            Temprs=null;
            logger.debug("sNumId:"+sNumId);
            if (!sNumId.equals("0")){
            sSql="Select case when count(VLNNUMBER)=0 then 'null' else VLNNUMBER end "+
                    " as bc From availableVLN Where mnosubcode='"+sMNOSubCode+"' And "+
            " vplmnid="+iVPLMNID+" And countrycode='"+cCountryCode+"' And Status='F' "+
            " and numid="+sNumId+" group by VLNNUMBER ";
            }
            logger.debug("sNumId:"+sNumId+",Get_VLNNumber:"+sSql);
            Temprs=s2t.Query(sSql);
            while (Temprs.next()){
                cVLNNUMBER=Temprs.getString("bc");
            }
            logger.debug("cVLNNUMBER:"+cVLNNUMBER);
            Temprs=null;
            if((cVLNNUMBER.equals("null")) || (cVLNNUMBER.equals(""))|| (cVLNNUMBER.equals("0"))) {
             if(!cCountryCode.equals("86")){
                sSql="Select case when count(min(VLNNUMBER))=0 then 'null' " +
                    "else min(VLNNUMBER) end "+
                    " as cd From availableVLN Where mnosubcode='"+sMNOSubCode+"' And "+
            " vplmnid="+iVPLMNID+" And countrycode='"+cCountryCode+
                    "' And Status='F' group by VLNNUMBER ";
            logger.debug("MODE:"+sMo+",Get_VLNNumber:"+sSql);
            Temprs=s2t.Query(sSql);
            while (Temprs.next()){
                cVLNNUMBER=Temprs.getString("cd");
            }}}
           logger.info("MODE:"+sMo+",VLNNUMBER:"+cVLNNUMBER);
           }
           else if (sMo.equals("D")){
               Temprs=null;
               cVLNNUMBER="null";
             sSql="SELECT a.vln as ab FROM vlnnumber a, COUNTRYINITIAL b " +
                     "WHERE a.vplmnid=b.vplmnid "+
                  " AND vln LIKE '"+cCountryCode+"%' AND a.serviceid = "+
                  " (SELECT MAX(Serviceid) FROM imsi WHERE homeimsi = '"+cTWNLDIMSI+"')";
             logger.debug("Get_VLNNumber[D]:"+sSql);
             Temprs=s2t.Query(sSql);
             while (Temprs.next()){
                cVLNNUMBER=Temprs.getString("ab");
            }
            logger.info("MODE:"+sMo+",VLNNUMBER:"+cVLNNUMBER);
           }
            if (!cVLNNUMBER.equals("null")) {
            sSql="update availableVLN set Status='B',lastupdatetime=sysdate," +
                    "s2tmsisdn='"+cS2TMSISDN+"' where VLNNUMBER='"+cVLNNUMBER+"'";
            logger.debug("Get_VLNNumber[B]:"+sSql);
            s2t.Update(sSql);
            return cCountryName+","+cVLNNUMBER+","+sCN;
            }
            else {return "Error";}

      }

    public String reGet_VLNNumber(String sCN,String sMo)throws SQLException, IOException{
       String cCountryCode="",iVPLMNID="",cVLNNUMBER="null",sNumId="";

           Temprs=null;
           
            sSql="Select vplmnid, countrycode,countryname from Countryinitial " +
                    "where countryinit='"+sCN+"'";
            logger.debug("MODE:"+sMo+",reGet_VLNNumber:"+sSql);
            Temprs=s2t.Query(sSql);
            while (Temprs.next()){
                iVPLMNID=Temprs.getString("vplmnid");
                cCountryCode=Temprs.getString("countrycode");
                cCountryName=Temprs.getString("countryname");
            }
            
           if (sMo.equals("A")){
            Temprs=null;
            cVLNNUMBER="null";
            sSql= "select case when count(numid)=0 then 0 else numid "+
            "end as ab from availableMSISDN where s2tmsisdn='"+cS2TMSISDN+
                    "' group by numid";
            Temprs=s2t.Query(sSql);
            while (Temprs.next()){
                sNumId=Temprs.getString("ab");
            }
            Temprs=null;
            logger.debug("resNumId:"+sNumId);
            if (!sNumId.equals("0")){
            sSql="Select case when count(VLNNUMBER)=0 then 'null' else VLNNUMBER end "+
                    " as bc From availableVLN Where mnosubcode='"+sMNOSubCode+"' And "+
            " vplmnid="+iVPLMNID+" And countrycode='"+cCountryCode+"' And Status in ('B','U') "+
            " and numid="+sNumId+" group by VLNNUMBER ";
            }
            logger.debug("sNumId:"+sNumId+",reGet_VLNNumber:"+sSql);
            Temprs=s2t.Query(sSql);
            while (Temprs.next()){
                cVLNNUMBER=Temprs.getString("bc");
            }
            logger.debug("recVLNNUMBER:"+cVLNNUMBER);
            Temprs=null;
            if((cVLNNUMBER.equals("null")) || (cVLNNUMBER.equals(""))|| (cVLNNUMBER.equals("0"))) {
             if(!cCountryCode.equals("86")){
                sSql="Select case when count(VLNNUMBER)=0 then 'null' " +
                    "else VLNNUMBER end "+
                    " as cd From availableVLN Where mnosubcode='"+sMNOSubCode+"' And "+
            " vplmnid="+iVPLMNID+" And countrycode='"+cCountryCode+"' And s2tmsisdn='"+
                    cS2TMSISDN+"' And Status in ('B','U') group by VLNNUMBER ";
            logger.debug("MODE:"+sMo+",reGet_VLNNumber:"+sSql);
            Temprs=s2t.Query(sSql);
            while (Temprs.next()){
                cVLNNUMBER=Temprs.getString("cd");
            }}}
           logger.info("MODE:"+sMo+",reVLNNUMBER:"+cVLNNUMBER);
           }
           else if (sMo.equals("D")){
               Temprs=null;
               cVLNNUMBER="null";
             sSql="SELECT a.vln as ab FROM vlnnumber a, COUNTRYINITIAL b " +
                     "WHERE a.vplmnid=b.vplmnid "+
                  " AND vln LIKE '"+cCountryCode+"%' AND a.serviceid = "+
                  " (SELECT MAX(Serviceid) FROM imsi WHERE homeimsi = '"+cTWNLDIMSI+"')";
             logger.debug("reGet_VLNNumber[D]:"+sSql);
             Temprs=s2t.Query(sSql);
             while (Temprs.next()){
                cVLNNUMBER=Temprs.getString("ab");
            }
            logger.info("MODE:"+sMo+",reVLNNUMBER:"+cVLNNUMBER);
           }
            if (!cVLNNUMBER.equals("null")) {
            sSql="set Stupdate availableVLN atus='B',lastupdatetime=sysdate," +
                    "s2tmsisdn='"+cS2TMSISDN+"' where VLNNUMBER='"+cVLNNUMBER+"'";
            logger.debug("reGet_VLNNumber[B]:"+sSql);
            s2t.Update(sSql);
            return cCountryName+","+cVLNNUMBER+","+sCN;
            }
            else {return "Error";}

      }

     public void Write_ProvisionLog()throws Exception {
    	 logger.debug("Write_ProvisionLog...");
        int iCount=0,j;
            sDATE=s2t.Date_Format();
            sCount="";
            j=0;
            TempRtA=null;
            sCMHKLOGID="";
            sSql="select mnologid.nextval as ab from dual";
            TempRtA=s2t.Query(sSql);
            while (TempRtA.next()){
                sCMHKLOGID=TempRtA.getString("ab");
            }
            TempRtA.close();
          sSql="insert into PROVLOG(LOGID,MNOSUBCODE,MNOTICKETNO,REQTIME,CONTENT,STEP)"+
               " values("+sCMHKLOGID+",'"+sMNOSubCode+"','"+cTicketNumber+"',sysdate"+
                  ",'"+Sparam+"','')";
            s2t.Inster(sSql);
            TempRtA=null;
            //找出最後序號
            sSql="select currentseq,count(currentseq) as ab from seqrec where "+
            "MNOSUBCODE='"+sMNOSubCode+"' and currentdate='"+sDATE+
                    "' group by currentseq";
            TempRtA=s2t.Query(sSql);
             while(TempRtA.next()){
             sCount=TempRtA.getString("currentseq");
             j=TempRtA.getInt("ab");
             }
            TempRtA.close();
            if (j>0){
           iCount=Integer.parseInt(sCount);
            iCount=iCount+1;
            sCount=Integer.toString(iCount);
            sSql="update seqrec set currentseq="+sCount+" where MNOSUBCODE='"+
                    sMNOSubCode+"'";
            logger.debug("update seqrec:"+sSql);
            s2t.Update(sSql);
            }
            else{
            iCount=1;
            sSql="delete seqrec where MNOSUBCODE='"+sMNOSubCode+"'";
            s2t.Delete(sSql);
            sSql="insert into seqrec(MNOSUBCODE,currentdate,currentseq) values('"+
                    sMNOSubCode+"','"+sDATE+"',"+Integer.toString(iCount)+")";
            logger.debug("insert seqrec:"+sSql);
            s2t.Inster(sSql);
           sCount=Integer.toString(iCount);}
           for (i=sCount.length();i<3;i++){
           sCount="0"+sCount;
           }
    }

     public String Validate_TicketNumber() throws SQLException, IOException{
        String cflag="",SubscrId="",cStep="";
        Temprs=null;
    sSql="Select result_flag,subscr_id from S2T_TB_TYPB_WO_SYNC_FILE_DTL"+
         " where subscr_id ='"+cTicketNumber+"'";
    logger.debug("Validate_TicketNumber:"+sSql);
    Temprs=s2t.Query(sSql);
    while (Temprs.next()){
    cflag=Temprs.getString("result_flag");
    SubscrId=Temprs.getString("subscr_id");
    }
    if (cflag==null) {cflag="";}
    if (SubscrId.equals(cTicketNumber)){
            if (cflag.equals("000")){ return "201";}
            else{ Temprs=null;
              sSql="Select STEP from PROVLOG"+
                   " where MNOTICKETNO ='"+cTicketNumber+"'";
              Temprs=s2t.Query(sSql);
              while (Temprs.next()){
                cStep=Temprs.getString("STEP");
    }
            return cStep;}
    }else{
       return "000";
    }
    }

     public String Query_ServiceStatus() throws SQLException, IOException{
        String cSta="";
            Temprs=null;
    sSql="select status from service where serviceid=(select serviceid from " +
            "imsi where imsi='"+cS2TIMSI+"')";
    logger.debug("Query_ServiceStatus:"+sSql);
    Temprs=s2t.Query(sSql);
    while (Temprs.next()){
    cSta=Temprs.getString("status");
    } return cSta;
    }

     public String Remove_PartnerMSISDN() throws SQLException, IOException{
            Temprs=null;
         sSql="delete followmedata where followmenumber =" +
               "(select followmenumber from followmedata where followmenumber='"+
               cOldTWNLDMSISDN+"')";
         logger.debug("Remove_PartnerMSISDN:"+sSql);
         s2t.Delete(sSql);
       return "0";
    }

     public void Update_VLNNumber() throws SQLException, InterruptedException{
        @SuppressWarnings("unused")
		String sVlnN="",sCountry="",sOldVln="",sVlnName="";//
          vln.firstElement();
          for (n=0; n<vln.size();n++){
            sVln=vln.get(n);
             y=sVln.indexOf(",");
             //VLN CoUNTRY NAME
             sCountry=sVln.substring(0, y);
             sVln=sVln.substring(y+1,sVln.length());
             y=sVln.indexOf(",");
             if (y>0){
             //VLN NUMBER
             sVlnN=sVln.substring(0, y);
             sVln=sVln.substring(y+1,sVln.length());}
             else{sVln=sVln.substring(y+2,sVln.length());
             sVlnN="";}
             y=sVln.indexOf(",");
             if (y>0){
             //VLN NAME
             sVlnName=sVln.substring(0, y);
             //VLN MODE
             sVln=sVln.substring(y+1,sVln.length());}
             else{sVln=sVln.substring(y+2,sVln.length());}
             if (sVln.equals("A")){
               sSql="update S2T_TB_TYPB_WO_SYNC_FILE_DTL set VLN_"+sCountry+
                       "='"+sVlnN+"'" +" where subscr_id ='"+cTicketNumber+"'";
             s2t.Update(sSql);}
             else if (sVln.equals("D")){
             sSql="select VLN_"+sCountry+" as ab from S2T_TB_TYPB_WO_SYNC_FILE_DTL " +
                  " where subscr_id ='"+cTicketNumber+"'";
               Temprs=s2t.Query(sSql);
               Tmpvln.removeAllElements();
               while (Temprs.next()){
                 sOldVln=Temprs.getString("ab");
                 Tmpvln.add(sOldVln);
               }
             sSql="update S2T_TB_TYPB_WO_SYNC_FILE_DTL set VLN_"+sCountry+"=''" +
                       " where subscr_id ='"+cTicketNumber+"'";
             s2t.Update(sSql);}
             }
    }

     public void Query_ByPartnerMSISDN(PrintWriter outA) throws SQLException, InterruptedException, Exception{
        String cStatus = "", cVln= "";//, cCountryname = "", cCountryinit = ""
        vln.removeAllElements();
         Temprs=null;
         sSql="SELECT b.homeimsi as homeimsi, b.imsi as imsi,CASE a.status WHEN '1' " +
              "THEN '1'"+
              " WHEN '3' THEN '0' WHEN '4' THEN '2' "+
              " WHEN '10' THEN '2' END as status,a.servicecode as ab"+
              " FROM service a,IMSI b"+
              " WHERE a.serviceid = "+
              "(SELECT Serviceid FROM followmedata WHERE followmenumber ='"+
              cTWNLDMSISDN+"')"+
              " AND a.serviceid=b.serviceid and (a.status=1 or a.status=3)";
         
         logger.debug("Query_ByPartnerMSISDN:"+sSql);
         Temprs=s2t.Query(sSql);
         while (Temprs.next()){
           cTWNLDIMSI=Temprs.getString("homeimsi");
           cS2TIMSI=Temprs.getString("imsi");
           cS2TMSISDN=Temprs.getString("ab");
           cStatus=Temprs.getString("status");
         }
         if (!cS2TMSISDN.equals("")){
         Temprs=null;
         sSql="SELECT b.countryname||','||a.vln||','||b.countryinit||',N' as ab " +
                 "FROM vlnnumber a, COUNTRYINITIAL b"+
              " WHERE a.vplmnid=b.vplmnid AND a.serviceid = "+
"(SELECT Serviceid FROM followmedata WHERE followmenumber = '"+cTWNLDMSISDN+"')";
       Temprs=s2t.Query(sSql);
         while (Temprs.next()){
           cVln=Temprs.getString("ab");
            vln.add(cVln);
         }
       Query_GPRSStatus();
        Query_PreProcessResult(outA,"000");}
         else {cS2TMSISDN="";
        Query_PreProcessResult(outA,"211");
         }
         
         
       //20141215 add
         /*String vlnString="";
         for(String s : vln){
        	 vlnString+=s+",";
         }
         vlnString=vlnString.substring(0,vlnString.length()-1);*/
         
          
         
         sSql="SELECT A.ADDONCODE,A.ADDONACTION,to_char(A.REQUESTDATETIME,'yyyymmddhh24miss') REQUESTDATETIME " 
         		+ "FROM ADDONSERVICE A "
         		+ "WHERE A.ADDONCODE != 'SX000' AND A.MNOMSISDN='"+cTWNLDMSISDN+"' AND ROWNUM<=1 "
         		+ "ORDER BY A.REQUESTDATETIME DESC";
         Temprs=s2t.Query(sSql);
         
         String ccAddonCode = "",ccAddonAction = "",ccRequestDateTime = "";
         
         while (Temprs.next()){
        	 ccAddonCode=Temprs.getString("ADDONCODE");
        	 ccAddonAction=Temprs.getString("ADDONACTION");
        	 ccRequestDateTime=Temprs.getString("REQUESTDATETIME");
    	 }
         
        outA.println("<TWNLD_IMSI>");
        outA.println(cTWNLDIMSI);
        outA.println("</TWNLD_IMSI>");
        outA.println("<TWNLD_MSISDN>");
        outA.println(cTWNLDMSISDN);
        outA.println("</TWNLD_MSISDN>");
        outA.println("<S2T_MSISDN>");
        outA.println(cS2TMSISDN);
        outA.println("</S2T_MSISDN>");
        outA.println("<S2T_IMSI>");
        outA.println(cS2TIMSI);
        outA.println("</S2T_IMSI>");
        outA.println("<Status>");
        outA.println(cStatus);
        outA.println("</Status>");
        
        //20141215 add
        /*outA.println("<VLN>");
        outA.println(vlnString);
        outA.println("</VLN>");*/
        outA.println("<Addon_Code>");
        outA.println(ccAddonCode);
        outA.println("</Addon_Code>");
        outA.println("<Addon_Action>");
        outA.println(ccAddonAction);
        outA.println("</Addon_Action>");
        outA.println("<Req_DateTime>");
        outA.println(ccRequestDateTime);
        outA.println("</Req_DateTime>");
        
        
        sreturnXml=sreturnXml+"<TWNLD_IMSI>"+cTWNLDIMSI+"</TWNLD_IMSI><TWNLD_MSISDN>"+
                cTWNLDMSISDN+"</TWNLD_MSISDN><S2T_MSISDN>"+cS2TMSISDN+"</S2T_MSISDN><S2T_IMSI>"+
                cS2TIMSI+"</S2T_IMSI><Status>"+cStatus+"</Status>"
                /*"<VLN>"+vlnString+"</VLN>"*/
                + "<Addon_Code>"+ccAddonCode+"</Addon_Code><Addon_Action>"+ccAddonAction+"</Addon_Action><Req_DateTime>"+ccRequestDateTime+"</Req_DateTime>"; ;
    }

     public void Query_GPRSStatus() throws IOException, SQLException{
        String sG="";
     cGPRS="";
    Temprs=null;
         sSql="SELECT nvl(PDPSUBSID,0) as ab FROM basicprofile WHERE msisdn = '"+
                 cS2TMSISDN+"'";
           logger.debug("Query_GPRSStatus:"+sSql);
           Temprs=s2t.Query(sSql);
         Temprs=s2t.Query(sSql);
         while (Temprs.next()){
           sG=Temprs.getString("ab");
         }
         logger.debug("GPRS_Values:"+sG);
         if ((sG.equals("0"))||(sG.equals(""))){cGPRS="0";}
         else {cGPRS="1";}
    }

     public void Query_ByPartnerIMSI(PrintWriter outA) throws SQLException, InterruptedException, Exception{
        String cStatus="",cVln="";//,cCountryinit="",cCountryname=""
        vln.removeAllElements();
         
        Temprs = null;
         
        sSql="SELECT b.homeimsi as homeimsi, b.imsi as imsi,CASE a.status " +
                 "WHEN '1' THEN '1'"+
              " WHEN '3' THEN '0' WHEN '4' THEN '2' "+
              "WHEN '10' THEN '2' END as status,a.servicecode as ab "+
              "FROM service a,IMSI b WHERE a.serviceid = "+
              "(SELECT MAX(Serviceid) FROM imsi WHERE homeimsi ='"+cTWNLDIMSI+"')"+
              " AND a.serviceid=b.serviceid and (a.status=1 or a.status=3)";
        
         logger.debug("Query_ByPartnerIMSI:"+sSql);
         Temprs = s2t.Query(sSql);
         
         while(Temprs.next()) {
           cTWNLDIMSI = Temprs.getString("homeimsi");
           cS2TIMSI = Temprs.getString("imsi");
           cS2TMSISDN = Temprs.getString("ab");
           cStatus=Temprs.getString("status");
         }
         
         if(!cS2TMSISDN.equals("")) {
            Temprs = null;
                  
            sSql = "SELECT  followmenumber FROM followmedata WHERE serviceid ="+
                   " (SELECT MAX(Serviceid) FROM imsi WHERE homeimsi ='"+
                       cTWNLDIMSI+"')";
            Temprs=s2t.Query(sSql);
                  while (Temprs.next()){
           cTWNLDMSISDN=Temprs.getString("followmenumber");
         }
        Temprs=null;
        sSql="SELECT b.countryname||','||a.vln||','||b.countryinit||',N' as ab "+
             " FROM vlnnumber a, COUNTRYINITIAL b WHERE a.vplmnid=b.vplmnid"+
             " AND a.serviceid = "+
             "(SELECT MAX(Serviceid) FROM imsi WHERE homeimsi ='"+cTWNLDIMSI+"')";
        Temprs=s2t.Query(sSql);
         while (Temprs.next()){
        	 cVln=Temprs.getString("ab");
        	 vln.addElement(cVln);
    	 }
        Query_GPRSStatus();
        Query_PreProcessResult(outA,"000");}
         else {cS2TMSISDN="";
        Query_PreProcessResult(outA,"211");
        }
         
         //20141215 add
         /*String vlnString="";
         for(String s : vln){
        	 vlnString+=","+s;
         }
         vlnString=vlnString.substring(0,vlnString.length()-1);*/
          
         
         sSql="SELECT A.ADDONCODE,A.ADDONACTION,to_char(A.REQUESTDATETIME,'yyyymmddhh24miss') REQUESTDATETIME "
         		+ "FROM ADDONSERVICE A "
         		+ "WHERE A.ADDONCODE != 'SX000' AND A.MNOIMSI='"+cTWNLDIMSI+"' AND ROWNUM<=1 "
         		+ "ORDER BY A.REQUESTDATETIME DESC";
         Temprs=s2t.Query(sSql);
         
         String ccAddonCode = "",ccAddonAction = "",ccRequestDateTime = "";
         
         while (Temprs.next()){
        	 ccAddonCode=Temprs.getString("ADDONCODE");
        	 ccAddonAction=Temprs.getString("ADDONACTION");
        	 ccRequestDateTime=Temprs.getString("REQUESTDATETIME");
    	 }
         
         
        outA.println("<TWNLD_IMSI>");
        outA.println(cTWNLDIMSI);
        outA.println("</TWNLD_IMSI>");
        outA.println("<TWNLD_MSISDN>");
        outA.println(cTWNLDMSISDN);
        outA.println("</TWNLD_MSISDN>");
        outA.println("<S2T_IMSI>");
        outA.println(cS2TIMSI);
        outA.println("</S2T_IMSI>");
        outA.println("<S2T_MSISDN>");
        outA.println(cS2TMSISDN);
        outA.println("</S2T_MSISDN>");
        outA.println("<Status>");
        outA.println(cStatus);
        outA.println("</Status>");
        
        
        
//        outA.println("<Addon_Service");
//        outA.println("<Addon_Item");
//        outA.println("<Addon_Code");
//        outA.println(cAddonCode);
//        outA.println("</Addon_Code>");
//        outA.println("</Addon_Item");
//        outA.println("</Addon_Service");
        
        //20141215 add
        /*outA.println("<VLN>");
        outA.println(vlnString);
        outA.println("</VLN>");*/
        outA.println("<Addon_Code>");
        outA.println(ccAddonCode);
        outA.println("</Addon_Code>");
        outA.println("<Addon_Action>");
        outA.println(ccAddonAction);
        outA.println("</Addon_Action>");
        outA.println("<Req_DateTime>");
        outA.println(ccRequestDateTime);
        outA.println("</Req_DateTime>");
        
        
        
        
        sreturnXml=sreturnXml+"<TWNLD_IMSI>"+cTWNLDIMSI+"</TWNLD_IMSI><TWNLD_MSISDN>"+
                cTWNLDMSISDN+"</TWNLD_MSISDN><S2T_IMSI>"+cS2TIMSI+"</S2T_IMSI><S2T_MSISDN>"+
                cS2TMSISDN+"</S2T_MSISDN><Status>"+cStatus+"</Status>"
                /*"<VLN>"+vlnString+"</VLN>"*/
                		+ "<Addon_Code>"+ccAddonCode+"</Addon_Code><Addon_Action>"+ccAddonAction+"</Addon_Action><Req_DateTime>"+ccRequestDateTime+"</Req_DateTime>"; 
              //  "<Addon_Service><Addon_Item><Addon_Code>"+cAddonCode+"</Addon_Code></Addon_Item></Addon_Service>";
    }

     public String Query_VLNCountryName(String cVID,String sVlnNu) throws SQLException{
           String sCouN="",sCounC="";
           Temprs=null;
            sSql="Select countryname,countrycode from Countryinitial where " +
                    "vplmnid='"+cVID+"'";
            Temprs=s2t.Query(sSql);
            while (Temprs.next()){
                sCouN=Temprs.getString("countryname");
                sCounC=Temprs.getString("countrycode");
            }
            return sCouN+","+sVlnNu+","+sCounC+",F";

}

public String Update_GPRSStatus() throws SQLException, IOException {
   String sCoun="";
   Temprs = null;
           
   sSql = "Select count(subscr_id) as ab from S2T_TB_TYPB_WO_SYNC_FILE_DTL " +
          "where subscr_id ='" + cTicketNumber + "'";
           
   logger.debug("Update_GPRSStatus:"+ sSql);
   Temprs = s2t.Query(sSql);
   
   while (Temprs.next()){
      sCoun = Temprs.getString("ab");
   }
   
   if(Integer.parseInt(sCoun) > 0) { 
	   return "107";
   } else {
	   return "0";
   }
}

public String Load_ResultDescription(String sDecs) throws SQLException {
	//sDecs = "000";
	
	
   String sD="";
   Temprs = null;
           
   sSql="Select describe from S2T_TB_RESULT"+
        " where RESULT_FLAG ='"+sDecs+"'";
   
   Temprs = s2t.Query(sSql);
    
   while (Temprs.next()) {
     sD = Temprs.getString("describe");
   }
     
   return sD;
}

public void Query_PreProcessResult_null(PrintWriter outA, String rcode) throws Exception{
	cRCode = rcode;
    logger.info("Return_Code:"+rcode);
    outA.println("<Return_Code>");
    outA.println(rcode);
    outA.println("</Return_Code>");
    outA.println("<Return_DateTime>");
    
    sreturnXml=sreturnXml+"<Return_Code>"+rcode+"</Return_Code><Return_DateTime>";
    
    outA.println(s2t.Date_Format()+s2t.Date_Format_Time());
    
    outA.println("</Return_DateTime>");
    sreturnXml = sreturnXml+"</Return_DateTime>";
    desc = Load_ResultDescription(rcode);
}

     public void Query_PreProcessResult(PrintWriter outA, String rcode) throws SQLException, InterruptedException, Exception {
         cRCode = "";
         
         //20141222 add 
         logger.info("Process_Code:" + rcode);
         Process_Code = rcode;
         
         if(!cReqStatus.equals("97") && !cReqStatus.equals("98") && !cReqStatus.equals("18")) {
           /*logger.info("Process_Code:" + rcode);
           Process_Code = rcode;*/
           
           if(rcode.equals("000") || rcode.equals("501")) {
               rcode = Query_ServiceOrderStatus(outA);
            } else {
            	Rollback_VLNNumber(rcode);
            }
         }
         
         //rcode = "000";     //****************************************
         
         cRCode = rcode;
         logger.info("Return_Code:"+rcode);
         outA.println("<Return_Code>");
         outA.println(rcode);
         outA.println("</Return_Code>");
         outA.println("<Return_DateTime>");
                
         sreturnXml=sreturnXml+"<Return_Code>"+rcode+"</Return_Code><Return_DateTime>";
         
         outA.println(s2t.Date_Format()+s2t.Date_Format_Time());
         sSql="update PROVLOG set replytime=sysdate where LOGID="+sCMHKLOGID;
         
         logger.debug("Update PROVLOG:"+sSql);
         s2t.Update(sSql);
         
         sSql="update S2T_TB_TYPB_WO_SYNC_FILE_DTL set s2t_operationdate="+
              "to_date('"+s2t.Date_Format()+s2t.Date_Format_Time()+
              "','YYYYMMDDHH24MISS')"+
              " where WORK_ORDER_NBR='"+cWorkOrderNBR+"'";
         
         logger.debug("update S2T_TB_TYPB_WO_SYNC_FILE_DTL:"+sSql);
         s2t.Update(sSql);
         
         sSql="update S2T_TB_SERVICE_ORDER_ITEM set timestamp="+
              "to_date('"+s2t.Date_Format()+s2t.Date_Format_Time()+
              "','YYYYMMDDHH24MISS')"+
              " where Service_Order_NBR='"+cServiceOrderNBR+"'";
                
         logger.debug("Update S2T_TB_SERVICE_ORDER_ITEM:"+sSql);
         s2t.Update(sSql);
                
         sSql="update S2T_TB_SERVICE_ORDER set timestamp="+
              "to_date('"+s2t.Date_Format()+s2t.Date_Format_Time()+
              "','YYYYMMDDHH24MISS')"+
              " where SERVICE_ORDER_NBR='"+cServiceOrderNBR+"'";
                
         logger.debug("Update S2T_TB_SERVICE_ORDER:"+sSql);
         s2t.Update(sSql);
                
         outA.println("</Return_DateTime>");
         sreturnXml = sreturnXml+"</Return_DateTime>";
         desc = Load_ResultDescription(rcode);
   }

    public String Query_ServiceOrderStatus(PrintWriter outF) throws SQLException, InterruptedException, IOException {
      String cMesg = "";
      
      for(i = 0; i < 15; i++) {
         Thread.sleep(2000);
         
         Temprs = null;
         sSql="select STATUS from S2T_TB_SERVICE_ORDER Where SERVICE_ORDER_NBR ='"+
              cServiceOrderNBR+"'";
         
         //logger.info(sSql);
         Temprs = s2t.Query(sSql);
         
        while(Temprs.next()) {
           cMesg = Temprs.getString("STATUS");
        }
        
        logger.info("Query_ServiceOrderStatus:"+Integer.toString(i)+" Times "+ cMesg);
        
        if(cMesg.equals("Y") || cMesg.equals("F")){
        	break;
        }
       }
      
      if(cMesg.equals("Y") || cMesg.equals("F")) {
            cMesg = Query_SyncFileDtlStatus();
            if (cMesg.equals("")) {
              cMesg="501";
            }
       }
     else {cMesg="501";
        }
      return cMesg;
  }

    public String Query_SyncFileDtlStatus() throws SQLException, InterruptedException, IOException{
      String cSt="";
     for (i=0;i<5;i++){
        Thread.sleep(1000);
        Temprs=null;
        sSql="select result_flag from S2T_TB_TYPB_WO_SYNC_FILE_DTL Where " +
                "SERVICE_ORDER_NBR ='"+cServiceOrderNBR+"'";
        Temprs=s2t.Query(sSql);
        while (Temprs.next()){
                cSt=Temprs.getString("result_flag");
        }
        logger.info("Query_SyncFileDtlStatus:"+Integer.toString(i)+" Times "+cSt);
            }

     return cSt;
    }

    public void Rollback_VLNNumber(String cMess) throws SQLException, IOException{
    String cMode="",sCy="";
    int iSetNO=0;
  if (vln.size()>0){
                 vln.firstElement();
                 for (n=0; n<vln.size();n++){
                 sVln=vln.get(n);
                 y=sVln.indexOf(",");
                 sCy=sVln.substring(0, y);
                 sVln=sVln.substring(y+1,sVln.length());
                 y=sVln.indexOf(",");
                 cVLNc=sVln.substring(0, y);
                 cMode=sVln.substring(sVln.length()-1,sVln.length());
                 sVln=sVln.substring(y+1,sVln.length()-2);
 if (cReqStatus.equals("00")){
      if (cMode.equals("A")) {
      sSql="update availableVLN set Status='F',lastupdatetime=sysdate," +
              "s2tmsisdn='' where VLNNUMBER='"+cVLNc+"'";
      s2t.Update(sSql);}
      else if (cMode.equals("D")) {
      sSql="update availableVLN set Status='U',lastupdatetime=sysdate where" +
              " VLNNUMBER='"+cVLNc+"'";
      s2t.Update(sSql);}}
        else if (cReqStatus.equals("07")) {
              if (sVln.equals("CHN")){
                  iSetNO=1;}
                 else if (sVln.equals("SGP")){
                   iSetNO=2;}
                 else if (sVln.equals("KHM")){
                   iSetNO=3;}
                 else if (sVln.equals("THA")){
                   iSetNO=4;}
                 else if (sVln.equals("IDN")){
                   iSetNO=5;}
       Temprs=null;
       sSql="select nvl(count(step_no),'0') as ab from S2T_TB_SERVICE_ORDER_ITEM "+
        "where SERVICE_ORDER_NBR="+cServiceOrderNBR+" and status ='Y' and step_no="+iSetNO;
       logger.debug("Check step_no Status:"+sSql+",sVln:"+sVln);
               Temprs=s2t.Query(sSql);
        while (Temprs.next()){
                iSetNO=Temprs.getInt("ab");
        }
               if (iSetNO==0){
          if (cMode.equals("A")) {
         sSql="update S2T_TB_TYPB_WO_SYNC_FILE_DTL set VLN_"+sCy+"=''" +
                       " where subscr_id ='"+cTicketNumber+"'";
         logger.debug("rollback_S2T_TB_TYPB_WO_SYNC_FILE_DTL:"+sSql);
             s2t.Update(sSql);
        sSql="update availableVLN set Status='F',lastupdatetime=sysdate," +
                "s2tmsisdn='' where VLNNUMBER='"+cVLNc+"'";
        logger.debug("rollback_availableVLN:"+sSql);
      s2t.Update(sSql);}}
          if (cMode.equals("D")) {
         sSql="update S2T_TB_TYPB_WO_SYNC_FILE_DTL set VLN_"+sCy+"='"+cVLNc+"'" +
                       " where subscr_id ='"+cTicketNumber+"'";
             s2t.Update(sSql);
         sSql="update availableVLN set Status='U',lastupdatetime=sysdate where " +
                 "VLNNUMBER='"+cVLNc+"'";
      s2t.Update(sSql);}
        }
      }
}
  if (cReqStatus.equals("00")) {
  sSql="update availableMSISDN set status='F',lastupdatetime=sysdate," +
          "partnermsisdn='' Where mnosubcode='"+sMNOSubCode+"' And s2tmsisdn='"+
          cS2TMSISDN+"'" ;
  logger.debug("rollback_availableMSISDN:"+sSql);
  s2t.Update(sSql); }
}

   /* public void Send_agree_mail(){
      String mailserver=s2tconf.getProperty("mailserver");
      String From=s2tconf.getProperty("From");
      String to=s2tconf.getProperty("false");
      String Subject,messageText;
      InternetAddress[] ToAddress=null;
    }*/

    public void Send_AlertMail(){
      try{
      String Smailserver=s2tconf.getProperty("mailserver");
      String SFrom=s2tconf.getProperty("From");
      String Sto=s2tconf.getProperty("RDGroup");
      String SSubject,SmessageText;
      SSubject=dReqDate+"-"+cTicketNumber+"-"+cTWNLDMSISDN+"-"+cReqStatus+
              " Error:"+cRCode;
      SmessageText = "DateTime=" +dReqDate+
                    "<br>Ticket_Number=" + cTicketNumber +
                    "<br>TWNLD_IMSI=" +cTWNLDIMSI +
                    "<br>TWNLD_MSISDN=" +cTWNLDMSISDN+
                    "<br>S2T_IMSI=" +cS2TIMSI+
                    "<br>Req_Status=" +cReqStatus+
                    "<br>VLN_Country="+cVLNCountry+
                    "<br>GPRS_Status="+cGPRS+
                    "<br>Return_Code:"+cRCode+
                    "<br>Description:"+desc+
                    "<br>Addon_Code:"+cAddonCode+
                    "<br>Addon_Action:"+cAddonAction+
                    "<br><br>iError Tag:"+iError+
                    "<br>iError_Message:"+iErrorMsg;
      				
      //20140919 Add Record Error SQL
      if(!"".equals(sErrorSQL)){
    	  SmessageText+="<br>Exception by SQL : "+sErrorSQL;
    	  logger.info("Add Error SQL on mail message:"+sErrorSQL);
      }
      
        s2t.SendAlertMail(Smailserver, SFrom, Sto, SSubject, SmessageText);
         logger.info("Send Mail Content:"+SmessageText);}
      catch(Exception ex){
        logger.error("JAVA Error:"+ex.toString());}
    }
    
    public void Send_AlertMail(String content){
        try{
        String Smailserver=s2tconf.getProperty("mailserver");
        String SFrom=s2tconf.getProperty("From");
        String Sto=s2tconf.getProperty("RDGroup");
        String SSubject,SmessageText;
        SSubject=dReqDate+"-"+cTicketNumber+"-"+cTWNLDMSISDN+"-"+cReqStatus+
                " Error:"+cRCode;
        SmessageText = content;        
          s2t.SendAlertMail(Smailserver, SFrom, Sto, SSubject, SmessageText);
           logger.info("Send Mail Content:"+SmessageText);}
        catch(Exception ex){
          logger.error("JAVA Error:"+ex.toString());}
      }
    
    //20150625 add
    /*public void Send_AlertMail(){
    	String Sto=s2tconf.getProperty("RDGroup");
    	String SSubject,SmessageText;
        SSubject=dReqDate+"-"+cTicketNumber+"-"+cTWNLDMSISDN+"-"+cReqStatus+
                " Error:"+cRCode;
        SmessageText = "DateTime=" +dReqDate+
                      "<br>Ticket_Number=" + cTicketNumber +
                      "<br>TWNLD_IMSI=" +cTWNLDIMSI +
                      "<br>TWNLD_MSISDN=" +cTWNLDMSISDN+
                      "<br>S2T_IMSI=" +cS2TIMSI+
                      "<br>Req_Status=" +cReqStatus+
                      "<br>VLN_Country="+cVLNCountry+
                      "<br>GPRS_Status="+cGPRS+
                      "<br>Return_Code:"+cRCode+
                      "<br>Description:"+desc+
                      "<br>Addon_Code:"+cAddonCode+
                      "<br>Addon_Action:"+cAddonAction+
                      "<br><br>iError Tag:"+iError+
                      "<br>iError_Message:"+iErrorMsg;

		
		String [] cmd=new String[3];
		cmd[0]="/bin/bash";
		cmd[1]="-c";
		cmd[2]= "/bin/echo \""+SmessageText+"\" | /bin/mailx -s \""+SSubject+"\" -r TWNLD_ALERT "+Sto;

		try{
			Process p = Runtime.getRuntime().exec (cmd[2]);
			p.waitFor();
			logger.info("send mail cmd:"+cmd[2]);
		}catch (Exception e){
			logger.error("send mail fail:"+SmessageText+"。",e);
		}
	}
    
    public void Send_AlertMail(String content){
    	
    	String Sto=s2tconf.getProperty("RDGroup");
        String SSubject,SmessageText;
        SSubject=dReqDate+"-"+cTicketNumber+"-"+cTWNLDMSISDN+"-"+cReqStatus+
                " Error:"+cRCode;
        SmessageText = content;   
        
    	try{
	        String [] cmd=new String[3];
			cmd[0]="/bin/bash";
			cmd[1]="-c";
			cmd[2]= "/bin/echo \""+SmessageText+"\" | /bin/mailx -s \""+SSubject+"\" -r TWNLD_ALERT "+Sto;
			
		
			Process p = Runtime.getRuntime().exec (cmd[2]);
			p.waitFor();
			logger.info("send mail cmd:"+cmd[2]);
		}catch (Exception e){
			logger.error("send mail fail:"+SmessageText+"。",e);
		}
      }*/

    public void read_xml(Document doc){
            NodeList nodes;
            
            for (i = 0; i < doc.getDocumentElement().getChildNodes().getLength(); i++) {
                String TagName = doc.getDocumentElement().getChildNodes().item(i).getNodeName();
                if (TagName.equals("Req_DateTime")) {
                    nodes = doc.getElementsByTagName("Req_DateTime");
                    if (nodes.item(0).getFirstChild() != null) {
                        dReqDate = nodes.item(0).getFirstChild().getNodeValue();
                    }
                    Sparam = nodes.item(0).getNodeName() + "=" + dReqDate;
                } else if (TagName.equals("Ticket_Number")) {
                    nodes = doc.getElementsByTagName("Ticket_Number");
                    if (nodes.item(0).getFirstChild() != null) {
                        cTicketNumber = nodes.item(0).getFirstChild().getNodeValue();
                    }
                    Sparam = Sparam + "," + nodes.item(0).getNodeName() + "=" + cTicketNumber;
                } else if (TagName.equals("TWNLD_IMSI")) {
                    nodes = doc.getElementsByTagName("TWNLD_IMSI");
                    if (nodes.item(0).getFirstChild() != null) {
                        cTWNLDIMSI = nodes.item(0).getFirstChild().getNodeValue();
                    }
                    Sparam = Sparam + "," + nodes.item(0).getNodeName() + "=" + cTWNLDIMSI;
                } else if (TagName.equals("TWNLD_MSISDN")) {
                    nodes = doc.getElementsByTagName("TWNLD_MSISDN");
                    if (nodes.item(0).getFirstChild() != null) {
                        cTWNLDMSISDN = nodes.item(0).getFirstChild().getNodeValue();
                    }
                    Sparam = Sparam + "," + nodes.item(0).getNodeName() + "=" + cTWNLDMSISDN;
                } else if (TagName.equals("S2T_IMSI")) {
                    nodes = doc.getElementsByTagName("S2T_IMSI");
                    if (nodes.item(0).getFirstChild() != null) {
                        cS2TIMSI = nodes.item(0).getFirstChild().getNodeValue();
                    }
                    Sparam = Sparam + "," + nodes.item(0).getNodeName() + "=" + cS2TIMSI;
                } else if (TagName.equals("Req_Status")) {
                    nodes = doc.getElementsByTagName("Req_Status");
                    if (nodes.item(0).getFirstChild() != null) {
                        cReqStatus = nodes.item(0).getFirstChild().getNodeValue();
                    }
                    
                    Sparam = Sparam + "," + nodes.item(0).getNodeName() + "=" + cReqStatus;
                } else if(TagName.equals("Addon_Service")) {   
                	nodes = doc.getElementsByTagName("Addon_Item");
                	if(nodes!=null && nodes.getLength()>0){
                		for(int i = 0; i < nodes.getLength(); i++) {
                			
                        	Node itemNode = nodes.item(i);

                        	if(itemNode.getNodeType() == Node.ELEMENT_NODE) {
                        		
                        	   //20150206  change
                        		
                        		Element first = (Element)itemNode;
                        		NodeList codeNode = first.getElementsByTagName("Addon_Code");
                                Element codeElement = (Element)codeNode.item(0);
                                NodeList codeNodeList = codeElement.getChildNodes();
                                cAddonCode = ((Node)codeNodeList.item(0)).getNodeValue().trim();
                                
                                Sparam =Sparam + "," + codeNode.item(0).getNodeName() + "=" + cAddonCode;
                                
                                NodeList actionNode = first.getElementsByTagName("Addon_Action");
                                Element actionElement = (Element)actionNode.item(0);
                                NodeList actionNodeList = actionElement.getChildNodes();
                                cAddonAction = ((Node)actionNodeList.item(0)).getNodeValue().trim(); 
                                
                                Sparam =Sparam + "," + actionNode.item(0).getNodeName() + "=" + cAddonAction;
                                
                                
                                if("18".equals(cReqStatus)&& "SX000".equalsIgnoreCase(cAddonCode)){
                             	   logger.info("Give up SX000 Data...");
                             	   continue;
                                }
          
                                Map<String,String> map = new HashMap<String,String>();
                                
                                map.put("AddonCode", cAddonCode);
                                map.put("AddonAction", cAddonAction);
                                cAddonItem.add(map);
                                
                               /*Element first = (Element)itemNode;
                        	   NodeList codeNode = first.getElementsByTagName("Addon_Code");
                               Element codeElement = (Element)codeNode.item(0);
                               NodeList codeNodeList = codeElement.getChildNodes();
                               
                               //20140919 Add 當是18時，讀取到SX000時不取資料
                               if("18".equals(cReqStatus)&& "SX000".equalsIgnoreCase(((Node)codeNodeList.item(0)).getNodeValue().trim())){
                            	   logger.info("Give up SX000 Data...");
                            	   continue;
                               }

                               cAddonCode = ((Node)codeNodeList.item(0)).getNodeValue().trim();
                               Sparam = Sparam + "," + codeNode.item(0).getNodeName() + "=" + cAddonCode;

                               NodeList actionNode = first.getElementsByTagName("Addon_Action");
                               Element actionElement = (Element)actionNode.item(0);
                               NodeList actionNodeList = actionElement.getChildNodes();
                               cAddonAction = ((Node)actionNodeList.item(0)).getNodeValue().trim();  
                               Sparam = Sparam + "," + actionNode.item(0).getNodeName() + "=" + cAddonAction;*/
                        	}
                        }
                	}
                } else if (TagName.equals("VLN_Country")) {
                    nodes = doc.getElementsByTagName("VLN_Country");
                    if (nodes.item(0).getFirstChild() != null) {
                        cVLNCountry = nodes.item(0).getFirstChild().getNodeValue();
                    }
                    Sparam = Sparam + "," + nodes.item(0).getNodeName() + "=" + cVLNCountry;
                } else if (TagName.equals("GPRS_Status")) {
                    nodes = doc.getElementsByTagName("GPRS_Status");
                    if (nodes.item(0).getFirstChild() != null) {
                        cGPRSStatus = nodes.item(0).getFirstChild().getNodeValue();
                    }
                    Sparam = Sparam + "," + nodes.item(0).getNodeName() + "=" + cGPRSStatus;
                }
            }
            
            //logger.info("Tag:" + Sparam);
            
    }

    ///20150625 add
    public void send_OTA_SMS(){
    	logger.debug("send_OTA_SMS");
    	
    	//20150626 add
    	String phone=cTWNLDMSISDN;
    	if("true".equals(s2tconf.getProperty("TestMod"))){
    		phone=s2tconf.getProperty("TestPhoneNumber");
    	}
    	
    	try {
    		String res = setOTASMSPostParam("", phone);
    		
    		Temprs=s2t.Query("select SMSLOGID.NEXTVAL SMSLOGID from dual");
    		String logid=null;
    		while(Temprs.next()){
    			logid=Temprs.getString("SMSLOGID");
    		}
    		
			logger.debug("send OTA sms result : " + res);
			sSql="INSERT INTO S2T_BL_SMS_LOG (SMSID, TYPE, PHONENUMBER, CREATETIME, CONTENT) "
					+ "VALUES ("+logid+", 'T','"+phone+"', SYSDATE, 'OTA Message')";
			logger.debug("SMS:" + sSql);
			
			//寫入資料庫
			s2t.Inster(sSql);
		} catch (IOException e) {
			ErrorHandle("IOException",e);
		} catch (SQLException e) {
			ErrorHandle("SQLException",e);
		}
    }
    public void send_SMS(String VARREALMSG1){
    	logger.debug("send_SMS");
    	
    	
    	//20150626 add
    	String phone=cTWNLDMSISDN;
    	if("true".equals(s2tconf.getProperty("TestMod"))){
    		phone=s2tconf.getProperty("TestPhoneNumber");
    	}
    	
    	//SimpleDateFormat spf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    	//20150514 add
    	try {
    		VARREALMSG1 = new String(VARREALMSG1.getBytes("BIG5"), "ISO-8859-1");
			String res = setSMSPostParam(VARREALMSG1, phone);
			
			Temprs=s2t.Query("select SMSLOGID.NEXTVAL SMSLOGID from dual");
    		String logid=null;
    		while(Temprs.next()){
    			logid=Temprs.getString("SMSLOGID");
    		}
			logger.debug("send "+logid+" sms result : " + res);

			sSql="INSERT INTO S2T_BL_SMS_LOG (SMSID, TYPE, PHONENUMBER, CREATETIME, CONTENT) "
					+ "VALUES ("+logid+", 'T','"+phone+"', SYSDATE, '"+VARREALMSG1+"')";
			logger.debug("SMS:" + sSql);
			
			//寫入資料庫
			s2t.Inster(sSql);
			
			if(res.indexOf("Message Submitted")==-1){
				throw new Exception("Sendding SMS Error!<br>"
						+ "cTWNLDMSISDN="+phone+"<br>"
						+ "VARREALMSG="+VARREALMSG1);
			}
		} catch (IOException e) {
			ErrorHandle("IOException",e);
		} catch (SQLException e) {
			ErrorHandle("SQLException",e);
		} catch (Exception e) {
			ErrorHandle(e);
		}

    	
    	//20150514 mod
        /*try {
            VARREALMSG1 = new String(VARREALMSG1.getBytes("BIG5"), "ISO-8859-1");
            sSql = "INSERT INTO MESSAGETASK (MESSAGETASKID, TYPE, SUBSIDIARYID, " +
                    "SERVICEID," + " SERVICECODE, SERVICETYPE, CONTROLFLAG, PRIORITY," +
                    " SENDTYPE, SUSPENDFLAG," + " STATUS, NOTIFYSTATUS, CREATEDATE, " +
                    "MSGCONTENT, POLLSTATUS, POLLCOUNT, " + "NEXTPOLLTIME, FAILURECOUNT," +
                    " ROUNDCOUNT, MSBDEFID)" + " VALUES (messagetaskid.nextval, '1', 42, " +
                    "402,'" + cTWNLDMSISDN + "', '1', '0'," + " 50, '1', '0', '0', 'A', " +
                    "SYSDATE,'" + VARREALMSG1 + "', '0', 0, " +
                    "SYSDATE+30/1440, 0, 0, 100)";
            logger.debug("SMS:" + sSql);
            s2t.Inster(sSql);
        } catch (SQLException ex) {
        	logger.error("SQLException",ex);
            java.util.logging.Logger.getLogger(TWNLDprovision.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            sErrorSQL+=sSql;
        } catch (UnsupportedEncodingException ex) {
        	logger.error("UnsupportedEncodingException",ex);
            java.util.logging.Logger.getLogger(TWNLDprovision.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }*/
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
    
    public void send_SMS(String VARREALMSG1,String sendtime){
    	logger.debug("send_SMS delay");
    	

    	//20150626 add
    	String phone=cTWNLDMSISDN;
    	if("true".equals(s2tconf.getProperty("TestMod"))){
    		phone=s2tconf.getProperty("TestPhoneNumber");
    	}
    	
    	
    	try {
    		Temprs=s2t.Query("select SMSLOGID.NEXTVAL SMSLOGID from dual");
    		String logid=null;
    		while(Temprs.next()){
    			logid=Temprs.getString("SMSLOGID");
    		}
        	
        	Map<String,String> map = new HashMap<String,String>();
        	map.put("VARREALMSG", VARREALMSG1);
        	map.put("TWNLDMSISDN", phone);
        	map.put("sendtime", sendtime);
        	map.put("SMSLOGID", logid);
        	//XXX
        	SMSThread.delaySMS.add(map);
    		
    		
    		VARREALMSG1 = new String(VARREALMSG1.getBytes("BIG5"), "ISO-8859-1");

			sSql="INSERT INTO S2T_BL_SMS_LOG (SMSID, TYPE, PHONENUMBER, CREATETIME, CONTENT) "
					+ "VALUES ("+logid+", 'T','"+phone+"', to_date('"+sendtime+"','yyyyMMddhh24mi'), '"+VARREALMSG1+"')";
			logger.debug("SMS:" + sSql);
			
			//寫入資料庫
			s2t.Inster(sSql);
		} catch (IOException e) {
			ErrorHandle("IOException",e);
		} catch (SQLException e) {
			ErrorHandle("SQLException",e);
		}
    }

    public void send_SMS1(String VARREALMSG2){
    	logger.debug("send_SMS1");
    	
    	//20150626 add
    	String phone=cTWNLDMSISDN;
    	if("true".equals(s2tconf.getProperty("TestMod"))){
    		phone=s2tconf.getProperty("TestPhoneNumber");
    	}
    	
    	//20150514 add
    	try {
    		VARREALMSG2 = new String(VARREALMSG2.getBytes("BIG5"), "ISO-8859-1");
			String res = setSMSPostParam(VARREALMSG2, phone);
			
			
			Temprs=s2t.Query("select SMSLOGID.NEXTVAL SMSLOGID from dual");
    		String logid=null;
    		while(Temprs.next()){
    			logid=Temprs.getString("SMSLOGID");
    		}
    		
			logger.debug("send "+logid+" sms result : " + res);
			
			sSql="INSERT INTO S2T_BL_SMS_LOG (SMSID, TYPE, PHONENUMBER, CREATETIME, CONTENT) "
					+ "VALUES ("+logid+", 'T','"+phone+"', SYSDATE, '"+VARREALMSG2+"')";
			logger.debug("SMS:" + sSql);

			//寫入資料庫
			s2t.Inster(sSql);
			
			if(res.indexOf("Message Submitted")==-1){
				throw new Exception("Sendding SMS Error!<br>"
						+ "cTWNLDMSISDN="+phone+"<br>"
						+ "VARREALMSG="+VARREALMSG2);
			}
		} catch (IOException e) {
			ErrorHandle("IOException",e);
		} catch (SQLException e) {
			ErrorHandle("SQLException",e);
		} catch (Exception e) {
			ErrorHandle(e);
		}
    	
    	//20150514 mod
       /* try {
            VARREALMSG2 = new String(VARREALMSG2.getBytes("BIG5"), "ISO-8859-1");
            sSql = "INSERT INTO MESSAGETASK (MESSAGETASKID, TYPE, SUBSIDIARYID, " +
                    "SERVICEID," + " SERVICECODE, SERVICETYPE, CONTROLFLAG, PRIORITY," +
                    " SENDTYPE, SUSPENDFLAG," + " STATUS, NOTIFYSTATUS, CREATEDATE, " +
                    "MSGCONTENT, POLLSTATUS, POLLCOUNT, " + "NEXTPOLLTIME, FAILURECOUNT," +
                    " ROUNDCOUNT, MSBDEFID)" + " VALUES (messagetaskid.nextval, '1', 42, " +
                    "402,'" + cTWNLDMSISDN + "', '1', '0'," + " 50, '1', '0', '0', 'A', " +
                    "SYSDATE,'" + VARREALMSG2 + "', '0', 0, " +
                    "SYSDATE+30/1440, 0, 0, 100)";
            logger.debug("SMS:" + sSql);
            s2t.Inster(sSql);
        } catch (SQLException ex) {
        	logger.error("SQLException",ex);
            java.util.logging.Logger.getLogger(TWNLDprovision.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            sErrorSQL+=sSql;
        } catch (UnsupportedEncodingException ex) {
        	logger.error("UnsupportedEncodingException",ex);
        	java.util.logging.Logger.getLogger(TWNLDprovision.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }*/
    }
    
    
    
    public void send_SMS1(String VARREALMSG2,String sendtime){
    	logger.debug("send_SMS1 dalay");
    	//20150626 add
    	String phone=cTWNLDMSISDN;
    	if("true".equals(s2tconf.getProperty("TestMod"))){
    		phone=s2tconf.getProperty("TestPhoneNumber");
    	}
    	

		
    	
    	try {
    		
    		Temprs=s2t.Query("select SMSLOGID.NEXTVAL SMSLOGID from dual");
    		String logid=null;
    		while(Temprs.next()){
    			logid=Temprs.getString("SMSLOGID");
    		}
        	
        	Map<String,String> map = new HashMap<String,String>();
        	map.put("VARREALMSG", VARREALMSG2);
        	map.put("TWNLDMSISDN", phone);
        	map.put("sendtime", sendtime);
        	map.put("SMSLOGID", logid);
        	//XXX
        	SMSThread.delaySMS.add(map);
        	
    		VARREALMSG2 = new String(VARREALMSG2.getBytes("BIG5"), "ISO-8859-1");

			sSql="INSERT INTO S2T_BL_SMS_LOG (SMSID, TYPE, PHONENUMBER, CREATETIME, CONTENT) "
					+ "VALUES (SMSLOGID.NEXTVAL, 'T','"+phone+"', to_date('"+sendtime+"','yyyyMMddhh24mi'), '"+VARREALMSG2+"')";
			logger.debug("SMS:" + sSql);
			
			//寫入資料庫
			s2t.Inster(sSql);
		} catch (IOException e) {
			ErrorHandle("IOException",e);
		} catch (SQLException e) {
			ErrorHandle("SQLException",e);
		}
    }
  //20150520 add
    public void send_dalay_SMS(List<Map<String,String>> sms){
    	
    	for(Map<String,String> map : sms){
    		String VARREALMSG = map.get("VARREALMSG");
    		String TWNLDMSISDN = map.get("TWNLDMSISDN");
    		String logid = map.get("SMSLOGID");
        	try {
        		VARREALMSG = new String(VARREALMSG.getBytes("BIG5"), "ISO-8859-1");
    			String res = setSMSPostParam(VARREALMSG, TWNLDMSISDN);
    			logger.debug("Send SMS "+logid+"  Msg="+VARREALMSG+",result="+res);  
    			if(res.indexOf("Message Submitted")==-1){
    				throw new Exception("Sendding SMS Error!<br>"
    						+ "cTWNLDMSISDN="+TWNLDMSISDN+"<br>"
    						+ "VARREALMSG="+VARREALMSG);
    			}
    		} catch (IOException e) {
            	//Send_AlertMail2("send delay sms error! \n"+ s.toString());
            	//20150625 add
            	//20150626 mod
            	ErrorHandle("send delay sms error!",e);
    		} catch (Exception e) {
    			ErrorHandle(e);
			}
    	}
    }

    public String Check_Pair_IMSI(String TWNImsi,String S2TImsi) throws SQLException{
        String Scheck="";
        Temprs=null;
        sSql="select count(imsi) as ab from imsi "+
             " where imsi = '"+S2TImsi+"' and homeimsi='"+TWNImsi+
             "'";
        
        logger.info("Check_Pair_IMSI:" + sSql);
        Temprs = s2t.Query(sSql);
      
        while(Temprs.next()) {
           Scheck = Temprs.getString("ab");
        }
        
        return Scheck;
    }

    public String Check_Pair_IMSI_status(String TWNImsi,String S2TImsi) throws SQLException{
        String Schec="";//status=0 未使用,1 使用
        Temprs=null;
        sSql="select status as ab from imsi "+
             " where imsi = '"+S2TImsi+"' and homeimsi='"+TWNImsi+
             "'";
        logger.info("Check_Pair_IMSI_status:"+sSql);
        Temprs=s2t.Query(sSql);
        while (Temprs.next()){
           Schec=Temprs.getString("ab");
        }
      return Schec;
    }

    public String Check_S2T_Msisdn_UNused() throws SQLException{
      String Scm="";  //status=0 未使用
      Temprs=null;
      sSql="select count(phonenumber) as ab from supplyphonenumber "+
            "where status=0 and subsidiaryid='"+Ssubsidiaryid+"'"+
            " and phonenumber='"+cS2TMSISDN+"'";
      logger.info("Check_S2T_Msisdn_UNused:"+sSql);
      Temprs=s2t.Query(sSql);
      while (Temprs.next()){
           Scm=Temprs.getString("ab");
      }
            if (Scm.equals("0")){
      sSql="update availableMSISDN set status='X',lastupdatetime=sysdate," +
          "partnermsisdn='' Where mnosubcode='"+sMNOSubCode+"' And s2tmsisdn='"+
          cS2TMSISDN+"'" ;
       logger.debug("Change_availableMSISDN(X):"+sSql);
       s2t.Update(sSql);}
        return Scm;
    }

public String Check_TWN_Msisdn_Status(String TWNImsiA,String S2TImsiA) throws SQLException{
   String Ssvrid="", TwnMsisdn="";
   Temprs=null;
              
   sSql = "select nvl(serviceid,'0') as ab from imsi "+
          " where imsi = '"+S2TImsiA+"' and homeimsi='"+TWNImsiA+
          "'";
              
   logger.info("Check_TWN_Msisdn_Status_A:" + sSql);
   
   Temprs = s2t.Query(sSql);
  
   while(Temprs.next()) {
      Ssvrid=Temprs.getString("ab");
   }
      
   if(!Ssvrid.equals("0")){
      Temprs = null;
        
      //sSql = "select followmenumber from followmedata where  serviceid='" + Ssvrid + "'";
      sSql = "select count(followmenumber) as ab from followmedata where  serviceid='" + Ssvrid + "' AND followmenumber='"+cTWNLDMSISDN+"'";
      logger.info("Check_TWN_Msisdn_Status_B:"+sSql);
      
      Temprs=s2t.Query(sSql);
      
      while(Temprs.next()) {
         //TwnMsisdn=Temprs.getString("followmenumber");
    	  TwnMsisdn=Temprs.getString("ab");
      }
      logger.info("Count Result:"+TwnMsisdn);
      
      if(TwnMsisdn.equals("")) {
    	  TwnMsisdn="0";
      }
   } else {
	   TwnMsisdn="0";
   }
        
   return TwnMsisdn;
}

    public void Check_Type_Code_87_MAP_VALUE(String sServiceCode)throws SQLException{
      Temprs=null;
      sSql="select CUSTOMERTYPE from service where servicecode='"+sServiceCode+"'";
      logger.info("Check_Type_Code_87_MAP_VALUE:"+sSql);
      Temprs=s2t.Query(sSql);
      while (Temprs.next()){
           sM_CTYPE=Temprs.getString("CUSTOMERTYPE");
      }
      if (sM_CTYPE.equals("1")){sM_CTYPE="3";}
    }

    public int Check_Tag(String TempS) throws SQLException{
       int iCountT=0;
       Temprs=null;
       sSql="select count(CONTENT) as ab from PROVLOG"+
            //" where substr(CONTENT,29,length(CONTENT)-28)='"+TempS+"'";
               " where CONTENT like '%"+TempS+"%'";
       logger.debug("Check TAG:"+sSql);
       Temprs=s2t.Query(sSql);
       while (Temprs.next()){
         iCountT=Temprs.getInt("ab");
       }
       return iCountT;
    }

    public void Find_Old_ORDER_NBR() throws SQLException{
        sOld_SERVICE_ORDER_NBR="";
        sOld_WORK_ORDER_NBR="";
           Temprs=null;
       sSql="Select max(SERVICE_ORDER_NBR) as ab,max(WORK_ORDER_NBR) as cd,S2T_MSISDN from "+
       "S2T_TB_TYPB_WO_SYNC_FILE_DTL where subscr_id ='"+cTicketNumber+"' "+
               "group by S2T_MSISDN";
       logger.debug("Check Old_SERVICE_ORDER_NBR:"+sSql);
       Temprs=s2t.Query(sSql);
       while (Temprs.next()){
                sOld_SERVICE_ORDER_NBR=Temprs.getString("ab");
                sOld_WORK_ORDER_NBR=Temprs.getString("cd");
                cS2TMSISDN=Temprs.getString("S2T_MSISDN");
            }
    }

    public void Find_Old_step_no() throws SQLException{
        sOld_step_no="0";
       Temprs=null;
       sSql="select nvl(min(step_no),'0') as ab from S2T_TB_SERVICE_ORDER_ITEM "+
        "where SERVICE_ORDER_NBR="+sOld_SERVICE_ORDER_NBR+" and status <>'Y'";
       logger.debug("Check step_no:"+sSql);
       Temprs=s2t.Query(sSql);
       while (Temprs.next()){
                sOld_step_no=Temprs.getString("ab");
            }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    	
    	System.out.println(request.getParameter("Addon_Item"));
    	
        try {
            processRequest(request, response);
        } catch (SQLException ex) {
        	ErrorHandle(ex);
    	} catch (InterruptedException ex) {
    		ErrorHandle(ex);
		} catch (Exception ex) {
			ErrorHandle(ex);
		}
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected synchronized void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (SQLException ex) {
        	ErrorHandle(ex);
        } catch (InterruptedException ex) {
        	ErrorHandle(ex);
    	} catch (Exception ex) {
    		ErrorHandle(ex);
		}

    }


    /**
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
 public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
    
    private String setOTASMSPostParam(String msg,String phone) throws IOException{
		//StringBuffer sb=new StringBuffer ();

		String PhoneNumber=phone,Text=msg,charset="big5",InfoCharCounter=null,PID=null,DCS=null;
		String param =
				"PhoneNumber=+{{PhoneNumber}}&"
				+ "UNUSED=on&"
				+ "UDH=&"
				+ "Data=B30200F1&"
				+ "PID=7F&"
				+ "DCS=F6&"
				+ "Submit=Submit&"
				+ "Binary=1";
		
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

		return HttpPost("http://192.168.10.125:8800/Send Binary Message Other.htm",param,"");
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
