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

package com.google.inject.spi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.internal.Annotations;
import com.google.inject.internal.ConfigurationException;
import com.google.inject.internal.Errors;
import com.google.inject.internal.ErrorsException;
import com.google.inject.internal.MoreTypes;
import com.google.inject.internal.Nullability;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * A constructor, field or method that can receive injections. Typically this is a member with the
 * {@literal @}{@link Inject} annotation. For non-private, no argument constructors, the member may
 * omit the annotation. 
 *
 * @author crazybob@google.com (Bob Lee)
 */
public final class InjectionPoint implements Serializable {

  private final boolean optional;
  private final Member member;
  private final ImmutableList<Dependency<?>> dependencies;
  private final AnnotationProviderFactory<?> annotationProviderFactory;

  static InjectionPoint customInjectionPoint(Member member, AnnotationProviderFactory<?> annotationProviderFactory) {
    if (member instanceof Field) {
      return new InjectionPoint((Field) member, false, annotationProviderFactory);
    }
    else {
      return new InjectionPoint((Method) member, false, annotationProviderFactory);
    }
  }

  private InjectionPoint(Member member,
      ImmutableList<Dependency<?>> dependencies, boolean optional) {
    this.member = member;
    this.dependencies = dependencies;
    this.optional = optional;
    this.annotationProviderFactory = null;
  }

  InjectionPoint(Method method, boolean optional,
      AnnotationProviderFactory<?> annotationProviderFactory) {
    this.member = method;
    this.optional = optional;
    this.annotationProviderFactory = annotationProviderFactory;

    this.dependencies = forMember(method, method.getGenericParameterTypes(),
        method.getParameterAnnotations());
  }

  InjectionPoint(Method method) {
    this(method, method.getAnnotation(Inject.class).optional(), null);
  }

  InjectionPoint(Constructor<?> constructor) {
    this.member = constructor;
    this.optional = false;
    this.annotationProviderFactory = null;
    // TODO(jessewilson): make sure that if @Inject it exists, its not optional
    this.dependencies = forMember(constructor, constructor.getGenericParameterTypes(),
        constructor.getParameterAnnotations());
  }

  InjectionPoint(Field field, boolean optional,
      AnnotationProviderFactory<?> annotationProviderFactory) {
    this.member = field;
    this.optional = optional;
    this.annotationProviderFactory = annotationProviderFactory;

    Annotation[] annotations = field.getAnnotations();

    Errors errors = new Errors(field);
    Key<?> key = null;
    try {
      key = Annotations.getKey(field.getGenericType(), field, annotations, errors);
    } catch (ErrorsException e) {
      errors.merge(e.getErrors());
    }
    ConfigurationException.throwNewIfNonEmpty(errors);

    this.dependencies = ImmutableList.<Dependency<?>>of(
        newDependency(key, Nullability.allowsNull(annotations), -1));
  }

  InjectionPoint(Field field) {
    this(field, field.getAnnotation(Inject.class).optional(), null);
  }

  private ImmutableList<Dependency<?>> forMember(Member member, Type[] genericParameterTypes,
      Annotation[][] annotations) {
    Errors errors = new Errors(member);
    Iterator<Annotation[]> annotationsIterator = Arrays.asList(annotations).iterator();

    List<Dependency<?>> dependencies = Lists.newArrayList();
    int index = 0;
    for (Type parameterType : genericParameterTypes) {
      try {
        Annotation[] parameterAnnotations = annotationsIterator.next();
        Key<?> key = Annotations.getKey(parameterType, member, parameterAnnotations, errors);
        dependencies.add(newDependency(key, Nullability.allowsNull(parameterAnnotations), index));
        index++;
      } catch (ErrorsException e) {
        errors.merge(e.getErrors());
      }
    }

    ConfigurationException.throwNewIfNonEmpty(errors);
    return ImmutableList.copyOf(dependencies);
  }

  // This metohd is necessary to create a Dependency<T> with proper generic type information
  private <T> Dependency<T> newDependency(Key<T> key, boolean allowsNull, int parameterIndex) {
    return new Dependency<T>(this, key, allowsNull, parameterIndex);
  }

