package at.jku.isse.ecco.storage.mem.tree;

import at.jku.isse.ecco.EccoException;
import at.jku.isse.ecco.artifact.Artifact;
import at.jku.isse.ecco.core.Association;
import at.jku.isse.ecco.dao.EntityFactory;
import at.jku.isse.ecco.featuretrace.FeatureTrace;
import at.jku.isse.ecco.featuretrace.FeatureTraceCondition;
import at.jku.isse.ecco.storage.mem.featuretrace.MemFeatureTrace;
import at.jku.isse.ecco.tree.Node;
import at.jku.isse.ecco.util.Location;
import org.eclipse.collections.impl.factory.Maps;
import org.logicng.formulas.FormulaFactory;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

public class MemNode implements Node, Node.Op {

	public static final long serialVersionUID = 1L;

	private boolean unique = true;

	private List<Op> children = new ArrayList<>();

	private Artifact.Op<?> artifact = null;

	private Op parent = null;

	private FeatureTrace featureTrace;

	private Location location;

	public Op copySingleNode(){
		return new MemNode(this.artifact);
	}

	@Override
	public Op copySingleNodeCompletely() {
		MemNode.Op newNode = new MemNode(this.artifact);
		newNode.setLocation(this.location);
		newNode.getFeatureTrace().setUserCondition(this.featureTrace.getUserConditionString());
		newNode.getFeatureTrace().setDiffCondition(this.featureTrace.getDiffConditionString());
		return newNode;
	}

	@Override
	public Op getEqualChild(Op template) {
		Collection<Node.Op> children = this.getChildren();
		for (Node.Op child : children){
			if (child.getArtifact() == template.getArtifact()){
				return child;
			}
		}
		return null;
	}

	@Override
	public FeatureTrace getFeatureTrace() {
		return this.featureTrace;
	}

	@Override
	public Location getLocation() {
		return this.location;
	}

	@Override
	public void setLocation(Location location){
		this.location = location;
	}

	@Deprecated
	public MemNode() {
	}

	public MemNode(Artifact.Op<?> artifact) {
		this.artifact = artifact;
		this.featureTrace = new MemFeatureTrace(this);
	}

	@Override
	public Op createNode(Artifact.Op<?> artifact) {
		return new MemNode(artifact);
	}

	@Override
	public boolean isAtomic() {
		if (this.artifact != null)
			return this.artifact.isAtomic();
		else
			return false;
	}

	@Override
	public Association.Op getContainingAssociation() {
		if (this.parent == null)
			return null;
		else
			return this.parent.getContainingAssociation();
	}

	@Override
	public Artifact.Op<?> getArtifact() {
		return artifact;
	}

	@Override
	public void setArtifact(Artifact.Op<?> artifact) {
		this.artifact = artifact;
	}

	@Override
	public Op getParent() {
		return parent;
	}

	@Override
	public void setParent(Op parent) {
		this.parent = parent;
	}

	@Override
	public boolean isUnique() {
		return this.unique;
	}

	@Override
	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	@Override
	public void addChild(Op child) {
		checkNotNull(child);

		if (this.getArtifact() != null && !this.getArtifact().isOrdered() && this.children.contains(child))
			throw new EccoException("An equivalent child is already contained. If multiple equivalent children are allowed use an ordered node.");

		this.children.add(child);
		child.setParent(this);
	}

	@Override
	public void addChildren(Op... children) {
		for (Op child : children)
			this.addChild(child);
	}

	@Override
	public void removeChild(Op child) {
		checkNotNull(child);

		if (this.children.remove(child))
			child.setParent(null);
		else
			throw new EccoException("Attempted to remove child that does not exist.");
	}

	@Override
	public void removeChildren(){
		this.children = new ArrayList<>();
	}

	@Override
	public List<Op> getChildren() {
		return this.children;
	}

	@Override
	public int hashCode() {
		return this.getArtifact() != null ? this.getArtifact().hashCode() : 0;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if (other == null) return false;
		if (!(other instanceof Node)) return false;

		Node otherNode = (Node) other;

		if (this.getArtifact() == null)
			return otherNode.getArtifact() == null;

		return this.getArtifact().equals(otherNode.getArtifact());
	}

	@Override
	public String toString() {
		return this.getNodeString();
	}

	private transient Map<String, Object> properties = null;

	@Override
	public Map<String, Object> getProperties() {
		if (this.properties == null)
			this.properties = Maps.mutable.empty();
		return this.properties;
	}
}
