package com.jrealm.game.tiles;

import java.awt.Graphics2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.jrealm.game.entity.material.MaterialManager;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.tiles.blocks.NormTile;
import com.jrealm.game.util.Camera;

import lombok.Data;

@Data
public class TileManager {

	public static ArrayList<TileMap> tm;
	private SpriteSheet spritesheet;
	private int blockWidth;
	private int blockHeight;

	private List<MaterialManager> materialManagers;
	private MaterialManager mm;
	private int width;
	private int height;

	private String genMap;
	private String solid;
	private int chuckSize;
	private String file;
	private int columns;

	public TileManager() {
		TileManager.tm = new ArrayList<TileMap>();
		this.materialManagers = new ArrayList<>();
	}

	public TileManager(String path) {
		this();
		this.addTileMap(path, 64, 64);
	}

	public TileManager(String path, int blockWidth, int blockHeight) {
		this();
		this.addTileMap(path, blockWidth, blockHeight);
	}

	public TileManager(SpriteSheet spritesheet, int chuckSize, MaterialManager... mm) {
		this();
		this.addTileMap(spritesheet, 64, 64, chuckSize, mm);

	}

	public TileManager(SpriteSheet spritesheet, int blockWidth, int blockHeight, int chuckSize, MaterialManager... mm) {
		this();
		this.addTileMap(spritesheet, blockWidth, blockHeight, chuckSize, mm);
		System.gc();
	}

	public String getGenMap() {
		return this.genMap;
	}

	public String getSolid() {
		return this.solid;
	}

	public int getChunkSize() {
		return this.chuckSize;
	}

	public int getBlockWidth() {
		return this.blockWidth;
	}

	public int getBlockHeight() {
		return this.blockHeight;
	}

	public String getFilename() {
		return this.file;
	}

	public int getColumns() {
		return this.columns;
	}

	public void generateTileMap(int chuckSize) {
		this.addTileMap(this.spritesheet, this.blockWidth, this.blockHeight, chuckSize, this.mm);
	}

	private void addTileMap(SpriteSheet spritesheet, int blockWidth, int blockHeight, int chuckSize,
			MaterialManager... mm) {
		this.materialManagers = Arrays.asList(mm);
		this.spritesheet = spritesheet;
		this.blockWidth = blockWidth;
		this.blockHeight = blockHeight;
		this.width = chuckSize;
		this.height = chuckSize;
		this.file = spritesheet.getFilename();
		this.columns = spritesheet.getCols();

		String[] data = new String[3];
		TileMapGenerator tmg = new TileMapGenerator(chuckSize, blockWidth, mm);

		// For now
		data[0] = "";

		for (int i = 0; i < chuckSize; i++) {
			for (int j = 0; j < chuckSize; j++) {
				data[0] += "0,";
			}
		}

		TileManager.tm.add(new TileMapObj(data[0], spritesheet, chuckSize, chuckSize, blockWidth, blockHeight,
				spritesheet.getCols()));

		TileManager.tm.add(new TileMapNorm(tmg.base, spritesheet, chuckSize, chuckSize, blockWidth, blockHeight,
				spritesheet.getCols()));
		// tm.add(new TileMapNorm(tmg.onTop, spritesheet, chuckSize, chuckSize,
		// blockWidth, blockHeight, spritesheet.getCols()));

		this.solid = data[0];
		this.genMap = tmg.base;
	}

	private void addTileMap(String path, int blockWidth, int blockHeight) {
		String imagePath;

		int width = 0;
		int height = 0;
		int tileWidth;
		int tileHeight;
		int tileColumns;
		int layers = 0;
		SpriteSheet sprite;

		String[] data = new String[10];

		try {
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = builderFactory.newDocumentBuilder();
			Document doc = builder.parse(new File(this.getClass().getClassLoader().getResource(path).toURI()));
			doc.getDocumentElement().normalize();

			NodeList list = doc.getElementsByTagName("tileset");
			Node node = list.item(0);
			Element eElement = (Element) node;

			imagePath = eElement.getAttribute("name");
			tileWidth = Integer.parseInt(eElement.getAttribute("tilewidth"));
			tileHeight = Integer.parseInt(eElement.getAttribute("tileheight"));
			tileColumns = Integer.parseInt(eElement.getAttribute("columns"));

			this.columns = tileColumns;
			this.file = imagePath;
			sprite = new SpriteSheet("tile/" + imagePath + ".png", tileWidth, tileHeight, 0);

			list = doc.getElementsByTagName("layer");
			layers = list.getLength();

			for (int i = 0; i < layers; i++) {
				node = list.item(i);
				eElement = (Element) node;
				if (i <= 0) {
					width = Integer.parseInt(eElement.getAttribute("width"));
					height = Integer.parseInt(eElement.getAttribute("height"));
				}

				data[i] = eElement.getElementsByTagName("data").item(0).getTextContent();

				if (i >= 1) {
					TileManager.tm
					.add(new TileMapNorm(data[i], sprite, width, height, blockWidth, blockHeight, tileColumns));
				} else {
					TileManager.tm
					.add(new TileMapObj(data[i], sprite, width, height, blockWidth, blockHeight, tileColumns));
				}
			}
		} catch (Exception e) {
			System.out.println("ERROR - TILEMANAGER: can not read tilemap:");
			e.printStackTrace();
			System.exit(0);
		}

		this.width = width;
		this.height = height;
	}

	public NormTile[] getNormalTile(int id) {
		int normMap = 1;
		if (TileManager.tm.size() < 2) {
			normMap = 0;
		}
		NormTile[] block = new NormTile[64];

		int i = 0;
		for (int x = 4; x > -4; x--) {
			for (int y = 4; y > -4; y--) {
				if (((id + (y + (x * this.height))) < 0)
						|| ((id + (y + (x * this.height))) > ((this.width * this.height) - 2))) {
					continue;
				}
				block[i] = (NormTile) TileManager.tm.get(normMap).getBlocks()[id + (y + (x * this.height))];
				i++;
			}
		}

		return block;
	}

	public AABB getRenderViewPort() {
		NormTile[] tiles = this.getNormalTile(909);
		NormTile first = tiles[0];
		NormTile last = tiles[tiles.length - 1];

		Vector2f pos = last.getPos();
		float width = first.getPos().x - pos.x;
		float height = first.getPos().y - pos.y;

		AABB viewPort = new AABB(pos.clone(), (int) width, (int) height);

		return viewPort;
	}

	public void render(Graphics2D g, Camera bounds) {
		bounds.setTileSize(this.blockWidth);

		bounds.setLimit(this.width * this.blockWidth, this.height * this.blockHeight);

		for (int i = 0; i < TileManager.tm.size(); i++) {
			TileManager.tm.get(i).render(g, bounds.getBounds());
		}
	}
}
