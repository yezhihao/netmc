package io.github.yezhihao.netmc.util;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * @author yezhihao
 * home https://gitee.com/yezhihao/jt808-server
 */
public final class AdapterMap<K, S, T> extends AbstractMap<K, T> {

    private final Map<K, S> src;
    private final Set<Entry<K, T>> entries;

    public AdapterMap(Map<K, S> src, Function<S, T> function) {
        this.src = src;
        this.entries = new AdapterSet(src.entrySet(), (Function<Entry<K, S>, Entry<K, T>>) e -> new SimpleEntry(e.getKey(), function.apply(e.getValue())));
    }

    @Override
    public Set<Entry<K, T>> entrySet() {
        return entries;
    }

    @Override
    public int size() {
        return src.size();
    }
}