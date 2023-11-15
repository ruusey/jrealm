package com.jrealm.game.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequestMessage {
	private String username;
	private String password;
	private long characterId;
}
