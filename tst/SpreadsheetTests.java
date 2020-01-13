// Initial Release: Wed Jul 12 17:41:05 EDT 2017 
import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test; // fixes some compile problems with annotations
import org.junit.Rule;
import org.junit.rules.Timeout;

public class SpreadsheetTests {
  /*Main method runs tests in this file*/ 
  public static void main(String args[])
  {
    org.junit.runner.JUnitCore.main("SpreadsheetTests");
  } 

  // Global timeout for all tests: use argument to Timeout.millis( __ );
  @Rule public Timeout globalTimeout = Timeout.millis(1000); 

  // Utility to creat a set based on arguments. Invoke as:
  //
  //   Set<String> set = toSet("B1","C1","D1");
  // 
  public static Set<String> toSet(String... args){
    Set<String> set = new HashSet<String>();
    for(String s : args){
      set.add(s);
    }
    return set;
  }

  // Utility to create a cell map based on arguments. Invoke as
  // 
  //   Map<String.Cell> cellMap = cellMap("A1","2.0","CX5","5.22");
  // 
  // Does not update any values in the cell so it
  public static Map<String,Cell> cellMap(String... args){
    Map<String,Cell> cellMap = new HashMap<String,Cell>();
    for(int i=0; i<args.length; i+=2){
      String id = args[i];
      Cell cell = Cell.make(args[i+1]);
      cellMap.put(id,cell);
    }
    return cellMap;
  }

  // Utility to sort a line involving a DAG link to get the links in
  // sorted order for comparison
  //
  // sortLine("A1 : [C1, E1, B1, D1]")
  //      --> "A1 : [B1, C1, D1, E1]"
  public static String sortLine(String s){
    String prefix = s.substring(0,7);
    String [] list = s.substring(8).replaceAll("]","").replaceAll(",","").split(" ");
    Arrays.sort(list);
    String listStr = Arrays.toString(list);
    return prefix + listStr;
  }

  // Utility to sort the lines of the Spreadsheet string to produce a
  // string which is independent of the order cells/links may appear
  // in the string. Uses sortedLine to order lines corresponding to
  // DAG links.
  public static String sortedLines(String s){
    try{
      StringBuilder sorted = new StringBuilder();
      String ulinks = 
        "\n"+
        "Cell Dependencies\n"+
        "Upstream Links:\n"+
        "";
      String cellMap =
        "    ID |  Value | Contents\n"+
        "-------+--------+---------------\n"+
        "";
      String dlinks = "Downstream Links:\n";

      int cellMapIdx = s.indexOf(cellMap);
      int ulinksIdx = s.indexOf(ulinks);
      int dlinksIdx = s.indexOf(dlinks);

      String [] cellMapLines = s.substring(cellMapIdx + cellMap.length(), ulinksIdx).split("\n");
      Arrays.sort(cellMapLines);
      sorted.append(cellMap);
      for(String c : cellMapLines){
        sorted.append(c); sorted.append('\n');
      }

      String [] ulinksLines = s.substring(ulinksIdx+ulinks.length(), dlinksIdx).split("\n");
      Arrays.sort(ulinksLines);
      sorted.append(ulinks);
      for(String u : ulinksLines){
        sorted.append(sortLine(u)); sorted.append('\n');
      }

      String [] dlinksLines = s.substring(dlinksIdx+dlinks.length(), s.length()).split("\n");
      Arrays.sort(dlinksLines);
      sorted.append(dlinks);
      for(String d : dlinksLines){
        sorted.append(sortLine(d)); sorted.append('\n');
      }

      return sorted.toString();
    }
    catch(StringIndexOutOfBoundsException e){
      return "UNABLE TO SORT INPUT STRING\n"+s;
    }
  }


  // Utility to test whether verifyFormatID(..) works properly by
  // throwing exceptions when an incorrect id is checked
  public static void test_verifyIDFormat(String id, boolean expect){
    boolean actual=true;
    String excMsg="", msg;    
    Spreadsheet sheet = new Spreadsheet();
    try{
      Spreadsheet.verifyIDFormat(id);
    }
    catch(Exception e){
      actual = false;
      excMsg = e.getMessage();
    }
    msg =
      String.format("verifyIDFormat behaved unexpectedly\n") +
      String.format("id: %s\n",id) +
      String.format("Expect accept: %s\n",expect)+
      String.format("Actual accept: %s\n",actual)+
      String.format("Exception msg: %s\n",excMsg)+
      "";
    assertEquals(msg,expect,actual);
  }

  @Test public void spreadsheet_verifyIDFormat1(){
    test_verifyIDFormat("A1",true);
  }
  @Test public void spreadsheet_verifyIDFormat2(){
    test_verifyIDFormat("AAB1",true);
  }
  @Test public void spreadsheet_verifyIDFormat3(){
    test_verifyIDFormat("X23",true);
  }
  @Test public void spreadsheet_verifyIDFormat4(){
    test_verifyIDFormat("XYZ234",true);
  }
  @Test public void spreadsheet_verifyIDFormat5(){
    test_verifyIDFormat("A",false);
  }
  @Test public void spreadsheet_verifyIDFormat6(){
    test_verifyIDFormat("XYZ",false);
  }
  @Test public void spreadsheet_verifyIDFormat7(){
    test_verifyIDFormat("a123",false);
  }
  @Test public void spreadsheet_verifyIDFormat8(){
    test_verifyIDFormat("A01",false);
  }
  @Test public void spreadsheet_verifyIDFormat9(){
    test_verifyIDFormat("999",false);
  }
  @Test public void spreadsheet_verifyIDFormat10(){
    test_verifyIDFormat("HI",false);
  }

  // Utility which compares expected string to an actual spreadsheet;
  // sorts lines to avoid map order dependencies.
  public static void test_spreadsheet(Spreadsheet sheet, String expect_unsorted){
    String actual, expect, msg;
    actual = sortedLines(sheet.toString());
    expect = sortedLines(expect_unsorted);
    msg = String.format("Spreadsheet string wrong\nExpect:\n%s\nActual:\n%s\n",expect,actual);
    assertEquals(msg,expect, actual);
  }

  @Test public void sheet_empty1(){
    Spreadsheet sheet = new Spreadsheet();
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "Downstream Links:\n"+
      "";
    test_spreadsheet(sheet, expect);
  }

