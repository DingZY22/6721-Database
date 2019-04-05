import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Scanner;

public class Driver {

	public static final int TUPLE_SIZE = 251;// one bytes to save the "\n"
	public static final int TUPLES_PER_BLOCK = 15;
	public static final int BLOCK_SIZE =  TUPLE_SIZE * TUPLES_PER_BLOCK;// 3765 
	public static final int SUM_TUPLE_SIZE = 80;// 9 bytes for CID, 18 bytes for, 23 bytes for '\b'  modified: cname 25
	public static final int SUM_SIZE = 18; // start from 12.	
	public static final byte[] MAX_ID = "111155992010-08-05999999999Sauncho Auchterlonie     Oklahoma City OK 73140 South                                                                                                                          sauchterloniegg@technorati.c04265590.66154210.31\n".getBytes();
	public static byte[] zero = "0".getBytes();
	public static final int MINUTE = 60000;  
	public static String[] TOP_TEN_MOST = new String[10];

	public static boolean DEBUG = true;
	public static int MAX_NUM_BLOCKS = 0;
	public static int MAX_NUM_TUPLES = 0;
	public static float RUNNING_MEMORY = 1024; // reserved memory to run the program
	public static final float K = 1024.0f;
	public static final float M = K * K;
	public static int io = 0;// count the numbers of I/O in total 

	public static void main(String[] args) throws IOException{

		init_();
		if(DEBUG) {

			System.out.println("************ END ***************");

		}

	}

	/**
	 * 
	 * the entrance for the entire program 
	 * And record the performing time and I/O times
	 * @throws IOException 
	 */
	public static void init_() throws IOException {


		float freeMemory = (float) Math.floor(Runtime.getRuntime().freeMemory() / (M));
		float totalMemory = (float)  Runtime.getRuntime().totalMemory() / M ;

		if(DEBUG) {
			System.out.println("Total Memory:" + totalMemory  + "MB");
			System.out.println("Free Memory:" + freeMemory + "MB");


		}

		if(freeMemory > 7) 
		{
			RUNNING_MEMORY *= 3.4;
		} 
		else 
		{
			RUNNING_MEMORY *= 1.7;
		}


		MAX_NUM_BLOCKS = (int) Math.floor((Runtime.getRuntime().freeMemory() - RUNNING_MEMORY * K) / BLOCK_SIZE);
		MAX_NUM_TUPLES = TUPLES_PER_BLOCK * MAX_NUM_BLOCKS;

		long startTime = System.currentTimeMillis();

		if(DEBUG) {
			System.out.println("**************************" + " PHASE ONE **************************");
			System.out.println("Free memory after reservation: " + (Runtime.getRuntime().freeMemory() - RUNNING_MEMORY * K) / M + "MB");
			System.out.println("Max tuples to fill:" + MAX_NUM_TUPLES+" tuples");
			System.out.println("Max blocks to fill:" + MAX_NUM_BLOCKS+" blocks");
		}
		Scanner reader = new Scanner(System.in);

		System.out.println("Please the number of tuples to test the program: 1 for 10K records\n "
				+ "                                                2 for 1M records\n"
				+ "                                                 3 for 1.5K records ");

		int choice = reader.nextInt();
		String files = null;
		while (choice != 1 && choice != 2 && choice != 3) {

			System.out.println("Please the number of tuples to test the program. 1 for 10K records, 2 for 1M records, 3 for 1.5K records ");


			choice = reader.nextInt();
		}

		if (choice == 1) {
			files = "input100000.txt";
		}
		else if (choice == 2) {
			files = "input1000000.txt";
		}
		else if (choice == 3) {
			files = "input.txt";
		}

		int numOfsubs = phaseOne("resource/" + files, "f1");
		if(DEBUG) {

			System.out.println("The I/O in phase1:" + io);
			recordTime(startTime, "The execute time for phase1");
			System.out.println("***********************" + " END OF PHASE ONE ***********************");
		}

		//Start of the PhaseTwo Multiple-way Merge
		phaseTwo("f1",numOfsubs);

		if(DEBUG) {

			System.out.println("The I/O in phase2:" + io);
			recordTime(startTime, "The execute time for phase2");
			System.out.println("***********************" + " END OF PHASE TWO ***********************");
		}

		int num_of_client = recordSums("resource/output/f1_sorted.txt");

		if(DEBUG) {

			System.out.println("The I/O in phase3:" + io);
			recordTime(startTime, "The execute time for phase3");
			System.out.println("The total number of clients is " + num_of_client);
			System.out.println("***********************" + " END OF RECORD SUM ***********************");
		}


		//System.out.println("Please enter the ID you want to calculate the sum: ");
		//int cid = reader.nextInt();
		int cid = 998943288;
		System.out.println("The sum is for id " + cid + ": "  + searchSum("resource/output/_sums.txt", cid));
		reader.close();

		if(DEBUG) {
			System.out.println("******************Top 10 most amount paid tuples are: ");
		}
		top10Print("resource/output/_sums.txt");

		if(DEBUG) {
			System.out.println("The I/O in this phase is:" + io);
			recordTime(startTime, "The execute time for this phase");
			System.out.println("***********************" + " END OF TOP10 RECORD ***********************");
		}


	}

