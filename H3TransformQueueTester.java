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
