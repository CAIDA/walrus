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

public class H3TransformQueueTester
{
    public static void main(String[] args)
    {
	H3TransformQueue queue = new H3TransformQueue(7);

	printThickRule();
	System.out.println("TEST INSERTIONS");
	printThinRule();
	insertIntoQueue(queue, 1, 10.0);
	queue.dumpForTesting2();
	insertIntoQueue(queue, 2, 10.0);
	queue.dumpForTesting2();
	insertIntoQueue(queue, 3, 9.0);
	queue.dumpForTesting2();
	insertIntoQueue(queue, 4, 11.0);
	queue.dumpForTesting2();
	insertIntoQueue(queue, 5, 9.0);
	queue.dumpForTesting2();
	insertIntoQueue(queue, 6, 11.0);
	queue.dumpForTesting2();
	insertIntoQueue(queue, 7, 12.0);
	queue.dumpForTesting2();
	insertIntoQueue(queue, 8, 13.0);
	queue.dumpForTesting2();
	insertIntoQueue(queue, 9, 8.0);
	queue.dumpForTesting2();
	insertIntoQueue(queue, 10, 10.5);
	queue.dumpForTesting2();

	printThickRule();
	System.out.println("TEST DELETIONS");
	printThinRule();
	removeFromQueue(queue);
	queue.dumpForTesting2();
	removeFromQueue(queue);
	queue.dumpForTesting2();
	removeFromQueue(queue);
	queue.dumpForTesting2();
	removeFromQueue(queue);
	queue.dumpForTesting2();
	removeFromQueue(queue);
	queue.dumpForTesting2();
	removeFromQueue(queue);
	queue.dumpForTesting2();
	removeFromQueue(queue);
	queue.dumpForTesting2();
	removeFromQueue(queue);
	queue.dumpForTesting2();

	printThickRule();
	System.out.println("TEST MIX OF INSERTIONS AND DELETIONS");
	printThinRule();

	insertIntoQueue(queue, 1, 10.0);
	queue.dumpForTesting2();
	removeFromQueue(queue);
	queue.dumpForTesting2();
	insertIntoQueue(queue, 2, 10.0);
	queue.dumpForTesting2();
	insertIntoQueue(queue, 3, 9.0);
	queue.dumpForTesting2();
	removeFromQueue(queue);
	queue.dumpForTesting2();
	insertIntoQueue(queue, 4, 11.0);
	queue.dumpForTesting2();
	insertIntoQueue(queue, 5, 9.0);
	queue.dumpForTesting2();
	insertIntoQueue(queue, 6, 11.0);
	queue.dumpForTesting2();
	removeFromQueue(queue);
	queue.dumpForTesting2();
	insertIntoQueue(queue, 7, 12.0);
	queue.dumpForTesting2();
	removeFromQueue(queue);
	queue.dumpForTesting2();
	removeFromQueue(queue);
	queue.dumpForTesting2();
	removeFromQueue(queue);
	queue.dumpForTesting2();
	removeFromQueue(queue);
	queue.dumpForTesting2();
	insertIntoQueue(queue, 8, 13.0);
	queue.dumpForTesting2();
	removeFromQueue(queue);
	queue.dumpForTesting2();
	insertIntoQueue(queue, 9, 8.0);
	queue.dumpForTesting2();
	insertIntoQueue(queue, 10, 10.5);
	queue.dumpForTesting2();
	removeFromQueue(queue);
	queue.dumpForTesting2();
	removeFromQueue(queue);
	queue.dumpForTesting2();
	removeFromQueue(queue);
	queue.dumpForTesting2();
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE METHODS
    ////////////////////////////////////////////////////////////////////////

    private static void insertIntoQueue(H3TransformQueue queue,
					int n, double r)
    {
	System.out.println("Inserting (" + n + ", " + r + ")");

	try
	{
	    queue.enqueue(n, r);
	}
	catch (RuntimeException e)
	{
	    System.out.println("ERROR: " + e);
	}
    }

    private static void removeFromQueue(H3TransformQueue queue)
    {
	try
	{
	    int n = queue.dequeue();
	    System.out.println("Removed " + n);
	}
	catch (RuntimeException e)
	{
	    System.out.println("ERROR: " + e);
	}
    }

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
