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

public class H3PointRenderList
    implements H3RenderList
{
    ////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ////////////////////////////////////////////////////////////////////////
    
    public H3PointRenderList(H3Graph graph, boolean useNodeSizes,
			     boolean includeColors)
    {
	this(graph, useNodeSizes,
	     true, includeColors,
	     true, includeColors,
	     true, includeColors);
    }

    public H3PointRenderList(H3Graph graph,
			     boolean useNodeSizes,
			     boolean includeNodes,
			     boolean includeNodeColor,
			     boolean includeTreeLinks,
			     boolean includeTreeLinkColor,
			     boolean includeNontreeLinks,
			     boolean includeNontreeLinkColor)
    {
	m_graph = graph;

	USE_NODE_SIZES = useNodeSizes;
	INCLUDE_NODES = includeNodes;
	INCLUDE_NODE_COLOR = includeNodeColor;
	INCLUDE_TREE_LINKS = includeTreeLinks;
	INCLUDE_TREE_LINK_COLOR = includeTreeLinkColor;
	INCLUDE_NONTREE_LINKS =
	    includeNontreeLinks && graph.getNumNontreeLinks() > 0;
	INCLUDE_NONTREE_LINK_COLOR =
	    includeNontreeLinks && includeNontreeLinkColor;

	// Node data. - - - - - - - - - - - - - - - - - - - - - - - - - - 

	if (INCLUDE_NODES)
	{
	    int nodeFormat = PointArray.COORDINATES | PointArray.BY_REFERENCE;
	    if (INCLUDE_NODE_COLOR)
	    {
		nodeFormat |= PointArray.COLOR_3;
	    }

	    int numNodes = graph.getNumNodes();

	    m_nearNodes = new PointArray(numNodes, nodeFormat);
	    m_nearNodeCoordinates = new double[numNodes * 3];
	    m_nearNodes.setCoordRefDouble(m_nearNodeCoordinates);
	    m_nearNodes.setValidVertexCount(0);

	    if (USE_NODE_SIZES)
	    {
		m_middleNodes = new PointArray(numNodes, nodeFormat);
		m_middleNodeCoordinates = new double[numNodes * 3];
		m_middleNodes.setCoordRefDouble(m_middleNodeCoordinates);
		m_middleNodes.setValidVertexCount(0);

		// --  --  --  --  --  --  --  --  --  --  --  --  --  --  --

		m_farNodes = new PointArray(numNodes, nodeFormat);
		m_farNodeCoordinates = new double[numNodes * 3];
		m_farNodes.setCoordRefDouble(m_farNodeCoordinates);
		m_farNodes.setValidVertexCount(0);
	    }

	    if (INCLUDE_NODE_COLOR)
	    {
		m_nearNodeColors = new byte[numNodes * 3];
		m_nearNodes.setColorRefByte(m_nearNodeColors);

		if (USE_NODE_SIZES)
		{
		    m_middleNodeColors = new byte[numNodes * 3];
		    m_middleNodes.setColorRefByte(m_middleNodeColors);

		    m_farNodeColors = new byte[numNodes * 3];
		    m_farNodes.setColorRefByte(m_farNodeColors);
		}
	    }
	}

	// Link data. - - - - - - - - - - - - - - - - - - - - - - - - - -

	if (INCLUDE_TREE_LINKS)
	{
	    int lineFormat = LineArray.COORDINATES | LineArray.BY_REFERENCE;
	    if (INCLUDE_TREE_LINK_COLOR)
	    {
		lineFormat |= LineArray.COLOR_3;
	    }

	    int numLinks = graph.getNumTreeLinks();
	    m_treeLinks = new LineArray(numLinks * 2, lineFormat);
	    m_treeLinkCoordinates = new double[numLinks * 3 * 2];
	    m_treeLinks.setCoordRefDouble(m_treeLinkCoordinates);
	    m_treeLinks.setValidVertexCount(0);

	    if (INCLUDE_TREE_LINK_COLOR)
	    {
		m_treeLinkColors = new byte[numLinks * 3 * 2];
		m_treeLinks.setColorRefByte(m_treeLinkColors);
	    }
	}

	if (INCLUDE_NONTREE_LINKS)
	{
	    int lineFormat = LineArray.COORDINATES | LineArray.BY_REFERENCE;
	    if (INCLUDE_NONTREE_LINK_COLOR)
	    {
		lineFormat |= LineArray.COLOR_3;
	    }

	    int numLinks = graph.getNumNontreeLinks();
	    m_nontreeLinks = new LineArray(numLinks * 2, lineFormat);
	    m_nontreeLinkCoordinates = new double[numLinks * 3 * 2];
	    m_nontreeLinks.setCoordRefDouble(m_nontreeLinkCoordinates);
	    m_nontreeLinks.setValidVertexCount(0);

	    if (INCLUDE_NONTREE_LINK_COLOR)
	    {
		m_nontreeLinkColors = new byte[numLinks * 3 * 2];
		m_nontreeLinks.setColorRefByte(m_nontreeLinkColors);
	    }
	}
    }

    ////////////////////////////////////////////////////////////////////////
    // INTERFACE METHODS (H3RenderList)
    ////////////////////////////////////////////////////////////////////////

    public void beginFrame()
    {
	m_numNearNodesDisplayed = 0;
	m_numMiddleNodesDisplayed = 0;
	m_numFarNodesDisplayed = 0;
	m_numTreeVerticesDisplayed = 0;
	m_numNontreeVerticesDisplayed = 0;

	m_nearNodeIndex = 0;
	m_middleNodeIndex = 0;
	m_farNodeIndex = 0;
	m_treeLinkIndex = 0;
	m_nontreeLinkIndex = 0;

	m_nearNodeColorIndex = 0;
	m_middleNodeColorIndex = 0;
	m_farNodeColorIndex = 0;
	m_treeLinkColorIndex = 0;
	m_nontreeLinkColorIndex = 0;
    }

    public void endFrame()
    {
	if (m_numNearNodesDisplayed > 0)
	{
	    m_nearNodes.setValidVertexCount(m_numNearNodesDisplayed);
	}

	if (m_numMiddleNodesDisplayed > 0)
	{
	    m_middleNodes.setValidVertexCount(m_numMiddleNodesDisplayed);
	}

	if (m_numFarNodesDisplayed > 0)
	{
	    m_farNodes.setValidVertexCount(m_numFarNodesDisplayed);
	}

	if (m_numTreeVerticesDisplayed > 0)
	{
	    m_treeLinks.setValidVertexCount(m_numTreeVerticesDisplayed);
	}

	if (m_numNontreeVerticesDisplayed > 0)
	{
	    m_nontreeLinks.setValidVertexCount(m_numNontreeVerticesDisplayed);
	}
    }

    public void addNode(int node)
    {
	if (INCLUDE_NODES && m_graph.checkNodeVisible(node))
	{
	    double radius = 0.0;
	    m_graph.getNodeCoordinates(node, m_source);

	    if (USE_NODE_SIZES)
	    {
		radius = m_graph.getNodeRadius(node);
		if (radius < FAR_NODES_THRESHOLD)
		{
		    ++m_numFarNodesDisplayed;
		    m_farNodeCoordinates[m_farNodeIndex++] = m_source.x;
		    m_farNodeCoordinates[m_farNodeIndex++] = m_source.y;
		    m_farNodeCoordinates[m_farNodeIndex++] = m_source.z;
		}
		else if (radius < MIDDLE_NODES_THRESHOLD)
		{
		    ++m_numMiddleNodesDisplayed;
		    m_middleNodeCoordinates[m_middleNodeIndex++] = m_source.x;
		    m_middleNodeCoordinates[m_middleNodeIndex++] = m_source.y;
		    m_middleNodeCoordinates[m_middleNodeIndex++] = m_source.z;
		}
		else
		{
		    ++m_numNearNodesDisplayed;
		    m_nearNodeCoordinates[m_nearNodeIndex++] = m_source.x;
		    m_nearNodeCoordinates[m_nearNodeIndex++] = m_source.y;
		    m_nearNodeCoordinates[m_nearNodeIndex++] = m_source.z;
		}
	    }
	    else
	    {
		++m_numNearNodesDisplayed;
		m_nearNodeCoordinates[m_nearNodeIndex++] = m_source.x;
		m_nearNodeCoordinates[m_nearNodeIndex++] = m_source.y;
		m_nearNodeCoordinates[m_nearNodeIndex++] = m_source.z;
	    }

	    if (INCLUDE_NODE_COLOR)
	    {
		int color = m_graph.getNodeColor(node);
		byte r = (byte)((color >> 16) & 0xff);
		byte g = (byte)((color >> 8) & 0xff);
		byte b = (byte)(color & 0xff);

		if (USE_NODE_SIZES)
		{
		    if (radius < FAR_NODES_THRESHOLD)
		    {
			m_farNodeColors[m_farNodeColorIndex++] = r;
			m_farNodeColors[m_farNodeColorIndex++] = g;
			m_farNodeColors[m_farNodeColorIndex++] = b;
		    }
		    else if (radius < MIDDLE_NODES_THRESHOLD)
		    {
			m_middleNodeColors[m_middleNodeColorIndex++] = r;
			m_middleNodeColors[m_middleNodeColorIndex++] = g;
			m_middleNodeColors[m_middleNodeColorIndex++] = b;
		    }
		    else
		    {
			m_nearNodeColors[m_nearNodeColorIndex++] = r;
			m_nearNodeColors[m_nearNodeColorIndex++] = g;
			m_nearNodeColors[m_nearNodeColorIndex++] = b;
		    }
		}
		else
		{
		    m_nearNodeColors[m_nearNodeColorIndex++] = r;
		    m_nearNodeColors[m_nearNodeColorIndex++] = g;
		    m_nearNodeColors[m_nearNodeColorIndex++] = b;
		}
	    }
	}
    }

    public void addTreeLink(int link)
    {
	if (INCLUDE_TREE_LINKS && m_graph.checkLinkVisible(link))
	{
	    int sourceNode = m_graph.getLinkSource(link);
	    int targetNode = m_graph.getLinkDestination(link);

	    if (SHOW_LINKS_OF_HIDDEN_NODES
		|| (m_graph.checkNodeVisible(sourceNode)
		    && m_graph.checkNodeVisible(targetNode)))
	    {
		m_numTreeVerticesDisplayed += 2;

		m_graph.getNodeCoordinates(sourceNode, m_source);
		m_treeLinkCoordinates[m_treeLinkIndex++] = m_source.x;
		m_treeLinkCoordinates[m_treeLinkIndex++] = m_source.y;
		m_treeLinkCoordinates[m_treeLinkIndex++] = m_source.z;

		m_graph.getNodeCoordinates(targetNode, m_target);
		m_treeLinkCoordinates[m_treeLinkIndex++] = m_target.x;
		m_treeLinkCoordinates[m_treeLinkIndex++] = m_target.y;
		m_treeLinkCoordinates[m_treeLinkIndex++] = m_target.z;

		if (INCLUDE_TREE_LINK_COLOR)
		{
		    int color = m_graph.getLinkColor(link);
		    byte r = (byte)((color >> 16) & 0xff);
		    byte g = (byte)((color >> 8) & 0xff);
		    byte b = (byte)(color & 0xff);

		    m_treeLinkColors[m_treeLinkColorIndex++] = r;
		    m_treeLinkColors[m_treeLinkColorIndex++] = g;
		    m_treeLinkColors[m_treeLinkColorIndex++] = b;

		    m_treeLinkColors[m_treeLinkColorIndex++] = r;
		    m_treeLinkColors[m_treeLinkColorIndex++] = g;
		    m_treeLinkColors[m_treeLinkColorIndex++] = b;
		}
	    }
	}
    }

    public void addNontreeLink(int link)
    {
	if (INCLUDE_NONTREE_LINKS && m_graph.checkLinkVisible(link))
	{
	    int sourceNode = m_graph.getLinkSource(link);
	    int targetNode = m_graph.getLinkDestination(link);

	    if (SHOW_LINKS_OF_HIDDEN_NODES
		|| (m_graph.checkNodeVisible(sourceNode)
		    && m_graph.checkNodeVisible(targetNode)))
	    {
		m_numNontreeVerticesDisplayed += 2;

		m_graph.getNodeCoordinates(sourceNode, m_source);
		m_nontreeLinkCoordinates[m_nontreeLinkIndex++] = m_source.x;
		m_nontreeLinkCoordinates[m_nontreeLinkIndex++] = m_source.y;
		m_nontreeLinkCoordinates[m_nontreeLinkIndex++] = m_source.z;

		m_graph.getNodeCoordinates(targetNode, m_target);
		m_nontreeLinkCoordinates[m_nontreeLinkIndex++] = m_target.x;
		m_nontreeLinkCoordinates[m_nontreeLinkIndex++] = m_target.y;
		m_nontreeLinkCoordinates[m_nontreeLinkIndex++] = m_target.z;

		if (INCLUDE_NONTREE_LINK_COLOR)
		{
		    int color = m_graph.getLinkColor(link);
		    byte r = (byte)((color >> 16) & 0xff);
		    byte g = (byte)((color >> 8) & 0xff);
		    byte b = (byte)(color & 0xff);

		    m_nontreeLinkColors[m_nontreeLinkColorIndex++] = r;
		    m_nontreeLinkColors[m_nontreeLinkColorIndex++] = g;
		    m_nontreeLinkColors[m_nontreeLinkColorIndex++] = b;

		    m_nontreeLinkColors[m_nontreeLinkColorIndex++] = r;
		    m_nontreeLinkColors[m_nontreeLinkColorIndex++] = g;
		    m_nontreeLinkColors[m_nontreeLinkColorIndex++] = b;
		}
	    }
	}
    }

    public void render(GraphicsContext3D gc)
    {
	long startTime = 0;
	if (DEBUG_PRINT)
	{
	    startTime = System.currentTimeMillis();
	    System.out.println("render.begin[" + startTime + "]");
	}

	if (m_numNearNodesDisplayed > 0)
	{
	    drawGeometry(gc, m_nearNodes, m_nearNodeAppearance);
	}

	if (m_numMiddleNodesDisplayed > 0)
	{
	    drawGeometry(gc, m_middleNodes, m_middleNodeAppearance);
	}

	if (m_numFarNodesDisplayed > 0)
	{
	    drawGeometry(gc, m_farNodes, m_farNodeAppearance);
	}

	if (m_numTreeVerticesDisplayed > 0)
	{
	    drawGeometry(gc, m_treeLinks, m_treeLinkAppearance);
	}

	if (m_numNontreeVerticesDisplayed > 0)
	{
	    drawGeometry(gc, m_nontreeLinks, m_nontreeLinkAppearance);
	}

	if (DEBUG_PRINT)
	{
	    long stopTime = System.currentTimeMillis();
	    long duration = stopTime - startTime;
	    System.out.println("render.end[" + stopTime + "]");
	    System.out.println("render.time[" + duration + "]");
	}
    }

    ////////////////////////////////////////////////////////////////////////
    // PUBLIC METHODS
    ////////////////////////////////////////////////////////////////////////

    public void setNearNodeAppearance(Appearance appearance)
    {
	m_nearNodeAppearance = appearance;
    }

    public void setMiddleNodeAppearance(Appearance appearance)
    {
	m_middleNodeAppearance = appearance;
    }

    public void setFarNodeAppearance(Appearance appearance)
    {
	m_farNodeAppearance = appearance;
    }

    public void setTreeLinkAppearance(Appearance appearance)
    {
	m_treeLinkAppearance = appearance;
    }

    public void setNontreeLinkAppearance(Appearance appearance)
    {
	m_nontreeLinkAppearance = appearance;
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE METHODS
    ////////////////////////////////////////////////////////////////////////

    private void drawGeometry(GraphicsContext3D gc, Geometry geometry,
			      Appearance appearance)
    {
	Appearance currentAppearance = null;

	if (appearance != null)
	{
	    currentAppearance = gc.getAppearance();
	    gc.setAppearance(appearance);
	}

	gc.draw(geometry);

	if (currentAppearance != null)
	{
	    gc.setAppearance(currentAppearance);
	}
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE FIELDS
    ////////////////////////////////////////////////////////////////////////

    private static final boolean DEBUG_PRINT = false;

    private static final boolean SHOW_LINKS_OF_HIDDEN_NODES = true;

    private static final double MIDDLE_NODES_THRESHOLD = 0.5;
    private static final double FAR_NODES_THRESHOLD = 0.2;

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private H3Graph m_graph;

    // These are set in the constructor.
    private final boolean USE_NODE_SIZES;
    private final boolean INCLUDE_NODES;
    private final boolean INCLUDE_NODE_COLOR;
    private final boolean INCLUDE_TREE_LINKS;
    private final boolean INCLUDE_TREE_LINK_COLOR;
    private final boolean INCLUDE_NONTREE_LINKS;
    private final boolean INCLUDE_NONTREE_LINK_COLOR;

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private Appearance m_nearNodeAppearance;
    private Appearance m_middleNodeAppearance;
    private Appearance m_farNodeAppearance;
    private Appearance m_treeLinkAppearance;
    private Appearance m_nontreeLinkAppearance;

    private Point3d m_source = new Point3d();  // scratch variable
    private Point3d m_target = new Point3d();  // scratch variable

    private int m_numNearNodesDisplayed;
    private int m_numMiddleNodesDisplayed;
    private int m_numFarNodesDisplayed;
    private int m_numTreeVerticesDisplayed;
    private int m_numNontreeVerticesDisplayed;

    private int m_nearNodeIndex;
    private int m_middleNodeIndex;
    private int m_farNodeIndex;
    private int m_treeLinkIndex;
    private int m_nontreeLinkIndex;

    private int m_nearNodeColorIndex;
    private int m_middleNodeColorIndex;
    private int m_farNodeColorIndex;
    private int m_treeLinkColorIndex;
    private int m_nontreeLinkColorIndex;

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    // This contains the colors of all nodes flattened into a single array.
    // The color of each node appears as consecutive r, g, and b values.
    private byte[] m_nearNodeColors;

    // This contains the coordinates of all nodes flattened into a single
    // array.  The coordinates of each node appear as consecutive x, y, and
    // z values.
    private double[] m_nearNodeCoordinates; 

    private PointArray m_nearNodes; // refs the above two arrays

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    // See comments above for the set m_nearNodes, m_nearNodeColors, etc.
    private byte[] m_middleNodeColors;
    private double[] m_middleNodeCoordinates; 
    private PointArray m_middleNodes;

    private byte[] m_farNodeColors;
    private double[] m_farNodeCoordinates; 
    private PointArray m_farNodes;

    private byte[] m_treeLinkColors;
    private double[] m_treeLinkCoordinates; 
    private LineArray m_treeLinks;

    private byte[] m_nontreeLinkColors;
    private double[] m_nontreeLinkCoordinates; 
    private LineArray m_nontreeLinks;
}
