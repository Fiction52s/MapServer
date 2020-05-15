package com.bn;

import java.net.URISyntaxException;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.amazonaws.services.cognitoidp.model.GetUserResult;

@Path( "/maps")
public class MapResource {

	private DAO dao = DAO.getInstance();
	
	private String getAccessToken(HttpHeaders headers)
	{
		try
		{
			String accessToken = headers.getRequestHeader("Session-Token").get(0);
			//System.out.println( "HEADER STUFF: " + accessToken);	
			
			return accessToken;
		}
		catch( Exception e )
		{
			System.out.println( "didnt find access token");
		}
		return null;
	}
	
	private String getVerifiedUsername( HttpHeaders headers )
	{
		GetUserResult res = getVerifiedUser( headers );
		if( res != null )
		{
			return res.getUsername();
		}
		
		return null;
	}
	
	private GetUserResult getVerifiedUser( HttpHeaders headers )
	{
		String accessToken = getAccessToken( headers );
		if( accessToken != null )
		{
			GetUserResult res = dao.VerifyUser(accessToken);
			if( res != null )
			{
				return res;
			}
		}
		
		return null;
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Map> list()
	{
		return dao.listAllMaps();
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response add( @Context HttpHeaders headers, Map map) throws URISyntaxException
	{	
		String username = getVerifiedUsername( headers );
		
		if( username != null )
		{
			map.setCreatorName(username);
			System.out.println( "inserting map: " + map.getName() + " by user: " + map.getCreatorName());
			dao.insertMap(map);
		}
		else
		{
			System.out.println( "tried to upload map " + map.getName() + " as invalid user");
		}
		
		
		//int newProductId = dao.add( map );
		//URI uri = new URI("/MyWebsite/rest/products/" + newProductId );
		//return Response.created(uri).build();
		return Response.ok().build();
	}
	
	@GET
	@Path( "{id}")
	public Response get( @PathParam("id") int id )
	{
		return Response.status( Response.Status.NOT_FOUND).build();
//		Product product = dao.get(id);
//		if( product != null )
//		{
//			return Response.ok(product, MediaType.APPLICATION_JSON).build();
//		}
//		else
//		{
//			return Response.status( Response.Status.NOT_FOUND).build();
//		}
	}
	
	@PUT
	@Consumes( MediaType.APPLICATION_JSON)
	@Path( "{id}")
	public Response update( @PathParam("id") int id, Map map)
	{
		return Response.ok().build();
//		product.setId(id);
//		if( dao.update( product ))
//		{
//			return Response.ok().build();
//		}
//		else
//		{
//			return Response.notModified().build();
//		}
	}
	
	
	
	@DELETE
	@Consumes(MediaType.APPLICATION_JSON)
	//@Path("{id}")
	public Response delete( @Context HttpHeaders headers, Map map )
	{
		String username = getVerifiedUsername( headers );
		
		if( username != null )
		{
			map.setCreatorName(username);
			System.out.println( "attempting to deleting map: " + map.getName() + " by user: " + map.getCreatorName());
			dao.deleteMap(map);
			return Response.ok().build();
		}
		else
		{
			System.out.println( "tried to upload map as invalid user");
			
		}
		
		return Response.notModified().build();
	}
}
