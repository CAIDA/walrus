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

public class H3Transformer
    implements Runnable
{
    ////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ////////////////////////////////////////////////////////////////////////

    public H3Transformer(H3Graph graph, H3RenderQueue queue,
			 boolean transformNontreeLinks)
    {
	m_visited = new int[graph.getNumNodes()];
	m_startingNode = graph.getRootNode();
	m_graph = graph;
	m_renderQueue = queue;
	m_transformQueue = new H3TransformQueue(graph.getNumNodes());
	m_transformNontreeLinks = transformNontreeLinks;
    }

    ////////////////////////////////////////////////////////////////////////
    // PUBLIC METHODS
    ////////////////////////////////////////////////////////////////////////

    public synchronized void transform(Matrix4d transform)
    {
	startRequest();
	{
	    if (DEBUG_PRINT)
	    {
		System.out.println("Hyperbolic.transform()");
	    }

	    if (m_graph.getNumNodes() > 0)
	    {
		++m_iteration;
		m_renderQueue.clear();
		m_transformQueue.clear();

		m_transformTemporary.mul(transform, m_transform);
		m_transform.set(m_transformTemporary);

		markNodeVisited(m_startingNode, m_iteration);
		m_startingRadius = transformAndEnqueueNode(m_startingNode);
		m_state = STATE_NODE;
	    }
	    else
	    {
		m_renderQueue.clear();
		m_renderQueue.end();
		m_state = STATE_IDLE;
	    }
	}
	endRequest();
    }

    public synchronized void transformNode(int node, Point4d p)
    {
	m_graph.getNodeLayoutCoordinates(node, p);
	m_transform.transform(p);
    }

    // XXX: Why isn't this safeguarded with startRequest() .. endRequest()?
    public synchronized void pushPosition()
    {
	Position position = new Position();
	position.startingNode = m_startingNode;
	position.transform.set(m_transform);

	m_savedPositions.add(position);
    }

    public synchronized void popPosition()
    {
	startRequest();
	{
	    if (m_graph.getNumNodes() > 0)
	    {
		Position position = (Position)
		    m_savedPositions.remove(m_savedPositions.size() - 1);
		reinstatePosition(position);
	    }
	    else
	    {
		m_renderQueue.clear();
		m_renderQueue.end();
		m_state = STATE_IDLE;
	    }
	}
	endRequest();
    }

    public synchronized void discardPosition()
    {
        m_savedPositions.remove(m_savedPositions.size() - 1);
    }

    public synchronized Position getPosition()
    {
	Position retval = new Position();
	retval.startingNode = m_startingNode;
	retval.transform.set(m_transform);
	return retval;
    }

    public synchronized void setPosition(Position position)
    {
	startRequest();
	{
	    if (m_graph.getNumNodes() > 0)
	    {
		reinstatePosition(position);
	    }
	    else
	    {
		m_renderQueue.clear();
		m_renderQueue.end();
		m_state = STATE_IDLE;
	    }
	}
	endRequest();
    }

    public synchronized void shutdown()
    {
	startRequest();
	{
	    m_state = STATE_SHUTDOWN;
	}
	endRequest();
    }

    ////////////////////////////////////////////////////////////////////////
    // INTERFACE METHODS (Runnable)
    ////////////////////////////////////////////////////////////////////////

    public void run()
    {
	transformIdentity();

	while (true)
	{
	    rendezvousWithRequests();
	    if (m_state == STATE_SHUTDOWN)
	    {
		System.out.println("H3Transformer exiting...");
		return;
	    }

	    m_numTransformed = 0;
	    while (m_state != STATE_IDLE
		   && m_numTransformed < NUM_PER_ITERATION)
	    {
		switch (m_state)
		{
		case STATE_NODE:
		    if (DEBUG_PRINT)
		    {
			System.out.println("Hyperbolic.STATE_NODE");
		    }
		    beNodeState();
		    break;

		case STATE_CHILD_LINK:
		    if (DEBUG_PRINT)
		    {
			System.out.println("Hyperbolic.STATE_CHILD_LINK");
		    }
		    beChildLinkState();
		    break;

		case STATE_NONTREE_LINK:
		    if (DEBUG_PRINT)
		    {
			System.out.println("Hyperbolic.STATE_NONTREE_LINK");
		    }
		    beNontreeLinkState();
		    break;

		case STATE_SHUTDOWN:
		    //FALLTHROUGH
		case STATE_IDLE:
		    //FALLTHROUGH
		default:
		    throw new RuntimeException("Invalid state: " + m_state);
		}
	    }

	    if (m_numTransformed > 0)
	    {
		m_renderQueue.add(m_numTransformed, m_transformedData);
	    }

	    if (m_state == STATE_IDLE)
	    {
		m_renderQueue.end();
	    }
	}
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE METHODS
    ////////////////////////////////////////////////////////////////////////

    private void transformIdentity()
    {
	System.out.println("Hyperbolic.transformIdentity()");

	if (m_graph.getNumNodes() > 0)
	{
	    ++m_iteration;
	    m_renderQueue.clear();
	    m_transformQueue.clear();
	    m_transform.setIdentity();

	    markNodeVisited(m_startingNode, m_iteration);
	    m_startingRadius = transformAndEnqueueNode(m_startingNode);
	    m_state = STATE_NODE;
	}
	else
	{
	    m_renderQueue.clear();
	    m_renderQueue.end();
	    m_state = STATE_IDLE;
	}
    }

    // NOTE: This assumes that m_graph.getNumNodes() > 0.
    private synchronized void reinstatePosition(Position position)
    {
	++m_iteration;
	m_renderQueue.clear();
	m_transformQueue.clear();

	m_startingNode = position.startingNode;
	m_transform.set(position.transform);

	markNodeVisited(m_startingNode, m_iteration);
	m_startingRadius = transformAndEnqueueNode(m_startingNode);
	m_state = STATE_NODE;
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE METHODS (states)
    ////////////////////////////////////////////////////////////////////////

    private void beNodeState()
    {
	if (m_transformQueue.isEmpty())
	{
	    m_state = STATE_IDLE;
	}
	else
	{
	    m_currentNode = m_transformQueue.dequeue();
	    m_transformedData[m_numTransformed++] =
		((long)H3RenderQueue.Element.TYPE_NODE << 32) | m_currentNode;

	    checkCandidateForStarting(m_currentNode);

	    int parent = m_graph.getNodeParent(m_currentNode);
	    if (parent >= 0)
	    {
		transformAndEnqueueNodeIfNotVisited(parent);
	    }

	    m_currentChildIndex = m_graph.getNodeChildIndex(m_currentNode);
	    m_currentNontreeIndex = m_graph.getNodeNontreeIndex(m_currentNode);
	    m_currentLinksEndIndex =
		m_graph.getNodeLinksEndIndex(m_currentNode);

	    m_currentIndex = m_currentChildIndex;

	    if (m_currentChildIndex < m_currentNontreeIndex)
	    {
		m_state = STATE_CHILD_LINK;
	    }
	    else
	    {
		// Stay in STATE_NODE unless there are non-tree links.
		if (m_transformNontreeLinks)
		{
		    if (m_currentNontreeIndex < m_currentLinksEndIndex)
		    {
			m_state = STATE_NONTREE_LINK;
		    }
		}
	    }
	}
    }

    private void beChildLinkState()
    {
	m_transformedData[m_numTransformed++] =
	    ((long)H3RenderQueue.Element.TYPE_TREE_LINK << 32)
	    | m_currentIndex;

	int child = m_graph.getLinkDestination(m_currentIndex);
	transformAndEnqueueNodeIfNotVisited(child);

	if (++m_currentIndex == m_currentNontreeIndex)
	{
	    m_state = STATE_NODE;
	    if (m_transformNontreeLinks)
	    {
		if (m_currentNontreeIndex < m_currentLinksEndIndex)
		{
		    m_state = STATE_NONTREE_LINK;
		}
	    }
	}
	// else stay in STATE_CHILD_LINK
    }

    private void beNontreeLinkState()
    {
	m_transformedData[m_numTransformed++] =
	    ((long)H3RenderQueue.Element.TYPE_NONTREE_LINK << 32)
	    | m_currentIndex;

	int target = m_graph.getLinkDestination(m_currentIndex);
	transformAndEnqueueNodeIfNotVisited(target);

	if (++m_currentIndex == m_currentLinksEndIndex)
	{
	    m_state = STATE_NODE;
	}
	// else stay in STATE_NONTREE_LINK
    }

    private void transformAndEnqueueNodeIfNotVisited(int node)
    {
	if (!markNodeVisited(node, m_iteration))
	{
	    transformAndEnqueueNode(node);
	}
    }

    private double transformAndEnqueueNode(int node)
    {
	double radius = transformNode(node);
	m_transformQueue.enqueue(node, radius);
	return radius;
    }

    // The same radius calculation is done in
    // H3NonadaptiveRenderLoop.computeNodeRadius().
    // The two methods should be kept in sync to ensure a consistent display
    // when the user turns adaptive rendering on/off.
    private double transformNode(int node)
    {
	m_graph.getNodeLayoutCoordinates(node, m_nodeCoordinates);
	m_transform.transform(m_nodeCoordinates);

	double radius = H3Math.computeRadiusEuclidean(m_nodeCoordinates);

	m_graph.setNodeRadius(node, radius);
	m_graph.setNodeCoordinates(node, m_nodeCoordinates);
	return radius;
    }

    private void checkCandidateForStarting(int node)
    {
	double radius = m_graph.getNodeRadius(node);
	if (radius > m_startingRadius)
	{
	    m_startingNode = node;
	    m_startingRadius = radius;
	}
    }

    private boolean checkNodeVisited(int node, int iteration)
    {
	return m_visited[node] == iteration;
    }

    private boolean markNodeVisited(int node, int iteration)
    {
	boolean retval = (m_visited[node] == iteration);
	if (!retval)
	{
	    m_visited[node] = iteration;
	}
	return retval;
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE METHODS (request synchronization)
    ////////////////////////////////////////////////////////////////////////

    private synchronized void startRequest()
    {
	++m_numPendingRequests;
	while (!m_isRequestTurn)
	{
	    waitIgnore();
	}
	--m_numPendingRequests;
    }

    private synchronized void endRequest()
    {
	m_isRequestTurn = false;
	notifyAll();
    }

    private synchronized void rendezvousWithRequests()
    {
	if (m_state == STATE_IDLE || m_numPendingRequests > 0)
	{
	    do
	    {
		m_isRequestTurn = true;
		notifyAll();
		waitIgnore();
	    }
	    while (m_state == STATE_IDLE);
	}
    }

    private synchronized void waitIgnore()
    {
	try { wait(); } catch (InterruptedException e) { }
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE FIELDS
    ////////////////////////////////////////////////////////////////////////

    private static final boolean DEBUG_PRINT = false;

    private static final int STATE_SHUTDOWN = 0;
    private static final int STATE_IDLE = 1;
    private static final int STATE_NODE = 2;
    private static final int STATE_CHILD_LINK = 3;
    private static final int STATE_NONTREE_LINK = 4;

    private int m_state = STATE_IDLE;

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private int m_numPendingRequests = 0;
    private boolean m_isRequestTurn = false;

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private int m_iteration = 0;
    // Whether a node has been visited in "traversal iteration" t > 0.
    private int[] m_visited; 

    private int m_startingNode; // Will be set to the root node in constructor.
    private double m_startingRadius = 0.0;

    private boolean m_transformNontreeLinks;

    private H3Graph m_graph;
    private H3RenderQueue m_renderQueue;
    private H3TransformQueue m_transformQueue;

    private Matrix4d m_transform = new Matrix4d();

    private List m_savedPositions = new ArrayList(); // List<Position>

    private Matrix4d m_transformTemporary = new Matrix4d(); // scratch
    private Point4d m_nodeCoordinates = new Point4d(); // scratch variable

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private static final int NUM_PER_ITERATION = 100;

    private int m_numTransformed = 0;

    // A long in m_transformedData is a structure of two ints (a, b),
    // where ((a << 32) | b) equals the long, and has one of the following
    // forms and meanings:
    //
    //    (TYPE_NODE, n)           => a node {n}
    //    (TYPE_TREE_LINK, t)      => a tree link {t}
    //    (TYPE_NONTREE_LINK, nt)  => a non-tree link {nt}
    //
    // where TYPE_NODE, etc., are the constants defined in
    // H3RenderQueue.Element.
    //
    private long[] m_transformedData = new long[NUM_PER_ITERATION];

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private int m_currentNode;
    private int m_currentIndex;
    private int m_currentChildIndex;
    private int m_currentNontreeIndex;
    private int m_currentLinksEndIndex;

    ////////////////////////////////////////////////////////////////////////
    // PUBLIC CLASSES
    ////////////////////////////////////////////////////////////////////////

    public static class Position
    {
	public int startingNode;
	public Matrix4d transform = new Matrix4d(); // Copy of original matrix.
    }
}
