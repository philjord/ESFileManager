package esfilemanager.loader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.zip.DataFormatException;

import esfilemanager.common.PluginException;
import esfilemanager.common.data.plugin.Master;
import esfilemanager.common.data.plugin.MasterFile;
import esfilemanager.tes3.ESMManagerTes3File;

/** For desktop to swap File path strings into ByteBuffer or randomaccessfile or io streams
 Android will have the same for SAF Uri starting points
*/
public class ESMManagerFile extends ESMManager {
	public ESMManagerFile(String fileName) {
		File m = new File(fileName);

		try {
			Master master = new MasterFile(m);
			master.load();
			addMaster(master);
		} catch (PluginException e1) {
			e1.printStackTrace();
		} catch (DataFormatException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	@Override
	public synchronized void addMaster(String fileNameToAdd) {
		try {
			File m = new File(fileNameToAdd);
			Master master = new MasterFile(m);
			if(master.load()) 
				addMaster(master);
		} catch (PluginException e1) {
			e1.printStackTrace();
		} catch (DataFormatException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public static IESMManager getESMManager(String esmFile) {
		File testFile = new File(esmFile);
		if (!testFile.exists() || !testFile.isFile()) {
			System.out.println("Master file '" + testFile.getAbsolutePath() + "' does not exist");

		} else {
			try {
				RandomAccessFile in = new RandomAccessFile(testFile, "r");
				// Notice as I'm only pulling 16 bytes the mapped byte buffer is a bad idea
				//RandomAccessFile in = new MappedByteBufferRAF(testFile, "r");
				try {
					byte[] prefix = new byte[16];
					int count = in.read(prefix);
					if (count == 16) {
						String recordType = new String(prefix, 0, 4);
						if (recordType.equals("TES3")) {
							in.close();
							return new ESMManagerTes3File(esmFile);
						}
					}
					in.close();
				} catch (IOException e) {
					//fall through, try tes4
				}

				//assume tes4
				return new ESMManagerFile(esmFile);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

}
