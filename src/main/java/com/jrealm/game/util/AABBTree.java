package com.jrealm.game.util;

import java.util.ArrayList;
import java.util.List;

import com.jrealm.game.entity.GameObject;
import com.jrealm.game.math.Rectangle;

import lombok.Data;

@Data
public class AABBTree {
	private ArrayList<Rectangle> nodeList;
	private int rootIndex = 0;

	public AABBTree() {
		this.nodeList = new ArrayList<Rectangle>();
	}

	public void insert(GameObject go) {
		this.nodeList.add(go.getBounds());
	}

	public Rectangle insertLeaf(Rectangle newNode) {
		//		while(!this.nodeList.get(treeIndex).isLeaf()) {
		//
		//			AABB aabb = newNode.aabb; // node to insert
		//			AABBNode treeNode = this.nodeList.get(treeIndex); // node that could be root or parent/grand
		//			AABBNode rightNode = this.nodeList.get(treeNode.right); // node that could be siblings / cousins or nil
		//			AABBNode leftNode = this.nodeList.get(treeNode.left); // node that could be siblings / cousins or nil
		//
		//			AABB combinedAabb = treeNode.aabb.merge(aabb); // new node that could be parent/grand of node to insert
		//
		//			float parentCost = 2.0f * combinedAabb.getSurfaceArea(); // cost of being a parent (lol)
		//			float minPushCost = 2.0f * (combinedAabb.getSurfaceArea() - treeNode.aabb.getSurfaceArea()); // cost of being the next descendant
		//
		//			float costLeft = 0;
		//			float costRight = 0;
		//
		//			if(leftNode.isLeaf()) {
		//				costLeft = aabb.merge(leftNode.aabb).getSurfaceArea() + minPushCost;
		//			} else {
		//				costLeft = (aabb.merge(leftNode.aabb).getSurfaceArea() - leftNode.aabb.getSurfaceArea()) + minPushCost;
		//			}
		//
		//			if(rightNode.isLeaf()) {
		//				costRight = aabb.merge(rightNode.aabb).getSurfaceArea() + minPushCost;
		//			} else {
		//				costRight = (aabb.merge(rightNode.aabb).getSurfaceArea() - rightNode.aabb.getSurfaceArea()) + minPushCost;
		//			}
		//
		//			if((parentCost < costLeft) && (parentCost < costRight)) {
		//				break;
		//			}
		//
		//			if(costLeft < costRight) {
		//				treeIndex = treeNode.left;
		//			} else {
		//				treeIndex = treeNode.right;
		//			}
		//		}

		this.nodeList.add(newNode);
		return newNode;
	}

	public List<Rectangle> getAllNodes() {
		return this.nodeList;
	}

	public boolean removeObject(GameObject go) {
		return this.nodeList.remove(go.getBounds());
	}
}