package nl.knaw.huygens.alexandria.client.model;

/*
 * #%L
 * alexandria-java-client
 * =======
 * Copyright (C) 2015 - 2017 Huygens ING (KNAW)
 * =======
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;

import nl.knaw.huygens.alexandria.api.JsonTypeNames;
import nl.knaw.huygens.alexandria.api.model.JsonWrapperObject;

@JsonTypeName(JsonTypeNames.ANNOTATIONLIST)
public class AnnotationList extends JsonWrapperObject implements List<AnnotationPojo> {
  List<AnnotationPojo> delegate = Lists.newArrayList();

  // delegated methods

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return delegate.contains(o);
  }

  @Override
  public Iterator<AnnotationPojo> iterator() {
    return delegate.iterator();
  }

  @Override
  public Object[] toArray() {
    return delegate.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return delegate.toArray(a);
  }

  @Override
  public boolean add(AnnotationPojo e) {
    return delegate.add(e);
  }

  @Override
  public boolean remove(Object o) {
    return delegate.remove(o);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return delegate.containsAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends AnnotationPojo> c) {
    return delegate.addAll(c);
  }

  @Override
  public boolean addAll(int index, Collection<? extends AnnotationPojo> c) {
    return delegate.addAll(index, c);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    return delegate.removeAll(c);
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return delegate.retainAll(c);
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  @Override
  public boolean equals(Object o) {
    return delegate.equals(o);
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  @Override
  public AnnotationPojo get(int index) {
    return delegate.get(index);
  }

  @Override
  public AnnotationPojo set(int index, AnnotationPojo element) {
    return delegate.set(index, element);
  }

  @Override
  public void add(int index, AnnotationPojo element) {
    delegate.add(index, element);
  }

  @Override
  public AnnotationPojo remove(int index) {
    return delegate.remove(index);
  }

  @Override
  public int indexOf(Object o) {
    return delegate.indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    return delegate.lastIndexOf(o);
  }

  @Override
  public ListIterator<AnnotationPojo> listIterator() {
    return delegate.listIterator();
  }

  @Override
  public ListIterator<AnnotationPojo> listIterator(int index) {
    return delegate.listIterator(index);
  }

  @Override
  public List<AnnotationPojo> subList(int fromIndex, int toIndex) {
    return delegate.subList(fromIndex, toIndex);
  }

}
