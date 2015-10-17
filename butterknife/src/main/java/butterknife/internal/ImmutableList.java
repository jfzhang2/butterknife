package butterknife.internal;

import java.util.AbstractList;
import java.util.RandomAccess;

/**
 * An immutable list of views which is lighter than {@code
 * 一个不可改变的view的列表的集合比Collections.unmodifiableList要轻量级一些
 * Collections.unmodifiableList(new ArrayList<>(Arrays.asList(foo, bar)))}.
 */
//支持快速随机访问   因为没有提供add与set方法  所以说是不可改变的
final class ImmutableList<T> extends AbstractList<T> implements RandomAccess {
  private final T[] views;

  ImmutableList(T[] views) {
    this.views = views;
  }

  @Override public T get(int index) {
    return views[index];
  }

  @Override public int size() {
    return views.length;
  }

  @Override public boolean contains(Object o) {
    for (T view : views) {
      if (view == o) {
        return true;
      }
    }
    return false;
  }
}
