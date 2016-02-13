package esmmanager;

public class Point
{
	public int x = 0;
	public int y = 0;

	public Point(int x, int y)
	{
		this.x = x;
		this.y = y;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof Point))
			return false;
		Point p = (Point) obj;
		return x == p.x && y == p.y;
	}

	@Override
	public int hashCode()
	{
		int bits = x;
		bits ^= y * 31;
		return (((int) bits) ^ ((int) (bits >> 32)));

	}
	
	@Override
	public String toString()
	{
		return "Point[" + x + ", " + y + "]";
	}
}
