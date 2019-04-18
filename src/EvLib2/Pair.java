package EvLib2;

public class Pair<T extends Comparable<T>, R extends Comparable<R>> implements Comparable<Pair<T, R>>{
	T a; R b;
	public Pair(T t, R r){a=t; b=r;}
	@Override public boolean equals(Object p){
		return p != null && p instanceof Pair && a.equals(((Pair<?, ?>)p).a) && b.equals(((Pair<?, ?>)p).b);
	}
	@Override public int hashCode(){
		return a.hashCode() + b.hashCode();
	}

	@Override
	public int compareTo(Pair<T, R> o){
		int tComp = a.compareTo(o.a);
		return tComp != 0 ? tComp : b.compareTo(o.b);
	}
}