	/**
	 * phase 1 do the split and sort put for the 2pmms for a large input of files of tuple

	 * @param inputFile // the file needs to be sorted and merge for the phase 2
	 * @param outputPrefix //the output prefix for the files
	 * @returnt the number of files need to be merged in the phase 2
	 */
	public static int phaseOne(String inputFile, String outputPrefix){

		int sublistCount = 0;//count the number of files ready to pass to phase2
		int bufferlength = 0;//count the bytes in a block
		int lines = 0;// count the lines of tuples in a subfile
	
		byte[][] tempsublist = new byte[MAX_NUM_TUPLES][TUPLE_SIZE];
		byte[] blockBuffer = new byte[BLOCK_SIZE];// input & output.

		try{

			FileInputStream in = new FileInputStream(inputFile);			
			while ((bufferlength = in.read(blockBuffer))!= -1) {

				//io counts ++ each time read a block from a file.
				io++;
				// not always blocks of 16
				// the offset of the last read is not 16 !!!!
				for (int i = 0; i < bufferlength / TUPLE_SIZE; i++) {

					System.arraycopy(blockBuffer, TUPLE_SIZE * i, tempsublist[lines++], 0 , TUPLE_SIZE);

				}

				//if the lines of tuples reach the maximum numbers of the main memory or 
				//the number of bytes is less than the buffer size 
				//then sort the sublist and write to the files

				//Note that Two conditions to trigger the sort
				//1. reach the maximum number of tuples or blocks for a subfile
				//2. not enough blocks to fit a block buffer call in.available() == 0
				if (lines == MAX_NUM_TUPLES  ||  in.available() == 0) {

					// sort the sublist
					//Implement a better a sort algorithm may have a better performing time

					QuickSort.sort(tempsublist, 0, lines - 1);
					FileOutputStream out = new FileOutputStream("resource/output/" + outputPrefix + "_" + sublistCount++ + ".txt");
				
					int lineCount = 0;
					//write the sublist to the block buffer and then write to the files

					while (lineCount < lines) {

						// take the tuples into the output buffer
						System.arraycopy(tempsublist[lineCount], 0 , blockBuffer, TUPLE_SIZE * (lineCount % TUPLES_PER_BLOCK), TUPLE_SIZE);					
						lineCount++;


						//if output buffer is full
						if (lineCount % TUPLES_PER_BLOCK == 0) {


							out.write(blockBuffer, 0, BLOCK_SIZE);
							//io counts ++ each time write a block to a file
							io++;
							



						} 
						//if no more lines
						else if (lineCount == lines) {

							//System.out.println(sublistCount);
							out.write(blockBuffer, 0, TUPLE_SIZE * (lineCount % TUPLES_PER_BLOCK));
							io++;
							//outtest.write(blockBuffer, 0, TUPLE_SIZE * (lineCount % TUPLES_PER_BLOCK));





						}
					}

					lines = 0;
					out.close();
					out = null;
					if (DEBUG) {
						//System.out.println("Finish " + sublistCount + " sublist, Free Memory:" + Runtime.getRuntime().freeMemory() / K + "KB");
					}
				}
			}

			in.close();
			in = null;

		}
		catch(IOException e){
			e.printStackTrace();
		}

		finally {
			tempsublist = null;
			blockBuffer = null;
			System.gc();
		}

		return sublistCount;
	}


