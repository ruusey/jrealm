package com.jrealm.account.dto;

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

    private List<ChestDto> playerVault;
    private List<CharacterDto> characters;
}
