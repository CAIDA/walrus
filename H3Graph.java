// 
// Copyright 2000,2001,2002 The Regents of the University of California
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
import javax.vecmath.*;

//
// This is the graph representation used and needed by the rendering part
// of Walrus.
// 
// Walrus traverses this graph while rendering the display.  Hence, the
// main requirement on the design of this class is fast read access.
// Also, nothing more need be stored in this class than the bare graph
// topology, coloring information, and coordinates; all other data (such
// as attributes associated with nodes and links) should be stored in a
// backing graph.  Because of these design goals, this class is minimal
// in implementation.
// 
// An additional design goal is the ability to handle large graphs.
// Because there will usually be a backing graph from which this class is
// populated, we want the overhead of construction to be as small as
// possible.  In particular, we want to eliminate the need for
// intermediate data structures during construction.  This goal is
// accomplished by requiring the user to populate links in a disciplined
// manner.  Specifically, links must be populated in batches, with all
// the links of each node populated in a consecutive sequence of
// operations.  That is, rather than allowing the user to add the links
// of all nodes in an arbitrary order, users must add all the links of
// some node A with a sequence of calls, and then all the links of some
// node B, and so on.  The ordering of the nodes themselves, however,
// doesn't matter.
// 
// This ordering requirement lets this class store a graph in a very
// compact way (as a set of parallel primitive arrays) without
// necessitating the use of an intermediate representation, which would
// raise the peak memory usage during graph construction---some graphs
// can take up more than 100 MBs of memory, so eliminating unnecessary
// data structures really is important.
// 
// So, the basic steps the user would take to create and populate an
// H3Graph is as follows, in pseudo-code:
// 
//        Create an H3Graph instance.
//        Iterate over the nodes in the user's data set [backing graph]:
//             If the node has outgoing links:
//                 Call startChildLinks().
//                 For each child link [tree link] (can be zero):
//                     Call addChildLink().
//                 Call startNontreeLinks().
//                 For each nontree link (can be zero):
//                     Call addNontreeLink().
//                 Call endNodeLinks().
// 
// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// 
// This class refers to nodes and links using internally defined indices.
// Suppose you create an instance with the following:
// 
//     H3Graph graph = new H3Graph(numNodes, numLinks);
// 
// Then, as far as H3Graph is concerned, the nodes are identified with
// the indices 0, ..., numNodes-1, and links with 0, ..., numLinks-1.
// The actual correspondence between these indices and the nodes and
// links in the user's data set (backing graph) is entirely up to the
// user; all that matters to H3Graph is that the user always handles the
// correspondences consistently.  To help the user maintain the
// correspondences, H3Graph can associate an external ID number with each
// node or link.  This facility is provided through the methods
// {get,set}NodeID() and {get,set}LinkID().  Also, addChildLink() and
// addNontreeLink() take a parameter containing the external ID of the
// link being added.  In summary, to use H3Graph, users must have some
// way of mapping between their set of nodes and links and the IDs known
// by H3Graph.
// 
// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// 
// Be sure to call setRootNode() at some point during construction so
// that the rendering part of Walrus (and any other users of this data)
// knows at which node to start its traversal.
// 

public class H3Graph
{
    ////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ////////////////////////////////////////////////////////////////////////

    public H3Graph(int numNodes, int numLinks)
    {
	// PRECONDITION: numNodes >= 0
	// PRECONDITION: numLinks >= 0

	m_numNodes = numNodes;
	m_numLinks = numLinks;

	m_nodes = new Nodes(numNodes);
	m_links = new Links(numLinks);
    }

    ////////////////////////////////////////////////////////////////////////
    // PUBLIC ACCESSOR METHODS
    ////////////////////////////////////////////////////////////////////////

    public int getNumNodes()
    {
	return m_numNodes;
    }

    public int getNumTreeLinks()
    {
	return m_numTreeLinks;
    }

    public int getNumNontreeLinks()
    {
	return m_numNontreeLinks;
    }

    public int getTotalNumLinks()
    {
	return m_numLinks;
    }

    public int getRootNode()
    {
	return m_rootNode;
    }

    public int getNodeID(int node)
    {
	return m_nodes.id[node];
    }

    public double getNodeRadius(int node)
    {
	return m_nodes.radius[node];
    }

