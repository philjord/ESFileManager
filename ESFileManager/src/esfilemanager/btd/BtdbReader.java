package esfilemanager.btd;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
// import java.util.zip.DataFormatException;
// import java.util.zip.Inflater;

import tools.WeakValueHashMap;
import zipBB.Inflater;
import zipBB.ZlibHeaderTrailerParser;
import zipBB.Inflater.InflationException;
import zipBB.ZlibHeaderTrailerParser.ZlibHeader;

/**
 * For desktop to swap File path strings into ByteBuffer or randomaccessfile or io streams Android will have the same
 * for SAF Uri starting points
 * 
 * https://github.com/fo76utils/fo76utils/blob/main/src/btdfile.cpp
 * 
 * Presumably Bethesda Terrain Database format
 * 
 */
public class BtdbReader {

	int		heightMapResX;				// landscape total X resolution (nCellsX * 128)
	int		heightMapResY;				// landscape total Y resolution (nCellsY * 128)
	int		cellMinX;					// X coordinate of cell in SW corner
	int		cellMinY;					// Y coordinate of cell in SW corner
	int		cellMaxX;					// X coordinate of cell in NE corner
	int		cellMaxY;					// Y coordinate of cell in NE corner
	int		nCellsX;					// world map X size in cells
	int		nCellsY;					// world map Y size in cells
	float	worldHeightMin;				// world map minimum height
	float	worldHeightMax;				// world map maximum height
	int		cellHeightMinMaxMapOffs;	// minimum, maximum height for each cell
	int		ltexCnt;					// number of land textures
	int		ltexOffs;					// table of LTEX form IDs
	int		ltexMapOffs;				// 8 x (land texture + 1) for each cell quadrant
	// all vertex color and ground cover related variables are zero for Starfield
	int		gcvrCnt;					// number of ground covers
	int		gcvrOffs;					// table of GCVR form IDs
	int		gcvrMapOffs;				// 8 x ground cover for each cell quadrant
	int		heightMapLOD4;				// 1/8 cell resolution (LOD4) data offset
	int		landTexturesLOD4;			// 1/8 cell resolution (LOD4) data offset
	int		vertexColorLOD4;			// 1/8 cell resolution (LOD4) data offset
	int		nCompressedBlocks;			// number of ZLib compressed blocks
	int		zlibBlocksTableOffs;		// table of compressed block offsets and sizes
	int		zlibBlocksDataOffs;			// ZLib compressed data
	int		zlibBlkTableOffsLOD3;
	int		zlibBlkTableOffsLOD2;
	int		zlibBlkTableOffsLOD1;
	int		zlibBlkTableOffsLOD0;

	public static class TileData {
		short					x0;
		short					y0;

		//FIXME: it appears the block mask nonsense is entirely insdie this file as a cryptic message system
		// bit 0: LOD0 vertex height and land textures are loaded
		public static final int	LOD0vertexheightandlandtextures	= 0x01;
		// bit 1: LOD0 ground cover is loaded
		public static final int	LOD0groundcover					= 0x02;
		// bit 2: LOD1 vertex height and land textures are loaded
		public static final int	LOD1vertexheightandlandtextures	= 0x04;
		// bit 3: IS MISSING! LOD1 terrain color is loaded but doesn't exist
		//would be LOD2terraincolors = 0x08; 
		// bit 4: LOD2 vertex height and land textures are loaded
		public static final int	LOD2vertexheightandlandtextures	= 0x10;
		// bit 5: LOD2 terrain color is loaded
		public static final int	LOD2terraincolor				= 0x20;
		// bit 6: LOD3 vertex height and land textures are loaded
		public static final int	LOD3vertexheightandlandtextures	= 0x40;
		// bit 7: LOD3 terrain color is loaded
		public static final int	LOD3terraincolor				= 0x80;
		// bit 8: LOD4 vertex height and land textures are loaded
		public static final int	LOD4vertexheightandlandtextures	= 0x100;
		// bit 9: LOD4 terrain color is loaded
		public static final int	LOD4terraincolor				= 0x200;

		int						blockMask;
		short[]					hmapData;								// vertex height, 1024 * 1024
		short[]					ltexData;								// land textures, 1024 * 1024
		byte[]					gcvrData;								// ground cover, 1024 * 1024
		short[]					vclrData;								// terrain color, 256 * 256
	};

	WeakValueHashMap<Integer, TileData>	tileCacheMap	= new WeakValueHashMap<Integer, TileData>();
	int									tileCacheIndex;

