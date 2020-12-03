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

	@Autowired
	public SongDalImpl(MongoTemplate mongoTemplate) {
		this.db = mongoTemplate;
	}

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

	@Override
	public DbQueryStatus getSongTitleById(String songId) {
		DbQueryStatus dbQueryStatus = findSongById(songId);
		
		if (dbQueryStatus.getData() != null && dbQueryStatus.getdbQueryExecResult() == DbQueryExecResult.QUERY_OK) {
			Song foundSong = (Song) dbQueryStatus.getData();
			dbQueryStatus.setData(foundSong.getSongName());
		}
		
		return dbQueryStatus;
	}

	@Override
	public DbQueryStatus deleteSongById(String songId) {
		DbQueryStatus dbQueryStatus;
		
		// get Song for id
		DbQueryStatus dbFindStatus = findSongById(songId);
		if (dbFindStatus.getData() == null) {
			// handle if id isnt valid
			dbQueryStatus = new DbQueryStatus("big L", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			return dbQueryStatus;
		}
		
		// once we have object - delete
		Song foundSong = (Song) dbFindStatus.getData();
		db.remove(foundSong, "songs");
		
		return new DbQueryStatus("succ", DbQueryExecResult.QUERY_OK);
	}

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