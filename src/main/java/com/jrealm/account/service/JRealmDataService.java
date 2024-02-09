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


	public <T> T executeDelete(String path, Class<T> responseClass) throws Exception {
		URI targetURI = new URI(this.baseUrl + path);
		HttpRequest httpRequest = HttpRequest.newBuilder().header("Content-Type", "application/json").uri(targetURI)
				.DELETE().build();

		HttpResponse<String> response = this.httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200)
			throw new IOException("Response was non 200.");

		return JRealmDataService.REQUEST_MAPPER.readValue(response.body(), responseClass);
	}

	public <T> T executePost(String path, Object object, Class<T> responseClass) throws Exception {
		URI targetURI = new URI(this.baseUrl + path);
		BodyPublisher body = HttpRequest.BodyPublishers
				.ofString(JRealmDataService.REQUEST_MAPPER.writeValueAsString(object));
		HttpRequest httpRequest = HttpRequest.newBuilder().header("Content-Type", "application/json").uri(targetURI)
				.POST(body).build();

		HttpResponse<String> response = this.httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200)
			throw new IOException("Response was non 200.");

		return JRealmDataService.REQUEST_MAPPER.readValue(response.body(), responseClass);
	}

	public <T> T executeGet(String path, Map<String, String> queryParams, Class<T> responseClass) throws Exception {
		URI targetURI = new URI(this.baseUrl + path);
		HttpRequest httpRequest = HttpRequest.newBuilder().header("Content-Type", "application/json").uri(targetURI)
				.GET().build();
		HttpResponse<String> response = this.httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
		// TODO: Add query params
		if (response.statusCode() != 200)
			throw new IOException("Response was non 200.");

		return JRealmDataService.REQUEST_MAPPER.readValue(response.body(), responseClass);
	}

	public static void main(String[] args) {
		JRealmDataService service = new JRealmDataService(HttpClient.newHttpClient(), "http://localhost:8085/");
		LoginRequestDto login = new LoginRequestDto("ru-admin@jrealm.com", "password");
		try {
			final SessionTokenDto response = service.executePost("/admin/account/login", login, SessionTokenDto.class);
			System.out.println(response.getToken());
		} catch (Exception e) {
			System.out.println("Failed to login. " + e.getMessage());
		}
	}

}
