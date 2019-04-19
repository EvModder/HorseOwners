package net.evmodder.EvLib;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

public class IndexMultiMapDriver{

	
	public static void main(String[] args){
		HashMap<Double, Vector<String>> test = new HashMap<>();
		IndexTreeMultiMap2<Double, Vector<String>, String> yeet = new IndexTreeMultiMap2<>(Vector::new, test);
		yeet.put(0.5, "x00");

		yeet.put(5.1, "l11");
		yeet.put(5.2, "m12");
		yeet.put(5.3, "n13");
		yeet.put(5.4, "o14");//
		yeet.put(5.4, "o14_2");
		yeet.put(5.5, "p15");
		yeet.put(5.6, "q16");

		yeet.put(0.5, "x01");

		yeet.put(2.61, "f5");
		yeet.put(2.62, "g6");
		yeet.put(2.63, "h7");
		yeet.put(2.64, "i8");
		yeet.put(2.65, "j9");
		yeet.put(2.66, "k10");

		yeet.put(0.5, "x02");

		yeet.put(2.45, "a0");
		yeet.put(2.46, "b1");
		yeet.put(2.47, "c2");
		yeet.put(2.48, "d3");
		yeet.put(2.49, "e4");

		yeet.put(0.5, "x03");
		yeet.put(0.1, "huehuehue");

		System.out.println(yeet.getKeyIndex(0.5)+","+yeet.getLowerIndex(0.5)+","+yeet.getUpperIndex(0.5));
		System.out.println(yeet.getKeyIndex(2.45)+","+yeet.getLowerIndex(2.45)+","+yeet.getUpperIndex(2.45));
		System.out.println(yeet.getKeyIndex(5.4)+","+yeet.getLowerIndex(5.4)+","+yeet.getUpperIndex(5.4));
		System.out.println(yeet.getKeyIndex(5.6)+","+yeet.getLowerIndex(5.6)+","+yeet.getUpperIndex(5.6));
		for(Iterator<Double> it = yeet.keyIterator(); it.hasNext();){
			Double key = it.next();
			System.out.println(yeet.get(key));
		}
		for(int i=0; i<yeet.size(); ++i){
			System.out.println(i+": "+yeet.atIndex(i));
		}
		for(int i=0; i<yeet.valuesSize(); ++i){
			System.out.println(i+": "+yeet.atValueIndex(i));
		}
	}
}