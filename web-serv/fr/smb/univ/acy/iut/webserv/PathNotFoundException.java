package fr.smb.univ.acy.iut.webserv;

public class PathNotFoundException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public PathNotFoundException(String request) {
		super("The path of the following http request was not found : " + request);
	}
}
