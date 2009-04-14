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

import junit.framework.TestCase;

import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author crazybob@google.com (Bob Lee)
 */
public class ConstantConversionTest extends TestCase {

  @Retention(RUNTIME)
  @Binder @interface NumericValue {}

  @Retention(RUNTIME)
  @Binder @interface BooleanValue {}

  @Retention(RUNTIME)
  @Binder @interface EnumValue {}

  @Retention(RUNTIME)
  @Binder @interface ClassName {}

  public static class Foo {
    @Inject @NumericValue Integer integerField;
    @Inject @NumericValue int primitiveIntField;
    @Inject @NumericValue Long longField;
    @Inject @NumericValue long primitiveLongField;
    @Inject @BooleanValue Boolean booleanField;
    @Inject @BooleanValue boolean primitiveBooleanField;
    @Inject @NumericValue Byte byteField;
    @Inject @NumericValue byte primitiveByteField;
    @Inject @NumericValue Short shortField;
    @Inject @NumericValue short primitiveShortField;
    @Inject @NumericValue Float floatField;
    @Inject @NumericValue float primitiveFloatField;
    @Inject @NumericValue Double doubleField;
    @Inject @NumericValue short primitiveDoubleField;
    @Inject @EnumValue Bar enumField;
    @Inject @ClassName Class<?> classField;
  }

  public enum Bar {
    TEE, BAZ, BOB;
  }

  public void testOneConstantInjection() throws CreationException {
    ContainerBuilder builder = new ContainerBuilder();
    builder.bindConstant(NumericValue.class).to("5");
    builder.bind(Simple.class);
    Container container = builder.create();
    Simple simple = container.getLocator(Simple.class).get();
    assertEquals(5, simple.i);
  }

  static class Simple {
    @Inject @NumericValue int i;
  }

  public void testConstantInjection() throws CreationException {
    ContainerBuilder b = new ContainerBuilder();
    b.bindConstant(NumericValue.class).to("5");
    b.bindConstant(BooleanValue.class).to("true");
    b.bindConstant(EnumValue.class).to("TEE");
    b.bindConstant(ClassName.class).to(Foo.class.getName());
    Container c = b.create();
    Foo foo = c.getLocator(Foo.class).get();

    checkNumbers(
      foo.integerField,
      foo.primitiveIntField,
      foo.longField,
      foo.primitiveLongField,
      foo.byteField,
      foo.primitiveByteField,
      foo.shortField,
      foo.primitiveShortField,
      foo.floatField,
      foo.primitiveFloatField,
      foo.doubleField,
      foo.primitiveDoubleField
    );

    assertEquals(Bar.TEE, foo.enumField);
    assertEquals(Foo.class, foo.classField);
  }

  void checkNumbers(Number... ns) {
    for (Number n : ns) {
      assertEquals(5, n.intValue());
    }
  }

  public void testInvalidInteger() throws CreationException {
    ContainerBuilder b = new ContainerBuilder();
    b.bindConstant(NumericValue.class).to("invalid");
    Container c = b.create();
    try {
      c.getLocator(InvalidInteger.class).get();
      fail();
    } catch (ConfigurationException e) { /* expected */ }
  }

  public static class InvalidInteger {
    @Inject @NumericValue Integer integerField;
  }

  public void testInvalidCharacter() throws CreationException {
    ContainerBuilder b = new ContainerBuilder();
    b.bindConstant(NumericValue.class).to("invalid");
    Container c = b.create();
    try {
      c.getLocator(InvalidCharacter.class).get();
      fail();
    } catch (ConfigurationException e) { /* expected */ }
  }

  public static class InvalidCharacter {
    @Inject @NumericValue char foo;
  }

  public void testInvalidEnum() throws CreationException {
    ContainerBuilder b = new ContainerBuilder();
    b.bindConstant(NumericValue.class).to("invalid");
    Container c = b.create();
    try {
      c.getLocator(InvalidEnum.class).get();
      fail();
    } catch (ConfigurationException e) { /* expected */ }
  }

  public static class InvalidEnum {
    @Inject @NumericValue Bar foo;
  }
}