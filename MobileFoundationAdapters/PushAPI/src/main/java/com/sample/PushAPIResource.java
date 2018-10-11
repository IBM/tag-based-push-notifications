/*
 *    Licensed Materials - Property of IBM
 *    5725-I43 (C) Copyright IBM Corp. 2015, 2016. All Rights Reserved.
 *    US Government Users Restricted Rights - Use, duplication or
 *    disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */

package com.sample;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import java.net.URL;
import java.net.HttpURLConnection;
import java.util.logging.Logger;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import org.json.*;
import com.ibm.mfp.adapter.api.ConfigurationAPI;
import com.ibm.mfp.adapter.api.OAuthSecurity;


@Path("/")
public class PushAPIResource {
	/*
	 * For more info on JAX-RS see
	 * https://jax-rs-spec.java.net/nonav/2.0-rev-a/apidocs/index.html
	 */

	// Define logger (Standard java.util.Logger)
	static Logger logger = Logger.getLogger(PushAPIResource.class.getName());

	// Inject the MFP configuration API:
	@Context
	ConfigurationAPI configApi;
	
	
	public String getToken() {
		// log message to server log
		logger.info("getToken");
		StringBuilder sb = new StringBuilder();		
		String baseUrl = configApi.getServerJNDIProperty("imfpush/mfp.push.authorization.server.url");
		logger.info(baseUrl);
        try {
        	
            URL url = new URL(baseUrl + "/az/v1/token");
            HttpURLConnection urlConnection = setUsernamePassword(url);
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
	} 
	
	private HttpURLConnection setUsernamePassword(URL url) throws IOException {
        HttpURLConnection urlConnection =(HttpURLConnection) url.openConnection();       
        String authStringEnc = "dGVzdDp0ZXN0"; //confidential client encoded.
        urlConnection.setRequestProperty("Authorization", "Basic " + authStringEnc);  
        urlConnection.setDoOutput(true);
        urlConnection.setRequestMethod("POST");
        urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        //set form parameters
        String postParameters = "grant_type=client_credentials&scope=messages.write tags.read tags.write push.application.org.mycity.myward";
        		urlConnection.setFixedLengthStreamingMode(postParameters.getBytes().length);
        		PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
        		out.print(postParameters);
        		out.close();
        return urlConnection;
    }
	

	
	
	public String getTags() {
		// log message to server log
		logger.info("getTags");

		StringBuilder sb = new StringBuilder();		
		String baseUrl = configApi.getServerJNDIProperty("mfpadmin/mfp.admin.push.url");			

        try {
        	
            URL url = new URL(baseUrl + "/v1/apps/org.mycity.myward/tags");       
            
            String tokenstr = getToken();       
            JSONObject jsonObj = new JSONObject(tokenstr);          
            String token = (String) jsonObj.get("access_token");           
            HttpURLConnection urlConnection =(HttpURLConnection) url.openConnection();       
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Authorization", "Bearer " + token);          
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

			logger.info("tags:"+ sb.toString());
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
	} 
	
	@POST
	@Path("/createTag/{tagname}")	
	@OAuthSecurity(enabled=false)
	public Boolean createTag(@PathParam("tagname") String tagname) {
		// log message to server log
		logger.info("createTag");
		logger.info(tagname);
		String tagNames[] = new String[10];
		
		String tags = getTags();
		JSONObject jsonObj = new JSONObject(tags);
		JSONArray jsonArray = jsonObj.getJSONArray("tags");
		for (int i = 0; i < jsonArray.length(); i++) {
		    JSONObject explrObject = jsonArray.getJSONObject(i);
		    logger.info((String) explrObject.get("name"));
		    tagNames[i] = (String) explrObject.get("name");
		    logger.info(tagNames[i]);
		}	
		
		for (String s: tagNames) {
			logger.info("inside for");
	        if (tagname.equals(s))
	            return true;
	    }
		
		 String tokenstr = getToken();       
         JSONObject jsonObject = new JSONObject(tokenstr);          
         String token = (String) jsonObject.get("access_token");                    
		 StringBuilder sb = new StringBuilder();
		
		String baseUrl = configApi.getServerJNDIProperty("mfpadmin/mfp.admin.push.url");			
        try {
        	
            URL url = new URL(baseUrl + "/v1/apps/org.mycity.myward/tags");    
            HttpURLConnection urlConnection =(HttpURLConnection) url.openConnection();       
            
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Authorization", "Bearer " + token);  
            urlConnection.setRequestProperty("Content-Type", "application/json");
            
            String input = "{\"description\":\"new tag\",\"name\":\""+tagname+ "\"}";
            logger.info(input);    
            OutputStream os = urlConnection.getOutputStream();
            os.write(input.getBytes());
            os.flush();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();           
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

		
	} 
	
	@POST
	@Path("/sendMessage")	
	@OAuthSecurity(enabled=false)
	public Boolean sendNotification(@QueryParam("tagnames") String tagnames,@QueryParam("description") String description) {
		// log message to server log
		logger.info("sendNotification");
		String tagList[] = new String[10];
		Boolean send = false;
		
		// get list of tags in server
		String tags = getTags();
		JSONObject jsonObj = new JSONObject(tags);
		JSONArray jsonArray = jsonObj.getJSONArray("tags");
		for (int i = 0; i < jsonArray.length(); i++) {
		    JSONObject explrObject = jsonArray.getJSONObject(i);		    
		    tagList[i] = (String) explrObject.get("name");
		    
		}	
		
		// tag names received by adapter 
		String newtags = tagnames.replace("\"","");
		 String newtags1 = newtags.replace("[","");
		String newtags2 = newtags1.replace("]","");
		String[] tagArray = newtags2.split(",");
		
		// check if any of the tags already present in server
		for (String s: tagList) {			
			for (String t: tagArray){							
	        if (t.equals(s))
	            send = true;	        	
			}		
	    }
		
		if (send)
		{
			logger.info("inside send");
			String tokenstr = getToken();       
			JSONObject jsonObject = new JSONObject(tokenstr);          
			String token = (String) jsonObject.get("access_token");  
		
			StringBuilder sb = new StringBuilder();
		
			String baseUrl = configApi.getServerJNDIProperty("mfpadmin/mfp.admin.push.url");			
			try {
        	
				URL url = new URL(baseUrl + "/v1/apps/org.mycity.myward/messages");    
				HttpURLConnection urlConnection =(HttpURLConnection) url.openConnection();       
            
				urlConnection.setDoOutput(true);
				urlConnection.setRequestMethod("POST");
				urlConnection.setRequestProperty("Authorization", "Bearer " + token);  
				urlConnection.setRequestProperty("Content-Type", "application/json");
           
            
				String input = "{\"message\":{\"alert\":\""+description+"\"},\"target\":{\"tagNames\":"+tagnames+"}}";
                
        
				logger.info(input);    
				OutputStream os = urlConnection.getOutputStream();
     
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
				writer.write(input);
				writer.close();
				os.close();            
            
            
				BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));
				String line;
				while ((line = reader.readLine()) != null) {
					sb.append(line);
				}
				reader.close();           
				return true;
			} catch (Exception e) {				 
				throw new RuntimeException(e);
			}
		}
		return true;


	}
	

	
	

}
