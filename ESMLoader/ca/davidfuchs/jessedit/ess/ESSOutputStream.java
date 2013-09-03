package ca.davidfuchs.jessedit.ess;

import com.google.common.io.LittleEndianDataOutputStream;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;

class ESSOutputStream {
    private LittleEndianDataOutputStream outputStream;

    public ESSOutputStream(OutputStream outputStream) {
        this.outputStream = new LittleEndianDataOutputStream(outputStream);
    }

    public void writeUTF8Array(String value) throws IOException {
        writeUTF8Array(value.toCharArray());
    }

    public void writeUTF8Array(char[] value) throws IOException {
        for (char c : value) {
            outputStream.write((byte) c);
        }
    }

    public void writeUnsignedByte(short unsignedByte) throws IOException {
        outputStream.writeByte(unsignedByte);
    }

    public void writeUnsignedByteArray(short[] unsignedBytes) throws IOException {
        for (short unsignedByte : unsignedBytes) {
            writeUnsignedByte(unsignedByte);
        }
    }

    public void writeUnsignedShort(int unsignedShort) throws IOException {
        outputStream.writeShort(unsignedShort);
    }

    public void writeUnsignedShortArray(int[] unsignedShorts) throws IOException {
        for (int unsignedShort : unsignedShorts) {
            writeUnsignedShort(unsignedShort);
        }
    }

    public void writeUnsignedInt(long unsignedInt) throws IOException {
        outputStream.writeInt((int) unsignedInt);
    }

    public void writeUnsignedIntArray(long[] unsignedInts) throws IOException {
        for (long unsignedInt : unsignedInts) {
            writeUnsignedInt(unsignedInt);
        }
    }

    public void writeUnsignedLong(BigInteger unsignedLong) throws IOException {
        outputStream.writeLong(unsignedLong.longValue());
    }

    public void writeUnsignedLongArray(BigInteger[] unsignedLongs) throws IOException {
        for (BigInteger unsignedLong : unsignedLongs) {
            writeUnsignedLong(unsignedLong);
        }
    }

    public void writeFloat(float value) throws IOException {
        outputStream.writeFloat(value);
    }

    public void writeDouble(double value) throws IOException {
        outputStream.writeDouble(value);
    }

    public void writeWideString(String value) throws IOException {
        char[] characters = value.toCharArray();
        writeUnsignedShort(characters.length);
        writeUTF8Array(characters);
    }

    public void writeBytes(byte[] bytes) throws IOException {
        outputStream.write(bytes);
    }

    /**
     * Reads a Win32 FILETIME structure and returns it as a DateTime.
     * <p/>
     * A Win32 FILETIME struct is two DWORDS (4-byte unsigned ints), but they're
     * actually flipped around so that the second 4 bytes come before the first
     * four bytes. So, we read them into a byte buffer by specifying offsets so
     * that the values go into the buffer in the right order.
     *
     * @return A DateTime class representing the FILETIME struct that was read.
     */
    public void writeFileTime(DateTime fileTime) throws IOException {
        DateTime baseFileTime = new DateTime(1601, 1, 1, 0, 0, DateTimeZone.UTC);

        ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.putLong((fileTime.getMillis() - baseFileTime.getMillis()) * 10000).rewind();

        int firstDWord = byteBuffer.getInt();
        int secondDWord = byteBuffer.getInt();

        writeUnsignedInt(secondDWord);
        writeUnsignedInt(firstDWord);
    }
}
