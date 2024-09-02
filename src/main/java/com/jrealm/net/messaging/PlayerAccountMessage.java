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
public class PlayerAccountMessage {
    private PlayerAccountDto account;
}
