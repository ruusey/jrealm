package com.jrealm.account;

import java.util.List;

import com.jrealm.game.entity.item.Chest;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlayerAccount {
	public int accountGuid;

	private List<Chest> chestStorage;
	public PlayerAccount() {
		// TODO Auto-generated constructor stub
	}

}
