package com.jrealm.account.service;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Map;

import com.jrealm.account.dto.LoginRequestDto;
import com.jrealm.account.dto.SessionTokenDto;

public interface JrealmDataService {
    public <T> T executeDelete(String path, Class<T> responseClass) throws Exception;
    public <T> T executePost(String path, Object object, Class<T> responseClass) throws Exception;
    public <T> T executePut(String path, Object object, Class<T> responseClass) throws Exception;
    public String executeGet(String path, Map<String, String> queryParams) throws Exception;
    public <T> T executeGet(String path, Map<String, String> queryParams, Class<T> responseClass) throws Exception;
    public void setAuth(HttpRequest.Builder builder);

    public static void main(String[] args) {
        JrealmClientDataService service = new JrealmClientDataService(HttpClient.newHttpClient(), "http://localhost:8085/", null);
        LoginRequestDto login = new LoginRequestDto("ru-admin@jrealm.com", "password");
        try {
            final SessionTokenDto response = service.executePost("/admin/account/login", login, SessionTokenDto.class);
            System.out.println(response.getToken());
        } catch (Exception e) {
            System.out.println("Failed to login. " + e.getMessage());
        }
    }
}
