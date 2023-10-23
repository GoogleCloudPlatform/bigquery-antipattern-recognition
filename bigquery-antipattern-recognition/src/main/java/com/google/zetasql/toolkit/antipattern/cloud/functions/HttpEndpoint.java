package com.google.zetasql.toolkit.antipattern.cloud.functions;

import java.io.BufferedWriter;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;

public class HttpEndpoint implements HttpFunction{
  public void service(final HttpRequest request, final HttpResponse response) throws Exception {
    final BufferedWriter writer = response.getWriter();
    writer.write("Hello world!");
  }
}
