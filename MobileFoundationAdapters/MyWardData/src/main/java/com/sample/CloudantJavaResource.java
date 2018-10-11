/**
* Copyright 2017 IBM Corp.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.sample;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.lightcouch.NoDocumentException;

import com.cloudant.client.api.Database;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.ibm.mfp.adapter.api.AdaptersAPI;
import com.ibm.mfp.adapter.api.ConfigurationAPI;
import com.ibm.mfp.adapter.api.OAuthSecurity;

@Path("/")

public class CloudantJavaResource {
	/*
	 * For more info on JAX-RS see https://jax-rs-spec.java.net/nonav/2.0-rev-a/apidocs/index.html
	 */

	@Context
	AdaptersAPI adaptersAPI;

	private Database getDB() throws Exception {
		CloudantJavaApplication app = adaptersAPI.getJaxRsApplication(CloudantJavaApplication.class);
		if (app.db != null) {
			return app.db;
		}
		throw new Exception("Unable to connect to Cloudant DB, check the configuration.");
	}

	@POST
	@OAuthSecurity(scope = "UserLogin")
	@Path("/userLogin")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addEntry_ul(MyWardGrievance myWardGrievance) throws Exception {
		if (myWardGrievance != null && myWardGrievance.hasRequiredFields()) {
			getDB().save(myWardGrievance);
			return Response.ok().build();
		} else {
			return Response.status(400).build();
		}
	}
	@POST
	@OAuthSecurity(scope = "socialLogin")
	@Path("/socialLogin")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addEntry_sl(MyWardGrievance myWardGrievance) throws Exception {
		if (myWardGrievance != null && myWardGrievance.hasRequiredFields()) {
			getDB().save(myWardGrievance);
			return Response.ok().build();
		} else {
			return Response.status(400).build();
		}
	}

	@DELETE
	@Path("/{id}")
	public Response deleteEntry(@PathParam("id") String id) throws Exception {
		try {
			MyWardGrievance myWardGrievance = getDB().find(MyWardGrievance.class, id);
			getDB().remove(myWardGrievance);
			return Response.ok().build();
		} catch (NoDocumentException e) {
			return Response.status(404).build();
		}
	}

	@Context
	ConfigurationAPI configurationAPI;
	@GET
	@OAuthSecurity(scope = "socialLogin")
	@Path("/socialLogin")
	@Produces("application/json")
	public Response getAllEntries_sl(
			@QueryParam(value = "lat") double latitude,
			@QueryParam(value = "lon") double longitude,
			@QueryParam(value = "radius") double radius) 
			throws Exception {
		
		try {
			
			 StringBuilder sb = new StringBuilder();
			sb.append('/')
					.append(configurationAPI.getPropertyValue("DBName"))
					.append("/_design/geodd/_geo/geoidx");
					
			String geoPath = sb.toString();
			 
			 URI  uri = new URIBuilder()
					.setUserInfo(
							configurationAPI.getPropertyValue("key"),						
							configurationAPI.getPropertyValue("password"))						
					.setHost(configurationAPI.getPropertyValue("account")+".cloudant.com")
					.setScheme("https")
					.setPath(geoPath)
					.setParameter("radius",Double.toString(radius))
					.setParameter("lat",
							Double.toString(latitude))
					.setParameter("lon",
							Double.toString(longitude))
					.setParameter("include_docs", "true").build(); 
			
			
			
			HttpGet httpget = new HttpGet(uri);

			CloseableHttpClient httpClient = HttpClientBuilder.create().build();
			
			HttpResponse httpResponse = httpClient.execute(httpget);		
			

			if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				String jsonString = EntityUtils.toString(httpResponse
						.getEntity());
				
				List<MyWardGrievance> grievances = new ArrayList<MyWardGrievance>();
				Gson gson = new Gson();
			
				JsonArray array = new JsonParser().parse(jsonString)
						.getAsJsonObject().getAsJsonArray("rows");
				
				if (array.size() > 0) {
					for (JsonElement elem : array) {
						JsonElement e = elem.getAsJsonObject().get("doc");
						MyWardGrievance temp = gson.fromJson(e, MyWardGrievance.class);
						
						grievances.add(temp);
					}
				}
				
				return Response.ok(new Gson().toJson(grievances), MediaType.APPLICATION_JSON)
						.build();
				
			}

			httpClient.close();
			return Response.serverError().entity(httpResponse.getStatusLine())
					.build();
		} catch (Exception e) {
			e.printStackTrace();			
			return Response.serverError().entity("Error").build();

		}	
	
	}
	
	@GET
	@OAuthSecurity(scope = "UserLogin")
	@Path("/userLogin")
	@Produces("application/json")
	public Response getAllEntries_ul(
			@QueryParam(value = "lat") double latitude,
			@QueryParam(value = "lon") double longitude,
			@QueryParam(value = "radius") double radius) 
			throws Exception {
		
		try {
			
			 StringBuilder sb = new StringBuilder();
			sb.append('/')
					.append(configurationAPI.getPropertyValue("DBName"))
					.append("/_design/geodd/_geo/geoidx");
					
			String geoPath = sb.toString();
			 
			 URI  uri = new URIBuilder()
					.setUserInfo(
							configurationAPI.getPropertyValue("key"),						
							configurationAPI.getPropertyValue("password"))						
					.setHost(configurationAPI.getPropertyValue("account")+".cloudant.com")
					.setScheme("https")
					.setPath(geoPath)
					.setParameter("radius",Double.toString(radius))
					.setParameter("lat",
							Double.toString(latitude))
					.setParameter("lon",
							Double.toString(longitude))
					.setParameter("include_docs", "true").build(); 
			
		//	HttpGet httpget = new HttpGet(GeoRadiusURI.build(DEMO_LOCATION));	
			
			HttpGet httpget = new HttpGet(uri);

			CloseableHttpClient httpClient = HttpClientBuilder.create().build();
			
			HttpResponse httpResponse = httpClient.execute(httpget);		
			

			if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				String jsonString = EntityUtils.toString(httpResponse
						.getEntity());
				
				List<MyWardGrievance> grievances = new ArrayList<MyWardGrievance>();
				Gson gson = new Gson();
			
				JsonArray array = new JsonParser().parse(jsonString)
						.getAsJsonObject().getAsJsonArray("rows");
				
				if (array.size() > 0) {
					for (JsonElement elem : array) {
						JsonElement e = elem.getAsJsonObject().get("doc");
						MyWardGrievance temp = gson.fromJson(e, MyWardGrievance.class);
						
						grievances.add(temp);
					}
				}
				
				return Response.ok(new Gson().toJson(grievances), MediaType.APPLICATION_JSON)
						.build();
				
			}

			httpClient.close();
			return Response.serverError().entity(httpResponse.getStatusLine())
					.build();
		} catch (Exception e) {
			e.printStackTrace();			
			return Response.serverError().entity("Error").build();

		}
		
		
		
	//	List<MyWardGrievance> entries = getDB().view("_all_docs").includeDocs(true).query(MyWardGrievance.class);
	//	return Response.ok(entries).build();
	}

	@GET
	@OAuthSecurity(scope = "socialLogin")
	@Path("/socialLogin/objectStorage")
	@Produces("application/json")
	public Response getObjectStorageAccess_sl() throws Exception {
		CloudantJavaApplication app = adaptersAPI.getJaxRsApplication(CloudantJavaApplication.class);
		return Response.ok(app.getObjectStorageAccess()).build();
	}
	@GET
	@OAuthSecurity(scope = "UserLogin")
	@Path("/userLogin/objectStorage")
	@Produces("application/json")
	public Response getObjectStorageAccess_ul() throws Exception {
		CloudantJavaApplication app = adaptersAPI.getJaxRsApplication(CloudantJavaApplication.class);
		return Response.ok(app.getObjectStorageAccess()).build();
	}
}
