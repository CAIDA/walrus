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

public class H3CapturingRotationRequest
    implements H3RotationRequest
{
    ////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ////////////////////////////////////////////////////////////////////////

    public H3CapturingRotationRequest(H3RotationRequest request)
    {
	m_request = request;
    }

    ////////////////////////////////////////////////////////////////////////
    // PUBLIC METHODS
    ////////////////////////////////////////////////////////////////////////

    public H3RotationRequest createReplayingRequest()
    {
	return new ReplayingRotationRequest(m_matrices);
    }

    public boolean getRotation(Matrix4d rot)
    {
	boolean retval = false;

	if (m_request != null)
	{
	    if (m_request.getRotation(rot))
	    {
		m_matrices.add(new Matrix4d(rot));
		retval = true;
	    }
	    else
	    {
		m_request = null;
	    }
	}

	return retval;
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE FIELDS
    ////////////////////////////////////////////////////////////////////////

    private H3RotationRequest m_request;
    private List m_matrices = new ArrayList();  // List<Matrix4d>

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE NESTED CLASSES
    ////////////////////////////////////////////////////////////////////////

    private static class ReplayingRotationRequest
	implements H3RotationRequest
    {
	public ReplayingRotationRequest(List matrices)
	{
	    m_matrices = matrices;
	}

	public boolean getRotation(Matrix4d rot)
	{
	    boolean retval = false;

	    if (m_nextRotation < m_matrices.size())
	    {
		Matrix4d m = (Matrix4d)m_matrices.get(m_nextRotation++);
		rot.set(m);
		retval = true;
	    }

	    return retval;
	}

	private int m_nextRotation;
	private List m_matrices;
    }
}
