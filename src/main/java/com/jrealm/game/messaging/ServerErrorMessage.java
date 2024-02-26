package com.jrealm.game.messaging;

import java.text.MessageFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServerErrorMessage {
	private int code;
	private String message;

	public static ServerErrorMessage from(int code, String message) {
		return ServerErrorMessage.builder().code(code).message(message).build();
	}

	@Override
	public String toString() {
		return MessageFormat.format("\"code\":{0}, \"message\": {1}", this.code, this.message);
	}
}
