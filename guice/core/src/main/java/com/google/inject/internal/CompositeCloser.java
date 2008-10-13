/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.inject.internal;

import com.google.inject.spi.Closer;

/**
 * A Composite implementation of {@link Closer}
 *
 * @version $Revision: 1.1 $
 * @author james.strachan@gmail.com (James Strachan)
 */
public class CompositeCloser implements Closer {
  private final Iterable<Closer> closers;

  public CompositeCloser(Iterable<Closer> closers) {
    this.closers = closers;
  }

  public void close(Object object) throws Throwable {
    for (Closer closer : closers) {
      closer.close(object);
    }
  }
}
