

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

public class RedditHarvester {
	final static String CLIENT_ID = "client_id_here"; //Vindicator's Reddit Creepy Crawly
	static ArrayList<RedditItem> items = new ArrayList<RedditItem>();
	public static void harvest(String sub, String limit) throws Exception{
		System.out.println("Retrieving " + limit + " items from /r/" + sub);
		File directory = new File(sub);
		if (!directory.exists()) directory.mkdirs();
		URL subreddit = new URL ("http://www.reddit.com/r/" + sub + "/.json?limit=" + limit);
		InputStream in = subreddit.openStream();
		BufferedReader read = new BufferedReader(new InputStreamReader(in));
		String json = read.readLine();
		in.close();
		read.close();
		JsonObject jsonObj = JsonObject.readFrom(json);
		JsonObject data = jsonObj.get("data").asObject();
		JsonArray children = data.get("children").asArray();
		for (int i = 0; i < children.size(); i++) {
			JsonObject item = children.get(i).asObject().get("data").asObject();
			boolean isSelf = item.get("is_self").asBoolean();
			if (isSelf) continue;
			String url = item.get("url").asString();
			if (url.matches(".*imgur.com/a/.*")) { //Parse Album
				String newUrl = url.split("imgur.com/a/")[1];
				HttpURLConnection conn = (HttpURLConnection) (new URL("https://api.imgur.com/3/album/" + newUrl)).openConnection();
				conn.setRequestProperty("Authorization", "Client-ID " + CLIENT_ID);
				BufferedReader imgur = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				String imgurJson = imgur.readLine();
				imgur.close();
				JsonObject imgurJsonObj = JsonObject.readFrom(imgurJson);
				JsonObject imgurData = imgurJsonObj.get("data").asObject();
				JsonArray imgurImages = imgurData.get("images").asArray();
				for (int k = 0; k < imgurImages.size(); k++) {
					String link = imgurImages.get(k).asObject().get("link").asString();
					items.add(new RedditItem(link,item));
				}
			} else items.add(new RedditItem(url,item));
		}
		for (RedditItem i : items)
			i.get();
	}
	public static void main(String[] args) throws Exception{
		if (args.length < 2) throw new Error("Invalid Number of Arguments");
		System.out.println("Parsing");
		for (int i = 1; i < args.length; i++) {
			harvest(args[i],args[0]);
		}
		System.out.println("Terminated Successfully");
	}
}
