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

import java.io.*;
import java.util.*;
import javax.vecmath.*;
import org.caida.libsea.GraphFileLexer;
import org.caida.libsea.GraphFileParser;
import org.caida.libsea.GraphFactory;
import org.caida.libsea.GraphBuilder;
import org.caida.libsea.Graph;
import org.caida.libsea.*;

public class H3GraphLoader
{
    ////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ////////////////////////////////////////////////////////////////////////

    public H3GraphLoader(Reader reader)
    {
	m_reader = reader;
	m_lexer = new StreamTokenizer(new BufferedReader(reader));
	m_lexer.resetSyntax();
	m_lexer.commentChar('#');
	m_lexer.eolIsSignificant(true);
	m_lexer.lowerCaseMode(true);
	m_lexer.parseNumbers();
	m_lexer.whitespaceChars(0, 32);
    }

    ////////////////////////////////////////////////////////////////////////
    // PUBLIC METHODS
    ////////////////////////////////////////////////////////////////////////

    public H3Graph load()
    {
	H3Graph retval = null;

	long startTime = 0;
	if (DEBUG_PRINT)
	{
	    startTime = System.currentTimeMillis();
	    System.out.println("load.begin[" + startTime +"]");
	}

	final int format = 2;
	if (format == 0)
	{
	    retval = loadOldFormat();
	}
	else if (format == 1)
	{
	    retval = loadCanonicalFormat();
	}
	else if (format == 2)
	{
	    retval = loadNewFormat();
	}
	else
	{
	    throw new RuntimeException();
	}

	if (DEBUG_PRINT)
	{
	    long stopTime = System.currentTimeMillis();
	    long duration = stopTime - startTime;
	    System.out.println("load.end[" + stopTime + "]");
	    System.out.println("load.time[" + duration + "]");
	}

	return retval;
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE METHODS
    ////////////////////////////////////////////////////////////////////////

    private H3Graph loadNewFormat()
    {
	H3Graph retval = null;

	if (DEBUG_MEMORY) { m_memoryUsage.startGathering(); }

	try
	{
	    GraphBuilder builder = GraphFactory.makeImmutableGraph();

	    GraphFileLexer lexer = new GraphFileLexer(m_reader);
	    GraphFileParser parser = new GraphFileParser(lexer);
	    parser.file(builder);

	    Graph graph = builder.endConstruction();

	    if (DEBUG_MEMORY) { m_memoryUsage.gatherAfterBufferLoaded(); }

	    retval = new UncachedGraph(graph);

	    if (DEBUG_MEMORY) { m_memoryUsage.gatherAtPeak(); }
	}
	catch (antlr.ANTLRException e)
	{
	    System.err.println("Failed to load graph: " + e);
	}
	catch (InvalidGraphDataException e)
	{
	    System.err.println("Could not create graph from file: " + e);
	}

	if (DEBUG_MEMORY)
	{
	    m_memoryUsage.gatherAtFinal();
	    m_memoryUsage.printUsage();
	}
	
	return retval;
    }

    ///////////////////////////////////////////////////////////////////////

    private H3Graph loadCanonicalFormat()
    {
	H3Graph retval = null;

	if (DEBUG_MEMORY) { m_memoryUsage.startGathering(); }

	CanonicalGraphLoader loader = new CanonicalGraphLoader(m_reader);
	Graph graph = loader.load();

	if (DEBUG_MEMORY) { m_memoryUsage.gatherAfterBufferLoaded(); }

	if (graph != null)
	{
	    try
	    {
		retval = new UncachedGraph(graph);
		if (DEBUG_MEMORY) { m_memoryUsage.gatherAtPeak(); }
	    }
	    catch (InvalidGraphDataException e)
	    {
		System.err.println("Could not create graph from file: " + e);
	    }
	}

	loader = null;

	if (DEBUG_MEMORY)
	{
	    m_memoryUsage.gatherAtFinal();
	    m_memoryUsage.printUsage();
	}

	return retval;
    }

    ///////////////////////////////////////////////////////////////////////

    private H3Graph loadOldFormat()
    {
	if (DEBUG_MEMORY) { m_memoryUsage.startGathering(); }

	H3Graph retval = null;

	m_buffer = new H3GraphBuffer();
	m_nodes = new HashMap();

	try
	{
	    boolean parsedNodes = false;
	    boolean parsedLinks = false;

	    while (m_lexer.nextToken() != StreamTokenizer.TT_EOF)
	    {
		if (m_lexer.ttype == 'n' || m_lexer.ttype == 's')
		{
		    if (m_lexer.ttype == 's' && parsedNodes)
		    {
			throw new ParseException("The root node must "
						 + "appear first.");
		    }

		    if (parsedLinks)
		    {
			throw new ParseException("All nodes must precede "
						 + "links.");
		    }

		    parsedNodes = true;
		    parseNode();
		}
		else if (m_lexer.ttype == 't' || m_lexer.ttype == 'l')
		{
		    parsedLinks = true;
		    parseLink();
		}
		else
		{
		    throw new ParseException("Expected line type "
					     + "(n, s, t, or l); encountered "
					     + findTokenName(m_lexer.ttype));
		}
	    }

	    if (DEBUG_MEMORY) { m_memoryUsage.gatherAfterBufferLoaded(); }

	    if (DEBUG_PRINT_TRACE)
	    {
		m_buffer.dumpForTesting();
	    }

	    retval = m_buffer.toGraph();

	    if (DEBUG_MEMORY) { m_memoryUsage.gatherAtPeak(); }
	}
	catch (IOException e)
	{
	    System.err.println("Error while loading graph: " + e);
	}
	catch (ParseException e)
	{
	    System.err.println("Syntax error near line "
			       + m_lexer.lineno() + ": "
			       + e.toString());
	}

	m_reader = null;
	m_lexer = null;
	m_buffer = null;
	m_nodes = null;

	if (DEBUG_MEMORY)
	{
	    m_memoryUsage.gatherAtFinal();
	    m_memoryUsage.printUsage();
	}

	return retval;
    }

    private void parseNode()
	throws ParseException, IOException
    {
	// 's'|'n' <int> <double>

	// NOTE: You might think at first sight that the following code
	//       does nothing special when encountering the 's' line, which
	//       specifies the root node.  Specifically, you may be puzzled
	//       at how information about the root node is communicated to
	//       H3GraphBuffer.  It turns out that it just happens
	//       automatically as a consequence of the mapping that is done
	//       between the external and internal IDs.  This mapping is
	//       necessary in general since the external IDs are allowed to
	//       start at any value and to have gaps.  In this mapping, the
	//       root node will always map to the internal ID of zero, since
	//       the 's' line must be the first node line in a file.  Hence,
	//       the expectation of H3GraphBuffer is met without additional
	//       effort.

	boolean isRoot = (m_lexer.ttype == 's');

	match(StreamTokenizer.TT_NUMBER);

	Integer id = new Integer((int)m_lexer.nval);
	Integer node = new Integer(m_buffer.addNode());

	if (DEBUG_PRINT_TRACE)
	{
	    System.out.println("Node mapping: ID(" + id + ") => Node("
			       + node + ")");
	}

	if (m_nodes.put(id, node) != null)
	{
	    throw new ParseException("Node " + id + " is multiply defined.");
	}

	match(StreamTokenizer.TT_NUMBER);
	int color = computeHotToColdColor(m_lexer.nval);

	match(StreamTokenizer.TT_EOL);
    }

    private void parseLink()
	throws ParseException, IOException
    {
	// 't'|'l' <double> <int> <int>

	boolean isTreeLink = (m_lexer.ttype == 't');

	match(StreamTokenizer.TT_NUMBER);
	int color = computeHotToColdColor(m_lexer.nval);

	match(StreamTokenizer.TT_NUMBER);

	Integer sourceID = new Integer((int)m_lexer.nval);
	Integer sourceNode = (Integer)m_nodes.get(sourceID);
	if (sourceNode == null)
	{
	    throw new ParseException("Unknown link source " + sourceID + ".");
	}

	match(StreamTokenizer.TT_NUMBER);

	Integer destinationID = new Integer((int)m_lexer.nval);
	Integer destinationNode = (Integer)m_nodes.get(destinationID);
	if (destinationNode == null)
	{
	    throw new ParseException("Unknown link destination "
				     + destinationID + ".");
	}

	if (DEBUG_PRINT_TRACE)
	{
	    System.out.println("Link: src ID(" + sourceID + ") => dst ID("
			       + destinationID + ")");

	    System.out.println("Link: src Node(" + sourceNode
			       + ") => dst Node(" + destinationNode + ")");
	}

	if (isTreeLink)
	{
	    m_buffer.addChild(sourceNode.intValue(),
			      destinationNode.intValue());
	}
	else
	{
	    m_buffer.addNontreeLink(sourceNode.intValue(),
				    destinationNode.intValue());
	}

	match(StreamTokenizer.TT_EOL);
    }

    private void match(int ttype)
	throws ParseException, IOException
    {
	if (m_lexer.nextToken() != ttype)
	{
	    throw new ParseException("Mismatched tokens: expected "
				     + findTokenName(ttype)
				     + ", encountered "
				     + findTokenName(m_lexer.ttype));
	}
    }

    private String findTokenName(int ttype)
    {
	if (ttype == StreamTokenizer.TT_EOF)
	{
	    return "TT_EOF";
	}
	else if (ttype == StreamTokenizer.TT_EOL)
	{
	    return "TT_EOL";
	}
	else if (ttype == StreamTokenizer.TT_NUMBER)
	{
	    return "TT_NUMBER";
	}
	else if (ttype == StreamTokenizer.TT_WORD)
	{
	    return "TT_WORD";
	}
	else
	{
	    if (Character.isISOControl((char)ttype))
	    {
		return "0x" + Integer.toHexString(ttype);
	    }
	    else
	    {
		return "'" + (char)ttype + "'";
	    }
	}
    }

    private int computeHotToColdColor(double x)
    {
	// Code adapted from
        // <http://www.swin.edu.au/astronomy/pbourke/colour/colourramp>,
	// "Colour Ramping for Data Visualization", by Paul Bourke, July 1996.

	int r = 255;
	int g = 255;
	int b = 255;

	if (x < 0.0)
	{
	    x = 0.0;
	}
	else if (x > 1.0)
	{
	    x = 1.0;
	}

	if (x < 0.25)
	{
	    r = 0;
	    g = (int)(255.0 * 4.0 * x);
	}
	else if (x < 0.5)
	{
	    r = 0;
	    b = (int)(255.0 + 255.0 * 4.0 * (0.25 - x));
	}
	else if (x < 0.75)
	{
	    r = (int)(255.0 * 4.0 * (x - 0.5));
	    b = 0;
	}
	else
	{
	    g = (int)(255.0 + 255.0 * 4.0 * (0.75 - x));
	    b = 0;
	}

	return (r << 16) | (g << 8) | b;
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE FIELDS
    ////////////////////////////////////////////////////////////////////////

    private static final boolean DEBUG_PRINT = true;
    private static final boolean DEBUG_PRINT_TRACE = false;
    private static final boolean DEBUG_MEMORY = true;

    private Reader m_reader;
    private StreamTokenizer m_lexer;
    private H3GraphBuffer m_buffer;
    private Map m_nodes;
    private MemoryUsage m_memoryUsage = new MemoryUsage();

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE CLASSES
    ////////////////////////////////////////////////////////////////////////

    private static class ParseException extends Exception
    {
	public ParseException()
	{
	}

	public ParseException(String s)
	{
	    super(s);
	}
    }

    ///////////////////////////////////////////////////////////////////////

    private static class InvalidGraphDataException extends Exception
    {
	public InvalidGraphDataException()
	{
	    super();
	}

	public InvalidGraphDataException(String s)
	{
	    super(s);
	}
    }

    ///////////////////////////////////////////////////////////////////////

    private static class MemoryUsage
    {
	public MemoryUsage()
	{
	}

	public void startGathering()
	{
	    System.gc();
	    m_baseTotalMemory = Runtime.getRuntime().totalMemory();
	    m_baseFreeMemory = Runtime.getRuntime().freeMemory();

	    m_bufferTotalMemory = 0;
	    m_bufferFreeMemory = 0;
	    m_peakTotalMemory = 0;
	    m_peakFreeMemory = 0;
	    m_finalTotalMemory = 0;
	    m_finalFreeMemory = 0;
	}

	public void gatherAfterBufferLoaded()
	{
	    System.gc();
	    m_bufferTotalMemory = Runtime.getRuntime().totalMemory();
	    m_bufferFreeMemory = Runtime.getRuntime().freeMemory();
	}

	public void gatherAtPeak()
	{
	    System.gc();
	    m_peakTotalMemory = Runtime.getRuntime().totalMemory();
	    m_peakFreeMemory = Runtime.getRuntime().freeMemory();
	}

	public void gatherAtFinal()
	{
	    System.gc();
	    m_finalTotalMemory = Runtime.getRuntime().totalMemory();
	    m_finalFreeMemory = Runtime.getRuntime().freeMemory();
	}

	public void printUsage()
	{
	    long baseUsed = m_baseTotalMemory - m_baseFreeMemory;
	    long bufferUsed = m_bufferTotalMemory - m_bufferFreeMemory;
	    long peakUsed = m_peakTotalMemory - m_peakFreeMemory;
	    long finalUsed = m_finalTotalMemory - m_finalFreeMemory;

	    System.out.println("===========================================");
	    System.out.println("baseTotalMemory = " + M(m_baseTotalMemory));
	    System.out.println("baseFreeMemory = " + M(m_baseFreeMemory));
	    System.out.println("baseUsed = " + M(baseUsed));
	    System.out.println("bufferTotalMemory = " +M(m_bufferTotalMemory));
	    System.out.println("bufferFreeMemory = " + M(m_bufferFreeMemory));
	    System.out.println("bufferUsed = " + M(bufferUsed));
	    System.out.println("peakTotalMemory = " + M(m_peakTotalMemory));
	    System.out.println("peakFreeMemory = " + M(m_peakFreeMemory));
	    System.out.println("peakUsed = " + M(peakUsed));
	    System.out.println("finalTotalMemory = " + M(m_finalTotalMemory));
	    System.out.println("finalFreeMemory = " + M(m_finalFreeMemory));
	    System.out.println("finalUsed = " + M(finalUsed));
	    System.out.println();
	    System.out.println("bufferUsed - baseUsed = "
			       + M(bufferUsed - baseUsed));
	    System.out.println("peakUsed - baseUsed = "
			       + M(peakUsed - baseUsed));
	    System.out.println("peakUsed - bufferUsed = "
			       + M(peakUsed - bufferUsed));
	    System.out.println("finalUsed - baseUsed = "
			       + M(finalUsed - baseUsed));
	    System.out.println("finalUsed - peakUsed = "
			       + M(finalUsed - peakUsed));
	    System.out.println("===========================================");
	}

	private String M(long n)
	{
	    long x = n / 100000;
	    long mega = x / 10;
	    long kilo = Math.abs(x % 10);
	    return "" + n + " (" + mega + "." + kilo + "e6)";
	}

	// At start.
	private long m_baseTotalMemory;
	private long m_baseFreeMemory;

	// After loading into graph buffer.
	private long m_bufferTotalMemory;
	private long m_bufferFreeMemory;

	// After populating graph, but before freeing the graph buffer.
	private long m_peakTotalMemory;  
	private long m_peakFreeMemory;

	// After freeing graph buffer, and leaving only graph.
	private long m_finalTotalMemory;
	private long m_finalFreeMemory;
    }

    ////////////////////////////////////////////////////////////////////////

    private static class UncachedGraph
	implements H3Graph
    {
	////////////////////////////////////////////////////////////////////
	// CONSTRUCTORS
	////////////////////////////////////////////////////////////////////

	public UncachedGraph(Graph graph)
	    throws InvalidGraphDataException
	{
	    m_graph = graph;

	    int numNodes = graph.getNumNodes();
	    int numLinks = graph.getNumLinks();

	    m_nodes = new Nodes(numNodes);
	    m_links = new Links(numLinks);

	    findSpanningTreeQualifierAttributes(graph);
	    m_rootNode = findSpanningTreeRootNode(graph, m_rootAttribute);
	    cacheLinks(graph, m_treeLinkAttribute);
	    createAttributes(graph);
	}

	///////////////////////////////////////////////////////////////////
	// PUBLIC ACCESSOR METHODS
	///////////////////////////////////////////////////////////////////

	public int getNumNodes()
	{
	    return m_graph.getNumNodes();
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
	    return m_graph.getNumLinks();
	}

	public int getRootNode()
	{
	    return m_rootNode;
	}

	public double getNodeRadius(int node)
	{
	    try
	    {
		return m_graph.getDoubleAttribute(ObjectType.NODE, node,
						  m_radiusAttribute);
	    }
	    catch (AttributeUnavailableException e)
	    {
		throw new RuntimeException("INTERNAL ERROR: " + e);
	    }
	}

	public void getNodeCoordinates(int node, Point3d point)
	{
	    try 
	    {
		m_graph.getDouble3Attribute(ObjectType.NODE, node,
					    m_coordinatesAttribute,
					    m_double3Temporary);
	    }
	    catch (AttributeUnavailableException e)
	    {
		throw new RuntimeException("INTERNAL ERROR: " + e);
	    }
	    point.x = m_double3Temporary[0];
	    point.y = m_double3Temporary[1];
	    point.z = m_double3Temporary[2];
	}

	public void getNodeCoordinates(int node, Point4d point)
	{
	    try
	    {
		m_graph.getDouble3Attribute(ObjectType.NODE, node,
					    m_coordinatesAttribute,
					    m_double3Temporary);
	    }
	    catch (AttributeUnavailableException e)
	    {
		throw new RuntimeException("INTERNAL ERROR: " + e);
	    }
	    point.x = m_double3Temporary[0];
	    point.y = m_double3Temporary[1];
	    point.z = m_double3Temporary[2];
	    point.w = 1.0;
	}

	public void getNodeLayoutCoordinates(int node, Point3d point)
	{
	    try
	    {
		m_graph.getDouble3Attribute(ObjectType.NODE, node,
					    m_layoutCoordinatesAttribute,
					    m_double3Temporary);
	    }
	    catch (AttributeUnavailableException e)
	    {
		throw new RuntimeException("INTERNAL ERROR: " + e);
	    }
	    double w = m_nodes.layoutW[node];
	    point.x = m_double3Temporary[0] / w;
	    point.y = m_double3Temporary[1] / w;
	    point.z = m_double3Temporary[2] / w;
	}

	public void getNodeLayoutCoordinates(int node, Point4d point)
	{
	    try
	    {
		m_graph.getDouble3Attribute(ObjectType.NODE, node,
					    m_layoutCoordinatesAttribute,
					    m_double3Temporary);
	    }
	    catch (AttributeUnavailableException e)
	    {
		throw new RuntimeException("INTERNAL ERROR: " + e);
	    }
	    point.x = m_double3Temporary[0];
	    point.y = m_double3Temporary[1];
	    point.z = m_double3Temporary[2];
	    point.w = m_nodes.layoutW[node] = m_nodes.layoutW[node];
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
	    try
	    {
		return m_graph.getIntegerAttribute(ObjectType.NODE, node,
						   m_colorAttribute);
	    }
	    catch (AttributeUnavailableException e)
	    {
		throw new RuntimeException("INTERNAL ERROR: " + e);
	    }
	}

	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public boolean checkNodeVisited(int node, int iteration)
	{
	    return m_nodes.visited[node] == iteration;
	}

	public boolean markNodeVisited(int node, int iteration)
	{
	    boolean retval = (m_nodes.visited[node] == iteration);
	    if (!retval)
	    {
		m_nodes.visited[node] = iteration;
	    }
	    return retval;
	}

	//=================================================================

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
	    try
	    {
		return m_graph.getIntegerAttribute(ObjectType.LINK, link,
						   m_colorAttribute);
	    }
	    catch (AttributeUnavailableException e)
	    {
		throw new RuntimeException("INTERNAL ERROR: " + e);
	    }
	}

	////////////////////////////////////////////////////////////////////
	// PUBLIC MUTATOR METHODS
	////////////////////////////////////////////////////////////////////

	public void transformNodes(Matrix4d t)
	{
	    int numNodes = getNumNodes();
	    Point4d p = new Point4d();
	    for (int i = 0; i < numNodes; i++)
	    {
		getNodeLayoutCoordinates(i, p);
		t.transform(p);
		setNodeCoordinates(i, p);
	    }
	}

	public void setRootNode(int node)
	{
	    m_rootNode = node;
	}

	public void setNodeRadius(int node, double radius)
	{
	    m_graph.setDoubleAttribute
		(ObjectType.NODE, node, m_radiusAttribute, radius);
	}

	public void setNodeCoordinates(int node, double x, double y, double z)
	{
	    m_graph.setDouble3Attribute
		(ObjectType.NODE, node, m_coordinatesAttribute, x, y, z);
	}

	public void setNodeCoordinates(int node, Point3d p)
	{
	    m_graph.setDouble3Attribute
		(ObjectType.NODE, node, m_coordinatesAttribute, p.x, p.y, p.z);
	}

	public void setNodeCoordinates(int node, Point4d p)
	{
	    m_graph.setDouble3Attribute
		(ObjectType.NODE, node, m_coordinatesAttribute,
		 p.x / p.w, p.y / p.w, p.z / p.w);
	}

	public void setNodeLayoutCoordinates(int node, double x, double y,
					     double z, double w)
	{
	    m_graph.setDouble3Attribute(ObjectType.NODE, node,
					m_layoutCoordinatesAttribute, x, y, z);
	    m_nodes.layoutW[node] = w;
	}

	public void setNodeLayoutCoordinates(int node, Point3d p)
	{
	    m_graph.setDouble3Attribute
		(ObjectType.NODE, node, m_layoutCoordinatesAttribute,
		 p.x, p.y, p.z);
	    m_nodes.layoutW[node] = 1.0;
	}

	public void setNodeLayoutCoordinates(int node, Point4d p)
	{
	    m_graph.setDouble3Attribute
		(ObjectType.NODE, node, m_layoutCoordinatesAttribute,
		 p.x, p.y, p.z);
	    m_nodes.layoutW[node] = p.w;
	}

	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public void setNodeColor(int node, int color)
	{
	    m_graph.setIntegerAttribute(ObjectType.NODE, node,
					m_colorAttribute, color);
	}
    
	public void setNodeColor(int node, byte r, byte g, byte b)
	{
	    int color = (r << 16) | (g << 8) | b;
	    setNodeColor(node, color);
	}

	public void setNodeDefaultColor(int color)
	{
	    int numNodes = getNumNodes();
	    for (int i = 0; i < numNodes; i++)
	    {
		setNodeColor(i, color);
	    }
	}

	public void setNodeDefaultColor(byte r, byte g, byte b)
	{
	    int color = (r << 16) | (g << 8) | b;
	    setNodeDefaultColor(color);
	}

	public void setLinkColor(int link, int color)
	{
	    m_graph.setIntegerAttribute(ObjectType.LINK, link,
					m_colorAttribute, color);
	}

	public void setLinkColor(int link, byte r, byte g, byte b)
	{
	    int color = (r << 16) | (g << 8) | b;
	    setLinkColor(link, color);
	}

	public void setLinkDefaultColor(int color)
	{
	    int numLinks = getTotalNumLinks();
	    for (int i = 0; i < numLinks; i++)
	    {
		setLinkColor(i, color);
	    }
	}

	public void setLinkDefaultColor(byte r, byte g, byte b)
	{
	    int color = (r << 16) | (g << 8) | b;
	    setLinkDefaultColor(color);
	}

	///////////////////////////////////////////////////////////////////////
	// PRIVATE METHODS
	///////////////////////////////////////////////////////////////////////

	private void createAttributes(Graph graph)
	    throws InvalidGraphDataException
	{
	    AttributeDefinitionIterator iterator =
		graph.getAttributeDefinitions();
	    try
	    {
		iterator.addAttributeDefinition
		    (RADIUS_ATTRIBUTE, ValueType.DOUBLE, null, null, true);
		m_radiusAttribute = iterator.getID();

		iterator.addAttributeDefinition
		    (COORDINATES_ATTRIBUTE, ValueType.DOUBLE3,
		     null, null, true);
		m_coordinatesAttribute = iterator.getID();

		iterator.addAttributeDefinition
		    (LAYOUT_COORDINATES_ATTRIBUTE, ValueType.DOUBLE3,
		     null, null, true);
		m_layoutCoordinatesAttribute = iterator.getID();

		iterator.addAttributeDefinition
		    (COLOR_ATTRIBUTE, ValueType.INTEGER, null, null, true);
		m_colorAttribute = iterator.getID();
	    }
	    catch (DuplicateObjectException e)
	    {
		String msg = "Could not add needed internal attributes to "
		    + "graph--some names already exist: " + e;
		throw new InvalidGraphDataException(msg);
	    }
	}

	///////////////////////////////////////////////////////////////////////

	private void cacheLinks(Graph graph, int treeLinkAttribute)
	{
	    BitSet treeLinksMap = createTreeLinksMap(graph, treeLinkAttribute);

	    NodeIterator nodeIterator = graph.getNodes();
	    while (!nodeIterator.atEnd())
	    {
		int node = nodeIterator.getObjectID();
		startChildLinks(node);
		{
		    LinkIterator linkIterator =
			nodeIterator.getOutgoingLinks();
		    while (!linkIterator.atEnd())
		    {
			int link = linkIterator.getObjectID();
			if (treeLinksMap.get(link))
			{
			    addChildLink(node, linkIterator.getDestination());
			}
			linkIterator.advance();
		    }
		}
		startNontreeLinks(node);
		{
		    LinkIterator linkIterator =
			nodeIterator.getOutgoingLinks();
		    while (!linkIterator.atEnd())
		    {
			int link = linkIterator.getObjectID();
			if (!treeLinksMap.get(link))
			{
			    addNontreeLink
				(node, linkIterator.getDestination());
			}
			linkIterator.advance();
		    }
		}
		endNodeLinks(node);

		nodeIterator.advance();
	    }
	}

	private BitSet createTreeLinksMap(Graph graph, int treeLinkAttribute)
	{
	    BitSet retval = new BitSet(graph.getNumLinks());

	    AttributesByAttributeIterator iterator =
		graph.getAttributeDefinition(treeLinkAttribute)
		.getLinkAttributes();
	    while (!iterator.atEnd())
	    {
		if (iterator.getAttributeValues().getBooleanValue())
		{
		    retval.set(iterator.getObjectID());
		}
		iterator.advance();
	    }

	    return retval;
	}
    
	///////////////////////////////////////////////////////////////////////

	private int findSpanningTreeRootNode(Graph graph, int attribute)
	    throws InvalidGraphDataException
	{
	    AttributesByAttributeIterator iterator =
		graph.getAttributeDefinition(attribute).getNodeAttributes();
	    while (!iterator.atEnd())
	    {
		if (iterator.getAttributeValues().getBooleanValue())
		{
		    return iterator.getObjectID();
		}
		iterator.advance();
	    }

	    String msg = "no root node found for spanning tree";
	    throw new InvalidGraphDataException(msg);
	}

	private void findSpanningTreeQualifierAttributes(Graph graph)
	    throws InvalidGraphDataException
	{
	    QualifierIterator qualifierIterator =
		graph.getQualifiersByType(SPANNING_TREE_QUALIFIER);
	    if (qualifierIterator.atEnd())
	    {
		String msg = "no qualifier of type `"
		    + SPANNING_TREE_QUALIFIER + "' found";
		throw new InvalidGraphDataException(msg);
	    }

	    System.out.println("Using qualifier `"
			       + qualifierIterator.getName() + "'...");

	    boolean foundRootAttribute = false;
	    boolean foundTreeLinkAttribute = false;

	    QualifierAttributeIterator qualifierAttributeIterator =
		qualifierIterator.getAttributes();
	    while (!qualifierAttributeIterator.atEnd())
	    {
		if (qualifierAttributeIterator.getName()
		    .equals(ROOT_ATTRIBUTE))
		{
		    foundRootAttribute = true;
		    m_rootAttribute =
			qualifierAttributeIterator.getAttributeID();
		    checkAttributeType
			(graph, SPANNING_TREE_QUALIFIER, ROOT_ATTRIBUTE,
			 m_rootAttribute, ValueType.BOOLEAN);
		}
		else if (qualifierAttributeIterator.getName()
			 .equals(TREE_LINK_ATTRIBUTE))
		{
		    foundTreeLinkAttribute = true;
		    m_treeLinkAttribute =
			qualifierAttributeIterator.getAttributeID();
		    checkAttributeType
			(graph, SPANNING_TREE_QUALIFIER, TREE_LINK_ATTRIBUTE,
			 m_treeLinkAttribute, ValueType.BOOLEAN);
		}

		qualifierAttributeIterator.advance();
	    }

	    if (!foundRootAttribute)
	    {
		String msg = "missing attribute `" + ROOT_ATTRIBUTE
		    + "' of qualifier type `" + SPANNING_TREE_QUALIFIER + "'";
		throw new InvalidGraphDataException(msg);
	    }

	    if (!foundTreeLinkAttribute)
	    {
		String msg = "missing attribute `" + TREE_LINK_ATTRIBUTE
		    + "' of qualifier type `" + SPANNING_TREE_QUALIFIER + "'";
		throw new InvalidGraphDataException(msg);
	    }
	}

	private void checkAttributeType
	    (Graph graph, String qualifierName, String attributeName,
	     int attribute, ValueType type)
	    throws InvalidGraphDataException
	{
	    AttributeDefinitionIterator iterator =
		graph.getAttributeDefinition(attribute);
	    if (iterator.getType() != type)
	    {
		String msg = "attribute `" + attributeName
		    + "' of qualifier type `" + qualifierName
		    + "' must have type " + type.getName()
		    + "; found " + iterator.getType().getName();
		throw new InvalidGraphDataException(msg);
	    }
	}

	// The following two functions, addNodeChild() and
	// addNodeNontreeLink(), must be called in a disciplined manner.
	// The sequence of calls to add the links of one node should never
	// interleave with the sequence of another node.  Additionally,
	// for a particular node, all child links must be added first,
	// followed by all non-tree links.
	// 
	// The required calling sequence for a node is as follows:
	//
	//    startChildLinks()
	//    addChildLink() ... addChildLink()  [zero or more]
	//    startNontreeLinks()
	//    addNontreeLink() ... addNontreeLink()  [zero or more]
	//    endNodeLinks()
	//
	// There need not be a sequence of these calls for nodes without
	// any links.

	private void startChildLinks(int node)
	{
	    m_nodes.treeLinks[node] = m_links.nextIndex;
	}

	private void addChildLink(int node, int child)
	{
	    ++m_numTreeLinks;

	    m_nodes.parent[child] = m_links.nextIndex;
	    m_links.source[m_links.nextIndex] = node;
	    m_links.destination[m_links.nextIndex] = child;
	    ++m_links.nextIndex;
	}

	private void startNontreeLinks(int node)
	{
	    m_nodes.nontreeLinks[node] = m_links.nextIndex;
	}

	private void addNontreeLink(int node, int target)
	{
	    ++m_numNontreeLinks;

	    m_links.source[m_links.nextIndex] = node;
	    m_links.destination[m_links.nextIndex] = target;
	    ++m_links.nextIndex;
	}

	private void endNodeLinks(int node)
	{
	    m_nodes.linksEnd[node] = m_links.nextIndex;
	}

	///////////////////////////////////////////////////////////////////
	// PRIVATE FIELDS
	///////////////////////////////////////////////////////////////////

	private static final String SPANNING_TREE_QUALIFIER = "spanning_tree";
	private static final String ROOT_ATTRIBUTE = "root";
	private static final String TREE_LINK_ATTRIBUTE = "tree_link";

	private static final String RADIUS_ATTRIBUTE =
	    "radius_org_caida_walrus";
	private static final String COORDINATES_ATTRIBUTE =
	    "coordinates_org_caida_walrus";
	private static final String LAYOUT_COORDINATES_ATTRIBUTE =
	    "layout_coordinates_org_caida_walrus";
	private static final String COLOR_ATTRIBUTE =
	    "color_org_caida_walrus";

	private Graph m_graph;
	private int m_numTreeLinks = 0;
	private int m_numNontreeLinks = 0;
	private int m_rootNode = 0;

	private int m_rootAttribute;               // boolean
	private int m_treeLinkAttribute;           // boolean

	private int m_radiusAttribute;             // double
	private int m_coordinatesAttribute;        // double3
	private int m_layoutCoordinatesAttribute;  // double3
	private int m_colorAttribute;              // integer

	private Nodes m_nodes;
	private Links m_links;

	private float[] m_float3Temporary = new float[3];
	private double[] m_double3Temporary = new double[3];

	///////////////////////////////////////////////////////////////////
	// PRIVATE CLASSES
	///////////////////////////////////////////////////////////////////

	private static class Nodes
	{
	    public Nodes(int numNodes)
	    {
		layoutW = new double[numNodes];

		parent = new int[numNodes];
		parent[0] = -1;  // The node at index zero is always the root.

		// The automatic initialization of these arrays to zero is
		// important in giving consistent values for nodes without
		// child or non-tree links (when startChildLinks() et al.
		// are not called for them).
		treeLinks = new int[numNodes];
		nontreeLinks = new int[numNodes];
		linksEnd = new int[numNodes];

		visited = new int[numNodes];
	    }

	    ////////////////////////////////////////////////////////////////
	    // ESSENTIAL NODE ATTRIBUTES
	    ////////////////////////////////////////////////////////////////

	    // The w component of the coordinates of nodes in the initial
	    // layout.  The x, y, and z components are stored in the double3
	    // attribute UncachedGraph.m_layoutCoordinatesAttribute.
	    public double[] layoutW;

	    // The parent value of a node gives the index of the link that
	    // connects the parent to that node.  Hence the source of the
	    // parent link gives the parent node and the destination gives
	    // the child node.
	    public int[] parent;

	    // The following arrays give indices into the array of links where
	    // the links of a node appear.  All the links of a node appear in
	    // a continguous block, with all child links grouped at the
	    // beginning and all non-tree links at the end.  For a node i,
	    // the values have
	    // the following relationship:
	    //
	    //  0 <= treeLinks[i] <= nontreeLinks[i]
	    //          <= linksEnd[i] <= m_numLinks
	    //
	    // Additionally,
	    //
	    //   (1) nontreeLinks[i] - treeLinks[i]
	    //          gives the number of child links,
	    //   (2) linksEnd[i] - nontreeLinks[i]
	    //          gives the number of non-tree links,
	    // and (3) linksEnd[i] - treeLinks[i]
	    //          gives the total number of links.
	    //
	    // If a node has no links, then all the values will equal some
	    // arbitrary value.
	    public int[] treeLinks;
	    public int[] nontreeLinks;
	    public int[] linksEnd;

	    // Whether a node has been visited in "traversal iteration" t > 0.
	    public int[] visited;
	}

	public static class Links
	{
	    public Links(int numLinks)
	    {
		source = new int[numLinks];
		destination = new int[numLinks];
	    }

	    public int nextIndex = 0;

	    ///////////////////////////////////////////////////////////////
	    // ESSENTIAL LINK ATTRIBUTES
	    ///////////////////////////////////////////////////////////////

	    // The indices of nodes.
	    public int[] source;
	    public int[] destination;
	}

	///////////////////////////////////////////////////////////////////
	// TEST METHODS
	///////////////////////////////////////////////////////////////////

	public void checkTreeReachability()
	{
	    checkTreeReachability(m_rootNode);
	}

	public void checkTreeReachability(int node)
	{
	    int numNodes = getNumNodes();
	    BitSet visited = new BitSet();
	    int numReachable = checkReachability(visited, node);
	    if (numReachable == numNodes)
	    {
		String msg =
		    "PASSED: All nodes reachable in tree from node " + node;
		System.out.println(msg);
	    }
	    else
	    {
		String msg = "FAILED: Only " + numReachable
		    + " nodes of " + numNodes
		    + " reachable in tree from node " + node;
		System.out.println(msg);

		System.out.println("Unvisited nodes:");
		for (int i = 0; i < numNodes; i++)
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
		String msg =
		    "ERROR: Encountered cycle in tree at node " + node;
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
	    int numNodes = getNumNodes();
	    int numLinks = getTotalNumLinks();

	    System.out.println();
	    System.out.println(this + ":");
	    System.out.println("\tnumNodes: " + numNodes);
	    System.out.println("\tnumLinks: " + numLinks);
	    System.out.println("\tnumTreeLinks: " + m_numTreeLinks);
	    System.out.println("\tnumNontreeLinks: " + m_numNontreeLinks);
	    System.out.println("\tnextIndex: " + m_links.nextIndex);
	
	    System.out.println("\nParent:\n");
	    for (int i = 0; i < m_nodes.parent.length; i++)
	    {
		if (i == numNodes)
		{
		    System.out.println
			("- - - - - - - - - - - - - - - - - - - -");
		}
		System.out.println(i + " => " + m_nodes.parent[i]);
	    }

	    System.out.println("\nLinksStart:\n");
	    for (int i = 0; i < m_nodes.treeLinks.length; i++)
	    {
		if (i == numNodes)
		{
		    System.out.println
			("- - - - - - - - - - - - - - - - - - - -");
		}
		System.out.println(i + " => " + m_nodes.treeLinks[i]);
	    }

	    System.out.println("\nNontreeLinksStart:\n");
	    for (int i = 0; i < m_nodes.nontreeLinks.length; i++)
	    {
		if (i == numNodes)
		{
		    System.out.println
			("- - - - - - - - - - - - - - - - - - - -");
		}
		System.out.println(i + " => " + m_nodes.nontreeLinks[i]);
	    }

	    System.out.println("\nLinksEnd:\n");
	    for (int i = 0; i < m_nodes.linksEnd.length; i++)
	    {
		if (i == numNodes)
		{
		    System.out.println
			("- - - - - - - - - - - - - - - - - - - -");
		}
		System.out.println(i + " => " + m_nodes.linksEnd[i]);
	    }
	}

	public void dumpForTesting2()
	{
	    int numNodes = getNumNodes();
	    int numLinks = getTotalNumLinks();

	    System.out.println();
	    System.out.println(this + ":");
	    System.out.println("\tnumNodes: " + numNodes);
	    System.out.println("\tnumLinks: " + numLinks);
	    System.out.println("\tnumTreeLinks: " + m_numTreeLinks);
	    System.out.println("\tnumNontreeLinks: " + m_numNontreeLinks);
	    System.out.println("\tnextIndex: " + m_links.nextIndex);
	
	    for (int i = 0; i < numNodes; i++)
	    {
		System.out.println("Node " + i + ":");
		System.out.println("\tparent link: " + m_nodes.parent[i]);
		System.out.println("\tparent node: "
				   + (m_nodes.parent[i] >= 0
				      ? m_links.source[m_nodes.parent[i]]
				      : -1));

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
			System.out.println
			    ("\t\t- - - - - - - - - - - - - - - -");
		    }

		    System.out.println("\t\t" + j + ": "
				       + m_links.source[j] + " => "
				       + m_links.destination[j]);
		}
	    }
	}
    }

