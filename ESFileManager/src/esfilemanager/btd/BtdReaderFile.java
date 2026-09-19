package esfilemanager.btd;

import java.io.File;
import java.io.FileInputStream;

/** For desktop to swap File path strings into ByteBuffer or randomaccessfile or io streams
 Android will have the same for SAF Uri starting points
*/
public class BtdReaderFile {

	public static void main(String[] args) {
		try {
			new BtdReaderFile("D:\\game_media\\Fallout76\\Data\\Terrain\\Appalachia.btd");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private BtdbReader btdbReader;

	public BtdReaderFile(String fileName) throws Exception {

		File testFile = new File(fileName);
		if (!testFile.exists() || !testFile.isFile()) {
			System.out.println("Master file '" + testFile.getAbsolutePath() + "' does not exist");
		} else {
			this.btdbReader = new BtdbReader(new FileInputStream(fileName).getChannel());
			btdbReader.load();
			//FIXME: possibly I do want the cache system, possibly ?? my weak one might keep on dropping the tile?

			// umm read a bit o data 
			for (int x = btdbReader.getCellMinX(); x <= btdbReader.getCellMaxX(); x++) {
				for (int y = btdbReader.getCellMinY(); y <= btdbReader.getCellMaxY(); y++) {

					//for (int x = -10; x <= 10; x++) {
					//	for (int y = -10; y <= 10; y++) {
					System.out.println("x = " + x + " y = " + y);
					short[] bufHM = new short[16512];
					btdbReader.getCellHeightMap(bufHM, x, y, 0);
					short[] bufLT = new short[16512];
					btdbReader.getCellLandTexture(bufLT, x, y, 0);
					short[] bufTC = new short[16512];
					btdbReader.getCellTerrainColor(bufTC, x, y, 0);
					byte[] bufGC = new byte[16512];
					btdbReader.getCellGroundCover(bufGC, x, y, 0);
					byte[] bufTS = new byte[64];
					btdbReader.getCellTextureSet(bufTS, x, y);
				}
			}
		}
	}

	public BtdbReader getBtdbReader() {
		return btdbReader;
	}

}
