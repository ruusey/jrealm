package com.jrealm.game.model;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.Stats;
import com.jrealm.game.math.Vector2f;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemTooltip {
    private Vector2f pos;
    private int width;
    private int height;

    private String title;
    private String description;

    private int minDamage;
    private int maxDamage;

    private byte targetClass;
    private byte tier;

    private Stats stats;

    private static final int PADDING = 8;
    private static final int LINE_HEIGHT = 22;
    private static final Color BG_COLOR = new Color(0.12f, 0.12f, 0.15f, 0.95f);
    private static final Color BORDER_COLOR = new Color(0.4f, 0.4f, 0.5f, 1f);
    private static final Color TITLE_COLOR = new Color(1f, 0.85f, 0.2f, 1f);
    private static final Color DESC_COLOR = new Color(0.75f, 0.75f, 0.75f, 1f);
    private static final Color STAT_POS_COLOR = new Color(0.3f, 1f, 0.3f, 1f);
    private static final Color STAT_NEG_COLOR = new Color(1f, 0.3f, 0.3f, 1f);
    private static final Color INFO_COLOR = new Color(0.6f, 0.8f, 1f, 1f);

    public ItemTooltip(GameItem item, Vector2f pos, int width, int height) {
        this.pos = pos;
        this.width = width;
        this.height = height;
        this.title = item.getName();
        this.description = item.getDescription();

        if (item.getDamage() != null) {
            this.minDamage = item.getDamage().getMin();
            this.maxDamage = item.getDamage().getMax();
        }

        this.targetClass = item.getTargetClass();
        this.tier = item.getTier();
        this.stats = item.getStats();
    }

    private String getClassName() {
        CharacterClass cls = CharacterClass.valueOf((int) this.targetClass);
        if (cls == null) return "Unknown";
        return cls.name();
    }

    private List<TooltipLine> buildLines() {
        List<TooltipLine> lines = new ArrayList<>();

        // Title
        if (this.title != null && !this.title.isEmpty()) {
            lines.add(new TooltipLine(this.title, TITLE_COLOR));
        }

        // Description - wrap long text to fit tooltip width
        if (this.description != null && !this.description.isEmpty()) {
            // Approximate char width for BitmapFont (typically ~7px per glyph at default scale)
            int charWidth = 7;
            int maxCharsPerLine = Math.max(8, (this.width - PADDING * 2) / charWidth);
            String[] words = this.description.split(" ");
            StringBuilder currentLine = new StringBuilder();
            for (String word : words) {
                if (currentLine.length() == 0) {
                    currentLine.append(word);
                } else if (currentLine.length() + 1 + word.length() <= maxCharsPerLine) {
                    currentLine.append(" ").append(word);
                } else {
                    lines.add(new TooltipLine(currentLine.toString(), DESC_COLOR));
                    currentLine = new StringBuilder(word);
                }
            }
            if (currentLine.length() > 0) {
                lines.add(new TooltipLine(currentLine.toString(), DESC_COLOR));
            }
        }

        // Separator
        lines.add(new TooltipLine("", null));

        // Damage
        if (this.maxDamage > 0) {
            lines.add(new TooltipLine("Damage: " + this.minDamage + " - " + this.maxDamage, INFO_COLOR));
        }

        // Class
        lines.add(new TooltipLine("Class: " + this.getClassName(), INFO_COLOR));

        // Tier
        lines.add(new TooltipLine("Tier: " + this.tier, INFO_COLOR));

        // Stats
        if (this.stats != null) {
            List<String> statParts = new ArrayList<>();
            this.addStat(statParts, "HP", this.stats.getHp());
            this.addStat(statParts, "MP", this.stats.getMp());
            this.addStat(statParts, "ATT", this.stats.getAtt());
            this.addStat(statParts, "DEF", this.stats.getDef());
            this.addStat(statParts, "SPD", this.stats.getSpd());
            this.addStat(statParts, "DEX", this.stats.getDex());
            this.addStat(statParts, "VIT", this.stats.getVit());
            this.addStat(statParts, "WIS", this.stats.getWis());

            if (!statParts.isEmpty()) {
                lines.add(new TooltipLine("", null));
                for (String part : statParts) {
                    boolean positive = part.contains("+");
                    lines.add(new TooltipLine(part, positive ? STAT_POS_COLOR : STAT_NEG_COLOR));
                }
            }
        }

        return lines;
    }

    private void addStat(List<String> parts, String name, int value) {
        if (value != 0) {
            String sign = value > 0 ? "+" : "";
            parts.add(name + " " + sign + value);
        }
    }

    public void render(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font) {
        List<TooltipLine> lines = this.buildLines();

        // Calculate actual height based on content
        int contentHeight = PADDING * 2 + (lines.size() * LINE_HEIGHT);
        int tooltipWidth = this.width;
        int tooltipHeight = Math.max(contentHeight, LINE_HEIGHT * 3);

        // Clamp position so tooltip stays on screen
        float drawX = this.pos.x;
        float drawY = this.pos.y;

        // Draw background with border
        batch.end();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        // Border
        shapes.setColor(BORDER_COLOR);
        shapes.rect(drawX - 1, drawY - 1, tooltipWidth + 2, tooltipHeight + 2);
        // Background
        shapes.setColor(BG_COLOR);
        shapes.rect(drawX, drawY, tooltipWidth, tooltipHeight);
        shapes.end();
        batch.begin();

        // Draw lines
        float textX = drawX + PADDING;
        float textY = drawY + PADDING + LINE_HEIGHT;

        for (int i = 0; i < lines.size(); i++) {
            TooltipLine line = lines.get(i);
            if (line.color == null || line.text.isEmpty()) {
                // Empty line / separator - just advance Y
                textY += LINE_HEIGHT / 2;
                continue;
            }
            font.setColor(line.color);
            font.draw(batch, line.text, textX, textY);
            textY += LINE_HEIGHT;
        }

        // Reset font color
        font.setColor(Color.WHITE);
    }

    private static class TooltipLine {
        String text;
        Color color;

        TooltipLine(String text, Color color) {
            this.text = text;
            this.color = color;
        }
    }
}
