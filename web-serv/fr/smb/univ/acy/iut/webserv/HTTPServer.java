package fr.smb.univ.acy.iut.webserv;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.Hashtable;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;

public class HTTPServer extends Server {
	
	private boolean debug;
	
	/* Customizable paths */
	
	/* default web root */
	private static final String WWW_DIR = "/home/www-java/www/";
	/* path to the script that handle spkac with openssl */
	private static final String script_path = "/home/www-java/scripts/handle_spkac.sh";
	/* Directory where we store spac requests */
	private static final String store_path = "/home/www-java/certs_req/"; /* same as csr store path ? */
	
	/* Allowed http methods */
	public enum Method {
		get,post;
	}
	
	/* Http codes */
	public enum HttpCode {
		HTTP_200("200 OK"),
		HTTP_400("400 Bad Request"),
		HTTP_404("404 Not Found"),
		HTTP_405("405 Method not Allowed"),
		HTTP_500("500 Internal Server Error");
		
		private final String value;
		
		private HttpCode(final String value) {
			this.value = value;
		}
		
		@Override
		public String toString() {
		 return this.value;
		}
	}
	
	/* basic response , easy to fill with variable */
	private static final String BASE_RESPONSE = "HTTP/1.1 {http_code}\n"
			+ "Content-Type: {mime_type}; charset=UTF-8\n"
			+ "Content-Length: {response_length}\n"
			+ "Connection: close\n"
			+ "\n"; 
	

	public HTTPServer(InetSocketAddress bindpoint, int timeout) throws IOException {
		super(bindpoint,timeout);
	}
	
	/* 
	 * Following methods retrieve informations about a request
	 */
	
	/* Parse the request , find the requested path and return it. Path must start with a '/'. */
	private String getPath(String request) throws PathNotFoundException {
		String path = "";
	
		try {
			path = request.split("\\\n")[0].split("\\s+")[1]; /* Get path from "GET <path> HTTPblabla" */
		} catch (Exception e) {
			throw new PathNotFoundException(request); /* If path is'nt found, the client request is malformed so we generate a 400 bad request */
		}
		
		/* if the path is '/' we have to redirect to the index.html page */
		if (path.equals("/")) {
			path = "/index.html";
			
		/* if path don't start with a '/' , the request is malformed or the algorithm failed to parse it. */
		} else if (!(path.startsWith("/"))) { 
			throw new PathNotFoundException(request);
		}
		
		if (this.debug) {
			System.out.println("[DEBUG]Parsed the path : " + path);
		}
		
		return path;	
	}
	
	/* Retrieve the POST parameters of a POST request. */
	private Hashtable <String, String> getPostParams(String req) {
		
		/*
		 * Post params are in the form "foo=1&bar=2" so to get every pair we split at '&' and then at '=' to get key and value.
		 * We put all the content within a hashtable , in the following format : {"foo":"1";"bar":"2"}
		 */
		
		Hashtable <String, String> params = new Hashtable <String, String>();
		
		String tab[] = req.split("\\\n"); /* Split every new lines */
		String paramLine = tab[tab.length-1]; /* The parameters line is the last one in a POST request. */
		
		if (this.debug) { 
			System.out.println("Param line:" + paramLine);		
		}

		/* Since every parameters is in the form "key=value" we can count the number of '=' to get the number of parameters. */
		int nbParams = paramLine.length() - paramLine.replace("=", "").length(); 
		
		/* if we got more than 1 parameters, we split them and put them in the hashtable */
		if (nbParams >  1) {
			String tab2[] = paramLine.split("\\&"); /* Split every "&" */
			
			for (int i = 0; i < nbParams; i++) {
				params.put(tab2[i].split("\\=")[0] ,tab2[i].split("\\=")[1]);
			}
		
		/* if we got only one, we don't have to split. */
		} else if (nbParams == 1){
			params.put(paramLine.split("\\=")[0] ,paramLine.split("\\=")[1]);
		} 
		
		if (this.debug) { 
			System.out.println("[DEBUG]Parsed post params : " + params);
		}
		
		return params;
	}
	
