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

public class H3GraphBuffer
{
    ////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ////////////////////////////////////////////////////////////////////////

    public H3GraphBuffer()
    {
	this(INITIAL_CAPACITY);
    }

    public H3GraphBuffer(int initialCapacity)
    {
	// PRECONDITION: initialCapacity > 0

	m_parentLinks = new int[initialCapacity];
	m_childLinks = new long[initialCapacity];
	m_nontreeLinks = new long[initialCapacity];

	if (FILL_ARRAYS)
	{
	    Arrays.fill(m_parentLinks, -1);
	    Arrays.fill(m_childLinks, -1);
	    Arrays.fill(m_nontreeLinks, -1);
	}
    }

    ////////////////////////////////////////////////////////////////////////
    // PUBLIC METHODS
    ////////////////////////////////////////////////////////////////////////

    public int addNode()
    {
	return m_numNodes++;
    }

    public void addChild(int parent, int child)
    {
	// PRECONDITION: 0 <= parent < m_numNodes
	// PRECONDITION: 0 <= child < m_numNodes
	// PRECONDITION: parent != child

	if (child >= m_parentLinks.length)
	{
	    m_parentLinks = expandArray(m_parentLinks, child);
	}

	if (m_numChildLinks == m_childLinks.length)
	{
	    m_childLinks = expandArray(m_childLinks);
	}

	m_parentLinks[child] = parent;
	m_childLinks[m_numChildLinks++] = ((long)parent << 32) | child;
    }

    public void addNontreeLink(int origin, int target)
    {
	// PRECONDITION: 0 <= origin < m_numNodes
	// PRECONDITION: 0 <= target < m_numNodes

	if (m_numNontreeLinks == m_nontreeLinks.length)
	{
	    m_nontreeLinks = expandArray(m_nontreeLinks);
	}

	m_nontreeLinks[m_numNontreeLinks++] = ((long)origin << 32) | target;
    }

