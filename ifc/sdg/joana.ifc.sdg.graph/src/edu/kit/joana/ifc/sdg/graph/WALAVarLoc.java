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
public class WALAVarLoc {

	private int valuenum;
	private WALAIRLoc loc;
	
	public WALAVarLoc(int valuenum, WALAIRLoc loc) {
		this.valuenum = valuenum;
		this.loc = loc;
	}
	
	@Override
	public boolean equals(Object o) {
						
		if(!(o instanceof WALAVarLoc)) 
			return false;
		
		WALAVarLoc other = (WALAVarLoc)o;
		
		if(this.valuenum==other.valuenum && this.loc.equals(other.loc)) {
			return true;
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		return valuenum * loc.hashCode();
	}
	
	public String toString(){
		return this.loc.toString() + " value number: " + this.valuenum;
	}
	
	public int getValunum() {
		return this.valuenum;
	}
	
	public WALAIRLoc getWALAIRLoc() {
		return this.loc;
	}
	
}
