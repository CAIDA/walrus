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


import javax.vecmath.*;

public class H3WobblingRotationRequest
    implements H3RotationRequest
{
    ////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ////////////////////////////////////////////////////////////////////////

    public H3WobblingRotationRequest()
    {
	Point4d[] coordinates =
	    createXYCircleCoordinates(RADIUS, NUM_SEGMENTS);
	m_rotations = createRotations(coordinates);
    }

    ////////////////////////////////////////////////////////////////////////
    // PUBLIC METHODS
    ////////////////////////////////////////////////////////////////////////

    public synchronized void start()
    {
	m_isRotating = true;
	m_position = 0;
    }

    public synchronized void end()
    {
	m_isRotating = false;
	waitIgnore();
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    public synchronized boolean getRotation(Matrix4d rot)
    {
	boolean retval = false;

	if (m_isRotating)
	{
	    rot.set(m_rotations[m_position]);
	    if (++m_position == NUM_SEGMENTS)
	    {
		m_position = 0;
	    }

	    retval = true;
	}
	else
	{
	    notifyAll(); // Wake up any thread waiting in end().
	}

	return retval;
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE METHODS
    ////////////////////////////////////////////////////////////////////////

    private Matrix4d[] createRotations(Point4d[] coordinates)
    {
	Matrix4d[] retval = new Matrix4d[coordinates.length];

	Matrix4d m = new Matrix4d();
	for (int i = 0; i < coordinates.length; i++)
	{
	    double startX = coordinates[i].x;
	    double startY = coordinates[i].y;

	    int next = i + 1;
	    if (next == coordinates.length)
	    {
		next = 0;
	    }

	    double endX = coordinates[next].x;
	    double endY = coordinates[next].y;

	    double dx = Math.toRadians(endX - startX);
	    double dy = Math.toRadians(endY - startY);

	    Matrix4d rot = new Matrix4d();
	    m.rotX(dy);
	    rot.rotY(dx);
	    rot.mul(m);

	    retval[i] = rot;
	}

	return retval;
    }

    private Point4d[] createXYCircleCoordinates(double radius, int numSegments)
    {
	Point4d[] retval = new Point4d[numSegments];

	Matrix4d rot = new Matrix4d();
	for (int i = 0; i < numSegments; i++)
	{
	    Point4d p = new Point4d(radius, 0.0, 0.0, 1.0);
	    retval[i] = p;

	    double angle = 2.0 * Math.PI * i / numSegments;
	    rot.rotZ(angle);
	    rot.transform(p);
	    p.project(p);
	}

	return retval;
    }

    private synchronized void waitIgnore()
    {
	try { wait(); } catch (InterruptedException e) { }
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE FIELDS
    ////////////////////////////////////////////////////////////////////////

    private static final double RADIUS = 2.5;
    private static final int NUM_SEGMENTS = 18;

    private boolean m_isRotating;

    private int m_position = 0;
    private Matrix4d[] m_rotations;
}
