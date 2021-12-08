package io.github.yezhihao.netmc.util;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;

/**
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
public final class AdapterCollection<S, T> extends AbstractCollection<T> {

    private final Collection<S> src;
    private final Iterator<T> iterator;

    public AdapterCollection(Collection<S> src, Function<S, T> function) {
        this.src = src;
        this.iterator = new Iterator<T>() {

            private final Function<S, T> f = function;
            private final Iterator<S> it = src.iterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public T next() {
                return f.apply(it.next());
            }

            @Override
            public void remove() {
                it.remove();
            }
        };
    }

    @Override
    public Iterator<T> iterator() {
        return iterator;
    }

    @Override
    public int size() {
        return src.size();
    }
}