	/* Parse the request to find the HTTP method used. If the method is'nt allowed, throw a 405 method allowed error response. */
	private Method getMethod(String request) throws MethodNotAllowedException {
		/* Split spaces , the method should be the first string of all the request. */
		String  meth = request.split("\\s+")[0].toLowerCase(); 
		for (Method m : Method.values()) {
			if (m.name().equals(meth)) {
				return m; /* if the method is in the Method enum, we return it */
		    }
		}
		throw new MethodNotAllowedException(); /* Else the method is not allowed */
	}
	
	/* Handle requests */
	public byte[] handle(byte[] request) {
		
		this.debug = true;
		
		String req = new String(request);
		
		if (this.debug) {
			System.out.println("[DEBUG]New request :\n" + req);
		}
		
		Hashtable<String, String> postParams = new Hashtable<String, String>();
		//Hashtable<String, String> getParams = new Hashtable<String, String>(); /* Should be there, but we don't use it so why do it ? */
		Method method = null;
		
		try {
			method = this.getMethod(req);
		} catch (MethodNotAllowedException e1) {
			return this.makeResponse(HttpCode.HTTP_405.toString(), "405 HTTP Method is'nt allowed :-(", "text/plain").getBytes();
		}
		
		String path;
		String file_content = "";
		String response;
		
		if (method.equals(Method.post)) {
			postParams = this.getPostParams(req);
		}
		
		try {
			path = this.getPath(req);
		} catch (PathNotFoundException e) {
			/* Path is not found , so we send back a 400 http error */
			return this.makeResponse(HttpCode.HTTP_400.toString(), "400 Bad client request :-(", "text/plain").getBytes();
			
		} 
		
		if (this.debug) {
			System.out.println("[DEBUG]Is this a SPKAC request :");
			System.out.println("\t Method POST : " + method.equals(Method.post));
			System.out.println("\tSpkac : " + postParams.containsKey("spkac"));
			System.out.println("\tEmail : " + postParams.containsKey("email"));
			System.out.println("\tCN : " +  postParams.containsKey("cn"));
		}
		
		/* if the request is from the spkac form we get handle it and send it to the pki with the forms parameters */
		if (method.equals(Method.post) && postParams.containsKey("spkac") && postParams.containsKey("email") && postParams.containsKey("cn")) { /* the request is a generated key and we need to handle it to give it to the PKI */
			if (this.debug) {
				System.out.println("[DEBUG]Detected SPKAC request");
			}
			path = "/done.html"; /* if spkac is handled successfully we sent the done.html page to the user. */
			this.spkac(postParams.get("spkac"), postParams.get("email"), postParams.get("cn")); 
		}
		
		try {
			file_content = this.getFileContent(path);
		} catch (FileNotFoundException e) {
			/* File is not found, so it's a 404 http error */
			return this.makeResponse(HttpCode.HTTP_404.toString(), "404 File was not found :-(", "text/plain").getBytes();
			
		} catch (InternalServerErrorException e) {
			/* An error happened while opening , reading a file, etc so the content can't be send back */
			return this.makeResponse(HttpCode.HTTP_500.toString(), "500 Internal Server Error : Server experienced an unexpected error while handling your request  :-(", "text/plain").getBytes();
		
		} 
		
		/* Everything is allright and we can send back the requested content */
		response = this.makeResponse(HttpCode.HTTP_200.toString(), file_content, "text/html");
		return response.getBytes();
	}
	
	/* Fill the basic response with given parameters */
	private String makeResponse(String http_code, String content, String mime_type) {
		String response = HTTPServer.BASE_RESPONSE;
		response = response.replaceAll("\\{http_code}", http_code);
		response = response.replaceAll("\\{mime_type}", mime_type);
		response += content + "\n";
		response = response.replaceAll("\\{response_length}", Integer.toString(response.length()));
		return response;
	}
	
	

