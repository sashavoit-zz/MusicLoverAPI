package com.csc301.songmicroservice;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.Map;

public class Utils {

	public static RequestBody emptyRequestBody = RequestBody.create(null, "");
	public static String PLAYLIST_MICROSERVICE_URL = "http://localhost:3002";
	public static String DELETE_ALL_SONGS_ENDPOINT = "/deleteAllSongsFromDb";
	
	// Used to determine path that was called from within each REST route, you don't need to modify this
	public static String getUrl(HttpServletRequest req) {
		String requestUrl = req.getRequestURL().toString();
		String queryString = req.getQueryString();

		if (queryString != null) {
			requestUrl += "?" + queryString;
		}
		return requestUrl;
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
	 * Delete song from user's favourite playlist.
	 * 
	 * @param songId  Id of song to delete.
	 * @param client  Client used for HTTP requests.
	 * @return        True if operation is successful, false otherwise.
	 */
	public static boolean deleteSongFromPlaylist(String songId, OkHttpClient client) {
		Request playlistRequest = new Request.Builder()
                .url(PLAYLIST_MICROSERVICE_URL + DELETE_ALL_SONGS_ENDPOINT + "/" + songId)
                .put(emptyRequestBody)
                .build();
		
		try {
			Response response = client.newCall(playlistRequest).execute();
			if (response.code() == 200) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}

		
	}
}