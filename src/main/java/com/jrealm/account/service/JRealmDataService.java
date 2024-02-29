package com.jrealm.account.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpResponse;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jrealm.account.dto.LoginRequestDto;
import com.jrealm.account.dto.SessionTokenDto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class JRealmDataService {
	private static final transient ObjectMapper REQUEST_MAPPER = new ObjectMapper();
	private HttpClient httpClient;
	private String baseUrl;
	private String sessionToken;

	public <T> T executeDelete(String path, Class<T> responseClass) throws Exception {
		URI targetURI = new URI(this.baseUrl + path);
		HttpRequest.Builder httpRequest = HttpRequest.newBuilder().header("Content-Type", "application/json").uri(targetURI)
				.DELETE();

		HttpResponse<String> response = this.httpClient.send(httpRequest.build(), HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200)
			throw new IOException("Response was non 200.");

		return JRealmDataService.REQUEST_MAPPER.readValue(response.body(), responseClass);
	}

	public <T> T executePost(String path, Object object, Class<T> responseClass) throws Exception {
		URI targetURI = new URI(this.baseUrl + path);
		BodyPublisher body = HttpRequest.BodyPublishers
				.ofString(JRealmDataService.REQUEST_MAPPER.writeValueAsString(object));
		HttpRequest.Builder httpRequest = HttpRequest.newBuilder().header("Content-Type", "application/json").uri(targetURI)
				.POST(body);

		HttpResponse<String> response = this.httpClient.send(httpRequest.build(), HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200)
			throw new IOException("Response was non 200.");

		return JRealmDataService.REQUEST_MAPPER.readValue(response.body(), responseClass);
	}
	
	public <T> T executePut(String path, Object object, Class<T> responseClass) throws Exception {
		URI targetURI = new URI(this.baseUrl + path);
		BodyPublisher body = HttpRequest.BodyPublishers
				.ofString(JRealmDataService.REQUEST_MAPPER.writeValueAsString(object));
		HttpRequest.Builder httpRequest = HttpRequest.newBuilder().header("Content-Type", "application/json").uri(targetURI)
				.PUT(body);

		HttpResponse<String> response = this.httpClient.send(httpRequest.build(), HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200)
			throw new IOException("Response was non 200.");

		return JRealmDataService.REQUEST_MAPPER.readValue(response.body(), responseClass);
	}

	public String executeGet(String path, Map<String, String> queryParams) throws Exception {
		URI targetURI = new URI(this.baseUrl + path);
		HttpRequest.Builder httpRequest = HttpRequest.newBuilder().header("Content-Type", "application/json").uri(targetURI)
				.GET();
		HttpResponse<String> response = this.httpClient.send(httpRequest.build(), HttpResponse.BodyHandlers.ofString());
		// TODO: Add query params
		if (response.statusCode() != 200)
			throw new IOException("Response was non 200.");

		return response.body();
	}

	public <T> T executeGet(String path, Map<String, String> queryParams, Class<T> responseClass) throws Exception {
		URI targetURI = new URI(this.baseUrl + path);
		HttpRequest.Builder httpRequest = HttpRequest.newBuilder().header("Content-Type", "application/json").uri(targetURI)
				.GET();
		this.setAuth(httpRequest);
		HttpResponse<String> response = this.httpClient.send(httpRequest.build(), HttpResponse.BodyHandlers.ofString());
		// TODO: Add query params
		if (response.statusCode() != 200)
			throw new IOException("Response was non 200.");

		return JRealmDataService.REQUEST_MAPPER.readValue(response.body(), responseClass);
	}
	
	private void setAuth(HttpRequest.Builder builder) {
		if(this.sessionToken!=null) {
			builder.header("Authorization", this.sessionToken);
		}
	}

	public static void main(String[] args) {
		JRealmDataService service = new JRealmDataService(HttpClient.newHttpClient(), "http://localhost:8085/", null);
		LoginRequestDto login = new LoginRequestDto("ru-admin@jrealm.com", "password");
		try {
			final SessionTokenDto response = service.executePost("/admin/account/login", login, SessionTokenDto.class);
			System.out.println(response.getToken());
		} catch (Exception e) {
			System.out.println("Failed to login. " + e.getMessage());
		}
	}

}
