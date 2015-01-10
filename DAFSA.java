package DAFSA;
import java.util.HashMap;
import java.util.HashSet;
import java.lang.StringBuilder;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.nio.charset.StandardCharsets;

/* 
 * This is a Java implementation of a deterministic acyclic finite
 * state automaton (DAFSA) data structure used for storing a finite
 * number of strings in a space-efficient way.  It constructs and 
 * initially populates itself using a dictionary text file.
 *
 * This data structure supports a query operation that runs in time
 * proportional to the number of characters in the query.
 * Read http://en.wikipedia.org/wiki/Deterministic_acyclic_finite_state_automaton
 * for more information. 
 *
 * Command-line arguments are parsed as filepaths to text files containing
 * words to add into the data structure. 
 *
 * @author David Weinberger (davidtweinberger@gmail.com) 
 * Adapted from http://stevehanov.ca/blog/index.php?id=115
 */

public class DAFSA {

	private String _previousWord;
	private DAFSA_Node _root;

	//list of nodes that have not been checked for duplication
	private ArrayList<Triple> _uncheckedNodes;

	//list of unique nodes that have been checked for duplication
	private HashSet<DAFSA_Node> _minimizedNodes;

	public DAFSA(){
		_previousWord = "";
		_root = new DAFSA_Node();
		_uncheckedNodes = new ArrayList<Triple>();
		_minimizedNodes = new HashSet<DAFSA_Node>(); //TODO type
	}

	//A class representing an immutable 3-tuple of (node, character, node)
	private class Triple {
		public final DAFSA_Node node;
		public final Character letter;
		public final DAFSA_Node next;
		public Triple(DAFSA_Node no, Character le, DAFSA_Node ne){
			node = no;
			letter = le;
			next = ne;
		}
	}

	//A (static) class representing a node in the data structure.
	private static class DAFSA_Node {
		//class variables
		private static int currentID = 0;

		//instance variables
		private int _id;
		private Boolean _final;
		private HashMap<Character, DAFSA_Node> _edges;

		public DAFSA_Node(){
			_id = new Integer(DAFSA_Node.currentID); DAFSA_Node.currentID++;
			_final = false;
			_edges = new HashMap<Character, DAFSA_Node>();
		}

		@Override
		public boolean equals(Object obj){
			return ((DAFSA_Node) obj).toString().equals(toString());
		}

		@Override
		public int hashCode(){
			int hash = 1;
			hash += 17*_id;
			hash += 31*_final.hashCode();
			hash += 13*_edges.hashCode();
			return hash;
		}

		//representation of this node as a string
		public String toString(){
			StringBuilder sb = new StringBuilder();
			if (_final){
				sb.append("1");
			} else {
				sb.append("0");
			}
			for (Entry<Character, DAFSA_Node> entry : _edges.entrySet()){
				sb.append("_");
				sb.append(entry.getKey());
				sb.append("_");
				sb.append(entry.getValue().getId());
			}
			return sb.toString();
		}

		//accessors
		public int getId(){ return _id; }
		public Boolean getFinal(){ return _final; }

		//mutators
		public void setId(int i){ _id = i; }
		public void setFinal(Boolean b){ _final = b; }

		//add edges to the hashmap
		public void addEdge(Character letter, DAFSA_Node destination){
			_edges.put(letter, destination);
		}

		public Boolean containsEdge(Character letter){
			return _edges.containsKey(letter);
		}

		public DAFSA_Node traverseEdge(Character letter){
			return _edges.get(letter);
		}

		public int numEdges(){
			return _edges.size();
		}

		public HashMap<Character, DAFSA_Node> getEdges(){
			return _edges;
		}

	}

