package model;

import java.io.Serializable;

public class MsgToServer implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -6741690047136197286L;
	private String msg;
	private String[] to;
	
	public MsgToServer(String msg, String[] to){
		this.msg = msg;
		this.to = to;
	}
	public String getMsg() {
		return msg;
	}

	public String[] getTo() {
		return to;
	}

}