	/* Utils methods */
	private String getFileContent(String path) throws FileNotFoundException, InternalServerErrorException {
		/* Simply read the content of a file and return it.*/
		if (this.debug) {
			System.out.println("[DEBUG]Getting the content of the file " + path);
		}
		
		String file_content = "";
		String full_path = HTTPServer.WWW_DIR + path;
		FileReader fr = null;
		BufferedReader br = null;
		try {
			fr = new FileReader(full_path);
			 br = new BufferedReader(fr);
			
			do { /* We must do this with a "do .. while" because otherwise br.ready is'nt initialized */
				file_content += br.readLine() + "\n";
				
			} while (br.ready());
			
		} catch (FileNotFoundException e) {
			throw new FileNotFoundException(full_path); /* We must relay the exception to generate a 404 not found */
		} catch (IOException e) {
			System.out.println("IOException in HTTPServer.getFileContent, reading a file");
			throw new InternalServerErrorException();
		} finally {
			if (fr != null)  {
				try {
					fr.close();
				} catch (IOException e) {
						System.out.println("IOException in HTTPServer.getFileContent , closing a FileReader");
				}
			}
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					System.out.println("IOException in HTTPServer.getFileContent , closing a BufferedReader");
				}
			}
		}
		
		return file_content;
	}
	
	private void writeToFile(String file_path ,String content) {
		
        BufferedWriter br = null;
        try {
            File file = new File(file_path);
            br = new BufferedWriter(new FileWriter(file));
            br.write(content);
        } catch ( IOException e ) {
        	e.printStackTrace();
        } finally {
          if ( br != null ) {
            try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
          }
        }
	}
	
	/* generates a random name with the character in charset , and of the length specified in parameter. */
	private String randomName(int length) {
		
        String charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        String name = "";
        Random rnd = new Random();
        
        for (int i = 0; i < length; i++) {
            int index = rnd.nextInt() % charset.length();
            
            if (index < 0) { /* result of modulo can be negative */
            	index += charset.length();
            }
            
            name += charset.charAt(index);
        }
        
        return name;

    }

	private String urlDecodeString(String str)
	    {
	
	    String urlString = "";
	    try {
	        urlString = URLDecoder.decode(str,"UTF-8");
	        } catch (UnsupportedEncodingException e) {
	        	e.printStackTrace();
	        }
	    
	     return urlString;
	}


	/* Write the spkac to a file and call the pki handling script , giving that file parameters */
	private void spkac(String spkac, String email, String cn) {
		
		/* generate a random filename to handle multiple spkac requests if needed */
		String filename = randomName(16);
		/* build the content to write to the file for the pki */
		spkac = "SPKAC=" + this.urlDecodeString(spkac).replaceAll("\\r|\\n", "") + "\n";
		spkac += "C=FR\n";
		spkac += "ST=Auvergne-Rhone-Alpes\n";
		spkac += "L=Annecy\n";
		spkac += "O=IUT\n";
		spkac += "OU=RT\n";
		spkac += "CN=" + this.urlDecodeString(cn) + "\n";
		
		/* Parameters sent via http are url encoded , we need to decode them. */
		email = this.urlDecodeString(email);
		
		String cmd[] = {HTTPServer.script_path,  filename , email};
		
		if (this.debug) {
			System.out.println("[DEBUG]URL-Decoded SPKAC : " + spkac);
			System.out.println("[DEBUG]URL-Decoded email : " + email);
		}
		
		
		this.writeToFile(HTTPServer.store_path + filename + ".csr", spkac); /* Save the spkac  to a random file and give the filename to our pki script */
		
		
		if (this.debug) {
			System.out.println("[DEBUG]Saved SPKAC to the file  " + HTTPServer.store_path+filename+".spkac");
		}
		
		Process proc = null;
		
		
	    try {
	    	System.out.println("[DEBUG]Executing the command \" " + String.join(" ", cmd) + "\"");
	        proc = Runtime.getRuntime().exec(cmd);
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	    
		
		if (this.debug) { 
			System.out.println("[DEBUG]Handled spkac successfully :-)");
		}
	}
	
	/* Main method */
	public static void main(String[] args) {
		
		try {
			InetSocketAddress address = new InetSocketAddress(Inet4Address.getByName("10.102.74.132"), 8080);
			System.out.println("Bind address : " + address);
			new HTTPServer(address,0);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
}