    public void getNodeCoordinates(int node, Point3d point)
    {
	point.x = m_nodes.x[node];
	point.y = m_nodes.y[node];
	point.z = m_nodes.z[node];
    }

    public void getNodeCoordinates(int node, Point4d point)
    {
	point.x = m_nodes.x[node];
	point.y = m_nodes.y[node];
	point.z = m_nodes.z[node];
	point.w = 1.0;
    }

    public void getNodeLayoutCoordinates(int node, Point3d point)
    {
	double w = m_nodes.layoutW[node];
	point.x = m_nodes.layoutX[node] / w;
	point.y = m_nodes.layoutY[node] / w;
	point.z = m_nodes.layoutZ[node] / w;
    }

    public void getNodeLayoutCoordinates(int node, Point4d point)
    {
	point.x = m_nodes.layoutX[node];
	point.y = m_nodes.layoutY[node];
	point.z = m_nodes.layoutZ[node];
	point.w = m_nodes.layoutW[node];
    }

    public int getNodeParent(int node)
    {
	int i = m_nodes.parent[node];
	return (i == -1 ? -1 : m_links.source[i]);
    }

    public int getNodeParentLink(int node)
    {
	return m_nodes.parent[node];
    }

    // The following methods--getNodeChildIndex(), getNodeNontreeIndex(),
    // and getNodeLinksEndIndex()--provide a way of iterating over all
    // the outgoing links of a node.
    //
    // You would iterate over the outgoing links in the following manner:
    //
    //     int start = graph.getNodeChildIndex(node);
    //     int end = graph.getNodeLinksEndIndex(node);
    //     int nontreeStart = graph.getNodeNontreeIndex();
    //
    //     for (int i = start; i < nontreeStart; i++)
    //     {
    //        /* i is the index of an outgoing child link */
    //        graph.setLinkColor(i, Color.black);
    //        /* ... */
    //     }
    //
    //     for (int i = nontreeStart; i < end; i++)
    //     {
    //        /* i is the index of an outgoing nontree link */
    //     }

    public int getNodeChildIndex(int node)
    {
	return m_nodes.treeLinks[node];
    }

    public int getNodeNontreeIndex(int node)
    {
	return m_nodes.nontreeLinks[node];
    }

    public int getNodeLinksEndIndex(int node)
    {
	return m_nodes.linksEnd[node];
    }

    public int getNodeColor(int node)
    {
	return m_nodes.color[node];
    }

    public boolean checkNodeVisible(int node)
    {
	return (m_nodes.isHidden == null || !m_nodes.isHidden.get(node));
    }

    //======================================================================

    public int getLinkID(int link)
    {
	return m_links.id[link];
    }

    public int getLinkSource(int link)
    {
	return m_links.source[link];
    }

    public int getLinkDestination(int link)
    {
	return m_links.destination[link];
    }

    public int getLinkColor(int link)
    {
	return m_links.color[link];
    }

    public boolean checkTreeLink(int link)
    {
	return m_links.isTreeLink.get(link);
    }

    public boolean checkLinkVisible(int link)
    {
	return (m_links.isHidden == null || !m_links.isHidden.get(link));
    }

    ////////////////////////////////////////////////////////////////////////
    // PUBLIC MUTATOR METHODS
    ////////////////////////////////////////////////////////////////////////

    // Computes new display coordinates for each node by transforming the
    // layout coordinates of nodes with the supplied matrix.
    public void transformNodes(Matrix4d t)
    {
	Point4d p = new Point4d();
	for (int i = 0; i < m_numNodes; i++)
	{
	    p.x = m_nodes.layoutX[i];
	    p.y = m_nodes.layoutY[i];
	    p.z = m_nodes.layoutZ[i];
	    p.w = m_nodes.layoutW[i];

	    t.transform(p);

	    m_nodes.x[i] = p.x / p.w;
	    m_nodes.y[i] = p.y / p.w;
	    m_nodes.z[i] = p.z / p.w;
	}
    }

    public void setRootNode(int node)
    {
	m_rootNode = node;
    }

    public void setNodeID(int node, int id)
    {
	m_nodes.id[node] = id;
    }

    public void setNodeRadius(int node, double radius)
    {
	m_nodes.radius[node] = radius;
    }

    public void setNodeCoordinates(int node, double x, double y, double z)
    {
	m_nodes.x[node] = x;
	m_nodes.y[node] = y;
	m_nodes.z[node] = z;
    }

