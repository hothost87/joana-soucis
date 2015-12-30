/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph;

/**
 * TODO: @author Add your name here.
 */
public class WALAVarDefLoc {

//	private int valuenum;
	private WALAVarLoc loc;
	private int nodeId;
	
	public WALAVarDefLoc(WALAVarLoc loc, int nodeId) {
//		this.valuenum = valuenum;
		this.loc = loc;
		this.nodeId = nodeId;
	}
	
	@Override
	public boolean equals(Object o) {
						
		if(!(o instanceof WALAVarDefLoc)) 
			return false;
		
		WALAVarDefLoc other = (WALAVarDefLoc)o;
		
		if(this.loc.equals(other.loc) && this.nodeId == other.nodeId) {
			return true;
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		return loc.hashCode() * nodeId;
	}
	
	public String toString(){
		return this.loc.toString() + " " + this.nodeId;
	}
	
//	public int getValunum() {
//		return this.valuenum;
//	}
//	
	public WALAVarLoc getWALAVarLoc() {
		return this.loc;
	}
	
	public int getNodeId() {
		return this.nodeId;
	}
	
}
