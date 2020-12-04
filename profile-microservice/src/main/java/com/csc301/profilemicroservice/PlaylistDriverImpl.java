package com.csc301.profilemicroservice;

import static org.neo4j.driver.v1.Values.parameters;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.springframework.stereotype.Repository;
import org.neo4j.driver.v1.Transaction;

@Repository
public class PlaylistDriverImpl implements PlaylistDriver {

	Driver driver = ProfileMicroserviceApplication.driver;

	public static void InitPlaylistDb() {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nPlaylist:playlist) ASSERT exists(nPlaylist.plName)";
				trans.run(queryStr);
				trans.success();
			}
			session.close();
		}
	}

	/**
	 * Like a song by a user, i.e. add a song to user's favourites.
	 * 
	 * @param userName: user that is liking the song
	 * @param songId: song that is being liked
	 * @return status of the query
	 */
	@Override
	public DbQueryStatus likeSong(String userName, String songId) {

		DbQueryExecResult ifSuccessful;
		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				String queryStr = 
						"MATCH (u:profile {userName: $userName})-[:created]->(p:playlist {plName: $userName + \"-favourites\"})\n"
						+ "MERGE(s:song {songId:$songId})\n"
						+ "MERGE(p)-[:includes]->(s)\n"
						+ "RETURN COUNT(u) as userCount, COUNT(p) as playlistCount";
				
				//Running a query
				StatementResult res = trans.run(queryStr, parameters("userName", userName, "songId", songId));
				
				boolean not404;
				if (res.hasNext()) {
					Record rec = res.next();
					
					//Check if user, playlist and song were all present in database 
					not404 = (long)rec.asMap().get("userCount") > 0 
							&& (long)rec.asMap().get("playlistCount") > 0;
				}else {
					//Empty response means that user, playlist or song is not found 
					not404 = false;
				}
				trans.success();
				
				ifSuccessful = not404 ? DbQueryExecResult.QUERY_OK : DbQueryExecResult.QUERY_ERROR_NOT_FOUND;
			}catch(Exception e) {
				//Error occurred, which means query was unsuccessful
				ifSuccessful = DbQueryExecResult.QUERY_ERROR_GENERIC;
			}
			session.close();
		}
		
		DbQueryStatus status = new DbQueryStatus("like a song", ifSuccessful);
		return status;
	}

	/**
	 * Unlike a song by a user, i.e. remove a to user's favourites.
	 * 
	 * @param userName: user that is unliking the song
	 * @param songId: song that is being unliked
	 * @return status of the query
	 */
	@Override
	public DbQueryStatus unlikeSong(String userName, String songId) {
		
		DbQueryExecResult ifSuccessful;
		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				String queryStr = "MATCH (u:profile {userName: $userName})-[:created]->(p:playlist {plName: $userName + \"-favourites\"})-[c:includes]->(s:song {songId: $songId})\n"
						+ "DETACH DELETE(c)\n"
						+ "RETURN COUNT(u) as userCount, COUNT(p) as playlistCount, COUNT(s) as songsCount";
				
				//Running a query
				StatementResult res = trans.run(queryStr, parameters("userName", userName, "songId", songId));
				
				boolean not404;
				if (res.hasNext()) {
					Record rec = res.next();
					
					//Check if user, playlist, song were all present in database and the song was liked before the query 
					not404 = (long)rec.asMap().get("userCount") > 0 
							&& (long)rec.asMap().get("playlistCount") > 0 
							&& (long)rec.asMap().get("songsCount") > 0 
							&& (long)rec.asMap().get("relationshipCount") > 0;
				}else {
					//Empty response means that user, playlist or song is not found 
					not404 = false;
				}
				trans.success();
				
				ifSuccessful = not404 ? DbQueryExecResult.QUERY_OK : DbQueryExecResult.QUERY_ERROR_NOT_FOUND;
			}catch(Exception e) {
				//Error occurred, which means query was unsuccessful
				ifSuccessful = DbQueryExecResult.QUERY_ERROR_GENERIC;
			}
			session.close();
		}
		
		DbQueryStatus status = new DbQueryStatus("unlike a song", ifSuccessful);
		return status;
		
	}

	/**
	 * Delete a song from database by its id.
	 * 
	 * @param songId: song that is being deleted
	 * @return status of the query
	 */
	@Override
	public DbQueryStatus deleteSongFromDb(String songId) {
		
		DbQueryExecResult ifSuccessful;
		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				String queryStr = "MATCH(s:song {songId: $songId})\n"
						+ "DETACH DELETE(s)\n"
						+ "RETURN COUNT(s) as songCount";
				StatementResult res = trans.run(queryStr, parameters("songId", songId));
				
				boolean not404;
				if (res.hasNext()) {
					Record rec = res.next();
					//Check if song was present in database before the query 
					not404 = (long)rec.asMap().get("songCount") > 0;
				}else {
					//Empty response means that user, playlist or song is not found 
					not404 = false;
				}
				trans.success();
				
				ifSuccessful = not404 ? DbQueryExecResult.QUERY_OK : DbQueryExecResult.QUERY_ERROR_NOT_FOUND;
			}catch(Exception e) {
				//Error occurred, which means query was unsuccessful
				ifSuccessful = DbQueryExecResult.QUERY_ERROR_GENERIC;
			}
			session.close();
		}
		
		DbQueryStatus status = new DbQueryStatus("delete a song", ifSuccessful);
		return status;

	}
}
