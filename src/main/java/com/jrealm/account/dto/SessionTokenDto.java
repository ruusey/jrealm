package com.jrealm.account.dto;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SessionTokenDto {
	private String accountGuid;
	private String token;
	private Timestamp expires;
}
