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
public class ChestDto extends TemporalDto {
	private static final long serialVersionUID = 4633372389187160480L;
	
	private Integer chestId;
	private Integer accountId;
	private Integer ordinal;
	private Set<GameItemRefDto> items;
}
