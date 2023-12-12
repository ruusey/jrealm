package com.jrealm.game.tiles;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.jrealm.game.entity.material.MaterialManager;
import com.jrealm.game.noise.SimplexNoise;

public class TileMapGenerator {

	public String base;
	private int[] data;

	public String onTop;
	private int[] layer;

	private int chuckSize;

	private List<MaterialManager> materialManagers;

	// could set this as enum however that seems a little too much.
	// could have a class with all the different types of tiles but that also seems a little too much.
	// could change tileset and would have to change the numbers anyways.
	// the best way would probably create a program that labels tiles with a mouse click,
	// however that's over kill.

	private int[] grass = {52, 53, 54};
	private int[] dirt = {46, 47, 48};

	private TileData[] tiles = { new TileData(0.6f, 35, this.grass), new TileData(1f, 29, this.dirt) }; // change this later?

	public TileMapGenerator(int chuckSize, int tileSize, MaterialManager... mm) {
		this.materialManagers = Arrays.asList(mm);
		this.chuckSize = chuckSize;
		this.layer = new int[chuckSize * chuckSize];
		this.base = "";
		this.onTop = "";

		int xStart = 0;
		int xEnd = 500;
		int yStart = 0;
		int yEnd = 500;

		this.data = this.simplexTiles(xStart, xEnd, yStart, yEnd);
		//beautifyTiles();

		for(int i = 0; i < (chuckSize * chuckSize); i++) {
			this.onTop += this.layer[i] + ",";
			this.base += this.data[i] + ",";
		}
	}

	// O(m*9n^2) -> O(m*n^2) -> O(n^2)
	/* private void beautifyTiles() {
        String result = "";

        for(int i = 0; i < chuckSize; i++) {
            for(int j = 0; j < chuckSize; j++) {
                int temp = data[j + i * chuckSize];
                if(temp != 29) continue;

                for(int x = 1; x > -2; x--) {
                    for(int y = 1; y > -2; y--) {
                        if(x == 0 && y == 0) continue;

                        int c = ((j + y) + (i + x) * chuckSize);
                        if(c > 0 && c < data.length && data[c] == 35) {
                            layer[c] = x + 26 + ((y + 1) * 9);
                            //data[c] = 29;
                        } else {
                            break;
                        }
                    }
                }
            }
        }
    } */

	private int[] simplexTiles(int xStart, int xEnd, int yStart, int yEnd) {
		int[] data = new int[this.chuckSize * this.chuckSize];
		Random r = new Random(System.nanoTime());
		SimplexNoise simplexNoise = new SimplexNoise(300, 0.4, r.nextInt(10000));
		double[][] result = new double[this.chuckSize][this.chuckSize];

		for(int i = 0; i < this.chuckSize; i++){
			for(int j = 0; j < this.chuckSize; j++){
				int x = (int) (xStart + (i * ((xEnd - xStart) / this.chuckSize)));
				int y = (int) (yStart + (j * ((yEnd - yStart) / this.chuckSize)));
				result[i][j] = 0.5 * (1 + simplexNoise.getNoise(x, y));

				for(int k = 0; k < this.tiles.length; k++) {
					if(result[i][j] < this.tiles[k].rarity) {
						if ((k == 0) && (result[i][j] < (Math.random() * 0.35))) {
							int rInt = r.nextInt(this.materialManagers.size());
							this.materialManagers.get(rInt).add(MaterialManager.TYPE.TREE, j + (i * this.chuckSize));
						}
						data[j + (i * this.chuckSize)] = this.tiles[k].generate();
						break;
					}
				}
			}
		}

		return data;
	}
}