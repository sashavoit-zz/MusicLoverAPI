package com.csc301.songmicroservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/")
public class SongController {

	@Autowired
	private final SongDal songDal;

	private OkHttpClient client = new OkHttpClient();

	/**
	 * Construct SongController object.
	 * 
	 * @param  songDal  Data Access Layer object, used to interact with DB.  
	 */
	public SongController(SongDal songDal) {
		this.songDal = songDal;
	}

	
	/**
	 * Get all song data in database, which has id of songId.
	 * 
	 * @param songId   Id of song to find.
	 * @param request  Request sent to server.
	 * @return         Response sent to client.
	 */
	@RequestMapping(value = "/getSongById/{songId}", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getSongById(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("GET %s", Utils.getUrl(request)));

		DbQueryStatus dbQueryStatus = songDal.findSongById(songId);

		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());

		return response;
	}

	
	/**
	 * Get all song title in database, which has id of songId.
	 * 
	 * @param songId   Id of song to find.
	 * @param request  Request sent to server.
	 * @return         Response sent to client.
	 */
	@RequestMapping(value = "/getSongTitleById/{songId}", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getSongTitleById(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("GET %s", Utils.getUrl(request)));
		
		DbQueryStatus dbQueryStatus = songDal.getSongTitleById(songId);

		
		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());

		return response;
	}

	
	/**
	 * Delete song in database, which has id of songId.
	 * 
	 * @param songId   Id of song to delete.
	 * @param request  Request sent to server.
	 * @return         Response sent to client.
	 */
	@RequestMapping(value = "/deleteSongById/{songId}", method = RequestMethod.DELETE)
	public @ResponseBody Map<String, Object> deleteSongById(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("DELETE %s", Utils.getUrl(request)));
		
		DbQueryStatus dbQueryStatus = songDal.deleteSongById(songId);
		
		// if we deleted a song - remove it from all playlists as well
		if (dbQueryStatus.getdbQueryExecResult() == DbQueryExecResult.QUERY_OK) {
			Utils.deleteSongFromPlaylist(songId, client);
		}

		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());

		return response;
	}

	
	/**
	 * Add song to database.
	 * 
	 * @param params   Contains data fields for Song.
	 * @param request  Request sent to server.
	 * @return         Response sent to client.
	 */
	@RequestMapping(value = "/addSong", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addSong(@RequestParam Map<String, String> params,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("POST %s", Utils.getUrl(request)));

		DbQueryStatus dbQueryStatus; 
		
		// ensure "songName", "songArtistFullName", "songAlbum" are only fields and they arent emprty
		if (params.size() != 3) {
			// error
			dbQueryStatus = new DbQueryStatus("big L - not the right num of params", DbQueryExecResult.QUERY_ERROR_GENERIC);
		} else if (params.get(Song.KEY_SONG_NAME) == null || params.get(Song.KEY_SONG_ARTIST_FULL_NAME) == null || params.get(Song.KEY_SONG_ALBUM) == null) {
			// error
			dbQueryStatus = new DbQueryStatus("big L - missing required param", DbQueryExecResult.QUERY_ERROR_GENERIC);
		} else if (params.get(Song.KEY_SONG_NAME).isEmpty() || params.get(Song.KEY_SONG_ARTIST_FULL_NAME).isEmpty() || params.get(Song.KEY_SONG_ALBUM).isEmpty()) {
			// error
			dbQueryStatus = new DbQueryStatus("big L - required param is empty", DbQueryExecResult.QUERY_ERROR_GENERIC);
		} else {
			// call DAL class to insert song into DB
			Song songToAdd = new Song(params.get(Song.KEY_SONG_NAME), params.get(Song.KEY_SONG_ARTIST_FULL_NAME), params.get(Song.KEY_SONG_ALBUM));
			dbQueryStatus = songDal.addSong(songToAdd);
		}
		
		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());

		return response;
	}

	
	/**
	 * Update a song's favourite count, whose id is songId.
	 * 
	 * @param songId           Id of song for which to update.
	 * @param shouldDecrement  Determines wheter to increment or decrement.
	 * @param request          Request sent to server.
	 * @return                 Response sent to client.
	 */
	@RequestMapping(value = "/updateSongFavouritesCount/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> updateFavouritesCount(@PathVariable("songId") String songId,
			@RequestParam("shouldDecrement") String shouldDecrement, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("data", String.format("PUT %s", Utils.getUrl(request)));
		
		DbQueryStatus dbQueryStatus; 
		
		// validate that "shouldDecrement" is T/F only
		if (shouldDecrement.contentEquals("true") || shouldDecrement.contentEquals("false")) {
			boolean boolShouldDecrement = Boolean.parseBoolean(shouldDecrement);
			
			// call DB tell them to update it
			dbQueryStatus = songDal.updateSongFavouritesCount(songId, boolShouldDecrement);
		} else {
			dbQueryStatus = new DbQueryStatus("big L - shouldDecrement can only be true or false", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
		
		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());

		return response;
	}
}