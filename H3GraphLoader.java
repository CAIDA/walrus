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

public class H3GraphLoader
{
    ////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ////////////////////////////////////////////////////////////////////////

    public H3GraphLoader(Reader reader)
    {
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
	long baseTotalMemory = 0;    // Stats at start.
	long baseFreeMemory = 0;

	long bufferTotalMemory = 0;  // Stats after loading into graph buffer.
	long bufferFreeMemory = 0;

	long peakTotalMemory = 0;    // Stats after populating graph, and while
	long peakFreeMemory = 0;     // the graph buffer is still reachable.

	long finalTotalMemory = 0;   // Stats after freeing graph buffer, and
	long finalFreeMemory = 0;    // leaving only graph.

	if (DEBUG_MEMORY)
	{
	    System.gc();
	    baseTotalMemory = Runtime.getRuntime().totalMemory();
	    baseFreeMemory = Runtime.getRuntime().freeMemory();
	}

	long startTime = 0;
	if (DEBUG_PRINT)
	{
	    startTime = System.currentTimeMillis();
	    System.out.println("load.begin[" + startTime +"]");
	}

	H3Graph retval = null;

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

	    if (DEBUG_MEMORY)
	    {
		System.gc();
		bufferTotalMemory = Runtime.getRuntime().totalMemory();
		bufferFreeMemory = Runtime.getRuntime().freeMemory();
	    }

	    if (DEBUG_PRINT_TRACE)
	    {
		m_buffer.dumpForTesting();
	    }

	    retval = m_buffer.toGraph();

	    if (DEBUG_MEMORY)
	    {
		System.gc();
		peakTotalMemory = Runtime.getRuntime().totalMemory();
		peakFreeMemory = Runtime.getRuntime().freeMemory();
	    }
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

	if (DEBUG_PRINT)
	{
	    long stopTime = System.currentTimeMillis();
	    long duration = stopTime - startTime;
	    System.out.println("load.end[" + stopTime + "]");
	    System.out.println("load.time[" + duration + "]");
	}

	if (DEBUG_MEMORY)
	{
	    System.gc();
	    finalTotalMemory = Runtime.getRuntime().totalMemory();
	    finalFreeMemory = Runtime.getRuntime().freeMemory();

	    long baseUsed = baseTotalMemory - baseFreeMemory;
	    long bufferUsed = bufferTotalMemory - bufferFreeMemory;
	    long peakUsed = peakTotalMemory - peakFreeMemory;
	    long finalUsed = finalTotalMemory - finalFreeMemory;

	    System.out.println("===========================================");
	    System.out.println("baseTotalMemory = " + M(baseTotalMemory));
	    System.out.println("baseFreeMemory = " + M(baseFreeMemory));
	    System.out.println("baseUsed = " + M(baseUsed));
	    System.out.println("bufferTotalMemory = " + M(bufferTotalMemory));
	    System.out.println("bufferFreeMemory = " + M(bufferFreeMemory));
	    System.out.println("bufferUsed = " + M(bufferUsed));
	    System.out.println("peakTotalMemory = " + M(peakTotalMemory));
	    System.out.println("peakFreeMemory = " + M(peakFreeMemory));
	    System.out.println("peakUsed = " + M(peakUsed));
	    System.out.println("finalTotalMemory = " + M(finalTotalMemory));
	    System.out.println("finalFreeMemory = " + M(finalFreeMemory));
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

	return retval;
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE METHODS
    ////////////////////////////////////////////////////////////////////////

    private String M(long n)
    {
	long x = n / 100000;
	long mega = x / 10;
	long kilo = x % 10;
	return "" + n + " (" + mega + "." + kilo + "e6)";
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

    private StreamTokenizer m_lexer;
    private H3GraphBuffer m_buffer = new H3GraphBuffer();
    private Map m_nodes = new HashMap();

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
}
