// 
// The Walrus Graph Visualization Tool.
// Copyright (C) 2000,2001,2002 The Regents of the University of California.
// 
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
// 
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
// 
// ######END_HEADER######
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
