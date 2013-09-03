package ca.davidfuchs.jessedit.ess;

import com.google.common.io.LittleEndianDataInputStream;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;

class ESSInputStream {
    private LittleEndianDataInputStream inputStream;

    public ESSInputStream(InputStream inputStream) {
        this.inputStream = new LittleEndianDataInputStream(inputStream);

    }

    public char[] readUTF8Array(int length) throws IOException {
        char[] result = new char[length];

        if (length > 0) {
            byte[] bytes = new byte[length];
            inputStream.read(bytes, 0, length);

            int index = 0;

            for (byte aByte : bytes) {
                result[index++] = (char) aByte;
            }
        }

        return result;
    }

    public short readUnsignedByte() throws IOException {
        return (short) inputStream.readUnsignedByte();
    }

    public short[] readUnsignedByteArray(int length) throws IOException {
        short[] result = new short[length];

        for (int index = 0; index < length; index++) {
            result[index] = readUnsignedByte();
        }

        return result;
    }

    public int readUnsignedShort() throws IOException {
        return inputStream.readUnsignedShort();
    }

    public int[] readUnsignedShortArray(int length) throws IOException {
        int[] result = new int[length];

        for (int index = 0; index < length; index++) {
            result[index] = readUnsignedShort();
        }

        return result;
    }

    public long readUnsignedInt() throws IOException {
        return (long) inputStream.readInt();
    }

    public long[] readUnsignedIntArray(int length) throws IOException {
        long[] result = new long[length];

        for (int index = 0; index < length; index++) {
            result[index] = readUnsignedInt();
        }

        return result;
    }

    public BigInteger readUnsignedLong() throws IOException {
        return BigInteger.valueOf(inputStream.readLong());
    }

    public BigInteger[] readUnsignedLongArray(int length) throws IOException {
        BigInteger[] result = new BigInteger[length];

        for (int index = 0; index < length; index++) {
            result[index] = readUnsignedLong();
        }

        return result;
    }

    public float readFloat() throws IOException {
        return inputStream.readFloat();
    }

    public double readDouble() throws IOException {
        return inputStream.readDouble();
    }

    public String readWideString() throws IOException {
        int length = readUnsignedShort();

        if (length == 0) {
            return null;
        }

        return String.valueOf(readUTF8Array(length));
    }

    public byte[] readBytes(int length) throws IOException {
        byte[] result = new byte[length];

        inputStream.read(result, 0, length);

        return result;
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
    public DateTime readFileTime() throws IOException {
        ByteBuffer fileTimeBytes = ByteBuffer.allocate(8);
        fileTimeBytes.putInt(4, (int) readUnsignedInt());
        fileTimeBytes.putInt(0, (int) readUnsignedInt());

        // Why divide by 10000? The FILETIME represents the number of 100-nanosecond intervals since January 1st, 1601.
        // The DateTime plus() method wants millseconds.  There are 1000000 nanoseconds in a millsecond, and since
        // FILETIME is in 100-nanosecond intervals, we divide the 1000000 by 100 and get 10000.
        DateTime fileTime = new DateTime(1601, 1, 1, 0, 0, DateTimeZone.UTC);
        fileTime = fileTime.plus(fileTimeBytes.getLong() / 10000);

        return fileTime.toDateTime(DateTimeZone.getDefault());
    }
}
