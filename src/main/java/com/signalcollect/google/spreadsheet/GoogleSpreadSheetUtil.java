/*
 *  @author Daniel Strebel
 *  @author Philip Stutz
 *  
 *  Copyright 2012 University of Zurich
 *      
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *         http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  
 */
package com.signalcollect.google.spreadsheet;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.CustomElementCollection;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;

public class GoogleSpreadSheetUtil {

	private SpreadsheetService service;

	public GoogleSpreadSheetUtil() throws Exception {
		service = new SpreadsheetService("Spreadsheet Submitter");
		authenticateSpreadsheetService();

	}

	private void authenticateSpreadsheetService() {
		String accessToken = new OAuth2().getAccessToken();
		service.setHeader("Authorization", "Bearer " + accessToken);
	}

	public void addDataToSpreadsheet(String spreadsheetName, String worksheetName,
			Map<String, String> data) {
		addDataToSpreadsheet(spreadsheetName, worksheetName, data, 0);
	}

	private void addDataToSpreadsheet(String spreadsheetName, String worksheetName,
			Map<String, String> data, int attempt) {
		try {
			URL metafeedUrl = new URL(
					"https://spreadsheets.google.com/feeds/spreadsheets/private/full");
			SpreadsheetFeed feed = service.getFeed(metafeedUrl,
					SpreadsheetFeed.class);
			SpreadsheetEntry queryResult = null;
			
			for(SpreadsheetEntry entry: feed.getEntries()) {
				if(entry.getTitle().getPlainText().equalsIgnoreCase(spreadsheetName)) {
					queryResult = entry;
					break;
				}
			}
			
			if (queryResult == null) {
				System.err.println("No spreadsheet with name: " + spreadsheetName + " found!");
				return;
			}
			
			List<WorksheetEntry> worksheetEntries = queryResult.getWorksheets();
			WorksheetEntry worksheet = null;
			for(WorksheetEntry entry: worksheetEntries) {
				if(entry.getTitle().getPlainText().equalsIgnoreCase(worksheetName)) {
					worksheet = entry;
					break;
				}
			}
			
			if (worksheet == null) {
				System.err.println("No worksheet with name: " + worksheet + " found!");
				return;
			}
			
			
			ListEntry newRow = new ListEntry();
			CustomElementCollection customElements = newRow.getCustomElements();
			
			for (String columnName: data.keySet()) {
				customElements.setValueLocal(columnName, data.get(columnName));
			}
			
			service.insert(worksheet.getListFeedUrl(), newRow);
			System.out.println("Successfully wrote data to spreadsheet: " + spreadsheetName);

		} catch (Exception e) {
			if (attempt >= 3) {
				e.printStackTrace();
			} else {
				int timeout = 10 ^ attempt;
				System.out.println("Spreadsheet API retry in " + timeout
						+ " second");
				e.printStackTrace();
				try {
					Thread.sleep(timeout * 1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				System.out.println("Retrying.");
				addDataToSpreadsheet(spreadsheetName, worksheetName, data, attempt++);
			}
		}
	}

	public static void main(String[] args) {
		try {
			Map<String, String> test = new HashMap<String, String>();
			test.put("C1", "v1");
			new GoogleSpreadSheetUtil().addDataToSpreadsheet("test", "sheet1", test);
		} catch (Exception e) {
		 e.printStackTrace();
		}
	}
}