  @Test public void sheet_simple_set1(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("A1","1");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |    1.0 | '1'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "Downstream Links:\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_simple_set2(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("A1","1.5");
    sheet.setCell("B2","hello");
    sheet.setCell("AA5","4.98");
    sheet.setCell("CP30","22.234");
    sheet.setCell("BB8","12");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |    1.5 | '1.5'\n"+
      "    B2 |  hello | 'hello'\n"+
      "   AA5 |    5.0 | '4.98'\n"+
      "  CP30 |   22.2 | '22.234'\n"+
      "   BB8 |   12.0 | '12'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "Downstream Links:\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_simple_set3(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("CP30","22.234");
    sheet.setCell("AA5","4.98    ");
    sheet.setCell("A1","1.5");
    sheet.setCell("BB8","  12  ");
    sheet.setCell("D51"," dude ");
    sheet.setCell("DU6","  SWEET");
    sheet.setCell("L1","  42.42");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |    1.5 | '1.5'\n"+
      "   D51 |   dude | 'dude'\n"+
      "   DU6 |  SWEET | 'SWEET'\n"+
      "  CP30 |   22.2 | '22.234'\n"+
      "   AA5 |    5.0 | '4.98'\n"+
      "    L1 |   42.4 | '42.42'\n"+
      "   BB8 |   12.0 | '12'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "Downstream Links:\n"+
      "";
    test_spreadsheet(sheet, expect);
  }

  @Test public void sheet_simple_set_get1(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("CP30","  22.234  ");
    sheet.setCell("AA5","4.98  ");
    sheet.setCell("A1"," 1.5");
    sheet.setCell("BB8"," 12");
    sheet.setCell("D51"," dude ");
    sheet.setCell("DU6"," SWEET  ");
    sheet.setCell("L1","42.42");

    assertEquals("22.2",   sheet.getCellDisplayString("CP30"));
    assertEquals("5.0",    sheet.getCellDisplayString("AA5"));
    assertEquals("1.5",    sheet.getCellDisplayString("A1"));
    assertEquals("12.0",   sheet.getCellDisplayString("BB8"));
    assertEquals("dude",   sheet.getCellDisplayString("D51"));
    assertEquals("SWEET",  sheet.getCellDisplayString("DU6"));
    assertEquals("42.4",   sheet.getCellDisplayString("L1"));

    assertEquals("22.234", sheet.getCellContents("CP30"));
    assertEquals("4.98",   sheet.getCellContents("AA5"));
    assertEquals("1.5",    sheet.getCellContents("A1"));
    assertEquals("12",     sheet.getCellContents("BB8"));
    assertEquals("dude",   sheet.getCellContents("D51"));
    assertEquals("SWEET",  sheet.getCellContents("DU6"));
    assertEquals("42.42",  sheet.getCellContents("L1"));
  }

  @Test(expected=RuntimeException.class)
  public void sheet_set_formula_parse_error1(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("A1","= 1.5 + / 12");
  }
  @Test(expected=RuntimeException.class)
  public void sheet_set_formula_parse_error2(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("A1","= 1.5 + 12");
    sheet.setCell("B1","= A1 * B2 (3.5)");
  }

  // Just check that notifyDownstreamOfChange(id) exists; cannot check
  // functionality as it is baked into the spreadsheet itself.
  @Test public void sheet_has_notifyDownstreamOfChange(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("A1","= 1.5 + 12");
    sheet.setCell("B1","= A1 * C1 +3.5");
    sheet.notifyDownstreamOfChange("C1");
    sheet.notifyDownstreamOfChange("A1");
  }

  @Test public void sheet_set_number_formula1(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("A1","=1.5");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |    1.5 | '=1.5'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "Downstream Links:\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_number_formula2(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("A1","=1.5*2");
    sheet.setCell("B1"," = 1.5 * 2  ");
    sheet.setCell("C1","= 6 / 2 + 10  / 5 + -(5*9+1) ");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |    3.0 | '=1.5*2'\n"+
      "    C1 |  -41.0 | '= 6 / 2 + 10  / 5 + -(5*9+1)'\n"+
      "    B1 |    3.0 | '= 1.5 * 2'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "Downstream Links:\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_number_formula3(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("A1","  =10/3 ");
    sheet.setCell("B1","= 1-0.00001");
    sheet.setCell("C1","=-5.6431 ");
    sheet.setCell("D1","= 12-(5.6431*2) ");
    sheet.setCell("E1","=8/(2+4)*3+9.5");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |    3.3 | '=10/3'\n"+
      "    E1 |   13.5 | '=8/(2+4)*3+9.5'\n"+
      "    D1 |    0.7 | '= 12-(5.6431*2)'\n"+
      "    C1 |   -5.6 | '=-5.6431'\n"+
      "    B1 |    1.0 | '= 1-0.00001'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "Downstream Links:\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_number_formula_set_get1(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("A1","  =10/3 ");
    sheet.setCell("B1","= 1-0.00001");
    sheet.setCell("C1","=-5.6431 ");
    sheet.setCell("D1","= 12-(5.6431*2) ");
    sheet.setCell("E1","=8/(2+4)*3+9.5");  

    assertEquals("=10/3"           , sheet.getCellContents("A1"));
    assertEquals("= 1-0.00001"     , sheet.getCellContents("B1"));
    assertEquals("=-5.6431"        , sheet.getCellContents("C1"));
    assertEquals("= 12-(5.6431*2)" , sheet.getCellContents("D1"));
    assertEquals("=8/(2+4)*3+9.5"  , sheet.getCellContents("E1"));

    assertEquals("3.3"  , sheet.getCellDisplayString("A1"));
    assertEquals("1.0"  , sheet.getCellDisplayString("B1"));
    assertEquals("-5.6" , sheet.getCellDisplayString("C1"));
    assertEquals("0.7"  , sheet.getCellDisplayString("D1"));
    assertEquals("13.5" , sheet.getCellDisplayString("E1"));
  }
  
  @Test public void sheet_set_delete_simple_cell1(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("A1","1.5");
    sheet.setCell("B1"," dude ");
    sheet.deleteCell("A1");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    B1 |   dude | 'dude'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "Downstream Links:\n"+
      "";
    test_spreadsheet(sheet, expect);
    sheet.deleteCell("B1");
    expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "Downstream Links:\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_delete_simple_cell2(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("CP30","22.234");
    sheet.setCell("AA5","4.98    ");
    sheet.setCell("A1","=1.5");
    sheet.setCell("BB8","  12  ");
    sheet.setCell("D51"," dude ");
    sheet.setCell("DU6","  SWEET");
    sheet.setCell("L1","  42.42");
    sheet.setCell("D1","= 12-(5.6431*2) ");
    sheet.deleteCell("A1");
    sheet.deleteCell("AA5");
    sheet.deleteCell("L1");
    sheet.deleteCell("D1");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "   D51 |   dude | 'dude'\n"+
      "   DU6 |  SWEET | 'SWEET'\n"+
      "  CP30 |   22.2 | '22.234'\n"+
      "   BB8 |   12.0 | '12'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "Downstream Links:\n"+
      "";
    test_spreadsheet(sheet, expect);
  }

  @Test public void sheet_set_formula01(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("B1","2.0");
    sheet.setCell("A1","=1.5 * B1");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |    3.0 | '=1.5 * B1'\n"+
      "    B1 |    2.0 | '2.0'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [B1]\n"+
      "Downstream Links:\n"+
      "  B1 : [A1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_formula02(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("A1","=1.5 * B1");
    sheet.setCell("B1","2.0");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |    3.0 | '=1.5 * B1'\n"+
      "    B1 |    2.0 | '2.0'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [B1]\n"+
      "Downstream Links:\n"+
      "  B1 : [A1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_formula03(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("A1","=1.5 * B1");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |  ERROR | '=1.5 * B1'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [B1]\n"+
      "Downstream Links:\n"+
      "  B1 : [A1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_formula04(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("A1","=1.5 * C1");
    sheet.setCell("B1","=2 * C1");
    sheet.setCell("C1","2");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |    3.0 | '=1.5 * C1'\n"+
      "    C1 |    2.0 | '2'\n"+
      "    B1 |    4.0 | '=2 * C1'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [C1]\n"+
      "  B1 : [C1]\n"+
      "Downstream Links:\n"+
      "  C1 : [A1, B1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_formula05(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("C1","2");
    sheet.setCell("A1","=1.5 * C1");
    sheet.setCell("B1","=2 * C1");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |    3.0 | '=1.5 * C1'\n"+
      "    C1 |    2.0 | '2'\n"+
      "    B1 |    4.0 | '=2 * C1'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [C1]\n"+
      "  B1 : [C1]\n"+
      "Downstream Links:\n"+
      "  C1 : [A1, B1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_formula06(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("A1","=2 * B1");
    sheet.setCell("B1","=2 * C1");
    sheet.setCell("C1","=2 * D1");
    sheet.setCell("D1","=2 * E1");
    sheet.setCell("E1","2");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |   32.0 | '=2 * B1'\n"+
      "    E1 |    2.0 | '2'\n"+
      "    D1 |    4.0 | '=2 * E1'\n"+
      "    C1 |    8.0 | '=2 * D1'\n"+
      "    B1 |   16.0 | '=2 * C1'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [B1]\n"+
      "  D1 : [E1]\n"+
      "  C1 : [D1]\n"+
      "  B1 : [C1]\n"+
      "Downstream Links:\n"+
      "  E1 : [D1]\n"+
      "  D1 : [C1]\n"+
      "  C1 : [B1]\n"+
      "  B1 : [A1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_formula07(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("E1","2");
    sheet.setCell("A1","=2 * B1");
    sheet.setCell("B1","=2 * C1");
    sheet.setCell("C1","=2 * D1");
    sheet.setCell("D1","=2 * E1");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |   32.0 | '=2 * B1'\n"+
      "    E1 |    2.0 | '2'\n"+
      "    D1 |    4.0 | '=2 * E1'\n"+
      "    C1 |    8.0 | '=2 * D1'\n"+
      "    B1 |   16.0 | '=2 * C1'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [B1]\n"+
      "  D1 : [E1]\n"+
      "  C1 : [D1]\n"+
      "  B1 : [C1]\n"+
      "Downstream Links:\n"+
      "  E1 : [D1]\n"+
      "  D1 : [C1]\n"+
      "  C1 : [B1]\n"+
      "  B1 : [A1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_formula08(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("C1","=2 * D1");
    sheet.setCell("B1","=2 * C1");
    sheet.setCell("E1","2");
    sheet.setCell("D1","=2 * E1");
    sheet.setCell("A1","=2 * B1");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |   32.0 | '=2 * B1'\n"+
      "    E1 |    2.0 | '2'\n"+
      "    D1 |    4.0 | '=2 * E1'\n"+
      "    C1 |    8.0 | '=2 * D1'\n"+
      "    B1 |   16.0 | '=2 * C1'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [B1]\n"+
      "  D1 : [E1]\n"+
      "  C1 : [D1]\n"+
      "  B1 : [C1]\n"+
      "Downstream Links:\n"+
      "  E1 : [D1]\n"+
      "  D1 : [C1]\n"+
      "  C1 : [B1]\n"+
      "  B1 : [A1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_formula09(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("C1","=2 * D1");
    sheet.setCell("B1","=2 * C1");
    sheet.setCell("D1","=2 * E1");
    sheet.setCell("A1","=2 * B1");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |  ERROR | '=2 * B1'\n"+
      "    D1 |  ERROR | '=2 * E1'\n"+
      "    C1 |  ERROR | '=2 * D1'\n"+
      "    B1 |  ERROR | '=2 * C1'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [B1]\n"+
      "  D1 : [E1]\n"+
      "  C1 : [D1]\n"+
      "  B1 : [C1]\n"+
      "Downstream Links:\n"+
      "  E1 : [D1]\n"+
      "  D1 : [C1]\n"+
      "  C1 : [B1]\n"+
      "  B1 : [A1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_formula10(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("A1","=2 * B1");
    sheet.setCell("B1","=2 * C1");
    sheet.setCell("C1","2");
    sheet.setCell("D1","=2 * E1");
    sheet.setCell("E1","=2 * A1");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |    8.0 | '=2 * B1'\n"+
      "    E1 |   16.0 | '=2 * A1'\n"+
      "    D1 |   32.0 | '=2 * E1'\n"+
      "    C1 |    2.0 | '2'\n"+
      "    B1 |    4.0 | '=2 * C1'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [B1]\n"+
      "  E1 : [A1]\n"+
      "  D1 : [E1]\n"+
      "  B1 : [C1]\n"+
      "Downstream Links:\n"+
      "  A1 : [E1]\n"+
      "  E1 : [D1]\n"+
      "  C1 : [B1]\n"+
      "  B1 : [A1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_formula11(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("A1","=B1 + C1 / 5 ");
    sheet.setCell("B1","8");
    sheet.setCell("C1","=D1 * 10");
    sheet.setCell("D1","=(1+E1) * 4");
    sheet.setCell("E1","7");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |   72.0 | '=B1 + C1 / 5'\n"+
      "    E1 |    7.0 | '7'\n"+
      "    D1 |   32.0 | '=(1+E1) * 4'\n"+
      "    C1 |  320.0 | '=D1 * 10'\n"+
      "    B1 |    8.0 | '8'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [C1, B1]\n"+
      "  D1 : [E1]\n"+
      "  C1 : [D1]\n"+
      "Downstream Links:\n"+
      "  E1 : [D1]\n"+
      "  D1 : [C1]\n"+
      "  C1 : [A1]\n"+
      "  B1 : [A1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_formula12(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("A1","=(B1 * C1 * D1 + E1 / 5)/1000 ");
    sheet.setCell("B1","=C1 + 5 * E1");
    sheet.setCell("C1","=D1 * 10");
    sheet.setCell("D1","=(1+E1) * 4");
    sheet.setCell("E1","7");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 | 3635.2 | '=(B1 * C1 * D1 + E1 / 5)/1000'\n"+
      "    E1 |    7.0 | '7'\n"+
      "    D1 |   32.0 | '=(1+E1) * 4'\n"+
      "    C1 |  320.0 | '=D1 * 10'\n"+
      "    B1 |  355.0 | '=C1 + 5 * E1'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [E1, D1, C1, B1]\n"+
      "  D1 : [E1]\n"+
      "  C1 : [D1]\n"+
      "  B1 : [E1, C1]\n"+
      "Downstream Links:\n"+
      "  E1 : [A1, D1, B1]\n"+
      "  D1 : [A1, C1]\n"+
      "  C1 : [A1, B1]\n"+
      "  B1 : [A1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_formula13(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("E1","=-1+2*4");
    sheet.setCell("B1","=C1 + 5 * E1");
    sheet.setCell("A1","=(B1 * C1 * D1 + E1 / 5)/1000 ");
    sheet.setCell("C1","=D1 * 10");
    sheet.setCell("D1","=(1+E1) * 4");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 | 3635.2 | '=(B1 * C1 * D1 + E1 / 5)/1000'\n"+
      "    E1 |    7.0 | '=-1+2*4'\n"+
      "    D1 |   32.0 | '=(1+E1) * 4'\n"+
      "    C1 |  320.0 | '=D1 * 10'\n"+
      "    B1 |  355.0 | '=C1 + 5 * E1'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [E1, D1, C1, B1]\n"+
      "  D1 : [E1]\n"+
      "  C1 : [D1]\n"+
      "  B1 : [E1, C1]\n"+
      "Downstream Links:\n"+
      "  E1 : [A1, D1, B1]\n"+
      "  D1 : [A1, C1]\n"+
      "  C1 : [A1, B1]\n"+
      "  B1 : [A1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_formula14(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("E1","=-1+2*4");
    sheet.setCell("B1","=C1 + 5 * E1");
    sheet.setCell("A1","=(B1 * C1 * D1 + E1 / 5)/1000 ");
    sheet.setCell("D1","=(1+E1) * 4");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |  ERROR | '=(B1 * C1 * D1 + E1 / 5)/1000'\n"+
      "    E1 |    7.0 | '=-1+2*4'\n"+
      "    D1 |   32.0 | '=(1+E1) * 4'\n"+
      "    B1 |  ERROR | '=C1 + 5 * E1'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [E1, D1, C1, B1]\n"+
      "  D1 : [E1]\n"+
      "  B1 : [E1, C1]\n"+
      "Downstream Links:\n"+
      "  E1 : [A1, D1, B1]\n"+
      "  D1 : [A1]\n"+
      "  C1 : [A1, B1]\n"+
      "  B1 : [A1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_formula15(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("E1","=-1+2*4");
    sheet.setCell("B1","=C1 + 5 * E1");
    sheet.setCell("A1","=(B1 * C1 * D1 + E1 / 5)/1000 ");
    sheet.setCell("D1","=(1+E1) * 4");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |  ERROR | '=(B1 * C1 * D1 + E1 / 5)/1000'\n"+
      "    E1 |    7.0 | '=-1+2*4'\n"+
      "    D1 |   32.0 | '=(1+E1) * 4'\n"+
      "    B1 |  ERROR | '=C1 + 5 * E1'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [E1, D1, C1, B1]\n"+
      "  D1 : [E1]\n"+
      "  B1 : [E1, C1]\n"+
      "Downstream Links:\n"+
      "  E1 : [A1, D1, B1]\n"+
      "  D1 : [A1]\n"+
      "  C1 : [A1, B1]\n"+
      "  B1 : [A1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }

  @Test public void sheet_set_remove_formula01(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("B1","2.0");
    sheet.setCell("A1","=1.5 * B1");
    sheet.deleteCell("A1");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    B1 |    2.0 | '2.0'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "Downstream Links:\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_remove_formula02(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("B1","2.0");
    sheet.setCell("A1","=1.5 * B1");
    sheet.deleteCell("B1");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |  ERROR | '=1.5 * B1'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [B1]\n"+
      "Downstream Links:\n"+
      "  B1 : [A1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_remove_formula03(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("A1","=1.5 * B1");
    sheet.setCell("B1","2.0");
    sheet.deleteCell("B1");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |  ERROR | '=1.5 * B1'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [B1]\n"+
      "Downstream Links:\n"+
      "  B1 : [A1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_remove_formula04(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("A1","=1.5 * C1");
    sheet.setCell("B1","=2 * C1");
    sheet.setCell("C1","2");
    sheet.deleteCell("C1");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |  ERROR | '=1.5 * C1'\n"+
      "    B1 |  ERROR | '=2 * C1'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [C1]\n"+
      "  B1 : [C1]\n"+
      "Downstream Links:\n"+
      "  C1 : [A1, B1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_remove_formula05(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("A1","=1.5 * C1");
    sheet.setCell("C1","2");
    sheet.deleteCell("C1");
    sheet.setCell("B1","=2 * C1");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |  ERROR | '=1.5 * C1'\n"+
      "    B1 |  ERROR | '=2 * C1'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [C1]\n"+
      "  B1 : [C1]\n"+
      "Downstream Links:\n"+
      "  C1 : [A1, B1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_remove_formula06(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("E1","2");
    sheet.setCell("A1","=2 * B1");
    sheet.setCell("B1","=2 * C1");
    sheet.setCell("C1","=2 * D1");
    sheet.setCell("D1","=2 * E1");
    sheet.deleteCell("E1");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |  ERROR | '=2 * B1'\n"+
      "    D1 |  ERROR | '=2 * E1'\n"+
      "    C1 |  ERROR | '=2 * D1'\n"+
      "    B1 |  ERROR | '=2 * C1'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [B1]\n"+
      "  D1 : [E1]\n"+
      "  C1 : [D1]\n"+
      "  B1 : [C1]\n"+
      "Downstream Links:\n"+
      "  E1 : [D1]\n"+
      "  D1 : [C1]\n"+
      "  C1 : [B1]\n"+
      "  B1 : [A1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_remove_formula07(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("A1","=2 * B1");
    sheet.setCell("B1","=2 * C1");
    sheet.setCell("C1","2");
    sheet.setCell("D1","=2 * E1");
    sheet.setCell("E1","=2 * A1");
    sheet.deleteCell("B1");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |  ERROR | '=2 * B1'\n"+
      "    E1 |  ERROR | '=2 * A1'\n"+
      "    D1 |  ERROR | '=2 * E1'\n"+
      "    C1 |    2.0 | '2'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [B1]\n"+
      "  E1 : [A1]\n"+
      "  D1 : [E1]\n"+
      "Downstream Links:\n"+
      "  A1 : [E1]\n"+
      "  E1 : [D1]\n"+
      "  B1 : [A1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_remove_formula08(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("D1","=2 * E1");
    sheet.setCell("C1","2");
    sheet.setCell("A1","=2 * B1");
    sheet.setCell("B1","=2 * C1");
    sheet.deleteCell("B1");
    sheet.setCell("E1","=2 * A1");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |  ERROR | '=2 * B1'\n"+
      "    E1 |  ERROR | '=2 * A1'\n"+
      "    D1 |  ERROR | '=2 * E1'\n"+
      "    C1 |    2.0 | '2'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [B1]\n"+
      "  E1 : [A1]\n"+
      "  D1 : [E1]\n"+
      "Downstream Links:\n"+
      "  A1 : [E1]\n"+
      "  E1 : [D1]\n"+
      "  B1 : [A1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_remove_formula09(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("A1","=B1 + C1 / 5 ");
    sheet.setCell("B1","8");
    sheet.setCell("C1","=D1 * 10");
    sheet.setCell("D1","=(1+E1) * 4");
    sheet.setCell("E1","7");
    sheet.deleteCell("B1");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |  ERROR | '=B1 + C1 / 5'\n"+
      "    E1 |    7.0 | '7'\n"+
      "    D1 |   32.0 | '=(1+E1) * 4'\n"+
      "    C1 |  320.0 | '=D1 * 10'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [C1, B1]\n"+
      "  D1 : [E1]\n"+
      "  C1 : [D1]\n"+
      "Downstream Links:\n"+
      "  E1 : [D1]\n"+
      "  D1 : [C1]\n"+
      "  C1 : [A1]\n"+
      "  B1 : [A1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_remove_formula10(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("E1","=-1+2*4");
    sheet.setCell("B1","=C1 + 5 * E1");
    sheet.setCell("A1","=(B1 * C1 * D1 + E1 / 5)/1000 ");
    sheet.setCell("C1","=D1 * 10");
    sheet.setCell("D1","=(1+E1) * 4");
    sheet.deleteCell("B1");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |  ERROR | '=(B1 * C1 * D1 + E1 / 5)/1000'\n"+
      "    E1 |    7.0 | '=-1+2*4'\n"+
      "    D1 |   32.0 | '=(1+E1) * 4'\n"+
      "    C1 |  320.0 | '=D1 * 10'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [E1, D1, C1, B1]\n"+
      "  D1 : [E1]\n"+
      "  C1 : [D1]\n"+
      "Downstream Links:\n"+
      "  E1 : [A1, D1]\n"+
      "  D1 : [A1, C1]\n"+
      "  C1 : [A1]\n"+
      "  B1 : [A1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }

  @Test public void sheet_set_replaces01(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("A1","1");
    sheet.setCell("A1","2");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |    2.0 | '2'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "Downstream Links:\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_replaces02(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("A1","=B1+1");
    sheet.setCell("B1","2");
    sheet.setCell("B1","4");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |    5.0 | '=B1+1'\n"+
      "    B1 |    4.0 | '4'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [B1]\n"+
      "Downstream Links:\n"+
      "  B1 : [A1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_replaces03(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("B1","2");
    sheet.setCell("A1","=B1+1");
    sheet.setCell("A1","=B1*2");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |    4.0 | '=B1*2'\n"+
      "    B1 |    2.0 | '2'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [B1]\n"+
      "Downstream Links:\n"+
      "  B1 : [A1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_replaces04(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("A1","=2 * B1");
    sheet.setCell("B1","=2 * C1");
    sheet.setCell("C1","=2 * D1");
    sheet.setCell("D1","=2 * E1");
    sheet.setCell("E1","2");
    sheet.setCell("E1","3");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |   48.0 | '=2 * B1'\n"+
      "    E1 |    3.0 | '3'\n"+
      "    D1 |    6.0 | '=2 * E1'\n"+
      "    C1 |   12.0 | '=2 * D1'\n"+
      "    B1 |   24.0 | '=2 * C1'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [B1]\n"+
      "  D1 : [E1]\n"+
      "  C1 : [D1]\n"+
      "  B1 : [C1]\n"+
      "Downstream Links:\n"+
      "  E1 : [D1]\n"+
      "  D1 : [C1]\n"+
      "  C1 : [B1]\n"+
      "  B1 : [A1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_replaces05(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("A1","=2 * B1");
    sheet.setCell("B1","=2 * C1");
    sheet.setCell("C1","=2 * D1");
    sheet.setCell("D1","=2 * E1");
    sheet.setCell("E1","2");
    sheet.setCell("B1","5");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |   10.0 | '=2 * B1'\n"+
      "    E1 |    2.0 | '2'\n"+
      "    D1 |    4.0 | '=2 * E1'\n"+
      "    C1 |    8.0 | '=2 * D1'\n"+
      "    B1 |    5.0 | '5'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [B1]\n"+
      "  D1 : [E1]\n"+
      "  C1 : [D1]\n"+
      "Downstream Links:\n"+
      "  E1 : [D1]\n"+
      "  D1 : [C1]\n"+
      "  B1 : [A1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_replaces06(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("A1","=2 * B1");
    sheet.setCell("B1","=2 * C1");
    sheet.setCell("C1","=2 * D1");
    sheet.setCell("D1","=2 * E1");
    sheet.setCell("E1","2");
    sheet.setCell("D1","=5*E1");
    sheet.setCell("B1","=1");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |    2.0 | '=2 * B1'\n"+
      "    E1 |    2.0 | '2'\n"+
      "    D1 |   10.0 | '=5*E1'\n"+
      "    C1 |   20.0 | '=2 * D1'\n"+
      "    B1 |    1.0 | '=1'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [B1]\n"+
      "  D1 : [E1]\n"+
      "  C1 : [D1]\n"+
      "Downstream Links:\n"+
      "  E1 : [D1]\n"+
      "  D1 : [C1]\n"+
      "  B1 : [A1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_replaces07(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("E1","=-1+2*4");
    sheet.setCell("B1","=C1 + 5 * E1");
    sheet.setCell("A1","=(B1 * C1 * D1 + E1 / 5)/1000 ");
    sheet.setCell("C1","=D1 * 10");
    sheet.setCell("D1","=(1+E1) * 4");
    sheet.setCell("B1","=1+4.5");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |   56.3 | '=(B1 * C1 * D1 + E1 / 5)/1000'\n"+
      "    E1 |    7.0 | '=-1+2*4'\n"+
      "    D1 |   32.0 | '=(1+E1) * 4'\n"+
      "    C1 |  320.0 | '=D1 * 10'\n"+
      "    B1 |    5.5 | '=1+4.5'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [E1, D1, C1, B1]\n"+
      "  D1 : [E1]\n"+
      "  C1 : [D1]\n"+
      "Downstream Links:\n"+
      "  E1 : [A1, D1]\n"+
      "  D1 : [A1, C1]\n"+
      "  C1 : [A1]\n"+
      "  B1 : [A1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_replaces08(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("E1","=-1+2*4");
    sheet.setCell("B1","=C1 + 5 * E1");
    sheet.setCell("A1","=(B1 * C1 * D1 + E1 / 5)/1000 ");
    sheet.setCell("C1","=D1 * 10");
    sheet.setCell("D1","=(1+E1) * 4");
    sheet.setCell("B1","=1+4.5");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |   56.3 | '=(B1 * C1 * D1 + E1 / 5)/1000'\n"+
      "    E1 |    7.0 | '=-1+2*4'\n"+
      "    D1 |   32.0 | '=(1+E1) * 4'\n"+
      "    C1 |  320.0 | '=D1 * 10'\n"+
      "    B1 |    5.5 | '=1+4.5'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [E1, D1, C1, B1]\n"+
      "  D1 : [E1]\n"+
      "  C1 : [D1]\n"+
      "Downstream Links:\n"+
      "  E1 : [A1, D1]\n"+
      "  D1 : [A1, C1]\n"+
      "  C1 : [A1]\n"+
      "  B1 : [A1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }

  @Test public void sheet_set_removes1(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("A1","1");
    sheet.setCell("A1","");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "Downstream Links:\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_removes_02(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("A1","=B1 + C1 / 5 ");
    sheet.setCell("B1","8");
    sheet.setCell("C1","=D1 * 10");
    sheet.setCell("D1","=(1+E1) * 4");
    sheet.setCell("E1","7");
    sheet.setCell("B1","");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |  ERROR | '=B1 + C1 / 5'\n"+
      "    E1 |    7.0 | '7'\n"+
      "    D1 |   32.0 | '=(1+E1) * 4'\n"+
      "    C1 |  320.0 | '=D1 * 10'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [C1, B1]\n"+
      "  D1 : [E1]\n"+
      "  C1 : [D1]\n"+
      "Downstream Links:\n"+
      "  E1 : [D1]\n"+
      "  D1 : [C1]\n"+
      "  C1 : [A1]\n"+
      "  B1 : [A1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_removes03(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("E1","=-1+2*4");
    sheet.setCell("B1","=C1 + 5 * E1");
    sheet.setCell("A1","=(B1 * C1 * D1 + E1 / 5)/1000 ");
    sheet.setCell("C1","=D1 * 10");
    sheet.setCell("D1","=(1+E1) * 4");
    sheet.setCell("B1","");
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |  ERROR | '=(B1 * C1 * D1 + E1 / 5)/1000'\n"+
      "    E1 |    7.0 | '=-1+2*4'\n"+
      "    D1 |   32.0 | '=(1+E1) * 4'\n"+
      "    C1 |  320.0 | '=D1 * 10'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [E1, D1, C1, B1]\n"+
      "  D1 : [E1]\n"+
      "  C1 : [D1]\n"+
      "Downstream Links:\n"+
      "  E1 : [A1, D1]\n"+
      "  D1 : [A1, C1]\n"+
      "  C1 : [A1]\n"+
      "  B1 : [A1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }
  @Test public void sheet_set_removes04(){
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("E1","=-1+2*4");
    sheet.setCell("B1","=C1 + 5 * E1");
    sheet.setCell("A1","=(B1 * C1 * D1 + E1 / 5)/1000 ");
    sheet.setCell("C1","=D1 * 10");
    sheet.setCell("D1","=(1+E1) * 4");
    sheet.setCell("B1",null);
    String expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |  ERROR | '=(B1 * C1 * D1 + E1 / 5)/1000'\n"+
      "    E1 |    7.0 | '=-1+2*4'\n"+
      "    D1 |   32.0 | '=(1+E1) * 4'\n"+
      "    C1 |  320.0 | '=D1 * 10'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [E1, D1, C1, B1]\n"+
      "  D1 : [E1]\n"+
      "  C1 : [D1]\n"+
      "Downstream Links:\n"+
      "  E1 : [A1, D1]\n"+
      "  D1 : [A1, C1]\n"+
      "  C1 : [A1]\n"+
      "  B1 : [A1]\n"+
      "";
    test_spreadsheet(sheet, expect);
  }


  // Utility to check whether a sheet detects cycles properly
  public static void test_sheet_cycle(String additions[],
                                      String expectS_unsorted,
                                      boolean expectCycleFound, String expectCycleString)
                                   
  {
    Spreadsheet sheet = new Spreadsheet();
    int addI = 0;
    boolean actualCycleFound = false;
    String actualCycleString = "";
    try{
      for(addI=0; addI<additions.length; addI+=2){
        String id = additions[addI];
        String contents = additions[addI+1];
        sheet.setCell(id,contents);
      }
    }
    catch(DAG.CycleException e){
      actualCycleFound = true;
      actualCycleString= e.getMessage();
    }

    String actualS = sortedLines(sheet.toString());
    String expectS = sortedLines(expectS_unsorted);

    boolean pass = true;
    pass = pass && expectS.equals(actualS);
    pass = pass && expectCycleFound == actualCycleFound;
    pass = pass && actualCycleString.contains(expectCycleString);

    if(pass) { return; }

    // Failed test
    StringBuilder additionsS = new StringBuilder();
    for(int i=0; i<additions.length; i+=2){
      additionsS.append(String.format("%2d: sheet.setCell(%4s, %s)\n",i/2,additions[i],additions[i+1]));
      if(i==addI){
        additionsS.deleteCharAt(additionsS.length()-1);
        additionsS.append(" --> CYCLE DETECTED HERE\n");
      }
    }

    String msg =
      String.format("Spreadsheet incorrect after possible cycle\n")+
      String.format("SETS COMPLETED = %d:\n%s\n",addI/2,additionsS.toString())+
      String.format("EXPECT CYCLE: %s %s\nACTUAL CYCLE: %s %s\n",expectCycleFound,expectCycleString,actualCycleFound,actualCycleString)+
      String.format("FINAL SPREADSHEET EXPECT:\n%s\nFINAL SPREADSHEET ACTUAL:\n%s\n",expectS,actualS)+
      "";
    fail(msg);
  }

  @Test public void sheet_set_formula_cycle01(){
    String expectS, expectCycleString; boolean expectCycleFound;
    String [] additions = {
      "A1","=A1",
    };
    expectCycleFound  = true;
    expectCycleString = "[A1, A1]";
    expectS =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "Downstream Links:\n"+
      "";
    test_sheet_cycle(additions,expectS,expectCycleFound,expectCycleString);
  }
  @Test public void sheet_set_formula_cycle02(){
    String expectS, expectCycleString; boolean expectCycleFound;
    String [] additions = {
      "A1","=B1*2",
      "B1","=C1*2",
      "C1","=A1*2",
    };
    expectCycleFound  = true;
    expectCycleString = "[C1, A1, B1, C1]";
    expectS =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |  ERROR | '=B1*2'\n"+
      "    B1 |  ERROR | '=C1*2'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [B1]\n"+
      "  B1 : [C1]\n"+
      "Downstream Links:\n"+
      "  C1 : [B1]\n"+
      "  B1 : [A1]\n"+
      "";
    test_sheet_cycle(additions,expectS,expectCycleFound,expectCycleString);
  }
  @Test public void sheet_set_formula_cycle03(){
    String expectS, expectCycleString; boolean expectCycleFound;
    String [] additions = {
      "A1","=B1*2",
      "B1","=C1*2",
      "C1","=D1*2",
      "D1","=E1*2",
      "E1","=F1*2",
      "F1","=A1*2",
    };
    expectCycleFound  = true;
    expectCycleString = "[F1, A1, B1, C1, D1, E1, F1]";
    expectS =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |  ERROR | '=B1*2'\n"+
      "    E1 |  ERROR | '=F1*2'\n"+
      "    D1 |  ERROR | '=E1*2'\n"+
      "    C1 |  ERROR | '=D1*2'\n"+
      "    B1 |  ERROR | '=C1*2'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [B1]\n"+
      "  E1 : [F1]\n"+
      "  D1 : [E1]\n"+
      "  C1 : [D1]\n"+
      "  B1 : [C1]\n"+
      "Downstream Links:\n"+
      "  F1 : [E1]\n"+
      "  E1 : [D1]\n"+
      "  D1 : [C1]\n"+
      "  C1 : [B1]\n"+
      "  B1 : [A1]\n"+
      "";
    test_sheet_cycle(additions,expectS,expectCycleFound,expectCycleString);
  }
  @Test public void sheet_set_formula_cycle04(){
    String expectS, expectCycleString; boolean expectCycleFound;
    String [] additions = {
      "A1","=(B1 * C1 * D1 + E1 / 5)/1000 ",
      "B1","=C1 + 5",           // originally "B1","=C1 + 5 * E1" but creates two possible cycles
      "C1","=D1 * 10",
      "D1","=(1+E1) * 4",
      "E1","=4*2 + B1*5",
    };
    expectCycleFound  = true;
    expectCycleString = "[E1, B1, C1, D1, E1]"; // originally "[E1, B1, E1]" but not unique
    expectS =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |  ERROR | '=(B1 * C1 * D1 + E1 / 5)/1000'\n"+
      "    D1 |  ERROR | '=(1+E1) * 4'\n"+
      "    C1 |  ERROR | '=D1 * 10'\n"+
      "    B1 |  ERROR | '=C1 + 5'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [B1, C1, D1, E1]\n"+
      "  B1 : [C1]\n"+
      "  C1 : [D1]\n"+
      "  D1 : [E1]\n"+
      "Downstream Links:\n"+
      "  B1 : [A1]\n"+
      "  C1 : [A1, B1]\n"+
      "  D1 : [A1, C1]\n"+
      "  E1 : [A1, D1]\n"+
      "";
    test_sheet_cycle(additions,expectS,expectCycleFound,expectCycleString);
  }
  @Test public void sheet_set_formula_cycle05(){
    String expectS, expectCycleString; boolean expectCycleFound;
    String [] additions = {
      "A1","=(B1 * C1 * D1 + E1 / 5)/10000 ",
      "B1","=C1 + 5 * E1",
      "C1","=D1 * 10",
      "D1","=(1+E1) * 4",
      "E1","=4*2 + 5",
      "D1","=E1*5 + C1",
    };
    expectCycleFound  = true;
    expectCycleString = "[D1, C1, D1]";
    expectS =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 | 1960.0 | '=(B1 * C1 * D1 + E1 / 5)/10000'\n"+
      "    B1 |  625.0 | '=C1 + 5 * E1'\n"+
      "    C1 |  560.0 | '=D1 * 10'\n"+
      "    D1 |   56.0 | '=(1+E1) * 4'\n"+
      "    E1 |   13.0 | '=4*2 + 5'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [B1, C1, D1, E1]\n"+
      "  B1 : [C1, E1]\n"+
      "  C1 : [D1]\n"+
      "  D1 : [E1]\n"+
      "Downstream Links:\n"+
      "  B1 : [A1]\n"+
      "  C1 : [A1, B1]\n"+
      "  D1 : [A1, C1]\n"+
      "  E1 : [A1, B1, D1]\n"+
      "";
    test_sheet_cycle(additions,expectS,expectCycleFound,expectCycleString);
  }

  @Test public void sheet_set_formula_nocycle01(){
    String expectS, expectCycleString; boolean expectCycleFound;
    String [] additions = {
      "A1","=B1*2",
      "B1","=C1*2",
      "C1","=D1*2",
      "D1","=E1*2",
      "E1","=F1*2",
      "B1","=2",
      "F1","=A1*2",
    };
    expectCycleFound  = false;
    expectCycleString = "";
    expectS =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |    4.0 | '=B1*2'\n"+
      "    F1 |    8.0 | '=A1*2'\n"+
      "    E1 |   16.0 | '=F1*2'\n"+
      "    D1 |   32.0 | '=E1*2'\n"+
      "    C1 |   64.0 | '=D1*2'\n"+
      "    B1 |    2.0 | '=2'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [B1]\n"+
      "  F1 : [A1]\n"+
      "  E1 : [F1]\n"+
      "  D1 : [E1]\n"+
      "  C1 : [D1]\n"+
      "Downstream Links:\n"+
      "  A1 : [F1]\n"+
      "  F1 : [E1]\n"+
      "  E1 : [D1]\n"+
      "  D1 : [C1]\n"+
      "  B1 : [A1]\n"+
      "";
    test_sheet_cycle(additions,expectS,expectCycleFound,expectCycleString);
  }
  @Test public void sheet_set_formula_nocycle02(){
    String expectS, expectCycleString; boolean expectCycleFound;
    String [] additions = {
      "A1","=(B1 * C1 * D1 + E1 / 5)/10000 ",
      "B1","=C1 + 5 * E1",
      "C1","=D1 * 10",
      "D1","=(1+E1) * 4",
      "E1","=4*2 + 5",
      "A1","=B1+1",
      "C1","=E1*4",
      "D1","=E1*5 + A1",
    };
    expectCycleFound  = false;
    expectCycleString = "";
    expectS =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |  118.0 | '=B1+1'\n"+
      "    E1 |   13.0 | '=4*2 + 5'\n"+
      "    D1 |  183.0 | '=E1*5 + A1'\n"+
      "    C1 |   52.0 | '=E1*4'\n"+
      "    B1 |  117.0 | '=C1 + 5 * E1'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [B1]\n"+
      "  D1 : [A1, E1]\n"+
      "  C1 : [E1]\n"+
      "  B1 : [E1, C1]\n"+
      "Downstream Links:\n"+
      "  A1 : [D1]\n"+
      "  E1 : [D1, C1, B1]\n"+
      "  C1 : [B1]\n"+
      "  B1 : [A1]\n"+
      "";
    test_sheet_cycle(additions,expectS,expectCycleFound,expectCycleString);
  }

  // // Tests for toSaveString() and fromSaveString()

  // @Test public void sheet_saving1(){
  //   String expect,saveString,actual;
  //   Spreadsheet sheet = new Spreadsheet();
  //   sheet.setCell("A1"," 1.5  ");
  //   sheet.setCell("B1","ack!");
  //   expect = sheet.toString();
  //   saveString = sheet.toSaveString();
  //   Spreadsheet copy1 = Spreadsheet.fromSaveString(saveString);
  //   actual = copy1.toString();
  //   assertEquals("to/from saveString() incorrect",expect,actual);
  // }
  // @Test public void sheet_saving2(){
  //   String expect,saveString,actual;
  //   Spreadsheet sheet = new Spreadsheet();
  //   sheet.setCell("A1","= 2*B1  ");
  //   sheet.setCell("B1","=C1/10");
  //   sheet.setCell("C1","=50");
  //   expect = sheet.toString();
  //   saveString = sheet.toSaveString();
  //   Spreadsheet copy1 = Spreadsheet.fromSaveString(saveString);
  //   sheet.setCell("C1","  100  ");
  //   actual = copy1.toString();
  //   assertEquals("to/from saveString() incorrect - distinct copies?",expect,actual);
  // }
  // @Test public void sheet_saving3(){
  //   String expect,saveString,actual;
  //   Spreadsheet sheet = new Spreadsheet();
  //   sheet.setCell("A1","=(B1 * C1 * D1 + E1 / 5)/10000 ");
  //   sheet.setCell("B1","=C1 + 5 * E1");
  //   sheet.setCell("C1","=D1 * 10");
  //   sheet.setCell("D1","=(1+E1) * 4");
  //   sheet.setCell("E1","=4*2 + 5");
  //   sheet.setCell("A1","=B1+1");
  //   sheet.setCell("C1","=E1*4");
  //   sheet.setCell("D1","=E1*5 + A1");
  //   expect = sheet.toString();
  //   saveString = sheet.toSaveString();
  //   Spreadsheet copy1 = Spreadsheet.fromSaveString(saveString);
  //   actual = copy1.toString();
  //   assertEquals("to/from saveString() incorrect",expect,actual);
  // }
  // @Test public void sheet_saving4(){
  //   String expect,saveString,actual;
  //   Spreadsheet sheet = new Spreadsheet();
  //   sheet.setCell("A1","=(B1 * C1 * D1 + E1 / 5)/10000 ");
  //   sheet.setCell("B1","=C1 + 5 * E1");
  //   sheet.setCell("C1","=D1 * 10");
  //   sheet.setCell("D1","=(1+E1) * 4");
  //   sheet.setCell("E1","=4*2 + 5");
  //   sheet.setCell("A1","=B1+1");
  //   sheet.setCell("C1","=E1*4");
  //   sheet.setCell("D1","=E1*5 + A1");
  //   expect =
  //     sheet.toString();
  //   saveString = sheet.toSaveString();
  //   Spreadsheet copy1 = Spreadsheet.fromSaveString(saveString);
  //   actual = copy1.toString();
  //   sheet.setCell("A1","hi");
  //   assertEquals("to/from saveString() incorrect - distinct copies?",expect,actual);
  // }
  // @Test public void sheet_saving5(){
  //   String expect,saveString,actual;
  //   Spreadsheet sheet = new Spreadsheet();
  //   sheet.setCell("A1","=(B1 * C1 * D1 + E1 / 5)/10000 ");
  //   sheet.setCell("B1","=C1 + 5 * E1");
  //   sheet.setCell("C1","=D1 * 10");
  //   sheet.setCell("D1","=(1+E1) * 4");
  //   sheet.setCell("E1","=4*2 + 5");
  //   sheet.setCell("A1","=B1+1");
  //   sheet.setCell("C1","=E1*4");
  //   sheet.setCell("D1","=E1*5 + A1");
  //   expect = sheet.toString();
  //   saveString = sheet.toSaveString();
  //   Spreadsheet copy1 = Spreadsheet.fromSaveString(saveString);
  //   actual = copy1.toString();
  //   sheet.setCell("D1","hi");
  //   assertEquals("to/from saveString() incorrect - distinct copies?",expect,actual);
  // }
  // @Test public void sheet_saving6(){
  //   Spreadsheet sheet = new Spreadsheet();
  //   sheet.setCell("A1","= 2*B1  ");
  //   sheet.setCell("B1","=C1/10");
  //   sheet.setCell("C1","=50");
  //   String expect1 = sheet.toString();
  //   String saveString1 = sheet.toSaveString();
  //   Spreadsheet copy1 = Spreadsheet.fromSaveString(saveString1);
  //   sheet.setCell("C1","  100  ");
  //   String actual1 = copy1.toString();
  //   assertEquals("to/from saveString() incorrect - distinct copies?",expect1,actual1);
  //   // Second copy
  //   String expect2 = sheet.toString();
  //   String saveString2 = sheet.toSaveString();
  //   Spreadsheet copy2 = Spreadsheet.fromSaveString(saveString2);
  //   sheet.setCell("A1","food");
  //   sheet.setCell("B1","drink");
  //   String actual2 = copy2.toString();
  //   assertEquals("to/from saveString() incorrect - distinct copies?",expect2,actual2);
  // }


  @Test public void sheet_stress1(){
    String expect;
    Spreadsheet sheet = new Spreadsheet();
    sheet.setCell("A1","=(B1 * C1 * D1 + E1 / 5)/10000 ");
    sheet.setCell("B1","=C1 + 5 * E1");
    sheet.setCell("C1","=D1 * 10");
    sheet.setCell("D1","=(1+E1) * 4");
    sheet.setCell("E1","=4*2 + 5");
    sheet.setCell("A1","=B1+1");
    sheet.setCell("C1","=E1*4");
    sheet.setCell("D1","=E1*5 + A1");
    expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |  118.0 | '=B1+1'\n"+
      "    E1 |   13.0 | '=4*2 + 5'\n"+
      "    D1 |  183.0 | '=E1*5 + A1'\n"+
      "    C1 |   52.0 | '=E1*4'\n"+
      "    B1 |  117.0 | '=C1 + 5 * E1'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [B1]\n"+
      "  D1 : [A1, E1]\n"+
      "  C1 : [E1]\n"+
      "  B1 : [E1, C1]\n"+
      "Downstream Links:\n"+
      "  A1 : [D1]\n"+
      "  E1 : [D1, C1, B1]\n"+
      "  C1 : [B1]\n"+
      "  B1 : [A1]\n"+
      "";
    test_spreadsheet(sheet, expect);
    // Spreadsheet sheet2 = Spreadsheet.fromSaveString(sheet.toSaveString());
    Spreadsheet sheet2 = new Spreadsheet();
    sheet2.setCell("A1","=(B1 * C1 * D1 + E1 / 5)/10000 ");
    sheet2.setCell("B1","=C1 + 5 * E1");
    sheet2.setCell("C1","=D1 * 10");
    sheet2.setCell("D1","=(1+E1) * 4");
    sheet2.setCell("E1","=4*2 + 5");
    sheet2.setCell("A1","=B1+1");
    sheet2.setCell("C1","=E1*4");
    sheet2.setCell("D1","=E1*5 + A1");
    test_spreadsheet(sheet2, expect);
    sheet2.deleteCell("A1");
    sheet2.deleteCell("B1");
    test_spreadsheet(sheet, expect);
    expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    E1 |   13.0 | '=4*2 + 5'\n"+
      "    D1 |  ERROR | '=E1*5 + A1'\n"+
      "    C1 |   52.0 | '=E1*4'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  D1 : [A1, E1]\n"+
      "  C1 : [E1]\n"+
      "Downstream Links:\n"+
      "  A1 : [D1]\n"+
      "  E1 : [D1, C1]\n"+
      "";
    test_spreadsheet(sheet2, expect);
    expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |  118.0 | '=B1+1'\n"+
      "    E1 |   13.0 | '=4*2 + 5'\n"+
      "    D1 |  183.0 | '=E1*5 + A1'\n"+
      "    C1 |   52.0 | '=E1*4'\n"+
      "    B1 |  117.0 | '=C1 + 5 * E1'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [B1]\n"+
      "  D1 : [A1, E1]\n"+
      "  C1 : [E1]\n"+
      "  B1 : [E1, C1]\n"+
      "Downstream Links:\n"+
      "  A1 : [D1]\n"+
      "  E1 : [D1, C1, B1]\n"+
      "  C1 : [B1]\n"+
      "  B1 : [A1]\n"+
      "";
    test_spreadsheet(sheet, expect);
    boolean cycleFound = false;
    try{
      sheet.setCell("E1","=2*A1");
    }
    catch(DAG.CycleException e){
      cycleFound = true;
    }
    assertTrue("Should have found a cycle\nSpreadsheet:\n"+sheet.toString(),cycleFound);
    cycleFound = false;
    try{
      sheet.setCell("B1","=F1+5");
      sheet.setCell("E1","=2*B1");
      sheet.setCell("F1","42");
    }
    catch(DAG.CycleException e){
      cycleFound = true;
    }
    assertFalse("Should be NO cycles\nSpreadsheet:\n"+sheet.toString(),cycleFound);
    expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |   48.0 | '=B1+1'\n"+
      "    F1 |   42.0 | '42'\n"+
      "    E1 |   94.0 | '=2*B1'\n"+
      "    D1 |  518.0 | '=E1*5 + A1'\n"+
      "    C1 |  376.0 | '=E1*4'\n"+
      "    B1 |   47.0 | '=F1+5'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [B1]\n"+
      "  E1 : [B1]\n"+
      "  D1 : [A1, E1]\n"+
      "  C1 : [E1]\n"+
      "  B1 : [F1]\n"+
      "Downstream Links:\n"+
      "  A1 : [D1]\n"+
      "  F1 : [B1]\n"+
      "  E1 : [D1, C1]\n"+
      "  B1 : [A1, E1]\n"+
      "";
    test_spreadsheet(sheet, expect);
    sheet.setCell("F1","bummer");
    expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |  ERROR | '=B1+1'\n"+
      "    F1 | bummer | 'bummer'\n"+
      "    E1 |  ERROR | '=2*B1'\n"+
      "    D1 |  ERROR | '=E1*5 + A1'\n"+
      "    C1 |  ERROR | '=E1*4'\n"+
      "    B1 |  ERROR | '=F1+5'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  A1 : [B1]\n"+
      "  E1 : [B1]\n"+
      "  D1 : [A1, E1]\n"+
      "  C1 : [E1]\n"+
      "  B1 : [F1]\n"+
      "Downstream Links:\n"+
      "  A1 : [D1]\n"+
      "  F1 : [B1]\n"+
      "  E1 : [D1, C1]\n"+
      "  B1 : [A1, E1]\n"+
      "";
    test_spreadsheet(sheet, expect);
    expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    E1 |   13.0 | '=4*2 + 5'\n"+
      "    D1 |  ERROR | '=E1*5 + A1'\n"+
      "    C1 |   52.0 | '=E1*4'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  D1 : [A1, E1]\n"+
      "  C1 : [E1]\n"+
      "Downstream Links:\n"+
      "  A1 : [D1]\n"+
      "  E1 : [D1, C1]\n"+
      "";
    test_spreadsheet(sheet2, expect);
    cycleFound = false;
    try{
      sheet2.setCell("A1","=42 / (10 + 11) - 1");
      sheet2.setCell("E1","= X1 + Y1 + Z1");
    }
    catch(DAG.CycleException e){
      cycleFound = true;
    }
    assertFalse("Should be NO cycles\nSpreadsheet:\n"+sheet2.toString(),cycleFound);
    expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |    1.0 | '=42 / (10 + 11) - 1'\n"+
      "    E1 |  ERROR | '= X1 + Y1 + Z1'\n"+
      "    D1 |  ERROR | '=E1*5 + A1'\n"+
      "    C1 |  ERROR | '=E1*4'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  E1 : [Z1, Y1, X1]\n"+
      "  D1 : [A1, E1]\n"+
      "  C1 : [E1]\n"+
      "Downstream Links:\n"+
      "  A1 : [D1]\n"+
      "  Z1 : [E1]\n"+
      "  Y1 : [E1]\n"+
      "  X1 : [E1]\n"+
      "  E1 : [D1, C1]\n"+
      "";
    test_spreadsheet(sheet2, expect);
    cycleFound = false;
    try{
      sheet2.setCell("X1","= 1+3");
      sheet2.setCell("Y1","2.5");
      sheet2.setCell("Z1","4");
      sheet2.setCell("X1","=A1+4");
      sheet2.setCell("A1","=C1/2");
    }
    catch(DAG.CycleException e){
      cycleFound = true;
    }
    assertTrue("Should have found a cycle\nSpreadsheet:\n"+sheet2.toString(),cycleFound);
    expect =
      "    ID |  Value | Contents\n"+
      "-------+--------+---------------\n"+
      "    A1 |    1.0 | '=42 / (10 + 11) - 1'\n"+
      "    Z1 |    4.0 | '4'\n"+
      "    Y1 |    2.5 | '2.5'\n"+
      "    X1 |    5.0 | '=A1+4'\n"+
      "    E1 |   11.5 | '= X1 + Y1 + Z1'\n"+
      "    D1 |   58.5 | '=E1*5 + A1'\n"+
      "    C1 |   46.0 | '=E1*4'\n"+
      "\n"+
      "Cell Dependencies\n"+
      "Upstream Links:\n"+
      "  X1 : [A1]\n"+
      "  E1 : [Z1, Y1, X1]\n"+
      "  D1 : [A1, E1]\n"+
      "  C1 : [E1]\n"+
      "Downstream Links:\n"+
      "  A1 : [X1, D1]\n"+
      "  Z1 : [E1]\n"+
      "  Y1 : [E1]\n"+
      "  X1 : [E1]\n"+
      "  E1 : [D1, C1]\n"+
      "";
    test_spreadsheet(sheet2, expect);
  }
}
