package esfilemanager.btd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

/** For desktop to swap File path strings into ByteBuffer or randomaccessfile or io streams
 Android will have the same for SAF Uri starting points
*/
public class BtdReaderFile {

	private BtdbReader btdbReader;

	public BtdReaderFile(String fileName) throws Exception {

		File testFile = new File(fileName);
		if (!testFile.exists() || !testFile.isFile()) {
			System.out.println("Master file '" + testFile.getAbsolutePath() + "' does not exist");
		} else {
			this.btdbReader = new BtdbReader(new FileInputStream(fileName).getChannel());
			btdbReader.load();
			//FIXME: possibly I do want the cache system, possibly ?? my weak one might keep on dropping the tile?

		}

	}

	public BtdbReader getBtdbReader() {
		return btdbReader;
	}

	public static void main(String[] args) {
		try {
			BtdReaderFile btdReaderFile = new BtdReaderFile("D:\\game_media\\Fallout76\\Data\\Terrain\\Appalachia.btd");
			debugItABit(btdReaderFile);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void debugItABit(BtdReaderFile btdReaderFile) {

		BtdbReader btdbReader = btdReaderFile.getBtdbReader();
		// umm read a bit o data 
	/*	for (int x = btdbReader.getCellMinX(); x <= btdbReader.getCellMaxX(); x++) {
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
		}*/

		// wee debug of cell 0,0
		// note my hieghts are reverse, so my row 127 == row 0  from the btddump.exe
		File f = new File("D:\\temp\\hmap.bin");
		File fout = new File("D:\\temp\\hmap.txt");
		try (FileChannel fc = new FileInputStream(f).getChannel()) {
			ByteBuffer bb = fc.map(MapMode.READ_ONLY, 0, fc.size());
			bb.order(ByteOrder.LITTLE_ENDIAN);

			byte[] strB = new byte[32 + 8];
			bb.get(strB);
			System.out.println("" + new String(strB));

			for (int i = 0; i < 8; i++)
				System.out.println("" + bb.getInt());

			// I seem to have 28 extra shorts
			for (int i = 0; i < 14; i++)
				System.out.println("" + bb.getInt());

			FileWriter fw = new FileWriter(fout);

			for (int x = 0; x < 128; x++) {
				//System.out.print("" + String.format("%03d",x)+" ");
				fw.write("" + String.format("%03d", x) + " ");
				for (int y = 0; y < 128; y++) {
					//	System.out.print(""+ (bb.getShort()&0xFFFF)+", ");

					fw.write("" +  String.format("%05d",(bb.getShort() & 0xFFFF)) + ", ");
				}
				//System.out.println("");
				fw.write("\n");
			}
			//System.out.println("and done");
			fw.flush();
			fw.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			File fout2 = new File("D:\\temp\\hmap2.txt");
			FileWriter fw2 = new FileWriter(fout2);
			short[] cellHeightMap = new short[16512];
			btdbReader.getCellHeightMap(cellHeightMap, 0, 0, 0);
			for (int x = 0; x < 128; x++) {
				fw2.write("" + String.format("%03d", x) + " ");
				for (int y = 0; y < 128; y++) {
					
					int sourceCol = y;
					int sourceRow = (128-1) - x; 
					int sourceIdx = sourceCol + (sourceRow * 128); //row span is the full 128	
					
					fw2.write("" +  String.format("%05d",(cellHeightMap[sourceIdx] & 0xFFFF)) + ", ");
				}
				//System.out.println("");
				fw2.write("\n");
			}
			//System.out.println("and done");
			fw2.flush();
			fw2.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
