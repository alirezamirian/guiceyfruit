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

package com.google.inject.multibindings;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import com.google.inject.Binder;
import com.google.inject.Binding;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.util.Types;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An API to bind multiple values separately, only to later inject them as a
 * complete collection. Multibinder is intended for use in your application's
 * module:
 * <pre><code>
 * public class SnacksModule extends AbstractModule {
 *   protected void configure() {
 *     Multibinder&lt;Snack&gt; multibinder
 *         = Multibinder.newSetBinder(binder(), Snack.class);
 *     multibinder.addBinding().toInstance(new Twix());
 *     multibinder.addBinding().toProvider(SnickersProvider.class);
 *     multibinder.addBinding().to(Skittles.class);
 *   }
 * }</code></pre>
 *
 * <p>With this binding, a {@link Set}{@code <Snack>} can now be injected:
 * <pre><code>
 * class SnackMachine {
 *   {@literal @}Inject
 *   public SnackMachine(Set&lt;Snack&gt; snacks) { ... }
 * }</code></pre>
 *
 * <p>Create multibindings from different modules is supported. For example, it
 * is okay to have both {@code CandyModule} and {@code ChipsModule} to both
 * create their own {@code Multibinder<Snack>}, and to each contribute bindings
 * to the set of snacks. When that set is injected, it will contain elements
 * from both modules.
 *
 * <p>Elements are resolved at set injection time. If an element is bound to a
 * provider, that provider's get method will be called each time the set is
 * injected (unless the binding is also scoped).
 *
 * <p>Annotations are be used to create different sets of the same element
 * type. Each distinct annotation gets its own independent collection of
 * elements.
 *
 * <p><strong>Elements must be distinct.</strong> If multiple bound elements
 * have the same value, set injection will fail.
 *
 * <p><strong>Elements must be non-null.</strong> If any set element is null,
 * set injection will fail.
 *
 * @author jessewilson@google.com (Jesse Wilson)
 */
public abstract class Multibinder<T> {
  private Multibinder() {}

  /**
   * Returns a new multibinder that collects instances of {@code type} in a
   * {@link Set} that is itself bound with no binding annotation.
   */
  public static <T> Multibinder<T> newSetBinder(Binder binder, Type type) {
    binder = binder.skipSources(RealMultibinder.class, Multibinder.class);
    RealMultibinder<T> result = new RealMultibinder<T>(binder, type, "",
        Key.get(Multibinder.<T>setOf(type)));
    binder.install(result);
    return result;
  }

  /**
   * Returns a new multibinder that collects instances of {@code type} in a
   * {@link Set} that is itself bound with {@code annotation}.
   */
  public static <T> Multibinder<T> newSetBinder(Binder binder, Type type, Annotation annotation) {
    binder = binder.skipSources(RealMultibinder.class, Multibinder.class);
    RealMultibinder<T> result = new RealMultibinder<T>(binder, type, annotation.toString(),
        Key.get(Multibinder.<T>setOf(type), annotation));
    binder.install(result);
    return result;
  }

  /**
   * Returns a new multibinder that collects instances of {@code type} in a
   * {@link Set} that is itself bound with {@code annotationType}.
   */
  public static <T> Multibinder<T> newSetBinder(Binder binder, Type type,
      Class<? extends Annotation> annotationType) {
    binder = binder.skipSources(RealMultibinder.class, Multibinder.class);
    RealMultibinder<T> result = new RealMultibinder<T>(binder, type, "@" + annotationType.getName(),
        Key.get(Multibinder.<T>setOf(type), annotationType));
    binder.install(result);
    return result;
  }

  @SuppressWarnings("unchecked")
  private static <T> TypeLiteral<Set<T>> setOf(Type elementType) {
    Type type = Types.setOf(elementType);
    return (TypeLiteral<Set<T>>) TypeLiteral.get(type);
  }

  /**
   * Returns a binding builder used to add a new element in the set. Each
   * bound element must have a distinct value. Bound providers will be
   * evaluated each time the set is injected.
   *
   * <p>It is an error to call this method without also calling one of the
   * {@code to} methods on the returned binding builder.
   *
   * <p>Scoping elements independently is supported. Use the {@code in} method
   * to specify a binding scope.
   */
  public abstract LinkedBindingBuilder<T> addBinding();

