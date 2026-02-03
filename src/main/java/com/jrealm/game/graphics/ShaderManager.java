package com.jrealm.game.graphics;

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

    public static void init() {
        ShaderProgram.pedantic = false;
        effectShader = new ShaderProgram(VERT_SHADER, FRAG_SHADER);
        if (!effectShader.isCompiled()) {
            log.error("Effect shader failed to compile: {}", effectShader.getLog());
        }
    }

    public static void applyEffect(SpriteBatch batch, Sprite.EffectEnum effect) {
        if (effect == null || effect == Sprite.EffectEnum.NORMAL) {
            batch.setShader(null);
            return;
        }

        batch.setShader(effectShader);
        float[] matrix;
        switch (effect) {
            case SEPIA: matrix = SEPIA; break;
            case REDISH: matrix = REDISH; break;
            case GRAYSCALE: matrix = GRAYSCALE; break;
            case DECAY: matrix = DECAY; break;
            case NEGATIVE: matrix = NEGATIVE; break;
            default: matrix = IDENTITY; break;
        }
        effectShader.setUniformMatrix("u_colorMatrix", new com.badlogic.gdx.math.Matrix4(matrix));
    }

    public static void clearEffect(SpriteBatch batch) {
        batch.setShader(null);
    }

    public static void dispose() {
        if (effectShader != null) {
            effectShader.dispose();
        }
    }
}
