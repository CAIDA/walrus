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

public class H3NonadaptivePicker
    extends H3PickerCommon
{
    ////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ////////////////////////////////////////////////////////////////////////
    
    public H3NonadaptivePicker(H3Graph graph, H3Canvas3D canvas,
			       H3ViewParameters parameters)
    {
	super(graph, canvas, parameters);

	m_numNodes = graph.getNumNodes();
    }

    ////////////////////////////////////////////////////////////////////////
    // INTERFACE METHODS (H3Picker)
    ////////////////////////////////////////////////////////////////////////

    public void reset()
    {
	m_computedPointsInEye = false;
    }

    ////////////////////////////////////////////////////////////////////////
    // PROTECTED METHODS (abstract in H3PickerCommon)
    ////////////////////////////////////////////////////////////////////////

    protected void computePointsInEye()
    {
	if (!m_computedPointsInEye)
	{
	    m_computedPointsInEye = true;

	    Transform3D transform = m_parameters.getObjectToEyeTransform();

	    Point3d p = new Point3d();
	    for (int i = 0; i < m_numNodes; i++)
	    {
		m_graph.getNodeCoordinates(i, p);
		transform.transform(p);

		m_pointsInEyeX[i] = p.x;
		m_pointsInEyeY[i] = p.y;
		m_pointsInEyeZ[i] = p.z;
	    }
	}
    }

    protected int getNumComputedPointsInEye()
    {
	return m_numNodes;
    }

    protected int getNodeInEye(int index)
    {
	return index;
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE FIELDS
    ////////////////////////////////////////////////////////////////////////

    private int m_numNodes;
    private boolean m_computedPointsInEye = false;
}
