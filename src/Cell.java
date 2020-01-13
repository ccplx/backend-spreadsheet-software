import java.util.*;

public class Cell {
    // Class of cell type
    // cell can be either a formula cell, number cell, or string cell
    // memory complexity: O(1)
    // for Formula: O(N), where N is the size of the formula tree

    // holds contents of cell
    protected String contents;

    // holds number value of cell
    // if string or unevaluated formula than this is null
    protected Double numberValue;

    // holds the root of a formula tree for the formula cell
    protected FNode formulaTree;

    // the kind can be either a number, formula or string
    protected String kind;

    // make function can be used to create different cell types based on input
    public static Cell make(String contents){

        if (contents == null)
            return null;
        contents = contents.trim();
        try {
            // if parseDouble works, the cell is a number
            Double num = Double.parseDouble(contents);
            Cell numCell = new Cell();
            numCell.kind = "number";
            numCell.numberValue = num;
            numCell.formulaTree = null;
            numCell.contents = contents;
            return numCell;
        } catch(Exception e) {
            // if cell is formula:
            if (contents.charAt(0) == '=') {
                Cell formulaCell = new Cell();
                formulaCell.kind = "formula";
                formulaCell.numberValue = null;
                FNode root = FNode.parseFormulaString(contents);
                formulaCell.formulaTree = root;
                formulaCell.contents = contents;
                return formulaCell;
            }
            else{
                // if cell is string
                Cell stringCell = new Cell();
                stringCell.kind = "string";
                stringCell.numberValue = null;
                stringCell.formulaTree = null;
                stringCell.contents = contents;
                return stringCell;
            }

        }
    }

    // returns the kind of the cell
    public String kind(){
        return kind;
    }

    // returns contents of cell
    public String contents(){
        return contents;
    }

    // returns whether cell is currently in error state
    public boolean isError(){
        if (kind.equals("formula")){
            if (numberValue == null){
                return true;
            }
        }
        return false;
    }

    // displays the string of the cell
    // if cell is number or formula, displays number formatted to one decimal place
    // if cell is in error state, "ERROR" is displayed
    public String displayString(){
        if (kind.equals("number") && numberValue()!=null){
            return String.format("%.1f",new Double(contents));
        } else if (kind.equals("string")){
            return contents;
        } else {
            if (numberValue == null){
                return "ERROR";
            } else {
                return String.format("%.1f",new Double(numberValue));
            }
        }
    }

    // displays the number value of cell
    public Double numberValue(){

        return numberValue;
    }

    // updates the value in a formula cell
    // if cell is not a formula, nothing
    // Runtime Complexity:
    //   O(1) for "number" and "string" cells
    //   O(T) for "formula" nodes where T is the number of nodes in the
    //        formula tree
    public void updateValue(Map<String,Cell> cellMap){

        if (kind.equals("formula")) {

            try {
                numberValue = evalFormulaTree(this.formulaTree, cellMap);
            } catch (EvalFormulaException e) {
                numberValue = null;

            }

        }


    }

    // A simple class to reflect problems evaluating a formula tree.
    public static class EvalFormulaException extends RuntimeException{

        public EvalFormulaException(String msg){

            super(msg);
        }


    }

    // Recursively evaluate the formula tree rooted at the given
    // node. Returns the computed value.
    // Runtime Complexity: O(T) --> this is because the algorithm traverses the entire tree using post-order traversal
    //   T: the number of nodes in the formula tree
    public static Double evalFormulaTree(FNode node, Map<String,Cell> cellMap){
        if(node.type == TokenType.Plus){
            Double leftVal = evalFormulaTree(node.left, cellMap);
            Double rightVal = evalFormulaTree(node.right, cellMap);
            return leftVal + rightVal;
        }
        else if(node.type == TokenType.Minus){
            Double leftVal = evalFormulaTree(node.left, cellMap);
            Double rightVal = evalFormulaTree(node.right, cellMap);
            return leftVal - rightVal;
        }
        else if(node.type == TokenType.Multiply) {
            Double leftVal = evalFormulaTree(node.left, cellMap);
            Double rightVal = evalFormulaTree(node.right, cellMap);
            return leftVal * rightVal;
            // Cases for multiply, divide, negate
        }
        else if(node.type == TokenType.Divide) {
            Double leftVal = evalFormulaTree(node.left, cellMap);
            Double rightVal = evalFormulaTree(node.right, cellMap);
            return leftVal / rightVal;
            // Cases for multiply, divide, negate
        }
        else if(node.type == TokenType.Negate) {
            Double leftVal = evalFormulaTree(node.left, cellMap);
            return -1*leftVal;
            // Cases for multiply, divide, negate
        }
        else if(node.type == TokenType.Number){
            return new Double(node.data);
            // node.data contains a string of a number
            // converts it to a double and return
        }
        else if(node.type == TokenType.CellID){
            Cell temp = cellMap.get(node.data);
            if (temp == null || temp.kind.equals("string") || temp.numberValue() == null){
                throw new EvalFormulaException(node.data+ " was id not set");
            }
            return temp.numberValue();

        }
        else{
            throw new EvalFormulaException("Error with TokenType ’"+node.type+"’");
        }

    }

    // returns the upstream IDs
    public Set<String> getUpstreamIDs(){
        Set<String> cells = new HashSet<>();
        if (kind.equals("formula"))
            getUpstreamIDs(formulaTree, cells);
        return cells;
    }

    // Return a set of upstream cells from this cell. Cells of kind
    // "string" and "number" return an empty set.
    // Runtime Complexity: O(T) --> looks through every node
    //   T: the number of nodes in the formula tree
    protected void getUpstreamIDs(FNode node, Set<String> c){
        if (node == null){
            return;
        } else if(node.type == TokenType.CellID){
            c.add(node.data);
        } else {
            getUpstreamIDs(node.left,c);
            getUpstreamIDs(node.right,c);
        }

    }



}
