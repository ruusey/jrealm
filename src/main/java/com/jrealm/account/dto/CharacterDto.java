package com.jrealm.account.dto;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CharacterDto extends TemporalDto{
	private static final long serialVersionUID = -8940547643757956271L;
	
	private Integer characterId;
	private String characterUuid;
	private Integer characterClass;
	private CharacterStatsDto stats;
	private Set<GameItemRefDto> items;
}