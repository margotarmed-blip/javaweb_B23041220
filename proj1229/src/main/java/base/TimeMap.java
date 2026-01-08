package base;

import java.util.HashMap;
import java.util.LinkedList;

public class TimeMap<K, V> {
    private static class Data<K, V> {
        public K key;
        public V value;
        public Long time;

        public Data(K key, V value, Long time) {
            this.key = key;
            this.value = value;
            this.time = time;
        }
    }

    public TimeMap(long expiration) {
        if (expiration <= 0) throw new RuntimeException("expiration must > 0");
        this.expiration = expiration;
    }

    public synchronized void setExpiration(long value) {
        if (value <= 0) throw new RuntimeException("expiration must > 0");
        this.expiration = value;
        checkNext();
    }

    private final LinkedList<Data<K, V>> cache = new LinkedList<>();
    private final HashMap<K, Data<K, V>> data = new HashMap<>();
    private long expiration = 0;//毫秒

    private void checkNext() {
        while (!cache.isEmpty()) {
            Data<K, V> val = cache.removeFirst();
            if (val.time + expiration < System.currentTimeMillis()) {
                data.remove(val.key);
                continue;
            }
            cache.addLast(val);//必须调换,因为set,get会更新,保证可以轮询到过期的元素
            break;
        }
    }

    public synchronized V get(K key) {//可能返回null
        checkNext();
        Data<K, V> vData = data.get(key);
        if (vData == null) return null;
        vData.time = System.currentTimeMillis();
        return vData.value;
    }

    public synchronized void set(K key, V value) {
        checkNext();
        Data<K, V> val = data.get(key);
        if (val != null) {
            val.value = value;
            val.time = System.currentTimeMillis();
        } else {
            val = new Data<>(key, value, System.currentTimeMillis());
            data.put(key, val);
            cache.addLast(val);
        }
    }
}
