package com.openrealm.account.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = false)
public class PlayerAccountDto extends TemporalDto {

    private static final long serialVersionUID = -3553188514579673153L;

    private String accountId;
    private String accountEmail;
    private String accountUuid;
    private String accountName;

    // Lifetime fame banked from dead characters. Mirrors the field on the data
    // service's PlayerAccountDto so this DTO round-trips through the data API
    // without dropping it on the floor. Server doesn't write this directly —
    // banking happens in the data service when characters are deleted with
    // the bankFame flag (see RealmManagerServer.playerDeath).
    private Long accountFame;

    private List<ChestDto> playerVault;
    private List<CharacterDto> characters;
}
