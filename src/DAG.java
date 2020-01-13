import java.util.HashMap;
import java.util.Set;
import java.util.*;


// Model a Directed Acyclic Graph (DAG) which allows nodes (vertices)
// to be specified by name as strings and added to the DAG by
// specifiying their upstream dependencies as a set of string IDs.
// Attempting to introduce a cycle causes an exception to be thrown.
public class DAG{


    // map that hold downstream link
    protected Map<String, Set<String>> downstreamLinks;

    // map that holds upstream links
    protected Map<String, Set<String>> upstreamLinks;

    // constructor creates empty hash maps
    public DAG(){
        downstreamLinks = new HashMap<String, Set<String>>();
        upstreamLinks   = new HashMap<String, Set<String>>();
    }

    // string representation of DAG
    // loops through each hashmap:
    // Runtime Complexity: O(U+D)
    // U = number of upstream links
    // D = number of downstream links
    @Override public String toString(){
        StringBuilder out = new StringBuilder();
        out.append("Upstream Links:\n");
        Set<String> ids = upstreamLinks.keySet();
        Iterator<String> idIter = ids.iterator();

        while (idIter.hasNext()){
            String id = idIter.next();
            Set<String> linkSet = upstreamLinks.get(id);
            out.append(String.format("%4s : %s\n", id, linkSet));
        }

        out.append("Downstream Links:\n");

        Set<String> upLinkIds = downstreamLinks.keySet();

        Iterator<String> iter =  upLinkIds.iterator();

        while (iter.hasNext()){
            String id = iter.next();
            Set<String> linkSet = downstreamLinks.get(id);
            if (linkSet != null && !linkSet.isEmpty())
                out.append(String.format("%4s : %s\n", id, linkSet));
        }
        return out.toString();
    }

    // returns the upstream links
    // returns an empty set if id does not have upstreamLinks
    // Runtime: O(1)
    public Set<String> getUpstreamLinks(String id){

        Set<String> upStreams = upstreamLinks.get(id);

        if(upStreams == null){
            return new HashSet<>();
        }
        return upStreams;
    }

    // returns the downstream links
    // returns an empty set if id does not have downstreamLinks
    // Runtime: O(1)
    public Set<String> getDownstreamLinks(String id){

        Set<String> downStreams = downstreamLinks.get(id);

        if (downStreams == null){
            return new HashSet<>();
        }

        return downStreams;
    }

    // Class representing a cycle that is detected on adding to the DAG
    public static class CycleException extends RuntimeException{

        // Construct an exception with the given error message
        public CycleException(String msg){
            super(msg);
        }

    }

