/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.annotations;

import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.ifc.sdg.graph.SDGNode;

/**
 * TODO: @author Add your name here.
 */
public class IFCAnnotationSDG {

	private final AnnotationType type;
	private final String level;
	private final SDGNode annotatedSDGNode;

	public IFCAnnotationSDG(AnnotationType type, String level, SDGNode annotatedSDGNode) {
		if (type == AnnotationType.DECLASS || type == null || level == null || annotatedSDGNode == null) {
			throw new IllegalArgumentException();
		}

		this.type = type;
		this.level = level;
		this.annotatedSDGNode = annotatedSDGNode;
	}
	
	public AnnotationType getType() {
		return type;
	}
	
	public SDGNode getSDGNode() {
		return annotatedSDGNode;
	}
	
	public String getLevel(){
		return level;
	}
	
}
