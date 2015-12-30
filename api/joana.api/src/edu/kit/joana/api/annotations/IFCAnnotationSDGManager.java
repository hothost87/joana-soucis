/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.annotations;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.ifc.sdg.graph.SDGNode;

/**
 * TODO: @author Add your name here.
 */
public class IFCAnnotationSDGManager {
	
	private final Map<SDGNode, IFCAnnotationSDG> sourceAnnotations;
	private final Map<SDGNode, IFCAnnotationSDG> sinkAnnotations;
	private final Map<SDGNode, IFCAnnotationSDG> declassAnnotations;
	private final IFCAnnotationApplicator app;
	
	public IFCAnnotationSDGManager(SDGProgram program) {
		this.sourceAnnotations = new HashMap<SDGNode, IFCAnnotationSDG>();
		this.sinkAnnotations = new HashMap<SDGNode, IFCAnnotationSDG>();
		this.declassAnnotations = new HashMap<SDGNode, IFCAnnotationSDG>();
		this.app = new IFCAnnotationApplicator(program);
	}
	
	public void addAnnotation(IFCAnnotationSDG ann) {
		if (ann.getType() == AnnotationType.SOURCE) {
			sourceAnnotations.put(ann.getSDGNode(), ann);
		} else if (ann.getType() == AnnotationType.SINK) {
			sinkAnnotations.put(ann.getSDGNode(), ann);
		} else {
			declassAnnotations.put(ann.getSDGNode(), ann);
		}
	}
	
	public Collection<IFCAnnotationSDG> getAnnotations() {
		LinkedList<IFCAnnotationSDG> ret = new LinkedList<IFCAnnotationSDG>();
		ret.addAll(sourceAnnotations.values());
		ret.addAll(sinkAnnotations.values());
		ret.addAll(declassAnnotations.values());
		return ret;
	}
	
	public void applyAllAnnotations() {
		app.applyAnnotationsSDG(getAnnotations());
	}
	
	public void unapplyAllAnnotations() {
		app.unapplyAnnotationsSDG(getAnnotations());
	}

}
