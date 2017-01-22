package model;

import java.io.Serializable;

public class MsgOnlineUsers implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7581346949113518123L;
	private String[] users;
	
	public MsgOnlineUsers(String[] users) {
		this.users = users;
	}
	
	public String[] getUsers() {
		return users;
	}
	
	@Override
	public String toString() {
		StringBuilder strBuilder = new StringBuilder("Online users message: ");
		for(String user : users) {
			strBuilder.append(user);
		}
		return strBuilder.toString();
	}
}
