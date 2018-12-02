package com.whereisevery1.database.model;

//import com.whereisevery1.database.model.Building;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;
import java.io.IOException;
import java.lang.Math;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.Select;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

//import java.io.FileWriter;
//import java.io.IOException;
//import org.json.simple.JSONArray;
//import org.json.simple.JSONObject;

/*
 * TODO: - Write the loading into database method
 * 		 - Parse time, days, rooms/buildings
 * 		 - Use the building class and room
 */
public class Scraping {

	// START - MUTABLE ATTRIBUTES
	private static HtmlUnitDriver driver;
	private static HashMap<String, Building> buildings;
	// END - MUTABLE ATTRIBUTES

	// START - IMMUTABLE ATTRIBUTES
	private final static String file = "/src/main/resources/catalog.txt";
	private final static String course_url = "https://my.sa.ucsb.edu/public/curriculum/coursesearch.aspx";
	private final static String courseListXPath = "//*[@id=\"ctl00_pageContent_courseList\"]";
	private final static String courseLevelXPath = "//*[@id=\"ctl00_pageContent_dropDownCourseLevels\"]";
	private final static String searchButtonXPath = "//*[@id=\"ctl00_pageContent_searchButton\"]";
	private final static String courseTableXPath = "//*[@id=\"aspnetForm\"]/table/tbody/tr[3]/td/div/center/table/tbody/tr";
	private final static String locationXPath = "//*[@id=\"aspnetForm\"]/table/tbody/tr[3]/td/div/center/table/tbody/tr[%d]/td[9]";
	private final static String daysXPath = "//*[@id=\"aspnetForm\"]/table/tbody/tr[3]/td/div/center/table/tbody/tr[%d]/td[7]";
	private final static String timesXPath = "//*[@id=\"aspnetForm\"]/table/tbody/tr[3]/td/div/center/table/tbody/tr[%d]/td[8]";
	private final static String allCourseLevels = "All";
	private final static String nonRooms = "T B A";
	// END - IMMUTABLE ATTRIBUTES

	public Scraping() {
		driver = new HtmlUnitDriver();
		driver.setJavascriptEnabled(true);
		driver.get(course_url);

		load_times_rooms_days(driver, get_subjectArea(driver));

		writeJSON();
	}

	/*
	 * parameters: driver -- HtmlUnitDriver object for scraping
	 */
	public ArrayList<String> get_subjectArea(HtmlUnitDriver driver) {
		Select s = new Select(driver.findElementByXPath(courseListXPath));
		ArrayList<String> temp = new ArrayList<String>();

		for (WebElement e : s.getOptions())
			temp.add(e.getText());

		return temp;
	}

	/*
	 * parameters: driver -- HtmlUnitDriver object for scraping courses --
	 * ArrayList<String> of courses already
	 */
	public void load_times_rooms_days(HtmlUnitDriver driver, ArrayList<String> courses) {

		/*
		 * TODO: Loop through each course and level Start with the one subject area Then
		 * move on to all subject areas and finally to all course levels add sleep()
		 * between courses
		 */

		// VARIABLE - DELIMITER TO PARSE DAY STRING, RANDOM DELAY FOR SCRAPING
		String delimiter = "(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)";
		double r = (Math.random() * ((60000 - 4000) + 1)) + 4000;

		for (String c : courses) {
			// grabs element id
			Select course_editbox = new Select(driver.findElementByXPath(courseListXPath));
			course_editbox.selectByVisibleText(c);

			// grabs course level and changes to ALL
			Select lvl_editbox = new Select(driver.findElementByXPath(courseLevelXPath));
			lvl_editbox.selectByVisibleText(allCourseLevels);

			// grabs element id and clicks
			WebElement search_button = driver.findElementByXPath(searchButtonXPath);
			search_button.click();

			for (int i = 1; i <= driver.findElements(By.xpath(courseTableXPath)).size(); i++) {
				String location = driver.findElement(By.xpath(String.format(locationXPath, i))).getText();

				String[] location_room = location.split(delimiter);

				if (location_room[0].compareTo(nonRooms) != 0) {
					if (!buildings.containsKey(location_room[0]))
						buildings.put(location, new Building(location_room[0]));

					buildings.get(location_room[0]).addToRoom(
							(location_room.length == 1) ? 0 : Integer.parseInt(location_room[1]),
							driver.findElement(By.xpath(String.format(daysXPath, i))).toString(),
							driver.findElement(By.xpath(String.format(timesXPath, i))).toString());

				}
			}

			// random delay
			try {
				Thread.sleep((long) r);
			} catch (Exception e) {
				// pass
			}
		}
	}

	public void writeJSON() {
		
		
		ObjectMapper mapper = new ObjectMapper();
		
		try {
			// Convert object to JSON string and save into a file directly
			mapper.writeValue(new File(file), this);

			// Convert object to JSON string
			String jsonInString = mapper.writeValueAsString(this);
			System.out.println(jsonInString);

			// Convert object to JSON string and pretty print
			jsonInString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
			System.out.println(jsonInString);

		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
//		JSONObject obj = new JSONObject();
//		obj.put("Name", "Catalog");
//		obj.put("LastScrape", "");
//
//		JSONArray buildingList = new JSONArray();
//		for (Building building : buildings.values()) {
//			buildingList.add("Building", building);
//
//			JSONArray roomList = new JSONArray();
//			for (Room room : building.getRooms().values()) {
//				roomList.add("Room", room);
//
//				JSONArray dayList = new JSONArray();
//				for (Day day : room.getTimes().values()) {
//					dayList.add("Day", day.value());
//
//					JSONArray timeList = new JSONArray();
//					for (Time time : day.getTimes()) {
//						timeList.add("Time", time);
//					}
//					obj.put("Time List", timeList);
//				}
//				obj.put("Day List", dayList);
//			}
//			obj.put("Room List", roomList);
//		}
//		obj.put("Building List", buildingList);
//
//		// try-with-resources statement based on post comment below :)
//		try (FileWriter data = new FileWriter(file)) {
//			data.write(obj.toJSONString());
//
//			// flag
//			System.out.println("Successfully Copied JSON Object to File...");
//			System.out.println("\nJSON Object: " + obj);
//		}
	}
}
