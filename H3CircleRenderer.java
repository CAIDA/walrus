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

import java.util.*;
import javax.media.j3d.*;
import javax.vecmath.*;

public class H3CircleRenderer
    implements H3AdaptiveRenderer
{
    ////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ////////////////////////////////////////////////////////////////////////
    
    public H3CircleRenderer(H3Graph graph, H3ViewParameters parameters,
			    H3RenderQueue queue, H3RenderList list)
    {
	m_graph = graph;
	m_parameters = parameters;
	m_renderQueue = queue;
	m_renderList = list;
    }

    ////////////////////////////////////////////////////////////////////////
    // INTERFACE METHODS (H3AdaptiveRenderer)
    ////////////////////////////////////////////////////////////////////////

    public void render(GraphicsContext3D gc)
    {
	long startTime = 0;
	if (DEBUG_PRINT)
	{
	    startTime = System.currentTimeMillis();
	    System.out.println("render.begin[" + startTime + "]");
	}

	computeRenderFrame(gc);

	m_parameters.putModelTransform(gc);
	m_renderList.render(gc);

	if (DEBUG_PRINT)
	{
	    long stopTime = System.currentTimeMillis();
	    long duration = stopTime - startTime;
	    System.out.println("render.end[" + stopTime + "]");
	    System.out.println("render.time[" + duration + "]");
	}
    }

    public void refine(GraphicsContext3D gc)
    {
	long startTime = 0;
	if (DEBUG_PRINT)
	{
	    startTime = System.currentTimeMillis();
	    System.out.println("refine.begin[" + startTime + "]");
	}

	computeRefineFrame(gc);

	m_parameters.putModelTransform(gc);
	m_renderList.render(gc);
	gc.flush(true);

	if (DEBUG_PRINT)
	{
	    long stopTime = System.currentTimeMillis();
	    long duration = stopTime - startTime;
	    System.out.println("refine.end[" + stopTime + "]");
	    System.out.println("refine.time[" + duration + "]");
	}
    }

    public void reset()
    {
	m_numDisplayedElements = 0;
    }

    public boolean isFinished()
    {
	return m_numDisplayedElements == m_renderQueue.getCurrentNumElements()
	    && m_renderQueue.isComplete();
    }

    public void setMaxDuration(long max)
    {
	m_maxDuration = max;
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE METHODS
    ////////////////////////////////////////////////////////////////////////

    private void computeRenderFrame(GraphicsContext3D gc)
    {
	long start = System.currentTimeMillis();

	m_renderList.beginFrame();
	{
	    Transform3D transform = m_parameters.getObjectToEyeTransform();

	    boolean more = computeDisplay(gc, 0, m_numDisplayedElements,
					  transform);
	    while (more && System.currentTimeMillis() - start < m_maxDuration)
	    {
		more = computeDisplay(gc, m_numDisplayedElements,
				      NUM_PER_ITERATION, transform);
	    }
	}
	m_renderList.endFrame();
    }

    private void computeRefineFrame(GraphicsContext3D gc)
    {
	long start = System.currentTimeMillis();

	m_renderList.beginFrame();
	{
	    Transform3D transform = m_parameters.getObjectToEyeTransform();

	    boolean more = true;
	    while (more && System.currentTimeMillis() - start < m_maxDuration)
	    {
		more = computeDisplay(gc, m_numDisplayedElements,
				      NUM_PER_ITERATION, transform);
	    }
	}
	m_renderList.endFrame();
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private boolean computeDisplay(GraphicsContext3D gc,
				   int index, int count,
				   Transform3D transform)
    {
	boolean retval = true;

	m_numDisplayedElements = index;

	H3Circle nodeImage = m_parameters.getNodeImage();
	Point3d eye = m_parameters.getEye();
	double nodeRadius = m_parameters.getNodeRadius();

	Point3d node = new Point3d();

	H3RenderQueue.Element element = new H3RenderQueue.Element();

	boolean more = true;
	while (more && count-- > 0)
	{
	    if (m_renderQueue.get(m_numDisplayedElements, element))
	    {
		++m_numDisplayedElements;

		if (element.type == H3RenderQueue.Element.TYPE_NODE)
		{
		    m_renderList.addNode(element.data);

		    // Render as a circle.
		    m_graph.getNodeCoordinates(element.data, node);
		    transform.transform(node);

		    double perspectiveScale = 1.0 / (1.0 - node.z / eye.z);
		    double radius = m_graph.getNodeRadius(element.data)
			* nodeRadius * perspectiveScale;

		    double centerX = eye.x + node.x * perspectiveScale;
		    double centerY = eye.y + node.y * perspectiveScale;

		    nodeImage.draw(gc, radius, centerX, centerY);
		}
		else if (element.type == H3RenderQueue.Element.TYPE_TREE_LINK)
		{
		    m_renderList.addTreeLink(element.data);
		}
		else //(type == H3RenderQueue.Element.TYPE_NONTREE_LINK)
		{
		    m_renderList.addNontreeLink(element.data);
		}
	    }
	    else
	    {
		retval = false;
		more = false;
	    }
	}

        return retval;
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE FIELDS
    ////////////////////////////////////////////////////////////////////////

    private static final boolean DEBUG_PRINT = false;
    private static final int NUM_PER_ITERATION = 25;

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private long m_maxDuration = Long.MAX_VALUE;

    private H3Graph m_graph;
    private H3ViewParameters m_parameters;
    private H3RenderQueue m_renderQueue;
    private H3RenderList m_renderList;

    private int m_numDisplayedElements = 0;
}