    public void setNodeCoordinates(int node, Point3d p)
    {
	m_nodes.x[node] = p.x;
	m_nodes.y[node] = p.y;
	m_nodes.z[node] = p.z;
    }

    public void setNodeCoordinates(int node, Point4d p)
    {
	m_nodes.x[node] = p.x / p.w;
	m_nodes.y[node] = p.y / p.w;
	m_nodes.z[node] = p.z / p.w;
    }

    public void setNodeLayoutCoordinates(int node, double x, double y,
					 double z, double w)
    {
	m_nodes.layoutX[node] = x;
	m_nodes.layoutY[node] = y;
	m_nodes.layoutZ[node] = z;
	m_nodes.layoutW[node] = w;
    }

    public void setNodeLayoutCoordinates(int node, Point3d p)
    {
	m_nodes.layoutX[node] = p.x;
	m_nodes.layoutY[node] = p.y;
	m_nodes.layoutZ[node] = p.z;
	m_nodes.layoutW[node] = 1.0;
    }

    public void setNodeLayoutCoordinates(int node, Point4d p)
    {
	m_nodes.layoutX[node] = p.x;
	m_nodes.layoutY[node] = p.y;
	m_nodes.layoutZ[node] = p.z;
	m_nodes.layoutW[node] = p.w;
    }

    // The following two methods, addChildLink() and addNodeNontreeLink(),
    // must be called in a disciplined manner.  The sequence of calls to
    // add the links of one node should never interleave with the sequence
    // of another node.  Additionally, for a particular node, all child links
    // must be added first, followed by all non-tree links.
    // 
    // The required calling sequence for a node is as follows:
    //
    //    startChildLinks()
    //    addChildLink() ... addChildLink()  [zero or more times]
    //    startNontreeLinks()
    //    addNontreeLink() ... addNontreeLink()  [zero or more times]
    //    endNodeLinks()
    //
    // There need not be a sequence of these calls for nodes without any links.

    public void startChildLinks(int node)
    {
	m_nodes.treeLinks[node] = m_links.nextIndex;
    }

    // linkID is the ID of the corresponding link in the backing libsea graph
    // (or whatever backing data store you're using).
    public void addChildLink(int node, int child, int linkID)
    {
	++m_numTreeLinks;

	int link = m_links.nextIndex++;
	m_nodes.parent[child] = link;
	m_links.id[link] = linkID;
	m_links.source[link] = node;
	m_links.destination[link] = child;
	m_links.isTreeLink.set(link);
    }

    public void startNontreeLinks(int node)
    {
	m_nodes.nontreeLinks[node] = m_links.nextIndex;
    }

    // linkID is the ID of the corresponding link in the backing libsea graph
    // (or whatever backing data store you're using).
    public void addNontreeLink(int node, int target, int linkID)
    {
	++m_numNontreeLinks;

	int link = m_links.nextIndex++;
	m_links.id[link] = linkID;
	m_links.source[link] = node;
	m_links.destination[link] = target;
    }

