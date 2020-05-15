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
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.GetUserRequest;
import com.amazonaws.services.cognitoidp.model.GetUserResult;
import com.amazonaws.services.cognitoidp.model.NotAuthorizedException;

public class DAO {

	private static DAO instance;
	private static Connection conn;
	private static String USER_POOL_ID;
	private static AWSCognitoIdentityProvider cognitoClient;
	//private static AmazonS3Client s3Client;
	
	static
	{
		//data.add( new Product( 1, "iPHone X", 999.99f));
		//data.add( new Product( 2, "XBOX 360", 329.50f));
		
		connect();
		
		USER_POOL_ID = "us-east-1_6v9AExXS8";
		cognitoClient = getAmazonCognitoIdentityClient();
		
		//VerifyUser();
		System.out.println("here we go");
		
		//AWSCognitoIdentityProvider cognitoClient = getAmazonCognitoIdentityClient();
		
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
	
	private static AWSCognitoIdentityProvider getAmazonCognitoIdentityClient() {
	      ClasspathPropertiesFileCredentialsProvider propertiesFileCredentialsProvider = 
	           new ClasspathPropertiesFileCredentialsProvider();
	       return AWSCognitoIdentityProviderClientBuilder.standard()
	                      .withCredentials(propertiesFileCredentialsProvider)
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
			//System.out.println( e );
		}
		
		return null;
	}
	
	private static void connect()
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
			
			System.out.println( "driver: " + driver + ", url: " + url + ", user: " + username + ", pass: " + password );
			
			Connection tempConn = DriverManager.getConnection(url, username, password );
			System.out.println( "Connected");
			
			return tempConn;
		} catch( Exception e) {System.out.println(e);}
		
		
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
	
	//return a result later
	public void deleteMap( Map m )
	{
		String statementStr = "delete from maps where mapname=\"" + m.getName() 
		+ "\" and creatorname=\"" + m.getCreatorName() + "\"";
		
		//System.out.println( "attempting to delete map " + m.getName() + " from user " + m.getCreatorName());
		try
		{
			PreparedStatement ins = conn.prepareStatement(statementStr);
			int res = ins.executeUpdate();
			
			if( res == 0 )
			{
				System.out.println( "map was not found.");	
			}
			else
			{
				System.out.println( "map deleted.");	
			}
		}
		catch( SQLException e )
		{
			System.out.println( e );
		}
	}
}
