package edu.kit.joana.ifc.sdg.core;

import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.SlicingBasedIFC;
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.Slicer;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.NotInLatticeException;

public class SoucisSlicingBasedIFC extends SlicingBasedIFC{
	
	private Collection<SDGNode> preds;

	public SoucisSlicingBasedIFC(SDG sdg, IStaticLattice<String> lattice,
			Slicer slicerForw, Slicer slicerBackw, Collection<SDGNode> preds) {
		super(sdg, lattice, slicerForw, slicerBackw);
		this.preds = preds;
	}
	
	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.IFC#checkIFlow()
	 */
	@Override
	public Collection<ClassifiedViolation> checkIFlow() throws NotInLatticeException {
		System.out.println("SOURCIS check flow");
		this.slicer = slicerBackw;
		Collection<SecurityNode> sources = collectStartpoints();
		DEBUG.outln(String.format("[%s] Executing slicing-based IFC on a graph with %d nodes and %d edges.", Calendar.getInstance().getTime(), this.g.vertexSet().size(), this.g.edgeSet().size()));
		DEBUG.outln(String.format("[%s] Collecting sinks...", Calendar.getInstance().getTime()));
		Collection<SecurityNode> sinks = collectEndpoints();
		DEBUG.outln(String.format("[%s] done. Collected %d sinks.", Calendar.getInstance().getTime(), sinks.size()));
		DEBUG.outln(String.format("[%s] Collecting sources...", Calendar.getInstance().getTime()));
		DEBUG.outln(String.format("[%s] done. Collected %d sources.", Calendar.getInstance().getTime(), sources.size()));
		Collection<SecurityNode> endPoints;
		String endpointsStr;
		if (sources.size() < sinks.size()) {
			this.slicer = slicerForw;
			endPoints = sources;
			endpointsStr = "sources";
			DEBUG.outln(String.format("[%s] Using forward slicing.", Calendar.getInstance().getTime()));
		} else {
			this.slicer = slicerBackw;
			endPoints = sinks;
			endpointsStr = "sinks";
			DEBUG.outln(String.format("[%s] Using backward slicing.", Calendar.getInstance().getTime()));
		}
		Collection<ClassifiedViolation> vios = new LinkedList<ClassifiedViolation>();
		DEBUG.outln(String.format("[%s] slicing each of the %d %s...", Calendar.getInstance().getTime(), endPoints.size(), endpointsStr));
		int count = 0;
		for (SecurityNode endPoint : endPoints) {
			count++;
			DEBUG.outln(String.format("[%s] %d of %d...", Calendar.getInstance().getTime(), count, endPoints.size()));
			Collection<SDGNode> slice = slicer.slice(endPoint);
			DEBUG.outln(String.format("[%s] done. Slice contains %d items", Calendar.getInstance().getTime(), slice.size()));
			DEBUG.outln(String.format("[%s] scanning for sources...", Calendar.getInstance().getTime()));
			addPossibleViolations(endPoint, slice, vios);
			DEBUG.outln(String.format("[%s] done.", Calendar.getInstance().getTime()));
		}
		DEBUG.outln(String.format("[%s] done. Found %d violation(s).", Calendar.getInstance().getTime(), vios.size()));
		return vios;
	}

	private Collection<SecurityNode> collectStartpoints() {
		Collection<SecurityNode> ret = new LinkedList<SecurityNode>();
		for (SDGNode n : this.g.vertexSet()) {
			SecurityNode sN = (SecurityNode) n;
			if (isStartpoint(sN)) {
				ret.add(sN);
			}
		}
		return ret;
	}

	private Collection<SecurityNode> collectEndpoints() {
		Collection<SecurityNode> ret = new LinkedList<SecurityNode>();
		for (SDGNode n : this.g.vertexSet()) {
			SecurityNode sN = (SecurityNode) n;
			if (isEndpoint(sN)) {
				ret.add(sN);
			}
		}
		return ret;
	}