	//Appalachia.btd file format:
	//
	//0x00000000-0x00000003: "BTDB"
	//0x00000004-0x00000007: (int32) 6, format version?
	//0x00000008-0x0000000B: (float) -700.0, mimimum height
	//0x0000000C-0x0000000F: (float) 38209.996, maximum height
	//0x00000010-0x00000013: (int32) 25728 (201 * 128), total resolution X
	//0x00000014-0x00000017: (int32) 25728 (201 * 128), total resolution Y
	//0x00000018-0x0000001B: (int32) -100, minimum cell X (west)
	//0x0000001C-0x0000001F: (int32) -100, minimum cell Y (south)
	//0x00000020-0x00000023: (int32) 100, maximum cell X (east)
	//0x00000024-0x00000027: (int32) 100, maximum cell Y (north)
	//0x00000028-0x0000002B: (int32) 43, number of land textures (LTEX records)
	//0x0000002C-0x000000D7: 43 * int32, LTEX form IDs
	//0x000000D8-0x0004EF5F: 201 * 201 * float[2], min, max height for each cell
	//0x0004EF60-0x0018A97F: 201 * 2 * 201 * 2 * byte[8], cell quadrant land textures 
	//						 (43 - LTEX_table_index, or zero if none)
	//0x0018A980-0x0018A983: (int32) 30, number of ground covers (GCVR records)
	//0x0018A984-0x0018A9FB: 30 * int32, GCVR form IDs
	//0x0018A9FC-0x002C641B: 201 * 2 * 201 * 2 * byte[8], cell quadrant ground covers 
	//						 (GCVR_table_index, or 0xFF if none)
	//0x002C641C-0x007B4C9B: 201 * 8 * 201 * 8 * int16, LOD4 height map, (64x64 heights, each 8 per cell)
	//						 Notice 64 with 8 inside each is 8 cells covered hence 1/8 the full size
	//                       0 to 65535 maps to minimum height to maximum height
	//0x007B4C9C-0x00CA351B: 201 * 8 * 201 * 8 * int16, LOD4 land textures,
	//                       bits[N * 3 to N * 3 + 2] = texture N alpha (0 to 7),
	//                       texture 6 is the base texture
	//0x00CA351C-0x01191D9B: 201 * 8 * 201 * 8 * int16, LOD4 terrain color
	//                       type in packed 16-bit format, 5 bits per channel
	//0x01191D9C-0x01250643: 97557 * (int32, int32), pairs of zlib compressed data
	//                       offset (relative to 0x01250644) and size
	//0x01250644-EOF:        zlib compressed data blocks
	//zlib compressed blocks     0 to  1351 (26 * 26 * 2): LOD3, 8x8 cell groups (128x128 heights each 16 per cell)
	//                        1352 to  6553 (51 * 51 * 2): LOD2, 4x4 cell groups (128x128 heights each 32 per cell)
	//                        6554 to 16754   (101 * 101): LOD1, 2x2 cell groups (128x128 heights each 64 per cell)
	//                       16755 to 57155   (201 * 201): LOD0 height map, LTEX (128x128 heights each 128 per cell)
	//                       57156 to 97556   (201 * 201): LOD0 ground cover mask
	//LOD3, LOD2: pairs of height map, LTEX and terrain color blocks
	//LOD1: height map, land textures block only
	//
	//height map, LTEX block format:
	//  0x0000-0x5FFF: 128 * (64, 128) int16, height map, only the samples (height values) not present at lower levels of detail
	//  0x6000-0xBFFF: 128 * (64, 128) int16, land texture alphas
	//
	//terrain color block format (LOD3 and LOD2 only):
	//  0x0000-0x5FFF: 128 * (64, 128) int16
	//  0x6000-0x7FFF: unused
	// 
	//ground cover block format (LOD0 only):
	//  0x0000-0x3FFF: 128 * 128 bytes, bit N is set if ground cover N is enabled
	//
	// In the above the 128 * (64, 128) represents the fact only the "new detail" is stored for each
	// decreasing LOD number, so LOD2 has 1 row of 64 values from LOD3 already and hence needs 64 value to make that
	// 128, but then also need to add a new 128 value row below that as well, giving 4x the detail as LOD3
	//
	//In Starfield BTD files, the SW and NE cell coordinates (0x0018-0x0027) are
	//zero, and terrain color and ground cover data are not present.
	//Height map and land texture blocks are in the following format:
	//  0x0000-0x7FFF: 128 * 128 int16 height map
	//  0x8000-0xFFFF: 128 * 128 int16 land texture alphas

	private FileChannel					fileChannel;
	private ByteBuffer					buf;

	public BtdbReader(FileChannel fileChannel) throws IOException {
		this.fileChannel = fileChannel;
		this.buf = fileChannel.map(MapMode.READ_ONLY, 0, fileChannel.size());
		this.buf.order(ByteOrder.LITTLE_ENDIAN);
	}

	public void load() throws IOException {
		if (!checkType(buf.getInt(), "BTDB"))
			throw new IOException("input file format is not BTD");
		int format = buf.getInt();
		if (format != 5 && format != 6) // must be 5 or 6 
			throw new IOException("unsupported BTD format version " + format);
		tileCacheIndex = 0;
		// TODO: check header data for errors
		worldHeightMin = buf.getFloat();
		worldHeightMax = buf.getFloat();
		heightMapResX = buf.getInt();
		heightMapResY = buf.getInt();
		cellMinX = buf.getInt();
		cellMinY = buf.getInt();
		cellMaxX = buf.getInt();
		cellMaxY = buf.getInt();
		boolean isStarfieldBTD = !(cellMinX != 0 | cellMinY != 0 | cellMaxX != 0 | cellMaxY != 0);
		if (isStarfieldBTD) {
			worldHeightMin *= 8.0f; // FIXME: Z scale may be incorrect
			worldHeightMax *= 8.0f;
			cellMinX = -(heightMapResX >> 8);
			cellMinY = -(heightMapResY >> 8);
			cellMaxX = cellMinX + (heightMapResX >> 7) - 1;
			cellMaxY = cellMinY + (heightMapResY >> 7) - 1;
		}
		nCellsX = cellMaxX + 1 - cellMinX;
		nCellsY = cellMaxY + 1 - cellMinY;
		ltexCnt = buf.getInt();
		ltexOffs = buf.position();
		for (int i = 0; i < ltexCnt; i++)
			buf.getInt();
		cellHeightMinMaxMapOffs = buf.position();
		buf.position(buf.position() + ((nCellsY * nCellsX) << 3));
		ltexMapOffs = buf.position();
		buf.position(buf.position() + ((nCellsY * nCellsX) << 5));
		if (isStarfieldBTD) {
			gcvrCnt = 0;
			gcvrOffs = 0;
			gcvrMapOffs = 0;
		} else {
			gcvrCnt = buf.getInt();
			gcvrOffs = buf.position();
			for (int i = 0; i < gcvrCnt; i++)
				buf.getInt();
			gcvrMapOffs = buf.position();
			buf.position(buf.position() + ((nCellsY * nCellsX) << 5));
		}
		heightMapLOD4 = buf.position();
		buf.position(buf.position() + ((nCellsY * nCellsX) << 7));
		landTexturesLOD4 = buf.position();
		buf.position(buf.position() + ((nCellsY * nCellsX) << 7));
		if (isStarfieldBTD) {
			vertexColorLOD4 = 0;
			zlibBlocksTableOffs = buf.position();
			zlibBlkTableOffsLOD3 = buf.position();
			buf.position(buf.position() + (((nCellsY + 7) >> 3) * ((nCellsX + 7) >> 3) * 8));
			zlibBlkTableOffsLOD2 = buf.position();
			buf.position(buf.position() + (((nCellsY + 3) >> 2) * ((nCellsX + 3) >> 2) * 8));
			zlibBlkTableOffsLOD1 = buf.position();
			buf.position(buf.position() + (((nCellsY + 1) >> 1) * ((nCellsX + 1) >> 1) * 8));
			zlibBlkTableOffsLOD0 = buf.position();
			buf.position(buf.position() + (nCellsY * nCellsX * 8));
		} else {
			vertexColorLOD4 = buf.position();
			buf.position(buf.position() + ((nCellsY * nCellsX) << 7));
			zlibBlocksTableOffs = buf.position();
			zlibBlkTableOffsLOD3 = buf.position();
			buf.position(buf.position() + (((nCellsY + 7) >> 3) * ((nCellsX + 7) >> 3) * 2 * 8));
			zlibBlkTableOffsLOD2 = buf.position();
			buf.position(buf.position() + (((nCellsY + 3) >> 2) * ((nCellsX + 3) >> 2) * 2 * 8));
			zlibBlkTableOffsLOD1 = buf.position();
			buf.position(buf.position() + (((nCellsY + 1) >> 1) * ((nCellsX + 1) >> 1) * 8));
			zlibBlkTableOffsLOD0 = buf.position();
			buf.position(buf.position() + (nCellsY * nCellsX * 2 * 8));
		}
		zlibBlocksDataOffs = buf.position();
		nCompressedBlocks = (buf.position() - zlibBlocksTableOffs) >> 3;
		buf.get(); // TODO: why are we reading asingle byte off,
		//the format is contiguous? is this just to ensure we have compressed data?
	}

