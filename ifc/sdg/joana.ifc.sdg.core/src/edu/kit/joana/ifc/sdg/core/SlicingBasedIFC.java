/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.core.DirectedSlicer.Direction;
import edu.kit.joana.ifc.sdg.core.conc.BarrierIFCSlicer;
import edu.kit.joana.ifc.sdg.core.conc.ProbabilisticNIChecker;
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.WALAVarDefLoc;
import edu.kit.joana.ifc.sdg.graph.WALAVarLoc;
import edu.kit.joana.ifc.sdg.graph.slicer.ContextSlicerBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.Slicer;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.NotInLatticeException;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;

/**
 * This class provides an IFC algorithm based on a given slicing algorithm, which is interchangeable. 
 * This is in contrast to e.g. {@link BarrierIFCSlicer} where slicing and IFC are tightly coupled to achieve better performance.<p>
 * The algorithm can be configured to use a backward slicer or a forward slicer. 
 * The backward variant of the algorithm proceeds as follows. For each information sink t do:<p>
 * <ol>
 * <li> Compute backward slice of t. </li>
 * <li> Check whether there is an information source s whose security level is strictly higher than or incomparable to the security level of t.
 * If yes, report a violation for (s,t).</li>
 * </ol>
 * For the forward variant, the forward slice of each source is computed and a malicious sink is searched for.<p>
 * Note, that this class can only be used to check for possibilistic leaks. To check also for probabilistic leaks, use {@link ProbabilisticNIChecker}.<br>
 * Also, there is no support for declassification, unless the provided chopper is configured appropriately.
 * @author Martin Mohr
 */
public class SlicingBasedIFC extends IFC {

	protected static final Logger DEBUG = Log.getLogger(Log.L_IFC_DEBUG);

	protected final DirectedSlicer slicerForw;
	protected final DirectedSlicer slicerBackw;
	protected DirectedSlicer slicer;
	
