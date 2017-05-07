package service;

import entity.InstagramUser;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import util.DBHelper;
import util.Util;

import java.text.MessageFormat;
import java.util.*;

/**
 * Created by Mahmood on 4/5/2017.
 * mahmood.neshati@gmail.com
 */
public class InstagramService {
    private static InstagramService service;


    public static InstagramService getInstance() {

        if (service == null) {
            service = new InstagramService();
        }
        return service;
    }


    private ArrayList<InstagramUser> getFollowersEntity(String token) throws JSONException {
        String endPoint = "https://api.instagram.com/v1/users/self/followed-by?access_token=" + token;
        String serviceResult = Util.getInstance().callGetService(endPoint);
        JSONObject jsonObj = new JSONObject(serviceResult);
        return getUserList(jsonObj);
    }

    private ArrayList<InstagramUser> getUserList(JSONObject jsonObj) throws JSONException {
        ArrayList<InstagramUser> out = new ArrayList<>();
        JSONArray array = (JSONArray) jsonObj.get("data");
        for (int i = 0; i < array.length(); i++) {
            JSONObject userInfo = array.getJSONObject(i);
            InstagramUser iu = new InstagramUser(userInfo.get("username").toString(), userInfo.get("id").toString(), userInfo.get("full_name").toString(),null);
            out.add(iu);
        }
        return out;
    }


