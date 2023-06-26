package com.google.zetasql.toolkit.antipattern;

public enum RecommendationType {

  SelectStar,
  SubqueryWithoutAgg,
  CTEsEvalMultipleTimes,
  OrderByWithoutLimit,
  StringComparison,
  NtileWindowFunction,
  UnnecessaryCrossJoin,
  SingleRowInsert;

}