	/**
	 * phasse 2 merge the split files into a original-size file with the sorted ID order
	 * 
	 * @param outputPrefix of the file name 
	 * @param num_files the number of files return by phase one 
	 * @throws IOException
	 */
	public static void phaseTwo(String outputPrefix,int num_files) throws IOException {

		System.gc();

		//count the numbers of blocks available on the main memory
		//In 5mb it is around 82 blocks.
		//Slides DB2 / (8)
		int inputBufferBlocks =(int) Math.floor((Runtime.getRuntime().freeMemory() - RUNNING_MEMORY * K) / BLOCK_SIZE) - 1;

		//count the numbers of the blocks distribute for each sublist.
		int avgBlocksEachList = (int) Math.floor(inputBufferBlocks / num_files);

		//numbers of input buffers each can store TUPLES_PER_BLOCK*TUPLE_SIZE Numbers tuples
		int TuplesEachList = avgBlocksEachList * TUPLES_PER_BLOCK;

		if(DEBUG) {

			System.out.println(" ");
			System.out.println("**************************" + " PHASE TWO **************************");
			System.out.println("Number of sub files is " + num_files );
			System.out.println("Max Blocks to fill for current free memory:"+ inputBufferBlocks);
			System.out.println("Average Block for each sub file:"+ avgBlocksEachList);
			System.out.println("Number of tuples for each input buffer:"+ TuplesEachList);
			//System.out.println("Number of blocks in each subfile are: " + MAX_NUM_BLOCKS );

		}

		/*
		 * Declare all the variables for the loading and merge procedure
		 */
		// [number of sub files][lines of tuple for each sub files][bytes of a tuple]
		byte[][][] inputBuffer = new byte[num_files][TuplesEachList][TUPLE_SIZE];
		byte[][] outputBuffer = new byte[TUPLES_PER_BLOCK][];
		byte[] blockBuffer = new byte[BLOCK_SIZE];

		//count the number of lines in one input buffer
		int[] tuple_counts = new int[num_files];
		//the pointers for each input buffer to point the smallest unchosen tuple
		int[] input_buffer_pointers = new int[num_files];
		//count the number of bytes in each sub file reader
		int[] readLength = new int[num_files];

		int output_Buffer_pointer = 0;
		boolean EmptyInputBuffer = false;
		int smallest_buffer_index = 0;
		//read file over
		boolean RFO = false;

		FileInputStream[] InputReader = new FileInputStream[num_files];

		try {

			// prepare the number of file stream readers to read each sub files 
			for (int i =0; i < num_files;i++) {

				InputReader[i] = new FileInputStream("resource/output/" + outputPrefix + "_" + i + ".txt");
			}
			// And one output writer
			FileOutputStream output = new FileOutputStream("resource/output/" +outputPrefix + "_sorted.txt");	

			while (RFO == false) {

				//loading the sub files content by blocks into input buffers to do the merge
				for (int i = 0; i < num_files; i++) {

					//refill the input buffer
					if(tuple_counts[i] != 0 || readLength[i] == -1) {
						continue;
					}

					int tuple_lines = 0;					
					while ((readLength[i] = InputReader[i].read(blockBuffer))!= -1) {

						//count 1 io times for each read
						io++;

						for(int j = 0;j < readLength[i]/TUPLE_SIZE;j++) {

							// load the sub files content into the input buffer to do the compare 
							System.arraycopy(blockBuffer, TUPLE_SIZE * j, inputBuffer[i][tuple_lines++], 0, TUPLE_SIZE);
						}

						// initialize the numbers of each input buffer 
						tuple_counts[i] = tuple_lines;

						// If the current loading blocks reach the threshold of current 
						// Break the loading process to do merge
						if (tuple_lines / TUPLES_PER_BLOCK == avgBlocksEachList) {
							break;
						}
					}
				}

				//merge				
				EmptyInputBuffer = false;
				smallest_buffer_index = 0;

				while (!EmptyInputBuffer) {

					outputBuffer[output_Buffer_pointer] = MAX_ID;

					// Find the smallest ID via each iteration
					for (int i = 0; i < num_files; i++) {

						if (input_buffer_pointers[i] < tuple_counts[i] && compare(inputBuffer[i][input_buffer_pointers[i]], outputBuffer[output_Buffer_pointer]) < 0) 
						{		

							outputBuffer[output_Buffer_pointer] = inputBuffer[i][input_buffer_pointers[i]];
							smallest_buffer_index = i;
						}



					}

					if(compare(outputBuffer[output_Buffer_pointer], MAX_ID) == 0) {
						RFO = true;
					} 

					else {

						output_Buffer_pointer++;

						input_buffer_pointers[smallest_buffer_index]++;
						if(input_buffer_pointers[smallest_buffer_index] == tuple_counts[smallest_buffer_index]) {	

							if(readLength[smallest_buffer_index] != -1) {

								input_buffer_pointers[smallest_buffer_index] = 0;
								tuple_counts[smallest_buffer_index] = 0;
								EmptyInputBuffer = true;


							}



						}
					}

					if (output_Buffer_pointer == TUPLES_PER_BLOCK  || EmptyInputBuffer|| RFO) {

						io++;

						for (int i = 0; i < output_Buffer_pointer; i++) {

							System.arraycopy(outputBuffer[i], 0, blockBuffer, TUPLE_SIZE * i, TUPLE_SIZE);
						}

						output.write(blockBuffer, 0, TUPLE_SIZE * output_Buffer_pointer);
						output_Buffer_pointer = 0;

					}

					if(RFO) {
						break;
					}
				}
			}

			output.close();
			output = null;

			for (int i = 0; i < num_files; i++) {
				InputReader[i].close();
				InputReader[i] = null;

				File file = new File("resource/output/" + outputPrefix + "_" + i + ".txt");
				file.delete();
			}

		}

		catch (IOException e) {
			e.printStackTrace();
		}

		finally {

			inputBuffer = null;
			blockBuffer = null;
			tuple_counts = null;
			readLength = null;
			input_buffer_pointers = null;
			outputBuffer = null;
			System.gc();		
		}

	}

