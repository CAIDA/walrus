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


import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.util.Observer;
import java.util.Observable;
import javax.media.j3d.*;
import javax.vecmath.*;

public class H3Canvas3D
    extends Canvas3D
{
    public H3Canvas3D(GraphicsConfiguration config)
    {
	super(config);
    }

    public void addPaintObserver(Observer o)
    {
	m_observable.addObserver(o);
    }

    public void removePaintObserver(Observer o)
    {
	m_observable.deleteObserver(o);
    }

    public void paint(Graphics g)
    {
	super.paint(g);
	m_observable.notifyPaintObservers();
    }

    //=======================================================================

    private PaintObservable m_observable = new PaintObservable();

    private class PaintObservable extends Observable
    {
	public PaintObservable()
	{
	    super();
	}

	public void notifyPaintObservers()
	{
	    setChanged();
	    notifyObservers();
	}
    }
}
