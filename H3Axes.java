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


import java.awt.*;
import javax.media.j3d.*;
import javax.vecmath.*;
import com.sun.j3d.utils.geometry.Text2D;
import com.sun.j3d.utils.geometry.*;

public class H3Axes
{
    public H3Axes()
    {
	createAxes();
	createAxisLabels();
	createUnitSphere();

	createGeneralAppearances();
    }

    public void draw(GraphicsContext3D gc, Transform3D transform)
    {
	gc.setModelTransform(transform);

	gc.setAppearance(m_axisAppearance);
	gc.draw(m_axes);

	gc.setAppearance(m_circleAppearance);
	gc.draw(m_xyCircle);
	gc.draw(m_yzCircle);
	gc.draw(m_xzCircle);

	drawAxisLabels(gc, transform);
    }

    public void setAxisAppearance(Appearance appearance)
    {
	m_axisAppearance = appearance;
    }

    public void setCircleAppearance(Appearance appearance)
    {
	m_circleAppearance = appearance;
    }

    public void setAxisColor(float r, float g, float b)
    {
	ColoringAttributes attributes =
	    new ColoringAttributes(r, g, b, ColoringAttributes.FASTEST);

	m_axisAppearance.setColoringAttributes(attributes);
    }

    public void setCircleColor(float r, float g, float b)
    {
	ColoringAttributes attributes =
	    new ColoringAttributes(r, g, b, ColoringAttributes.FASTEST);

	m_circleAppearance.setColoringAttributes(attributes);
    }

    // ===================================================================

    private void createAxes()
    {
	double radius = 1.0;

	LineArray axisGeometry = new LineArray(6, LineArray.COORDINATES);
	Point3d[] coordinates = new Point3d[6];
	coordinates[0] = new Point3d(-radius, 0.0, 0.0);
	coordinates[1] = new Point3d(radius, 0.0, 0.0);
	coordinates[2] = new Point3d(0.0, -radius, 0.0);
	coordinates[3] = new Point3d(0.0, radius, 0.0);
	coordinates[4] = new Point3d(0.0, 0.0, -radius);
	coordinates[5] = new Point3d(0.0, 0.0, radius);
	axisGeometry.setCoordinates(0, coordinates);

	m_axes = axisGeometry;
    }

    private void createAxisLabels()
    {
	{
	    m_xT3D = new Transform3D();
	    m_xT3D.setScale(0.01);
	    m_xT3D.setTranslation(new Vector3d(1.0, 0.0, 0.0));

	    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	    Text3D xLabel = createText3D("X");
	    Appearance xAppearance =
		createLabelAppearance(new Color3f(0.5f, 0.1f, 0.1f));
	    m_xLabel = new Shape3D(xLabel, xAppearance);
	}

	{
	    m_yT3D = new Transform3D();
	    m_yT3D.setScale(0.01);
	    m_yT3D.setTranslation(new Vector3d(0.0, 1.0, 0.0));

	    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	    Text3D yLabel = createText3D("Y");
	    Appearance yAppearance =
		createLabelAppearance(new Color3f(0.1f, 0.5f, 0.1f));
	    m_yLabel = new Shape3D(yLabel, yAppearance);
	}

	{
	    Matrix4d rot = new Matrix4d();
	    rot.rotX(-Math.PI / 2.0);
	    rot.rotY(Math.PI / 2.0);

	    Matrix4d m = new Matrix4d();
	    m.set(0.01, new Vector3d(0.0, 0.0, 1.0));  // (scale, translation)
	    m.mul(rot);

	    m_zT3D = new Transform3D(m);

	    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	    Text3D zLabel = createText3D("Z");
	    zLabel.setAlignment(Text3D.ALIGN_LAST);

	    Appearance zAppearance =
		createLabelAppearance(new Color3f(0.1f, 0.1f, 0.5f));
	    m_zLabel = new Shape3D(zLabel, zAppearance);
	}
    }

