package com.esri.kura.example.flightsensor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FlightReader {
	private static final Logger s_logger = LoggerFactory.getLogger(FlightReader.class);
	private static String sampledata = "[{\"hex\":\"a8222c\",\"squawk\":\"0000\",\"flight\":\"\",\"lat\":0,\"lon\":0,\"validposition\":0,\"altitude\":17825,\"vert_rate\":1728,\"track\":0,\"validtrack\":0,\"speed\":0,\"messages\":37,\"seen\":2},{\"hex\":\"acd5ad\",\"squawk\":\"7464\",\"flight\":\"AAL1147 \",\"lat\":33.935715,\"lon\":-117.336002,\"validposition\":1,\"altitude\":14950,\"vert_rate\":-896,\"track\":265,\"validtrack\":1,\"speed\":410,\"messages\":124,\"seen\":0},{\"hex\":\"add66d\",\"squawk\":\"7240\",\"flight\":\"AAL1538 \",\"lat\":33.64428,\"lon\":-117.862644,\"validposition\":1,\"altitude\":16675,\"vert_rate\":1664,\"track\":90,\"validtrack\":1,\"speed\":401,\"messages\":63,\"seen\":21},{\"hex\":\"ad0851\",\"squawk\":\"0000\",\"flight\":\"\",\"lat\":0,\"lon\":0,\"validposition\":0,\"altitude\":24975,\"vert_rate\":1920,\"track\":119,\"validtrack\":1,\"speed\":421,\"messages\":36,\"seen\":0},{\"hex\":\"a01724\",\"squawk\":\"7264\",\"flight\":\"GTI3403 \",\"lat\":33.962511,\"lon\":-117.575455,\"validposition\":1,\"altitude\":8475,\"vert_rate\":3840,\"track\":138,\"validtrack\":1,\"speed\":279,\"messages\":395,\"seen\":0},{\"hex\":\"0d09a9\",\"squawk\":\"3276\",\"flight\":\"AMX789  \",\"lat\":33.396378,\"lon\":-117.689886,\"validposition\":1,\"altitude\":35000,\"vert_rate\":64,\"track\":147,\"validtrack\":1,\"speed\":455,\"messages\":794,\"seen\":210},{\"hex\":\"a638d5\",\"squawk\":\"0204\",\"flight\":\"N50NT   \",\"lat\":34.106963,\"lon\":-117.591419,\"validposition\":1,\"altitude\":1775,\"vert_rate\":0,\"track\":270,\"validtrack\":1,\"speed\":126,\"messages\":891,\"seen\":0}]";
	
	public static String getFilteredJson(String urlString) {
		Flight[] flights = getFlightsFromUrl(urlString);

		Flight[] filteredFlights = Arrays.stream(flights).filter(x->x.speed >0 && x.seen < 10).toArray(size -> new Flight[size]);
		
		try {
			ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writeValueAsString(filteredFlights);
			return json;
		} catch (JsonProcessingException e) {
			s_logger.error("Error getting filtered Json.  Url is " + urlString + ".  Error is " +e.getMessage());
		}
		return "";
	}
	
	public static Flight[] getFlightsFromUrl(String urlString) {
		try {
			URL url = new URL(urlString);
			Set<Flight> flights = fromJSON(new TypeReference<Set<Flight>>() {}, url);
			return flights.toArray(new Flight[flights.size()]);
		} catch (MalformedURLException e) {
			s_logger.error("Error getting flights from url.  Url is " + urlString + ".  Error is " +e.getMessage());
		}
		return null;
	}
	
	private static Flight[] getFlightsFromString(String str) {
		Set<Flight> flights = fromJSON(new TypeReference<Set<Flight>>() {}, str);
		return flights.toArray(new Flight[flights.size()]);
	}
	
	public static <T> T fromJSON(final TypeReference<T> type,
		      final String jsonPacket) {
		   T data = null;

		   try {
		      data = new ObjectMapper().readValue(jsonPacket, type);
		   } catch (Exception e) {
			   s_logger.error("Error converting JSON to Flight.  JSON string is " + jsonPacket + ".  Error is " +e.getMessage());
		   }
		   return data;
		}
	
	public static <T> T fromJSON(final TypeReference<T> type,
		      final URL urlString) {
		   T data = null;

		   try {
		      data = new ObjectMapper().readValue(urlString, type);
		   } catch (Exception e) {
			   s_logger.error("Error converting JSON to Flight from URL.  Url is " + urlString + ".  Error is " +e.getMessage());
		   }
		   return data;
		}

}