	private boolean isEndpoint(SecurityNode n) {
		switch (slicer.getDirection()) {
		case BACKWARD:
			return n.isInformationSink();
		case FORWARD:
			return n.isInformationSource();
		default:
			throw new IllegalStateException("unhandled case: " + slicer.getDirection());
		}
	}
	
	private boolean isStartpoint(SecurityNode n) {
		switch (slicer.getDirection()) {
		case BACKWARD:
			return n.isInformationSource();
		case FORWARD:
			return n.isInformationSink();
		default:
			throw new IllegalStateException("unhandled case: " + slicer.getDirection());
		}
	}

	private void addPossibleViolations(SecurityNode endPoint, Collection<SDGNode> slice, Collection<ClassifiedViolation> vios) {
		for (SDGNode n : slice) {
			SecurityNode sNode = (SecurityNode) n;
			String secLevelOfOtherEndpoint = getLevel(sNode);
			String secLevelOfEndpoint = getLevel(endPoint);
			if (isStartpoint(sNode) && secLevelOfOtherEndpoint != null && isLeakage(endPoint, sNode)) {
				if (endPoint.isInformationSource() && sNode.isInformationSink()) {
					System.out.println(sNode.getLabel());
					checkDependenceonSlice(sNode, slice);
					vios.add(ClassifiedViolation.createViolation(sNode, endPoint, secLevelOfOtherEndpoint));
				} else if (endPoint.isInformationSink() && sNode.isInformationSource()) {
					System.out.println(endPoint.getLabel());
					checkDependenceonSlice(endPoint, slice);
					vios.add(ClassifiedViolation.createViolation(endPoint, sNode, secLevelOfEndpoint));
				}
			}
		}
	}

	private boolean isLeakage(SecurityNode n1, SecurityNode n2) {
		if (!(xor(n1.isInformationSource(), n2.isInformationSource()) && xor(n1.isInformationSink(), n2.isInformationSink()) && !n1.isDeclassification() && !n2.isDeclassification())) {
			throw new IllegalArgumentException("Exactly one of the provided nodes must be an information source, the other must be an information sink!");
		}
		SecurityNode src, snk;
		if (n1.isInformationSource()) {
			src = n1;
			snk = n2;
		} else {
			src = n2;
			snk = n1;
		}
		
		return !l.leastUpperBound(src.getProvided(), snk.getRequired()).equals(snk.getRequired());
	}
	
	private static boolean xor(boolean b1, boolean b2) {
		return (b1 || b2) && !(b1 && b2);
	}
	
	private static String getLevel(SecurityNode secNode) {
		if (secNode.isInformationSource()) {
			return secNode.getProvided();
		} else {
			return secNode.getRequired();
		}
	}
	
	private void checkDependenceonSlice(SecurityNode sink, Collection<SDGNode> slice) {
		
		System.out.println(sink.getBytecodeMethod());
		
		Iterator<SDGEdge> edgeIt = g.incomingEdgesOf(sink).iterator();
		
		while(edgeIt.hasNext()) {
			SDGEdge edge = edgeIt.next();
			
			if(edge.getKind().equals(SDGEdge.Kind.CONTROL_DEP_CALL) || edge.getKind().equals(SDGEdge.Kind.CONTROL_DEP_COND) || edge.getKind().equals(SDGEdge.Kind.CONTROL_DEP_EXPR) || edge.getKind().equals(SDGEdge.Kind.CONTROL_DEP_UNCOND)) {
				SDGNode source = edge.getSource();
				if(slice.contains(source)) {
					System.out.println("control dependence on " + source.getLabel() + " " + edge.getKind());
//					return;
				}
			}
		}
		
		for(SDGNode pred : preds) {
			if(slice.contains(pred)) {
				System.out.println("data dependence on " + pred);
			}
		}
		
		return;
	}
	
}
