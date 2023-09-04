package com.jrealm.game.util;

import java.util.ArrayList;
import java.util.List;

import com.jrealm.game.entity.GameObject;
import com.jrealm.game.math.AABB;

import lombok.Data;

@Data
public class AABBTree {

	private ArrayList<AABB> nodeList;
	// private HashMap<GameObject, AABBNode> nodeIndex;

	private int rootIndex = 0;

	public AABBTree() {
		this.nodeList = new ArrayList<AABB>();
		// this.nodeIndex = new HashMap<GameObject, AABBNode>();
	}

	public void insert(GameObject go) {

		System.out.println("Adding: game object to tree...");
		this.nodeList.add(go.getBounds());

	}

	// TODO: fix inifite loop. check if tree is correctly created
	public AABB insertLeaf(AABB newNode) {

		// make sure we are inserting a new leaf?


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

		// suitable sibling leaf node found

		this.nodeList.add(newNode);

		return newNode;
	}

	public List<AABB> getAllNodes() {
		return this.nodeList;
	}

	public void removeObject(GameObject go) {
		// AABBNode node = this.nodeIndex.get(go);
		boolean success = this.nodeList.remove(go.getBounds());

	}






	@Override
	public String toString() {
		String result = "[\n";
		ArrayList<AABB> tree = this.nodeList;

		//		for(int i = 0; i < tree.size(); i++) {
		//			result += "  " + i + ":{ " + tree.get(i). + ", "
		//					+
		//					tree.get(i).left + ", " + tree.get(i).right +
		//					", " + tree.get(i).parent + " }\n";
		//		}

		return result + "]";
	}

	private static class AABBNode {

		public AABB aabb;
		public GameObject go;

		public int parent = -1;
		public int left = -1;
		public int right = -1;

		public int index;
		public int height = -1; // not in use

		public boolean isLeaf() { return this.left == -1; }

		public AABBNode(GameObject go, int index) {
			this.go = go;
			this.aabb = go.getBounds();
			this.index = index;
		}

		public AABBNode(int parent, int left, int right, int index, int height) {
			this.parent = parent;
			this.left = left;
			this.right = right;
			this.index = index;
			this.height = height;
		}
	}
}