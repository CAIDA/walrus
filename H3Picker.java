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


import javax.vecmath.*;

public interface H3Picker
{
    // (x, y) are the AWT (that is, window) coordinates of the location
    // clicked on by the user.  {center} is the AWT coordinates of the node
    // actually picked, if any.  This returns the ID of the node picked,
    // or -1 if no node was picked.
    int pickNode(int x, int y, Point2d center);

    // (x, y) are the AWT (that is, window) coordinates of the location
    // clicked on by the user.
    void highlightNode(int x, int y);
    void highlightNode(int node);
    void reset();
}