    public int registerNewUser(String userID, String token) {
        try {
            DBHelper.getInstance().insertNewUser(userID,token,"Unfollow");
            ArrayList<InstagramUser> followerEntity = getFollowersEntity(token);
            DBHelper.getInstance().updateFollowersTable(followerEntity, userID);
            return 1;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return 0;
    }


    public JSONObject UnfollowService(String userID, String token) {
        try {
            ArrayList<InstagramUser> newFollowerEntity = getFollowersEntity(token);
            ArrayList<InstagramUser> oldFollowerEntity = DBHelper.getInstance().selectFollowers(userID);
            HashSet<InstagramUser> newFollowers = getNewFollowers(newFollowerEntity, oldFollowerEntity);
            HashSet<InstagramUser> lostFollowers = getLostFollowers(newFollowerEntity, oldFollowerEntity);
            if (newFollowers.size() > 0) {
                DBHelper.getInstance().updateFollowersTable(new ArrayList<>(newFollowers), userID);
                System.out.println(newFollowers.toString());
            }
            if (lostFollowers.size() > 0) {
                DBHelper.getInstance().deleteFollowers(new ArrayList<>(lostFollowers), userID);
                System.out.println(lostFollowers.toString());
            }
            return generateMessage(newFollowers, lostFollowers);
        } catch (Exception ee) {
            ee.printStackTrace();
        }

        return null;
    }

    private JSONObject generateMessage(HashSet<InstagramUser> newFollowers, HashSet<InstagramUser> lostFollowers) {
        int lostCount = lostFollowers.size();
        int newCount = newFollowers.size();
        String message = "";

        if (lostCount == 0 && newCount == 0)
            message = generateWithoutChangeMessage();
        else if (lostCount == 0 && newCount > 0)
            message = generateNewFollower(newFollowers);
        else if (lostCount > 0 && newCount == 0)
            message = generateNewLoss(lostFollowers);
        else if (lostCount > 0 && newCount > 0)
            message = generateNewLossAndNewFollwer(lostFollowers, newFollowers);

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("message", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    private String generateNewLossAndNewFollwer(HashSet<InstagramUser> lostFollowers, HashSet<InstagramUser> newFollowers) {
        String newIds = getReportIds(newFollowers);
        String lossIds = getReportIds(lostFollowers);
        String template = "از آخرین باری که چک کردید، {0,number} نفر جدیدا شما رو فالو کرده اند. نام (های) کاربری:\n" +
                "<BR>\n" +
                "{1}\n" +
                "<BR>\n" +
                "در ضمن  {2,number} نفر جدیدا شما رو آنفالو کرده اند. نام (های) کاربری:\n" +
                "<BR>\n" +
                "{3}";
        return MessageFormat.format(template, newFollowers.size(), newIds, lostFollowers.size(), lossIds);
    }

    private String getReportIds(HashSet<InstagramUser> followerList) {
        ArrayList<InstagramUser> newlist = new ArrayList<>(followerList);
        int newCount = Math.min(5, newlist.size());
        StringBuilder newIds = new StringBuilder();
        for (int i = 0; i < newCount; i++) {
            newIds.append(newlist.get(i).getUsername()).append(" ,");
        }
        return newIds.substring(0, newIds.length() - 1);

    }


    private String generateNewLoss(HashSet<InstagramUser> lostFollowers) {
        ArrayList<InstagramUser> list = new ArrayList<>(lostFollowers);
        int count = Math.min(5, lostFollowers.size());
        StringBuilder Ids = new StringBuilder();
        for (int i = 0; i < count; i++) {
            Ids.append(list.get(i).getUsername()).append(" ,");
        }
        Ids = new StringBuilder(Ids.substring(0, Ids.length() - 1));
        String template = "از آخرین باری که چک کردید، {0,number} نفر جدیدا شما رو آنفالو کرده اند. نام (های) کاربری:\n" +
                "<BR>\n" +
                "{1}";
        return MessageFormat.format(template, lostFollowers.size(), Ids.toString());

    }

    private String generateNewFollower(HashSet<InstagramUser> newFollowers) {
        ArrayList<InstagramUser> list = new ArrayList<>(newFollowers);
        int count = Math.min(5, newFollowers.size());
        StringBuilder Ids = new StringBuilder();
        for (int i = 0; i < count; i++) {
            Ids.append(list.get(i).getUsername()).append(" ,");
        }
        Ids = new StringBuilder(Ids.substring(0, Ids.length() - 1));
        String template = "از آخرین باری که چک کردید، {0,number} نفر جدیدا شما رو فالو کرده اند. نام (های) کاربری:\n" +
                "<BR>\n" +
                "{1}";
        return MessageFormat.format(template, newFollowers.size(), Ids.toString());
    }

    private String generateWithoutChangeMessage() {
        return "از آخرین باری که چک کردید، کسی نه شما را فالو کرده نه آنفالو.";
    }

    private HashSet<InstagramUser> getLostFollowers(ArrayList<InstagramUser> newFollowerEntity, ArrayList<InstagramUser> oldFollowerEntity) {
        HashSet<InstagramUser> union = new HashSet<>();
        union.addAll(oldFollowerEntity);
        union.addAll(newFollowerEntity);
        union.removeAll(newFollowerEntity);
        return union;
    }

    private HashSet<InstagramUser> getNewFollowers(ArrayList<InstagramUser> newFollowerEntity, ArrayList<InstagramUser> oldFollowerEntity) {
        HashSet<InstagramUser> union = new HashSet<>();
        union.addAll(oldFollowerEntity);
        union.addAll(newFollowerEntity);
        union.removeAll(oldFollowerEntity);
        return union;
    }

    public JSONObject CloseFriendService(String token) throws JSONException {
        JSONObject object = new JSONObject();
        HashSet<String> user_recent_medias_id = getUserRecentMediaIds(token);
        HashMap<String, Double> likers_id = new HashMap<>();
        HashMap<String, Double> commenters_id= new HashMap<>();

        for (String media_id : user_recent_medias_id) {
            getLikersPoint(likers_id,media_id,token);
            getCommentersPoint(media_id,token,commenters_id);
        }
        HashMap<String,Double> user_points = addAllPointsTogether(likers_id,commenters_id);
        user_points = sortHashMapByValues(user_points);
        String message = generateCloseFriendMessage(user_points.keySet());
        object.put("message",message);
        return object;

    }

    private String generateCloseFriendMessage(Set<String> strings) {
        ArrayList<String> l = new ArrayList<>(strings);
        StringBuilder allUsers= new StringBuilder();
        for (String next : l) {
            allUsers.append(next).append(", ");
        }
        allUsers.trimToSize();
        String template = "با توجه به فعالیت های اخیر در اکانت شما، صمیمی ترین دوستان شما اینا هستند:\n" +
                "<BR>\n" +
                "{0}";
        return MessageFormat.format(template,allUsers.toString());

    }

    private HashMap<String, Double> sortHashMapByValues(
            HashMap<String, Double> passedMap) {
        List<String> mapKeys = new ArrayList<>(passedMap.keySet());
        List<Double> mapValues = new ArrayList<>(passedMap.values());
        Collections.sort(mapValues);
        Collections.sort(mapKeys);

        HashMap<String, Double> sortedMap =
                new HashMap<>();

        for (Double val : mapValues) {
            Iterator<String> keyIt = mapKeys.iterator();
            while (keyIt.hasNext()) {
                String key = keyIt.next();
                Double comp1 = passedMap.get(key);
                if (comp1.equals(val)) {
                    keyIt.remove();
                    sortedMap.put(key, val);
                    break;
                }
            }
        }
        return sortedMap;
    }

    private HashMap<String, Double> addAllPointsTogether(HashMap<String,Double> likers_id, HashMap<String,Double> commenters_id ) {
        HashMap<String,Double> user_points = new HashMap();
        for (String user_id : likers_id.keySet()){
            if (commenters_id.containsKey(user_id))
                user_points.put(user_id, likers_id.get(user_id) + commenters_id.get(user_id));
            else
                user_points.put(user_id, likers_id.get(user_id));
        }
        return user_points;
    }

    private void getCommentersPoint(String media_id,String token,HashMap commenters_id) {
        String endpoint = "https://api.instagram.com/v1/media/" + media_id + "/comments?access_token=" + token;
        String media_service = Util.getInstance().callGetService(endpoint);
        try {
            JSONObject media = new JSONObject(media_service);
            JSONArray data = ((JSONArray)(media.get("data")));
            for (int i = 0; i < data.length(); i++) {
                String username =  ((JSONObject) data.getJSONObject(i).get("from")).getString("username");
                if (!commenters_id.containsKey(username))
                    commenters_id.put(username, 1.25);
                else {
                    double old_cm_point = (double) commenters_id.get(username);
                    commenters_id.put(username, 1.25 + old_cm_point);
                }
            }
        } catch (JSONException e) {
            System.out.print("API does not return a response");
        }
    }

    private HashSet<String> getUserRecentMediaIds(String token) throws JSONException {
        String endpoint = "https://api.instagram.com/v1/users/self/media/recent/?access_token=" + token;
        HashSet<String> user_media_ids = new HashSet<>();
        String service_result = Util.getInstance().callGetService(endpoint);
        try {
            JSONObject json_obj = null;
            json_obj = new JSONObject(service_result);
            json_obj.getJSONArray("data");
            JSONArray data = ((JSONArray) (json_obj.get("data")));;
            for (int i = 0; i < data.length(); i++) {
                JSONObject objects = data.getJSONObject(i);
                Iterator key = objects.keys();
                while (key.hasNext()) {
                    String k = key.next().toString();
                    user_media_ids.add(objects.getString("id"));
                }
            }
            return user_media_ids;

        }catch (JSONException e) {
            System.out.println("There is no media!");
        }
        return user_media_ids;
    }


    private void getLikersPoint(HashMap<String, Double> likers_id,String media_id,String token) {
        String endpoint = "https://api.instagram.com/v1/media/" + media_id + "/likes?access_token=" + token;
        String media_service = Util.getInstance().callGetService(endpoint);
        try {
            JSONObject media = new JSONObject(media_service);
            JSONArray data = ((JSONArray)(media.get("data")));
            for (int i = 0; i < data.length(); i++) {
                String username = data.getJSONObject(i).getString("username");
                if ( likers_id.size() != 0 && likers_id.keySet().contains(username) )
                    likers_id.put( username, likers_id.get(username) + 1 );
                else
                    likers_id.put( username, 1.0 );

            }
        } catch (JSONException e) {
            System.out.print("API does not return a response");
        }
    }

}