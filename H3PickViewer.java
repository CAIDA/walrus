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

public class H3PickViewer
{
    public H3PickViewer(double radius)
    {
	createAxes(radius);
	createCircle(radius);
    }

    // (x, y) in image-plate coordinates
    public void draw(GraphicsContext3D gc, double x, double y)
    {
	Vector3d t = new Vector3d(x, y, 0.0);
	Transform3D translation = new Transform3D();
	translation.set(t);

	Transform3D transform = new Transform3D(m_imageToVworld);
	transform.mul(translation);

	gc.setModelTransform(transform);
	gc.draw(m_axes);
	gc.draw(m_circle);
    }

    public void setImageToVworldTransform(Transform3D transform)
    {
	m_imageToVworld.set(transform);
    }

    // ===================================================================

    private void createAxes(double radius)
    {
	LineAttributes lineAttributes = new LineAttributes();
	lineAttributes.setLineWidth(1.0f);
	lineAttributes.setLineAntialiasingEnable(ANTIALIASING);

	ColoringAttributes coloringAttributes =
	    new ColoringAttributes(0.5f, 0.5f, 0.5f,
				   ColoringAttributes.FASTEST);
      
	Appearance appearance = new Appearance();
	appearance.setLineAttributes(lineAttributes);
	appearance.setColoringAttributes(coloringAttributes);

	LineArray lines = new LineArray(4, LineArray.COORDINATES);
	lines.setCoordinate(0, new Point3d(-radius, 0.0, 0.0));
	lines.setCoordinate(1, new Point3d(radius, 0.0, 0.0));
	lines.setCoordinate(2, new Point3d(0.0, -radius, 0.0));
	lines.setCoordinate(3, new Point3d(0.0, radius, 0.0));

	m_axes = new Shape3D(lines, appearance);
    }

    private void createCircle(double radius)
    {
	LineAttributes lineAttributes = new LineAttributes();
	lineAttributes.setLineWidth(1.0f);
	lineAttributes.setLineAntialiasingEnable(ANTIALIASING);

	ColoringAttributes coloringAttributes =
	    new ColoringAttributes(0.5f, 0.5f, 0.5f,
				   ColoringAttributes.FASTEST);

	Appearance appearance = new Appearance();
	appearance.setLineAttributes(lineAttributes);
	appearance.setColoringAttributes(coloringAttributes);

	int numSegments = 72;
	int[] stripLengths = { numSegments + 1 };

	Point3f[] xyPoints = 
	    createXYCircleCoordinates((float)radius, numSegments);
	LineStripArray xyLines = new LineStripArray(xyPoints.length,
						    GeometryArray.COORDINATES,
						    stripLengths);
	xyLines.setCoordinates(0, xyPoints);
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

    private static final boolean ANTIALIASING = false;

    private Transform3D m_imageToVworld = new Transform3D();

    private Shape3D m_axes;
    private Shape3D m_circle;
}
