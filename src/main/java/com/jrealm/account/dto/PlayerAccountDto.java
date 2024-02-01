package com.jrealm.account.dto;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerAccountDto extends TemporalDto {

	private static final long serialVersionUID = -3553188514579673153L;
	
	private Integer accountId;
	private String accountEmail;
	private String accountUuid;
	private String accountName;
	
	private Set<ChestDto> playerVault;
	private Set<CharacterDto> characters;
}
