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

public class H3DisplayPosition
{
    ////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ////////////////////////////////////////////////////////////////////////
    
    public H3DisplayPosition
	(int centerNode, Transform3D rotation, Matrix4d translation)
    {
	m_centerNode = centerNode;
	m_rotation = new Transform3D(rotation);
	m_translation = new Matrix4d(translation);
    }

    ///////////////////////////////////////////////////////////////////////
    // PUBLIC METHODS
    ///////////////////////////////////////////////////////////////////////

    public int getCenterNode()
    {
	return m_centerNode;
    }

    public Transform3D getRotation()
    {
	return m_rotation;
    }

    public Matrix4d getTranslation()
    {
	return m_translation;
    }

    ///////////////////////////////////////////////////////////////////////
    // PRIVATE FIELDS
    ///////////////////////////////////////////////////////////////////////

    private int m_centerNode;
    private Transform3D m_rotation;
    private Matrix4d m_translation;
}
