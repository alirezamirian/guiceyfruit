/*
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

package com.google.inject;

import com.google.inject.commands.*;
import com.google.inject.internal.UniqueAnnotations;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The entry point to the Guice framework. Creates {@link Injector}s from
 * {@link Module}s.
 *
 * <p>Guice supports a model of development that draws clear boundaries between
 * APIs, Implementations of these APIs, Modules which configure these
 * implementations, and finally Applications which consist of a collection of
 * Modules. It is the Application, which typically defines your {@code main()}
 * method, that bootstraps the Guice Injector using the {@code Guice} class, as
 * in this example:
 * <pre>
 *     public class FooApplication {
 *       public static void main(String[] args) {
 *         Injector injector = Guice.createInjector(
 *             new ModuleA(),
 *             new ModuleB(),
 *             . . .
 *             new FooApplicationFlagsModule(args)
 *         );
 *
 *         // Now just bootstrap the application and you're done
 *         MyStartClass starter = injector.getInstance(MyStartClass.class);
 *         starter.runApplication();
 *       }
 *     }
 * </pre>
 */
public final class Guice {

  private Guice() {}

  /**
   * Creates an injector for the given set of modules.
   *
   * @throws CreationException if one or more errors occur during Injector
   *     construction
   */
  public static Injector createInjector(Module... modules) {
    return createInjector(Arrays.asList(modules));
  }

  /**
   * Creates an injector for the given set of modules.
   *
   * @throws CreationException if one or more errors occur during Injector
   *     construction
   */
  public static Injector createInjector(Iterable<? extends Module> modules) {
    return createInjector(Stage.DEVELOPMENT, modules);
  }

  /**
   * Creates an injector for the given set of modules, in a given development
   * stage.
   *
   * @throws CreationException if one or more errors occur during Injector
   *     construction
   */
  public static Injector createInjector(Stage stage, Module... modules) {
    return createInjector(stage, Arrays.asList(modules));
  }

  /**
   * Creates an injector for the given set of modules, in a given development
   * stage.
   *
   * @throws CreationException if one or more errors occur during Injector
   *     construction
   */
  public static Injector createInjector(Stage stage,
      Iterable<? extends Module> modules) {
    return createInjector(null, stage, modules);
  }


  /**
   * Creates an injector for the given set of modules, with the given parent
   * injector.
   *
   * @throws CreationException if one or more errors occur during Injector
   *     construction
   */
  public static Injector createInjector(Injector parent,
      Iterable<? extends Module> modules) {
    return createInjector(parent, Stage.DEVELOPMENT, modules);
  }


  /**
   * Creates an injector for the given set of modules, with the given parent
   * injector.
   *
   * @throws CreationException if one or more errors occur during Injector
   *     construction
   */
  public static Injector createInjector(Injector parent,
      Module... modules) {
    return createInjector(parent, Stage.DEVELOPMENT, Arrays.asList(modules));
  }

  /**
   * Creates an injector for the given set of modules, in a given development
   * stage, with the given parent injector.
   *
   * @throws CreationException if one or more errors occur during Injector
   *     construction
   */
  public static Injector createInjector(
      Injector parent, Stage stage,
      Iterable<? extends Module> modules) {
    return new InjectorBuilder()
        .stage(stage)
        .parentInjector(parent)
        .addModules(modules)
        .build();
  }

  /**
   * Returns a new {@link Module} that overlays {@code overridesModule} over
   * {@code module}. If a key is bound by both modules, only the binding in
   * overrides is kept. This can be used to replace bindings in a production
   * module with test bindings:
   * <pre>
   * Module functionalTestModule
   *     = Guice.overrideModule(new ProductionModule(), new TestModule());
   * </pre>
   */
  public static Module overrideModule(Module module, Module overridesModule) {
    final FutureInjector futureInjector = new FutureInjector();
    CommandRecorder commandRecorder = new CommandRecorder(futureInjector);
    final List<Command> commands = commandRecorder.recordCommands(module);
    final List<Command> overrideCommands = commandRecorder.recordCommands(overridesModule);

    return new AbstractModule() {
      public void configure() {
        final Set<Key> overriddenKeys = new HashSet<Key>();

        bind(Object.class).annotatedWith(UniqueAnnotations.create())
            .toInstance(new Object() {
              @Inject void initialize(Injector injector) {
                futureInjector.initialize(injector);
              }
            });

        // execute the overrides module, keeping track of which keys were bound
        new CommandReplayer() {
          @Override public <T> void replayBind(Binder binder, BindCommand<T> command) {
            overriddenKeys.add(command.getKey());
            super.replayBind(binder, command);
          }
          @Override public void replayBindConstant(Binder binder, BindConstantCommand command) {
            overriddenKeys.add(command.getKey());
            super.replayBindConstant(binder, command);
          }
        }.replay(binder(), overrideCommands);

        // bind the regular module, skipping overridden keys. We only skip each
        // overridden key once, so things still blow up if the module binds the
        // same key multiple times
        new CommandReplayer() {
          @Override public <T> void replayBind(Binder binder, BindCommand<T> command) {
            if (!overriddenKeys.remove(command.getKey())) {
              super.replayBind(binder, command);
            }
          }
          @Override public void replayBindConstant(Binder binder, BindConstantCommand command) {
            if (!overriddenKeys.remove(command.getKey())) {
              super.replayBindConstant(binder, command);
            }
          }
        }.replay(binder(), commands);

        // TODO: bind the overridden keys using multibinder
      }
    };
  }
}