	public void insert(String word){
		//if word is alphabetically before the previous word
		if (_previousWord.compareTo(word) > 0) {
			System.err.println("Inserted in wrong order:" + _previousWord + ", " + word);
			return;
		}

		//find the common prefix between word and previous word
		int prefix = 0;
		int len = Math.min(word.length(), _previousWord.length());
		for (int i=0; i<len; i++){
			if (word.charAt(i) != _previousWord.charAt(i)){
				break;
			}
			prefix += 1;
		}

		//check the unchecked nodes for redundant nodes, proceeding from
		//the last one down to the common prefix size.  Then truncate the list at that point.
		minimize(prefix);

		//add the suffix, starting from the correct node mid-way through the graph
		DAFSA_Node node;
		if (_uncheckedNodes.size() == 0) {
			node = _root;
		}  else {
			node = _uncheckedNodes.get(_uncheckedNodes.size() - 1).node;
		}

		String remainingLetters = word.substring(prefix); //the prefix+1th character to the end of the string

		for (int j=0; j<remainingLetters.length(); j++){
			DAFSA_Node nextNode = new DAFSA_Node();
			Character letter = remainingLetters.charAt(j);
			node.addEdge(letter, nextNode);
			_uncheckedNodes.add(new Triple(node, letter, nextNode));
			node = nextNode;
		}

		node.setFinal(true);
		_previousWord = word;

	}

	public void minimize(int downTo){
		// proceed from the leaf up to a certain point
		for (int i = _uncheckedNodes.size() - 1; i >= downTo; i--){ 
			Triple t = _uncheckedNodes.get(i);
			java.util.Iterator<DAFSA_Node> iter = _minimizedNodes.iterator();
			Boolean foundMatch = false;
			while (iter.hasNext()){
				DAFSA_Node match = iter.next();
				if (t.next.equals(match)){
					//replace the child with the previously encountered one
					t.node.addEdge(t.letter, t.next);
					foundMatch = true;
					break;
				}
			}
			if (!foundMatch){
				_minimizedNodes.add(t.next);
			}
			_uncheckedNodes.remove(i);/*
			if (_minimizedNodes.contains(t.next)){
				t.node.addEdge(t.letter, t.next);
			} else {
				_minimizedNodes.add(t.next);
			}
			_uncheckedNodes.remove(i);*/
		}
	}

	public Boolean contains(String word){
		DAFSA_Node node = _root;
		Character letter;
		for (int i=0; i<word.length(); i++){
			letter = word.charAt(i);
			if (node.containsEdge(letter) == false){
				return false;
			} else {
				node = node.traverseEdge(letter);
			}
		}
		if (node.getFinal() == true){
			return true;
		} else {
			return false;
		}
	}

	public int nodeCount(){
		//counts nodes
		return _minimizedNodes.size();
	}

	public int edgeCount(){
		//counts edges
		int count = 0;
		java.util.Iterator<DAFSA_Node> iter = _minimizedNodes.iterator();
		DAFSA_Node curr;
		while (iter.hasNext()) {
			curr = iter.next();
			System.out.println(curr.toString());
			count += curr.numEdges();
		}
		return count;
	}

	public void finish(){
		//minimize all unchecked nodes
		minimize(0);
	}

	public int parseWordsFromFile(String fileName){
		Path path = Paths.get(fileName);
		int wordCount = 0;
		try (Scanner scanner =  new Scanner(path, StandardCharsets.UTF_8.name())) {
			while (scanner.hasNextLine()){
				String nextline = scanner
									.nextLine()
									.toLowerCase()
									.replaceAll("\n", "")
									.replaceAll("'", "");
				String[] words = nextline.split(" ");
				for (String word : words){
					insert(word); //inserts into the data structure
					wordCount++;
					if (wordCount % 1000 == 0){
						System.out.println(wordCount);
					}
				}
			}
		} catch (IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		return wordCount;
	}

	public static void main(String[] args) {
		DAFSA d = new DAFSA();
		int global_count = 0;
		for (int i=0; i<args.length; i++){
			global_count += d.parseWordsFromFile(args[i]);
		}
		d.finish();
		System.out.println("Finished! Inserted " + global_count + " words from " + args.length + " files.");
		System.out.println("Node count: " + d.nodeCount());
		System.out.println("Edge count: " + d.edgeCount());


		System.out.println("contains hello: " + d.contains("hello"));
	}
}