	/**
	 * sum up paid amount with regard to each same client id from the _sorted file 
	 * create new sum tuple together with id 
	 * And write to the new file 
	 * 
	 * @param sorted file name
	 * @return total number of client in this phase
	 * @throws IOException
	 */
	public static int recordSums(String sortedFile) throws IOException {

		System.gc();

		//Declare the variables used in this phase
		byte[] blockBuffer = new byte[BLOCK_SIZE];
		byte[] compareBuffer = new byte[TUPLE_SIZE];
		byte[] currentTuple = new byte[TUPLE_SIZE];
		byte[] Sums = new byte[SUM_SIZE];
		byte[][] outputbuffer = new byte[BLOCK_SIZE][SUM_TUPLE_SIZE];

		int bufferlength = 0;
		int MAX_SumsLines = BLOCK_SIZE / SUM_TUPLE_SIZE;
		int sumcounts = 0;
		int first = 0;
		int client_number_counts = 0;
	
		//byte[] test = "111226842011-09-28998943288Clevey Adamovitz         Wilson MI 49896 Midwest                                                                                                                               cadamovitzbe@sciencedaily.co07722907.60327860.58".getBytes();


		FileInputStream SumIn = new FileInputStream(sortedFile);
		FileOutputStream SumOut = new FileOutputStream("resource/output/" + "_sums.txt");


		try {

			while ((bufferlength = SumIn.read(blockBuffer))!= -1) {


				io++;
				int clientCounter = 0;


				for (int i = 0; i < bufferlength / TUPLE_SIZE; i++) {


					//first load into compare buffer 
					//only execute once  
					if (first == 0)
					{
						System.arraycopy(blockBuffer, 0, compareBuffer, 0, TUPLE_SIZE);
						System.arraycopy(extractSum(compareBuffer), 0, Sums, 0, SUM_SIZE);
						first ++;
						clientCounter++;
						client_number_counts++;
					}

					//If the first time loading, the remaining client available is TUPLES_PER_BLOCK-1
					//The index start is 1 not 0;
					//read the current counter point tuple prepare to do the compare


					if (first == 1 && TUPLE_SIZE * (clientCounter) < bufferlength) {


						System.arraycopy(blockBuffer, TUPLE_SIZE * (clientCounter) , currentTuple, 0, TUPLE_SIZE);
						clientCounter++;

					}
					//Not first time load TUPLES_PER_BLOCK Num of clients
					else if (first != 1){

						System.arraycopy(blockBuffer, TUPLE_SIZE* (clientCounter) , currentTuple, 0, TUPLE_SIZE);
						clientCounter++;


					}

					// Compare.If the same Client then do the sum
					// Do the sum And put it into sum buffer
					if (compare (compareBuffer, currentTuple) == 0) {


						byte[] s = sumTwoBytes(Sums, extractSum(currentTuple));
						System.arraycopy(s, 0 , Sums , 0 , SUM_SIZE);

					}

					//If not the same Client then create a New Type of tuple with ID and Sum
					//And put it into output buffer
					//reset the sum buffer.
					//Update the compareBffer with current tuple
					//increment 1 sums counter.

					else {

						outputbuffer[sumcounts] = CreateSumTuple(compareBuffer,Sums);		
						Sums = new byte[SUM_SIZE];//reset
						System.arraycopy(currentTuple, 0, compareBuffer, 0, TUPLE_SIZE);	
						System.arraycopy(extractSum(compareBuffer), 0, Sums, 0, SUM_SIZE);
						sumcounts++;
						client_number_counts++;

					}


					// if output buffer is full to write
					if (sumcounts == MAX_SumsLines ) {


						for (int t =0; t < sumcounts; t++) {

							io++;
							SumOut.write(outputbuffer[t]);
						}

						sumcounts = 0;

					}


				

					if (first == 1 && TUPLE_SIZE * (clientCounter) == bufferlength) {
						break;
					}


				}

				// if the num of lines reach whole file  
				// not enough then write
				if (SumIn.available() == 0) {

					outputbuffer[sumcounts] = CreateSumTuple(compareBuffer,Sums);

					for (int t =0; t <= sumcounts; t++) {

						io++;
						SumOut.write(outputbuffer[t]);						

					}

				}

				first++;
			}


			SumIn.close();
			SumOut.close();
			if (DEBUG) {
				System.out.println("");
				System.out.println("************** RECORD SUM DATA ***************");

			}
		}


		catch (IOException e) {
			e.printStackTrace();
		}

		finally {

			blockBuffer = null;
			compareBuffer = null;
			currentTuple = null;
			Sums = null;
			outputbuffer = null;
			System.gc();


		}
		return client_number_counts;
	}