    public H3Graph toGraph()
    {
	sortCompositeLinks();

	int numLinks = m_numChildLinks + m_numNontreeLinks;
	H3Graph graph = new H3Graph(m_numNodes, numLinks);

	copyLinks(graph);
	return graph;
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE METHODS
    ////////////////////////////////////////////////////////////////////////

    private void copyLinks(H3Graph graph)
    {
	LinkSpan span = new LinkSpan();

	span.parent = -1; // Do this for assertion checking in computeLinkSpan
	span.childStart = span.nontreeStart = 0;
	span.childEnd = span.nontreeEnd = 0;

	while (computeLinkSpan(span))
	{
	    graph.startChildLinks(span.parent);

	    for (int i = span.childStart; i < span.childEnd; i++)
	    {
		long data = m_childLinks[i];
		int parent = (int)(data >> 32);
		int child = (int)(data & 0xFFFFFFFF);

		if (parent != span.parent)
		{
		    throw new
			RuntimeException("ASSERT: parent == span.parent.");
		}

		graph.addChildLink(span.parent, child);
	    }

	    graph.startNontreeLinks(span.parent);

	    for (int i = span.nontreeStart; i < span.nontreeEnd; i++)
	    {
		long data = m_nontreeLinks[i];
		int source = (int)(data >> 32);
		int destination = (int)(data & 0xFFFFFFFF);

		if (source != span.parent)
		{
		    throw new
			RuntimeException("ASSERT: source == span.parent.");
		}

		graph.addNontreeLink(span.parent, destination);
	    }

	    graph.endNodeLinks(span.parent);
	}
    }

    private boolean computeLinkSpan(LinkSpan span)
    {
	boolean retval = true;

	int previousParent = span.parent;

	span.parent = -1;
	span.childStart = span.childEnd;
	span.nontreeStart = span.nontreeEnd;

	if (span.childStart == m_numChildLinks
	    && span.nontreeStart == m_numNontreeLinks)
	{
	    retval = false;
	}
	else
	{
	    // Find next parent. - - - - - - - - - - - - - - - - - - - - - 

	    int nextChildParent = Integer.MAX_VALUE;
	    int nextNontreeParent = Integer.MAX_VALUE;

	    if (span.childStart < m_numChildLinks)
	    {
		long data = m_childLinks[span.childStart];
		nextChildParent = (int)(data >> 32);
	    }

	    if (span.nontreeStart < m_numNontreeLinks)
	    {
		long data = m_nontreeLinks[span.nontreeStart];
		nextNontreeParent = (int)(data >> 32);
	    }

	    span.parent = Math.min(nextChildParent, nextNontreeParent);

	    if (span.parent < 0)
	    {
		throw new RuntimeException("ASSERT: span.parent("
					   + span.parent + ") >= 0");
	    }

	    if (span.parent < previousParent)
	    {
		throw new RuntimeException("ASSERT: span.parent("
					   + span.parent
					   + ") > previousParent("
					   + previousParent + ")");
	    }

	    // Compute spans. - - - - - - - - - - - - - - - - - - - - - - - 

	    while (span.childEnd < m_numChildLinks)
	    {
		long data = m_childLinks[span.childEnd];
		int nextParent = (int)(data >> 32);
		if (nextParent == span.parent)
		{
		    ++span.childEnd;
		}
		else
		{
		    break;
		}
	    }

	    while (span.nontreeEnd < m_numNontreeLinks)
	    {
		long data = m_nontreeLinks[span.nontreeEnd];
		int nextParent = (int)(data >> 32);
		if (nextParent == span.parent)
		{
		    ++span.nontreeEnd;
		}
		else
		{
		    break;
		}
	    }
	}

	return retval;
    }

    private void sortCompositeLinks()
    {
	Arrays.sort(m_childLinks, 0, m_numChildLinks);
	Arrays.sort(m_nontreeLinks, 0, m_numNontreeLinks);
    }

    private int[] expandArray(int[] array, int minimumSize)
    {
	int newSize = array.length;
	do
	{
	    newSize *= ENLARGING_FACTOR;
	}
	while (newSize < minimumSize);

	int[] newArray = new int[newSize];
	System.arraycopy(array, 0, newArray, 0, array.length);

	if (FILL_ARRAYS)
	{
	    Arrays.fill(newArray, array.length, newArray.length, -1);
	}

	return newArray;
    }

    private long[] expandArray(long[] array)
    {
	long[] newArray = new long[ENLARGING_FACTOR * array.length];

	System.arraycopy(array, 0, newArray, 0, array.length);

	if (FILL_ARRAYS)
	{
	    Arrays.fill(newArray, array.length, newArray.length, -1);
	}

	return newArray;
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE FIELDS
    ////////////////////////////////////////////////////////////////////////

    private final static boolean FILL_ARRAYS = true; // useful for debugging

    private final static int INITIAL_CAPACITY = 30000;
    private final static int ENLARGING_FACTOR = 2;

    private int m_numNodes = 0;
    private int m_numChildLinks = 0;
    private int m_numNontreeLinks = 0;

    private int[] m_parentLinks;
    private long[] m_childLinks;   // (parent << 32) | child)
    private long[] m_nontreeLinks;  // (origin << 32) | target)

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE CLASSES
    ////////////////////////////////////////////////////////////////////////

    private static class LinkSpan
    {
	public int parent;
	public int childStart;
	public int childEnd;
	public int nontreeStart;
	public int nontreeEnd;
    }

    ////////////////////////////////////////////////////////////////////////
    // TEST METHODS
    ////////////////////////////////////////////////////////////////////////

    public void sortCompositeLinksForTesting()
    {
	sortCompositeLinks();
    }

    public void dumpForTesting()
    {
	System.out.println();
	System.out.println(this + ":");
	System.out.println("\tnumNodes: " + m_numNodes);
	System.out.println("\tnumChildLinks: " + m_numChildLinks);
	System.out.println("\tnumNontreeLinks: " + m_numNontreeLinks);
	
	System.out.println("\nParentLinks (" + m_parentLinks.length + "):\n");
	for (int i = 0; i < m_parentLinks.length; i++)
	{
	    if (i == m_numNodes)
	    {
		System.out.println("- - - - - - - - - - - - - - - - - - - -");
	    }
	    System.out.println(i + " => " + m_parentLinks[i]);
	}

	System.out.println("\nChildLinks (" + m_childLinks.length + "):\n");
	for (int i = 0; i < m_childLinks.length; i++)
	{
	    long data = m_childLinks[i];
	    int parent = (int)(data >> 32);
	    int child = (int)(data & 0xFFFFFFFF);

	    if (i == m_numChildLinks)
	    {
		System.out.println("- - - - - - - - - - - - - - - - - - - -");
	    }
	    System.out.print("[" + Long.toHexString(data) + "] ");
	    System.out.println(parent + " => " + child);
	}

	System.out.println("\nNontreeLinks (" + m_nontreeLinks.length +"):\n");
	for (int i = 0; i < m_nontreeLinks.length; i++)
	{
	    long data = m_nontreeLinks[i];
	    int origin = (int)(data >> 32);
	    int target = (int)(data & 0xFFFFFFFF);

	    if (i == m_numNontreeLinks)
	    {
		System.out.println("- - - - - - - - - - - - - - - - - - - -");
	    }
	    System.out.print("[" + Long.toHexString(data) + "] ");
	    System.out.println(origin + " => " + target);
	}
    }
}