	public int getCellMinX() {
		return cellMinX;
	}

	public int getCellMinY() {
		return cellMinY;
	}

	public int getCellMaxX() {
		return cellMaxX;
	}

	public int getCellMaxY() {
		return cellMaxY;
	}

	public float getMinHeight() {
		return worldHeightMin;
	}

	public float getMaxHeight() {
		return worldHeightMax;
	}

	public int getLandTextureCount() {
		return ltexCnt;
	}

	public int getGroundCoverCount() {
		return gcvrCnt;
	}

	public int getLandTexture(int n) {
		if (n >= ltexCnt)
			return 0;
		return readUInt32(ltexOffs + (n << 2));
	}

	public int getGroundCover(int n) {
		if (n >= gcvrCnt)
			return 0;
		return readUInt32(gcvrOffs + (n << 2));
	}

	/** N = 128 >> l, buffer size = N * N
	 height map in pixelFormatGRAY16 format	
	 * @param buf
	 * @param cellX
	 * @param cellY
	 * @param lod
	 */
	public void getCellHeightMap(short[] buf, int cellX, int cellY, int lod) {
		TileData tileData = loadTile(cellX, cellY, //
				(~0 << (lod + lod)) & (0x0155));//0x0155=0b000101010101
		int x0 = ((cellX - cellMinX) & 7) << 7;
		int y0 = ((cellY - cellMinY) & 7) << 7;
		int n = 128 >> lod;
		byte m = (byte)(7 - lod);
		for (int yc = 0; yc < n; yc++) {
			for (int xc = 0; xc < n; xc++) {
				buf[(yc << m) | xc] = tileData.hmapData[((y0 + (yc << lod)) << 10) + x0 + (xc << lod)];
			}
		}

		//System.out.println("getCellHeightMap cellX="	+ cellX + ", cellY=" + cellY + ",lod=" + lod + " filled "
		//					+ ((n << m) | n) + " shorts");

	}

