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

public class H3RepeatingRotationRequest
    implements H3RotationRequest
{
    ////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ////////////////////////////////////////////////////////////////////////

    public H3RepeatingRotationRequest()
    {
	clear();
    }

    ////////////////////////////////////////////////////////////////////////
    // PUBLIC METHODS
    ////////////////////////////////////////////////////////////////////////

    public synchronized void start()
    {
	clear();
    }

    public synchronized void rotate(double h, double v)
    {
	m_horizontal = h;
	m_vertical = v;
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
	    m_rot.rotX(m_vertical);
	    rot.rotY(m_horizontal);
	    rot.mul(m_rot);
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

    private void clear()
    {
	m_isRotating = true;
	m_horizontal = 0.0;
	m_vertical = 0.0;
    }

    private synchronized void waitIgnore()
    {
	try { wait(); } catch (InterruptedException e) { }
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE FIELDS
    ////////////////////////////////////////////////////////////////////////

    private boolean m_isRotating;
    private double m_horizontal;
    private double m_vertical;

    private Matrix4d m_rot = new Matrix4d();
}
