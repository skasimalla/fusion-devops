package com.as.FusionDevops;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class FusionDevops {
	public static void main(String args[]) {

		if (args != null && args.length == 2) {
			String serverNameColonPort = args[0];
			String userColonPass = args[1];
			createDump(serverNameColonPort, userColonPass);

		}


	}

	static String printScript(String userColonPass, String nameOfObject, String fusionEndPoint, String typeOfObject) {
		String s = "curl -k -u " + userColonPass + " -o jsons/" + nameOfObject + ".json \"" + fusionEndPoint
				+ "/api/apollo/objects/export?" + typeOfObject + ".ids=" + nameOfObject + "\"";

		System.out.println(s);
		return s;

	}

	public static void createDump(String serverNameColonPort, String userColonPass) {
		List<String> coll = getCollectionsList(serverNameColonPort, userColonPass);
		try {
			getAllFiles(serverNameColonPort, userColonPass, coll);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void printReport(String serverNameColonPort, String userColonPass) {
		HashMap<String, List> hm = getDataSourcesList(serverNameColonPort, userColonPass);
		for (String s : hm.keySet()) {
			List<String> li = hm.get(s);
			System.out.print(s);
			for (String s1 : li) {
				System.out.print("," + s1);
			}
			System.out.println();
		}
	}

	public static HashMap<String, List> getDataSourcesList(String serverNameColonPort, String userColonPass) {
		HashMap<String, List> hm = null;
		String collectionsJsonStr = getResponse(serverNameColonPort + "/api/apollo/connectors/datasources/",
				userColonPass);
		JSONParser jp = new JSONParser();
		JSONArray ja1 = null;
		try {
			ja1 = (JSONArray) jp.parse(collectionsJsonStr);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		hm = new HashMap<String, List>();
		for (int i = 0; i < ja1.size(); i++) {
			JSONObject jo = (JSONObject) ja1.get(i);
			List<String> li = new ArrayList<String>();
			String s1 = (String) jo.get("id");
			li.add((String) jo.get("type"));
			JSONObject jo1 = (JSONObject) jo.get("properties");
			li.add((String) jo1.get("collection"));
			li.add((String) jo.get("pipeline"));
			li.add((String) jo.get("description"));
			li.add(((String) jo.get("created")).substring(0, 10));
			hm.put(s1, li);
		}
		return hm;
	}

	public static List<String> getCollectionsList(String serverNameColonPort, String userColonPass) {
		String collectionsJsonStr = getResponse(serverNameColonPort + "/api/apollo/collections/", userColonPass);
		JSONParser jp = new JSONParser();
		JSONArray ja1 = null;
		try {
			ja1 = (JSONArray) jp.parse(collectionsJsonStr);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		HashMap<String, String> hm = new HashMap<String, String>();
		for (int i = 0; i < ja1.size(); i++) {
			JSONObject jo = (JSONObject) ja1.get(i);
			String s1 = (String) jo.get("id");
			String type = (String) jo.get("type");
			hm.put(s1, type);
		}
		List<String> li = new ArrayList<String>();
		for (String s : hm.keySet()) {
			if (hm.get(s).equals("DATA") && !s.startsWith("system_")) {
				// System.out.println(s + " " + hm.get(s));
				li.add(s);
			}
		}
		return li;
	}

	public static void getAllFiles(String serverNameColonPort, String userColonPass, List<?> collections)
			throws IOException {
		BufferedWriter statusReport = new BufferedWriter(new FileWriter("jsons/statusReport.csv", false));
		for (int i1 = 0; i1 < collections.size(); i1++) {
			// No appending
			String stringUrl = serverNameColonPort + "/api/apollo/connectors/datasources?collection="
					+ collections.get(i1);
			String str = getResponse(stringUrl, userColonPass);
			JSONParser jp = new JSONParser();
			JSONArray ja1 = null;
			try {
				ja1 = (JSONArray) jp.parse(str);
			} catch (ParseException e1) {
				e1.printStackTrace();
			}
			HashMap<String, String> hm = new HashMap<String, String>();
			HashMap<String, String> hm2 = new HashMap<String, String>();
			for (int i = 0; i < ja1.size(); i++) {
				JSONObject jo = (JSONObject) ja1.get(i);
				String s1 = (String) jo.get("pipeline");
				hm.put(s1, "");
				String s2 = (String) jo.get("id");// datasources
				hm2.put(s2, "");
			}
			// For writing datasources to file
			for (String s1 : hm2.keySet()) {
				PrintWriter out = null;
				try {
					out = new PrintWriter(new BufferedWriter(new FileWriter("jsons/" + s1 + ".json", false)));
				} catch (IOException e) {
					e.printStackTrace();
				} // No appending
				System.out.println("Exporting datasource " + collections.get(i1) + " > " + s1);
				statusReport.write(collections.get(i1) + "," + s1 + "\n");
				stringUrl = serverNameColonPort + "/api/apollo/connectors/datasources/" + s1;
				// System.out.println(stringUrl);
				String res = getResponse(stringUrl, userColonPass);
				out.print(res);
				out.close();
			}
			// For writing pipelines to file
			for (String s1 : hm.keySet()) {
				PrintWriter out = null;
				try {
					out = new PrintWriter(new BufferedWriter(new FileWriter("jsons/" + s1 + ".json", false)));
					System.out.println("Exporting pipeline " + collections.get(i1) + " > " + s1);
					statusReport.write(collections.get(i1) + "," + s1);
					stringUrl = serverNameColonPort + "/api/apollo/index-pipelines/" + s1;
					String res = getResponse(stringUrl, userColonPass);
					out.print(res);
				} catch (IOException e) {
					e.printStackTrace();
				} // No appending

				out.close();
			}

			// For writing Schema file
			PrintWriter out = null;
			try {
				String s1= (String) collections.get(i1);
				out = new PrintWriter(new BufferedWriter(new FileWriter("jsons/" + collections.get(i1) + "_schema.json", false)));
				System.out.println("Exporting schema " + collections.get(i1) + " > " + s1);
				statusReport.write(collections.get(i1) + "," + s1);
				stringUrl = serverNameColonPort + "/api/apollo/solr/"+ s1+"/schema";
				String res = getResponse(stringUrl, userColonPass);
				out.print(res);
			} catch (IOException e) {
				e.printStackTrace();
			} // No appending
			out.close();
			
			//Writing Solr config
			try {
				String s1= (String) collections.get(i1);
				out = new PrintWriter(new BufferedWriter(new FileWriter("jsons/" + collections.get(i1) + "_schema.json", false)));
				System.out.println("Exporting schema " + collections.get(i1) + " > " + s1);
				statusReport.write(collections.get(i1) + "," + s1);
				stringUrl = serverNameColonPort + "/api/apollo/solr/"+ s1+"/config";
				String res = getResponse(stringUrl, userColonPass);
				out.print(res);
			} catch (IOException e) {
				e.printStackTrace();
			} // No appending
			out.close();

			
		}
		statusReport.flush();
		statusReport.close();
	}

	public static String getResponse(String stringUrl, String userColonPass) {
		String theString = "";
		try {
			URL url = new URL(stringUrl);
			URLConnection uc = url.openConnection();
			uc.setRequestProperty("X-Requested-With", "Curl");
			String basicAuth = "Basic " + new String(new Base64().encode(userColonPass.getBytes()));
			uc.setRequestProperty("Authorization", basicAuth);
			theString = IOUtils.toString(uc.getInputStream(), "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return theString;
	}

}