  /**
   * The actual multibinder plays several roles:
   *
   * <p>As a Multibinder, it acts as a factory for LinkedBindingBuilders for
   * each of the set's elements. Each binding is given an annotation that
   * identifies it as a part of this set.
   *
   * <p>As a Module, it installs the binding to the set itself. As a module,
   * this implements equals() and hashcode() in order to trick Guice into
   * executing its configure() method only once. That makes it so that
   * multiple multibinders can be created for the same target collection, but
   * only one is bound. Since the list of bindings is retrieved from the
   * injector itself (and not the multibinder), each multibinder has access to
   * all contributions from all multibinders.
   *
   * <p>As a Provider, this constructs the set instances.
   *
   * <p>We use a subclass to hide 'implements Module, Provider' from the public
   * API.
   */
  static final class RealMultibinder<T>
      extends Multibinder<T> implements Module, Provider<Set<T>> {

    private final Type elementType;
    private final String setName;
    private final Key<Set<T>> setKey;

    /* the target injector's binder. non-null until initialization, null afterwards */
    private Binder binder;

    /* a provider for each element in the set. null until initialization, non-null afterwards */
    private List<Provider<T>> providers;

    private RealMultibinder(Binder binder, Type elementType,
        String setName, Key<Set<T>> setKey) {
      this.binder = checkNotNull(binder, "binder");
      this.elementType = checkNotNull(elementType, "elementType");
      this.setName = checkNotNull(setName, "setName");
      this.setKey = checkNotNull(setKey, "setKey");
    }

    @SuppressWarnings("unchecked")
    public void configure(Binder binder) {
      checkState(!isInitialized(), "Multibinder was already initialized");

      binder.bind(setKey).toProvider(this);
    }

    @SuppressWarnings("unchecked")
    @Override public LinkedBindingBuilder<T> addBinding() {
      checkState(!isInitialized(), "Multibinder was already initialized");

      return binder.bind((Key<T>) Key.get(elementType, new RealElement(setName)));
    }

    /**
     * Invoked by Guice at Injector-creation time to prepare providers for each
     * element in this set. At this time the set's size is known, but its
     * contents are only evaluated when get() is invoked.
     */
    @Inject void initialize(Injector injector) {
      providers = new ArrayList<Provider<T>>();
      for (Map.Entry<Key<?>, Binding<?>> entry : injector.getBindings().entrySet()) {
        if (keyMatches(entry.getKey())) {
          @SuppressWarnings("unchecked")
          Binding<T> binding = (Binding<T>) entry.getValue();
          providers.add(binding.getProvider());
        }
      }

      this.binder = null;
    }

    private boolean keyMatches(Key<?> key) {
      return key.getTypeLiteral().getType().equals(elementType)
          && key.getAnnotation() instanceof Element
          && ((Element) key.getAnnotation()).setName().equals(setName);
    }

    private boolean isInitialized() {
      return binder == null;
    }

    public Set<T> get() {
      checkState(isInitialized(), "Multibinder is not initialized");

      Set<T> result = new LinkedHashSet<T>();
      for (Provider<T> provider : providers) {
        final T newValue = provider.get();
        checkState(newValue != null, "Set injection failed due to null element");
        checkState(result.add(newValue),
            "Set injection failed due to duplicated element \"%s\"", newValue);
      }
      return Collections.unmodifiableSet(result);
    }
    
    String getSetName() {
      return setName;
    }
    
    Key<Set<T>> getSetKey() {
      return setKey;
    }

    @Override public boolean equals(Object o) {
      return o instanceof RealMultibinder
          && ((RealMultibinder<?>) o).setKey.equals(setKey);
    }

    @Override public int hashCode() {
      return setKey.hashCode();
    }

    @Override public String toString() {
      return new StringBuilder()
          .append(setName)
          .append(setName.length() > 0 ? " " : "")
          .append("Multibinder<")
          .append(elementType)
          .append(">")
          .toString();
    }

    public Type getElementType() {
      return elementType;
    }
  }
}
