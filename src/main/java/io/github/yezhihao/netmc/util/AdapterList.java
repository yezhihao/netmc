package io.github.yezhihao.netmc.util;

import java.util.AbstractList;
import java.util.List;
import java.util.function.Function;

/**
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
public final class AdapterList<S, T> extends AbstractList<T> {

    private final List<S> src;
    private final Function<S, T> function;

    public AdapterList(List<S> src, Function<S, T> function) {
        this.src = src;
        this.function = function;
    }

    @Override
    public T get(int index) {
        return function.apply(src.get(index));
    }

    @Override
    public int size() {
        return src.size();
    }
}