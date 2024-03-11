package at.jku.isse.ecco.test;

import at.jku.isse.ecco.dao.*;
import at.jku.isse.ecco.storage.mem.dao.*;
import at.jku.isse.ecco.test.util.NodeUtil;
import at.jku.isse.ecco.tree.*;
import at.jku.isse.ecco.util.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class TreesTest {

	@Test
	public void Trees_Full() {
		EntityFactory ef = new MemEntityFactory();

		Node.Op root1 = NodeUtil.createTestTree1();
		Node.Op root2 = NodeUtil.createTestTree2();

		Trees.checkConsistency(root1); // TODO: have this return true/false or an error code instead of throwing an exception?
		Trees.checkConsistency(root2);

		assertEquals(Trees.countArtifacts(root1), 16);
		assertEquals(Trees.countArtifacts(root2), 17);

		Trees.print(root1);
		Trees.print(root2);

		Trees.updateArtifactReferences(root1);
		Trees.updateArtifactReferences(root2);

		Trees.map(root1, root2);

		//Node.Op copy1 = EccoUtil.deepCopyTree(root1, ef);

		Node.Op root3 = Trees.slice(root1, root2);

		Trees.sequence(root1);
		Trees.sequence(root2);
		Trees.sequence(root3);

		//NodeOperator.NodeOperand merge1 = Trees.merge(root1, root3); // TODO: should it be like this?

		//Trees.merge(root1, root3);
		root1.merge(root3);

		//Trees.equals(root1, copy1);
		//root1.merge(copy1); // TODO: this fails... why?


		// TODO: other operations

	}

	@Test
	public void testCopy(){
		Node.Op tree = NodeUtil.createTestTree1();
		Node.Op copyTree = tree.copyTree();
		assertTrue(Trees.equals(tree, copyTree));
	}

	@Test
	public void testCopyWithMiddleNode(){
		Node.Op tree = NodeUtil.createTestTree1();
		Node.Op middleNode = tree.getChildren().get(0).getChildren().get(1);
		Node.Op copyMiddleNode = middleNode.copyTree();
		Node.Op copyRoot = copyMiddleNode;
		while (!(copyRoot instanceof RootNode)){
			copyRoot = copyRoot.getParent();
		}
		assertTrue(Trees.equals(tree, copyRoot));
	}

	@Test
	public void testCreatePathSkeleton(){
		Node.Op tree = NodeUtil.createTestTree1();
		Node.Op middleNode = tree.getChildren().get(0).getChildren().get(1);
		Node.Op pathSkeleton = middleNode.createPathSkeleton();

		assertTrue(pathSkeleton.isUnique());
		assertFalse(pathSkeleton.getParent().isUnique());
		// root nodes are always true
		assertTrue(pathSkeleton.getParent().getParent().isUnique());
		assertTrue(pathSkeleton.getChildren().isEmpty());
        assertEquals(1, pathSkeleton.getParent().getChildren().size());
		assertEquals(1, pathSkeleton.getParent().getParent().getChildren().size());
	}

	@BeforeEach
	public void beforeTest() {
		System.out.println("BEFORE");
	}

	@AfterEach
	public void afterTest() {
		System.out.println("AFTER");
	}

}
