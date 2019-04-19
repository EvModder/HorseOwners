package net.evmodder.EvLib;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A Red-Black tree based {@link NavigableMap} implementation.
 * The map is sorted according to the {@linkplain Comparable natural
 * ordering} of its keys, or by a {@link Comparator} provided at map
 * creation time, depending on which constructor is used.
 *
 * <p>
 * This implementation provides guaranteed log(n) time cost for the
 * {@code containsKey}, {@code get}, {@code put} and {@code remove}
 * operations. Algorithms are adaptations of those in Cormen, Leiserson, and
 * Rivest's <em>Introduction to Algorithms</em>.
 *
 * <p>
 * Note that the ordering maintained by a tree map, like any sorted map, and
 * whether or not an explicit comparator is provided, must be <em>consistent
 * with {@code equals}</em> if this sorted map is to correctly implement the
 * {@code Map} interface. (See {@code Comparable} or {@code Comparator} for a
 * precise definition of <em>consistent with equals</em>.) This is so because
 * the {@code Map} interface is defined in terms of the {@code equals}
 * operation, but a sorted map performs all key comparisons using its {@code
 * compareTo} (or {@code compare}) method, so two keys that are deemed equal by
 * this method are, from the standpoint of the sorted map, equal. The behavior
 * of a sorted map <em>is</em> well-defined even if its ordering is
 * inconsistent with {@code equals}; it just fails to obey the general contract
 * of the {@code Map} interface.
 *
 * <p>
 * <strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access a map concurrently, and at least one of the
 * threads modifies the map structurally, it <em>must</em> be synchronized
 * externally. (A structural modification is any operation that adds or
 * deletes one or more mappings; merely changing the value associated
 * with an existing key is not a structural modification.) This is
 * typically accomplished by synchronizing on some object that naturally
 * encapsulates the map.
 * If no such object exists, the map should be "wrapped" using the
 * {@link Collections#synchronizedSortedMap Collections.synchronizedSortedMap}
 * method. This is best done at creation time, to prevent accidental
 * unsynchronized access to the map:
 * 
 * <pre>
 *   SortedMap m = Collections.synchronizedSortedMap(new IndexTreeMap(...));
 * </pre>
 *
 * <p>
 * The iterators returned by the {@code iterator} method of the collections
 * returned by all of this class's "collection view methods" are
 * <em>fail-fast</em>: if the map is structurally modified at any time after
 * the iterator is created, in any way except through the iterator's own
 * {@code remove} method, the iterator will throw a {@link
 * ConcurrentModificationException}. Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the future.
 *
 * <p>
 * Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification. Fail-fast iterators
 * throw {@code ConcurrentModificationException} on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness: <em>the fail-fast behavior of iterators
 * should be used only to detect bugs.</em>
 *
 * <p>
 * All {@code Map.Entry} pairs returned by methods in this class
 * and its views represent snapshots of mappings at the time they were
 * produced. They do <strong>not</strong> support the {@code Entry.setValue}
 * method. (Note however that it is possible to change mappings in the
 * associated map using {@code put}.)
 *
 * <p>
 * This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
 * @author Josh Bloch and Doug Lea
 * @see Map
 * @see HashMap
 * @see Hashtable
 * @see Comparable
 * @see Comparator
 * @see Collection
 * @since 1.2
 */

public class IndexTreeMultiMap2<K, C extends Cloneable & Collection<X>, X> extends AbstractMap<K, C>
implements NavigableMap<K, C>, Cloneable, java.io.Serializable
{
	/**
	 * Trashy hideous hot mess until I figure out something more proper
	 */
	@SuppressWarnings("unchecked")
	private static <T> T reflClone(T t){
		if(t == null) return null;
		try{return (T)t.getClass().getMethod("clone").invoke(t);}
		catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException
			| NoSuchMethodException | SecurityException e){e.printStackTrace();}
		return null;
	}

	/**
	 * More madness
	 */
	private Supplier<C> supplier;

	/**
	 * The comparator used to maintain order in this tree map, or
	 * null if it uses the natural ordering of its keys.
	 *
	 * @serial
	 */
	private final Comparator<? super K> comparator;

	private transient Entry<K, C, X> root;

	/**
	 * The number of entries in the tree
	 */
	private transient int size = 0;

	/**
	 * The number of structural modifications to the tree.
	 */
	private transient int modCount = 0;

	/**
	 * Constructs a new, empty tree map, using the natural ordering of its
	 * keys. All keys inserted into the map must implement the {@link
	 * Comparable} interface. Furthermore, all such keys must be
	 * <em>mutually comparable</em>: {@code k1.compareTo(k2)} must not throw
	 * a {@code ClassCastException} for any keys {@code k1} and
	 * {@code k2} in the map. If the user attempts to put a key into the
	 * map that violates this constraint (for example, the user attempts to
	 * put a string key into a map whose keys are integers), the
	 * {@code put(Object key, Object value)} call will throw a
	 * {@code ClassCastException}.
	 */
	public IndexTreeMultiMap2(Supplier<C> supplier){
		comparator = null;
		this.supplier = supplier;
	}

	/**
	 * Constructs a new, empty tree map, ordered according to the given
	 * comparator. All keys inserted into the map must be <em>mutually
	 * comparable</em> by the given comparator: {@code comparator.compare(k1,
	 * k2)} must not throw a {@code ClassCastException} for any keys
	 * {@code k1} and {@code k2} in the map. If the user attempts to put
	 * a key into the map that violates this constraint, the {@code put(Object
	 * key, Object value)} call will throw a
	 * {@code ClassCastException}.
	 *
	 * @param comparator
	 *            the comparator that will be used to order this map.
	 *            If {@code null}, the {@linkplain Comparable natural
	 *            ordering} of the keys will be used.
	 */
	public IndexTreeMultiMap2(Supplier<C> supplier, Comparator<? super K> comparator){
		this.comparator = comparator;
		this.supplier = supplier;
	}

	/**
	 * Constructs a new tree map containing the same mappings as the given
	 * map, ordered according to the <em>natural ordering</em> of its keys.
	 * All keys inserted into the new map must implement the {@link
	 * Comparable} interface. Furthermore, all such keys must be
	 * <em>mutually comparable</em>: {@code k1.compareTo(k2)} must not throw
	 * a {@code ClassCastException} for any keys {@code k1} and
	 * {@code k2} in the map. This method runs in n*log(n) time.
	 *
	 * @param m
	 *            the map whose mappings are to be placed in this map
	 * @throws ClassCastException
	 *             if the keys in m are not {@link Comparable},
	 *             or are not mutually comparable
	 * @throws NullPointerException
	 *             if the specified map is null
	 */
	public IndexTreeMultiMap2(Supplier<C> supplier, Map<? extends K, ? extends C> m){
		comparator = null;
		this.supplier = supplier;
		putAll(m);
	}

	/**
	 * Constructs a new tree map containing the same mappings and
	 * using the same ordering as the specified sorted map. This
	 * method runs in linear time.
	 *
	 * @param m
	 *            the sorted map whose mappings are to be placed in this map,
	 *            and whose comparator is to be used to sort this map
	 * @throws NullPointerException
	 *             if the specified map is null
	 */
	public IndexTreeMultiMap2(Supplier<C> supplier, SortedMap<K, ? extends C> m){
		comparator = m.comparator();
		this.supplier = supplier;
		try{
			buildFromSorted(m.size(), m.entrySet().iterator(), null, null);
		}
		catch(java.io.IOException cannotHappen){}
		catch(ClassNotFoundException cannotHappen){}
	}

	// Query Operations
	/**
	 * Returns the number of key-value mappings in this map.
	 *
	 * @return the number of key-value mappings in this map
	 */
	public int size(){
		return size;
	}

	/**
	 * Returns the number of mapped values in this map.
	 *
	 * @return the number of mapped values in this map
	 */
	public int valuesSize(){
		return root == null ? 0 : root.size;
	}

	/**
	 * Returns the number of values in the map associated with the given key
	 *
	 * @return the number of values in the map associated with the given key
	 */
	public int valuesSize(Object key){
		Entry<K, C, X> e = getEntry(key);
		return e == null ? 0 : e.value.size();
	}

	/**
	 * Returns {@code true} if this map contains a mapping for the specified key.
	 *
	 * @param key
	 *            key whose presence in this map is to be tested
	 * @return {@code true} if this map contains a mapping for the specified key
	 * @throws ClassCastException
	 *             if the specified key cannot be compared
	 *             with the keys currently in the map
	 * @throws NullPointerException
	 *             if the specified key is null
	 *             and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 */
	public boolean containsKey(Object key){
		return getEntry(key) != null;
	}

	/**
	 * Returns {@code true} if this map maps one or more keys to the
	 * specified value. More formally, returns {@code true} if and only if
	 * this map contains at least one mapping to a value {@code v} such
	 * that {@code (value==null ? v==null : value.equals(v))}. This
	 * operation will probably require time linear in the map size for
	 * most implementations.
	 *
	 * @param value
	 *            value whose presence in this map is to be tested
	 * @return {@code true} if a mapping to {@code value} exists;
	 *         {@code false} otherwise
	 * @since 1.2
	 */
	public boolean containsValue(Object value){
		for(Entry<K, C, X> e = getFirstEntry(); e != null; e = successor(e))
			if(valEquals(value, e.value)) return true;
		for(Entry<K, C, X> e = getFirstEntry(); e != null; e = successor(e))
			if(e.value != null && e.value.contains(value)) return true;
		return false;
	}

	/**
	 * Returns the value to which the specified key is mapped,
	 * or {@code null} if this map contains no mapping for the key.
	 *
	 * <p>
	 * More formally, if this map contains a mapping from a key
	 * {@code k} to a value {@code v} such that {@code key} compares
	 * equal to {@code k} according to the map's ordering, then this
	 * method returns {@code v}; otherwise it returns {@code null}.
	 * (There can be at most one such mapping.)
	 *
	 * <p>
	 * A return value of {@code null} does not <em>necessarily</em>
	 * indicate that the map contains no mapping for the key; it's also
	 * possible that the map explicitly maps the key to {@code null}.
	 * The {@link #containsKey containsKey} operation may be used to
	 * distinguish these two cases.
	 *
	 * @throws ClassCastException
	 *             if the specified key cannot be compared
	 *             with the keys currently in the map
	 * @throws NullPointerException
	 *             if the specified key is null
	 *             and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 */
	public C get(Object key){
		Entry<K, C, X> p = getEntry(key);
		return (p == null ? null : p.value);
	}

	/**
	 * Returns the value(s) at the specified index
	 *
	 * <p>
	 * More formally, returns the {@code values} for the key
	 * {@code k} to some elements {@code values} such that the given
	 * {@code index} falls somewhere within the {@code values}.
	 * (There can be at most one such mapping.)
	 *
	 * @param index index of the value(s) to return
	 * @return value(s) at the specified index
	 * @throws ArrayIndexOutOfBoundsException if the index is out of range
	 *         ({@code index < 0 || index >= valuesSize()})
	 */
	public C atValueIndex(int index){
		if(root == null){
			throw new ArrayIndexOutOfBoundsException(index + " >= " + size);//size==0
		}
		if(index >= root.size){
			throw new ArrayIndexOutOfBoundsException(index + " >= " + root.size);
		}
		return getEntryAtValueIndex(index).value;
	}

	/**
	 * Returns the values at the specified key index
	 *
	 * <p>
	 * More formally, returns the {@code values} for the key
	 * in the map at {@code index}, the {@code i}th key-value mapping
	 * (There can be at most one such mapping.)
	 *
	 * @param index index of the target key-mapping
	 * @return value(s) mapped to by the key at the specified index
	 * @throws ArrayIndexOutOfBoundsException if the index is out of range
	 *         ({@code index < 0 || index >= size()})
	 */
	public C atIndex(int index){
		if(index >= size){
			throw new ArrayIndexOutOfBoundsException(index + " >= " + size);
		}
		Entry<K, C, X> p = getEntryAtIndex(index);
		return (p == null ? null : p.value);
	}

	public Comparator<? super K> comparator(){
		return comparator;
	}

	/**
	 * @throws NoSuchElementException
	 *             {@inheritDoc}
	 */
	public K firstKey(){
		return key(getFirstEntry());
	}

	/**
	 * @throws NoSuchElementException
	 *             {@inheritDoc}
	 */
	public K lastKey(){
		return key(getLastEntry());
	}

	/**
	 * Copies all of the mappings from the specified map to this map.
	 * These mappings replace any mappings that this map had for any
	 * of the keys currently in the specified map.
	 *
	 * @param map
	 *            mappings to be stored in this map
	 * @throws ClassCastException
	 *             if the class of a key or value in
	 *             the specified map prevents it from being stored in this map
	 * @throws NullPointerException
	 *             if the specified map is null or
	 *             the specified map contains a null key and this map does not
	 *             permit null keys
	 */
	public void putAll(Map<? extends K, ? extends C> map){
		int mapSize = map.size();
		if(size == 0 && mapSize != 0 && map instanceof SortedMap){
			Comparator<?> c = ((SortedMap<?, ?>)map).comparator();
			if(c == comparator || (c != null && c.equals(comparator))){
				++modCount;
				try{
					buildFromSorted(mapSize, map.entrySet().iterator(), null, null);
				}
				catch(java.io.IOException cannotHappen){}
				catch(ClassNotFoundException cannotHappen){}
				return;
			}
		}
		super.putAll(map);
	}

	/**
	 * Returns this map's entry for the given key, or {@code null} if the map
	 * does not contain an entry for the key.
	 *
	 * @return this map's entry for the given key, or {@code null} if the map
	 *         does not contain an entry for the key
	 * @throws ClassCastException
	 *             if the specified key cannot be compared
	 *             with the keys currently in the map
	 * @throws NullPointerException
	 *             if the specified key is null
	 *             and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 */
	final Entry<K, C, X> getEntry(Object key){
		// Offload comparator-based version for sake of performance
		if(comparator != null) return getEntryUsingComparator(key);
		if(key == null) throw new NullPointerException();
		@SuppressWarnings("unchecked")
		Comparable<? super K> k = (Comparable<? super K>)key;
		Entry<K, C, X> p = root;
		while(p != null){
			int cmp = k.compareTo(p.key);
			if(cmp < 0) p = p.left;
			else if(cmp > 0) p = p.right;
			else return p;
		}
		return null;
	}

	/**
	 * Version of getEntry using comparator. Split off from getEntry
	 * for performance. (This is not worth doing for most methods,
	 * that are less dependent on comparator performance, but is
	 * worthwhile here.)
	 */
	final Entry<K, C, X> getEntryUsingComparator(Object key){
		@SuppressWarnings("unchecked")
		K k = (K)key;
		Comparator<? super K> cpr = comparator;
		if(cpr != null){
			Entry<K, C, X> p = root;
			while(p != null){
				int cmp = cpr.compare(k, p.key);
				if(cmp < 0) p = p.left;
				else if(cmp > 0) p = p.right;
				else return p;
			}
		}
		return null;
	}

	/**
	 * Returns this map's entry for the given index
	 *
	 * @param index index [lower, upper) of value at entry to return
	 * @return this map's entry containing the value for the given index, or
	 *         {@code null} if the index falls outside of the range of the map
	 */
	final Entry<K, C, X> getEntryAtValueIndex(int index){
		Entry<K, C, X> p = root;
		while(p != null){
			int leftSz = p.left != null ? p.left.size : 0;
			if(leftSz > index) p = p.left;
			else if(leftSz + p.value.size() <= index){
				index -= leftSz;
				index -= p.value.size();
				p = p.right;
			}
			else return p;
		}
		return null;
	}

	/**
	 * Returns this map's entry for the key at the given index
	 *
	 * @param index index of the key-mapping entry to return
	 * @return this map's entry at the key corresponding to the given index, or
	 *         {@code null} if the index falls outside of the range of the map
	 */
	final Entry<K, C, X> getEntryAtIndex(int index){
		Entry<K, C, X> p = root;
		while(p != null){
			int leftSz = (p.left != null ? p.left.treeSz : 0);
			if(leftSz > index) p = p.left;
			else if(leftSz < index){
				index -= leftSz;
				--index;
				p = p.right;
			}
			else return p;
		}
		return null;
	}

	/**
	 * Gets the entry corresponding to the specified key; if no such entry
	 * exists, returns the entry for the least key greater than the specified
	 * key; if no such entry exists (i.e., the greatest key in the Tree is less
	 * than the specified key), returns {@code null}.
	 */
	final Entry<K, C, X> getCeilingEntry(K key){
		Entry<K, C, X> p = root;
		while(p != null){
			int cmp = compare(key, p.key);
			if(cmp < 0){
				if(p.left != null) p = p.left;
				else return p;
			}
			else if(cmp > 0){
				if(p.right != null){
					p = p.right;
				}
				else{
					Entry<K, C, X> parent = p.parent;
					Entry<K, C, X> ch = p;
					while(parent != null && ch == parent.right){
						ch = parent;
						parent = parent.parent;
					}
					return parent;
				}
			}
			else return p;
		}
		return null;
	}

	/**
	 * Gets the entry corresponding to the specified key; if no such entry
	 * exists, returns the entry for the greatest key less than the specified
	 * key; if no such entry exists, returns {@code null}.
	 */
	final Entry<K, C, X> getFloorEntry(K key){
		Entry<K, C, X> p = root;
		while(p != null){
			int cmp = compare(key, p.key);
			if(cmp > 0){
				if(p.right != null) p = p.right;
				else return p;
			}
			else if(cmp < 0){
				if(p.left != null){
					p = p.left;
				}
				else{
					Entry<K, C, X> parent = p.parent;
					Entry<K, C, X> ch = p;
					while(parent != null && ch == parent.left){
						ch = parent;
						parent = parent.parent;
					}
					return parent;
				}
			}
			else return p;

		}
		return null;
	}

	/**
	 * Gets the entry for the least key greater than the specified
	 * key; if no such entry exists, returns the entry for the least
	 * key greater than the specified key; if no such entry exists
	 * returns {@code null}.
	 */
	final Entry<K, C, X> getHigherEntry(K key){
		Entry<K, C, X> p = root;
		while(p != null){
			int cmp = compare(key, p.key);
			if(cmp < 0){
				if(p.left != null) p = p.left;
				else return p;
			}
			else{
				if(p.right != null){
					p = p.right;
				}
				else{
					Entry<K, C, X> parent = p.parent;
					Entry<K, C, X> ch = p;
					while(parent != null && ch == parent.right){
						ch = parent;
						parent = parent.parent;
					}
					return parent;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the entry for the greatest key less than the specified key; if
	 * no such entry exists (i.e., the least key in the Tree is greater than
	 * the specified key), returns {@code null}.
	 */
	final Entry<K, C, X> getLowerEntry(K key){
		Entry<K, C, X> p = root;
		while(p != null){
			int cmp = compare(key, p.key);
			if(cmp > 0){
				if(p.right != null) p = p.right;
				else return p;
			}
			else{
				if(p.left != null){
					p = p.left;
				}
				else{
					Entry<K, C, X> parent = p.parent;
					Entry<K, C, X> ch = p;
					while(parent != null && ch == parent.left){
						ch = parent;
						parent = parent.parent;
					}
					return parent;
				}
			}
		}
		return null;
	}

	/**
	 * Returns this map's entry index for the given key, or {@code -1} if the map
	 * does not contain an entry for the key.
	 *
	 * @return this map's entry index for the given key, or {@code -1} if the map
	 *         does not contain an entry for the key
	 * @throws ClassCastException
	 *             if the specified key cannot be compared
	 *             with the keys currently in the map
	 * @throws NullPointerException
	 *             if the specified key is null
	 *             and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 */
	public final int getKeyIndex(Object key){
		// Offload comparator-based version for sake of performance
		if(comparator != null) return getLowerIndexUsingComparator(key);
		if(key == null) throw new NullPointerException();
		@SuppressWarnings("unchecked")
		Comparable<? super K> k = (Comparable<? super K>)key;
		Entry<K, C, X> p = root;
		int index = 0;
		while(p != null){
			int cmp = k.compareTo(p.key);
			if(cmp < 0) p = p.left;
			else if(cmp > 0){
				if(p.left != null) index += p.left.treeSz;
				++index;
				p = p.right;
			}
			else{
				if(p.left != null) index += p.left.treeSz;
				return index;
			}
		}
		return -1;
	}

	/**
	 * Version of getKeyIndex using comparator. Split off from getKeyIndex
	 * for performance. (This is not worth doing for most methods,
	 * that are less dependent on comparator performance, but is
	 * worthwhile here.)
	 */
	public final int getKeyIndexUsingComparator(Object key){
		@SuppressWarnings("unchecked")
		K k = (K)key;
		Comparator<? super K> cpr = comparator;
		if(cpr != null){
			Entry<K, C, X> p = root;
			int index = 0;
			while(p != null){
				int cmp = cpr.compare(k, p.key);
				if(cmp < 0) p = p.left;
				else if(cmp > 0){
					if(p.left != null) index += p.left.treeSz;
					++index;
					p = p.right;
				}
				else{
					if(p.left != null) index += p.left.treeSz;
					return index;
				}
			}
		}
		return -1;
	}

	/**
	 * Returns this map's entry index for the given key, or {@code -1} if the map
	 * does not contain an entry for the key.
	 *
	 * @return this map's entry index for the given key, or {@code -1} if the map
	 *         does not contain an entry for the key
	 * @throws ClassCastException
	 *             if the specified key cannot be compared
	 *             with the keys currently in the map
	 * @throws NullPointerException
	 *             if the specified key is null
	 *             and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 */
	public final int getLowerIndex(Object key){
		// Offload comparator-based version for sake of performance
		if(comparator != null) return getLowerIndexUsingComparator(key);
		if(key == null) throw new NullPointerException();
		@SuppressWarnings("unchecked")
		Comparable<? super K> k = (Comparable<? super K>)key;
		Entry<K, C, X> p = root;
		int index = 0;
		while(p != null){
			int cmp = k.compareTo(p.key);
			if(cmp < 0) p = p.left;
			else if(cmp > 0){
				if(p.left != null) index += p.left.size;
				index += p.value.size();
				p = p.right;
			}
			else{
				if(p.left != null) index += p.left.size;
				return index;
			}
		}
		return -1;
	}

	/**
	 * Version of getLowerIndex using comparator. Split off from getLowerIndex
	 * for performance. (This is not worth doing for most methods,
	 * that are less dependent on comparator performance, but is
	 * worthwhile here.)
	 */
	public final int getLowerIndexUsingComparator(Object key){
		@SuppressWarnings("unchecked")
		K k = (K)key;
		Comparator<? super K> cpr = comparator;
		if(cpr != null){
			Entry<K, C, X> p = root;
			int index = 0;
			while(p != null){
				int cmp = cpr.compare(k, p.key);
				if(cmp < 0) p = p.left;
				else if(cmp > 0){
					if(p.left != null) index += p.left.size;
					index += p.value.size();
					p = p.right;
				}
				else{
					if(p.left != null) index += p.left.size;
					return index;
				}
			}
		}
		return -1;
	}

	/**
	 * Returns this map's entry index for the given key, or {@code size} if the map
	 * does not contain an entry for the key.
	 *
	 * @return this map's entry index for the given key, or {@code size} if the map
	 *         does not contain an entry for the key
	 * @throws ClassCastException
	 *             if the specified key cannot be compared
	 *             with the keys currently in the map
	 * @throws NullPointerException
	 *             if the specified key is null
	 *             and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 */
	public final int getUpperIndex(Object key){
		// Offload comparator-based version for sake of performance
		if(comparator != null) return getUpperIndexUsingComparator(key);
		if(key == null) throw new NullPointerException();
		@SuppressWarnings("unchecked")
		Comparable<? super K> k = (Comparable<? super K>)key;
		Entry<K, C, X> p = root;
		int index = 0;
		while(p != null){
			int cmp = k.compareTo(p.key);
			if(cmp < 0) p = p.left;
			else if(cmp > 0){
				if(p.left != null) index += p.left.size;
				index += p.value.size();
				p = p.right;
			}
			else{
				if(p.left != null) index += p.left.size;
				return index + p.value.size();
			}
		}
		return root != null ? root.size : 0;
	}

	/**
	 * Version of getUpperIndex using comparator. Split off from getUpperIndex
	 * for performance. (This is not worth doing for most methods,
	 * that are less dependent on comparator performance, but is
	 * worthwhile here.)
	 */
	public final int getUpperIndexUsingComparator(Object key){
		@SuppressWarnings("unchecked")
		K k = (K)key;
		Comparator<? super K> cpr = comparator;
		if(cpr != null){
			Entry<K, C, X> p = root;
			int index = 0;
			while(p != null){
				int cmp = cpr.compare(k, p.key);
				if(cmp < 0) p = p.left;
				else if(cmp > 0){
					if(p.left != null) index += p.left.size;
					index += p.value.size();
					p = p.right;
				}
				else{
					if(p.left != null) index += p.left.size;
					return index + p.value.size();
				}
			}
		}
		return root != null ? root.size : 0;
	}

	/**
	 * Associates the specified values with the specified key in this map.
	 * If the map previously contained a mapping for the key, the new values are
	 * added to the old values
	 *
	 * @param key
	 *            key with which the specified value is to be associated
	 * @param values
	 *            values to be associated with the specified key
	 *
	 * @return the previous values associated with {@code key}, or
	 *         {@code null} if there was no mapping for {@code key}.
	 *         (A {@code null} return can also indicate that the map
	 *         previously associated {@code null} with {@code key}.)
	 * @throws ClassCastException
	 *             if the specified key cannot be compared
	 *             with the keys currently in the map
	 * @throws NullPointerException
	 *             if the specified key is null
	 *             and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 */
	public C put(K key, C values){
		Entry<K, C, X> t = root;
		if(t == null){
			compare(key, key); // type (and possibly null) check

			root = new Entry<>(key, values, null);
			root.size = values.size();
			size = 1;
			++modCount;
			return null;
		}
		int cmp;
		Entry<K, C, X> parent;
		// split comparator and comparable paths
		Comparator<? super K> cpr = comparator;
		if(cpr != null){
			do{
				parent = t;
				cmp = cpr.compare(key, t.key);
				if(cmp < 0) t = t.left;
				else if(cmp > 0) t = t.right;
				else{
					int numNew = t.value.size();
					C oldValue = reflClone(t.value);
					if(t.addValues(values)){
						numNew = t.value.size() - numNew;
						while(t != null){
							t.size += numNew;
							t = t.parent;
						}
					}
					return oldValue;
				}
			}
			while(t != null);
		}
		else{
			if(key == null) throw new NullPointerException();
			@SuppressWarnings("unchecked")
			Comparable<? super K> k = (Comparable<? super K>)key;
			do{
				parent = t;
				cmp = k.compareTo(t.key);
				if(cmp < 0) t = t.left;
				else if(cmp > 0) t = t.right;
				else{
					int numNew = t.value.size();
					C oldValue = reflClone(t.value);
					if(t.addValues(values)){
						numNew = t.value.size() - numNew;
						while(t != null){
							t.size += numNew;
							t = t.parent;
						}
					}
					return oldValue;
				}
			}
			while(t != null);
		}
		t = parent;
		while(t != null){
			++t.treeSz;
			t.size += values.size();
			t = t.parent;
		}
		Entry<K, C, X> e = new Entry<>(key, values, parent);
		if(cmp < 0) parent.left = e;
		else parent.right = e;
		fixAfterInsertion(e);
		++size;
		++modCount;
		return null;
	}

	/**
	 * Associates the specified value with the specified key in this map.
	 * If the map previously contained a mapping for the key, the new value is
	 * added to the old values
	 *
	 * @param key
	 *            key with which the specified value is to be associated
	 * @param value
	 *            value to be associated with the specified key
	 *
	 * @return the previous values associated with {@code key}, or
	 *         {@code null} if there was no mapping for {@code key}.
	 *         (A {@code null} return can also indicate that the map
	 *         previously associated {@code null} with {@code key}.)
	 *
	 * @throws IllegalAccessException
	 *             if the class or its nullary constructor is not accessible.
	 * @throws InstantiationException
	 *             if this Class represents an abstract class, an interface, an array class,
	 *             a primitive type, or void; or if the class has no nullary constructor;
	 *             or if the instantiation fails for some other reason.
	 * @throws ClassCastException
	 *             if the specified key cannot be compared
	 *             with the keys currently in the map
	 * @throws NullPointerException
	 *             if the specified key is null
	 *             and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 */
	public C put(K key, X value){
		Entry<K, C, X> t = root;
		if(t == null){
			compare(key, key); // type (and possibly null) check
			C values = supplier.get();
			values.add(value);
			root = new Entry<>(key, values, null);
			root.size = 1;
			size = 1;
			++modCount;
			return null;
		}
		int cmp;
		Entry<K, C, X> parent;
		// split comparator and comparable paths
		Comparator<? super K> cpr = comparator;
		if(cpr != null) {
			do{
				parent = t;
				cmp = cpr.compare(key, t.key);
				if(cmp < 0) t = t.left;
				else if(cmp > 0) t = t.right;
				else{
					C oldValue = reflClone(t.value);
					if(t.addValue(value)){
						while(t != null){
							++t.size;
							t = t.parent;
						}
					}
					return oldValue;
				}
			}
			while(t != null);
		}
		else{
			if(key == null) throw new NullPointerException();
			@SuppressWarnings("unchecked")
			Comparable<? super K> k = (Comparable<? super K>)key;
			do{
				parent = t;
				cmp = k.compareTo(t.key);
				if(cmp < 0) t = t.left;
				else if(cmp > 0) t = t.right;
				else{
					C oldValue = reflClone(t.value);
					if(t.addValue(value)){
						while(t != null){
							++t.size;
							t = t.parent;
						}
					}
					return oldValue;
				}
			}
			while(t != null);
		}
		t = parent;
		while(t != null){
			++t.treeSz;
			++t.size;
			t = t.parent;
		}
		C values = supplier.get();
		values.add(value);
		Entry<K, C, X> e = new Entry<>(key, values, parent);
		if(cmp < 0) parent.left = e;
		else parent.right = e;
		fixAfterInsertion(e);
		++size;
		++modCount;
		return null;
	}

	/**
	 * Updates the mapping at the given value index with the new values
	 * The new values are added to the old values at the specified index
	 *
	 * @param index
	 *            index [lower, upper) of the values-mapping which will be updated
	 * @param values
	 *            new values to be added with the existing values at the specified index
	 *
	 * @return the previous values from the mapping at the given value index
	 * @throws ArrayIndexOutOfBoundsException if the index is out of range
	 *         ({@code index < 0 || index >= valuesSize()})
	 */
	public C putAtValueIndex(int index, C values){
		if(root == null){
			throw new ArrayIndexOutOfBoundsException(index + " >= 0");
		}
		if(index >= root.size){
			throw new ArrayIndexOutOfBoundsException(index + " >= " + root.size);
		}
		Entry<K, C, X> p = getEntryAtValueIndex(index);
		C oldValue = p.value;
		int numNew = p.value.size();
		if(p.addValues(values)){
			numNew = p.value.size() - numNew;
			while(p != null){
				p.size += numNew;
				p = p.parent;
			}
		}
		return oldValue;
	}

	/**
	 * Updates the mapping at the given value index with the new value
	 * The new value is added to the existing values at the specified index
	 *
	 * @param index
	 *            index [lower, upper) of the values-mapping which will be updated
	 * @param value
	 *            new value to be added with the values at the specified index
	 *
	 * @return the previous values from the mapping at the given value index
	 * @throws ArrayIndexOutOfBoundsException if the index is out of range
	 *         ({@code index < 0 || index >= valuesSize()})
	 */
	public C putAtValueIndex(int index, X value){
		if(root == null){
			throw new ArrayIndexOutOfBoundsException(index + " >= 0");
		}
		if(index >= root.size){
			throw new ArrayIndexOutOfBoundsException(index + " >= " + root.size);
		}
		Entry<K, C, X> p = getEntryAtValueIndex(index);
		C oldValue = p.value;
		if(p.addValue(value)){
			while(p != null){
				++p.size;
				p = p.parent;
			}
		}
		return oldValue;
	}

	/**
	 * Updates an existing key-value mapping at the given index with new values
	 * The new values are added to the old values at the specified key index
	 *
	 * @param index
	 *            index of the key for the mapping which will be updated
	 * @param values
	 *            values to be associated with the key at the specified index
	 *
	 * @return the previous values associated with {@code key}
	 * @throws ArrayIndexOutOfBoundsException if the index is out of range
	 *         ({@code index < 0 || index >= size()})
	 */
	public C putAtIndex(int index, C values){
		if(index >= size){
			throw new ArrayIndexOutOfBoundsException(index + " >= " + size);
		}
		Entry<K, C, X> p = getEntryAtIndex(index);
		C oldValue = p.value;
		int numNew = p.value.size();
		if(p.addValues(values)){
			numNew = p.value.size() - numNew;
			while(p != null){
				p.size += numNew;
				p = p.parent;
			}
		}
		return oldValue;
	}

	/**
	 * Updates a key-value mapping at the given index with the new value
	 * The new value is added to the existing values at the specified key index
	 *
	 * @param index
	 *            index of the key for the mapping which will be updated
	 * @param value
	 *            value to be associated with the key at the specified index
	 *
	 * @return the previous values associated with {@code key}
	 * @throws ArrayIndexOutOfBoundsException if the index is out of range
	 *         ({@code index < 0 || index >= size()})
	 */
	public C putAtIndex(int index, X value){
		if(index >= size){
			throw new ArrayIndexOutOfBoundsException(index + " >= " + size);
		}
		Entry<K, C, X> p = getEntryAtIndex(index);
		C oldValue = p.value;
		if(p.addValue(value)){
			while(p != null){
				++p.size;
				p = p.parent;
			}
		}
		return oldValue;
	}

	/**
	 * Removes the mapping for this key from this IndexTreeMap if present.
	 *
	 * @param key
	 *            key for which mapping should be removed
	 * @return the previous value associated with {@code key}, or
	 *         {@code null} if there was no mapping for {@code key}.
	 *         (A {@code null} return can also indicate that the map
	 *         previously associated {@code null} with {@code key}.)
	 * @throws ClassCastException
	 *             if the specified key cannot be compared
	 *             with the keys currently in the map
	 * @throws NullPointerException
	 *             if the specified key is null
	 *             and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 */
	public C remove(Object key){
		Entry<K, C, X> p = getEntry(key);
		if(p == null) return null;

		C oldValue = p.value;
		deleteEntry(p);
		return oldValue;
	}

	/**
	 * Removes the given value from the mapping for this key
	 * from this IndexTreeMap if both the value and the key are present.
	 *
	 * @param key
	 *            key for which mapping should be removed
	 * @param value
	 *            value which should be removed from the mapping
	 * @return true if the mapping was found and the value was removed
	 * @throws ClassCastException
	 *             if the specified key cannot be compared
	 *             with the keys currently in the map
	 * @throws NullPointerException
	 *             if the specified key is null
	 *             and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 */
	public boolean remove(Object key, Object value){
		Entry<K, C, X> p = getEntry(key);
		if(p == null) return false;

		if(p.removeValue(value)){
			Entry<K, C, X> parent = p.parent;
			while(parent != null){
				--parent.size;
				parent = parent.parent;
			}
			if(p.value.isEmpty()) deleteEntry(p);
			return true;
		}
		return false;
	}

	/**
	 * Removes the given values from the mapping for this key
	 * from this IndexTreeMap if the key mapping can be found
	 *
	 * @param key
	 *            key for which mapping should be removed
	 * @param values
	 *            values which should be removed from the mapping for the key
	 * @return true if the mapping was found one or more values were removed
	 * @throws ClassCastException
	 *             if the specified key cannot be compared
	 *             with the keys currently in the map
	 * @throws NullPointerException
	 *             if the specified key is null
	 *             and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 */
	public boolean remove(Object key, C values){
		Entry<K, C, X> p = getEntry(key);
		if(p == null) return false;

		int oldSize = p.value.size();
		if(p.removeValues(values)){
			int numRemoved = oldSize - p.value.size();
			Entry<K, C, X> parent = p.parent;
			while(parent != null){
				parent.size -= numRemoved;
				parent = parent.parent;
			}
			if(p.value.isEmpty()) deleteEntry(p);
			return true;
		}
		return false;
	}

	/**
	 * Removes the mapping for the value(s) at the given index
	 *
	 * @param index index [lower, upper) used to find the mapped values to remove
	 * @return the removed values
	 * @throws ArrayIndexOutOfBoundsException if the index is out of range
	 *         ({@code index < 0 || index >= valuesSize()})
	 */
	public C removeAtValueIndex(int index){
		if(root == null){
			throw new ArrayIndexOutOfBoundsException(index + " >= 0");
		}
		if(index >= root.size){
			throw new ArrayIndexOutOfBoundsException(index + " >= " + root.size);
		}
		Entry<K, C, X> p = getEntryAtValueIndex(index);
		C oldValue = p.value;
		deleteEntry(p);
		return oldValue;
	}

	/**
	 * Removes the mapping for the key at the given index
	 *
	 * @param index index of the key whose entry will be deleted
	 * @return the values mapped to by the given key index prior to deletion
	 * @throws ArrayIndexOutOfBoundsException if the key index is out of range
	 *         ({@code index < 0 || index >= size()})
	 */
	public C removeAtIndex(int index){
		if(index >= size){
			throw new ArrayIndexOutOfBoundsException(index + " >= " + size);
		}
		Entry<K, C, X> p = getEntryAtIndex(index);
		C oldValue = p.value;
		deleteEntry(p);
		return oldValue;
	}

	/**
	 * Removes all of the mappings from this map.
	 * The map will be empty after this call returns.
	 */
	public void clear(){
		++modCount;
		size = 0;
		root = null;
	}

	/**
	 * Returns a shallow copy of this {@code IndexTreeMap} instance. (The keys
	 * and
	 * values themselves are not cloned.)
	 *
	 * @return a shallow copy of this map
	 */
	public Object clone(){
		IndexTreeMultiMap2<?, ?, ?> clone;
		try{
			clone = (IndexTreeMultiMap2<?, ?, ?>)super.clone();
		}
		catch(CloneNotSupportedException e){
			throw new InternalError(e);
		}

		// Put clone into "virgin" state (except for comparator)
		clone.root = null;
		clone.size = 0;
		clone.modCount = 0;
		clone.entrySet = null;
		clone.navigableKeySet = null;
		clone.descendingMap = null;

		// Initialize clone with our mappings
		try{
			clone.buildFromSorted(size, entrySet().iterator(), null, null);
		}
		catch(java.io.IOException cannotHappen){}
		catch(ClassNotFoundException cannotHappen){}

		return clone;
	}

	// NavigableMap API methods
	public Map.Entry<K, C> firstEntry(){
		return exportEntry(getFirstEntry());
	}

	public Map.Entry<K, C> lastEntry(){
		return exportEntry(getLastEntry());
	}

	public Map.Entry<K, C> pollFirstEntry(){
		Entry<K, C, X> p = getFirstEntry();
		Map.Entry<K, C> result = exportEntry(p);
		if(p != null) deleteEntry(p);
		return result;
	}

	public Map.Entry<K, C> pollLastEntry(){
		Entry<K, C, X> p = getLastEntry();
		Map.Entry<K, C> result = exportEntry(p);
		if(p != null) deleteEntry(p);
		return result;
	}

	/**
	 * @throws ClassCastException
	 *             {@inheritDoc}
	 * @throws NullPointerException
	 *             if the specified key is null
	 *             and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 * @since 1.6
	 */
	public Map.Entry<K, C> lowerEntry(K key){
		return exportEntry(getLowerEntry(key));
	}

	/**
	 * @throws ClassCastException
	 *             {@inheritDoc}
	 * @throws NullPointerException
	 *             if the specified key is null
	 *             and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 * @since 1.6
	 */
	public K lowerKey(K key){
		return keyOrNull(getLowerEntry(key));
	}

	/**
	 * @throws ClassCastException
	 *             {@inheritDoc}
	 * @throws NullPointerException
	 *             if the specified key is null
	 *             and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 * @since 1.6
	 */
	public Map.Entry<K, C> floorEntry(K key){
		return exportEntry(getFloorEntry(key));
	}

	/**
	 * @throws ClassCastException
	 *             {@inheritDoc}
	 * @throws NullPointerException
	 *             if the specified key is null
	 *             and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 * @since 1.6
	 */
	public K floorKey(K key){
		return keyOrNull(getFloorEntry(key));
	}

	/**
	 * @throws ClassCastException
	 *             {@inheritDoc}
	 * @throws NullPointerException
	 *             if the specified key is null
	 *             and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 * @since 1.6
	 */
	public Map.Entry<K, C> ceilingEntry(K key){
		return exportEntry(getCeilingEntry(key));
	}

	/**
	 * @throws ClassCastException
	 *             {@inheritDoc}
	 * @throws NullPointerException
	 *             if the specified key is null
	 *             and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 * @since 1.6
	 */
	public K ceilingKey(K key){
		return keyOrNull(getCeilingEntry(key));
	}

	/**
	 * @throws ClassCastException
	 *             {@inheritDoc}
	 * @throws NullPointerException
	 *             if the specified key is null
	 *             and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 * @since 1.6
	 */
	public Map.Entry<K, C> higherEntry(K key){
		return exportEntry(getHigherEntry(key));
	}

	/**
	 * @throws ClassCastException
	 *             {@inheritDoc}
	 * @throws NullPointerException
	 *             if the specified key is null
	 *             and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 * @since 1.6
	 */
	public K higherKey(K key){
		return keyOrNull(getHigherEntry(key));
	}

	// Views
	/**
	 * Fields initialized to contain an instance of the entry set view
	 * the first time this view is requested. Views are stateless, so
	 * there's no reason to create more than one.
	 */
	private transient EntrySet entrySet;
	private transient KeySet<K, C, X> navigableKeySet;
	private transient NavigableMap<K, C> descendingMap;

	/**
	 * Returns a {@link Set} view of the keys contained in this map.
	 *
	 * <p>
	 * The set's iterator returns the keys in ascending order.
	 * The set's spliterator is
	 * <em><a href="Spliterator.html#binding">late-binding</a></em>,
	 * <em>fail-fast</em>, and additionally reports {@link Spliterator#SORTED}
	 * and {@link Spliterator#ORDERED} with an encounter order that is ascending
	 * key order. The spliterator's comparator (see
	 * {@link java.util.Spliterator#getComparator()}) is {@code null} if
	 * the tree map's comparator (see {@link #comparator()}) is {@code null}.
	 * Otherwise, the spliterator's comparator is the same as or imposes the
	 * same total ordering as the tree map's comparator.
	 *
	 * <p>
	 * The set is backed by the map, so changes to the map are
	 * reflected in the set, and vice-versa. If the map is modified
	 * while an iteration over the set is in progress (except through
	 * the iterator's own {@code remove} operation), the results of
	 * the iteration are undefined. The set supports element removal,
	 * which removes the corresponding mapping from the map, via the
	 * {@code Iterator.remove}, {@code Set.remove},
	 * {@code removeAll}, {@code retainAll}, and {@code clear}
	 * operations. It does not support the {@code add} or {@code addAll}
	 * operations.
	 */
	public Set<K> keySet(){
		return navigableKeySet();
	}

	public NavigableSet<K> navigableKeySet(){
		KeySet<K, C, X> nks = navigableKeySet;
		return (nks != null) ? nks : (navigableKeySet = new KeySet<>(this));
	}

	public NavigableSet<K> descendingKeySet(){
		return descendingMap().navigableKeySet();
	}

	//TODO: return Collection<X> instead?
	/**
	 * Returns a {@link Collection} view of the values contained in this map.
	 *
	 * <p>
	 * The collection's iterator returns the values in ascending order
	 * of the corresponding keys. The collection's spliterator is
	 * <em><a href="Spliterator.html#binding">late-binding</a></em>,
	 * <em>fail-fast</em>, and additionally reports {@link Spliterator#ORDERED}
	 * with an encounter order that is ascending order of the corresponding
	 * keys.
	 *
	 * <p>
	 * The collection is backed by the map, so changes to the map are
	 * reflected in the collection, and vice-versa. If the map is
	 * modified while an iteration over the collection is in progress
	 * (except through the iterator's own {@code remove} operation),
	 * the results of the iteration are undefined. The collection
	 * supports element removal, which removes the corresponding
	 * mapping from the map, via the {@code Iterator.remove},
	 * {@code Collection.remove}, {@code removeAll},
	 * {@code retainAll} and {@code clear} operations. It does not
	 * support the {@code add} or {@code addAll} operations.
	 */
	@SuppressWarnings("unchecked")
	public Collection<C> values(){
		Collection<C> vs = null;
		try{
			Field transientField = getClass().getDeclaredField("values");
			System.out.print("Access?: " + transientField.isAccessible());
			transientField.setAccessible(true);
			vs = (Collection<C>) transientField.get(this);
			if(vs == null){
				vs = new Values();
				transientField.set(this, vs);
			}
		}
		catch(IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e){}
		return vs;
	}

	/**
	 * Returns a {@link Set} view of the mappings contained in this map.
	 *
	 * <p>
	 * The set's iterator returns the entries in ascending key order. The
	 * sets's spliterator is
	 * <em><a href="Spliterator.html#binding">late-binding</a></em>,
	 * <em>fail-fast</em>, and additionally reports {@link Spliterator#SORTED}
	 * and
	 * {@link Spliterator#ORDERED} with an encounter order that is ascending key
	 * order.
	 *
	 * <p>
	 * The set is backed by the map, so changes to the map are
	 * reflected in the set, and vice-versa. If the map is modified
	 * while an iteration over the set is in progress (except through
	 * the iterator's own {@code remove} operation, or through the
	 * {@code setValue} operation on a map entry returned by the
	 * iterator) the results of the iteration are undefined. The set
	 * supports element removal, which removes the corresponding
	 * mapping from the map, via the {@code Iterator.remove},
	 * {@code Set.remove}, {@code removeAll}, {@code retainAll} and
	 * {@code clear} operations. It does not support the
	 * {@code add} or {@code addAll} operations.
	 */
	public Set<Map.Entry<K, C>> entrySet(){
		EntrySet es = entrySet;
		return (es != null) ? es : (entrySet = new EntrySet());
	}

	public NavigableMap<K, C> descendingMap(){
		NavigableMap<K, C> km = descendingMap;
		return (km != null) ? km : (descendingMap =
				new DescendingSubMap<>(this, true, null, true, true, null, true));
	}

	/**
	 * @throws ClassCastException
	 *             {@inheritDoc}
	 * @throws NullPointerException
	 *             if {@code fromKey} or {@code toKey} is
	 *             null and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 * @throws IllegalArgumentException
	 *             {@inheritDoc}
	 * @since 1.6
	 */
	public NavigableMap<K, C> subMap(
K fromKey, boolean fromInclusive, K toKey, boolean toInclusive){
		return new AscendingSubMap<>(this, false, fromKey, fromInclusive, false, toKey, toInclusive);
	}

	/**
	 * @throws ClassCastException
	 *             {@inheritDoc}
	 * @throws NullPointerException
	 *             if {@code toKey} is null
	 *             and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 * @throws IllegalArgumentException
	 *             {@inheritDoc}
	 * @since 1.6
	 */
	public NavigableMap<K, C> headMap(K toKey, boolean inclusive){
		return new AscendingSubMap<>(this, true, null, true, false, toKey, inclusive);
	}

	/**
	 * @throws ClassCastException
	 *             {@inheritDoc}
	 * @throws NullPointerException
	 *             if {@code fromKey} is null
	 *             and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 * @throws IllegalArgumentException
	 *             {@inheritDoc}
	 * @since 1.6
	 */
	public NavigableMap<K, C> tailMap(K fromKey, boolean inclusive){
		return new AscendingSubMap<>(this, false, fromKey, inclusive, true, null, true);
	}

	/**
	 * @throws ClassCastException
	 *             {@inheritDoc}
	 * @throws NullPointerException
	 *             if {@code fromKey} or {@code toKey} is
	 *             null and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 * @throws IllegalArgumentException
	 *             {@inheritDoc}
	 */
	public SortedMap<K, C> subMap(K fromKey, K toKey){
		return subMap(fromKey, true, toKey, false);
	}

	/**
	 * @throws ClassCastException
	 *             {@inheritDoc}
	 * @throws NullPointerException
	 *             if {@code toKey} is null
	 *             and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 * @throws IllegalArgumentException
	 *             {@inheritDoc}
	 */
	public SortedMap<K, C> headMap(K toKey){
		return headMap(toKey, false);
	}

	/**
	 * @throws ClassCastException
	 *             {@inheritDoc}
	 * @throws NullPointerException
	 *             if {@code fromKey} is null
	 *             and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 * @throws IllegalArgumentException
	 *             {@inheritDoc}
	 */
	public SortedMap<K, C> tailMap(K fromKey){
		return tailMap(fromKey, true);
	}

	@Override public boolean replace(K key, C oldValue, C newValue){
		Entry<K, C, X> p = getEntry(key);
		if(p != null && Objects.equals(oldValue, p.value)){
			p.value = newValue;
			return true;
		}
		return false;
	}

	@Override public C replace(K key, C value){
		Entry<K, C, X> p = getEntry(key);
		if(p != null){
			C oldValue = reflClone(p.value);
			p.value = value;
			return oldValue;
		}
		return null;
	}

	@Override public void forEach(BiConsumer<? super K, ? super C> action){
		Objects.requireNonNull(action);
		int expectedModCount = modCount;
		for(Entry<K, C, X> e = getFirstEntry(); e != null; e = successor(e)){
			action.accept(e.key, e.value);

			if(expectedModCount != modCount){
				throw new ConcurrentModificationException();
			}
		}
	}

	@Override public void replaceAll(
			BiFunction<? super K, ? super C, ? extends C> function){
		Objects.requireNonNull(function);
		int expectedModCount = modCount;

		for(Entry<K, C, X> e = getFirstEntry(); e != null; e = successor(e)){
			C newValue = function.apply(e.key, e.value);

			e.value = newValue;

			if(expectedModCount != modCount){
				throw new ConcurrentModificationException();
			}
		}
	}

	// View class support
	class Values extends AbstractCollection<C>{
		public Iterator<C> iterator(){
			return new ValueIterator(getFirstEntry());
		}

		public int size(){
			return IndexTreeMultiMap2.this.size();
		}

		public boolean contains(Object o){
			return IndexTreeMultiMap2.this.containsValue(o);
		}

		public boolean remove(Object o){
			for(Entry<K, C, X> e = getFirstEntry(); e != null; e = successor(e)){
				if(valEquals(e.getValue(), o)){
					deleteEntry(e);
					return true;
				}
			}
			return false;
		}

		public void clear(){
			IndexTreeMultiMap2.this.clear();
		}

		public Spliterator<C> spliterator(){
			return new ValueSpliterator<K, C, X>(IndexTreeMultiMap2.this, null, null, 0, -1, 0);
		}
	}

	class EntrySet extends AbstractSet<Map.Entry<K, C>>{
		public Iterator<Map.Entry<K, C>> iterator(){
			return new EntryIterator(getFirstEntry());
		}

		public boolean contains(Object o){
			if(!(o instanceof Map.Entry)) return false;
			Map.Entry<?, ?> entry = (Map.Entry<?, ?>)o;
			Object value = entry.getValue();
			Entry<K, C, X> p = getEntry(entry.getKey());
			return p != null && valEquals(p.getValue(), value);
		}

		public boolean remove(Object o){
			if(!(o instanceof Map.Entry)) return false;
			Map.Entry<?, ?> entry = (Map.Entry<?, ?>)o;
			Object value = entry.getValue();
			Entry<K, C, X> p = getEntry(entry.getKey());
			if(p != null && valEquals(p.getValue(), value)){
				deleteEntry(p);
				return true;
			}
			return false;
		}

		public int size(){
			return IndexTreeMultiMap2.this.size();
		}

		public void clear(){
			IndexTreeMultiMap2.this.clear();
		}

		public Spliterator<Map.Entry<K, C>> spliterator(){
			return new EntrySpliterator<K, C, X>(IndexTreeMultiMap2.this, null, null, 0, -1, 0);
		}
	}

	Iterator<K> keyIterator(){
		return new KeyIterator(getFirstEntry());
	}

	Iterator<K> descendingKeyIterator(){
		return new DescendingKeyIterator(getLastEntry());
	}

	static final class KeySet<K, C extends Cloneable & Collection<X>, X>
	extends AbstractSet<K> implements NavigableSet<K>{
		private final NavigableMap<K, C> m;

		KeySet(NavigableMap<K, C> map){
			m = map;
		}

		@SuppressWarnings("unchecked")
		public Iterator<K> iterator(){
			if(m instanceof IndexTreeMultiMap2) return ((IndexTreeMultiMap2<K, C, X>)m).keyIterator();
			else return ((IndexTreeMultiMap2.NavigableSubMap<K, C, X>)m).keyIterator();
		}

		@SuppressWarnings("unchecked")
		public Iterator<K> descendingIterator(){
			if(m instanceof IndexTreeMultiMap2) return ((IndexTreeMultiMap2<K, C, X>)m).descendingKeyIterator();
			else return ((IndexTreeMultiMap2.NavigableSubMap<K, C, X>)m).descendingKeyIterator();
		}

		public int size(){
			return m.size();
		}
		public boolean isEmpty(){
			return m.isEmpty();
		}
		public boolean contains(Object o){
			return m.containsKey(o);
		}
		public void clear(){
			m.clear();
		}
		public K lower(K k){
			return m.lowerKey(k);
		}
		public K floor(K k){
			return m.floorKey(k);
		}
		public K ceiling(K k){
			return m.ceilingKey(k);
		}
		public K higher(K k){
			return m.higherKey(k);
		}
		public K first(){
			return m.firstKey();
		}
		public K last(){
			return m.lastKey();
		}
		public Comparator<? super K> comparator(){
			return m.comparator();
		}
		public K pollFirst(){
			Map.Entry<K, ?> e = m.pollFirstEntry();
			return (e == null) ? null : e.getKey();
		}
		public K pollLast(){
			Map.Entry<K, ?> e = m.pollLastEntry();
			return (e == null) ? null : e.getKey();
		}
		public boolean remove(Object o){
			int oldSize = size();
			m.remove(o);
			return size() != oldSize;
		}
		public NavigableSet<K> subSet(K fromElement, boolean fromInclusive, K toElement, boolean toInclusive){
			return new KeySet<>(m.subMap(fromElement, fromInclusive, toElement, toInclusive));
		}
		public NavigableSet<K> headSet(K toElement, boolean inclusive){
			return new KeySet<>(m.headMap(toElement, inclusive));
		}
		public NavigableSet<K> tailSet(K fromElement, boolean inclusive){
			return new KeySet<>(m.tailMap(fromElement, inclusive));
		}
		public SortedSet<K> subSet(K fromElement, K toElement){
			return subSet(fromElement, true, toElement, false);
		}
		public SortedSet<K> headSet(K toElement){
			return headSet(toElement, false);
		}
		public SortedSet<K> tailSet(K fromElement){
			return tailSet(fromElement, true);
		}
		public NavigableSet<K> descendingSet(){
			return new KeySet<>(m.descendingMap());
		}

		public Spliterator<K> spliterator(){
			return keySpliteratorFor(m);
		}
	}

	/**
	 * Base class for IndexTreeMap Iterators
	 */
	abstract class PrivateEntryIterator<T> implements Iterator<T>{
		Entry<K, C, X> next;
		Entry<K, C, X> lastReturned;
		int expectedModCount;

		PrivateEntryIterator(Entry<K, C, X> first){
			expectedModCount = modCount;
			lastReturned = null;
			next = first;
		}

		public final boolean hasNext(){
			return next != null;
		}

		final Entry<K, C, X> nextEntry(){
			Entry<K, C, X> e = next;
			if(e == null) throw new NoSuchElementException();
			if(modCount != expectedModCount) throw new ConcurrentModificationException();
			next = successor(e);
			lastReturned = e;
			return e;
		}

		final Entry<K, C, X> prevEntry(){
			Entry<K, C, X> e = next;
			if(e == null) throw new NoSuchElementException();
			if(modCount != expectedModCount) throw new ConcurrentModificationException();
			next = predecessor(e);
			lastReturned = e;
			return e;
		}

		public void remove(){
			if(lastReturned == null) throw new IllegalStateException();
			if(modCount != expectedModCount) throw new ConcurrentModificationException();
			// deleted entries are replaced by their successors
			if(lastReturned.left != null && lastReturned.right != null) next = lastReturned;
			deleteEntry(lastReturned);
			expectedModCount = modCount;
			lastReturned = null;
		}
	}

	final class EntryIterator extends PrivateEntryIterator<Map.Entry<K, C>>{
		EntryIterator(Entry<K, C, X> first){
			super(first);
		}
		public Map.Entry<K, C> next(){
			return nextEntry();
		}
	}

	final class ValueIterator extends PrivateEntryIterator<C>{
		ValueIterator(Entry<K, C, X> first){
			super(first);
		}
		public C next(){
			return nextEntry().value;
		}
	}

	final class KeyIterator extends PrivateEntryIterator<K>{
		KeyIterator(Entry<K, C, X> first){
			super(first);
		}
		public K next(){
			return nextEntry().key;
		}
	}

	final class DescendingKeyIterator extends PrivateEntryIterator<K>{
		DescendingKeyIterator(Entry<K, C, X> first){
			super(first);
		}
		public K next(){
			return prevEntry().key;
		}
		public void remove(){
			if(lastReturned == null) throw new IllegalStateException();
			if(modCount != expectedModCount) throw new ConcurrentModificationException();
			deleteEntry(lastReturned);
			lastReturned = null;
			expectedModCount = modCount;
		}
	}

	// Little utilities
	/**
	 * Compares two keys using the correct comparison method for this IndexTreeMap.
	 */
	@SuppressWarnings("unchecked") final int compare(Object k1, Object k2){
		return comparator == null ? ((Comparable<? super K>)k1).compareTo((K)k2) : comparator.compare((K)k1, (K)k2);
	}

	/**
	 * Test two values for equality. Differs from o1.equals(o2) only in
	 * that it copes with {@code null} o1 properly.
	 */
	static final boolean valEquals(Object o1, Object o2){
		return (o1 == null ? o2 == null : o1.equals(o2));
	}

	/**
	 * Return SimpleImmutableEntry for entry, or null if null
	 */
	static <K, C extends Cloneable & Collection<X>, X> Map.Entry<K, C> exportEntry(
			IndexTreeMultiMap2.Entry<K, C, X> e){
		return (e == null) ? null : new AbstractMap.SimpleImmutableEntry<>(e);
	}

	/**
	 * Return key for entry, or null if null
	 */
	static <K, C extends Cloneable & Collection<X>, X> K keyOrNull(IndexTreeMultiMap2.Entry<K, C, X> e){
		return (e == null) ? null : e.key;
	}

	/**
	 * Returns the key corresponding to the specified Entry.
	 * 
	 * @throws NoSuchElementException
	 *             if the Entry is null
	 */
	static <K> K key(Entry<K, ?, ?> e){
		if(e == null) throw new NoSuchElementException();
		return e.key;
	}

	// SubMaps
	/**
	 * Dummy value serving as unmatchable fence key for unbounded SubMapIterators
	 */
	private static final Object UNBOUNDED = new Object();

	/**
	 * @serial include
	 */
	abstract static class NavigableSubMap<K, C extends Cloneable & Collection<X>, X>
	extends AbstractMap<K, C> implements NavigableMap<K, C>, java.io.Serializable{
		private static final long serialVersionUID = -2102997345730753777L;
		/**
		 * The backing map.
		 */
		final IndexTreeMultiMap2<K, C, X> m;

		/**
		 * Endpoints are represented as triples (fromStart, lo,
		 * loInclusive) and (toEnd, hi, hiInclusive). If fromStart is
		 * true, then the low (absolute) bound is the start of the
		 * backing map, and the other values are ignored. Otherwise,
		 * if loInclusive is true, lo is the inclusive bound, else lo
		 * is the exclusive bound. Similarly for the upper bound.
		 */
		final K lo, hi;
		final boolean fromStart, toEnd;
		final boolean loInclusive, hiInclusive;

		NavigableSubMap(IndexTreeMultiMap2<K, C, X> m, boolean fromStart,
				K lo, boolean loInclusive, boolean toEnd, K hi, boolean hiInclusive){
			if(!fromStart && !toEnd){
				if(m.compare(lo, hi) > 0) throw new IllegalArgumentException("fromKey > toKey");
			}
			else{
				if(!fromStart) // type check
					m.compare(lo, lo);
				if(!toEnd) m.compare(hi, hi);
			}

			this.m = m;
			this.fromStart = fromStart;
			this.lo = lo;
			this.loInclusive = loInclusive;
			this.toEnd = toEnd;
			this.hi = hi;
			this.hiInclusive = hiInclusive;
		}

		// internal utilities
		final boolean tooLow(Object key){
			if(!fromStart){
				int c = m.compare(key, lo);
				if(c < 0 || (c == 0 && !loInclusive)) return true;
			}
			return false;
		}

		final boolean tooHigh(Object key){
			if(!toEnd){
				int c = m.compare(key, hi);
				if(c > 0 || (c == 0 && !hiInclusive)) return true;
			}
			return false;
		}

		final boolean inRange(Object key){
			return !tooLow(key) && !tooHigh(key);
		}

		final boolean inClosedRange(Object key){
			return (fromStart || m.compare(key, lo) >= 0) && (toEnd || m.compare(hi, key) >= 0);
		}

		final boolean inRange(Object key, boolean inclusive){
			return inclusive ? inRange(key) : inClosedRange(key);
		}

		/*
		 * Absolute versions of relation operations.
		 * Subclasses map to these using like-named "sub"
		 * versions that invert senses for descending maps
		 */

		final IndexTreeMultiMap2.Entry<K, C, X> absLowest(){
			IndexTreeMultiMap2.Entry<K, C, X> e = (fromStart ? m.getFirstEntry() :
				(loInclusive ? m.getCeilingEntry(lo) : m.getHigherEntry(lo)));
			return (e == null || tooHigh(e.key)) ? null : e;
		}

		final IndexTreeMultiMap2.Entry<K, C, X> absHighest(){
			IndexTreeMultiMap2.Entry<K, C, X> e = (toEnd ? m.getLastEntry() :
				(hiInclusive ? m.getFloorEntry(hi) : m.getLowerEntry(hi)));
			return (e == null || tooLow(e.key)) ? null : e;
		}

		final IndexTreeMultiMap2.Entry<K, C, X> absCeiling(K key){
			if(tooLow(key)) return absLowest();
			IndexTreeMultiMap2.Entry<K, C, X> e = m.getCeilingEntry(key);
			return (e == null || tooHigh(e.key)) ? null : e;
		}

		final IndexTreeMultiMap2.Entry<K, C, X> absHigher(K key){
			if(tooLow(key)) return absLowest();
			IndexTreeMultiMap2.Entry<K, C, X> e = m.getHigherEntry(key);
			return (e == null || tooHigh(e.key)) ? null : e;
		}

		final IndexTreeMultiMap2.Entry<K, C, X> absFloor(K key){
			if(tooHigh(key)) return absHighest();
			IndexTreeMultiMap2.Entry<K, C, X> e = m.getFloorEntry(key);
			return (e == null || tooLow(e.key)) ? null : e;
		}

		final IndexTreeMultiMap2.Entry<K, C, X> absLower(K key){
			if(tooHigh(key)) return absHighest();
			IndexTreeMultiMap2.Entry<K, C, X> e = m.getLowerEntry(key);
			return (e == null || tooLow(e.key)) ? null : e;
		}

		/** Returns the absolute high fence for ascending traversal */
		final IndexTreeMultiMap2.Entry<K, C, X> absHighFence(){
			return (toEnd ? null : (hiInclusive ? m.getHigherEntry(hi) : m.getCeilingEntry(hi)));
		}

		/** Return the absolute low fence for descending traversal */
		final IndexTreeMultiMap2.Entry<K, C, X> absLowFence(){
			return (fromStart ? null : (loInclusive ? m.getLowerEntry(lo) : m.getFloorEntry(lo)));
		}

		// Abstract methods defined in ascending vs descending classes
		// These relay to the appropriate absolute versions

		abstract IndexTreeMultiMap2.Entry<K, C, X> subLowest();
		abstract IndexTreeMultiMap2.Entry<K, C, X> subHighest();
		abstract IndexTreeMultiMap2.Entry<K, C, X> subCeiling(K key);
		abstract IndexTreeMultiMap2.Entry<K, C, X> subHigher(K key);
		abstract IndexTreeMultiMap2.Entry<K, C, X> subFloor(K key);
		abstract IndexTreeMultiMap2.Entry<K, C, X> subLower(K key);

		/** Returns ascending iterator from the perspective of this submap */
		abstract Iterator<K> keyIterator();

		abstract Spliterator<K> keySpliterator();

		/** Returns descending iterator from the perspective of this submap */
		abstract Iterator<K> descendingKeyIterator();

		// public methods
		public boolean isEmpty(){
			return (fromStart && toEnd) ? m.isEmpty() : entrySet().isEmpty();
		}

		public int size(){
			return (fromStart && toEnd) ? m.size() : entrySet().size();
		}

		public final boolean containsKey(Object key){
			return inRange(key) && m.containsKey(key);
		}

		public final C put(K key, C value){
			if(!inRange(key)) throw new IllegalArgumentException("key out of range");
			return m.put(key, value);
		}

		public final C get(Object key){
			return !inRange(key) ? null : m.get(key);
		}

		public final C remove(Object key){
			return !inRange(key) ? null : m.remove(key);
		}

		public final Map.Entry<K, C> ceilingEntry(K key){
			return exportEntry(subCeiling(key));
		}

		public final K ceilingKey(K key){
			return keyOrNull(subCeiling(key));
		}

		public final Map.Entry<K, C> higherEntry(K key){
			return exportEntry(subHigher(key));
		}

		public final K higherKey(K key){
			return keyOrNull(subHigher(key));
		}

		public final Map.Entry<K, C> floorEntry(K key){
			return exportEntry(subFloor(key));
		}

		public final K floorKey(K key){
			return keyOrNull(subFloor(key));
		}

		public final Map.Entry<K, C> lowerEntry(K key){
			return exportEntry(subLower(key));
		}

		public final K lowerKey(K key){
			return keyOrNull(subLower(key));
		}

		public final K firstKey(){
			return key(subLowest());
		}

		public final K lastKey(){
			return key(subHighest());
		}

		public final Map.Entry<K, C> firstEntry(){
			return exportEntry(subLowest());
		}

		public final Map.Entry<K, C> lastEntry(){
			return exportEntry(subHighest());
		}

		public final Map.Entry<K, C> pollFirstEntry(){
			IndexTreeMultiMap2.Entry<K, C, X> e = subLowest();
			Map.Entry<K, C> result = exportEntry(e);
			if(e != null) m.deleteEntry(e);
			return result;
		}

		public final Map.Entry<K, C> pollLastEntry(){
			IndexTreeMultiMap2.Entry<K, C, X> e = subHighest();
			Map.Entry<K, C> result = exportEntry(e);
			if(e != null) m.deleteEntry(e);
			return result;
		}

		// Views
		transient NavigableMap<K, C> descendingMapView;
		transient EntrySetView entrySetView;
		transient KeySet<K, C, X> navigableKeySetView;

		public final NavigableSet<K> navigableKeySet(){
			KeySet<K, C, X> nksv = navigableKeySetView;
			return (nksv != null) ? nksv : (navigableKeySetView = new IndexTreeMultiMap2.KeySet<>(this));
		}

		public final Set<K> keySet(){
			return navigableKeySet();
		}

		public NavigableSet<K> descendingKeySet(){
			return descendingMap().navigableKeySet();
		}

		public final SortedMap<K, C> subMap(K fromKey, K toKey){
			return subMap(fromKey, true, toKey, false);
		}

		public final SortedMap<K, C> headMap(K toKey){
			return headMap(toKey, false);
		}

		public final SortedMap<K, C> tailMap(K fromKey){
			return tailMap(fromKey, true);
		}

		// View classes
		abstract class EntrySetView extends AbstractSet<Map.Entry<K, C>>{
			private transient int size = -1, sizeModCount;

			public int size(){
				if(fromStart && toEnd) return m.size();
				if(size == -1 || sizeModCount != m.modCount){
					sizeModCount = m.modCount;
					size = 0;
					Iterator<?> i = iterator();
					while(i.hasNext()){
						++size;
						i.next();
					}
				}
				return size;
			}

			public boolean isEmpty(){
				IndexTreeMultiMap2.Entry<K, C, X> n = absLowest();
				return n == null || tooHigh(n.key);
			}

			public boolean contains(Object o){
				if(!(o instanceof Map.Entry)) return false;
				Map.Entry<?, ?> entry = (Map.Entry<?, ?>)o;
				Object key = entry.getKey();
				if(!inRange(key)) return false;
				IndexTreeMultiMap2.Entry<?, ?, ?> node = m.getEntry(key);
				return node != null && valEquals(node.getValue(), entry.getValue());
			}

			public boolean remove(Object o){
				if(!(o instanceof Map.Entry)) return false;
				Map.Entry<?, ?> entry = (Map.Entry<?, ?>)o;
				Object key = entry.getKey();
				if(!inRange(key)) return false;
				IndexTreeMultiMap2.Entry<K, C, X> node = m.getEntry(key);
				if(node != null && valEquals(node.getValue(), entry.getValue())){
					m.deleteEntry(node);
					return true;
				}
				return false;
			}
		}

		/**
		 * Iterators for SubMaps
		 */
		abstract class SubMapIterator<T> implements Iterator<T>{
			IndexTreeMultiMap2.Entry<K, C, X> lastReturned;
			IndexTreeMultiMap2.Entry<K, C, X> next;
			final Object fenceKey;
			int expectedModCount;

			SubMapIterator(IndexTreeMultiMap2.Entry<K, C, X> first, IndexTreeMultiMap2.Entry<K, C, X> fence){
				expectedModCount = m.modCount;
				lastReturned = null;
				next = first;
				fenceKey = fence == null ? UNBOUNDED : fence.key;
			}

			public final boolean hasNext(){
				return next != null && next.key != fenceKey;
			}

			final IndexTreeMultiMap2.Entry<K, C, X> nextEntry(){
				IndexTreeMultiMap2.Entry<K, C, X> e = next;
				if(e == null || e.key == fenceKey) throw new NoSuchElementException();
				if(m.modCount != expectedModCount) throw new ConcurrentModificationException();
				next = successor(e);
				lastReturned = e;
				return e;
			}

			final IndexTreeMultiMap2.Entry<K, C, X> prevEntry(){
				IndexTreeMultiMap2.Entry<K, C, X> e = next;
				if(e == null || e.key == fenceKey) throw new NoSuchElementException();
				if(m.modCount != expectedModCount) throw new ConcurrentModificationException();
				next = predecessor(e);
				lastReturned = e;
				return e;
			}

			final void removeAscending(){
				if(lastReturned == null) throw new IllegalStateException();
				if(m.modCount != expectedModCount) throw new ConcurrentModificationException();
				// deleted entries are replaced by their successors
				if(lastReturned.left != null && lastReturned.right != null) next = lastReturned;
				m.deleteEntry(lastReturned);
				lastReturned = null;
				expectedModCount = m.modCount;
			}

			final void removeDescending(){
				if(lastReturned == null) throw new IllegalStateException();
				if(m.modCount != expectedModCount) throw new ConcurrentModificationException();
				m.deleteEntry(lastReturned);
				lastReturned = null;
				expectedModCount = m.modCount;
			}

		}

		final class SubMapEntryIterator extends SubMapIterator<Map.Entry<K, C>>{
			SubMapEntryIterator(IndexTreeMultiMap2.Entry<K, C, X> first, IndexTreeMultiMap2.Entry<K, C, X> fence){
				super(first, fence);
			}
			public Map.Entry<K, C> next(){
				return nextEntry();
			}
			public void remove(){
				removeAscending();
			}
		}

		final class DescendingSubMapEntryIterator extends SubMapIterator<Map.Entry<K, C>>{
			DescendingSubMapEntryIterator(
					IndexTreeMultiMap2.Entry<K, C, X> last, IndexTreeMultiMap2.Entry<K, C, X> fence){
				super(last, fence);
			}

			public Map.Entry<K, C> next(){
				return prevEntry();
			}
			public void remove(){
				removeDescending();
			}
		}

		// Implement minimal Spliterator as KeySpliterator backup
		final class SubMapKeyIterator extends SubMapIterator<K> implements Spliterator<K>{
			SubMapKeyIterator(IndexTreeMultiMap2.Entry<K, C, X> first, IndexTreeMultiMap2.Entry<K, C, X> fence){
				super(first, fence);
			}
			public K next(){
				return nextEntry().key;
			}
			public void remove(){
				removeAscending();
			}
			public Spliterator<K> trySplit(){
				return null;
			}
			public void forEachRemaining(Consumer<? super K> action){
				while(hasNext())
					action.accept(next());
			}
			public boolean tryAdvance(Consumer<? super K> action){
				if(hasNext()){
					action.accept(next());
					return true;
				}
				return false;
			}
			public long estimateSize(){
				return Long.MAX_VALUE;
			}
			public int characteristics(){
				return Spliterator.DISTINCT | Spliterator.ORDERED | Spliterator.SORTED;
			}
			public final Comparator<? super K> getComparator(){
				return NavigableSubMap.this.comparator();
			}
		}

		final class DescendingSubMapKeyIterator extends SubMapIterator<K> implements Spliterator<K>{
			DescendingSubMapKeyIterator(IndexTreeMultiMap2.Entry<K, C, X> last,
					IndexTreeMultiMap2.Entry<K, C, X> fence){
				super(last, fence);
			}
			public K next(){
				return prevEntry().key;
			}
			public void remove(){
				removeDescending();
			}
			public Spliterator<K> trySplit(){
				return null;
			}
			public void forEachRemaining(Consumer<? super K> action){
				while(hasNext())
					action.accept(next());
			}
			public boolean tryAdvance(Consumer<? super K> action){
				if(hasNext()){
					action.accept(next());
					return true;
				}
				return false;
			}
			public long estimateSize(){
				return Long.MAX_VALUE;
			}
			public int characteristics(){
				return Spliterator.DISTINCT | Spliterator.ORDERED;
			}
		}
	}

	/**
	 * @serial include
	 */
	static final class AscendingSubMap<K, C extends Cloneable & Collection<X>, X> extends NavigableSubMap<K, C, X>{
		private static final long serialVersionUID = 912986545866124777L;

		AscendingSubMap(IndexTreeMultiMap2<K, C, X> m, boolean fromStart, K lo, boolean loInclusive, boolean toEnd,
				K hi, boolean hiInclusive){
			super(m, fromStart, lo, loInclusive, toEnd, hi, hiInclusive);
		}

		public Comparator<? super K> comparator(){
			return m.comparator();
		}

		public NavigableMap<K, C> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive){
			if(!inRange(fromKey, fromInclusive)) throw new IllegalArgumentException("fromKey out of range");
			if(!inRange(toKey, toInclusive)) throw new IllegalArgumentException("toKey out of range");
			return new AscendingSubMap<>(m, false, fromKey, fromInclusive, false, toKey, toInclusive);
		}

		public NavigableMap<K, C> headMap(K toKey, boolean inclusive){
			if(!inRange(toKey, inclusive)) throw new IllegalArgumentException("toKey out of range");
			return new AscendingSubMap<>(m, fromStart, lo, loInclusive, false, toKey, inclusive);
		}

		public NavigableMap<K, C> tailMap(K fromKey, boolean inclusive){
			if(!inRange(fromKey, inclusive)) throw new IllegalArgumentException("fromKey out of range");
			return new AscendingSubMap<>(m, false, fromKey, inclusive, toEnd, hi, hiInclusive);
		}

		public NavigableMap<K, C> descendingMap(){
			NavigableMap<K, C> mv = descendingMapView;
			return (mv != null) ? mv : (descendingMapView = new DescendingSubMap<>(m, fromStart, lo,
					loInclusive, toEnd, hi, hiInclusive));
		}

		Iterator<K> keyIterator(){
			return new SubMapKeyIterator(absLowest(), absHighFence());
		}

		Spliterator<K> keySpliterator(){
			return new SubMapKeyIterator(absLowest(), absHighFence());
		}

		Iterator<K> descendingKeyIterator(){
			return new DescendingSubMapKeyIterator(absHighest(), absLowFence());
		}

		final class AscendingEntrySetView extends EntrySetView{
			public Iterator<Map.Entry<K, C>> iterator(){
				return new SubMapEntryIterator(absLowest(), absHighFence());
			}
		}

		public Set<Map.Entry<K, C>> entrySet(){
			EntrySetView es = entrySetView;
			return (es != null) ? es : (entrySetView = new AscendingEntrySetView());
		}

		IndexTreeMultiMap2.Entry<K, C, X> subLowest(){
			return absLowest();
		}
		IndexTreeMultiMap2.Entry<K, C, X> subHighest(){
			return absHighest();
		}
		IndexTreeMultiMap2.Entry<K, C, X> subCeiling(K key){
			return absCeiling(key);
		}
		IndexTreeMultiMap2.Entry<K, C, X> subHigher(K key){
			return absHigher(key);
		}
		IndexTreeMultiMap2.Entry<K, C, X> subFloor(K key){
			return absFloor(key);
		}
		IndexTreeMultiMap2.Entry<K, C, X> subLower(K key){
			return absLower(key);
		}
	}

	/**
	 * @serial include
	 */
	static final class DescendingSubMap<K, C extends Cloneable & Collection<X>, X> extends NavigableSubMap<K, C, X>{
		private static final long serialVersionUID = 912986545866120777L;

		DescendingSubMap(IndexTreeMultiMap2<K, C, X> m,
				boolean fromStart, K lo, boolean loInclusive, boolean toEnd, K hi, boolean hiInclusive){
			super(m, fromStart, lo, loInclusive, toEnd, hi, hiInclusive);
		}

		private final Comparator<? super K> reverseComparator = Collections.reverseOrder(m.comparator);

		public Comparator<? super K> comparator(){
			return reverseComparator;
		}

		public NavigableMap<K, C> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive){
			if(!inRange(fromKey, fromInclusive)) throw new IllegalArgumentException("fromKey out of range");
			if(!inRange(toKey, toInclusive)) throw new IllegalArgumentException("toKey out of range");
			return new DescendingSubMap<>(m, false, toKey, toInclusive, false, fromKey, fromInclusive);
		}

		public NavigableMap<K, C> headMap(K toKey, boolean inclusive){
			if(!inRange(toKey, inclusive)) throw new IllegalArgumentException("toKey out of range");
			return new DescendingSubMap<>(m, false, toKey, inclusive, toEnd, hi, hiInclusive);
		}

		public NavigableMap<K, C> tailMap(K fromKey, boolean inclusive){
			if(!inRange(fromKey, inclusive)) throw new IllegalArgumentException("fromKey out of range");
			return new DescendingSubMap<>(m, fromStart, lo, loInclusive, false, fromKey, inclusive);
		}

		public NavigableMap<K, C> descendingMap(){
			NavigableMap<K, C> mv = descendingMapView;
			return (mv != null) ? mv : (descendingMapView = new AscendingSubMap<>(m, fromStart, lo,
					loInclusive, toEnd, hi, hiInclusive));
		}

		Iterator<K> keyIterator(){
			return new DescendingSubMapKeyIterator(absHighest(), absLowFence());
		}

		Spliterator<K> keySpliterator(){
			return new DescendingSubMapKeyIterator(absHighest(), absLowFence());
		}

		Iterator<K> descendingKeyIterator(){
			return new SubMapKeyIterator(absLowest(), absHighFence());
		}

		final class DescendingEntrySetView extends EntrySetView{
			public Iterator<Map.Entry<K, C>> iterator(){
				return new DescendingSubMapEntryIterator(absHighest(), absLowFence());
			}
		}

		public Set<Map.Entry<K, C>> entrySet(){
			EntrySetView es = entrySetView;
			return (es != null) ? es : (entrySetView = new DescendingEntrySetView());
		}

		IndexTreeMultiMap2.Entry<K, C, X> subLowest(){
			return absHighest();
		}
		IndexTreeMultiMap2.Entry<K, C, X> subHighest(){
			return absLowest();
		}
		IndexTreeMultiMap2.Entry<K, C, X> subCeiling(K key){
			return absFloor(key);
		}
		IndexTreeMultiMap2.Entry<K, C, X> subHigher(K key){
			return absLower(key);
		}
		IndexTreeMultiMap2.Entry<K, C, X> subFloor(K key){
			return absCeiling(key);
		}
		IndexTreeMultiMap2.Entry<K, C, X> subLower(K key){
			return absHigher(key);
		}
	}

	// Red-black mechanics
	private static final boolean RED = false;
	private static final boolean BLACK = true;

	/**
	 * Node in the Tree. Doubles as a means to pass key-value pairs back to
	 * user (see Map.Entry).
	 */

	static final class Entry<K, C extends Cloneable & Collection<X>, X> implements Map.Entry<K, C>{
		K key;
		private C value;
		Entry<K, C, X> left;
		Entry<K, C, X> right;
		Entry<K, C, X> parent;
		boolean color = BLACK;
		int size, treeSz = 1;

		/**
		 * Make a new cell with given key, value, and parent, and with
		 * {@code null} child links, and BLACK color.
		 */
		Entry(K key, C value, Entry<K, C, X> parent){
			this.key = key;
			this.value = value;
			this.parent = parent;
			this.size = value.size();
		}

		/**
		 * Returns the key.
		 *
		 * @return the key
		 */
		public K getKey(){
			return key;
		}

		/**
		 * Returns the value associated with the key.
		 *
		 * @return the value associated with the key
		 */
		public C getValue(){
			return value;
		}

		/**
		 * Replaces the value currently associated with the key with the given value.
		 *
		 * @return the value associated with the key before this method was called
		 */
		public C setValue(C value){
			if(this.value == null) return null;
			C oldValue = reflClone(this.value);
			this.value = value;
			return oldValue;
		}

		/**
		 * Adds the given value to the values currently associated with the key
		 * 
		 * @return true if the set changed as a result of the call
		 */
		public boolean addValue(X value){
			return this.value.add(value);
		}

		/**
		 * Adds the given values to the set of values currently associated with the key
		 * 
		 * @return true if the set changed as a result of the call
		 */
		public boolean addValues(Collection<X> values){
			return this.value.addAll(values);
		}

		/**
		 * Removes the given value from the set of values currently associated with the key
		 * 
		 * @return true if the set contained the specified value
		 */
		public boolean removeValue(Object value){
			return this.value.remove(value);
		}

		/**
		 * Removes the given values from the set of values currently associated with the key
		 * 
		 * @return true if the set changed as a result of the call
		 */
		public boolean removeValues(Collection<?> values){
			return this.value.removeAll(values);
		}

		public boolean equals(Object o){
			if(!(o instanceof Map.Entry)) return false;
			Map.Entry<?, ?> e = (Map.Entry<?, ?>)o;

			return valEquals(key, e.getKey()) && valEquals(value, e.getValue());
		}

		public int hashCode(){
			int keyHash = (key == null ? 0 : key.hashCode());
			int valueHash = (value == null ? 0 : value.hashCode());
			return keyHash ^ valueHash;
		}

		public String toString(){
			return key + "=" + value;
		}
	}

	/**
	 * Returns the first Entry in the IndexTreeMap (according to the
	 * IndexTreeMap's
	 * key-sort function). Returns null if the IndexTreeMap is empty.
	 */
	final Entry<K, C, X> getFirstEntry(){
		Entry<K, C, X> p = root;
		if(p != null) while(p.left != null) p = p.left;
		return p;
	}

	/**
	 * Returns the last Entry in the IndexTreeMap (according to the
	 * IndexTreeMap's
	 * key-sort function). Returns null if the IndexTreeMap is empty.
	 */
	final Entry<K, C, X> getLastEntry(){
		Entry<K, C, X> p = root;
		if(p != null) while(p.right != null) p = p.right;
		return p;
	}

	/**
	 * Returns the successor of the specified Entry, or null if no such.
	 */
	static <K, C extends Cloneable & Collection<X>, X> Entry<K, C, X> successor(Entry<K, C, X> t){
		if(t == null) return null;
		else if(t.right != null){
			Entry<K, C, X> p = t.right;
			while(p.left != null) p = p.left;
			return p;
		}
		else{
			Entry<K, C, X> p = t.parent;
			Entry<K, C, X> ch = t;
			while(p != null && ch == p.right){
				ch = p;
				p = p.parent;
			}
			return p;
		}
	}

	/**
	 * Returns the predecessor of the specified Entry, or null if no such.
	 */
	static <K, C extends Cloneable & Collection<X>, X> Entry<K, C, X> predecessor(Entry<K, C, X> t){
		if(t == null) return null;
		else if(t.left != null){
			Entry<K, C, X> p = t.left;
			while(p.right != null) p = p.right;
			return p;
		}
		else{
			Entry<K, C, X> p = t.parent;
			Entry<K, C, X> ch = t;
			while(p != null && ch == p.left){
				ch = p;
				p = p.parent;
			}
			return p;
		}
	}

	/**
	 * Balancing operations.
	 *
	 * Implementations of rebalancings during insertion and deletion are
	 * slightly different than the CLR version. Rather than using dummy
	 * nilnodes, we use a set of accessors that deal properly with null. They
	 * are used to avoid messiness surrounding nullness checks in the main
	 * algorithms.
	 */

	private static <K, C extends Cloneable & Collection<X>, X> boolean colorOf(Entry<K, C, X> p){
		return (p == null ? BLACK : p.color);
	}

	private static <K, C extends Cloneable & Collection<X>, X> Entry<K, C, X> parentOf(Entry<K, C, X> p){
		return (p == null ? null : p.parent);
	}

	private static <K, C extends Cloneable & Collection<X>, X> void setColor(Entry<K, C, X> p, boolean c){
		if(p != null) p.color = c;
	}

	private static <K, C extends Cloneable & Collection<X>, X> Entry<K, C, X> leftOf(Entry<K, C, X> p){
		return (p == null) ? null : p.left;
	}

	private static <K, C extends Cloneable & Collection<X>, X> Entry<K, C, X> rightOf(Entry<K, C, X> p){
		return (p == null) ? null : p.right;
	}

	/** From CLR */
	private void rotateLeft(Entry<K, C, X> p){
		if(p != null){
			Entry<K, C, X> r = p.right;
			p.right = r.left;
			if(r.left != null) r.left.parent = p;
			r.parent = p.parent;
			if(p.parent == null) root = r;
			else if(p.parent.left == p) p.parent.left = r;
			else p.parent.right = r;
			r.left = p;
			p.parent = r;
			// Tree size updates
			p.size -= r.value.size(); --p.treeSz;
			if(r.right != null){p.size -= r.right.size; p.treeSz -= r.right.treeSz;}
			r.size += p.value.size(); ++r.treeSz;
			if(p.left != null){r.size += p.left.size; r.treeSz += p.left.treeSz;}
		}
	}

	/** From CLR */
	private void rotateRight(Entry<K, C, X> p){
		if(p != null){
			Entry<K, C, X> l = p.left;
			p.left = l.right;
			if(l.right != null) l.right.parent = p;
			l.parent = p.parent;
			if(p.parent == null) root = l;
			else if(p.parent.right == p) p.parent.right = l;
			else p.parent.left = l;
			l.right = p;
			p.parent = l;
			// Tree size updates
			p.size -= l.value.size(); --p.treeSz;
			if(l.left != null){p.size -= l.left.size; p.treeSz -= l.left.treeSz;}
			l.size += p.value.size(); ++l.treeSz;
			if(p.right != null){l.size += p.right.size; l.treeSz += p.right.treeSz;}
		}
	}

	/** From CLR */
	private void fixAfterInsertion(Entry<K, C, X> x){
		x.color = RED;

		while(x != null && x != root && x.parent.color == RED){
			if(parentOf(x) == leftOf(parentOf(parentOf(x)))){
				Entry<K, C, X> y = rightOf(parentOf(parentOf(x)));
				if(colorOf(y) == RED){
					setColor(parentOf(x), BLACK);
					setColor(y, BLACK);
					setColor(parentOf(parentOf(x)), RED);
					x = parentOf(parentOf(x));
				}
				else{
					if(x == rightOf(parentOf(x))){
						x = parentOf(x);
						rotateLeft(x);
					}
					setColor(parentOf(x), BLACK);
					setColor(parentOf(parentOf(x)), RED);
					rotateRight(parentOf(parentOf(x)));
				}
			}
			else{
				Entry<K, C, X> y = leftOf(parentOf(parentOf(x)));
				if(colorOf(y) == RED){
					setColor(parentOf(x), BLACK);
					setColor(y, BLACK);
					setColor(parentOf(parentOf(x)), RED);
					x = parentOf(parentOf(x));
				}
				else{
					if(x == leftOf(parentOf(x))){
						x = parentOf(x);
						rotateRight(x);
					}
					setColor(parentOf(x), BLACK);
					setColor(parentOf(parentOf(x)), RED);
					rotateLeft(parentOf(parentOf(x)));
				}
			}
		}
		root.color = BLACK;
	}

	/**
	 * Delete node p, and then rebalance the tree.
	 */
	private void deleteEntry(Entry<K, C, X> p){
		++modCount;
		--size;

		Entry<K, C, X> parent = p.parent;
		while(parent != null){
			--parent.treeSz;
			parent.size -= p.value.size();
			parent = parent.parent;
		}

		// If strictly internal, copy successor's element to p and then make p point to successor.
		if(p.left != null && p.right != null){
			Entry<K, C, X> s = successor(p);
			p.key = s.key;
			p.value = s.value;
			p = s;
		} // p has 2 children

		// Start fixup at replacement node, if it exists.
		Entry<K, C, X> replacement = (p.left != null ? p.left : p.right);

		if(replacement != null){
			// Link replacement to parent
			replacement.parent = p.parent;
			if(p.parent == null) root = replacement;
			else if(p == p.parent.left) p.parent.left = replacement;
			else p.parent.right = replacement;

			// Null out links so they are OK to use by fixAfterDeletion.
			p.left = p.right = p.parent = null;

			// Fix replacement
			if(p.color == BLACK) fixAfterDeletion(replacement);
		}
		else if(p.parent == null){ // return if we are the only node.
			root = null;
		}
		else{ // No children. Use self as phantom replacement and unlink.
			if(p.color == BLACK) fixAfterDeletion(p);

			if(p.parent != null){
				if(p == p.parent.left) p.parent.left = null;
				else if(p == p.parent.right) p.parent.right = null;
				p.parent = null;
			}
		}
	}

	/** From CLR */
	private void fixAfterDeletion(Entry<K, C, X> x){
		while(x != root && colorOf(x) == BLACK){
			if(x == leftOf(parentOf(x))){
				Entry<K, C, X> sib = rightOf(parentOf(x));

				if(colorOf(sib) == RED){
					setColor(sib, BLACK);
					setColor(parentOf(x), RED);
					rotateLeft(parentOf(x));
					sib = rightOf(parentOf(x));
				}

				if(colorOf(leftOf(sib)) == BLACK && colorOf(rightOf(sib)) == BLACK){
					setColor(sib, RED);
					x = parentOf(x);
				}
				else{
					if(colorOf(rightOf(sib)) == BLACK){
						setColor(leftOf(sib), BLACK);
						setColor(sib, RED);
						rotateRight(sib);
						sib = rightOf(parentOf(x));
					}
					setColor(sib, colorOf(parentOf(x)));
					setColor(parentOf(x), BLACK);
					setColor(rightOf(sib), BLACK);
					rotateLeft(parentOf(x));
					x = root;
				}
			}
			else{ // symmetric
				Entry<K, C, X> sib = leftOf(parentOf(x));

				if(colorOf(sib) == RED){
					setColor(sib, BLACK);
					setColor(parentOf(x), RED);
					rotateRight(parentOf(x));
					sib = leftOf(parentOf(x));
				}

				if(colorOf(rightOf(sib)) == BLACK && colorOf(leftOf(sib)) == BLACK){
					setColor(sib, RED);
					x = parentOf(x);
				}
				else{
					if(colorOf(leftOf(sib)) == BLACK){
						setColor(rightOf(sib), BLACK);
						setColor(sib, RED);
						rotateLeft(sib);
						sib = leftOf(parentOf(x));
					}
					setColor(sib, colorOf(parentOf(x)));
					setColor(parentOf(x), BLACK);
					setColor(leftOf(sib), BLACK);
					rotateRight(parentOf(x));
					x = root;
				}
			}
		}

		setColor(x, BLACK);
	}

	private static final long serialVersionUID = 919286545866124777L;

	/**
	 * Save the state of the {@code IndexTreeMap} instance to a stream (i.e.,
	 * serialize it).
	 *
	 * @serialData The <em>size</em> of the IndexTreeMap (the number of
	 *             key-value
	 *             mappings) is emitted (int), followed by the key (Object)
	 *             and value (Object) for each key-value mapping represented
	 *             by the IndexTreeMap. The key-value mappings are emitted in
	 *             key-order (as determined by the IndexTreeMap's Comparator,
	 *             or by the keys' natural ordering if the IndexTreeMap has no
	 *             Comparator).
	 */
	private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException{
		// Write out the Comparator and any hidden stuff
		s.defaultWriteObject();

		// Write out size (number of Mappings)
		s.writeInt(size);

		// Write out keys and values (alternating)
		for(Iterator<Map.Entry<K, C>> i = entrySet().iterator(); i.hasNext();){
			Map.Entry<K, C> e = i.next();
			s.writeObject(e.getKey());
			s.writeObject(e.getValue());
		}
	}

	/**
	 * Reconstitute the {@code IndexTreeMap} instance from a stream (i.e.,
	 * deserialize it).
	 */
	private void readObject(final java.io.ObjectInputStream s)
			throws java.io.IOException,ClassNotFoundException{
		// Read in the Comparator and any hidden stuff
		s.defaultReadObject();

		// Read in size
		int size = s.readInt();

		buildFromSorted(size, null, s, null);
	}

	/** Intended to be called only from TreeSet.readObject */
	void readTreeSet(int size, java.io.ObjectInputStream s, C defaultVal)
			throws java.io.IOException, ClassNotFoundException{
		buildFromSorted(size, null, s, defaultVal);
	}

	/** Intended to be called only from TreeSet.addAll */
	void addAllForTreeSet(SortedSet<? extends K> set, C defaultVal){
		try{
			buildFromSorted(set.size(), set.iterator(), null, defaultVal);
		}
		catch(java.io.IOException cannotHappen){}
		catch(ClassNotFoundException cannotHappen){}
	}

	/**
	 * Linear time tree building algorithm from sorted data. Can accept keys
	 * and/or values from iterator or stream. This leads to too many
	 * parameters, but seems better than alternatives. The four formats
	 * that this method accepts are:
	 *
	 * 1) An iterator of Map.Entries. (it != null, defaultVal == null).
	 * 2) An iterator of keys. (it != null, defaultVal != null).
	 * 3) A stream of alternating serialized keys and values.
	 * (it == null, defaultVal == null).
	 * 4) A stream of serialized keys. (it == null, defaultVal != null).
	 *
	 * It is assumed that the comparator of the IndexTreeMap is already set
	 * prior
	 * to calling this method.
	 *
	 * @param size
	 *            the number of keys (or key-value pairs) to be read from
	 *            the iterator or stream
	 * @param it
	 *            If non-null, new entries are created from entries
	 *            or keys read from this iterator.
	 * @param str
	 *            If non-null, new entries are created from keys and
	 *            possibly values read from this stream in serialized form.
	 *            Exactly one of it and str should be non-null.
	 * @param defaultVal
	 *            if non-null, this default value is used for
	 *            each value in the map. If null, each value is read from
	 *            iterator or stream, as described above.
	 * @throws java.io.IOException
	 *             propagated from stream reads. This cannot
	 *             occur if str is null.
	 * @throws ClassNotFoundException
	 *             propagated from readObject.
	 *             This cannot occur if str is null.
	 */
	private void buildFromSorted(int size, Iterator<?> it, java.io.ObjectInputStream str, C defaultVal)
			throws java.io.IOException, ClassNotFoundException{
		this.size = size;
		root = buildFromSorted(0, 0, size - 1, computeRedLevel(size), it, str, defaultVal);
	}

	/**
	 * Recursive "helper method" that does the real work of the
	 * previous method. Identically named parameters have
	 * identical definitions. Additional parameters are documented below.
	 * It is assumed that the comparator and size fields of the IndexTreeMap are
	 * already set prior to calling this method. (It ignores both fields.)
	 *
	 * @param level the current level of tree. Initial call should be 0.
	 * @param lo the first element index of this subtree. Initial should be 0.
	 * @param hi the last element index of this subtree. Initial should be size-1.
	 * @param redLevel the level at which nodes should be red.
	 *                 Must be equal to computeRedLevel for tree of this size.
	 */
	@SuppressWarnings("unchecked")
	private final Entry<K, C, X> buildFromSorted(int level, int lo, int hi,
			int redLevel, Iterator<?> it,
			java.io.ObjectInputStream str, C defaultVal) throws java.io.IOException, ClassNotFoundException{
		/*
		 * Strategy: The root is the middlemost element. To get to it, we
		 * have to first recursively construct the entire left subtree,
		 * so as to grab all of its elements. We can then proceed with right
		 * subtree.
		 *
		 * The lo and hi arguments are the minimum and maximum
		 * indices to pull out of the iterator or stream for current subtree.
		 * They are not actually indexed, we just proceed sequentially,
		 * ensuring that items are extracted in corresponding order.
		 */

		if(hi < lo) return null;

		int mid = (lo + hi) >>> 1;

		Entry<K, C, X> left = null;
		if(lo < mid) left = buildFromSorted(level + 1, lo, mid - 1, redLevel, it, str, defaultVal);

		// extract key and/or value from iterator or stream
		K key;
		C value;
		if(it != null){
			if(defaultVal == null){
				Map.Entry<?, ?> entry = (Map.Entry<?, ?>)it.next();
				key = (K)entry.getKey();
				value = (C)entry.getValue();
			}
			else{
				key = (K)it.next();
				value = defaultVal;
			}
		}
		else{ // use stream
			key = (K)str.readObject();
			value = (defaultVal != null ? defaultVal : (C)str.readObject());
		}

		Entry<K, C, X> middle = new Entry<>(key, value, null);

		// color nodes in non-full bottommost level red
		if(level == redLevel) middle.color = RED;

		if(left != null){
			middle.left = left;
			left.parent = middle;
			middle.size += left.size;
		}

		if(mid < hi){
			Entry<K, C, X> right = buildFromSorted(level + 1, mid + 1, hi, redLevel, it, str, defaultVal);
			middle.right = right;
			right.parent = middle;
			middle.size += right.size;
		}

		return middle;
	}

	/**
	 * Find the level down to which to assign all nodes BLACK. This is the
	 * last `full' level of the complete binary tree produced by
	 * buildTree. The remaining nodes are colored RED. (This makes a `nice'
	 * set of color assignments wrt future insertions.) This level number is
	 * computed by finding the number of splits needed to reach the zeroeth
	 * node. (The answer is ~lg(N), but in any case must be computed by same
	 * quick O(lg(N)) loop.)
	 */
	private static int computeRedLevel(int sz){
		int level = 0;
		for(int m = sz - 1; m >= 0; m = m / 2 - 1) ++level;
		return level;
	}

	/**
	 * Currently, we support Spliterator-based versions only for the
	 * full map, in either plain of descending form, otherwise relying
	 * on defaults because size estimation for submaps would dominate
	 * costs. The type tests needed to check these for key views are
	 * not very nice but avoid disrupting existing class
	 * structures. Callers must use plain default spliterators if this
	 * returns null.
	 */
	static <K, C extends Cloneable & Collection<X>, X> Spliterator<K> keySpliteratorFor(
			NavigableMap<K, ? extends C> m){
		if(m instanceof IndexTreeMultiMap2){
			@SuppressWarnings("unchecked")
			IndexTreeMultiMap2<K, C, X> t = (IndexTreeMultiMap2<K, C, X>)m;
			return t.keySpliterator();
		}
		if(m instanceof DescendingSubMap){
			@SuppressWarnings("unchecked")
			DescendingSubMap<K, C, X> dm = (DescendingSubMap<K, C, X>)m;
			IndexTreeMultiMap2<K, C, X> tm = dm.m;
			if(dm == tm.descendingMap){
				IndexTreeMultiMap2<K, C, X> t = (IndexTreeMultiMap2<K, C, X>)tm;
				return t.descendingKeySpliterator();
			}
		}
		@SuppressWarnings("unchecked")
		NavigableSubMap<K, C, X> sm = (NavigableSubMap<K, C, X>)m;
		return sm.keySpliterator();
	}

	final Spliterator<K> keySpliterator(){
		return new KeySpliterator<K, C, X>(this, null, null, 0, -1, 0);
	}

	final Spliterator<K> descendingKeySpliterator(){
		return new DescendingKeySpliterator<K, C, X>(this, null, null, 0, -2, 0);
	}

	/**
	 * Base class for spliterators. Iteration starts at a given
	 * origin and continues up to but not including a given fence (or
	 * null for end). At top-level, for ascending cases, the first
	 * split uses the root as left-fence/right-origin. From there,
	 * right-hand splits replace the current fence with its left
	 * child, also serving as origin for the split-off spliterator.
	 * Left-hands are symmetric. Descending versions place the origin
	 * at the end and invert ascending split rules. This base class
	 * is non-commital about directionality, or whether the top-level
	 * spliterator covers the whole tree. This means that the actual
	 * split mechanics are located in subclasses. Some of the subclass
	 * trySplit methods are identical (except for return types), but
	 * not nicely factorable.
	 *
	 * Currently, subclass versions exist only for the full map
	 * (including descending keys via its descendingMap). Others are
	 * possible but currently not worthwhile because submaps require
	 * O(n) computations to determine size, which substantially limits
	 * potential speed-ups of using custom Spliterators versus default
	 * mechanics.
	 *
	 * To boostrap initialization, external constructors use
	 * negative size estimates: -1 for ascend, -2 for descend.
	 */
	static class IndexTreeMapSpliterator<K, C extends Cloneable & Collection<X>, X>{
		final IndexTreeMultiMap2<K, C, X> tree;
		IndexTreeMultiMap2.Entry<K, C, X> current; // traverser; initially first node in range
		IndexTreeMultiMap2.Entry<K, C, X> fence; // one past last, or null
		int side; // 0: top, -1: is a left split, +1: right
		int est; // size estimate (exact only for top-level)
		int expectedModCount; // for CME checks

		IndexTreeMapSpliterator(IndexTreeMultiMap2<K, C, X> tree, IndexTreeMultiMap2.Entry<K, C, X> origin,
				IndexTreeMultiMap2.Entry<K, C, X> fence, int side, int est,
				int expectedModCount){
			this.tree = tree;
			this.current = origin;
			this.fence = fence;
			this.side = side;
			this.est = est;
			this.expectedModCount = expectedModCount;
		}

		final int getEstimate(){ // force initialization
			int s;
			IndexTreeMultiMap2<K, C, X> t;
			if((s = est) < 0){
				if((t = tree) != null){
					current = (s == -1) ? t.getFirstEntry() : t.getLastEntry();
					s = est = t.size;
					expectedModCount = t.modCount;
				}
				else s = est = 0;
			}
			return s;
		}

		public final long estimateSize(){
			return (long)getEstimate();
		}
	}

	static final class KeySpliterator<K, C extends Cloneable & Collection<X>, X>
	extends IndexTreeMapSpliterator<K, C, X> implements Spliterator<K>{
		KeySpliterator(IndexTreeMultiMap2<K, C, X> tree, IndexTreeMultiMap2.Entry<K, C, X> origin,
				IndexTreeMultiMap2.Entry<K, C, X> fence, int side, int est, int expectedModCount){
			super(tree, origin, fence, side, est, expectedModCount);
		}

		public KeySpliterator<K, C, X> trySplit(){
			if(est < 0) getEstimate(); // force initialization
			int d = side;
			IndexTreeMultiMap2.Entry<K, C, X> e = current, f = fence,
					s = ((e == null || e == f) ? null : // empty
						(d == 0) ? tree.root : // was top
						(d > 0) ? e.right : // was right
						(d < 0 && f != null) ? f.left : // was left
						null);
			if(s != null && s != e && s != f && tree.compare(e.key, s.key) < 0){ // e not already past s
				side = 1;
				return new KeySpliterator<>(tree, e, current = s, -1, est >>>= 1, expectedModCount);
			}
			return null;
		}

		public void forEachRemaining(Consumer<? super K> action){
			if(action == null) throw new NullPointerException();
			if(est < 0) getEstimate(); // force initialization
			IndexTreeMultiMap2.Entry<K, C, X> f = fence, e, p, pl;
			if((e = current) != null && e != f){
				current = f; // exhaust
				do{
					action.accept(e.key);
					if((p = e.right) != null){
						while((pl = p.left) != null) p = pl;
					}
					else{
						while((p = e.parent) != null && e == p.right) e = p;
					}
				}
				while((e = p) != null && e != f);
				if(tree.modCount != expectedModCount) throw new ConcurrentModificationException();
			}
		}

		public boolean tryAdvance(Consumer<? super K> action){
			IndexTreeMultiMap2.Entry<K, C, X> e;
			if(action == null) throw new NullPointerException();
			if(est < 0) getEstimate(); // force initialization
			if((e = current) == null || e == fence) return false;
			current = successor(e);
			action.accept(e.key);
			if(tree.modCount != expectedModCount) throw new ConcurrentModificationException();
			return true;
		}

		public int characteristics(){
			return (side == 0 ? Spliterator.SIZED : 0) | Spliterator.DISTINCT | Spliterator.SORTED |
					Spliterator.ORDERED;
		}

		public final Comparator<? super K> getComparator(){
			return tree.comparator;
		}

	}

	static final class DescendingKeySpliterator<K, C extends Cloneable & Collection<X>, X>
	extends IndexTreeMapSpliterator<K, C, X> implements Spliterator<K>{
		DescendingKeySpliterator(IndexTreeMultiMap2<K, C, X> tree, IndexTreeMultiMap2.Entry<K, C, X> origin,
				IndexTreeMultiMap2.Entry<K, C, X> fence, int side, int est, int expectedModCount){
			super(tree, origin, fence, side, est, expectedModCount);
		}

		public DescendingKeySpliterator<K, C, X> trySplit(){
			if(est < 0) getEstimate(); // force initialization
			int d = side;
			IndexTreeMultiMap2.Entry<K, C, X> e = current, f = fence,
					s = ((e == null || e == f) ? null : // empty
						(d == 0) ? tree.root : // was top
						(d < 0) ? e.left : // was left
						(d > 0 && f != null) ? f.right : // was right
						null);
			if(s != null && s != e && s != f && tree.compare(e.key, s.key) > 0){ // e not already past s
				side = 1;
				return new DescendingKeySpliterator<>(tree, e, current = s, -1, est >>>= 1, expectedModCount);
			}
			return null;
		}

		public void forEachRemaining(Consumer<? super K> action){
			if(action == null) throw new NullPointerException();
			if(est < 0) getEstimate(); // force initialization
			IndexTreeMultiMap2.Entry<K, C, X> f = fence, e, p, pr;
			if((e = current) != null && e != f){
				current = f; // exhaust
				do{
					action.accept(e.key);
					if((p = e.left) != null){
						while((pr = p.right) != null) p = pr;
					}
					else{
						while((p = e.parent) != null && e == p.left) e = p;
					}
				}
				while((e = p) != null && e != f);
				if(tree.modCount != expectedModCount) throw new ConcurrentModificationException();
			}
		}

		public boolean tryAdvance(Consumer<? super K> action){
			IndexTreeMultiMap2.Entry<K, C, X> e;
			if(action == null) throw new NullPointerException();
			if(est < 0) getEstimate(); // force initialization
			if((e = current) == null || e == fence) return false;
			current = predecessor(e);
			action.accept(e.key);
			if(tree.modCount != expectedModCount) throw new ConcurrentModificationException();
			return true;
		}

		public int characteristics(){
			return (side == 0 ? Spliterator.SIZED : 0) | Spliterator.DISTINCT | Spliterator.ORDERED;
		}
	}

	static final class ValueSpliterator<K, C extends Cloneable & Collection<X>, X>
	extends IndexTreeMapSpliterator<K, C, X> implements Spliterator<C>{
		ValueSpliterator(IndexTreeMultiMap2<K, C, X> tree, IndexTreeMultiMap2.Entry<K, C, X> origin,
				IndexTreeMultiMap2.Entry<K, C, X> fence, int side, int est, int expectedModCount){
			super(tree, origin, fence, side, est, expectedModCount);
		}

		public ValueSpliterator<K, C, X> trySplit(){
			if(est < 0) getEstimate(); // force initialization
			int d = side;
			IndexTreeMultiMap2.Entry<K, C, X> e = current, f = fence,
					s = ((e == null || e == f) ? null : // empty
						(d == 0) ? tree.root : // was top
						(d > 0) ? e.right : // was right
						(d < 0 && f != null) ? f.left : // was left
						null);
			if(s != null && s != e && s != f && tree.compare(e.key, s.key) < 0){ // e not already past s
				side = 1;
				return new ValueSpliterator<>(tree, e, current = s, -1, est >>>= 1, expectedModCount);
			}
			return null;
		}

		public void forEachRemaining(Consumer<? super C> action){
			if(action == null) throw new NullPointerException();
			if(est < 0) getEstimate(); // force initialization
			IndexTreeMultiMap2.Entry<K, C, X> f = fence, e, p, pl;
			if((e = current) != null && e != f){
				current = f; // exhaust
				do{
					action.accept(e.value);
					if((p = e.right) != null){
						while((pl = p.left) != null) p = pl;
					}
					else{
						while((p = e.parent) != null && e == p.right) e = p;
					}
				}
				while((e = p) != null && e != f);
				if(tree.modCount != expectedModCount) throw new ConcurrentModificationException();
			}
		}

		public boolean tryAdvance(Consumer<? super C> action){
			IndexTreeMultiMap2.Entry<K, C, X> e;
			if(action == null) throw new NullPointerException();
			if(est < 0) getEstimate(); // force initialization
			if((e = current) == null || e == fence) return false;
			current = successor(e);
			action.accept(e.value);
			if(tree.modCount != expectedModCount) throw new ConcurrentModificationException();
			return true;
		}

		public int characteristics(){
			return (side == 0 ? Spliterator.SIZED : 0) | Spliterator.ORDERED;
		}
	}

	static final class EntrySpliterator<K, C extends Cloneable & Collection<X>, X>
	extends IndexTreeMapSpliterator<K, C, X> implements Spliterator<Map.Entry<K, C>>{
		EntrySpliterator(IndexTreeMultiMap2<K, C, X> tree, IndexTreeMultiMap2.Entry<K, C, X> origin,
				IndexTreeMultiMap2.Entry<K, C, X> fence, int side, int est, int expectedModCount){
			super(tree, origin, fence, side, est, expectedModCount);
		}

		public EntrySpliterator<K, C, X> trySplit(){
			if(est < 0) getEstimate(); // force initialization
			int d = side;
			IndexTreeMultiMap2.Entry<K, C, X> e = current, f = fence,
					s = ((e == null || e == f) ? null : // empty
						(d == 0) ? tree.root : // was top
						(d > 0) ? e.right : // was right
						(d < 0 && f != null) ? f.left : // was left
						null);
			if(s != null && s != e && s != f && tree.compare(e.key, s.key) < 0){ // e not already past s
				side = 1;
				return new EntrySpliterator<>(tree, e, current = s, -1, est >>>= 1, expectedModCount);
			}
			return null;
		}

		public void forEachRemaining(Consumer<? super Map.Entry<K, C>> action){
			if(action == null) throw new NullPointerException();
			if(est < 0) getEstimate(); // force initialization
			IndexTreeMultiMap2.Entry<K, C, X> f = fence, e, p, pl;
			if((e = current) != null && e != f){
				current = f; // exhaust
				do{
					action.accept(e);
					if((p = e.right) != null){
						while((pl = p.left) != null) p = pl;
					}
					else{
						while((p = e.parent) != null && e == p.right) e = p;
					}
				}
				while((e = p) != null && e != f);
				if(tree.modCount != expectedModCount) throw new ConcurrentModificationException();
			}
		}

		public boolean tryAdvance(Consumer<? super Map.Entry<K, C>> action){
			IndexTreeMultiMap2.Entry<K, C, X> e;
			if(action == null) throw new NullPointerException();
			if(est < 0) getEstimate(); // force initialization
			if((e = current) == null || e == fence) return false;
			current = successor(e);
			action.accept(e);
			if(tree.modCount != expectedModCount) throw new ConcurrentModificationException();
			return true;
		}

		public int characteristics(){
			return (side == 0 ? Spliterator.SIZED : 0) | Spliterator.DISTINCT | Spliterator.SORTED |
					Spliterator.ORDERED;
		}

		@Override public Comparator<Map.Entry<K, C>> getComparator(){
			// Adapt or create a key-based comparator
			if(tree.comparator != null){
				return Map.Entry.comparingByKey(tree.comparator);
			}
			else{
				return (Comparator<Map.Entry<K, C>> & Serializable)(e1, e2) -> {
					@SuppressWarnings("unchecked")
					Comparable<? super K> k1 = (Comparable<? super K>)e1.getKey();
					return k1.compareTo(e2.getKey());
				};
			}
		}
	}
}
