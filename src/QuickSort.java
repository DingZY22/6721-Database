
public class QuickSort {

	
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
	

	public static void sort(byte[][] data, int start, int end) {
		
		if (end - start <=0) { return; }
		
		int splitPoint = partition(data, start, end);
		
		sort(data, start, splitPoint - 1 );
		sort(data, splitPoint + 1, end);
	}
	
	private static int partition(byte[][] data, int first, int last) {

	
		int iLo = first + 1, iHi = last; //lowest, highest untested indices
		int mid = (first + last)/2; // take pivot from here
		byte[] pivot = data[mid];        //    so in-order not worst case

		//Almost the body of main while loop follows, except for non-sentinal search
		//Swap the chosen pivot value into beginning
		data[mid] = data[first];
		data[first] = pivot;  //serves as first sentinel for downward sweep

		while (compare(data[iHi],pivot) > 0) // normal downward scan
			iHi--;
		
		while (iLo <= iHi && compare(data[iLo],pivot) < 0) // no sentinel upward yet
			iLo++;
		
		if (iLo <= iHi) {
			byte[] temp = data[iLo];
			data[iLo] = data[iHi];
			data[iHi] = temp;
			iHi--;
			iLo++;
		}

		while (iLo <= iHi) { // now have sentinels both ways
			
			while (compare(data[iHi],pivot) > 0) 
				iHi--;
			
			while (compare(data[iLo],pivot) < 0) // have sentenel swapped in place now
				iLo++;
			
			if (iLo <= iHi) {
				byte[] temp = data[iLo];
				data[iLo] = data[iHi];
				data[iHi] = temp;
				iHi--;
				iLo++;
			}
		}
		
		
		int iPivot = iLo-1;          // place of last smaller value
		data[first] = data[iPivot];  // swap with pivot
		data[iPivot] = pivot;
		return iPivot;
	} 


}
