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
