/* 
 * Copyright 2001-2009 Terracotta, Inc. 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not 
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at 
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0 
 *   
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations 
 * under the License.
 * 
 */

package org.quartz.utils;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * An implementation of <code>Map</code> that wraps another <code>Map</code> and flags itself 'dirty' when it is modified.
 * </p>
 * 
 * @author James House
 */
public class DirtyFlagMap implements Map, Cloneable, java.io.Serializable {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Data members. ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */
  private static final long serialVersionUID = 1433884852607126222L;

  private boolean dirty = false;
  private Map map;

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constructors. ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * Create a DirtyFlagMap that 'wraps' a <code>HashMap</code>.
   * </p>
   * 
   * @see java.util.HashMap
   */
  public DirtyFlagMap() {

    map = new HashMap();
  }

  /**
   * <p>
   * Create a DirtyFlagMap that 'wraps' a <code>HashMap</code> that has the given initial capacity.
   * </p>
   * 
   * @see java.util.HashMap
   */
  public DirtyFlagMap(int initialCapacity) {

    map = new HashMap(initialCapacity);
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface. ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * Determine whether the <code>Map</code> is flagged dirty.
   * </p>
   */
  public boolean isDirty() {

    return dirty;
  }

  /**
   * <p>
   * Get a direct handle to the underlying Map.
   * </p>
   */
  public Map getWrappedMap() {

    return map;
  }

  @Override
  public void clear() {

    if (map.isEmpty() == false) {
      dirty = true;
    }

    map.clear();
  }

  @Override
  public boolean containsKey(Object key) {

    return map.containsKey(key);
  }

  @Override
  public boolean containsValue(Object val) {

    return map.containsValue(val);
  }

  @Override
  public Set entrySet() {

    return new DirtyFlagMapEntrySet(map.entrySet());
  }

  @Override
  public boolean equals(Object obj) {

    if (obj == null || !(obj instanceof DirtyFlagMap)) {
      return false;
    }

    return map.equals(((DirtyFlagMap) obj).getWrappedMap());
  }

  @Override
  public int hashCode() {

    return map.hashCode();
  }

  @Override
  public Object get(Object key) {

    return map.get(key);
  }

  @Override
  public boolean isEmpty() {

    return map.isEmpty();
  }

  @Override
  public Set keySet() {

    return new DirtyFlagSet(map.keySet());
  }

  @Override
  public Object put(Object key, Object val) {

    dirty = true;

    return map.put(key, val);
  }

  @Override
  public void putAll(Map t) {

    if (!t.isEmpty()) {
      dirty = true;
    }

    map.putAll(t);
  }

  @Override
  public Object remove(Object key) {

    Object obj = map.remove(key);

    if (obj != null) {
      dirty = true;
    }

    return obj;
  }

  @Override
  public int size() {

    return map.size();
  }

  @Override
  public Collection values() {

    return new DirtyFlagCollection(map.values());
  }

  @Override
  public Object clone() {

    DirtyFlagMap copy;
    try {
      copy = (DirtyFlagMap) super.clone();
      if (map instanceof HashMap) {
        copy.map = (Map) ((HashMap) map).clone();
      }
    } catch (CloneNotSupportedException ex) {
      throw new IncompatibleClassChangeError("Not Cloneable.");
    }

    return copy;
  }

  /**
   * Wrap a Collection so we can mark the DirtyFlagMap as dirty if the underlying Collection is modified.
   */
  private class DirtyFlagCollection implements Collection {

    private Collection collection;

    public DirtyFlagCollection(Collection c) {

      collection = c;
    }

    protected Collection getWrappedCollection() {

      return collection;
    }

    @Override
    public Iterator iterator() {

      return new DirtyFlagIterator(collection.iterator());
    }

    @Override
    public boolean remove(Object o) {

      boolean removed = collection.remove(o);
      if (removed) {
        dirty = true;
      }
      return removed;
    }

    @Override
    public boolean removeAll(Collection c) {

      boolean changed = collection.removeAll(c);
      if (changed) {
        dirty = true;
      }
      return changed;
    }

    @Override
    public boolean retainAll(Collection c) {

      boolean changed = collection.retainAll(c);
      if (changed) {
        dirty = true;
      }
      return changed;
    }

    @Override
    public void clear() {

      if (collection.isEmpty() == false) {
        dirty = true;
      }
      collection.clear();
    }

    // Pure wrapper methods
    @Override
    public int size() {

      return collection.size();
    }

    @Override
    public boolean isEmpty() {

      return collection.isEmpty();
    }

    @Override
    public boolean contains(Object o) {

      return collection.contains(o);
    }

    @Override
    public boolean add(Object o) {

      return collection.add(o);
    } // Not supported

    @Override
    public boolean addAll(Collection c) {

      return collection.addAll(c);
    } // Not supported

    @Override
    public boolean containsAll(Collection c) {

      return collection.containsAll(c);
    }

    @Override
    public Object[] toArray() {

      return collection.toArray();
    }

    @Override
    public Object[] toArray(Object[] array) {

      return collection.toArray(array);
    }
  }

  /**
   * Wrap a Set so we can mark the DirtyFlagMap as dirty if the underlying Collection is modified.
   */
  private class DirtyFlagSet extends DirtyFlagCollection implements Set {

    public DirtyFlagSet(Set set) {

      super(set);
    }

    protected Set getWrappedSet() {

      return (Set) getWrappedCollection();
    }
  }

  /**
   * Wrap an Iterator so that we can mark the DirtyFlagMap as dirty if an element is removed.
   */
  private class DirtyFlagIterator implements Iterator {

    private Iterator iterator;

    public DirtyFlagIterator(Iterator iterator) {

      this.iterator = iterator;
    }

    @Override
    public void remove() {

      dirty = true;
      iterator.remove();
    }

    // Pure wrapper methods
    @Override
    public boolean hasNext() {

      return iterator.hasNext();
    }

    @Override
    public Object next() {

      return iterator.next();
    }
  }

  /**
   * Wrap a Map.Entry Set so we can mark the Map as dirty if the Set is modified, and return Map.Entry objects wrapped in the <code>DirtyFlagMapEntry</code> class.
   */
  private class DirtyFlagMapEntrySet extends DirtyFlagSet {

    public DirtyFlagMapEntrySet(Set set) {

      super(set);
    }

    @Override
    public Iterator iterator() {

      return new DirtyFlagMapEntryIterator(getWrappedSet().iterator());
    }

    @Override
    public Object[] toArray() {

      return toArray(new Object[super.size()]);
    }

    @Override
    public Object[] toArray(Object[] array) {

      if (array.getClass().getComponentType().isAssignableFrom(Map.Entry.class) == false) {
        throw new IllegalArgumentException("Array must be of type assignable from Map.Entry");
      }

      int size = super.size();

      Object[] result = (array.length < size) ? (Object[]) Array.newInstance(array.getClass().getComponentType(), size) : array;

      Iterator entryIter = iterator(); // Will return DirtyFlagMapEntry objects
      for (int i = 0; i < size; i++) {
        result[i] = entryIter.next();
      }

      if (result.length > size) {
        result[size] = null;
      }

      return result;
    }
  }

  /**
   * Wrap an Iterator over Map.Entry objects so that we can mark the Map as dirty if an element is removed or modified.
   */
  private class DirtyFlagMapEntryIterator extends DirtyFlagIterator {

    public DirtyFlagMapEntryIterator(Iterator iterator) {

      super(iterator);
    }

    @Override
    public Object next() {

      return new DirtyFlagMapEntry((Map.Entry) super.next());
    }
  }

  /**
   * Wrap a Map.Entry so we can mark the Map as dirty if a value is set.
   */
  private class DirtyFlagMapEntry implements Map.Entry {

    private Map.Entry entry;

    public DirtyFlagMapEntry(Map.Entry entry) {

      this.entry = entry;
    }

    @Override
    public Object setValue(Object o) {

      dirty = true;
      return entry.setValue(o);
    }

    // Pure wrapper methods
    @Override
    public Object getKey() {

      return entry.getKey();
    }

    @Override
    public Object getValue() {

      return entry.getValue();
    }
  }
}