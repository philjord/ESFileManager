package TES4Gecko;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

public class PluginSpill
{
	private File spillFile;

	private long cacheSize;

	private Map<Long, byte[]> cacheMap;

	private RandomAccessFile spill;

	private long nextWrite = 0L;

	private long currentPosition = 0L;

	public PluginSpill(File spillFile, long cacheSize) throws IOException
	{
		this.spillFile = spillFile;
		this.cacheSize = cacheSize;

		if (spillFile.exists())
		{
			spillFile.delete();
		}

		this.spill = new RandomAccessFile(spillFile, "rw");

		this.cacheMap = new HashMap<Long, byte[]>(1000);

		if (Main.debugMode)
			System.out.println("Spill cache size is " + cacheSize / 1048576L + "MB");
	}

	public synchronized void close() throws IOException
	{
		if (this.spill != null)
		{
			this.spill.close();
			this.spill = null;
			this.cacheMap = null;
		}

		if (this.spillFile.exists())
			this.spillFile.delete();
	}

	public synchronized long write(byte[] data) throws IOException
	{
		long position = this.nextWrite;
		if (data.length > 0)
		{
			this.cacheMap.put(new Long(this.nextWrite), data);
			this.nextWrite += data.length;
		}

		if (this.nextWrite - this.currentPosition >= this.cacheSize)
		{
			if (Main.debugMode)
			{
				System.out.println("Writing cached data to spill file");
			}
			this.spill.seek(this.currentPosition);
			while (this.currentPosition < this.nextWrite)
			{
				Long cachePosition = new Long(this.currentPosition);
				byte[] buffer = this.cacheMap.get(cachePosition);
				this.cacheMap.remove(cachePosition);
				this.spill.write(buffer);
				this.currentPosition += buffer.length;
			}
		}

		return position;
	}

	public synchronized byte[] read(long position, int length) throws IOException
	{
		if ((position < 0L) || (length <= 0))
		{
			return new byte[0];
		}

		byte[] data = new byte[length];
		Long cachePosition = new Long(position);
		byte[] buffer = this.cacheMap.get(cachePosition);
		if (buffer != null)
		{
			if (buffer.length != length)
			{
				throw new IOException("Cached data length " + buffer.length + " is incorrect");
			}
			System.arraycopy(buffer, 0, data, 0, length);
		}
		else
		{
			this.spill.seek(position);
			int count = this.spill.read(data);
			if (count != length)
			{
				throw new IOException("Premature end-of-data on spill file");
			}
		}
		return data;
	}

	public synchronized void reset()
	{
		this.nextWrite = 0L;
		this.currentPosition = 0L;
		this.cacheMap.clear();
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.PluginSpill
 * JD-Core Version:    0.6.0
 */