    public void endNodeLinks(int node)
    {
	m_nodes.linksEnd[node] = m_links.nextIndex;
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    public void setNodeColor(int node, int color)
    {
	m_nodes.color[node] = color;
    }

    public void setNodeColor(int node, byte r, byte g, byte b)
    {
	m_nodes.color[node] = (r << 16) | (g << 8) | b;
    }

    public void setNodeDefaultColor(int color)
    {
	Arrays.fill(m_nodes.color, color);
    }

    public void setNodeDefaultColor(byte r, byte g, byte b)
    {
	int color = (r << 16) | (g << 8) | b;
	setNodeDefaultColor(color);
    }

    public void setNodeVisibility(int node, boolean isVisible)
    {
	if (m_nodes.isHidden == null)
	{
	    m_nodes.isHidden = new BitSet(m_numNodes);
	}

	if (isVisible)
	{
	    m_nodes.isHidden.clear(node);
	}
	else
	{
	    m_nodes.isHidden.set(node);
	}
    }

    public void resetNodeVisibility()
    {
	m_nodes.isHidden = null;
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    public void setLinkID(int link, int id)
    {
	m_links.id[link] = id;
    }

    public void setLinkColor(int link, int color)
    {
	m_links.color[link] = color;
    }

    public void setLinkColor(int link, byte r, byte g, byte b)
    {
	m_links.color[link] = (r << 16) | (g << 8) | b;
    }

    public void setLinkDefaultColor(int color)
    {
	Arrays.fill(m_links.color, color);
    }

    public void setLinkDefaultColor(byte r, byte g, byte b)
    {
	int color = (r << 16) | (g << 8) | b;
	setLinkDefaultColor(color);
    }

    public void setLinkVisibility(int link, boolean isVisible)
    {
	if (m_links.isHidden == null)
	{
	    m_links.isHidden = new BitSet(m_numLinks);
	}

	if (isVisible)
	{
	    m_links.isHidden.clear(link);
	}
	else
	{
	    m_links.isHidden.set(link);
	}
    }

    public void resetLinkVisibility()
    {
	m_links.isHidden = null;
    }

    public void resetLinkVisibility(boolean treeLink)
    {
	if (m_links.isHidden != null)
	{
	    int length = m_links.isHidden.length();
	    for (int i = 0; i < length; i++)
	    {
		if (m_links.isHidden.get(i)
		    && m_links.isTreeLink.get(i) == treeLink)
		{
		    m_links.isHidden.clear(i);
		}
	    }

	    if (m_links.isHidden.length() == 0)
	    {
		m_links.isHidden = null;
	    }
	}
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE FIELDS
    ////////////////////////////////////////////////////////////////////////

    private int m_numNodes;
    private int m_numLinks;

    private int m_numTreeLinks = 0;
    private int m_numNontreeLinks = 0;

    private int m_rootNode = 0;
    private Nodes m_nodes;
    private Links m_links;

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE CLASSES
    ////////////////////////////////////////////////////////////////////////

    private static class Nodes
    {
	public Nodes(int numNodes)
	{
	    id = new int[numNodes];
	    radius = new double[numNodes];
	    x = new double[numNodes];
	    y = new double[numNodes];
	    z = new double[numNodes];
	    layoutX = new double[numNodes];
	    layoutY = new double[numNodes];
	    layoutZ = new double[numNodes];
	    layoutW = new double[numNodes];
	    parent = new int[numNodes];

	    // The automatic initialization of these arrays to zero is
	    // important in giving consistent values for nodes without
	    // child or non-tree links (e.g., when startChildLinks() et al.
	    // are not called for them).
	    treeLinks = new int[numNodes];
	    nontreeLinks = new int[numNodes];
	    linksEnd = new int[numNodes];

	    color = new int[numNodes];
	}

	////////////////////////////////////////////////////////////////////
	// ESSENTIAL NODE ATTRIBUTES
	////////////////////////////////////////////////////////////////////

	// The ID of the node in the backing org.caida.libsea.Graph.
	// This mapping is necessary since the IDs in the backing graph
	// need not form a contiguous block starting at zero.
	public int[] id;

	// The radius of the nodes as determined solely by their position.
	// This is inversely proportional to the distance from the origin to
	// a node as determined with the hyperbolic metric.
	public double[] radius;

	// The coordinates of nodes after hyperbolic view transformation.
	public double[] x;
	public double[] y;
	public double[] z;

	// The coordinates of nodes in initial layout.
	public double[] layoutX;
	public double[] layoutY;
	public double[] layoutZ;
	public double[] layoutW;

	// The parent value of a node gives the index of the link that
	// connects the parent to that node.  Hence the source of the parent
	// link gives the parent node and the destination gives the child node.
	public int[] parent;

	// The following arrays give indices into the array of links where
	// the links of a node appear.  All the links of a node appear in
	// a continguous block, with all child links grouped at the beginning
	// and all non-tree links at the end.  For a node i, the values have
	// the following relationship:
	//
	//  0 <= treeLinks[i] <= nontreeLinks[i] <= linksEnd[i] <= m_numLinks
	//
	// Additionally,
	//
	//   nontreeLinks[i] - treeLinks[i] gives the number of child links,
	//   linksEnd[i] - nontreeLinks[i] gives the number of non-tree links,
	// and linksEnd[i] - treeLinks[i] gives the total number of links.
	//
	// If a node has no links, then all the values will equal some
	// arbitrary value.
	public int[] treeLinks;
	public int[] nontreeLinks;
	public int[] linksEnd;

	////////////////////////////////////////////////////////////////////
	// INESSENTIAL NODE ATTRIBUTES
	////////////////////////////////////////////////////////////////////

	// Color in packed RGB format (R, G, and B in the lower three octets).
	public int[] color;

	// Whether a node should NOT be drawn.
	// This will be null if all nodes should be visible.
	// We use the inverted logic because BitSet is created with all its
	// bits cleared.
	public BitSet isHidden;
    }

    public static class Links
    {
	public Links(int numLinks)
	{
	    id = new int[numLinks];
	    source = new int[numLinks];
	    destination = new int[numLinks];
	    isTreeLink = new BitSet(numLinks);
	    color = new int[numLinks];
	}

	public int nextIndex = 0;

	////////////////////////////////////////////////////////////////////
	// ESSENTIAL LINK ATTRIBUTES
	////////////////////////////////////////////////////////////////////

	// The ID of the link in the backing org.caida.libsea.Graph.
	// This mapping is necessary since the IDs in the backing graph
	// need not form a contiguous block starting at zero.
	public int[] id;

	// The indices of nodes.
	public int[] source;
	public int[] destination;

	// Whether a link is a tree link or a nontree link.
	public BitSet isTreeLink;

	////////////////////////////////////////////////////////////////////
	// INESSENTIAL LINK ATTRIBUTES
	////////////////////////////////////////////////////////////////////

	// Color in packed RGB format (R, G, and B in the lower three octets).
	public int[] color;

	// Whether a link should NOT be drawn.
	// This will be null if all links should be visible.
	// We use the inverted logic because BitSet is created with all its
	// bits cleared.
	public BitSet isHidden;
    }

    ////////////////////////////////////////////////////////////////////////
    // TEST METHODS
    ////////////////////////////////////////////////////////////////////////

    // Place nodes with invalid layout coordinates at the origin.
    public void sanitizeLayoutCoordinates()
    {
	Point4d p = new Point4d();
	for (int i = 0; i < m_numNodes; i++)
	{
	    getNodeLayoutCoordinates(i, p);
	    if (!H3Math.isFinite(p))
	    {
		setNodeLayoutCoordinates(i, 0.0, 0.0, 0.0, 1.0);
	    }
	}
    }

    // Returns the number of nodes with valid (not NaN or infinite) layout
    // coordinates.
    public int checkLayoutCoordinates()
    {
	int numXNaNs = 0;
	int numYNaNs = 0;
	int numZNaNs = 0;
	int numWNaNs = 0;

	int numXInfs = 0;
	int numYInfs = 0;
	int numZInfs = 0;
	int numWInfs = 0;

	int retval = 0;

	Point4d p = new Point4d();
	for (int i = 0; i < m_numNodes; i++)
	{
	    getNodeLayoutCoordinates(i, p);
	    if (H3Math.isFinite(p))
	    {
		++retval;
	    }

	    numXNaNs += (Double.isNaN(p.x) ? 1 : 0);
	    numYNaNs += (Double.isNaN(p.y) ? 1 : 0);
	    numZNaNs += (Double.isNaN(p.z) ? 1 : 0);
	    numWNaNs += (Double.isNaN(p.w) ? 1 : 0);

	    numXInfs += (Double.isInfinite(p.x) ? 1 : 0);
	    numYInfs += (Double.isInfinite(p.y) ? 1 : 0);
	    numZInfs += (Double.isInfinite(p.z) ? 1 : 0);
	    numWInfs += (Double.isInfinite(p.w) ? 1 : 0);
	}

	System.out.println("numXNaNs = " + numXNaNs);
	System.out.println("numYNaNs = " + numYNaNs);
	System.out.println("numZNaNs = " + numZNaNs);
	System.out.println("numWNaNs = " + numWNaNs);

	System.out.println("numXInfs = " + numXInfs);
	System.out.println("numYInfs = " + numYInfs);
	System.out.println("numZInfs = " + numZInfs);
	System.out.println("numWInfs = " + numWInfs);

	return retval;
    }

    ////////////////////////////////////////////////////////////////////////

    public void checkTreeReachability()
    {
	checkTreeReachability(0);
    }

    public void checkTreeReachability(int node)
    {
	BitSet visited = new BitSet();
	int numReachable = checkReachability(visited, node);
	if (numReachable == m_numNodes)
	{
	    String msg =
		"PASSED: All nodes reachable in tree from node " + node;
	    System.out.println(msg);
	}
	else
	{
	    String msg = "FAILED: Only " + numReachable
		+ " nodes of " + m_numNodes
		+ " reachable in tree from node " + node;
	    System.out.println(msg);

	    System.out.println("Unvisited nodes:");
	    for (int i = 0; i < m_numNodes; i++)
	    {
		if (!visited.get(i))
		{
		    System.out.println("\t" + i);
		}
	    }

	    dumpForTesting();
	    dumpForTesting2();
	}
    }

    private int checkReachability(BitSet visited, int node)
    {
	int retval = 1;

	if (visited.get(node))
	{
	    String msg = "ERROR: Encountered cycle in tree at node " + node;
	    System.out.println(msg);
	    return 0;
	}
	visited.set(node);

	int treeLinks = m_nodes.treeLinks[node];
	int nontreeLinks = m_nodes.nontreeLinks[node];

	while (treeLinks < nontreeLinks)
	{
	    int child = m_links.destination[treeLinks++];
	    retval += checkReachability(visited, child);
	}

	return retval;
    }

    public void dumpForTesting()
    {
	System.out.println();
	System.out.println(this + ":");
	System.out.println("\tnumNodes: " + m_numNodes);
	System.out.println("\tnumLinks: " + m_numLinks);
	System.out.println("\tnumTreeLinks: " + m_numTreeLinks);
	System.out.println("\tnumNontreeLinks: " + m_numNontreeLinks);
	System.out.println("\tnextIndex: " + m_links.nextIndex);
	
	System.out.println("\nParent:\n");
	for (int i = 0; i < m_nodes.parent.length; i++)
	{
	    if (i == m_numNodes)
	    {
		System.out.println("- - - - - - - - - - - - - - - - - - - -");
	    }
	    System.out.println(i + " => " + m_nodes.parent[i]);
	}

	System.out.println("\nLinksStart:\n");
	for (int i = 0; i < m_nodes.treeLinks.length; i++)
	{
	    if (i == m_numNodes)
	    {
		System.out.println("- - - - - - - - - - - - - - - - - - - -");
	    }
	    System.out.println(i + " => " + m_nodes.treeLinks[i]);
	}

	System.out.println("\nNontreeLinksStart:\n");
	for (int i = 0; i < m_nodes.nontreeLinks.length; i++)
	{
	    if (i == m_numNodes)
	    {
		System.out.println("- - - - - - - - - - - - - - - - - - - -");
	    }
	    System.out.println(i + " => " + m_nodes.nontreeLinks[i]);
	}

	System.out.println("\nLinksEnd:\n");
	for (int i = 0; i < m_nodes.linksEnd.length; i++)
	{
	    if (i == m_numNodes)
	    {
		System.out.println("- - - - - - - - - - - - - - - - - - - -");
	    }
	    System.out.println(i + " => " + m_nodes.linksEnd[i]);
	}
    }

    public void dumpForTesting2()
    {
	System.out.println();
	System.out.println(this + ":");
	System.out.println("\tnumNodes: " + m_numNodes);
	System.out.println("\tnumLinks: " + m_numLinks);
	System.out.println("\tnumTreeLinks: " + m_numTreeLinks);
	System.out.println("\tnumNontreeLinks: " + m_numNontreeLinks);
	System.out.println("\tnextIndex: " + m_links.nextIndex);
	
	for (int i = 0; i < m_numNodes; i++)
	{
	    System.out.println("Node " + i + ":");
	    System.out.println("\tparent link: " + m_nodes.parent[i]);
	    System.out.println("\tparent node: "
			       + (m_nodes.parent[i] >= 0
				  ? m_links.source[m_nodes.parent[i]] : -1));

	    int treeLinks = m_nodes.treeLinks[i];
	    int nontreeLinks = m_nodes.nontreeLinks[i];
	    int linksEnd = m_nodes.linksEnd[i];

	    System.out.println("\ttreeLinks: " + treeLinks);
	    System.out.println("\tnontreeLinks: " + nontreeLinks);
	    System.out.println("\tlinksEnd: " + linksEnd);

	    for (int j = treeLinks; j < linksEnd; j++)
	    {
		if (j == nontreeLinks)
		{
		    System.out.println("\t\t- - - - - - - - - - - - - - - -");
		}

		System.out.println("\t\t" + j + ": "
				   + m_links.source[j] + " => "
				   + m_links.destination[j]);
	    }
	}
    }
}