    // adds a new id into the DAG
    // runtime complexity is generally  O(P + D) (see case 3)
    // P: longest path in DAG starting from id
    // D ( worst case ):  size of downstream links
    // However, runtime changes on a case-by-case basis
    public void add(String id, Set<String> upstreamIDs){

        // case 1 : upstreamIds is an empty set
        // remove id from DAG
        // Runtime: O(D)
        // D : number of downstramLinks in DAG
        if (upstreamIDs.isEmpty()){
            upstreamLinks.remove(id);
            Set<String> downs = downstreamLinks.keySet();
            for (String s : downs){
                removeFromDownstream(id, s);
            }
            return;
        }

        // case 2 : upstreamLinks is empty
        // this is the first add in a dag
        // Runtime: O(Up) --> Up : size if upstreamIDs
        // this is determined by the while loop that iterates
        // through each upstream ID
        if (upstreamLinks.isEmpty()) {
            upstreamLinks.put(id, upstreamIDs);
            String cycle = checkForCycles(id);
            if (cycle != null){
                upstreamLinks.remove(id);
                throw new CycleException(cycle);
            }
            for(String next : upstreamIDs){
                Set<String> dep = new HashSet<>();
                dep.add(id);
                downstreamLinks.put(next,dep);
            }
            return;
        }

        // case 3: (average case) upstreamLinks is not empty
        // Runtime: O(P + D) --> P: longest path from newly placed id
        // D : number of downstream links
        // this is because the DAG is checked for any cycles starting from id
        // and a each downstream link is checked to see if it has id in its downstream links

        // upstream set before adding id
        Set<String> currentDependents = upstreamLinks.get(id);
        // new set that contains all upstream Ids, and might contain all currentDependents
        // if current dependents is not null
        Set<String> dependencies = new HashSet<>(); dependencies.addAll(upstreamIDs);

        if (currentDependents != null){
            dependencies.addAll(currentDependents);
        }


        upstreamLinks.remove(id);
        upstreamLinks.put(id, upstreamIDs);

        // checks for cycles
        // if a string is returned, then it has a cycle
        // and the id is removed
        // else the id is added to both upstream and downstream maps
        // Runtime : O(P+D)
        String cycle = checkForCycles(id);
        if (cycle != null){
            if (currentDependents == null || currentDependents.isEmpty()) {
                upstreamLinks.remove(id);
                throw new CycleException(cycle);
            }
            upstreamLinks.remove(id);
            upstreamLinks.put(id, currentDependents);

            throw new CycleException(cycle);
        }

        // adds currentDependencies to dependencies, to
        // lower the bounds
        // thus it is not technically O(D), but worse case
        // is O(D)
        Iterator<String> iter = dependencies.iterator();
        // checks ever dependency
        // if dependency already exists, then
        //   if dependency is in upStreamIds
        //      then add id to downstreamLinks
        //   else remove it
        // else: create a new downstream id and make a new
        //       set containing id in it
        while (iter.hasNext()){

            String dependent = iter.next();

            if (downstreamLinks.containsKey(dependent)){
                if (upstreamIDs.contains(dependent)){
                    downstreamLinks.get(dependent).add(id);
                } else {
                    Set<String> d = downstreamLinks.get(dependent);
                    d.remove(id);
                    if (d.isEmpty()){
                        downstreamLinks.remove(dependent);
                    }
                }
            } else {
                // create new set and add it to downstreamLinks
                Set<String> dep = new HashSet<>();
                dep.add(id);
                downstreamLinks.put(dependent,dep);
            }
        }
    }


    // Helper method which checks for cyclic dependencies starting at
    // the id given.  Uses the static checkForCyles helper method below.
    // If a cycle is found, return a String representation of the cycle.
    // If no cycle is found, return null.
    protected String checkForCycles(String id){
        List<String> path = new ArrayList<String>(Arrays.asList(id));
        if(checkForCycles(upstreamLinks, path)){
            return path.toString();
        }
        return null;
    }

    // recursive helper method is used to check for cycles in DAG
    // uses depth first search algorithm, which is O(P) --> P: longest path from id
    // returns true if cycle is found and false if no cycle is found
    public static boolean checkForCycles(Map<String, Set<String>> links, List<String> curPath) {

        String lastNode = curPath.get(curPath.size() - 1);
        Set<String> neighbors = links.get(lastNode);

        if (neighbors == null || neighbors.isEmpty()) {
            return false;
        }

        for (String nid : neighbors) {
            curPath.add(nid);
            if (curPath.get(0).equals(nid)) {
                return true;
            }

            boolean result = checkForCycles(links, curPath);
            if (result) {
                return true;
            }

            curPath.remove(curPath.size() - 1);
        }
        return false;
    }

    // removes a given id from both hash maps
    // removes id from all downstream links wich makes
    // runtime: O(L_i)
    public void remove(String id){
        if (upstreamLinks.get(id) == null || upstreamLinks.get(id).isEmpty()){
            return;
        }

        Set<String> up = upstreamLinks.get(id);
        upstreamLinks.remove(id);

        for (String link: up){
            removeFromDownstream(id,link);
        }

    }


    // Helper that eliminates the downstream link from upID to downID
    protected void removeFromDownstream(String upID, String downID){
        if (downID == null){return;}
        downstreamLinks.get(downID).remove(upID);
    }

    // did'nt see this until after I wrote add(0
//    protected void addToDownstream(String upID, String downID){
//    }

}
