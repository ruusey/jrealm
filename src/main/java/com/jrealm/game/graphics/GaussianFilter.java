package com.jrealm.game.graphics;

import java.awt.image.BufferedImage;
import java.awt.image.Kernel;

public class GaussianFilter extends ConvolveFilter {
	protected float radius;

	protected Kernel kernel;

	public GaussianFilter() {
		this(2.0F);
	}

	public GaussianFilter(float radius) {
		setRadius(radius);
	}

	public void setRadius(float radius) {
		this.radius = radius;
		this.kernel = makeKernel(radius);
	}

	public float getRadius() {
		return this.radius;
	}

	public BufferedImage filter(BufferedImage src, BufferedImage dst) {
		int width = src.getWidth();
		int height = src.getHeight();
		if (dst == null)
			dst = createCompatibleDestImage(src, null);
		int[] inPixels = new int[width * height];
		int[] outPixels = new int[width * height];
		src.getRGB(0, 0, width, height, inPixels, 0, width);
		if (this.radius > 0.0F) {
			convolveAndTranspose(this.kernel, inPixels, outPixels, width, height, this.alpha, CLAMP_EDGES);
			convolveAndTranspose(this.kernel, outPixels, inPixels, height, width, this.alpha, CLAMP_EDGES);
		}
		dst.setRGB(0, 0, width, height, inPixels, 0, width);
		return dst;
	}

	public static void convolveAndTranspose(Kernel kernel, int[] inPixels, int[] outPixels, int width, int height,
			boolean alpha, int edgeAction) {
		float[] matrix = kernel.getKernelData(null);
		int cols = kernel.getWidth();
		int cols2 = cols / 2;
		for (int y = 0; y < height; y++) {
			int index = y;
			int ioffset = y * width;
			for (int x = 0; x < width; x++) {
				float r = 0.0F, g = 0.0F, b = 0.0F, a = 0.0F;
				int moffset = cols2;
				for (int col = -cols2; col <= cols2; col++) {
					float f = matrix[moffset + col];
					if (f != 0.0F) {
						int ix = x + col;
						if (ix < 0) {
							if (edgeAction == CLAMP_EDGES) {
								ix = 0;
							} else if (edgeAction == WRAP_EDGES) {
								ix = (x + width) % width;
							}
						} else if (ix >= width) {
							if (edgeAction == CLAMP_EDGES) {
								ix = width - 1;
							} else if (edgeAction == WRAP_EDGES) {
								ix = (x + width) % width;
							}
						}
						int rgb = inPixels[ioffset + ix];
						a += f * (rgb >> 24 & 0xFF);
						r += f * (rgb >> 16 & 0xFF);
						g += f * (rgb >> 8 & 0xFF);
						b += f * (rgb & 0xFF);
					}
				}
				int ia = alpha ? PixelUtils.clamp((int) (a + 0.5D)) : 255;
				int ir = PixelUtils.clamp((int) (r + 0.5D));
				int ig = PixelUtils.clamp((int) (g + 0.5D));
				int ib = PixelUtils.clamp((int) (b + 0.5D));
				outPixels[index] = ia << 24 | ir << 16 | ig << 8 | ib;
				index += height;
			}
		}
	}

	public static Kernel makeKernel(float radius) {
		int r = (int) Math.ceil(radius);
		int rows = r * 2 + 1;
		float[] matrix = new float[rows];
		float sigma = radius / 3.0F;
		float sigma22 = 2.0F * sigma * sigma;
		float sigmaPi2 = 6.2831855F * sigma;
		float sqrtSigmaPi2 = (float) Math.sqrt(sigmaPi2);
		float radius2 = radius * radius;
		float total = 0.0F;
		int index = 0;
		for (int row = -r; row <= r; row++) {
			float distance = (row * row);
			if (distance > radius2) {
				matrix[index] = 0.0F;
			} else {
				matrix[index] = (float) Math.exp((-distance / sigma22)) / sqrtSigmaPi2;
			}
			total += matrix[index];
			index++;
		}
		for (int i = 0; i < rows; i++)
			matrix[i] = matrix[i] / total;
		return new Kernel(rows, 1, matrix);
	}

	public String toString() {
		return "Blur/Gaussian Blur...";
	}
}
