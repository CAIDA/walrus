// 
// Copyright 2000 The Regents of the University of California
// All Rights Reserved
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

		m_graph.markNodeVisited(m_startingNode, m_iteration);
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

	    m_graph.markNodeVisited(m_startingNode, m_iteration);
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

	m_graph.markNodeVisited(m_startingNode, m_iteration);
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
	if (!m_graph.markNodeVisited(node, m_iteration))
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

    private double transformNode(int node)
    {
	m_graph.getNodeLayoutCoordinates(node, m_nodeCoordinates);
	m_transform.transform(m_nodeCoordinates);

	double radius = computeRadiusEuclidean(m_nodeCoordinates);

	m_graph.setNodeRadius(node, radius);
	m_graph.setNodeCoordinates(node, m_nodeCoordinates);
	return radius;
    }

    // Returns 1.0 / d_hyp(0, p), where d_hyp is the hyperbolic metric
    // (simplified) and 0 is the origin.
    private double computeRadiusHyperbolic(Point4d p)
    {
	double d = (p.x * p.x + p.y * p.y + p.z * p.z) / (p.w * p.w);
	double a = Math.sqrt(1.0 / (1.0 - d));
	double b = Math.sqrt(d / (1.0 - d));
	return 1.0 / (1.0 + 2.0 * Math.log(a + b));
    }

    // This seems to be about ten times faster than computeRadiusHyperbolic().
    private double computeRadiusEuclidean(Point4d p)
    {
	double d = (p.x * p.x + p.y * p.y + p.z * p.z) / (p.w * p.w);
	return 1.0 - d;
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