    ///////////////////////////////////////////////////////////////////////

    private static class CanonicalGraphLoader
    {
	///////////////////////////////////////////////////////////////////
	// CONSTRUCTORS
	///////////////////////////////////////////////////////////////////

	public CanonicalGraphLoader(Reader reader)
	{
	    m_lexer = new StreamTokenizer(new BufferedReader(reader));
	    m_lexer.resetSyntax();
	    m_lexer.commentChar('#');
	    m_lexer.eolIsSignificant(true);
	    m_lexer.lowerCaseMode(true);
	    m_lexer.parseNumbers();
	    m_lexer.whitespaceChars(0, 32);
	}

	///////////////////////////////////////////////////////////////////
	// PUBLIC METHODS
	///////////////////////////////////////////////////////////////////

	public Graph load()
	{
	    Graph retval = null;
	    try
	    {
		GraphBuilder builder = null;
		while (m_lexer.nextToken() != StreamTokenizer.TT_EOF)
		{
		    if (m_lexer.ttype == 'h')
		    {
			if (builder != null)
			{
			    String msg = "Encountered duplicate header line";
			    throw new ParseException(msg);
			}
			builder = GraphFactory.makeImmutableGraph();
			parseHeader(builder);
		    }
		    else if (m_lexer.ttype == 't' || m_lexer.ttype == 'l')
		    {
			if (builder == null)
			{
			    String msg = "Missing header line";
			    throw new ParseException(msg);
			}
			parseLink(builder);
		    }
		    else
		    {
			String msg =
			    "Expected line type (h, t, or l); encountered "
			    + findTokenName(m_lexer.ttype);
			throw new ParseException(msg);
		    }
		}

		try
		{
		    QualifierCreator creator = builder.addQualifier
			("spanning_tree", "default_spanning_tree", null);
		    creator.associateAttribute(m_rootAttribute);
		    creator.associateAttribute(m_treeLinkAttribute);
		}
		catch (DuplicateObjectException e)
		{
		    String msg =
			"INTERNAL ERROR: while adding qualifier: " + e;
		    throw new ParseException(msg);
		}

		retval = builder.endConstruction();
	    }
	    catch (IOException e)
	    {
		System.err.println("Error while loading graph: " + e);
	    }
	    catch (ParseException e)
	    {
		System.err.println("Syntax error near line "
				   + m_lexer.lineno() + ": "
				   + e.toString());
	    }

	    return retval;
	}

