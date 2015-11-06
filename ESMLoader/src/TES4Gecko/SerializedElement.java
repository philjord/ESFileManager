package TES4Gecko;

public class SerializedElement
{
	public static int compareArrays(byte[] arrayA, int positionA, byte[] arrayB, int positionB, int count)
	{
		int diff = 0;
		int indexA = positionA;
		int indexB = positionB;
		for (int i = 0; i < count; i++)
		{
			if (arrayA[indexA] != arrayB[indexB])
			{
				if (arrayA[indexA] > arrayB[indexB])
				{
					diff = 1;
					break;
				}
				diff = -1;

				break;
			}

			indexA++;
			indexB++;
		}

		return diff;
	}

	public static int getShort(byte[] buffer, int offset)
	{
		return buffer[(offset + 0)] & 0xFF | (buffer[(offset + 1)] & 0xFF) << 8;
	}

	public static void setShort(int number, byte[] buffer, int offset)
	{
		buffer[offset] = (byte) number;
		buffer[(offset + 1)] = (byte) (number >>> 8);
	}

	public static int getInteger(byte[] buffer, int offset)
	{
		return buffer[(offset + 0)] & 0xFF | (buffer[(offset + 1)] & 0xFF) << 8 | (buffer[(offset + 2)] & 0xFF) << 16
				| (buffer[(offset + 3)] & 0xFF) << 24;
	}

	public static void setInteger(int number, byte[] buffer, int offset)
	{
		buffer[offset] = (byte) number;
		buffer[(offset + 1)] = (byte) (number >>> 8);
		buffer[(offset + 2)] = (byte) (number >>> 16);
		buffer[(offset + 3)] = (byte) (number >>> 24);
	}

	public static int[] getIntegerArray(byte[] buffer, int offset)
	{
		int bufLen = buffer.length - offset;
		int[] retArray = new int[bufLen / 4];
		int i = offset;
		for (int j = 0; i < bufLen; j++)
		{
			retArray[j] = (buffer[(offset + j * 4)] & 0xFF | (buffer[(offset + j * 4 + 1)] & 0xFF) << 8
					| (buffer[(offset + j * 4 + 2)] & 0xFF) << 16 | (buffer[(offset + j * 4 + 3)] & 0xFF) << 24);

			i += 4;
		}

		return retArray;
	}

	public static void setIntegerArray(int[] numArray, byte[] buffer, int offset)
	{
		for (int j = 0; j < numArray.length; j++)
		{
			buffer[(offset + 4 * j)] = (byte) numArray[j];
			buffer[(offset + 4 * j + 1)] = (byte) (numArray[j] >>> 8);
			buffer[(offset + 4 * j + 2)] = (byte) (numArray[j] >>> 16);
			buffer[(offset + 4 * j + 3)] = (byte) (numArray[j] >>> 24);
		}
	}

	public static long getLong(byte[] buffer, int offset)
	{
		return buffer[(offset + 0)] & 0xFF | (buffer[(offset + 1)] & 0xFF) << 8 | (buffer[(offset + 2)] & 0xFF) << 16
				| (buffer[(offset + 3)] & 0xFF) << 24 | (buffer[(offset + 4)] & 0xFF) << 32 | (buffer[(offset + 5)] & 0xFF) << 40
				| (buffer[(offset + 6)] & 0xFF) << 48 | (buffer[(offset + 7)] & 0xFF) << 56;
	}

	public static void setLong(long number, byte[] buffer, int offset)
	{
		buffer[offset] = (byte) (int) number;
		buffer[(offset + 1)] = (byte) (int) (number >>> 8);
		buffer[(offset + 2)] = (byte) (int) (number >>> 16);
		buffer[(offset + 3)] = (byte) (int) (number >>> 24);
		buffer[(offset + 4)] = (byte) (int) (number >>> 32);
		buffer[(offset + 5)] = (byte) (int) (number >>> 40);
		buffer[(offset + 6)] = (byte) (int) (number >>> 48);
		buffer[(offset + 7)] = (byte) (int) (number >>> 56);
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.SerializedElement
 * JD-Core Version:    0.6.0
 */