// 
// Copyright 2000 The Regents of the University of California
// All Rights Reserved
// 
// Permission to use, copy, modify and distribute any part of this
// Walrus software package for educational, research and non-profit
// purposes, without fee, and without a written agreement is hereby
// granted, provided that the above copyright notice, this paragraph
// and the following paragraphs appear in all copies.
//   
// Those desiring to incorporate this into commercial products or use
// for commercial purposes should contact the Technology Transfer
// Office, University of California, San Diego, 9500 Gilman Drive, La
// Jolla, CA 92093-0910, Ph: (858) 534-5815, FAX: (858) 534-7345.
// 
// IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY
// PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL
// DAMAGES, INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
//  
// THE SOFTWARE PROVIDED HEREIN IS ON AN "AS IS" BASIS, AND THE
// UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE,
// SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS. THE UNIVERSITY
// OF CALIFORNIA MAKES NO REPRESENTATIONS AND EXTENDS NO WARRANTIES
// OF ANY KIND, EITHER IMPLIED OR EXPRESS, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY OR FITNESS FOR A
// PARTICULAR PURPOSE, OR THAT THE USE OF THE SOFTWARE WILL NOT INFRINGE
// ANY PATENT, TRADEMARK OR OTHER RIGHTS.
//  
// The Walrus software is developed by the Walrus Team at the
// University of California, San Diego under the Cooperative Association
// for Internet Data Analysis (CAIDA) Program.  Support for this effort
// is provided by NSF grant ANI-9814421, DARPA NGI Contract N66001-98-2-8922,
// Sun Microsystems, and CAIDA members.
// 

import javax.media.j3d.*;
import javax.vecmath.*;
import java.util.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import com.sun.image.codec.jpeg.*;

public class H3ScreenCapturer
{
    ////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ////////////////////////////////////////////////////////////////////////

    public H3ScreenCapturer()
    {
	this(RASTER_OFFSET_X, RASTER_OFFSET_Y, RASTER_WIDTH, RASTER_HEIGHT);
    }

    public H3ScreenCapturer(int dx, int dy, int width, int height)
    {
	BufferedImage image =
	    new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

	ImageComponent2D imageComponent =
	    new ImageComponent2D(ImageComponent.FORMAT_RGB, image);

	m_raster = new Raster(new Point3f(0.0f, 0.0f, 0.0f),
			      Raster.RASTER_COLOR,
			      dx, dy, width, height,
			      imageComponent, null);
    }

    ////////////////////////////////////////////////////////////////////////
    // PUBLIC METHODS
    ////////////////////////////////////////////////////////////////////////

    public void capture(GraphicsContext3D gc)
    {
	if (m_captureEnabled)
	{
	    gc.readRaster(m_raster);
            BufferedImage image = m_raster.getImage().getImage();
	    String filename = generateCaptureFileName();

            try
	    {
                FileOutputStream out = new FileOutputStream(filename);

                JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
                JPEGEncodeParam param =
		    encoder.getDefaultJPEGEncodeParam(image);
                param.setQuality(1.0f, false); // (% quality, ?)
                encoder.setJPEGEncodeParam(param);
                encoder.encode(image);

                out.close();
            }
	    catch (IOException e)
	    {
                System.out.println("ERROR: While capturing screen: " + e);
            }
	}
    }

    public void enableCapturing()
    {
	m_captureEnabled = true;
	m_captureSequenceNum = 0;
    }

    public void disableCapturing()
    {
	m_captureEnabled = false;
    }

    public void toggleCapturing()
    {
	if (m_captureEnabled)
	{
	    disableCapturing();
	}
	else
	{
	    enableCapturing();
	}
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE METHODS
    ////////////////////////////////////////////////////////////////////////

    private String generateCaptureFileName()
    {
	++m_captureSequenceNum;
	return m_captureFilePrefix
	    + createPaddedString(m_captureSequenceNum, CAPTURE_COUNT_WIDTH)
	    + ".jpg";
    }

    private String createPaddedString(int n, int width)
    {
	String retval = Integer.toString(n);
	
	if (retval.length() < width)
	{
	    StringBuffer buffer = new StringBuffer();

	    int padding = width - retval.length();
	    while (padding-- > 0)
	    {
		buffer.append('0');
	    }

	    buffer.append(retval);
	    retval = buffer.toString();
	}

	return retval;
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE FIELDS
    ////////////////////////////////////////////////////////////////////////

    private static final int RASTER_OFFSET_X = 0;
    private static final int RASTER_OFFSET_Y = 0;
    private static final int RASTER_WIDTH = 888;
    private static final int RASTER_HEIGHT = 964;
    private static final int CAPTURE_COUNT_WIDTH = 5;

    private boolean m_captureEnabled = false;
    private String m_captureFilePrefix = new String("h3-cap");
    private int m_captureSequenceNum;
    private Raster m_raster;
}
