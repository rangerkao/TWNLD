

public class Record {
	private String MNOIMSI;
	private String MNOMSISDN;
	private String ADDON_ACTION;
	
	
	public Record(){};
	
	public Record(String mNOIMSI, String mNOMSISDN, String aDDON_ACTION) {
		super();
		MNOIMSI = mNOIMSI;
		MNOMSISDN = mNOMSISDN;
		ADDON_ACTION = aDDON_ACTION;
	}
	
	
	public String getMNOIMSI() {
		return MNOIMSI;
	}
	public void setMNOIMSI(String mNOIMSI) {
		MNOIMSI = mNOIMSI;
	}
	public String getMNOMSISDN() {
		return MNOMSISDN;
	}
	public void setMNOMSISDN(String mNOMSISDN) {
		MNOMSISDN = mNOMSISDN;
	}
	public String getADDON_ACTION() {
		return ADDON_ACTION;
	}
	public void setADDON_ACTION(String aDDON_ACTION) {
		ADDON_ACTION = aDDON_ACTION;
	}
}
