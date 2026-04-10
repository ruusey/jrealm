package com.openrealm.account.service;

import java.net.http.HttpClient;
import java.util.Map;

import com.openrealm.account.dto.LoginRequestDto;
import com.openrealm.account.dto.SessionTokenDto;

public interface OpenRealmDataService {
    public <T> T executeDelete(String path, Class<T> responseClass) throws Exception;
    public <T> T executePost(String path, Object object, Class<T> responseClass) throws Exception;
    public <T> T executePut(String path, Object object, Class<T> responseClass) throws Exception;
    public String executeGet(String path, Map<String, String> queryParams) throws Exception;
    public <T> T executeGet(String path, Map<String, String> queryParams, Class<T> responseClass) throws Exception;
    public <T> T executeGetWithToken(String path, String token, Class<T> responseClass) throws Exception;

    public static void main(String[] args) {
        OpenRealmClientDataService service = new OpenRealmClientDataService(HttpClient.newHttpClient(), "http://localhost/", null);
        LoginRequestDto login = new LoginRequestDto("ru-admin@jrealm.com", "password");
        try {
            final SessionTokenDto response = service.executePost("/admin/account/login", login, SessionTokenDto.class);
            System.out.println(response.getToken());
        } catch (Exception e) {
            System.out.println("Failed to login. " + e.getMessage());
        }
    }
}