	/** 
	 * OK so new news, the LOD4 data is required by every Lod below it, as each only includes new height data
	 * This accounts for the 128 x (64 or 128) in the text above, as that's reading the missing bits, plus a 
	 * full new row interleaved (so half the row is in the above LOD and then a new row below that) 
	 * I now have a fat LOD4 system and then also the LOD3 down to LOD0 as "per cell"
	 * If I have 3/4 LOD4 at any given time say (so I'm looking a the 50x50 around at better lods) then I just
	 * have 2,500 cells around, and later when I feel keen a load can read say 1+3 extra LOD3 to make each 
	 * scene object cover twice as much, 50 is 50x52m = 2.5km of less than LOD4 so possibly too much anyway
	 * I might start with say 2LOD0 2LOD1 2LOD 2LOD3 = 8 plus center = 9 =  a 9x9 which is only 81, plus the gap fillers  
	 * 1-3 return is 128x128 across so,
	 * lod1=2x2 cells, 128x128 values so 64x64 grid
	 * lod2=4x4 cells, 128x128 values so 32x32 grid
	 * lod3=8x8 cells, 128x128 values so 16x16 grid
	 * lod4=8x8 cells, but 64x64 values so only 8x8 grid in each cell
	 * Returns 64x64 shorts in each hmapData and vclrData, representing 8 values per cell, across 8 cells worth of distance
	 * height map in pixelFormatGRAY16 format
	 * 	 
	 * @param hmapData
	 * @param ltexData
	 * @param vclrData
	 * @param cellX you should call (x % 8 == 0) only so 0,8,16,24,etc up to 200 (25 sets across)
	 * but the issue is you should call that with the 100 offset so please only call -100 to 100 mod 8
	 * being -100, -92, -86 etc up to 100 
	 * @param cellY
	 */
	public void getLOD4Data(short[] hmapData, short[] vclrData, int cellX, int cellY) {

		int x0 = (cellX - cellMinX); // as a cell from 0 to 201, not -100 to 100, in the lod4 8 grid world
		int y0 = (cellY - cellMinY);

		if (x0 % 8 != 0)
			System.out.println("x0%8!=0 " + x0 + " from cellX " + cellX);
		if (y0 % 8 != 0)
			System.out.println("y0%8!=0 " + y0 + " from cellY " + cellY);

		// don't go below 0 nor close than 8 to the max side
		//TODO: a call to cell 200 won't return what the user wants, as that call only has 2 cells left 200 and 201
		// so instead of this fix I should just limit the yy and xx to 16 in that case
		if (x0 < 0)
			x0 = 0;
		if (x0 > nCellsX - 8)
			x0 = nCellsX - 8;
		if (y0 < 0)
			y0 = 0;
		if (y0 > nCellsY - 8)
			y0 = nCellsY - 8;

		//0x002C641C-0x007B4C9B: 201 * 8 * 201 * 8 * int16, LOD4 height map, (64x64 heights, each 8 per cell)
		//						 Notice 64 with 8 inside each is 8 cells covered hence 1/8 the full size
		//                       0 to 65535 maps to minimum height to maximum height
		//0x007B4C9C-0x00CA351B: 201 * 8 * 201 * 8 * int16, LOD4 land textures,
		//                       bits[N * 3 to N * 3 + 2] = texture N alpha (0 to 7),
		//                       texture 6 is the base texture
		//0x00CA351C-0x01191D9B: 201 * 8 * 201 * 8 * int16, LOD4 terrain color
		//                       type in packed 16-bit format, 5 bits per channel
		int idx = 0;
		for (int yy = 0; yy < 64; yy++) {
			for (int xx = 0; xx < 64; xx++) {
				int offs = (((y0 * 8) + yy) * (nCellsX * 8)) + ((x0 * 8) + xx);
				hmapData[idx] = readUInt16(heightMapLOD4 + (offs * 2));
				//ltex is no use to a lod4
				//ltexData[idx] = readUInt16(landTexturesLOD4 + (offs * 2));
				vclrData[idx] = readUInt16(vertexColorLOD4 + (offs * 2));
				idx++;
			}
		}
	}

	/**
	 * return a float[2] of CellHeightMinMax
	 * @param buf
	 * @param cellX
	 * @param cellY
	 */
	public void getCellHeightMinMax(float[] buf, int cellX, int cellY) {
		//cellHeightMinMaxMapOffs, skipped like this
		//buf.position(buf.position() + ((nCellsY * nCellsX) << 3)); 4 then double for 2 floats
		//0x000000D8-0x0004EF5F: 201 * 201 * float[2], min, max height for each cell
		int x = cellX - cellMinX;
		int y = cellY - cellMinY;

		buf[0] = readFloat(cellHeightMinMaxMapOffs + (((y * nCellsX) + x) << 3) + 0);
		buf[1] = readFloat(cellHeightMinMaxMapOffs + (((y * nCellsX) + x) << 3) + 4);
	}

	/** land texture opacities in pixelFormatA16 format (see filebuf.hpp)
	 * N = 128 >> l, buffer size = N * N
	 * @param buf
	 * @param cellX
	 * @param cellY
	 * @param lod
	 */
	public void getCellLandTexture(short[] buf, int cellX, int cellY, int lod) {
		TileData tileData = loadTile(cellX, cellY, (~0 << (lod + lod)) & 0x0155);
		int x0 = ((cellX - cellMinX) & 7) << 7;
		int y0 = ((cellY - cellMinY) & 7) << 7;
		int n = 128 >> lod;
		byte m = (byte)(7 - lod);
		for (int yc = 0; yc < n; yc++) {
			for (int xc = 0; xc < n; xc++) {
				int tmp = tileData.ltexData[((y0 + (yc << lod)) << 10) + x0 + (xc << lod)];
				tmp = ((tmp & 0x7E00) >> 9) | (tmp & 0x01C0) | ((tmp & 0x003F) << 9);
				tmp = ((tmp & 0x7038) >> 3) | (tmp & 0x01C0) | ((tmp & 0x0E07) << 3);
				buf[(yc << m) | xc] = (short)tmp;
			}
		}
	}

	/** ground cover mask in pixelFormatA8 format
	 * N = 128 >> l, buffer size = N * N
	 * @param buf
	 * @param cellX
	 * @param cellY
	 * @param lod
	 */
	public void getCellGroundCover(byte[] buf, int cellX, int cellY, int lod) {
		TileData tileData = loadTile(cellX, cellY, 0x0002);
		int x = cellX - cellMinX;
		int y = cellY - cellMinY;
		int x0 = (x & 7) << 7;
		int y0 = (y & 7) << 7;
		int n = 128 >> lod;
		byte m = (byte)(7 - lod);
		int gcvrMask = 0;
		for (int yc = 0; yc < n; yc++) {
			for (int xc = 0; xc < n; xc++) {
				if (!((xc & ((n >> 1) - 1)) != 0)) {
					int offs = ((((y << 1) | (yc >> (m - 1))) * (nCellsX << 1) + ((x << 1) | (xc >> (m - 1)))) << 3)
								+ gcvrMapOffs;
					for (byte i = 8; i-- > 0; offs++)
						gcvrMask |= (((readUInt8(offs) & 0xff) < gcvrCnt) ? 1 : 0 << i);
				}
				int tmp = tileData.gcvrData[((y0 + (yc << lod)) << 10) + x0 + (xc << lod)] & 0xFF;

				//Computes the result of bitwise left-rotating the value of x by s positions. This operation is also known as a left circular shift. 
				// tmp = std::rotl(std::uint8_t(tmp), 4);
				tmp = rotateLeft((byte)tmp, 4) & 0xFF;
				tmp = ((tmp & 0xCC) >> 2) | ((tmp & 0x33) << 2);
				tmp = (((tmp & 0xAA) >> 1) | ((tmp & 0x55) << 1)) & gcvrMask;
				buf[(yc << m) | xc] = (byte)tmp;
			}
		}
	}

