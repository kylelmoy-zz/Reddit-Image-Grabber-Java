
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import com.eclipsesource.json.JsonObject;

public class RedditItem {
	final boolean useMySQL = false;
	final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	final String DB_URL = "jdbc:mysql://localhost:3306/images";
	final String USER = "user";
	final String PASS = "password";
	String u;
	String t;
	String a;
	String p;
	String s;
	String tag;

	public RedditItem(String url, JsonObject jsonObj) {
		u = url;
		t = jsonObj.get("title").asString();
		a = jsonObj.get("author").asString();
		p = jsonObj.get("permalink").asString();
		s = jsonObj.get("subreddit").asString().toLowerCase();
		t = tag.replaceAll("[^a-zA-Z0-9\\._]+", "_");
		if (jsonObj.get("url").asString().matches(".*imgur.com/a/.*")) {
			tag = t;
		} else if (t.contains("[")) {
			tag = t.split("\\[")[1].split("\\]")[0];
		}
	}

	public void get() throws Exception {
		String content = getContent();
		URL url = new URL(u);
		if (content.contains("image")) {
			String[] path = url.getFile().split("/");
			String name = path[path.length - 1];
			String type = "bmp";
			if (content.equals("image/jpeg"))
				type = "jpg";
			else if (content.equals("image/png"))
				type = "png";
			else if (content.equals("image/gif"))
				type = "gif";
			if (!name.contains(".gif") && !name.contains(".jpg")
					&& !name.contains(".png"))
				name += "." + type;
			File file;
			String folder = "";
			if (tag != null) {
				File directory = new File(s + "/" + tag + "/");
				if (!directory.exists())
					directory.mkdirs();
				folder += s + "/" + tag + "/";
			} else {
				folder += s + "/";
			}
			file = new File(folder + name);
			if (!file.exists()) {
				try {
					ReadableByteChannel rbc = Channels.newChannel(url
							.openStream());
					FileOutputStream fos = new FileOutputStream(file);
					fos.getChannel().transferFrom(rbc, 0, 1 << 24);
					fos.close();
					if (useMySQL) {
						mySQLWrite(file.toString());
					} else {
						PrintWriter out = new PrintWriter(new BufferedWriter(
								new FileWriter(s + "/manifest.txt", true)));
						out.println(file + "\t" + t + "\t" + a + "\t" + p
								+ "\t" + u);
						out.close();
					}
					System.out.println("DOWNLOADED: " + file.toString());
				} catch (Exception e) {
					System.err.println("FAILED: " + url.toString());
					e.printStackTrace();
				}
			}
		} else {
			PrintWriter out = new PrintWriter(new BufferedWriter(
					new FileWriter(s + "/unknown.txt", true)));
			out.println(u);
			out.close();
		}
	}

	private String getContent() {
		try {
			HttpURLConnection.setFollowRedirects(false);
			HttpURLConnection con = (HttpURLConnection) (new URL(u))
					.openConnection();
			con.setRequestMethod("HEAD");
			int i;
			for (i = 0; i < 10; i++) {
				String key = con.getHeaderFieldKey(i);
				if (key != null)
					if (key.equals("Content-Type"))
						break;
			}
			String response = con.getHeaderField(i);
			return response == null ? "NONE" : response;
		} catch (Exception e) {
			System.err.println("NO CONTENT TYPE FOUND");
			return "NONE";
		}
	}

	private void mySQLWrite(String file) {
		Connection conn = null;
		Statement stmt = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(DB_URL, USER, PASS);
			stmt = conn.createStatement();
			stmt.executeUpdate("INSERT INTO images (path, sub, title, author, permalink) VALUES ('" + file + "', '"
					+ s + "', '" + t + "', '" + a + "', '" + p + "')");
		} catch (SQLException se) {
			se.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (stmt != null)
					conn.close();
			} catch (SQLException se) {
			}
			try {
				if (conn != null)
					conn.close();
			} catch (SQLException se) {
				se.printStackTrace();
			}
		}
	}
}