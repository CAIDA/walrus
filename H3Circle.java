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

public class H3Circle
{
    public H3Circle()
    {
	createCircle(1.0, NUM_SEGMENTS);
    }

    public void draw(GraphicsContext3D gc, double radius, double x, double y)
    {
	establishTransform(gc, radius, x, y);
	gc.draw(m_circle);
    }

    public void drawGeometry(GraphicsContext3D gc, double radius,
			     double x, double y)
    {
	establishTransform(gc, radius, x, y);
	gc.draw(m_lines);
    }

    public void setImageToVworldTransform(Transform3D transform)
    {
	m_imageToVworld.set(transform);
    }

    // ===================================================================

    private void establishTransform(GraphicsContext3D gc, double radius,
				    double x, double y)
    {
	Vector3d t = new Vector3d(x, y, 0.0);
	Transform3D translation = new Transform3D();
	translation.set(radius, t);  // (scale, translation)

	Transform3D transform = new Transform3D(m_imageToVworld);
	transform.mul(translation);

	gc.setModelTransform(transform);
    }

    private void createCircle(double radius, int numSegments)
    {
	LineAttributes lineAttributes = new LineAttributes();
	lineAttributes.setLineWidth(1.0f);
	lineAttributes.setLineAntialiasingEnable(ANTIALIASING);

	ColoringAttributes coloringAttributes =
	    new ColoringAttributes(0.9f, 0.5f, 0.0f,
				   ColoringAttributes.FASTEST);

	Appearance appearance = new Appearance();
	appearance.setLineAttributes(lineAttributes);
	appearance.setColoringAttributes(coloringAttributes);

	int[] stripLengths = { numSegments + 1 };

	Point3f[] xyPoints = 
	    createXYCircleCoordinates((float)radius, numSegments);
	LineStripArray xyLines = new LineStripArray(xyPoints.length,
						    GeometryArray.COORDINATES,
						    stripLengths);
	xyLines.setCoordinates(0, xyPoints);
	m_lines = xyLines;
	m_circle = new Shape3D(xyLines, appearance);
    }

    private Point3f[] createXYCircleCoordinates(float radius, int numSegments)
    {
	Point3f[] retval = new Point3f[numSegments + 1];

	Matrix4d rot = new Matrix4d();
	for (int i = 0; i <= numSegments; i++)
	{
	    retval[i] = new Point3f(radius, 0.0f, 0.0f);

	    double angle = 2.0 * Math.PI * i / numSegments;
	    rot.rotZ(angle);
	    rot.transform(retval[i]);
	}

	return retval;
    }

    // =====================================================================

    private static final int NUM_SEGMENTS = 12; // 3, 6, 12, 18, 36, 72
    private static final boolean ANTIALIASING = false;

    private Transform3D m_objectTransform = new Transform3D();
    private Transform3D m_imageToVworld = new Transform3D();

    private Geometry m_lines;
    private Shape3D m_circle;
}
