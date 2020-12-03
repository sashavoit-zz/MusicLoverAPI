package com.csc301.profilemicroservice;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.csc301.profilemicroservice.Utils;
import com.csc301.profilemicroservice.ProfileDriverImpl;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/")
public class ProfileController {
	public static final String KEY_USER_NAME = "userName";
	public static final String KEY_USER_FULLNAME = "fullName";
	public static final String KEY_USER_PASSWORD = "password";

	@Autowired
	private final ProfileDriverImpl profileDriver;

	@Autowired
	private final PlaylistDriverImpl playlistDriver;

	OkHttpClient client = new OkHttpClient();

	public ProfileController(ProfileDriverImpl profileDriver, PlaylistDriverImpl playlistDriver) {
		this.profileDriver = profileDriver;
		this.playlistDriver = playlistDriver;
	}

	/**
	 * Performs POST request at route /profile to add profile to database.
	 * 
	 * @param params: parameters of the request
	 * @param request: body of the request
	 * @return status of the request
	 */
	@RequestMapping(value = "/profile", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addProfile(@RequestParam Map<String, String> params,
			HttpServletRequest request) {
		Map<String, Object> response = new HashMap<String, Object>();
		try {
			//Parsing json body
			JSONObject body = new JSONObject(Utils.getBody(request));
			String userName = body.getString(KEY_USER_NAME);
			String fullName = body.getString(KEY_USER_FULLNAME);
			String password = body.getString(KEY_USER_PASSWORD);
		
			DbQueryStatus status = profileDriver.createUserProfile(userName, fullName, password);
		
			//Adding status to the response
			response = Utils.setResponseStatus(response, status.getdbQueryExecResult(), status.getData());
			return response;
		}catch(Exception e) {
			//Exception occurred, request was unsuccessful 
			response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
			return response;
		}
	}

	/**
	 * Performs PUT request at route /followFriend/{userName}/{friendUserName} to follow a friend.
	 * 
	 * @param userName: user who is following a friend 
	 * @param friendUserName: friend who is being followed
	 * @param request: body of the request
	 * @return status of the request
	 */
	@RequestMapping(value = "/followFriend/{userName}/{friendUserName}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> followFriend(@PathVariable("userName") String userName,
			@PathVariable("friendUserName") String friendUserName, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		try {
			DbQueryStatus status = profileDriver.followFriend(userName, friendUserName);
		
			//Adding status to the response
			response = Utils.setResponseStatus(response, status.getdbQueryExecResult(), status.getData());

			return response;
		}catch(Exception e) {
			//Exception occurred, request was unsuccessful
			response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
			return response;
		}
		
	}
	
	/**
	 * Performs GET request at route /getAllFriendFavouriteSongTitles/{userName} to get all friends' favourite songs of the user.
	 * 
	 * @param userName: user
	 * @param request: body of the request
	 * @return status of the request and friends' favourite songs
	 */
	@RequestMapping(value = "/getAllFriendFavouriteSongTitles/{userName}", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getAllFriendFavouriteSongTitles(@PathVariable("userName") String userName,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		try {
			DbQueryStatus status = profileDriver.getAllSongFriendsLike(userName);
		
			//Adding status to the response
			response = Utils.setResponseStatus(response, status.getdbQueryExecResult(), status.getData());

			return response;
		}catch(Exception e) {
			//Exception occurred, request was unsuccessful
			response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
			return response;
		}
		
	}

	/**
	 * Performs PUT request at route /unfollowFriend/{userName}/{friendUserName} to unfollow a friend.
	 * 
	 * @param userName: user who is unfollowing a friend 
	 * @param friendUserName: friend who is being unfollowed
	 * @param request: body of the request
	 * @return status of the request
	 */
	@RequestMapping(value = "/unfollowFriend/{userName}/{friendUserName}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> unfollowFriend(@PathVariable("userName") String userName,
			@PathVariable("friendUserName") String friendUserName, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		try {
			DbQueryStatus status = profileDriver.unfollowFriend(userName, friendUserName);
		
			//Adding status to the response
			response = Utils.setResponseStatus(response, status.getdbQueryExecResult(), status.getData());

			return response;
		}catch(Exception e) {
			//Exception occurred, request was unsuccessful
			response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
			return response;
		}
		
	}

	/**
	 * Performs PUT request at route /likeSong/{userName}/{songId} to like a song.
	 * 
	 * @param userName: user who is liking a song 
	 * @param songId: id of a song that is being liked
	 * @param request: body of the request
	 * @return status of the request
	 */
	@RequestMapping(value = "/likeSong/{userName}/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> likeSong(@PathVariable("userName") String userName,
			@PathVariable("songId") String songId, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		try {
			DbQueryStatus status = playlistDriver.likeSong(userName, songId);
		
			//Adding status to the response
			response = Utils.setResponseStatus(response, status.getdbQueryExecResult(), status.getData());

			return response;
		}catch(Exception e) {
			//Exception occurred, request was unsuccessful
			response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
			return response;
		}
	}

	/**
	 * Performs PUT request at route /likeSong/{userName}/{songId} to unlike a song.
	 * 
	 * @param userName: user who is unliking a song 
	 * @param songId: id of a song that is being unliked
	 * @param request: body of the request
	 * @return status of the request
	 */
	@RequestMapping(value = "/unlikeSong/{userName}/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> unlikeSong(@PathVariable("userName") String userName,
			@PathVariable("songId") String songId, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		try {
			DbQueryStatus status = playlistDriver.unlikeSong(userName, songId);
		
			//Adding status to the response
			response = Utils.setResponseStatus(response, status.getdbQueryExecResult(), status.getData());

			return response;
		}catch(Exception e) {
			//Exception occurred, request was unsuccessful
			response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
			return response;
		}
	}

	/**
	 * Performs PUT request at route /deleteAllSongsFromDb/{songId} to delete a song from db.
	 * 
	 * @param songId: id of a song that is being deleted
	 * @param request: body of the request
	 * @return status of the request
	 */
	@RequestMapping(value = "/deleteAllSongsFromDb/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> deleteAllSongsFromDb(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		try {
			DbQueryStatus status = playlistDriver.deleteSongFromDb(songId);
		
			//Adding status to the response
			response = Utils.setResponseStatus(response, status.getdbQueryExecResult(), status.getData());

			return response;
		}catch(Exception e) {
			//Exception occurred, request was unsuccessful
			response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
			return response;
		}
	}
}