	public static String searchSum(String sumFile, int targetID) throws IOException {

		byte[] blockBuffer = new byte[4000];
		byte[] currentSumTuple = new byte[SUM_TUPLE_SIZE];
		byte[] currentID = new byte[9];
		int bufferlength = 0;
		FileInputStream SumRead = new FileInputStream(sumFile);

		try {
			while((bufferlength =SumRead.read(blockBuffer))!= -1){
				io++;
				for (int i =0; i < bufferlength / SUM_TUPLE_SIZE; i++ ) {


					System.arraycopy(blockBuffer, i * SUM_TUPLE_SIZE, currentID, 0, 9);
					int ei = extractID(currentID);
					System.arraycopy(blockBuffer, i * SUM_TUPLE_SIZE, currentSumTuple, 0, SUM_TUPLE_SIZE);

					if (ei == targetID) {

						SumRead.close();
						return extractTargetSum(currentSumTuple);
					}
					else {continue;}

				}

			}
			SumRead.close();
		}

		catch (IOException e) {
			e.printStackTrace();
		}
		finally {


		}

		return "";

	}

	public static String top10Print(String sumFile) throws IOException {

		byte[] blockBuffer = new byte[4000];// not 4016 for the sake of the sum tuple size
		byte[] currentSumTuple = new byte[SUM_TUPLE_SIZE];
		int bufferlength = 0;
		FileInputStream SumRead = new FileInputStream(sumFile);

		PriorityQueue<byte[]> top10 = new PriorityQueue<>(new Comparator<byte[]>() {
			@Override
			public int compare(byte[] o1, byte[] o2) {

				String o1str = new String(o1, 12, 18);
				String o2str = new String(o2, 12, 18);
				return o2str.compareTo(o1str);

			}
		});

		try {
			while((bufferlength =SumRead.read(blockBuffer))!= -1){
				io++;
				for (int i = 0; i < bufferlength / SUM_TUPLE_SIZE; i++ ) {

					System.arraycopy(blockBuffer, i * SUM_TUPLE_SIZE, currentSumTuple, 0, SUM_TUPLE_SIZE);
					byte[] currentSumTupleTemp = new byte[SUM_TUPLE_SIZE];
					System.arraycopy(currentSumTuple,0, currentSumTupleTemp, 0, SUM_TUPLE_SIZE);
					top10.add(currentSumTupleTemp);
				}


			}
			SumRead.close();

			for(int j=0;j<10;j++) {
				byte[] temp = top10.poll();
				String re = new String(temp);
				System.out.print(re);
			}
		}

		catch (IOException e) {
			e.printStackTrace();
		}
		finally {

		}

		return "";

	}
	//time recorder
	public static void recordTime(long startTime, int divide, String function) {
		long endTime = System.currentTimeMillis();
		long duration = (endTime - startTime);
		System.out.println(function + " time is:" + duration/divide + " milliseconds");
	}

