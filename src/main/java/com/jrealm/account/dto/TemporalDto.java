package com.jrealm.account.dto;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TemporalDto implements Serializable {

	private static final long serialVersionUID = -4882496257221385663L;

	private Date created;
	private Date updated;
	private Date deleted;

	public boolean isDeleted() {
		return this.deleted != null;
	}
}
