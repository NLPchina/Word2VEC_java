package com.ansj.vec;

public class Test {
    public static void main(String[] args) {
	int[]  arr = {1,32,23,32,1} ;
	int flag = arr[0] ;
	for (int i = 1; i < arr.length; i++) {
	    flag = flag ^ arr[i] ;
	}
	System.out.println(flag);
    }
}
