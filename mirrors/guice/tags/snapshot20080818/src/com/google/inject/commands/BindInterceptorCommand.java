/**
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.inject.commands;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.inject.matcher.Matcher;
import java.lang.reflect.Method;
import java.util.Arrays;
import static java.util.Collections.unmodifiableList;
import java.util.List;
import org.aopalliance.intercept.MethodInterceptor;

/**
 * Immutable snapshot of a request to bind an interceptor.
 *
 * @deprecated replaced with {@link com.google.inject.spi.InterceptorBinding}
 *
 * @author jessewilson@google.com (Jesse Wilson)
 */
@Deprecated
public final class BindInterceptorCommand implements Command {
  private final Object source;
  private final Matcher<? super Class<?>> classMatcher;
  private final Matcher<? super Method> methodMatcher;
  private final List<MethodInterceptor> interceptors;

  BindInterceptorCommand(
      Object source,
      Matcher<? super Class<?>> classMatcher,
      Matcher<? super Method> methodMatcher,
      MethodInterceptor[] interceptors) {
    this.source = checkNotNull(source, "source");
    this.classMatcher = checkNotNull(classMatcher, "classMatcher");
    this.methodMatcher = checkNotNull(methodMatcher, "methodMatcher");
    this.interceptors = unmodifiableList(Arrays.asList(interceptors.clone()));
  }

  public Object getSource() {
    return source;
  }

  public Matcher<? super Class<?>> getClassMatcher() {
    return classMatcher;
  }

  public Matcher<? super Method> getMethodMatcher() {
    return methodMatcher;
  }

  public List<MethodInterceptor> getInterceptors() {
    return interceptors;
  }

  public <T> T acceptVisitor(Visitor<T> visitor) {
    return visitor.visitBindInterceptor(this);
  }
}