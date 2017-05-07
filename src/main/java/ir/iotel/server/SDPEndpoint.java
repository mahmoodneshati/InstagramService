package ir.iotel.server;

/**
 * Created by Mahmood on 4/25/2017.
 * mahmood.neshati@gmail.com
 */


import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import service.InstagramService;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;


@Path("instagram")
public class SDPEndpoint {
   /* private static Properties prop = new Properties();
    private static String gold_endpoint;*/


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String checkGet() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("Message", "OK");
        return object.toString();
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registerService(JSONObject jsonObject) throws JSONException {
        String userID = jsonObject.getJSONObject("inputs").getString("id");
        String token = jsonObject.getJSONObject("inputs").getString("access_token");
        int result = InstagramService.getInstance().registerNewUser(userID, token);
        if (result == 1)
            return Response.status(200).build();
        else
            return Response.status(400).build();
    }

    @POST
    // The Java method will produce content identified by the MIME Media type "text/plain"
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/unFollowService")
    public String unFollowService(JSONObject unFollowYabMessage) throws IOException, JSONException {
        String userID = unFollowYabMessage.getString("id");
        String token = unFollowYabMessage.getString("access_token");
        JSONObject jsonObject = InstagramService.getInstance().UnfollowService(userID, token);
        return jsonObject.toString();
    }
    @POST
    // The Java method will produce content identified by the MIME Media type "text/plain"
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/closeFriend")
    public String closeFriendService(JSONObject closeFriendMessage) throws IOException, JSONException {
        String token = closeFriendMessage.getString("access_token");
        JSONObject jsonObject = InstagramService.getInstance().CloseFriendService(token);
        return jsonObject.toString();
    }



}