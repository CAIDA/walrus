import java.awt.image.*;
import java.io.*;

public class PPMWriter
{
    public PPMWriter(BufferedImage image)
    {
	m_raster = image.getRaster();
    }

    // Assumes BufferedImage is TYPE_INT_ARGB.

    // 8-bit PPM format (see ppm(5)):
    //
    //    P6
    //    width height
    //    255
    //    <pixels:binary R,G,B bytes; left to right, top to bottom>
    //
    // For example:
    //
    //    P6
    //    2 2
    //    255
    //    0 0 0 255 255 255    \ Each number actually represents the binary
    //    0 0 0 255 255 255    / byte for each R, G, and B component.

    public void write(OutputStream out)
	throws IOException
    {
	writeHeader(out);
	writeImage(out);
    }

    private void writeHeader(OutputStream out)
	throws IOException
    {
	String header = "P6\n" + m_raster.getWidth() + " "
	    + m_raster.getHeight() + "\n255\n";

	out.write(header.getBytes());
    }

    private void writeImage(OutputStream out)
	throws IOException
    {
	int minX = m_raster.getMinX();
	int minY = m_raster.getMinY();
	int width = m_raster.getWidth();
	int right = minX + width;
	int bottom = minY + m_raster.getHeight();

	System.out.println("minX=" + minX + "; minY=" + minY);
	System.out.println("right=" + right + "; bottom=" + bottom);

	m_pixels4 = new int[4 * m_raster.getWidth()];
	m_pixels3 = new byte[3 * m_raster.getWidth()];

	for (int y = minY; y < bottom; y++)
	{
	    m_raster.getPixels(minX, y, width, 1, m_pixels4);
	    for (int i = 0; i < width; i++)
	    {
		m_pixels3[3 * i] = (byte)m_pixels4[4 * i];
		m_pixels3[3 * i + 1] = (byte)m_pixels4[4 * i + 1];
		m_pixels3[3 * i + 2] = (byte)m_pixels4[4 * i + 2];
	    }

	    out.write(m_pixels3);
	}
    }

    private Raster m_raster;
    private int[] m_pixels4;
    private byte[] m_pixels3;
}
