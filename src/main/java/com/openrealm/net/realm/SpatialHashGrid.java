package com.openrealm.net.realm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.openrealm.game.entity.GameObject;

/**
 * Spatial hash grid for O(1) neighbor lookups instead of O(n) brute-force scans.
 * Entities are bucketed into grid cells based on their world position.
 * Queries only check the cells that overlap the search area.
 */
public class SpatialHashGrid {
    private final float cellSize;
    private final float inverseCellSize;
    private final Map<Long, Set<Long>> cells;
    private final Map<Long, Long> entityCells; // entityId -> cellKey

    public SpatialHashGrid(float cellSize) {
        this.cellSize = cellSize;
        this.inverseCellSize = 1.0f / cellSize;
        this.cells = new ConcurrentHashMap<>();
        this.entityCells = new ConcurrentHashMap<>();
    }

    private long cellKey(int cx, int cy) {
        return ((long) cx << 32) | (cy & 0xFFFFFFFFL);
    }

    private int cellX(float worldX) {
        return (int) Math.floor(worldX * inverseCellSize);
    }

    private int cellY(float worldY) {
        return (int) Math.floor(worldY * inverseCellSize);
    }

    public void insert(long entityId, float x, float y) {
        long key = cellKey(cellX(x), cellY(y));
        cells.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(entityId);
        entityCells.put(entityId, key);
    }

    public void remove(long entityId) {
        Long oldKey = entityCells.remove(entityId);
        if (oldKey != null) {
            Set<Long> cell = cells.get(oldKey);
            if (cell != null) {
                cell.remove(entityId);
                if (cell.isEmpty()) {
                    cells.remove(oldKey);
                }
            }
        }
    }

    public void update(long entityId, float x, float y) {
        long newKey = cellKey(cellX(x), cellY(y));
        Long oldKey = entityCells.get(entityId);
        if (oldKey != null && oldKey == newKey) {
            return; // same cell, no-op
        }
        if (oldKey != null) {
            Set<Long> oldCell = cells.get(oldKey);
            if (oldCell != null) {
                oldCell.remove(entityId);
                if (oldCell.isEmpty()) {
                    cells.remove(oldKey);
                }
            }
        }
        cells.computeIfAbsent(newKey, k -> ConcurrentHashMap.newKeySet()).add(entityId);
        entityCells.put(entityId, newKey);
    }

    /**
     * Returns entity IDs within the given circular radius of (cx, cy).
     * Only checks cells that overlap the bounding box of the circle.
     */
    public List<Long> queryRadius(float cx, float cy, float radius) {
        int minCX = cellX(cx - radius);
        int maxCX = cellX(cx + radius);
        int minCY = cellY(cy - radius);
        int maxCY = cellY(cy + radius);

        List<Long> result = new ArrayList<>();
        for (int gx = minCX; gx <= maxCX; gx++) {
            for (int gy = minCY; gy <= maxCY; gy++) {
                Set<Long> cell = cells.get(cellKey(gx, gy));
                if (cell != null) {
                    result.addAll(cell);
                }
            }
        }
        return result;
    }

    /**
     * Returns the cell key for a given world position.
     * Players in the same cell see approximately the same entities.
     */
    public long getCellKey(float x, float y) {
        return cellKey(cellX(x), cellY(y));
    }

    public void clear() {
        cells.clear();
        entityCells.clear();
    }
}
