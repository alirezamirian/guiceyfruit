/**
 * Copyright (C) 2007 Google Inc.
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

package com.google.inject.spi;

import com.google.inject.Binding;

/**
 * A binding to a constant.
 *
 * <p>Example: {@code bindConstant().annotatedWith(PoolSize.class).to(5);}
 *
 * @author crazybob@google.com (Bob Lee)
 */
public interface ConstantBinding<T> extends Binding<T> {

  /**
   * Gets the constant value associated with this binding.
   */
  T getValue();
}
