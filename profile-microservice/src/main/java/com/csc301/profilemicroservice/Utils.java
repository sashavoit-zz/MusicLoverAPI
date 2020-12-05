package com.csc301.profilemicroservice;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.springframework.http.HttpStatus;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Utils {

	public static RequestBody emptyRequestBody = RequestBody.create(null, "");
		
	// Used to determine path that was called from within each REST route, you don't need to modify this
	public static String getUrl(HttpServletRequest req) {
		String requestUrl = req.getRequestURL().toString();
		String queryString = req.getQueryString();

		if (queryString != null) {
			requestUrl += "?" + queryString;
		}
		return requestUrl;
	}
	
	//Used to extract body from HttpServletRequest
	public static String getBody(HttpServletRequest req) {
		try {
			BufferedReader br = req.getReader();
			return br.lines().collect(Collectors.joining(System.lineSeparator()));
		} catch (IOException e) {
			// Shouldn't happen
			return null;
		}
	}
	
	// Sets the response status and data for a response from the server. You will not always be able to use this function
	public static Map<String, Object> setResponseStatus(Map<String, Object> response, DbQueryExecResult dbQueryExecResult, Object data) {	
		switch (dbQueryExecResult) {
		case QUERY_OK:
			response.put("status", HttpStatus.OK);
			if (data != null) {
				response.put("data", data);
			}
			break;
		case QUERY_ERROR_NOT_FOUND:
			response.put("status", HttpStatus.NOT_FOUND);
			break;
		case QUERY_ERROR_GENERIC:
			response.put("status", HttpStatus.INTERNAL_SERVER_ERROR);
			break;
		}
		
		return response;
	}
	
	/**
	 * Method to call song microservice to update song favourites count
	 * 
	 * @param client: okhttp client
	 * @param baseUrl: url of song microservice
	 * @param shouldDecrement: should count be decremented 
	 * @param songId: song's id
	 * @return status of the response
	 * @throws IOException
	 */
	public static int updateSongFavouritesCount(OkHttpClient client, String baseUrl, boolean shouldDecrement, String songId) throws IOException{
		//Setting up the url
		HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + "/updateSongFavouritesCount").newBuilder();
		urlBuilder.addPathSegment(songId);
		urlBuilder.addQueryParameter("shouldDecrement", Boolean.toString(shouldDecrement));
		String url = urlBuilder.build().toString();
		
		//Setting up the request
		Request request = new Request.Builder()
                .url(url)
                .put(emptyRequestBody)
                .build();
		
		//Calling the endpoint to upd favourite count
        Response response = client.newCall(request).execute();
        return response.code();
		
	}
	
	/**
	 * Method to call song microservice to get title of the song
	 * 
	 * @param client: okhttp client
	 * @param baseUrl: url of song microservice
	 * @param songId: song's id
	 * @return song's title; null if not found
	 * @throws IOException
	 */
	public static String getSondTitleById(OkHttpClient client, String baseUrl, String songId) throws IOException{
		//Setting up the url
		HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + "/getSongTitleById").newBuilder();
		urlBuilder.addPathSegment(songId);
		String url = urlBuilder.build().toString();
				
		//Setting up the request
		Request request = new Request.Builder()
                .url(url)
                .build();
		
		//Calling the endpoint to get song title
        Response response = client.newCall(request).execute();        
        JSONObject json = new JSONObject(response.body().string());
        if (!json.get("status").equals("OK")) {
        	//Song not found
        	return null;
        }
        
        return json.getString("data");
		
	}
	
	/**
	 * Method to convert mapping from friends to song's ids to mapping from friends to song's titles
	 * 
	 * @param client: okhttp client
	 * @param baseUrl: url of song microservice
	 * @param friendsToSongIds: mapping from friends to song's ids
	 * @return mapping from friends to song's titles
	 * @throws IOException
	 */
	public static Map<String, ArrayList<String>> convertSongIdsToSongTitles(OkHttpClient client, String baseUrl, Map<String, ArrayList<String>> friendsToSongIds) throws IOException{
		Map<String, ArrayList<String>> friendsToSongTitles = new HashMap<String, ArrayList<String>>();
		
		//Iterating over pairs (name, songId)
		for (String name : friendsToSongIds.keySet()) {
			friendsToSongTitles.put(name, new ArrayList<String>());
			for (String songId : friendsToSongIds.get(name)) {
				String title = getSondTitleById(client, baseUrl, songId);
				friendsToSongTitles.get(name).add(title);
			}
		}
		
		return friendsToSongTitles;
	}
	
	/**
	 * Method to check if song is present in song microservice db
	 * 
	 * @param client: okhttp client
	 * @param baseUrl: url of song microservice
	 * @param songId: song's id
	 * @return true if song with songId is present; false, otherwise
	 * @throws IOException
	 */
	public static boolean checkIfSongIsInSongMicroservice(OkHttpClient client, String baseUrl, String songId) throws IOException{
		String title = getSondTitleById(client, baseUrl, songId);
		return title != null;
	}
}