  /**
   * Returns the injected constructor, field, or method.
   */
  public Member getMember() {
    return member;
  }

  /**
   * Returns the dependencies for this injection point. If the injection point is for a method or
   * constructor, the dependencies will correspond to that member's parameters. Field injection
   * points always have a single dependency for the field itself.
   *
   * @return a possibly-empty list
   */
  public List<Dependency<?>> getDependencies() {
    return dependencies;
  }

  /**
   * Returns true if this injection point shall be skipped if the injector cannot resolve bindings
   * for all required dependencies. Both explicit bindings (as specified in a module), and implicit
   * bindings ({@literal @}{@link com.google.inject.ImplementedBy ImplementedBy}, default
   * constructors etc.) may be used to satisfy optional injection points.
   */
  public boolean isOptional() {
    return optional;
  }

  /**
   * Returns the custom annotation provider factory for custom injection points or null for regular
   * @Inject injection points
   *
   * @return the custom annotation provider factory for custom injection points or null for regular
   * injection points
   */
  public AnnotationProviderFactory<?> getAnnotationProviderFactory() {
    return annotationProviderFactory;
  }

  @Override public boolean equals(Object o) {
    return o instanceof InjectionPoint
        && member == ((InjectionPoint) o).member;
  }

  @Override public int hashCode() {
    return member.hashCode();
  }

  @Override public String toString() {
    return MoreTypes.toString(member);
  }

  private Object writeReplace() throws ObjectStreamException {
    Member serializableMember = member != null ? MoreTypes.serializableCopy(member) : null;
    return new InjectionPoint(serializableMember, dependencies, optional);
  }

  /**
   * Returns a new injection point for the injectable constructor of {@code type}.
   *
   * @param type a concrete type with exactly one constructor annotated {@literal @}{@link Inject},
   *     or a no-arguments constructor that is not private.
   * @throws RuntimeException if there is no injectable constructor, more than one injectable
   *     constructor, or if parameters of the injectable constructor are malformed, such as a
   *     parameter with multiple binding annotations.
   */
  public static InjectionPoint forConstructorOf(Class<?> type) {
    Errors errors = new Errors(type);

    Constructor<?> found = null;
    for (Constructor<?> constructor : type.getDeclaredConstructors()) {
      Inject inject = constructor.getAnnotation(Inject.class);
      if (inject != null) {
        if (inject.optional()) {
          errors.optionalConstructor(constructor);
        }

        if (found != null) {
          errors.tooManyConstructors(type);
        }

        found = constructor;
      }
    }

    ConfigurationException.throwNewIfNonEmpty(errors);

    if (found != null) {
      return new InjectionPoint(found);
    }

    // If no annotated constructor is found, look for a no-arg constructor
    // instead.
    try {
      Constructor<?> noArgCtor = type.getDeclaredConstructor();

      // Disallow private constructors on non-private classes (unless they have @Inject)
      if (Modifier.isPrivate(noArgCtor.getModifiers())
          && !Modifier.isPrivate(type.getModifiers())) {
        errors.missingConstructor(type);
        throw new ConfigurationException(errors);
      }

      return new InjectionPoint(noArgCtor);
    } catch (NoSuchMethodException e) {
      errors.missingConstructor(type);
      throw new ConfigurationException(errors);
    }
  }

  /**
   * Adds all static method and field injection points on {@code type} to {@code injectionPoints}.
   * All fields are added first, and then all methods. Within the fields, supertype fields are added
   * before subtype fields. Similarly, supertype methods are added before subtype methods.
   *
   * @throws RuntimeException if there is a malformed injection point on {@code type}, such as a
   *      field with multiple binding annotations. When such an exception is thrown, the valid
   *      injection points are still added to the collection.
   */
  public static void addForStaticMethodsAndFields(Map<Class<? extends Annotation>,AnnotationProviderFactory> customInjections, Class<?> type, Collection<InjectionPoint> sink) {
    Errors errors = new Errors();
    addInjectionPoints(customInjections, type, Factory.FIELDS, true, sink, errors);
    addInjectionPoints(customInjections, type, Factory.METHODS, true, sink, errors);
    ConfigurationException.throwNewIfNonEmpty(errors);
  }

