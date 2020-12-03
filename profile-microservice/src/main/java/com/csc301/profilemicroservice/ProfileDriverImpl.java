package com.csc301.profilemicroservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import org.springframework.stereotype.Repository;
import org.neo4j.driver.v1.Transaction;
import static org.neo4j.driver.v1.Values.parameters;


@Repository
public class ProfileDriverImpl implements ProfileDriver {

	Driver driver = ProfileMicroserviceApplication.driver;

	public static void InitProfileDb() {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.userName)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.password)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT nProfile.userName IS UNIQUE";
				trans.run(queryStr);

				trans.success();
			}
			session.close();
		}
	}
	
	/**
	 * Creates a user profile in the database
	 * 
	 * @param userName: user name of new profile
	 * @param fullName: full name of new profile
	 * @param password: password of new profile
	 * @return status of the query
	 */
	@Override
	public DbQueryStatus createUserProfile(String userName, String fullName, String password) {
		
		DbQueryExecResult ifSuccessful;
		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				String queryStr = "CREATE (nProfile:profile {userName: $userName, fullName: $fullName, password: $password})\n"
						+ "CREATE (nPlaylist:playlist {plName: $userName + \"-favourites\"})\n"
						+ "CREATE (nProfile)-[:created]->(nPlaylist)";
				
				//Running a query
				trans.run(queryStr, parameters("userName", userName, "fullName", fullName, "password", password));
				trans.success();
				ifSuccessful = DbQueryExecResult.QUERY_OK;
			}catch(Exception e) {
				//Exception occurred, query was unsuccessful
				ifSuccessful = DbQueryExecResult.QUERY_ERROR_GENERIC;
			}
			session.close();
		}
		
		DbQueryStatus status = new DbQueryStatus("create user profile", ifSuccessful);
		return status;
		
	}

	/**
	 * Adds a follow relation between user and a friend
	 * 
	 * @param userName: user name of user
	 * @param frndUserName: user name of a friend
	 * @return status of the query
	 */
	@Override
	public DbQueryStatus followFriend(String userName, String frndUserName) {
		
		DbQueryExecResult ifSuccessful;
		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				String queryStr = "MATCH(user:profile {userName: $userName})\n"
						+ "MATCH(friend:profile {userName: $frndUserName})\n"
						+ "CREATE(user)-[:follows]->(friend)\n"
						+ "RETURN COUNT(user) as userCount, COUNT(friend) as friendCount";
				
				//Running a query
				StatementResult res = trans.run(queryStr, parameters("userName", userName, "frndUserName", frndUserName));
				
				boolean not404;
				if (res.hasNext()) {
					Record rec = res.next();
					
					//Checking that user and his friend were found in database
					not404 = (long)rec.asMap().get("userCount") > 0 && (long)rec.asMap().get("friendCount") > 0;
				}else {
					//Response is empty, user or friend were not found
					not404 = false;
				}
				trans.success();
				
				ifSuccessful = not404 ? DbQueryExecResult.QUERY_OK : DbQueryExecResult.QUERY_ERROR_NOT_FOUND;
			}catch(Exception e) {
				//Exception occurred, query was unsuccessful
				ifSuccessful = DbQueryExecResult.QUERY_ERROR_GENERIC;
			}
			session.close();
		}
		
		DbQueryStatus status = new DbQueryStatus("follow a friend", ifSuccessful);
		return status;
		
	}

	/**
	 * Removes a follow relation between user and a friend
	 * 
	 * @param userName: user name of user
	 * @param frndUserName: user name of a friend
	 * @return status of the query
	 */
	@Override
	public DbQueryStatus unfollowFriend(String userName, String frndUserName) {
		
		DbQueryExecResult ifSuccessful;
		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				String queryStr = "MATCH(user:profile {userName: $userName})\n"
						+ "MATCH(friend:profile {userName: $frndUserName})\n"
						+ "MATCH(user)-[f:follows]->(friend)\n"
						+ "DELETE f \n"
						+ "RETURN COUNT(user) as userCount, COUNT(friend) as friendCount";
				
				//Running a query
				StatementResult res = trans.run(queryStr, parameters("userName", userName, "frndUserName", frndUserName));
				
				boolean not404;
				if (res.hasNext()) {
					Record rec = res.next();
					
					//Checking that user and his friend were found in database
					not404 = (long)rec.asMap().get("userCount") > 0 && (long)rec.asMap().get("friendCount") > 0;
				}else {
					//Response is empty, user or friend were not found
					not404 = false;
				}
				trans.success();
				
				ifSuccessful = not404 ? DbQueryExecResult.QUERY_OK : DbQueryExecResult.QUERY_ERROR_NOT_FOUND;
			}catch(Exception e) {
				//Exception occurred, query was unsuccessful
				ifSuccessful = DbQueryExecResult.QUERY_ERROR_GENERIC;
			}
			session.close();
		}
		
		DbQueryStatus status = new DbQueryStatus("unfollow a friend", ifSuccessful);
		return status;
		
	}

	/**
	 * Get all songs that friends of a user like
	 * 
	 * @param userName: user name of user
	 * @return status of the query and array of user names of friends and songs they like
	 */
	@Override
	public DbQueryStatus getAllSongFriendsLike(String userName) {
			
		DbQueryExecResult ifSuccessful;
		Object data;
		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				String queryStr = "MATCH (p:profile {userName: $userName})\n"
						+ "OPTIONAL MATCH (p)-[:follows]->(friend:profile)\n"
						+ "OPTIONAL MATCH (friend)-[:created]->(list:playlist {plName: friend.userName + \"-favourites\"})-[:contains]->(s: song)\n"
						+ "WITH friend.userName as name, COLLECT(s.songId) as songs\n"
						+ "RETURN COLLECT([name, songs]) as pairs";
				
				//Running a query
				StatementResult res = trans.run(queryStr, parameters("userName", userName));
				
				if (res.hasNext()) {
					//Query was successful, retrieve the data
					ifSuccessful = DbQueryExecResult.QUERY_OK;
					data = res.next().asMap().get("pairs");
				}else {
					//Result is empty, user was not found
					ifSuccessful = DbQueryExecResult.QUERY_ERROR_NOT_FOUND;
					data = null;
				}
				
			}catch(Exception e) {
				//Exception occurred, query was unsuccessful
				ifSuccessful = DbQueryExecResult.QUERY_ERROR_GENERIC;
				data = null;
			}
			session.close();
		}
		
		DbQueryStatus status = new DbQueryStatus("get all songs friends like", ifSuccessful);
		status.setData(data);
		return status;
		
	}
}
