package com.openrealm.game.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.openrealm.game.entity.item.GameItem;
import com.openrealm.game.model.EnemyModel;
import com.openrealm.game.model.MapModel;
import com.openrealm.game.model.PortalModel;

/**
 * Resolves admin-command arguments that historically only accepted a numeric
 * ID to ALSO accept the entity's logical name. Used by /spawn, /item, /portal,
 * /world, /tile, etc — anywhere a moderator types something on the chat line.
 *
 * Resolution order is consistent across all types:
 *   1. If the token parses as an int, treat it as an ID and look it up in
 *      the appropriate GameDataManager map. (Even when the data set ALSO
 *      has a name that happens to look numeric, the int branch always
 *      wins — IDs are the canonical identity and never ambiguous.)
 *   2. Otherwise case-insensitive exact-match against the entity's name
 *      field (mapName / portalName / name).
 *   3. Otherwise reject with an exception that lists the close candidates.
 *
 * AMBIGUOUS NAMES: when the editor's unique-name validation hasn't caught
 * a duplicate (e.g. legacy data), an exact-match name search can hit
 * multiple records. The contract here is to THROW IllegalArgumentException
 * with the colliding IDs in the message — falling through to "first match
 * wins" makes the bug invisible and silently spawns the wrong entity.
 */
public final class GameDataLookup {
    private GameDataLookup() {}

    public static int resolveEnemyId(final String token) {
        if (GameDataManager.ENEMIES == null) {
            throw new IllegalArgumentException("Enemy data not loaded.");
        }
        // 1. Numeric ID first.
        final Integer parsed = tryParseInt(token);
        if (parsed != null) {
            if (!GameDataManager.ENEMIES.containsKey(parsed)) {
                throw new IllegalArgumentException("Unknown enemy id: " + parsed);
            }
            return parsed;
        }
        // 2. Exact name (case-insensitive). Collect every match so we can
        //    fail loudly on ambiguity rather than silently pick one.
        final List<Integer> matches = new ArrayList<>();
        final String lower = token.toLowerCase(Locale.ROOT);
        for (final EnemyModel e : GameDataManager.ENEMIES.values()) {
            if (e.getName() != null && e.getName().toLowerCase(Locale.ROOT).equals(lower)) {
                matches.add(e.getEnemyId());
            }
        }
        return resolveSingle("enemy", token, matches);
    }

    public static int resolveItemId(final String token) {
        if (GameDataManager.GAME_ITEMS == null) {
            throw new IllegalArgumentException("Item data not loaded.");
        }
        final Integer parsed = tryParseInt(token);
        if (parsed != null) {
            if (!GameDataManager.GAME_ITEMS.containsKey(parsed)) {
                throw new IllegalArgumentException("Unknown item id: " + parsed);
            }
            return parsed;
        }
        final List<Integer> matches = new ArrayList<>();
        final String lower = token.toLowerCase(Locale.ROOT);
        for (final GameItem i : GameDataManager.GAME_ITEMS.values()) {
            if (i.getName() != null && i.getName().toLowerCase(Locale.ROOT).equals(lower)) {
                matches.add(i.getItemId());
            }
        }
        return resolveSingle("item", token, matches);
    }

    /** MapModel resolution. Mirrors /portal + /world's existing patterns
     *  but with strict ambiguity rejection. Caller can use .getMapId(). */
    public static MapModel resolveMap(final String token) {
        if (GameDataManager.MAPS == null) {
            throw new IllegalArgumentException("Map data not loaded.");
        }
        final Integer parsed = tryParseInt(token);
        if (parsed != null) {
            final MapModel m = GameDataManager.MAPS.get(parsed);
            if (m == null) throw new IllegalArgumentException("Unknown map id: " + parsed);
            return m;
        }
        final List<MapModel> matches = new ArrayList<>();
        final String lower = token.toLowerCase(Locale.ROOT);
        for (final MapModel m : GameDataManager.MAPS.values()) {
            if (m.getMapName() != null && m.getMapName().toLowerCase(Locale.ROOT).equals(lower)) {
                matches.add(m);
            }
        }
        if (matches.size() == 1) return matches.get(0);
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("No map matches '" + token + "'. "
                    + "Use a numeric mapId or an exact mapName.");
        }
        final List<Integer> ids = new ArrayList<>();
        for (final MapModel m : matches) ids.add(m.getMapId());
        throw new IllegalArgumentException("Map name '" + token + "' is ambiguous — "
                + "multiple records with that name (ids=" + ids + "). Rename one in the editor.");
    }

    public static int resolvePortalId(final String token) {
        if (GameDataManager.PORTALS == null) {
            throw new IllegalArgumentException("Portal data not loaded.");
        }
        final Integer parsed = tryParseInt(token);
        if (parsed != null) {
            if (!GameDataManager.PORTALS.containsKey(parsed)) {
                throw new IllegalArgumentException("Unknown portal id: " + parsed);
            }
            return parsed;
        }
        final List<Integer> matches = new ArrayList<>();
        final String lower = token.toLowerCase(Locale.ROOT);
        for (final PortalModel p : GameDataManager.PORTALS.values()) {
            if (p.getPortalName() != null && p.getPortalName().toLowerCase(Locale.ROOT).equals(lower)) {
                matches.add(p.getPortalId());
            }
        }
        return resolveSingle("portal", token, matches);
    }

    /** Try parseInt without throwing — returns null on non-numeric input. */
    private static Integer tryParseInt(final String token) {
        if (token == null || token.isBlank()) return null;
        try { return Integer.parseInt(token.trim()); }
        catch (final NumberFormatException e) { return null; }
    }

    private static int resolveSingle(final String kind, final String token, final List<Integer> matches) {
        if (matches.size() == 1) return matches.get(0);
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("No " + kind + " matches '" + token
                    + "'. Use a numeric id or an exact name.");
        }
        throw new IllegalArgumentException(kind + " name '" + token + "' is ambiguous — "
                + "multiple records share that name (ids=" + matches + "). Rename one in the editor.");
    }
}