	///////////////////////////////////////////////////////////////////
	// PRIVATE METHODS
	///////////////////////////////////////////////////////////////////

	// The GraphBuilder will be initialized such that attribute values
	// can be added for the "tree_link" attribute.

	private void parseHeader(GraphBuilder builder)
	    throws ParseException, IOException
	{
	    // 'h' <num-nodes> <num-links> <root-node>

	    match(StreamTokenizer.TT_NUMBER);
	    int numNodes = (int)m_lexer.nval;

	    match(StreamTokenizer.TT_NUMBER);
	    int numLinks = (int)m_lexer.nval;

	    match(StreamTokenizer.TT_NUMBER);
	    int rootNode = (int)m_lexer.nval;

	    match(StreamTokenizer.TT_EOL);

	    builder.allocateNodes(numNodes);
	    builder.allocateLinks(numLinks);
	    builder.allocatePaths(0);
	    builder.allocatePathLinks(0);

	    try
	    {
		m_rootAttribute = builder.addAttributeDefinition
		    ("root", ValueType.BOOLEAN, -1, null, false);

		AttributeCreator creator = builder.addNodeAttribute(rootNode);
		creator.addBooleanValue(true);

		m_treeLinkAttribute = builder.addAttributeDefinition
		    ("tree_link", ValueType.BOOLEAN, -1, null, false);
	    }
	    catch (DuplicateObjectException e)
	    {
		String msg = "INTERNAL ERROR: while adding attributes: " + e;
		throw new ParseException(msg);
	    }
	}

