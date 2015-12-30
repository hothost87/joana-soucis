/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph;

import com.ibm.wala.ssa.SSAInstruction;

/**
 * TODO: @author Add your name here.
 */
public class WALAIRLoc {

	private int cgNodeID;
	private SSAInstruction instruction;
	
	public WALAIRLoc(int cgNodeID, SSAInstruction instruction) {
		this.cgNodeID = cgNodeID;
		this.instruction = instruction;
	}
	
	@Override
	public boolean equals(Object o) {
						
		if(!(o instanceof WALAIRLoc)) 
			return false;
		
		WALAIRLoc other = (WALAIRLoc)o;
		
		if(this.cgNodeID==other.cgNodeID && this.instruction.equals(other.instruction)) {
			if(this.instruction.hasDef()) {
				if(this.instruction.getDef() != other.instruction.getDef()) {
					return false;
				}
			}
			return true;
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		return cgNodeID * instruction.iindex;
	}
	
	public String toString(){
		return "CGNodeID: " + this.cgNodeID + ", Conditional instruction: " + this.instruction;
	}
	
	public int getCGNodeID(){
		return this.cgNodeID;
	}
	
	public SSAInstruction getInstruction() {
		return this.instruction;
	}
	
}
