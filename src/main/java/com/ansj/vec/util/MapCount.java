//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.ansj.vec.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

public class MapCount<T> {
    private HashMap<T, Integer> hm = null;

    public MapCount() {
        this.hm = new HashMap();
    }

    public MapCount(int initialCapacity) {
        this.hm = new HashMap(initialCapacity);
    }

    public void add(T t, int n) {
        Integer integer = null;
        if((integer = (Integer)this.hm.get(t)) != null) {
            this.hm.put(t, Integer.valueOf(integer.intValue() + n));
        } else {
            this.hm.put(t, Integer.valueOf(n));
        }

    }

    public void add(T t) {
        this.add(t, 1);
    }

    public int size() {
        return this.hm.size();
    }

    public void remove(T t) {
        this.hm.remove(t);
    }

    public HashMap<T, Integer> get() {
        return this.hm;
    }

    public String getDic() {
        Iterator iterator = this.hm.entrySet().iterator();
        StringBuilder sb = new StringBuilder();
        Entry next = null;

        while(iterator.hasNext()) {
            next = (Entry)iterator.next();
            sb.append(next.getKey());
            sb.append("\t");
            sb.append(next.getValue());
            sb.append("\n");
        }

        return sb.toString();
    }

    public static void main(String[] args) {
        System.out.println(9223372036854775807L);
    }
}
