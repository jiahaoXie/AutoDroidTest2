package test;

import java.util.ArrayList;
import java.util.List;

public class list {
	public static void main(String[] args) {
		List<Integer> list=new ArrayList<Integer>();
		list.add(1);
		list.add(2);
		list.add(3);
		System.out.println("list size:"+list.size());
		System.out.println(list.get(0));
		System.out.println(list.get(1));
	}
}