	//my version of Integer.rotateLeft(tmp, 4); but for a byte 
	//TODO: is this correct? I've never loaded ground cover so not ever tested this call
	//https://labex.io/tutorials/java-how-to-perform-bit-rotation-in-java-414101
	public static byte rotateLeft(byte x, int distance) {
		//return (byte)((x << distance) | (x >>> -distance));
		return (byte)((x << 2) | (x >> (32 - 2)));
	}

	/** vertex colors in pixelFormatRGBA16 format, l >= 2
	N = 128 >> l, buffer size = N * N
	 * @param buf
	 * @param cellX
	 * @param cellY
	 * @param lod
	 */
	public void getCellTerrainColor(short[] buf, int cellX, int cellY, int lod) {
		TileData tileData = loadTile(cellX, cellY, (~0 << (lod + lod)) & 0x02A0);
		int x0 = ((cellX - cellMinX) & 7) << 5;
		int y0 = ((cellY - cellMinY) & 7) << 5;
		int n = 128 >> lod;
		byte m = (byte)(7 - lod);
		for (int yc = 0; yc < n; yc++) {
			int yy = (lod >= 2 ? (yc << (lod - 2)) : (yc >> (2 - lod)));
			for (int xc = 0; xc < n; xc++) {
				int xx = (lod >= 2 ? (xc << (lod - 2)) : (xc >> (2 - lod)));
				buf[(yc << m) | xc] = tileData.vclrData[((y0 + yy) << 8) + x0 + xx];
			}
		}
	}

	/**	 8x16 bytes SWTexID,SWGCID,SETexID,SEGCID,NWTexID,NWGCID,NETexID,NEGCID ,  each is 6 skip 2 then 8 bytes
	 buf[0..5]   = SW quadrant land texture IDs (0xFF: no texture), getLandTexture() returns the corresponding form IDs
	 buf[8..15]  = SW quadrant ground cover IDs (0xFF: none), getGroundCover() returns the corresponding form IDs
	 buf[16..31] = SE quadrant land texture and ground cover IDs
	 buf[32..63] = NW and NE quadrants	
	 * @param buf requires a byte[64] for return
	 * @param cellX
	 * @param cellY
	 */
	public void getCellTextureSet(byte[] buf, int cellX, int cellY) {
		int x = cellX - cellMinX;
		int y = cellY - cellMinY;
		for (int q = 0; q < 4; q++) {
			int offs = (((y << 1) | (q >> 1)) * (nCellsX << 1) + ((x << 1) | (q & 1))) << 3;
			int t = (q << 4);
			for (int i = 0; i < 8; i++) {
				byte tmp = readUInt8(ltexMapOffs + offs + i);
				if (tmp == 0 || tmp > ltexCnt)
					tmp = (byte)0xFF;
				else
					tmp = (byte)(ltexCnt - tmp);
				if (i < 5)
					buf[t + 5 - i] = tmp;
				if (tmp != 0xFF)
					buf[t + 0] = tmp; // default texture
			}
			buf[t + 6] = (byte)0xFF;
			buf[t + 7] = (byte)0xFF;
			int g = ((q << 4) + 8);
			for (int i = 0; i < 8; i++) {
				byte tmp = readUInt8(gcvrMapOffs + offs + i);
				if (tmp >= gcvrCnt)
					tmp = (byte)0xFF;
				buf[g + 7 - i] = tmp;
			}
		}
		//	System.out.println(
		//			"getCellTextureSet cellX=" + cellX + ", cellY=" + cellY + " filled " + (((3 << 4) + 8) + 7) + " bytes");

	}

	private static void loadBlockLines_8(byte[] dst, int destOff, byte[] src, int srcOff) {
		int i = 0;
		for (i = 0; i < 128; i++)
			dst[destOff++] = src[srcOff++];
		destOff = destOff + (1024 - 128);
		for (i = 0; i < 128; i++)
			dst[destOff++] = src[srcOff++];
	}

	private static void loadBlockLines_16(short[] dst, int destOff, byte[] src, int srcOff, int xd, int yd) {
		for (int i = 0; i < 64; i++, destOff = destOff + xd, srcOff = srcOff + 2) {
			destOff = destOff + xd;
			dst[destOff] = readUInt16Fast(src, srcOff);
		}
		destOff = destOff + yd;
		for (int i = 0; i < 128; i++, destOff = destOff + xd, srcOff = srcOff + 2) {
			dst[destOff] = readUInt16Fast(src, srcOff);
		}
	}

	private static short readUInt16Fast(byte[] src, int srcOff) {
		// as always little endian
		//bb.order(ByteOrder.LITTLE_ENDIAN); style
		return (short)(((src[srcOff + 1] & 0xff) << 8) + (src[srcOff + 0] & 0xff));
	}

