package com.arubanetworks.meridiansamples.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

public class Lists {

    private Lists() {
    }

    public static <E> ArrayList<E> newArrayList() {
        return new ArrayList<>();
    }

    public interface IPredicate<T> {
        boolean apply(T type);
    }

    public static <T> List<T> filter(List<T> target, IPredicate<T> predicate) {
        List<T> result = new ArrayList<>();
        for (T element : target) {
            if (predicate.apply(element)) {
                result.add(element);
            }
        }
        return result;
    }

    /**
     * Returns a reversed view of the specified list. For example, {@code
     * Lists.reverse(Arrays.asList(1, 2, 3))} returns a list containing {@code 3,
     * 2, 1}. The returned list is backed by this list, so changes in the returned
     * list are reflected in this list, and vice-versa. The returned list supports
     * all of the optional list operations supported by this list.
     * <p/>
     * <p>The returned list is random-access if the specified list is random
     * access.
     */
    private static <T> List<T> reverse(List<T> list) {
        if (list instanceof ReverseList) {
            return ((ReverseList<T>) list).getForwardList();
        } else {
            return new ReverseList<>(list);
        }
    }

    private static class ReverseList<T> extends AbstractList<T> {
        private final List<T> forwardList;

        ReverseList(List<T> forwardList) {
            this.forwardList = forwardList;
        }

        List<T> getForwardList() {
            return forwardList;
        }

        private int reverseIndex(int index) {
            int size = size();
            return (size - 1) - index;
        }

        private int reversePosition(int index) {
            int size = size();
            return size - index;
        }

        @Override
        public void add(int index, @Nullable T element) {
            forwardList.add(reversePosition(index), element);
        }

        @Override
        public void clear() {
            forwardList.clear();
        }

        @Override
        public T remove(int index) {
            return forwardList.remove(reverseIndex(index));
        }

        @Override
        protected void removeRange(int fromIndex, int toIndex) {
            subList(fromIndex, toIndex).clear();
        }

        @Override
        public T set(int index, @Nullable T element) {
            return forwardList.set(reverseIndex(index), element);
        }

        @Override
        public T get(int index) {
            return forwardList.get(reverseIndex(index));
        }

        @Override
        public boolean isEmpty() {
            return forwardList.isEmpty();
        }

        @Override
        public int size() {
            return forwardList.size();
        }

        @Override
        public boolean contains(@Nullable Object o) {
            return forwardList.contains(o);
        }

        @Override
        public boolean containsAll(@NonNull Collection<?> c) {
            return forwardList.containsAll(c);
        }

        @NonNull
        @Override
        public List<T> subList(int fromIndex, int toIndex) {
            return reverse(forwardList.subList(
                    reversePosition(toIndex), reversePosition(fromIndex)));
        }

        @Override
        public int indexOf(@Nullable Object o) {
            int index = forwardList.lastIndexOf(o);
            return (index >= 0) ? reverseIndex(index) : -1;
        }

        @Override
        public int lastIndexOf(@Nullable Object o) {
            int index = forwardList.indexOf(o);
            return (index >= 0) ? reverseIndex(index) : -1;
        }

        @NonNull
        @Override
        public Iterator<T> iterator() {
            return listIterator();
        }

        @NonNull
        @Override
        public ListIterator<T> listIterator(int index) {
            int start = reversePosition(index);
            final ListIterator<T> forwardIterator = forwardList.listIterator(start);
            return new ListIterator<T>() {

                boolean canRemove;
                boolean canSet;

                @Override
                public void add(T e) {
                    forwardIterator.add(e);
                    forwardIterator.previous();
                    canSet = canRemove = false;
                }

                @Override
                public boolean hasNext() {
                    return forwardIterator.hasPrevious();
                }

                @Override
                public boolean hasPrevious() {
                    return forwardIterator.hasNext();
                }

                @Override
                public T next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    canSet = canRemove = true;
                    return forwardIterator.previous();
                }

                @Override
                public int nextIndex() {
                    return reversePosition(forwardIterator.nextIndex());
                }

                @Override
                public T previous() {
                    if (!hasPrevious()) {
                        throw new NoSuchElementException();
                    }
                    canSet = canRemove = true;
                    return forwardIterator.next();
                }

                @Override
                public int previousIndex() {
                    return nextIndex() - 1;
                }

                @Override
                public void remove() {
                    forwardIterator.remove();
                    canRemove = canSet = false;
                }

                @Override
                public void set(T e) {
                    forwardIterator.set(e);
                }
            };
        }
    }
}
