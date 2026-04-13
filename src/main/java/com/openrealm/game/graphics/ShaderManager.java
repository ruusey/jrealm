package com.openrealm.game.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ShaderManager {
    private static ShaderProgram effectShader;

    // Color matrices matching Sprite.EffectEnum values
    private static final float[] IDENTITY = {
        1, 0, 0, 0,
        0, 1, 0, 0,
        0, 0, 1, 0,
        0, 0, 0, 0
    };

    private static final float[] SEPIA = {
        0.393f, 0.769f, 0.189f, 0,
        0.349f, 0.686f, 0.168f, 0,
        0.272f, 0.534f, 0.131f, 0,
        0, 0, 0, 0
    };

    private static final float[] REDISH = {
        1.0f, 0, 0, 0,
        0, 0.3f, 0, 0,
        0, 0, 0.3f, 0,
        0, 0, 0, 0
    };

    private static final float[] GRAYSCALE = {
        0.333f, 0.333f, 0.333f, 0,
        0.333f, 0.333f, 0.333f, 0,
        0.333f, 0.333f, 0.333f, 0,
        0, 0, 0, 0
    };

    private static final float[] DECAY = {
        0, 0.333f, 0.333f, 0,
        0.333f, 0, 0.333f, 0,
        0.333f, 0.333f, 0, 0,
        0, 0, 0, 0
    };

    private static final float[] NEGATIVE = {
        1, 0, 0, 0,
        0, 1, 0, 0,
        0, 0, 1, 0,
        0, 0, 0, 0
    };

    // Stasis: dark grey stone look (desaturated + heavily darkened)
    private static final float[] STASIS = {
        0.15f, 0.15f, 0.15f, 0,
        0.15f, 0.15f, 0.15f, 0,
        0.15f, 0.15f, 0.15f, 0,
        0.08f, 0.08f, 0.1f, 0
    };

    // Armored: bright metallic blue-silver tint (doubled defense)
    private static final float[] ARMORED = {
        0.5f, 0.4f, 0.2f, 0,
        0.3f, 0.5f, 0.3f, 0,
        0.2f, 0.4f, 0.7f, 0,
        0.1f, 0.1f, 0.15f, 0
    };

    // Poisoned: sickly green tint
    private static final float[] POISONED = {
        0.2f, 0.5f, 0.1f, 0,
        0.1f, 0.6f, 0.1f, 0,
        0.05f, 0.3f, 0.05f, 0,
        0.05f, 0.15f, 0.0f, 0
    };

    // Cursed: dark reddish-purple tint (enemy takes more damage)
    private static final float[] CURSED = {
        0.6f, 0.1f, 0.15f, 0,
        0.1f, 0.05f, 0.1f, 0,
        0.15f, 0.05f, 0.2f, 0,
        0.1f, 0.0f, 0.05f, 0
    };

    // Armor Broken: cracked cyan-purple tint (defense reduced to zero)
    private static final float[] ARMOR_BROKEN = {
        0.3f, 0.2f, 0.5f, 0,
        0.15f, 0.4f, 0.5f, 0,
        0.4f, 0.5f, 0.7f, 0,
        0.1f, 0.12f, 0.18f, 0
    };

    // Invincible: bright white-gold shimmer (immune to all damage)
    private static final float[] INVINCIBLE = {
        0.9f, 0.7f, 0.3f, 0,
        0.7f, 0.9f, 0.5f, 0,
        0.3f, 0.5f, 0.9f, 0,
        0.2f, 0.2f, 0.15f, 0
    };

    // Zero out RGB coefficients, use 4th column (constant) for dark grey
    private static final float[] SILHOUETTE = {
        0, 0, 0, 0,
        0, 0, 0, 0,
        0, 0, 0, 0,
        0.2f, 0.2f, 0.2f, 0
    };

    // Single-pass outline shader: samples 4 neighboring texels and draws
    // a solid color if any neighbor has alpha > 0 but the center doesn't.
    // Renders the outline in one draw call instead of four.
    private static ShaderProgram outlineShader;

    private static final String OUTLINE_FRAG =
        "#ifdef GL_ES\n" +
        "precision mediump float;\n" +
        "#endif\n" +
        "varying vec4 v_color;\n" +
        "varying vec2 v_texCoords;\n" +
        "uniform sampler2D u_texture;\n" +
        "uniform vec2 u_texelSize;\n" +
        "uniform float u_outlineSize;\n" +
        "uniform vec4 u_outlineColor;\n" +
        "void main() {\n" +
        "  vec4 center = texture2D(u_texture, v_texCoords);\n" +
        "  if (center.a > 0.01) {\n" +
        "    gl_FragColor = center * v_color;\n" +
        "    return;\n" +
        "  }\n" +
        "  float offset = u_outlineSize;\n" +
        "  float a = 0.0;\n" +
        "  a += texture2D(u_texture, v_texCoords + vec2( offset, 0.0) * u_texelSize).a;\n" +
        "  a += texture2D(u_texture, v_texCoords + vec2(-offset, 0.0) * u_texelSize).a;\n" +
        "  a += texture2D(u_texture, v_texCoords + vec2(0.0,  offset) * u_texelSize).a;\n" +
        "  a += texture2D(u_texture, v_texCoords + vec2(0.0, -offset) * u_texelSize).a;\n" +
        "  if (a > 0.0) {\n" +
        "    gl_FragColor = u_outlineColor;\n" +
        "  } else {\n" +
        "    gl_FragColor = vec4(0.0);\n" +
        "  }\n" +
        "}\n";

    // Pre-cached Matrix4 objects to avoid per-call allocations
    private static com.badlogic.gdx.math.Matrix4 MAT_IDENTITY;
    private static com.badlogic.gdx.math.Matrix4 MAT_SEPIA;
    private static com.badlogic.gdx.math.Matrix4 MAT_REDISH;
    private static com.badlogic.gdx.math.Matrix4 MAT_GRAYSCALE;
    private static com.badlogic.gdx.math.Matrix4 MAT_DECAY;
    private static com.badlogic.gdx.math.Matrix4 MAT_NEGATIVE;
    private static com.badlogic.gdx.math.Matrix4 MAT_SILHOUETTE;
    private static com.badlogic.gdx.math.Matrix4 MAT_STASIS;
    private static com.badlogic.gdx.math.Matrix4 MAT_CURSED;
    private static com.badlogic.gdx.math.Matrix4 MAT_POISONED;
    private static com.badlogic.gdx.math.Matrix4 MAT_ARMORED;
    private static com.badlogic.gdx.math.Matrix4 MAT_INVINCIBLE;
    private static com.badlogic.gdx.math.Matrix4 MAT_ARMOR_BROKEN;

    private static final String VERT_SHADER =
        "attribute vec4 a_position;\n" +
        "attribute vec4 a_color;\n" +
        "attribute vec2 a_texCoord0;\n" +
        "uniform mat4 u_projTrans;\n" +
        "varying vec4 v_color;\n" +
        "varying vec2 v_texCoords;\n" +
        "void main() {\n" +
        "  v_color = a_color;\n" +
        "  v_texCoords = a_texCoord0;\n" +
        "  gl_Position = u_projTrans * a_position;\n" +
        "}\n";

    private static final String FRAG_SHADER =
        "#ifdef GL_ES\n" +
        "precision mediump float;\n" +
        "#endif\n" +
        "varying vec4 v_color;\n" +
        "varying vec2 v_texCoords;\n" +
        "uniform sampler2D u_texture;\n" +
        "uniform mat4 u_colorMatrix;\n" +
        "void main() {\n" +
        "  vec4 texColor = texture2D(u_texture, v_texCoords) * v_color;\n" +
        "  if (texColor.a > 0.01) {\n" +
        "    vec4 rgb = vec4(texColor.rgb, 1.0);\n" +
        "    vec4 transformed = u_colorMatrix * rgb;\n" +
        "    gl_FragColor = vec4(clamp(transformed.rgb, 0.0, 1.0), texColor.a);\n" +
        "  } else {\n" +
        "    gl_FragColor = texColor;\n" +
        "  }\n" +
        "}\n";

    // Vibrance shader: subtle saturation boost + contrast for crisp pixel art
    private static ShaderProgram vibranceShader;

    private static final String VIBRANCE_FRAG =
        "#ifdef GL_ES\n" +
        "precision mediump float;\n" +
        "#endif\n" +
        "varying vec4 v_color;\n" +
        "varying vec2 v_texCoords;\n" +
        "uniform sampler2D u_texture;\n" +
        "uniform float u_saturation;\n" +
        "uniform float u_contrast;\n" +
        "void main() {\n" +
        "  vec4 texColor = texture2D(u_texture, v_texCoords) * v_color;\n" +
        "  if (texColor.a > 0.01) {\n" +
        "    float luma = dot(texColor.rgb, vec3(0.299, 0.587, 0.114));\n" +
        "    vec3 saturated = mix(vec3(luma), texColor.rgb, u_saturation);\n" +
        "    vec3 contrasted = (saturated - 0.5) * u_contrast + 0.5;\n" +
        "    gl_FragColor = vec4(clamp(contrasted, 0.0, 1.0), texColor.a);\n" +
        "  } else {\n" +
        "    gl_FragColor = texColor;\n" +
        "  }\n" +
        "}\n";

    public static void init() {
        ShaderProgram.pedantic = false;
        effectShader = new ShaderProgram(VERT_SHADER, FRAG_SHADER);
        if (!effectShader.isCompiled()) {
            log.error("Effect shader failed to compile: {}", effectShader.getLog());
        }

        // Compile vibrance shader
        vibranceShader = new ShaderProgram(VERT_SHADER, VIBRANCE_FRAG);
        if (!vibranceShader.isCompiled()) {
            log.error("Vibrance shader failed to compile: {}", vibranceShader.getLog());
        }

        // Compile outline shader
        outlineShader = new ShaderProgram(VERT_SHADER, OUTLINE_FRAG);
        if (!outlineShader.isCompiled()) {
            log.error("Outline shader failed to compile: {}", outlineShader.getLog());
        }

        // Pre-cache Matrix4 objects
        MAT_IDENTITY = new com.badlogic.gdx.math.Matrix4(IDENTITY);
        MAT_SEPIA = new com.badlogic.gdx.math.Matrix4(SEPIA);
        MAT_REDISH = new com.badlogic.gdx.math.Matrix4(REDISH);
        MAT_GRAYSCALE = new com.badlogic.gdx.math.Matrix4(GRAYSCALE);
        MAT_DECAY = new com.badlogic.gdx.math.Matrix4(DECAY);
        MAT_NEGATIVE = new com.badlogic.gdx.math.Matrix4(NEGATIVE);
        MAT_SILHOUETTE = new com.badlogic.gdx.math.Matrix4(SILHOUETTE);
        MAT_STASIS = new com.badlogic.gdx.math.Matrix4(STASIS);
        MAT_CURSED = new com.badlogic.gdx.math.Matrix4(CURSED);
        MAT_POISONED = new com.badlogic.gdx.math.Matrix4(POISONED);
        MAT_ARMORED = new com.badlogic.gdx.math.Matrix4(ARMORED);
        MAT_INVINCIBLE = new com.badlogic.gdx.math.Matrix4(INVINCIBLE);
        MAT_ARMOR_BROKEN = new com.badlogic.gdx.math.Matrix4(ARMOR_BROKEN);
    }

    private static Sprite.EffectEnum lastAppliedEffect = null;

    public static void applyEffect(SpriteBatch batch, Sprite.EffectEnum effect) {
        if (effect == null || effect == Sprite.EffectEnum.NORMAL) {
            if (lastAppliedEffect != null && lastAppliedEffect != Sprite.EffectEnum.NORMAL) {
                if (vibranceActive) {
                    batch.setShader(vibranceShader);
                    vibranceShader.setUniformf("u_saturation", vibSaturation);
                    vibranceShader.setUniformf("u_contrast", vibContrast);
                } else {
                    batch.setShader(null);
                }
                lastAppliedEffect = Sprite.EffectEnum.NORMAL;
            }
            return;
        }

        // Skip redundant shader switches
        if (effect == lastAppliedEffect) return;

        batch.setShader(effectShader);
        com.badlogic.gdx.math.Matrix4 matrix;
        switch (effect) {
            case SEPIA: matrix = MAT_SEPIA; break;
            case REDISH: matrix = MAT_REDISH; break;
            case GRAYSCALE: matrix = MAT_GRAYSCALE; break;
            case DECAY: matrix = MAT_DECAY; break;
            case NEGATIVE: matrix = MAT_NEGATIVE; break;
            case SILHOUETTE: matrix = MAT_SILHOUETTE; break;
            case STASIS: matrix = MAT_STASIS; break;
            case CURSED: matrix = MAT_CURSED; break;
            case POISONED: matrix = MAT_POISONED; break;
            case ARMORED: matrix = MAT_ARMORED; break;
            case INVINCIBLE: matrix = MAT_INVINCIBLE; break;
            case ARMOR_BROKEN: matrix = MAT_ARMOR_BROKEN; break;
            default: matrix = MAT_IDENTITY; break;
        }
        effectShader.setUniformMatrix("u_colorMatrix", matrix);
        lastAppliedEffect = effect;
    }

    private static boolean vibranceActive = false;
    private static float vibSaturation = 1.0f;
    private static float vibContrast = 1.0f;

    public static void clearEffect(SpriteBatch batch) {
        if (lastAppliedEffect != null && lastAppliedEffect != Sprite.EffectEnum.NORMAL) {
            if (vibranceActive) {
                batch.setShader(vibranceShader);
                vibranceShader.setUniformf("u_saturation", vibSaturation);
                vibranceShader.setUniformf("u_contrast", vibContrast);
            } else {
                batch.setShader(null);
            }
            lastAppliedEffect = Sprite.EffectEnum.NORMAL;
        }
    }

    /**
     * Apply the vibrance shader as the default batch shader.
     * Call once at the start of each frame before drawing.
     * @param saturation 1.0 = normal, 1.3 = boosted, 0.0 = grayscale
     * @param contrast 1.0 = normal, 1.15 = slightly punchy
     */
    public static void applyVibrance(SpriteBatch batch, float saturation, float contrast) {
        batch.setShader(vibranceShader);
        vibranceShader.setUniformf("u_saturation", saturation);
        vibranceShader.setUniformf("u_contrast", contrast);
        vibranceActive = true;
        vibSaturation = saturation;
        vibContrast = contrast;
        lastAppliedEffect = null;
    }

    /**
     * Restore the vibrance shader after an effect shader was used.
     * Use this instead of clearEffect() when vibrance is active.
     */
    public static void restoreVibrance(SpriteBatch batch, float saturation, float contrast) {
        batch.setShader(vibranceShader);
        vibranceShader.setUniformf("u_saturation", saturation);
        vibranceShader.setUniformf("u_contrast", contrast);
        lastAppliedEffect = null;
    }

    /**
     * Apply the single-pass outline shader. Renders outline + body in one draw call per entity.
     * @param batch the sprite batch
     * @param texWidth texture width in pixels (for texel size calculation)
     * @param texHeight texture height in pixels
     */
    public static void applyOutlineShader(SpriteBatch batch, float texWidth, float texHeight) {
        batch.setShader(outlineShader);
        outlineShader.setUniformf("u_texelSize", 1.0f / texWidth, 1.0f / texHeight);
        outlineShader.setUniformf("u_outlineSize", 2.5f);
        outlineShader.setUniformf("u_outlineColor", 0.2f, 0.2f, 0.2f, 1.0f);
        lastAppliedEffect = null; // force re-apply on next effect
    }

    public static void clearOutlineShader(SpriteBatch batch) {
        if (vibranceActive) {
            batch.setShader(vibranceShader);
            vibranceShader.setUniformf("u_saturation", vibSaturation);
            vibranceShader.setUniformf("u_contrast", vibContrast);
        } else {
            batch.setShader(null);
        }
        lastAppliedEffect = Sprite.EffectEnum.NORMAL;
    }

    public static void dispose() {
        if (effectShader != null) {
            effectShader.dispose();
        }
        if (vibranceShader != null) {
            vibranceShader.dispose();
        }
        if (outlineShader != null) {
            outlineShader.dispose();
        }
    }
}