	/**
	 * Instantiates a new SlicingBasedIFC algorithm. 
	 * Note, that this class relies on the used {@link Slicer} being consistent with the used direction. For example, a {@link ContextSlicerBackward} is
	 * inconsistent with {@link DirectedSlicer.Direction#FORWARD}, since {@link ContextSlicerBackward} is a backward slicer.
	 * @param sdg the sdg to perform slicing-based IFC on
	 * @param lattice the security lattice used for IFC
	 * @param slicer the slicer to be used for IFC
	 * @param dir additional information which denotes whether the given slicer is a backward slicer or a forward slicer - note that the algorithm will
	 * produce wrong results if the given direction is not consistent with the given slicer.
	 */
	public SlicingBasedIFC(SDG sdg, IStaticLattice<String> lattice, Slicer slicerForw, Slicer slicerBackw) {
		super(sdg, lattice);
		this.slicerForw = DirectedSlicer.decorateWithDirection(slicerForw, Direction.FORWARD);
		this.slicerBackw = DirectedSlicer.decorateWithDirection(slicerBackw, Direction.BACKWARD);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.IFC#checkIFlow()
	 */
	@Override
	public Collection<ClassifiedViolation> checkIFlow() throws NotInLatticeException {
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
		
		//at this point, our implementation should support only forward slicing (because the backward slicing from conditional instruction will always reach defining statements via DD?)
		
//		if (sources.size() < sinks.size()) {
			this.slicer = slicerForw;
			endPoints = sources;
			endpointsStr = "sources";
			DEBUG.outln(String.format("[%s] Using forward slicing.", Calendar.getInstance().getTime()));
//		} else {
//			this.slicer = slicerBackw;
//			endPoints = sinks;
//			endpointsStr = "sinks";
//			DEBUG.outln(String.format("[%s] Using backward slicing.", Calendar.getInstance().getTime()));
//		}
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
//					System.out.println(sNode.getLabel());
//					checkDependenceonSlice(sNode, slice);
					vios.add(ClassifiedViolation.createViolation(sNode, endPoint, secLevelOfOtherEndpoint));
				} else if (endPoint.isInformationSink() && sNode.isInformationSource()) {
//					System.out.println(endPoint.getLabel());
//					checkDependenceonSlice(endPoint, slice);
					vios.add(ClassifiedViolation.createViolation(endPoint, sNode, secLevelOfEndpoint));
				}
			}
		}
	}
	
	private void addPossibleTaintedLocs(SecurityNode endPoint, Collection<SDGNode> slice, Collection<WALAVarLoc> vios) {
		for (SDGNode n : slice) {
			SecurityNode sNode = (SecurityNode) n;
			String secLevelOfOtherEndpoint = getLevel(sNode);
			String secLevelOfEndpoint = getLevel(endPoint);
			if (isStartpoint(sNode) && secLevelOfOtherEndpoint != null && isLeakage(endPoint, sNode)) {
				if (endPoint.isInformationSource() && sNode.isInformationSink()) {
					checkDependenceonSlice(sNode, slice, vios);
				} else if (endPoint.isInformationSink() && sNode.isInformationSource()) {
					checkDependenceonSlice(endPoint, slice, vios);
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
	
	private void checkDependenceonSlice(SecurityNode sink, Collection<SDGNode> slice, Collection<WALAVarLoc> vios) {
				
//		for(SDGNode node : slice) {
//			System.out.println(node.getLabel());
//		}
		
//		System.out.println("sink: " + sink.getLabel() + " " + sink.getSource());
		
//		if(sink.getSource().toString().contains("Simple.java")){
//			for(SDGNode node : slice) {
//				System.out.println(node.getLabel());
//			}
//		}
		
//		Iterator<SDGEdge> edgeIt = g.incomingEdgesOf(sink).iterator();
//		
//		while(edgeIt.hasNext()) {
//			SDGEdge edge = edgeIt.next();
//			
//			if(edge.getKind().equals(SDGEdge.Kind.CONTROL_DEP_CALL) || edge.getKind().equals(SDGEdge.Kind.CONTROL_DEP_COND) || edge.getKind().equals(SDGEdge.Kind.CONTROL_DEP_EXPR) || edge.getKind().equals(SDGEdge.Kind.CONTROL_DEP_UNCOND)) {
//				SDGNode source = edge.getSource();
//				if(slice.contains(source)) {
//					System.out.println("control dependence on " + source.getLabel() + " " + edge.getKind());
//					return;
//				}
//			}
//		}
		
//		Collection<SDGNode> defNodes = g.getDefNodes(sink.getId());
//				
//		for(SDGNode defNode : defNodes) {
//			
//			System.out.println("defnode: " + defNode.getLabel());
//			
//			if(slice.contains(defNode)) {
////				for(SDGNode node : slice) {
////					if(node.getLabel().contains("v6"))
////						System.out.println("look at: " + node.getLabel());
////				}
//				System.out.println("data dependence on " + defNode.getLabel());
//			}
//		}
		
//		for(SDGNode pred : preds) {
//			if(slice.contains(pred)) {
//				System.out.println("data dependence on " + pred);
//			}
//		}
		
//		Collection<WALAVarLoc> ret= new ArrayList<WALAVarLoc>();
		
		Collection<WALAVarDefLoc> vdlocs = g.getVDLocs(sink.getId());
		
		if(vdlocs!=null){ 
			for(WALAVarDefLoc vdloc : vdlocs) {
				SDGNode node = g.getNode(vdloc.getNodeId());
				
				if(slice.contains(node)) {
//					WALAVarLoc vloc = new WALAVarLoc(vdloc.getValunum(), vdloc.getWALAIRLoc());
					
					WALAVarLoc vloc = vdloc.getWALAVarLoc();
					
					if(!vios.contains(vloc))
						vios.add(vloc);
				}
			}
		}
		
//		return ret;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.IFC#checkIFlow2wala()
	 */
	@Override
	public Collection<WALAVarLoc> checkIFlow2wala() throws NotInLatticeException {
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
		Collection<WALAVarLoc> vios = new ArrayList<WALAVarLoc>();
		DEBUG.outln(String.format("[%s] slicing each of the %d %s...", Calendar.getInstance().getTime(), endPoints.size(), endpointsStr));
		int count = 0;
		for (SecurityNode endPoint : endPoints) {
			count++;
			DEBUG.outln(String.format("[%s] %d of %d...", Calendar.getInstance().getTime(), count, endPoints.size()));
			Collection<SDGNode> slice = slicer.slice(endPoint);
			DEBUG.outln(String.format("[%s] done. Slice contains %d items", Calendar.getInstance().getTime(), slice.size()));
			DEBUG.outln(String.format("[%s] scanning for sources...", Calendar.getInstance().getTime()));
//			addPossibleViolations(endPoint, slice, vios);
			addPossibleTaintedLocs(endPoint, slice, vios);
			DEBUG.outln(String.format("[%s] done.", Calendar.getInstance().getTime()));
		}
		DEBUG.outln(String.format("[%s] done. Found %d violation(s).", Calendar.getInstance().getTime(), vios.size()));
		return vios;
	}
	
//	private void checkDependenceonSlice(SecurityNode sink, Collection<SDGNode> slice) {
//		
//		System.out.println(sink.getBytecodeMethod());
//		
//		Iterator<SDGEdge> edgeIt = g.incomingEdgesOf(sink).iterator();
//		
//		while(edgeIt.hasNext()) {
//			SDGEdge edge = edgeIt.next();
//			
//			if(edge.getKind().equals(SDGEdge.Kind.CONTROL_DEP_CALL) || edge.getKind().equals(SDGEdge.Kind.CONTROL_DEP_COND) || edge.getKind().equals(SDGEdge.Kind.CONTROL_DEP_EXPR) || edge.getKind().equals(SDGEdge.Kind.CONTROL_DEP_UNCOND)) {
//				SDGNode source = edge.getSource();
//				if(slice.contains(source)) {
//					System.out.println("control dependence");
////					return;
//				}
//			} else if (edge.getKind().equals(SDGEdge.Kind.DATA_DEP)) {
//				SDGNode source = edge.getSource();
//				if(slice.contains(source)) {
//					System.out.println("data dependence on " + source.getLabel());
//					System.out.println(source.getBytecodeIndex());
//					System.out.println(source.getBytecodeMethod());
//					
//				}
//			}
//		}
//		return;
//	}
	
}
