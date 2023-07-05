package com.google.zetasql.toolkit.antipattern.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BatchIterator<T> implements Iterator<List<T>> {

  private final Iterator<T> sourceIterator;
  private final int batchSize;

  public BatchIterator(Iterator<T> sourceIterator, int batchSize) {
    this.sourceIterator = sourceIterator;
    this.batchSize = batchSize;
  }

  @Override
  public boolean hasNext() {
    return sourceIterator.hasNext();
  }

  @Override
  public List<T> next() {
    ArrayList<T> result = new ArrayList<>();

    while (sourceIterator.hasNext() && result.size() <= batchSize) {
      result.add(sourceIterator.next());
    }

    return result;
  }
}
