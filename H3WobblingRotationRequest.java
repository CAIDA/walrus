// 
// Copyright (C) 2000,2001,2002 The Regents of the University of California.
// All Rights Reserved.
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
// GOVERNMENT PURPOSE RIGHTS
// Contract No.NGI N66001-98-2-8922
// Contractor Name: SPAWAR
// Expiration Date: 3/1/2008
// The Government's rights to use, modify, reproduce, release, perform, 
// display, or disclose these technical data are restricted by paragraph 
// (b)(2) of the Rights in Technical Data - Noncommercial Items clause 
// contained in the above identified contract.  No restrictions apply after 
// the expiration date shown above.  Any reproduction of technical data or 
// portions thereof marked with this legend must also reproduce the markings.
//
//######END_HEADER######

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
