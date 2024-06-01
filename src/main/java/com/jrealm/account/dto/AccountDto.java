package com.jrealm.account.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@Builder
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountDto implements Serializable {
    private static final long serialVersionUID = -8182611782332882587L;

    private String accountId;
    private Integer externalId;
    private String identifier;
    private String accountGuid;

    @JsonProperty("email")
    private String email;
    private String password;
    private String accountName;
    private Map<String, String> accountProperties;
    private List<AccountProvision> accountProvisions;
    private List<AccountSubscription> accountSubscriptions;

    private Date created;
    private Date updated;
    private Date deleted;

    public boolean isAdmin() {
	return this.accountSubscriptions != null && this.accountSubscriptions.size() > 0
		&& this.accountSubscriptions.contains(AccountSubscription.ADMIN);
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (this.getClass() != obj.getClass())
	    return false;
	AccountDto other = (AccountDto) obj;
	return Objects.equals(this.accountGuid, other.accountGuid) && Objects.equals(this.accountId, other.accountId)
		&& Objects.equals(this.accountName, other.accountName) && Objects.equals(this.email, other.email)
		&& Objects.equals(this.externalId, other.externalId)
		&& Objects.equals(this.identifier, other.identifier);
    }

    @Override
    public int hashCode() {
	return Objects.hash(this.accountGuid, this.accountId, this.accountName, this.email, this.externalId,
		this.identifier);
    }

}
