package com.csc301.songmicroservice;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class SongDalImpl implements SongDal {

	private final MongoTemplate db;


	/**
	 * Constructs SongDalImpl object.
	 * 
	 * @param  mongoTemplate  Spring's built in class for mongoDB operations 
	 */
	@Autowired
	public SongDalImpl(MongoTemplate mongoTemplate) {
		this.db = mongoTemplate;
	}

	
	/**
	 * Add a song to the database (duplicates are fine).
	 * 
	 * @param  songToAdd  Song that is to be added to DB.
	 * @return            DbQueryStatus with data about success or failure of add operation.
	 */
	@Override
	public DbQueryStatus addSong(Song songToAdd) {
		// assume songToAdd is properly formatted
		// add song to songs collection
		Song addedSong = db.insert(songToAdd, "songs");
		
		// validate query result by checking if song was added properly
		DbQueryStatus dbQueryStatus;
		if (addedSong.equals(songToAdd)) {
			dbQueryStatus = new DbQueryStatus("succ", DbQueryExecResult.QUERY_OK);
			dbQueryStatus.setData(addedSong);
		} else {
			dbQueryStatus = new DbQueryStatus("big L", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
		
		return dbQueryStatus;
	}

	
	/**
	 * Find Song in database that has songId.
	 * 
	 * @param  songId  Id of song which we want to find.
	 * @return         Return success and song data if found, else returns not found status.
	 */
	@Override
	public DbQueryStatus findSongById(String songId) {
		// find song
		Song foundSong = db.findById(songId, Song.class, "songs");
		
		// validate query result by checking if there was a result
		DbQueryStatus dbQueryStatus;
		if (foundSong != null) {
			dbQueryStatus = new DbQueryStatus("succ", DbQueryExecResult.QUERY_OK);
			dbQueryStatus.setData(foundSong);
		} else {
			dbQueryStatus = new DbQueryStatus("big L", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
		}
		
		return dbQueryStatus;
	}

	
	/**
	 * Find song title in database that has songId.
	 * 
	 * @param  songId  Id of song for which we want to get its title
	 * @return         Return success and song title if found, else returns not found status.
	 */
	@Override
	public DbQueryStatus getSongTitleById(String songId) {
		DbQueryStatus dbQueryStatus = findSongById(songId);
		
		if (dbQueryStatus.getData() != null && dbQueryStatus.getdbQueryExecResult() == DbQueryExecResult.QUERY_OK) {
			Song foundSong = (Song) dbQueryStatus.getData();
			dbQueryStatus.setData(foundSong.getSongName());
		}
		
		return dbQueryStatus;
	}

	
	/**
	 * Delete song from database.
	 * 
	 * @param  songId   Id of song which we want to delete.
	 * @return          Return success and if found and deleted, else returns not found status.
	 */
	@Override
	public DbQueryStatus deleteSongById(String songId) {
		DbQueryStatus dbQueryStatus;
		
		// get Song for id
		DbQueryStatus dbFindStatus = findSongById(songId);
		if (dbFindStatus.getData() == null || dbFindStatus.getdbQueryExecResult() != DbQueryExecResult.QUERY_OK) {
			// handle if id isnt valid
			dbQueryStatus = new DbQueryStatus("big L", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			return dbQueryStatus;
		}
		
		// once we have object - delete
		Song foundSong = (Song) dbFindStatus.getData();
		db.remove(foundSong, "songs");
		
		return new DbQueryStatus("succ", DbQueryExecResult.QUERY_OK);
	}


	/**
	 * Update song's AmountFavourites in database.
	 * 
	 * @param  songId           Id of song which we want to update amount favourites.
	 * @param  shouldDecrement  Update song's AmountFavourites by decrementing if true, else by incrementing.
	 * @return                  Return success and if found and updated, return error if trying to decrement below zero or
	 *                          returns not found status if song not in DB.
	 */
	@Override
	public DbQueryStatus updateSongFavouritesCount(String songId, boolean shouldDecrement) {
		// find song
		DbQueryStatus dbQueryStatus = findSongById(songId);
		
		if (dbQueryStatus.getData() != null && dbQueryStatus.getdbQueryExecResult() == DbQueryExecResult.QUERY_OK) {
			Song foundSong = (Song) dbQueryStatus.getData();
			
			// update amountFav
			long newAmountFav = foundSong.getSongAmountFavourites();
			if (shouldDecrement) {
				if (newAmountFav > 0) {
					newAmountFav--;
				} else {
					dbQueryStatus = new DbQueryStatus("big L - youre trying to decrement below 0", DbQueryExecResult.QUERY_ERROR_GENERIC);
					return dbQueryStatus;
				}
			} else {
				newAmountFav++;
			}
			
			// update song
			foundSong.setSongAmountFavourites(newAmountFav);
			
			// save it in DB
			db.save(foundSong, "songs");
			
			dbQueryStatus.setData(null);
		}
		
		return dbQueryStatus;
	}
}