	public static void recordTime(long startTime, String function) {
		recordTime(startTime, 1, function);
	}

	public static int compare(byte[] t1, byte[] t2) {

		for (int i = 18; i < 27; i++) {
			if (t1[i] == t2[i]) {
				continue;
			} else {
				return t1[i] - t2[i];
			}
		}

		return 0;
	}

	public static byte[] extractSum(byte[] s) {

		byte[] sum = new byte[SUM_SIZE];

		System.arraycopy(s, 241, sum, 9, 9);

		// make up zero for the insignificant digits
		for (int i =0; i < 9; i++) {

			System.arraycopy(zero, 0, sum, i , 1);
		}



		return sum;
	}

	public static int extractID(byte[] t) {

		byte[] id = new byte[9];
		System.arraycopy(t, 0, id, 0, 9);


		int i = Integer.parseInt(new String(id));

		return i;

	}

	public static String extractTargetSum(byte[] si) {

		byte[] tsum =new byte[SUM_TUPLE_SIZE];
		System.arraycopy(si, 12, tsum, 0, SUM_SIZE);
		return (new String(tsum));



	}

	public static byte[] sumTwoBytes(byte[] a1, byte[] a2) {



		int len = a1.length;//18

		String[] temp = new String[len];//18
		String s = "";
		int[] roundUp = new int[len];//18
		roundUp[len - 1] = 0; // the first digit round up is always 0.
		int i1 = 0,i2 = 0;
		int sum = 0;
		int round_up_shift = 1;
		// fill up round up array, start from len - 2
		// Always count round up for the next one
		for (int i = len - 1; i >= 0; i--) {

			i1 = Character.getNumericValue((char) (a1[i]));			
			i2 = Character.getNumericValue((char) (a2[i]));

			sum = i1 + i2 + roundUp[i];

			if (i == 0) {
				temp[i] = "0";
				break;
			}

			if (i == 15) { 

				temp[i] = ".";

				continue;
			}
			if (i == 16) {

				round_up_shift = 2;
			}
			else if(i != 16){
				round_up_shift = 1;
			}

			if (sum >= 10) {


				temp[i] = Integer.toString(sum%10);
				roundUp[i - round_up_shift] = 1;

			}
			else  {


				temp[i] = Integer.toString(sum%10);
				roundUp[i - round_up_shift] = 0;

			}
			round_up_shift = 1;
		}

		for (int j = 0; j < len; j++) {	

			s+=temp[j];
		}

		return s.getBytes();



	}

	public static byte[] CreateSumTuple(byte[] ori, byte[] sum) {

		byte[] temp_sum = new byte[SUM_TUPLE_SIZE];

		// copy the cid into new tuple
		System.arraycopy(ori, 18, temp_sum, 0 , 9);

		for (int i = 9; i < 12; i++) {
			temp_sum[i]= 0x20;
		}

		//copy the sum into new tuple start from index 12
		System.arraycopy(sum, 0, temp_sum, 12, SUM_SIZE);

		for (int i = 30; i < 39; i++) {
			temp_sum[i]= 0x20;
		}

		System.arraycopy(ori, 27, temp_sum, 39, 25);

		for (int i = 64; i < 79; i++) {
			temp_sum[i]= 0x20;
		}
		//line separator at index 79.
		temp_sum[79]= 0x0a;

		//System.out.print(new String(temp_sum));
		return temp_sum;

	}
}
