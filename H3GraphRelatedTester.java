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

public class H3GraphRelatedTester
{
    public static void main(String[] args)
    {
	{
	    printThickRule();
	    System.out.println("Empty Graph");
	    printThinRule();
	    {
		H3GraphBuffer buffer = new H3GraphBuffer(5);
		buffer.dumpForTesting();
	    }
	    printThickRule();	
	}

	{
	    printThickRule();
	    System.out.println("Complete Binary Tree of Height 3");
	    printThinRule();
	    {
		H3GraphBuffer buffer = new H3GraphBuffer(5);
		H3GraphGenerator.createTree(buffer, 2, 3);
		buffer.dumpForTesting();
		buffer.sortCompositeLinksForTesting();
		buffer.dumpForTesting();
		H3GraphGenerator.changeToGraph(buffer, 2, 3);
		buffer.dumpForTesting();

		H3Graph graph = buffer.toGraph();
		graph.dumpForTesting();
		graph.dumpForTesting2();
	    }
	    printThickRule();	
	}

	{
	    printThickRule();
	    System.out.println("Complete Binary Tree of Height 7");
	    printThinRule();
	    {
		H3GraphBuffer buffer = new H3GraphBuffer(10);
		H3GraphGenerator.createTree(buffer, 2, 7);
		buffer.dumpForTesting();
		buffer.sortCompositeLinksForTesting();
		buffer.dumpForTesting();

		H3Graph graph = buffer.toGraph();
		graph.dumpForTesting();
		graph.dumpForTesting2();
	    }
	    printThickRule();	
	}

	{
	    printThickRule();
	    System.out.println("Complete Ternary Tree of Height 3");
	    printThinRule();
	    {
		H3GraphBuffer buffer = new H3GraphBuffer(5);
		H3GraphGenerator.createTree(buffer, 3, 3);
		buffer.dumpForTesting();
		buffer.sortCompositeLinksForTesting();
		buffer.dumpForTesting();
		H3GraphGenerator.changeToGraph(buffer, 3, 3);
		buffer.dumpForTesting();

		H3Graph graph = buffer.toGraph();
		graph.dumpForTesting();
		graph.dumpForTesting2();
	    }
	    printThickRule();	
	}
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE METHODS
    ////////////////////////////////////////////////////////////////////////

    private static void printThickRule()
    {
	System.out.println(
"============================================================================"
);
    }

    private static void printThinRule()
    {
	System.out.println(
"----------------------------------------------------------------------------"
);
    }
}
