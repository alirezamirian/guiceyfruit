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

import java.util.Map;

/**
 * Injects dependencies into constructors, methods and fields annotated with
 * &#64;{@link Inject}.
 *
 * <p>When injecting a method or constructor, you can additionally annotate
 * its parameters with &#64;{@link Inject} and specify a dependency name. When
 * a parameter has no annotation, the container uses the name from the method
 * or constructor's &#64;{@link Inject} annotation respectively.
 *
 * <p>For example:
 *
 * <pre>
 *  class Foo {
 *
 *    // Inject the int constant named "i".
 *    &#64;Inject("i") int i;
 *
 *    // Inject the default implementation of Bar and the String constant
 *    // named "s".
 *    &#64;Inject Foo(Bar bar, @Inject("s") String s) {
 *      ...
 *    }
 *
 *    // Inject the default implementation of Baz and the Bob implementation
 *    // named "foo".
 *    &#64;Inject void initialize(Baz baz, @Inject("foo") Bob bob) {
 *      ...
 *    }
 *
 *    // Inject the default implementation of Tee.
 *    &#64;Inject void setTee(Tee tee) {
 *      ...
 *    }
 *  }
 * </pre>
 *
 * <p>To get an instance of {@code Foo}:
 *
 * <pre>
 *  Container c = ...;
 *  Key&lt;Foo> fooKey = Key.get(Foo.class);
 *  Factory&lt;Foo> fooFactory = c.getFactory(fooKey);
 *  Foo foo = fooFactory.get();
 * </pre>
 *
 * @see ContainerBuilder
 * @author crazybob@google.com (Bob Lee)
 */
public interface Container {

  /**
   * Injects dependencies into the fields and methods of an existing object.
   */
  void injectMembers(Object o);

  /**
   * Gets a factory which injects the given class's constructor and creates
   * new instances of {@code T}.
   */
  <T> Factory<T> getCreator(Class<T> implementation);

  /**
   * Gets the factory bound to the given key.
   */
  <T> Factory<T> getFactory(Key<T> key);

  /**
   * Gets all bindings.
   */
  Map<Key<?>, Binding<?>> getBindings();

  /**
   * Gets a binding for the given key.
   */
  <T> Binding<T> getBinding(Key<T> key);
}