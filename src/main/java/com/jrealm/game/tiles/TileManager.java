package com.jrealm.game.tiles;

import java.awt.Graphics2D;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.entity.material.MaterialManager;
import com.jrealm.game.graphics.SpriteSheet;
import com.jrealm.game.math.AABB;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.tiles.blocks.NormTile;
import com.jrealm.game.util.Camera;

import lombok.Data;

@Data
public class TileManager {

	public volatile ArrayList<TileMap> tm;
	private SpriteSheet spritesheet;
	private int blockWidth;
	private int blockHeight;

	private List<MaterialManager> materialManagers;
	private int width;
	private int height;

	private String genMap;
	private String solid;
	private int chuckSize;
	private String file;
	private int columns;

	private Camera playerCam;

	public TileManager() {
		this.tm = new ArrayList<>();
	}

	public TileManager(String path, Camera playerCam) {
		this(path, GlobalConstants.BASE_TILE_SIZE, GlobalConstants.BASE_TILE_SIZE, playerCam);
	}

	public TileManager(String path, int blockWidth, int blockHeight, Camera playerCam) {
		this.tm = new ArrayList<>();

		this.playerCam = playerCam;
		this.blockWidth = blockWidth;
		this.blockHeight = blockHeight;
		this.addTileMap(path, blockWidth, blockHeight);
	}

	public TileManager(SpriteSheet spritesheet, int chuckSize, Camera playerCam, MaterialManager... mm) {
		this(spritesheet, GlobalConstants.BASE_TILE_SIZE, GlobalConstants.BASE_TILE_SIZE, chuckSize, playerCam, mm);
	}

	public TileManager(SpriteSheet spritesheet, int blockWidth, int blockHeight, int chuckSize, Camera playerCam,
			MaterialManager... mm) {
		this.tm = new ArrayList<>();
		this.playerCam = playerCam;
		this.blockWidth = blockWidth;
		this.blockHeight = blockHeight;
		this.chuckSize = chuckSize;
		this.spritesheet = spritesheet;
		this.materialManagers = Arrays.asList(mm);
		this.addTileMap(spritesheet, blockWidth, blockHeight, chuckSize, Arrays.asList(mm));
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
		this.addTileMap(this.spritesheet, this.blockWidth, this.blockHeight, chuckSize, this.materialManagers);
	}

	private void addTileMap(SpriteSheet spritesheet, int blockWidth, int blockHeight, int chuckSize,
			List<MaterialManager> matManagers) {
		this.materialManagers = matManagers;
		this.spritesheet = spritesheet;
		this.blockWidth = blockWidth;
		this.blockHeight = blockHeight;
		this.width = chuckSize;
		this.height = chuckSize;
		this.file = spritesheet.getFilename();
		this.columns = spritesheet.getCols();
		this.playerCam.setTileSize(blockWidth);

		String[] data = new String[3];
		TileMapGenerator tmg = new TileMapGenerator(chuckSize, blockWidth, this.materialManagers.get(0));

		// For now
		data[0] = "";

		for (int i = 0; i < chuckSize; i++) {
			for (int j = 0; j < chuckSize; j++) {
				data[0] += "0,";
			}
		}

		this.tm.add(new TileMapObj(data[0], spritesheet, chuckSize, chuckSize, blockWidth, blockHeight,
				spritesheet.getCols()));

		this.tm.add(new TileMapNorm(tmg.base, spritesheet, chuckSize, chuckSize, blockWidth, blockHeight,spritesheet.getCols()));

		this.playerCam.setLimit(chuckSize * blockWidth, chuckSize * blockHeight);

		this.solid = data[0];
		this.genMap = tmg.base;
	}

	private void addTileMap(String path, int blockWidth, int blockHeight) {
		String imagePath;
		this.playerCam.setTileSize(blockWidth);
		int width = 0;
		int height = 0;
		int tileWidth;
		int tileHeight;
		int tileColumns;
		int layers = 0;
		SpriteSheet sprite;

		String[] data = new String[10];

		try {

			InputStream inputStream = TileManager.class.getClassLoader().getResourceAsStream(path);
			String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();

			Document doc = db.parse(new InputSource(new StringReader(text)));
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

				if (i == 0) {
					this.tm
					.add(new TileMapNorm(data[i], sprite, width, height, blockWidth, blockHeight, tileColumns));
				} else {
					this.tm
					.add(new TileMapObj(data[i], sprite, width, height, blockWidth, blockHeight, tileColumns));
				}
			}
			this.playerCam.setLimit(width * blockWidth, height * blockHeight);

		} catch (Exception e) {
			System.out.println("ERROR - TILEMANAGER: can not read tilemap:");
			e.printStackTrace();
			System.exit(0);
		}

		this.width = width;
		this.height = height;
	}

	public synchronized NormTile[] getNormalTile(Vector2f pos) {
		int normMap = 0;

		NormTile[] block = new NormTile[100];

		int i = 0;
		for (int x = (int) (pos.x - 5); x > (pos.x + 5); x++) {
			for (int y = (int) (pos.y - 5); y > (int) (pos.y + 5); y++) {
				if ((x != pos.x) || (y != pos.y)) {

					block[i] = (NormTile) this.tm.get(normMap).getBlocks()[(y + (x * this.height))];
					i++;
				}


			}
		}

		return block;
	}

	public AABB getRenderViewPort() {
		return new AABB(
				this.playerCam.getTarget().getPos().clone(-(2 * GlobalConstants.BASE_TILE_SIZE),
						-(2 * GlobalConstants.BASE_TILE_SIZE)),
				(10 * GlobalConstants.BASE_TILE_SIZE), (10 * GlobalConstants.BASE_TILE_SIZE));

	}

	//	public AABB getRenderViewPort(Player player) {
	//		com.jrealm.game.tiles.blocks.Tile[] tiles = this.getNormalTile(player.getPos());
	//		if ((tiles == null) || ((tiles[0] == null) && (tiles[63] == null)))
	//			return new AABB(player.getPos(), 0, 0);
	//		com.jrealm.game.tiles.blocks.Tile first = tiles[0];
	//		com.jrealm.game.tiles.blocks.Tile last = tiles[tiles.length - 1];
	//
	//		Vector2f pos = last.getPos();
	//		float width = first.getPos().x - pos.x;
	//		float height = first.getPos().y - pos.y;
	//
	//		AABB viewPort = new AABB(pos.clone(), (int) width, (int) height);
	//
	//		return viewPort;
	//	}

	public void render(Graphics2D g) {
		if (this.playerCam == null)
			return;
		AABB bounds = new AABB(this.playerCam.getTarget().getPos().clone(-132, -132), 312, 312);
		for (int i = 0; i < this.tm.size(); i++) {
			this.tm.get(i).render(g, bounds);
		}
	}
}
