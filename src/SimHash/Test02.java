package SimHash;

import java.util.ArrayList;
import java.util.List;

public class Test02 {
	public static void main(String[] args) {
		List<int[]> list=new ArrayList<int[]>();
		int[] i={1,2,3,4,5};
		int[] j={12,23,34,45,56};
		int[] k={11,22,33,44,55};
		list.add(i);
		list.add(j);
		list.add(k);
//		Iterator itr=list.iterator();
//		if(itr.hasNext()){
//			System.out.println(itr.next());
//		}
		for(int[] t:list){
			for(int a=0;a<t.length;a++){
				System.out.print(t[a]+" ");
			}
			System.out.println();
		}
	}
}
