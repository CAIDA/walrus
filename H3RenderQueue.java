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

public class H3RenderQueue
{
    ////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ////////////////////////////////////////////////////////////////////////

    public H3RenderQueue(int size)
    {
	m_data = new long[size];
    }

    ////////////////////////////////////////////////////////////////////////
    // PUBLIC METHODS
    ////////////////////////////////////////////////////////////////////////

    public synchronized boolean get(int index, Element element)
    {
	boolean retval = false;

	boolean tryAgain;
	do
	{
	    tryAgain = false;
	    if (index < m_numElements)
	    {
		decode(m_data[index], element);
		retval = true;
	    }
	    else 
	    {
		if (!m_isComplete)
		{
		    m_isWaitingForData = true;
		    waitIgnore();
		    tryAgain = true;
		}
	    }
	}
	while (tryAgain);

	return retval;
    }

    public synchronized int getMaxNumElements()
    {
	return m_data.length;
    }

    public synchronized int getCurrentNumElements()
    {
	return m_numElements;
    }

    public synchronized boolean isComplete()
    {
	return m_isComplete;
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    public synchronized void add(int n, long[] data)
    {
	System.arraycopy(data, 0, m_data, m_numElements, n);
	m_numElements += n;
	notifyIfWaiting();
    }

    public synchronized void clear()
    {
	m_numElements = 0;
	m_isComplete = false;
	notifyIfWaiting();
    }

    public synchronized void end()
    {
	m_isComplete = true;
	notifyIfWaiting();
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE METHODS
    ////////////////////////////////////////////////////////////////////////

    private void decode(long data, Element element)
    {
	// See comments for m_transformedData in H3Transformer.java
	// for details about the encoding of {data}.

	element.type = (int)(data >> 32);
	element.data = (int)(data & 0xFFFFFFFF);
    }

    private synchronized void notifyIfWaiting()
    {
	if (m_isWaitingForData)
	{
	    m_isWaitingForData = false;
	    notifyAll();
	}
    }

    private synchronized void waitIgnore()
    {
	try { wait(); } catch (InterruptedException e) { }
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE FIELDS
    ////////////////////////////////////////////////////////////////////////

    private long[] m_data;
    private int m_numElements = 0;
    private boolean m_isComplete = false;
    private boolean m_isWaitingForData = false;

    ////////////////////////////////////////////////////////////////////////
    // PUBLIC CLASSES
    ////////////////////////////////////////////////////////////////////////

    public static class Element
    {
	public static final int TYPE_NODE = 0;
	public static final int TYPE_TREE_LINK = 1;
	public static final int TYPE_NONTREE_LINK = 2;
	
	int type;
	int data;
    }
}