	// This expects the GraphBuilder to have been initialized such that
	// attribute values can be added for the "tree_link" attribute.

	private void parseLink(GraphBuilder builder)
	    throws ParseException, IOException
	{
	    // 't'|'l' <int> <int>

	    boolean isTreeLink = (m_lexer.ttype == 't');

	    match(StreamTokenizer.TT_NUMBER);
	    int source = (int)m_lexer.nval;

	    match(StreamTokenizer.TT_NUMBER);
	    int destination = (int)m_lexer.nval;

	    match(StreamTokenizer.TT_EOL);

	    int link = builder.addLink(source, destination);

	    if (isTreeLink)
	    {
		AttributeCreator creator = builder.addLinkAttribute(link);
		creator.addBooleanValue(true);
	    }
	}

	private void match(int ttype)
	    throws ParseException, IOException
	{
	    if (m_lexer.nextToken() != ttype)
	    {
		throw new ParseException("Mismatched tokens: expected "
					 + findTokenName(ttype)
					 + ", encountered "
					 + findTokenName(m_lexer.ttype));
	    }
	}

	private String findTokenName(int ttype)
	{
	    if (ttype == StreamTokenizer.TT_EOF)
	    {
		return "TT_EOF";
	    }
	    else if (ttype == StreamTokenizer.TT_EOL)
	    {
		return "TT_EOL";
	    }
	    else if (ttype == StreamTokenizer.TT_NUMBER)
	    {
		return "TT_NUMBER";
	    }
	    else if (ttype == StreamTokenizer.TT_WORD)
	    {
		return "TT_WORD";
	    }
	    else
	    {
		if (Character.isISOControl((char)ttype))
		{
		    return "0x" + Integer.toHexString(ttype);
		}
		else
		{
		    return "'" + (char)ttype + "'";
		}
	    }
	}

	///////////////////////////////////////////////////////////////////
	// PRIVATE FIELDS
	///////////////////////////////////////////////////////////////////

	private static final boolean DEBUG_PRINT = true;
	private static final boolean IS_VERBOSE = false;

	private StreamTokenizer  m_lexer;
	private int  m_rootAttribute = -1;
	private int  m_treeLinkAttribute = -1;
    }
}
