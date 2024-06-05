package at.jku.isse.ecco.adapter.c.parser;

import at.jku.isse.ecco.dao.EntityFactory;
import at.jku.isse.ecco.tree.Node;
import org.antlr.v4.runtime.tree.ParseTree;


public class CEccoTranslator extends CBaseVisitor<Node.Op>{

    private String[] codeLines;

    private EntityFactory entityFactory;


    public CEccoTranslator(String[] codeLines, EntityFactory entityFactory){
        this.codeLines = codeLines;
        this.entityFactory = entityFactory;
    }

    public Node.Op translate(ParseTree tree){
        return tree.accept(this);
    }

    @Override
    public Node.Op visitTranslationUnit(CParser.TranslationUnitContext ctx){
        // TODO
        // if the uppermost node is not ordered, insert an ordered node

        // get filenumbers that belong to functions
        // get filenumbers that don't belong to functions

        // order the functions according to start line
        // loop: add all lines before next function and then visit function



        return null;
    }

    @Override
    public Node.Op visitFunctionDefinition(CParser.FunctionDefinitionContext ctx){
        // TODO

        // create function node with function signature as data
        // add all lines that belong to function as children

        return null;
    }

}
