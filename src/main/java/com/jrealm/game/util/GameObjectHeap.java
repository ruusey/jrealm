package com.jrealm.game.util;

import java.util.ArrayList;

import com.jrealm.game.entity.GameObject;

// Maybe Quicksort?
public class GameObjectHeap extends ArrayList<GameObjectKey> {

	private static final long serialVersionUID = 1L;

	public void buildHeap() {
		for (int i = this.size() / 2; i >= 0; i--) {
			this.minHeapify(i);
		}
	}

	public void add(float value, GameObject go) {
		GameObjectKey gok = new GameObjectKey(value, go);
		super.add(gok);

		int i = this.size() - 1;
		int parent = this.parent(i);

		if (i > 3) {
			while ((parent != i) && (this.get(i).value < this.get(parent).value)) {
				this.swap(i, parent);
				i = parent;
				parent = this.parent(i);
			}
		}
	}

	public boolean contains(GameObject go) {
		for(int i = 0; i < this.size(); i++) {
			if(go.equals(this.get(i).go) )
				return true;
		}

		return false;
	}

	public void remove(GameObject go) {
		for(int i = 0; i < this.size(); i++) {
			if (go.equals(this.get(i).go)) {
				super.remove(i);
			}
		}
	}

	private void minHeapify(int i) {
		int left = (2 * i) + 1;
		int right = (2 * i) + 2;
		int smallest = -1;

		if ((left < (this.size() - 1)) && (this.get(left).value < this.get(i).value)) {
			smallest = left;
		} else {
			smallest = i;
		}

		if ((right < (this.size() - 1)) && (this.get(right).value < this.get(smallest).value)) {
			smallest = right;
		}

		if (smallest != i) {
			this.swap(i, smallest);
			this.minHeapify(smallest);
		}
	}

	private int parent(int i) {
		if ((i % 2) == 1)
			return i / 2;

		return (i - 1) / 2;
	}

	private void swap(int i, int parent) {
		GameObjectKey temp = this.get(parent);
		this.set(parent, this.get(i));
		this.set(i, temp);
	}

	@Override
	public String toString() {
		String string = "[";

		for(int i = 0; i < this.size(); i++) {
			string += this.get(i).value + ", ";
		}

		string += "]";

		return string;
	}
}
