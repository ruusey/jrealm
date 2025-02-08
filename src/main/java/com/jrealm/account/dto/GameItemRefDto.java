package com.jrealm.account.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.jrealm.net.entity.NetGameItemRef;

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
public class GameItemRefDto extends TemporalDto {
    private static final long serialVersionUID = -5119762736198793613L;

    private Integer itemId;
    private Integer slotIdx;
    private String itemUuid;
    
    
    public NetGameItemRef asNetGameItemRef() {
    	return new NetGameItemRef(itemId, slotIdx, itemUuid);
    }
   
}
