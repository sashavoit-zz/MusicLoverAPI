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

	@Override
	public DbQueryStatus likeSong(String userName, String songId) {

		DbQueryExecResult ifSuccessful;
		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				String queryStr = 
						"MATCH (u:profile {userName: $userName})-[:created]->(p:playlist {plName: $userName + \"-favourites\"})\n"
						+ "MATCH(s:song {songId: $songId})\n"
						+ "MERGE(p)-[:contains]->(s)\n"
						+ "RETURN COUNT(u) as userCount, COUNT(p) as playlistCount, COUNT(s) as songsCount";
				StatementResult res = trans.run(queryStr, parameters("userName", userName, "songId", songId));
				
				boolean not404;
				if (res.hasNext()) {
					Record rec = res.next();
					not404 = (long)rec.asMap().get("userCount") > 0 
							&& (long)rec.asMap().get("playlistCount") > 0 
							&& (long)rec.asMap().get("songsCount") > 0;
				}else {
					not404 = false;
				}
				trans.success();
				
				ifSuccessful = not404 ? DbQueryExecResult.QUERY_OK : DbQueryExecResult.QUERY_ERROR_NOT_FOUND;
			}catch(Exception e) {
				e.printStackTrace();
				ifSuccessful = DbQueryExecResult.QUERY_ERROR_GENERIC;
			}
			session.close();
		}
		
		DbQueryStatus status = new DbQueryStatus("like a song", ifSuccessful);
		return status;
	}

	@Override
	public DbQueryStatus unlikeSong(String userName, String songId) {
		
		DbQueryExecResult ifSuccessful;
		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				String queryStr = "MATCH (u:profile {userName: $userName})-[:created]->(p:playlist {plName: $userName + \"-favourites\"})-[c:contains]->(s:song {songId: $songId})\n"
						+ "DETACH DELETE(c)\n"
						+ "RETURN COUNT(u) as userCount, COUNT(p) as playlistCount, COUNT(s) as songsCount";
				StatementResult res = trans.run(queryStr, parameters("userName", userName, "songId", songId));
				
				boolean not404;
				if (res.hasNext()) {
					Record rec = res.next();
					not404 = (long)rec.asMap().get("userCount") > 0 
							&& (long)rec.asMap().get("playlistCount") > 0 
							&& (long)rec.asMap().get("songsCount") > 0 
							&& (long)rec.asMap().get("relationshipCount") > 0;
				}else {
					not404 = false;
				}
				trans.success();
				
				ifSuccessful = not404 ? DbQueryExecResult.QUERY_OK : DbQueryExecResult.QUERY_ERROR_NOT_FOUND;
			}catch(Exception e) {
				e.printStackTrace();
				ifSuccessful = DbQueryExecResult.QUERY_ERROR_GENERIC;
			}
			session.close();
		}
		
		DbQueryStatus status = new DbQueryStatus("unlike a song", ifSuccessful);
		return status;
		
	}

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
					not404 = (long)rec.asMap().get("songCount") > 0;
				}else {
					not404 = false;
				}
				trans.success();
				
				ifSuccessful = not404 ? DbQueryExecResult.QUERY_OK : DbQueryExecResult.QUERY_ERROR_NOT_FOUND;
			}catch(Exception e) {
				ifSuccessful = DbQueryExecResult.QUERY_ERROR_GENERIC;
			}
			session.close();
		}
		
		DbQueryStatus status = new DbQueryStatus("delete a song", ifSuccessful);
		return status;

	}
}
