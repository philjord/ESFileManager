package esfilemanager.btd;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import tools.WeakValueHashMap;

/** For desktop to swap File path strings into ByteBuffer or randomaccessfile or io streams
 Android will have the same for SAF Uri starting points
 
https://github.com/fo76utils/fo76utils/blob/main/src/btdfile.cpp

Presumably Bethesda Terrain Database format


Next step, download f076utils and dump the raw height map for a cell out then compare to my data dump
sad but faster than playing

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
	int		heightMapLOD4;				// 1/8 cell resolution
	int		landTexturesLOD4;			// 1/8 cell resolution
	int		vertexColorLOD4;			// 1/8 cell resolution
	int		nCompressedBlocks;			// number of ZLib compressed blocks
	int		zlibBlocksTableOffs;		// table of compressed block offsets and sizes
	int		zlibBlocksDataOffs;			// ZLib compressed data
	int		zlibBlkTableOffsLOD3;
	int		zlibBlkTableOffsLOD2;
	int		zlibBlkTableOffsLOD1;
	int		zlibBlkTableOffsLOD0;

	public static class TileData {
		short	x0;
		short	y0;
		// bit 0: LOD0 vertex height and land textures are loaded
		// bit 1: LOD0 ground cover is loaded
		// bit 2: LOD1 vertex height and land textures are loaded
		// bit 4: LOD2 vertex height and land textures are loaded
		// bit 5: LOD2 terrain color is loaded
		// bit 6: LOD3 vertex height and land textures are loaded
		// bit 7: LOD3 terrain color is loaded
		// bit 8: LOD4 vertex height and land textures are loaded
		// bit 9: LOD4 terrain color is loaded
		int		blockMask;
		short[]	hmapData;	// vertex height, 1024 * 1024
		short[]	ltexData;	// land textures, 1024 * 1024
		byte[]	gcvrData;	// ground cover, 1024 * 1024
		short[]	vclrData;	// terrain color, 256 * 256
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
	//0x0004EF60-0x0018A97F: 201 * 2 * 201 * 2 * byte[8], cell quadrant land
	//                       textures (43 - LTEX_table_index, or zero if none)
	//0x0018A980-0x0018A983: (int32) 30, number of ground covers (GCVR records)
	//0x0018A984-0x0018A9FB: 30 * int32, GCVR form IDs
	//0x0018A9FC-0x002C641B: 201 * 2 * 201 * 2 * byte[8], cell quadrant ground
	//                       covers (GCVR_table_index, or 0xFF if none)
	//0x002C641C-0x007B4C9B: 201 * 8 * 201 * 8 * int16, LOD4 height map,
	//                       0 to 65535 maps to minimum height to maximum height
	//0x007B4C9C-0x00CA351B: 201 * 8 * 201 * 8 * int16, LOD4 land textures,
	//                       bits[N * 3 to N * 3 + 2] = texture N alpha (0 to 7),
	//                       texture 6 is the base texture
	//0x00CA351C-0x01191D9B: 201 * 8 * 201 * 8 * int16, LOD4 terrain color
	//                       type in packed 16-bit format, 5 bits per channel
	//0x01191D9C-0x01250643: 97557 * (int32, int32), pairs of zlib compressed data
	//                       offset (relative to 0x01250644) and size
	//0x01250644-EOF:        zlib compressed data blocks
	//zlib compressed blocks     0 to  1351 (26 * 26 * 2): LOD3, 8x8 cell groups
	//                        1352 to  6553 (51 * 51 * 2): LOD2, 4x4 cell groups
	//                        6554 to 16754   (101 * 101): LOD1, 2x2 cell groups
	//                       16755 to 57155   (201 * 201): LOD0 height map, LTEX
	//                       57156 to 97556   (201 * 201): LOD0 ground cover mask
	//LOD3, LOD2: pairs of height map, LTEX and terrain color blocks
	//LOD1: height map, land textures block only
	//height map, LTEX block format:
	//  0x0000-0x5FFF: 64 * (64, 128) int16, height map, only the samples
	//		not present at lower levels of detail
	//  0x6000-0xBFFF: 64 * (64, 128) int16, land texture alphas
	//terrain color block format (LOD3 and LOD2 only):
	//  0x0000-0x5FFF: 64 * (64, 128) int16
	//  0x6000-0x7FFF: unused
	//ground cover block format (LOD0 only):
	//  0x0000-0x3FFF: 128 * 128 bytes, bit N is set if ground cover N is enabled
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
	 Or in human readable format lod=4=72shorts, lod=3=272, lod=2=1056 lod=1=4160 ,lod=0=16512		 
	 * @param buf
	 * @param cellX
	 * @param cellY
	 * @param lod
	 */
	public void getCellHeightMap(short[] buf, int cellX, int cellY, int lod) {
		TileData tileData = loadTile(cellX, cellY, (~0 << (lod + lod)) & 0x0155);
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
	 * return a float[2] of CellHeightMinMax
	 * @param buf
	 * @param cellX
	 * @param cellY
	 */
	public void getCellHeightMinMax(float[] buf, int cellX, int cellY) {
		//cellHeightMinMaxMapOffs
		// skipped like this
		//buf.position(buf.position() + ((nCellsY * nCellsX) << 3)); 4 then double for 2 floats
		//0x000000D8-0x0004EF5F: 201 * 201 * float[2], min, max height for each cell
		int x = cellX - cellMinX;
		int y = cellY - cellMinY;

		buf[0] = readFloat(cellHeightMinMaxMapOffs + (((y * nCellsX) + x) << 3) + 0);
		buf[1] = readFloat(cellHeightMinMaxMapOffs + (((y * nCellsX) + x) << 3) + 4);
	}

	/** land texture opacities in pixelFormatA16 format (see filebuf.hpp)
	lod=4=72shorts, lod=3=272, lod=2=1056 lod=1=4160 ,lod=0=16512
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
	lod=4=72shorts, lod=3=272, lod=2=1056 lod=1=4160 ,lod=0=16512
	//FIXME: a ROTL is missing!
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

	//my version of Integer.rotateLeft(tmp, 4); but for a byte TODO: is this correct?
	//https://labex.io/tutorials/java-how-to-perform-bit-rotation-in-java-414101
	public static byte rotateLeft(byte x, int distance) {
		//return (byte)((x << distance) | (x >>> -distance));
		return (byte)((x << 2) | (x >> (32 - 2)));
	}

	/** vertex colors in pixelFormatRGBA16 format, l >= 2
	lod=4=72shorts, lod=3=272, lod=2=1056 lod=1=4160 ,lod=0=16512
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
	/*
	 void loadBlockLines_8(unsigned char *dst, const unsigned char *src)	{
	  for (size_t i = 0; i < 128; i++, dst++, src++)
	    *dst = *src;
	  dst = dst + (1024 - 128);
	  for (size_t i = 0; i < 128; i++, dst++, src++)
	    *dst = *src;
	}*/

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
	/*
	 void loadBlockLines_16(std::uint16_t *dst, const unsigned char *src, size_t xd, size_t yd)	{
		  for (size_t i = 0; i < 64; i++, dst = dst + xd, src = src + 2)  {
		    dst = dst + xd;
		    *dst = FileBuffer::readUInt16Fast(src);
		  }
		  dst = dst + yd;
		  for (size_t i = 0; i < 128; i++, dst = dst + xd, src = src + 2)
		    *dst = FileBuffer::readUInt16Fast(src);
		}
	
	 */

	private static short readUInt16Fast(byte[] src, int srcOff) {
		// as always little endian
		//bb.order(ByteOrder.LITTLE_ENDIAN); style
		return (short)(((src[srcOff + 1] & 0xff) << 8) + (src[srcOff + 0] & 0xff));
	}

	private void loadBlock(TileData tileData, int dataOffs, int cellId, int lod, boolean otherData, byte[] zlibBuf)
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

		Inflater inflater = new Inflater();
		synchronized (buf) {
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
							loadBlock(tileData, ((yy << 10) + xx) << (lod + 7), cellId, lod, false, zlibBuf);
						if (((blockMask & 0xA0) & (1 << (lod + lod + 1))) != 0)
							loadBlock(tileData, ((yy << 8) + xx) << (lod + 5), cellId, lod, true, zlibBuf);
						if (((blockMask & 0x02) & (1 << (lod + lod + 1))) != 0)
							loadBlock(tileData, ((yy << 10) + xx) << (lod + 7), cellId, lod, true, zlibBuf);
					}
					if (++t >= threadCnt)
						t = 0;
				}
			}
		}
	}

	private void loadBlock_SF(TileData tileData, int dataOffs, int n, int lod, byte[] zlibBuf) throws IOException {
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

		Inflater inflater = new Inflater();
		synchronized (buf) {
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
						loadBlock_SF(tileData, ((yy << 10) + xx) << (l + 7), n, l, zlibBuf);
					}
					if (++t >= threadCnt)
						t = 0;
				}
			}
			break;
		}
	}

	//private static int	cacheHit	= 0;
	//private static int	cacheMiss	= 0;

	private TileData loadTile(int cellX, int cellY, int blockMask) {
		int x0 = (cellX - cellMinX) & 0xFFF8;
		int y0 = (cellY - cellMinY) & 0xFFF8;
		int cacheKey = (y0 << 16) | x0;
		TileData t = tileCacheMap.get(cacheKey);
		if (t == null) {
			//cacheMiss++;
			//if(cacheMiss%100==0)
			//	System.out.println("Btdb cacheMiss " + cacheMiss + " cacheHit " +cacheHit);
			t = new TileData();
			tileCacheMap.put(cacheKey, t);
		} else {
			//cacheHit++;
		}
		TileData tileData = t;
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
		return tileData;
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
