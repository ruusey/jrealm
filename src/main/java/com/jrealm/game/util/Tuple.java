package com.jrealm.game.util;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Tuple<X, Y> {
	private final X x;
	private final Y y;
}