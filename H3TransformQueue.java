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


public class H3TransformQueue
{
    ////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ////////////////////////////////////////////////////////////////////////

    public H3TransformQueue(int numNodes)
    {
	m_elements = new int[numNodes + 1];
	m_radii = new double[numNodes + 1];

	m_radii[0] = Double.MAX_VALUE; // sentinel for use in enqueue()
    }

    ////////////////////////////////////////////////////////////////////////
    // PUBLIC METHODS
    ////////////////////////////////////////////////////////////////////////

    public int dequeue()
    {
	if (m_numElements == 0)
	{
	    throw new RuntimeException("Queue is empty.");
	}

	int retval = m_elements[1];

	double lastRadius = m_radii[m_numElements];
	int lastElement = m_elements[m_numElements];
	--m_numElements;

	int i = 1;
	boolean more = true;
	while (more && i * 2 <= m_numElements)
	{
	    int child = i * 2;
	    if (child < m_numElements)
	    {
		if (m_radii[child + 1] > m_radii[child])
		{
		    ++child;
		}
	    }

	    if (lastRadius < m_radii[child])
	    {
		m_radii[i] = m_radii[child];
		m_elements[i] = m_elements[child];
		i = child;
	    }
	    else
	    {
		more = false;
	    }
	}

	m_radii[i] = lastRadius;
	m_elements[i] = lastElement;

	return retval;
    }

    public void enqueue(int node, double radius)
    {
	if (m_numElements == m_elements.length - 1)
	{
	    throw new RuntimeException("Queue is full.");
	}

	int i = ++m_numElements;
	while (m_radii[i / 2] < radius)
	{
	    m_radii[i] = m_radii[i / 2];
	    m_elements[i] = m_elements[i / 2];
	    i /= 2;
	}

	m_radii[i] = radius;
	m_elements[i] = node;
    }

    public void clear()
    {
	m_numElements = 0;
    }

    public boolean isEmpty()
    {
	return m_numElements == 0;
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE FIELDS
    ////////////////////////////////////////////////////////////////////////

    private int m_numElements = 0;
    private int[] m_elements;
    private double[] m_radii;

    ////////////////////////////////////////////////////////////////////////
    // TEST METHODS
    ////////////////////////////////////////////////////////////////////////

    public void dumpForTesting()
    {
	System.out.println();
	System.out.println(this + ":");
	System.out.println("\tnumElements: " + m_numElements);
	System.out.println("\telements.length: " + m_elements.length);
	System.out.println("\tradii.length: " + m_radii.length);
	
	for (int i = 1; i <= m_numElements; i++)
	{
	    System.out.println("\t" + i + ": ("
			       + m_elements[i] + ", "
			       + m_radii[i] + ")");

	    if (i * 2 <= m_numElements)
	    {
		if (m_radii[i * 2] > m_radii[i])
		{
		    System.out.println("ERROR: Heap order violated "
				       + "by left child.");
		}
		else if (i * 2 < m_numElements
			 && m_radii[i * 2 + 1] > m_radii[i])
		{
		    System.out.println("ERROR: Heap order violated "
				       + "by right child.");
		}
	    }
	}

	System.out.println();
    }

    public void dumpForTesting2()
    {
	System.out.println();
	System.out.println(this + ":");
	System.out.println("\tnumElements: " + m_numElements);
	System.out.println("\telements.length: " + m_elements.length);
	System.out.println("\tradii.length: " + m_radii.length);
	
	if (m_numElements > 0)
	{
	    dumpForTesting2Aux(1, 1);
	}

	System.out.println();
    }

    public void dumpForTesting2Aux(int n, int level)
    {
	if (n * 2 + 1 <= m_numElements)
	{
	    dumpForTesting2Aux(n * 2 + 1, level + 1);
	}

	indentForTesting(level);
	System.out.println("[" + n + "] ("
			   + m_elements[n] + ", "
			   + m_radii[n] + ")");

	if (m_radii[n] > m_radii[n / 2])
	{
	    System.out.println("ERROR: Heap order violated by " + n + ".");
	}

	if (n * 2 <= m_numElements)
	{
	    dumpForTesting2Aux(n * 2, level + 1);
	}
    }

    public void indentForTesting(int n)
    {
	System.out.print('\t');
	for (int i = 0; i < n; i++)
	{
	    System.out.print("  ");
	}
    }
}
