package com.jrealm.net.messaging;

import com.jrealm.account.dto.PlayerAccountDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseMessage {
    private long playerId;
    private int classId;
    private boolean success;
    private float spawnX;
    private float spawnY;
    private String token;
    private PlayerAccountDto account;
}
