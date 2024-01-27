package com.jrealm.account.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jrealm.account.dto.LoginRequestDto;
import com.jrealm.account.dto.SessionTokenDto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ClientAuthService {
	private static final transient ObjectMapper REQUEST_MAPPER = new ObjectMapper();
	private HttpClient httpClient;
	private String baseUrl;

	public SessionTokenDto executePost(String path, Object object) throws Exception {
		URI targetURI = new URI(this.baseUrl + path);
		BodyPublisher body = HttpRequest.BodyPublishers
				.ofString(ClientAuthService.REQUEST_MAPPER.writeValueAsString(object));
		HttpRequest httpRequest = HttpRequest.newBuilder().header("Content-Type", "application/json").uri(targetURI)
				.POST(body).build();

		HttpResponse<String> response = this.httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() != 200)
			throw new IOException("Response was non 200.");

		return ClientAuthService.REQUEST_MAPPER.readValue(response.body(), SessionTokenDto.class);
	}

	public static void main(String[] args) {
		ClientAuthService service = new ClientAuthService(HttpClient.newHttpClient(), "http://localhost:8085/");
		LoginRequestDto login = new LoginRequestDto("ru-admin@jrealm.com", "password");
		try {
			final SessionTokenDto response = service.executePost("/admin/account/login", login);
			System.out.println(response.getToken());
		}catch(Exception e) {
			System.out.println("Failed to login. " + e.getMessage());
		}
	}

}
