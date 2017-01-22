package model;

import java.io.Serializable;

public class MsgToClient implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 8732229900412361057L;
	private String msg;
	private String from;
	
	public MsgToClient(String msg, String from){
		this.msg = msg;
		this.from = from;
	}
	public String getMsg() {
		return msg;
	}

	public String getFrom() {
		return from;
	}

	
}
