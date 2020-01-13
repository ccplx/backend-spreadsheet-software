// HW 3 Final Tests: Runs CellTests DAGTests SpreadSheetTests
import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test; // fixes some compile problems with annotations

public class HW3FinalTests {
  /*Main method runs tests other files*/ 
  public static void main(String args[])
  {
    org.junit.runner.JUnitCore.main("CellTests","DAGTests","SpreadsheetTests");
  } 
}
