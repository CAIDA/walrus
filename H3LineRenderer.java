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


import java.util.*;
import javax.media.j3d.*;
import javax.vecmath.*;

public class H3LineRenderer
    implements H3AdaptiveRenderer
{
    ////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ////////////////////////////////////////////////////////////////////////
    
    public H3LineRenderer(H3Graph graph, H3RenderQueue queue,
			  H3RenderList list)
    {
	m_graph = graph;
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

	computeRenderFrame();
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

	computeRefineFrame();
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

    private void computeRenderFrame()
    {
	long start = System.currentTimeMillis();

	m_renderList.beginFrame();

	boolean more = computeDisplay(0, m_numDisplayedElements);
	while (more && System.currentTimeMillis() - start < m_maxDuration)
	{
	    more = computeDisplay(m_numDisplayedElements, NUM_PER_ITERATION);
	}

	m_renderList.endFrame();
    }

    private void computeRefineFrame()
    {
	long start = System.currentTimeMillis();

	m_renderList.beginFrame();

	boolean more = true;
	while (more && System.currentTimeMillis() - start < m_maxDuration)
	{
	    more = computeDisplay(m_numDisplayedElements, NUM_PER_ITERATION);
	}

	m_renderList.endFrame();
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private boolean computeDisplay(int index, int count)
    {
	boolean retval = true;

	m_numDisplayedElements = index;

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
    private H3RenderQueue m_renderQueue;
    private H3RenderList m_renderList;

    private int m_numDisplayedElements = 0;
}
