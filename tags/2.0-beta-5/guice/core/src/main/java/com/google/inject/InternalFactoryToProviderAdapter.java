/**
 * Copyright (C) 2006 Google Inc.
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

package com.google.inject;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.inject.internal.Errors;
import com.google.inject.internal.ErrorsException;
import com.google.inject.internal.SourceProvider;
import com.google.inject.spi.Closeable;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.Closer;
import com.google.inject.spi.CloseErrors;

/**
 * @author crazybob@google.com (Bob Lee)
*/
class InternalFactoryToProviderAdapter<T> implements InternalFactory<T>, Closeable {

  private final Initializable<Provider<? extends T>> initializable;
  private final Object source;

  public InternalFactoryToProviderAdapter(Initializable<Provider<? extends T>> initializable) {
    this(initializable, SourceProvider.UNKNOWN_SOURCE);
  }

  public InternalFactoryToProviderAdapter(
      Initializable<Provider<? extends T>> initializable, Object source) {
    this.initializable = checkNotNull(initializable, "provider");
    this.source = checkNotNull(source, "source");
  }

  public T get(Errors errors, InternalContext context, Dependency<?> dependency)
      throws ErrorsException {
    try {
      Provider<? extends T> provider = initializable.get(errors);
      T value = provider.get();
      return errors.checkForNull(value, source, dependency);
    } catch (RuntimeException userException) {
      Errors userErrors = ProvisionException.getErrors(userException);
      throw errors.withSource(source)
          .errorInProvider(userException, userErrors).toException();
    }
  }

  public void close(Closer closer, CloseErrors errors) {
    if (initializable instanceof Closeable) {
      Closeable closeable = (Closeable) initializable;
      closeable.close(closer, errors);
    }
  }

  @Override public String toString() {
    return initializable.toString();
  }
}