  /**
   * Adds all instance method and field injection points on {@code type} to {@code injectionPoints}.
   * All fields are added first, and then all methods. Within the fields, supertype fields are added
   * before subtype fields. Similarly, supertype methods are added before subtype methods.
   *
   * @throws RuntimeException if there is a malformed injection point on {@code type}, such as a
   *      field with multiple binding annotations. When such an exception is thrown, the valid
   *      injection points are still added to the collection.
   */
  public static void addForInstanceMethodsAndFields(Map<Class<? extends Annotation>,AnnotationProviderFactory> customInjections, Class<?> type,
      Collection<InjectionPoint> sink) {
    // TODO (crazybob): Filter out overridden members.
    Errors errors = new Errors();
    addInjectionPoints(customInjections, type, Factory.FIELDS, false, sink, errors);
    addInjectionPoints(customInjections, type, Factory.METHODS, false, sink, errors);
    ConfigurationException.throwNewIfNonEmpty(errors);
  }

  private static <M extends Member & AnnotatedElement> void addInjectionPoints(
      Map<Class<? extends Annotation>,AnnotationProviderFactory> customInjections,
      Class<?> type,
      Factory<M> factory, boolean statics, Collection<InjectionPoint> injectionPoints,
      Errors errors) {
    if (type == Object.class) {
      return;
    }

    // Add injectors for superclass first.
    addInjectionPoints(customInjections, type.getSuperclass(), factory, statics, injectionPoints, errors);

    // Add injectors for all members next
    addInjectorsForMembers(customInjections, type, factory, statics, injectionPoints, errors);
  }

  private static <M extends Member & AnnotatedElement> void addInjectorsForMembers(
      Map<Class<? extends Annotation>,AnnotationProviderFactory> customInjections,
      Class<?> type,
      Factory<M> factory, boolean statics, Collection<InjectionPoint> injectionPoints,
      Errors errors) {
    for (M member : factory.getMembers(type)) {
      if (isStatic(member) != statics) {
        continue;
      }

      Inject inject = member.getAnnotation(Inject.class);
      if (inject != null) {
        try {
          injectionPoints.add(factory.create(member));
        } catch (ConfigurationException e) {
          if (!inject.optional()) {
            errors.merge(e.getErrorMessages());
          }
        }
        continue;
      }

      if (!customInjections.isEmpty()) {
        Set<Entry<Class<? extends Annotation>,AnnotationProviderFactory>> entries = customInjections
            .entrySet();
        for (Entry<Class<? extends Annotation>, AnnotationProviderFactory> entry : entries) {
          Class<? extends Annotation> annotationType = entry.getKey();
          AnnotationProviderFactory annotationProviderFactory = entry.getValue();
          Annotation annotation = member.getAnnotation(annotationType);
          if (annotation != null) {
            try {
              InjectionPoint injectionPoint = InjectionPoint.customInjectionPoint(member, annotationProviderFactory);
              injectionPoints.add(injectionPoint);
            } catch (ConfigurationException e) {
              if (!inject.optional()) {
                errors.merge(e.getErrorMessages());
              }
            }
            break;
          }
        }
      }
    }
  }

  private static boolean isStatic(Member member) {
    return Modifier.isStatic(member.getModifiers());
  }

  private interface Factory<M extends Member & AnnotatedElement> {
    Factory<Field> FIELDS = new Factory<Field>() {
      public Field[] getMembers(Class<?> type) {
        return type.getDeclaredFields();
      }
      public InjectionPoint create(Field member) {
        return new InjectionPoint(member);
      }
    };

    Factory<Method> METHODS = new Factory<Method>() {
      public Method[] getMembers(Class<?> type) {
        return type.getDeclaredMethods();
      }
      public InjectionPoint create(Method member) {
        return new InjectionPoint(member);
      }
    };

    M[] getMembers(Class<?> type);
    InjectionPoint create(M member);
  }

  private static final long serialVersionUID = 0;
}