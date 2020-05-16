package com.bn;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.GetUserRequest;
import com.amazonaws.services.cognitoidp.model.GetUserResult;
import com.amazonaws.services.cognitoidp.model.NotAuthorizedException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.DeleteObjectRequest;

public class DAO {

	private static DAO instance;
	
	private static Connection conn;
	
	private static ClasspathPropertiesFileCredentialsProvider creds;
	
	private static AWSCognitoIdentityProvider cognitoClient;
	
	private static AmazonS3 s3;
	private static String bucketName = "breakneckmaps";
	
	
	static
	{
		connectToDatabase();
		
		loadAWSCreds();
		
		cognitoClient = getAmazonCognitoIdentityClient();
		s3 = getAmazonS3();
	}
	
	public static DAO getInstance()
	{
		if( instance == null )
		{
			instance = new DAO();
		}
		
		return instance;
	}
	
	private DAO() {}
	
	private static void loadAWSCreds()
	{
		creds = new ClasspathPropertiesFileCredentialsProvider();
	}
	
	private static AmazonS3 getAmazonS3()
	{
		return AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1)
		.withCredentials(creds).build();
	}
	
	private static AWSCognitoIdentityProvider getAmazonCognitoIdentityClient() {
	       return AWSCognitoIdentityProviderClientBuilder.standard()
	                      .withCredentials(creds)
	                             .withRegion("us-east-1")
	                             .build();
	   }
	
	public GetUserResult VerifyUser( String accessToken )
	{
		GetUserRequest req = new GetUserRequest();
		req.setAccessToken(accessToken);
		
		try
		{
			GetUserResult outcome = cognitoClient.getUser(req);	
			System.out.println( "got user:!!!: " + outcome.getUsername() );
			
			return outcome;
		}
		catch( NotAuthorizedException e )
		{
			System.out.println( "get user request failed" );
		}
		
		return null;
	}
	
	private static void connectToDatabase()
	{
		try
		{
			conn = getConnection();
			System.out.println( "connection successful"); 
		}catch( Exception e) {System.out.println(e);}
		finally{System.out.println("connection attempt complete.");}
	}
	
	private static Properties readPropertiesFile(String fileName) throws IOException {
	      FileInputStream fis = null;
	      Properties prop = null;
	      try {
	    	  fis = new FileInputStream( DAO.class.getResource(fileName).getFile());//ClassLoader.getSystemResourceAsStream(fileName);//new FileInputStream(fileName);
	         //fis = new FileInputStream(fileName);
	         prop = new Properties();
	         prop.load(fis);
	      } catch(FileNotFoundException fnfe) {
	         fnfe.printStackTrace();
	      } catch(IOException ioe) {
	         ioe.printStackTrace();
	      } 
	      catch( Exception e )
	      {
	    	  System.out.println( e );
	      }
	      finally {
	    	  if( fis != null )
	    		  fis.close();
	      }
	      return prop;
	   }
	
	private static Connection getConnection() throws Exception
	{
		try
		{
			Properties prop = readPropertiesFile("DB.properties");
			if( prop == null )
			{
				System.out.println( "prop is null");
				
				System.out.println( "working dir is: " + Paths.get(".").toAbsolutePath().normalize().toString());
			}
			
			String driver = prop.getProperty("driver");
			String url = prop.getProperty("url");
			String username = prop.getProperty("username");
			String password = prop.getProperty("password");
			
			Class.forName(driver);
			
			//System.out.println( "driver: " + driver + ", url: " + url + ", user: " + username + ", pass: " + password );
			
			Connection tempConn = DriverManager.getConnection(url, username, password );
			System.out.println( "Connected");
			
			return tempConn;
		} catch( Exception e) {System.out.println(e);}
		
		
		return null;
	}
	
	public Map getMap( int id )
	{
		try
		{
			//SELECT COUNT(id) FROM table WHERE id = 123
			PreparedStatement sel = conn.prepareStatement(
					"SELECT * FROM maps WHERE id = " + id );
			
			ResultSet rs = sel.executeQuery();
			
			if( rs.next() )
			{
				Map m = new Map( rs.getInt("id"), rs.getString("mapname"), rs.getString("creatorname"));
				return m;
			}
		}
		catch( SQLException e )
		{
			System.out.println(e);
		}
		
		return null;
	}
	
	public List<Map> listAllMaps()
	{
		List<Map> maps = new ArrayList<>();
		try
		{			
			PreparedStatement sel = conn.prepareStatement(
					"SELECT * FROM maps" );
			ResultSet rs = sel.executeQuery();
			while( rs.next() ) 
			{
				//Map m = new Map( String name, String creatorName );//rs.getInt( "id"), rs.getString("first"), 42.0f);
				Map m = new Map( rs.getInt("id"), rs.getString("mapname"), rs.getString("creatorname") );
				maps.add( m );
			}
			rs.close();
			sel.close();
		}
		catch( SQLException e )
		{
			System.out.println(e);
		}
		
		
		return maps;
	}
	
	public void insertMap( Map m )
	{
		String statementStr = "insert into maps ( mapname, creatorname ) values (\"" + m.getName() 
		+ "\",\"" + m.getCreatorName() + "\")";
		
		try
		{
			PreparedStatement ins = conn.prepareStatement(statementStr);
			ins.executeUpdate();
		}
		catch( SQLException e )
		{
			System.out.println( e );
		}
		
		//"insert into maps ( mapname, creatorname ) values ("testmap", "test" )"
	}
	
	public boolean deleteMap( int id )
	{
		Map m = getMap( id );
		if( m != null )
		{
			String key = m.createKey();
			
			System.out.println( "key is: " + key );
			DeleteObjectRequest deleteReq = new DeleteObjectRequest(bucketName, key);
			boolean deleteSuccess = false;
			try
			{
				s3.deleteObject(deleteReq);
				deleteSuccess = true;
			}
			catch( Exception e )
			{
				System.out.println( "deletetion failed (S3) and threw an exception");
				System.out.println( e );
			}
			
			if( deleteSuccess )
			{
				String statementStr = "delete from maps where id=" + id;
				try
				{
					PreparedStatement ins = conn.prepareStatement(statementStr);
					ins.executeUpdate();
					System.out.println( "map deleted.");
					return true;
				}
				catch( SQLException e )
				{
					System.out.println( "deletetion failed on the db and threw an exception");
					System.out.println( e );
				}
			}
		}
		else
		{
			System.out.println( "map was not found.");	
		}
		
		return false;
	}
}
