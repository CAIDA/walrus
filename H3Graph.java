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
import javax.vecmath.*;

public interface H3Graph
{
    int getNumNodes();
    int getNumTreeLinks();
    int getNumNontreeLinks();
    int getTotalNumLinks();
    int getRootNode();
    double getNodeRadius(int node);
    void getNodeCoordinates(int node, Point3d point);
    void getNodeCoordinates(int node, Point4d point);
    void getNodeLayoutCoordinates(int node, Point3d point);
    void getNodeLayoutCoordinates(int node, Point4d point);
    int getNodeParent(int node);
    int getNodeParentLink(int node);
    int getNodeChildIndex(int node);
    int getNodeNontreeIndex(int node);
    int getNodeLinksEndIndex(int node);
    int getNodeColor(int node);
    boolean checkNodeVisited(int node, int iteration);
    boolean markNodeVisited(int node, int iteration);
    int getLinkSource(int link);
    int getLinkDestination(int link);
    int getLinkColor(int link);
    void transformNodes(Matrix4d t);
    void setRootNode(int node);
    void setNodeRadius(int node, double radius);
    void setNodeCoordinates(int node, double x, double y, double z);
    void setNodeCoordinates(int node, Point3d p);
    void setNodeCoordinates(int node, Point4d p);
    void setNodeLayoutCoordinates(int node, double x, double y,
					 double z, double w);
    void setNodeLayoutCoordinates(int node, Point3d p);
    void setNodeLayoutCoordinates(int node, Point4d p);
    void setNodeColor(int node, int color);
    void setNodeColor(int node, byte r, byte g, byte b);
    void setNodeDefaultColor(int color);
    void setNodeDefaultColor(byte r, byte g, byte b);
    void setLinkColor(int link, int color);
    void setLinkColor(int link, byte r, byte g, byte b);
    void setLinkDefaultColor(int color);
    void setLinkDefaultColor(byte r, byte g, byte b);

    void checkTreeReachability();
    void checkTreeReachability(int node);
    void dumpForTesting();
    void dumpForTesting2();
}