	private void loadBlock(	TileData tileData, int dataOffs, int cellId, int lod, boolean otherData, byte[] zlibBuf,
							Inflater inflater)
			throws IOException {
		if (lod >= 2)
			cellId = (cellId << 1) + (otherData ? 1 : 0);
		else if (lod == 0 && otherData)
			cellId = cellId + (nCellsY * nCellsX);
		int zlibBlkTableOffs = zlibBlkTableOffsLOD0 + (cellId * (4 + 4));// each is a pair of ints for offset and size
		if (lod == 1)
			zlibBlkTableOffs = zlibBlkTableOffsLOD1 + (cellId * (4 + 4));
		else if (lod == 2)
			zlibBlkTableOffs = zlibBlkTableOffsLOD2 + (cellId * (4 + 4));
		else if (lod == 3)
			zlibBlkTableOffs = zlibBlkTableOffsLOD3 + (cellId * (4 + 4));
		int offs = buf.getInt(zlibBlkTableOffs) + zlibBlocksDataOffs;
		int compressedSize = buf.getInt(zlibBlkTableOffs + 4);
		if ((offs + compressedSize) > fileChannel.size())
			throw new IOException("end of input file");

		/*	synchronized (buf) {
				ByteBuffer input = buf.position(offs).slice();
				input.limit(compressedSize);
				inflater.setInput(input);
			}
		
			try {
				int count = inflater.inflate(zlibBuf);
				if (count != ((lod == 0 && otherData) ? 16384 : ((lod >= 2 && otherData) ? 32768 : 49152)))
					System.err.println("Inflate count issue! count = "	+ count + " expected "
										+ ((lod == 0 && otherData) ? 16384 : ((lod >= 2 && otherData) ? 32768 : 49152)));
			} catch (DataFormatException e) {
				System.err.println("loadBlock Inflater DataFormatException ");
			}
			inflater.reset();*/
		ByteBuffer input = null;
		synchronized (buf) {
			input = buf.position(offs).slice();
		}
		input.limit(compressedSize);

		ByteBuffer dstBuf = ByteBuffer.wrap(zlibBuf);
		ZlibHeader header = ZlibHeaderTrailerParser.parseHeader(input);
		if (header == null) {
			throw new AssertionError("Expect to read the header in the first try since the buffer is large enough.");
		}
		if (header.dictChecksum != 0) {
			throw new AssertionError("Expect to have no dictionary.");
		}
		try {
			boolean done = false;
			while (!done) {
				switch (inflater.inflate(dstBuf, input)) {
					case NEED_MORE_INPUT:
						//not sure about this but keep trucking anyway
						break;
					case NEED_MORE_OUTPUT:
						//only does 65k per call just keep going
						break;
					case DONE:
						done = true;
						break;
				}
			}

		} catch (InflationException e) {
			e.printStackTrace();
		}

		inflater.reset();

		int pOff = 0;
		int xd = 1 << (!otherData || lod == 0 ? lod : (lod - 2));
		int yd = (!otherData || lod == 0 ? (1024 - 128) : ((256 - 128) >> 2)) << lod;

		if (otherData) {
			for (int y = 0; y < 128; y = y + 2) {
				if (lod == 0) {// ground cover
					loadBlockLines_8(tileData.gcvrData, dataOffs + (y << 10), zlibBuf, pOff);
					pOff = pOff + 256;
				} else { // vertex color
					loadBlockLines_16(tileData.vclrData, dataOffs + (y << (lod + 6)), zlibBuf, pOff, xd, yd);
					pOff = pOff + 384;
				}
			}
		} else {
			for (int y = 0; y < 128; y = y + 2) {
				// vertex height  
				loadBlockLines_16(tileData.hmapData, dataOffs + (y << (lod + 10)), zlibBuf, pOff, xd, yd);
				pOff = pOff + 384;
			}
			for (int y = 0; y < 128; y = y + 2) {
				// landscape textures
				loadBlockLines_16(tileData.ltexData, dataOffs + (y << (lod + 10)), zlibBuf, pOff, xd, yd);
				pOff = pOff + 384;
			}
		}
	}

	private void loadBlocks(TileData tileData, int x, int y, int threadIndex, int threadCnt, int blockMask)
			throws Exception {

		if (!(vertexColorLOD4 != 0)) {
			loadBlocks_SF(tileData, x, y, threadIndex, threadCnt, blockMask);
			return;
		}
		// both for reuse below as deburners
		Inflater inflater = new Inflater();
		byte[] zlibBuf = new byte[0xC000];
		int t = 0;
		// LOD3..LOD0
		for (byte lod = 4; lod-- > 0;) {
			for (int yy = 0; yy < (8 >> lod); yy++) {
				int yc = (y >> lod) + yy;
				if (yc >= ((nCellsY + (1 << lod) - 1) >> lod))
					break;
				for (int xx = 0; xx < (8 >> lod); xx++) {
					int xc = (x >> lod) + xx;
					if (xc >= ((nCellsX + (1 << lod) - 1) >> lod))
						break;
					if (t == threadIndex) {
						int cellId = yc * ((nCellsX + (1 << lod) - 1) >> lod) + xc;
						if (((blockMask & 0x55) & (1 << (lod + lod))) != 0)
							loadBlock(tileData, ((yy << 10) + xx) << (lod + 7), cellId, lod, false, zlibBuf, inflater);
						if (((blockMask & 0xA0) & (1 << (lod + lod + 1))) != 0)
							loadBlock(tileData, ((yy << 8) + xx) << (lod + 5), cellId, lod, true, zlibBuf, inflater);
						if (((blockMask & 0x02) & (1 << (lod + lod + 1))) != 0)
							loadBlock(tileData, ((yy << 10) + xx) << (lod + 7), cellId, lod, true, zlibBuf, inflater);
					}
					if (++t >= threadCnt)
						t = 0;
				}
			}
		}
	}

