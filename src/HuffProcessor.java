import java.util.PriorityQueue;

/**
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
 * 
 * @author Owen Astrachan
 */

public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); 
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private final int myDebugLevel;
	
	public static final int DEBUG_HIGH = 4;
	public static final int DEBUG_LOW = 1;
	
	public HuffProcessor() {
		this(0);
	}
	
	public HuffProcessor(int debug) {
		myDebugLevel = debug;
	}

	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void compress(BitInputStream in, BitOutputStream out){
		
		int[] freq /*counts*/ = readForCounts(in);
		HuffNode root = makeTreeFromCounts(freq);
		String[] codings = makeCodingsFromTree(root);
		
		out.writeBits(BITS_PER_INT, HUFF_TREE);
		writeHeader(root, out);
		
		in.reset();
		writeCompressedBits(codings, in, out);
		out.close();
		
//		while (true){
//			int val = in.readBits(BITS_PER_WORD);
//			if (val == -1) break;
//			out.writeBits(BITS_PER_WORD, val);
//		}
//		out.close();
	}
		
	private int[] readForCounts(BitInputStream in) {
		int[] freq = new int[ALPH_SIZE + 1];
		freq[PSEUDO_EOF] = 1;
		
		while(true) {
			int bit = in.readBits(BITS_PER_WORD);
			
			// Break when it reaches the end
			if (bit == -1) {
				break;
			}
			
			// The rest
			else {
				freq[bit] += 1;
			}
		}
		return freq;
	}
	
	private HuffNode makeTreeFromCounts(int[] freq) {
		PriorityQueue<HuffNode> pq = new PriorityQueue<>();
		
		// Loop through every index in freq that has value > 0
		for (int i = 0; i < freq.length; i++) {
			if (freq[i] > 0) {
				pq.add(new HuffNode(i, freq[i], null, null));
			}
		}
		
		while (pq.size() > 1) {
			HuffNode left = pq.remove();
			HuffNode right = pq.remove();
			// create new HuffNode t with weight from left.weight + right.weight
			// and left, right subtrees
			
			// Is the following 0 value correct?
			HuffNode t = new HuffNode(0, left.myWeight + right.myWeight, left, right);
			pq.add(t);
		}
		HuffNode root = pq.remove();
		
		if (myDebugLevel >= DEBUG_HIGH) {
			System.out.printf("pq created with %d nodes\n", pq.size());
		}
		
		return root;
	}
	
	private String[] encodings = new String[ALPH_SIZE + 1];
	private String[] makeCodingsFromTree(HuffNode root) {
		codingHelper(root, "", encodings);
		return encodings;
	}
	private void codingHelper(HuffNode root, String path, String[] encodings) {
		if (root.myLeft == null && root.myRight == null) {
			encodings[root.myValue] = path;
			return;
		}
		codingHelper(root.myLeft, path + "0", encodings);
		codingHelper(root.myRight, path + "1", encodings);
		
		if (root.myLeft == null && root.myRight == null) {
			encodings[root.myValue] = path;
			if (myDebugLevel >= DEBUG_HIGH) {
				System.out.printf("encoding for %d is %s\n", root.myValue, path);
			}
			return;
		}
	}
	
	private void writeHeader(HuffNode root, BitOutputStream out) {
		if (root == null) {
			return;
		}
//		if (root.myLeft == null && root.myRight == null) {
//			out.writeBits(BITS_PER_INT, 1);
//			out.writeBits(BITS_PER_WORD + 1, root.myValue);
//		}
		if (root.myLeft == null && root.myRight == null) {
			out.writeBits(1, 1);
			out.writeBits(BITS_PER_WORD + 1, root.myValue);
			//myBitsWritten += BITS_PER_WORD + 2; 
			//return;
		}
		
		out.writeBits(BITS_PER_INT, 0);
		writeHeader(root.myLeft, out);
		writeHeader(root.myRight, out);
	}
	
	private void writeCompressedBits(String[] codings, BitInputStream in, BitOutputStream out) {
		String code = "";
		
		
//		if bit != -1
//				
//				if == -1
		while (true) {
			int bit = in.readBits(BITS_PER_WORD);
			if (bit == -1) {
				code = codings[PSEUDO_EOF];
				out.writeBits(code.length(), Integer.parseInt(code, 2));
				break;
			}
//			if ((bit == PSEUDO_EOF)) {
//				code = codings[PSEUDO_EOF];
//				out.writeBits(code.length(), Integer.parseInt(code, 2));
//				return;
//			}
			code = codings[bit];
			out.writeBits(code.length(), Integer.parseInt(code, 2));
		}
	}
	
	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void decompress(BitInputStream in, BitOutputStream out){
		int bits = in.readBits(BITS_PER_INT);
		if (bits != HUFF_TREE) {
			throw new HuffException("illegal header starts with " +bits);
		}
		// also throw exception if reading bits ever fails (readBits returns -1)
		
		HuffNode root = readTreeHeader(in);
		readCompressedBits(root, in, out);
		out.close();

//		while (true){
//			int val = in.readBits(BITS_PER_WORD);
//			if (val == -1) break;
//			out.writeBits(BITS_PER_WORD, val);
//		}
//		out.close();
	}
	
	private HuffNode readTreeHeader(BitInputStream in) {
		int bits = in.readBits(1);
		if (bits == -1) {
			throw new HuffException("bit size invalid");
		}

		// should I make a leaf node for PSEUDO_EOF?
		if (bits == 0) {
			HuffNode left = readTreeHeader(in);
			// should I be abridging the "in"?
			HuffNode right = readTreeHeader(in);
			return new HuffNode(0, 0, left, right);
		} 
		
		else {
			int value = in.readBits(BITS_PER_WORD + 1);
			return new HuffNode(value, 0, null, null);
		}
	}
	
	private void readCompressedBits(HuffNode root, BitInputStream in, BitOutputStream out) {
		HuffNode current = root;
		while (true) {
			int bits = in.readBits(1);
			
			// Throw an exception
			if (bits == -1) {
				throw new HuffException("bad input, no PSEUDO_EOF");
			}
			
			// No exception
			else {
				
				// Go left when bits == 0
				if (bits == 0) {
					current = current.myLeft;
				}
				
				// Go right when bits == 1
				else {
					current = current.myRight;
				}
				
				if ((current.myLeft == null) && (current.myRight == null)) {
					// Last node
					if (current.myValue == PSEUDO_EOF) {
						break;
					}
					
					// Write bits for current.value
					else {
						// Note: you break when reaching PSEUDO_EOF, so when 
						// you write a leaf value to the output stream, you write 8, or BITS_PER_WORD bits
						out.writeBits(BITS_PER_WORD, current.myValue);
						current = root; // start back after leaf
					}
				}
			}
		}
	}
	
}