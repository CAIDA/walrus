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

public class H3GraphGenerator
{
    ////////////////////////////////////////////////////////////////////////
    // PUBLIC METHODS
    ////////////////////////////////////////////////////////////////////////

    public static void createTree(H3GraphBuffer buffer, int arity, int height)
    {
	int numNodes = calculateNumNodes(arity, height);
	addNodes(buffer, numNodes);
	addLinks(buffer, arity, height, 0, 0);
    }

    public static void createGraph(H3GraphBuffer buffer, int arity, int height)
    {
	int numNodes = calculateNumNodes(arity, height);
	addNodes(buffer, numNodes);
	addLinks(buffer, arity, height, 0, 0);
	addNontreeLinks(buffer, arity, height, 0, 0);
    }

    public static void changeToGraph(H3GraphBuffer buffer,
				     int arity, int height)
    {
	addNontreeLinks(buffer, arity, height, 0, 0);
    }

    //----------------------------------------------------------------------

    public static void createTernaryTreeSet(H3GraphBuffer buffer)
    {
	int root = buffer.addNode(); // add root

	buffer.addChild(root, createTernaryTree(buffer,
						new int[] {10,10,10}));

	buffer.addChild(root, createTernaryTree(buffer,
						new int[] {100,100,100}));

	buffer.addChild(root, createTernaryTree(buffer,
						new int[] {100,100,50}));

	buffer.addChild(root, createTernaryTree(buffer,
						new int[] {100,100,5}));

	buffer.addChild(root, createTernaryTree(buffer,
						new int[] {100,50,50}));

	buffer.addChild(root, createTernaryTree(buffer,
						new int[] {100,50,5}));

	buffer.addChild(root, createTernaryTree(buffer,
						new int[] {500,500,500}));

	buffer.addChild(root, createTernaryTree(buffer,
						new int[] {500,500,50}));

	buffer.addChild(root, createTernaryTree(buffer,
						new int[] {500,500,5}));

	buffer.addChild(root, createTernaryTree(buffer,
						new int[] {500,50,50}));

	buffer.addChild(root, createTernaryTree(buffer,
						new int[] {500,50,5}));
    }


    //----------------------------------------------------------------------

    public static void createLinearIncreasingTree(H3GraphBuffer buffer,
						  int base, int scale,
						  int numIterations)
    {
	createIncreasingTree(buffer, base, scale, numIterations,
			     new SizeFunction() {
	    public int evaluate(int base, int scale, int numIterations,
				int currentIteration, int currentSize)
		{
		    return base + scale * currentIteration;
		}
	});
    }

    public static void createQuadraticIncreasingTree(H3GraphBuffer buffer,
						  int base, int scale,
						  int numIterations)
    {
	createIncreasingTree(buffer, base, scale, numIterations,
			     new SizeFunction() {
	    public int evaluate(int base, int scale, int numIterations,
				int currentIteration, int currentSize)
		{
		    return base + scale * currentIteration * currentIteration;
		}
	});
    }

    public static void createCubicIncreasingTree(H3GraphBuffer buffer,
						  int base, int scale,
						  int numIterations)
    {
	createIncreasingTree(buffer, base, scale, numIterations,
			     new SizeFunction() {
	    public int evaluate(int base, int scale, int numIterations,
				int currentIteration, int currentSize)
		{
		    int c = currentIteration;
		    return base	+ scale * c * c * c;
		}
	});
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE METHODS
    ////////////////////////////////////////////////////////////////////////

    private static int createTernaryTree(H3GraphBuffer buffer, int[] sizes)
    {
	return createIncreasingTree(buffer, 1, 1, 3,
				    new StaticSizeFunction(sizes));
    }

    //----------------------------------------------------------------------

    public static int createIncreasingTree(H3GraphBuffer buffer,
					   int base, int scale,
					   int numIterations,
					   SizeFunction function)
    {
	// ASSERT: base > 0
	// ASSERT: scale > 0
	// ASSERT: numIterations > 0

	int root = buffer.addNode(); // add root	
	
	int size = base;
	for (int i = 0; i < numIterations; i++)
	{
	    int subtree = buffer.addNode();
	    buffer.addChild(root, subtree);

	    size = function.evaluate(base, scale, numIterations, i, size);
	    for (int j = 1; j <= size; j++)
	    {
		buffer.addChild(subtree, buffer.addNode());
	    }
	}

	return root;
    }

    //----------------------------------------------------------------------

    private static void addLinks(H3GraphBuffer buffer, int arity,
				 int maxHeight, int height, int parent)
    {
	if (height < maxHeight)
	{
	    int start = parent * arity;
	    for (int i = 1; i <= arity; i++)
	    {
		int child = start + i;
		buffer.addChild(parent, child);
		addLinks(buffer, arity, maxHeight, height + 1, child);
	    }
	}
    }

    private static void addNontreeLinks(H3GraphBuffer buffer, int arity,
					int maxHeight, int height, int parent)
    {
	if (height < maxHeight)
	{
	    int start = parent * arity;
	    int firstChild = start + 1;

	    buffer.addNontreeLink(firstChild, parent);
	    addNontreeLinks(buffer, arity, maxHeight, height + 1, firstChild);

	    for (int i = 2; i <= arity; i++)
	    {
		int child = start + i;
		buffer.addNontreeLink(firstChild, child);
		addNontreeLinks(buffer, arity, maxHeight, height + 1, child);
	    }
	}
    }

    private static void addNodes(H3GraphBuffer buffer, int numNodes)
    {
	for (int i = 0; i < numNodes; i++)
	{
	    buffer.addNode();
	}
    }

    private static int calculateNumNodes(int arity, int height)
    {
	int retval = 1;
	int pow = 1;

	while (height-- > 0)
	{
	    pow *= arity;
	    retval += pow;
	}

	return retval;
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE CLASSES
    ////////////////////////////////////////////////////////////////////////

    private static interface SizeFunction
    {
	int evaluate(int base, int scale, int numIterations,
		     int currentIteration, int currentSize);
    }

    private static class StaticSizeFunction implements SizeFunction
    {
	public StaticSizeFunction(int[] sizes)
	{
	    m_sizes = sizes;
	}

	public int evaluate(int base, int scale, int numIterations,
			    int currentIteration, int currentSize)
	{
	    return m_sizes[currentIteration % m_sizes.length];
	}

	private int[] m_sizes;
    }
}