	private void loadBlock_SF(TileData tileData, int dataOffs, int n, int lod, byte[] zlibBuf, Inflater inflater)
			throws IOException {
		int zlibBlkTableOffs = zlibBlkTableOffsLOD0 + (n << 3);
		if (lod == 1)
			zlibBlkTableOffs = zlibBlkTableOffsLOD1 + (n << 3);
		else if (lod == 2)
			zlibBlkTableOffs = zlibBlkTableOffsLOD2 + (n << 3);
		else if (lod == 3)
			zlibBlkTableOffs = zlibBlkTableOffsLOD3 + (n << 3);
		int offs = buf.getInt(zlibBlkTableOffs) + zlibBlocksDataOffs;
		int compressedSize = buf.getInt(zlibBlkTableOffs + 4);
		if ((offs + compressedSize) > fileChannel.size())
			throw new IOException("end of input file");

		/*synchronized (buf) {
			ByteBuffer input = buf.position(offs).slice();
			input.limit(compressedSize);
			inflater.setInput(input);
		}
		
		try {
			int count = inflater.inflate(zlibBuf);
			if (count != 65536)
				System.err.println("Inflate count issue! count = " + count + " expected " + 65536);
		} catch (DataFormatException e) {
			System.err.println("loadBlock_SF Inflater DataFormatException ");
		}
		inflater.reset();*/

		ByteBuffer input = null;
		synchronized (buf) {
			input = buf.position(offs).slice();
		}
		input.limit(compressedSize);

		ByteBuffer dstBuf = ByteBuffer.wrap(zlibBuf);
		ZlibHeader header = ZlibHeaderTrailerParser.parseHeader(input);
		if (header == null) {
			throw new AssertionError("Expect to read the header in the first try since the buffer is large enough.");
		}
		if (header.dictChecksum != 0) {
			throw new AssertionError("Expect to have no dictionary.");
		}
		try {
			boolean done = false;
			while (!done) {
				switch (inflater.inflate(dstBuf, input)) {
					case NEED_MORE_INPUT:
						//not sure about this but keep trucking anyway
						break;
					case NEED_MORE_OUTPUT:
						//only does 65k per call just keep going
						break;
					case DONE:
						done = true;
						break;
				}
			}

		} catch (InflationException e) {
			e.printStackTrace();
		}

		inflater.reset();

		int pOff = 0;
		int xd = 1 << lod;
		for (int y = 0; y < 128; y++) {// vertex height
			short[] dst = tileData.hmapData;
			int dstOff = (dataOffs + (y << (lod + 10)));
			for (int i = 0; i < 128; i++, dstOff = dstOff + xd, pOff = pOff + 2)
				dst[dataOffs] = readUInt16Fast(zlibBuf, pOff);

		}
		for (int y = 0; y < 128; y++) { // landscape textures
			short[] dst = tileData.ltexData;
			int dstOff = (dataOffs + (y << (lod + 10)));
			for (int i = 0; i < 128; i++, dstOff = dstOff + xd, pOff = pOff + 2)
				dst[dataOffs] = readUInt16Fast(zlibBuf, pOff);
		}
	}

	private void loadBlocks_SF(TileData tileData, int x, int y, int threadIndex, int threadCnt, int blockMask)
			throws IOException {
		Inflater inflater = new Inflater();
		byte[] zlibBuf = new byte[0x10000];
		int t = 0;
		// LOD0..LOD3
		for (byte l = 0; l < 4; l++) {
			if (!((blockMask & (1 << (l + l))) != 0))
				continue;
			for (int yy = 0; yy < (8 >> l); yy++) {
				int yc = (y >> l) + yy;
				if (yc >= ((nCellsY + (1 << l) - 1) >> l))
					break;
				for (int xx = 0; xx < (8 >> l); xx++) {
					int xc = (x >> l) + xx;
					if (xc >= ((nCellsX + (1 << l) - 1) >> l))
						break;
					if (t == threadIndex) {
						int n = yc * ((nCellsX + (1 << l) - 1) >> l) + xc;
						loadBlock_SF(tileData, ((yy << 10) + xx) << (l + 7), n, l, zlibBuf, inflater);
					}
					if (++t >= threadCnt)
						t = 0;
				}
			}
			break;
		}
	}

	// we must implement our own caching and cut out the CompressedTextureLoader
	private static Map<Integer, TileData>	loadedTiles		= Collections
			.synchronizedMap(new WeakValueHashMap<Integer, TileData>());

	// we can't request the same file at the same time, this tell threads to wait for each other
	// key to mask of loaded data
	private static Map<Integer, Integer>	loadingTiles	= Collections
			.synchronizedMap(new HashMap<Integer, Integer>());

