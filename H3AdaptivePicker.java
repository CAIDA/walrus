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

public class H3AdaptivePicker
    extends H3PickerCommon
{
    ////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ////////////////////////////////////////////////////////////////////////
    
    public H3AdaptivePicker(H3Graph graph, H3Canvas3D canvas,
			    H3ViewParameters parameters,
			    H3RenderQueue queue)
    {
	super(graph, canvas, parameters);

	m_renderQueue = queue;

	int numNodes = graph.getNumNodes();
	m_nodesInEye = new int[numNodes];
    }

    ////////////////////////////////////////////////////////////////////////
    // INTERFACE METHODS (H3Picker)
    ////////////////////////////////////////////////////////////////////////

    public void reset()
    {
	m_numExaminedElements = 0;
	m_numComputedPointsInEye = 0;
    }

    ////////////////////////////////////////////////////////////////////////
    // PROTECTED METHODS (abstract in H3PickerCommon)
    ////////////////////////////////////////////////////////////////////////

    protected void computePointsInEye()
    {
	int currentNumElements = m_renderQueue.getCurrentNumElements();
	if (m_numExaminedElements < currentNumElements)
	{
	    if (DEBUG_PRINT)
	    {
		System.out.println("computing points in eye ...");
		System.out.println("numExaminedElements = "
				   + m_numExaminedElements);
		System.out.println("currentNumElements = "
				   + currentNumElements);
		System.out.println("numComputedPointsInEye = "
				   + m_numComputedPointsInEye);
	    }		

	    Transform3D transform = m_parameters.getObjectToEyeTransform();

	    Point3d p = new Point3d();
	    H3RenderQueue.Element element = new H3RenderQueue.Element();
	    for (int i = m_numExaminedElements; i < currentNumElements; i++)
	    {
		m_renderQueue.get(i, element);
		if (element.type == H3RenderQueue.Element.TYPE_NODE)
		{
		    m_nodesInEye[m_numComputedPointsInEye] = element.data;

		    m_graph.getNodeCoordinates(element.data, p);
		    transform.transform(p);

		    m_pointsInEyeX[m_numComputedPointsInEye] = p.x;
		    m_pointsInEyeY[m_numComputedPointsInEye] = p.y;
		    m_pointsInEyeZ[m_numComputedPointsInEye] = p.z;

		    ++m_numComputedPointsInEye;
		}
	    }

	    m_numExaminedElements = currentNumElements;
	}
    }

    protected int getNumComputedPointsInEye()
    {
	return m_numComputedPointsInEye;
    }

    protected int getNodeInEye(int index)
    {
	return m_nodesInEye[index];
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE FIELDS
    ////////////////////////////////////////////////////////////////////////

    private H3RenderQueue m_renderQueue;

    private int m_numExaminedElements = 0;
    private int m_numComputedPointsInEye = 0;

    private int[] m_nodesInEye;
}