    private void drawAxisLabels(GraphicsContext3D gc, Transform3D transform)
    {
	{
	    Transform3D xT3D = new Transform3D(transform);
	    xT3D.mul(m_xT3D);
	    gc.setModelTransform(xT3D);
	    gc.draw(m_xLabel);
	}

	{
	    Transform3D yT3D = new Transform3D(transform);
	    yT3D.mul(m_yT3D);
	    gc.setModelTransform(yT3D);
	    gc.draw(m_yLabel);
	}

	{
	    Transform3D zT3D = new Transform3D(transform);
	    zT3D.mul(m_zT3D);
	    gc.setModelTransform(zT3D);
	    gc.draw(m_zLabel);
	}
    }

    private void createUnitSphere()
    {
	int numSegments = 72;
	int[] stripLengths = { numSegments + 1 };

	{
	    Point3f[] xyPoints = createXYCircleCoordinates(1.0f, numSegments);
	    LineStripArray xyLines = new LineStripArray(xyPoints.length,
						GeometryArray.COORDINATES,
						stripLengths);
	    xyLines.setCoordinates(0, xyPoints);
	    m_xyCircle = xyLines;
	}

	{
	    Point3f[] yzPoints = createXYCircleCoordinates(1.0f, numSegments);
	    for (int i = yzPoints.length - 1; i >= 0; i--)
	    {
		Point3f p = yzPoints[i];
		p.z = p.y;
		p.y = p.x;
		p.x = 0.0f;
	    }

	    LineStripArray yzLines = new LineStripArray(yzPoints.length,
						GeometryArray.COORDINATES,
						stripLengths);
	    yzLines.setCoordinates(0, yzPoints);
	    m_yzCircle = yzLines;
	}

	{
	    Point3f[] xzPoints = createXYCircleCoordinates(1.0f, numSegments);
	    for (int i = xzPoints.length - 1; i >= 0; i--)
	    {
		Point3f p = xzPoints[i];
		p.z = p.x;
		p.x = p.y;
		p.y = 0.0f;
	    }

	    LineStripArray xzLines = new LineStripArray(xzPoints.length,
						GeometryArray.COORDINATES,
						stripLengths);
	    xzLines.setCoordinates(0, xzPoints);
	    m_xzCircle = xzLines;
	}
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

    private Text3D createText3D(String text)
    {
	Font font = new Font("SansSerif", Font.PLAIN, 14);
	Font3D font3D = new Font3D(font, new FontExtrusion());
	return new Text3D(font3D, text);
    }

    private Appearance createLabelAppearance(Color3f color)
    {
	Appearance retval = new Appearance();

	ColoringAttributes coloring = new ColoringAttributes(color,
					     ColoringAttributes.FASTEST);
	retval.setColoringAttributes(coloring);
	
	return retval;
    }

    private void createGeneralAppearances()
    {
	{
	    LineAttributes lineAttributes = new LineAttributes();
	    lineAttributes.setLineAntialiasingEnable(ANTIALIASING);

	    ColoringAttributes coloringAttributes =
		new ColoringAttributes(0.3f, 0.6f, 0.3f,
				       ColoringAttributes.FASTEST);
      
	    m_axisAppearance = new Appearance();
	    m_axisAppearance.setLineAttributes(lineAttributes);
	    m_axisAppearance.setColoringAttributes(coloringAttributes);
	}

	{
	    LineAttributes lineAttributes = new LineAttributes();
	    lineAttributes.setLineAntialiasingEnable(ANTIALIASING);

	    ColoringAttributes coloringAttributes =
		new ColoringAttributes(0.3f, 0.6f, 0.3f,
				       ColoringAttributes.FASTEST);

	    m_circleAppearance = new Appearance();
	    m_circleAppearance.setLineAttributes(lineAttributes);
	    m_circleAppearance.setColoringAttributes(coloringAttributes);
	}
    }

    // =====================================================================

    private static final boolean ANTIALIASING = false;

    private Geometry m_axes;
    private Geometry m_xyCircle;
    private Geometry m_yzCircle;
    private Geometry m_xzCircle;
    private Shape3D m_xLabel;
    private Shape3D m_yLabel;
    private Shape3D m_zLabel;
    private Transform3D m_xT3D;
    private Transform3D m_yT3D;
    private Transform3D m_zT3D;
    private Appearance m_axisAppearance;
    private Appearance m_circleAppearance;
}