	private TileData loadTile(int cellX, int cellY, int blockMask) {

		//getCellGroundCover(byte[] buf, int cellX, int cellY, int lod) {
		//TileData tileData = loadTile(cellX, cellY, 0x0002);
		//0x0002=0b0010
		//getCellHeightMap(short[] buf, int cellX, int cellY, int lod) {
		//TileData tileData = loadTile(cellX, cellY, (~0 << (lod + lod)) & (0x0155));
		//0x0155=0b0001 0101 0101
		//getCellLandTexture(short[] buf, int cellX, int cellY, int lod) {
		//TileData tileData = loadTile(cellX, cellY, (~0 << (lod + lod)) & 0x0155);
		//again
		//getCellTerrainColor(short[] buf, int cellX, int cellY, int lod) {
		//TileData tileData = loadTile(cellX, cellY, (~0 << (lod + lod)) & 0x02A0);
		//0x02A0=0b0010 1010 0000

		// below the mask is also checked against 0x03F7=0b0011 1111 0111 or all of them

		//it appears the block mask nonsense is entirely inside this file 
		/*		// bit 0: LOD0 vertex height and land textures are loaded
				public static final int	LOD0vertexheightandlandtextures	= 0x01;
				// bit 1: LOD0 ground cover is loaded
				public static final int	LOD0groundcover					= 0x02;
				// bit 2: LOD1 vertex height and land textures are loaded
				public static final int	LOD1vertexheightandlandtextures	= 0x04;
				// bit 3: IS MISSING! LOD1 terrain color doesn't exist
				//would be LOD1terraincolors = 0x08; 
				// bit 4: LOD2 vertex height and land textures are loaded
				public static final int	LOD2vertexheightandlandtextures	= 0x10;
				// bit 5: LOD2 terrain color is loaded
				public static final int	LOD2terraincolor				= 0x20;
				// bit 6: LOD3 vertex height and land textures are loaded
				public static final int	LOD3vertexheightandlandtextures	= 0x40;
				// bit 7: LOD3 terrain color is loaded
				public static final int	LOD3terraincolor				= 0x80;
				// bit 8: LOD4 vertex height and land textures are loaded
				public static final int	LOD4vertexheightandlandtextures	= 0x100;
				// bit 9: LOD4 terrain color is loaded
		*/

		synchronized (tileCacheMap) {
			int x0 = (cellX - cellMinX) & 0xFFF8;
			int y0 = (cellY - cellMinY) & 0xFFF8;
			int cacheKey = (y0 << 16) | x0;

			//physics and visuals and bullet all want this data concurrently, however the weak value might have it or might not
			// and it the root object exists it might not be fully loaded yet

			Integer loadingMask = null;
			// in one go check if it's loaded or loading, and then indicate this thread will load it
			synchronized (loadedTiles) {
				TileData t = loadedTiles.get(cacheKey);
				if (t == null) {
					loadingMask = loadingTiles.get(cacheKey);
					if (loadingMask == null) {
						// no ones loading it, so time to load it up and indicate the mask it will finish as
						loadingTiles.put(cacheKey, blockMask);
					} else {
						//otherwise we wait and later check if it got loaded as we want, if not do our load
					}
				}

			}

			if (loadingMask != null) {
				// just wait until loaded then check if it fulfils use
				while (true) {
					try {
						Thread.sleep(5);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					loadingMask = loadingTiles.get(cacheKey);
					if (loadingMask != null) {
						synchronized (loadedTiles) {
							TileData t = loadedTiles.get(cacheKey);
							// did it return a TileData mask that fulfills us?
							blockMask = (blockMask & ~(t.blockMask)) & 0x03F7;
							// if it won't fulfill our requirements then we will need to load it
							if (!(blockMask != 0)) {
								return t;
							} else {
								loadingTiles.put(cacheKey, blockMask);
								loadingMask = blockMask;
								break;
							}
						}
					}
				}

			}

			TileData tileData = new TileData();

			// checks if the mask data I want is loaded into the tile, it's part of the loading system
			blockMask = (blockMask & ~(tileData.blockMask)) & 0x03F7;
			if (!(blockMask != 0))
				return tileData;

			if ((blockMask & 0x0155) != 0) {
				tileData.hmapData = new short[0x00100000];//1,048,576
				tileData.ltexData = new short[0x00100000];
			}
			if ((blockMask & 0x0002) != 0)
				tileData.gcvrData = new byte[0x00100000];
			if ((blockMask & 0x02A0) != 0)
				tileData.vclrData = new short[0x00010000];
			if (!(vertexColorLOD4 != 0)) {
				// this looks like if the file has no vcolors but they are asked for hand back white not black			
				if ((blockMask & 0x02A0) != 0) {
					//     std::memset(tileData.vclrData.data(), 0xFF, sizeof(std::uint16_t) << 16);
					for (int i = 0; i < (2 << 16); i++)
						tileData.vclrData[i] = 0xFF;
				}
				blockMask &= 0x0155;
			}

			// LOD4
			if ((blockMask & 0x0300) != 0) {
				for (int yy = 0; yy < 64; yy++) {
					if ((cellMinY + ((yy >> 3) + y0)) > cellMaxY)
						break;
					for (int xx = 0; xx < 64; xx++) {
						if ((cellMinX + ((xx >> 3) + x0)) > cellMaxX)
							break;
						int offs = (yy + (y0 << 3)) * (nCellsX << 3) + (xx + (x0 << 3));
						if ((blockMask & 0x0155) != 0) {
							tileData.hmapData[((yy << 10) + xx) << 4] = readUInt16(heightMapLOD4 + (offs << 1));
							tileData.ltexData[((yy << 10) + xx) << 4] = readUInt16(landTexturesLOD4 + (offs << 1));
						}
						if ((blockMask & 0x02A0) != 0) {
							tileData.vclrData[((yy << 8) + xx) << 2] = readUInt16(vertexColorLOD4 + (offs << 1));
						}
					}
				}
			}
			// LOD3..LOD0
			final int threadCnt = 4;
			ArrayList<Thread> threads = new ArrayList<Thread>(threadCnt);
			for (int i = 0; i < threadCnt; i++) {
				try {
					final int bm = blockMask;
					final int threadIdx = i;
					Thread th = new Thread() {
						@Override
						public void run() {
							try {
								loadBlocks(tileData, x0, y0, threadIdx, threadCnt, bm);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					};
					threads.add(th);
					th.start();

				} catch (Exception e) {
					e.printStackTrace();
					break;
				}
			}
			for (int i = 0; i < threadCnt; i++) {
				if (threads.get(i) != null) {
					try {
						threads.get(i).join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			tileData.blockMask = tileData.blockMask | blockMask;

			synchronized (loadedTiles) {
				loadedTiles.put(cacheKey, tileData);
				loadingTiles.remove(cacheKey);
			}

			return tileData;
		}

	}

	private int readUInt32(int offs) {
		return buf.getInt(offs);
	}

	private float readFloat(int offs) {
		return buf.getFloat(offs);
	}

	private byte readUInt8(int offs) {
		return buf.get(offs);
	}

	private short readUInt16(int offs) {
		return buf.getShort(offs);
	}

	//check a 4 char string with a 4 byte int
	private static boolean checkType(int i, String s) {
		byte[] b = s.getBytes();
		return ((b[3] << 24) | (b[2] << 16) | (b[1] << 8) | (b[0] << 0)) == i;
	}

}
