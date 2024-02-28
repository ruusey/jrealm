package com.jrealm.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateTokenResponseDto {
	private String id;
	private String token;
	private String tokenName;
}
