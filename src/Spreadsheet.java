import java.util.Iterator;
import java.util.*;

// Basic model for a spreadsheet. Allows cells to be set by specifying their ID
public class Spreadsheet{

    // DAG is useful for checking for any cycles in spreadsheet
    protected DAG dag;
    // Map contains all cells in spreadsheet along with their Ids
    protected Map<String,Cell> cellMap;

    // Constructs a new empty spreadsheet
    // by creating an empty DAG and empty Map
    public Spreadsheet(){
        dag = new DAG();
        cellMap = new HashMap<>();
    }

    // String representation of spreadsheet
    // Runtime: O(C+U+D)
    // C: number of cells, U: size of upstreamLinks in dag
    // D: size of downstreamLinks in dag
    public String toString(){

        StringBuilder sb = new StringBuilder();

        sb.append("    ID |  Value | Contents\n");
        sb.append("-------+--------+---------------\n");

        Set<String> ids = cellMap.keySet();

        Iterator<String> iter = ids.iterator();

        while (iter.hasNext()){
            String id = iter.next();
            Cell c = cellMap.get(id);
            sb.append(String.format("%6s | %6s | '%s'\n", id, c.displayString(), c.contents()));
        }

        sb.append("\nCell Dependencies\n" + dag.toString());

        return sb.toString();

    }

    // Check if a cell ID is well formatted.  It must match the regular
    // expression : Capital Letter + Number from 0-9 + Number from 0-9
    public static void verifyIDFormat(String id){
        if (!id.matches("^[A-Z]+[1-9][0-9]*$")){
            throw new RuntimeException("Not a valid ID");
        }
    }

    // Retrieve a string which should be displayed for the value of the
    // cell with the given ID. Return "" if the specified cell is empty.
    // Runtime: O(1)
    public String getCellDisplayString(String id){
        Cell cell =  cellMap.get(id);
        if (cell == null){
            return "";
        }
        return cell.displayString();
    }

    // Retrieve a string which is the actual contents of the cell with
    // the given ID. Return "" if the specified cell is empty.
    // Runtime: O(1)
    public String getCellContents(String id){
        Cell cell =  cellMap.get(id);
        if (cell == null){
            return "";
        }
        return cell.contents();
    }


    // Delete the contents of the cell with the given ID. Update all
    // downstream cells of the change. If specified cell is empty, do
    // nothing.
    public void deleteCell(String id){

        cellMap.remove(id);
        notifyDownstreamOfChange(id);
        dag.remove(id);

    }

    // Sets the given cell with the given contents. If contents is "" or
    // null, delete the cell indicated.
    public void setCell(String id, String contents){

        if (contents == null || contents.equals("")){
            deleteCell(id);
            return;
        }

        Cell cell = Cell.make(contents);

        Set<String> upDependencies = cell.getUpstreamIDs();


        dag.add(id, upDependencies);

        cellMap.put(id, cell);

        cell.updateValue(cellMap);

        notifyDownstreamOfChange(id);

    }

    // updates all downstream links after id has been updated
    // and performs the same for those downstreamlinks (recursive)
    // O(D) --> D: number of downstream links
    public void notifyDownstreamOfChange(String id){
        Set<String> downStreamLinks = dag.getDownstreamLinks(id);

        for (String downStreamId: downStreamLinks){
            Cell downCell = cellMap.get(downStreamId);
            if (downCell != null){
                downCell.updateValue(cellMap);
                notifyDownstreamOfChange(downStreamId);
            }
        }
    }

}