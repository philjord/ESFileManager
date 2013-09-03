package ca.davidfuchs.jessedit.ess;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

public class ESSUtils {
    public static BufferedImage getScreenShot(ESSFile essFile) {
        StructHeader header = essFile.getHeader();
        StructBasicData basicData = essFile.getBasicData();

        BufferedImage bufferedImage = new BufferedImage((int) header.getShotWidth(), (int) header.getShotHeight(), BufferedImage.TYPE_INT_RGB);
        WritableRaster raster = bufferedImage.getRaster();

        int[] pixelData = new int[basicData.getScreenShotData().length];
        for (int index = 0; index < basicData.getScreenShotData().length; index++) {
            pixelData[index] = basicData.getScreenShotData()[index];
        }

        raster.setPixels(0, 0, (int) header.getShotWidth(), (int) header.getShotHeight(), pixelData);

        return bufferedImage;
    }

    public static String getPrettyHexBytes(byte[] bytes) {
        StringBuilder response = new StringBuilder();

        for (byte aByte : bytes) {
            response.append(String.format("%02X ", aByte));

        }

        return response.toString();
    }
}
