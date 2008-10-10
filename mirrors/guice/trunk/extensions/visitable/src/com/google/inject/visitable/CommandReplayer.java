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

package com.google.inject.visitable;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.binder.AnnotatedConstantBindingBuilder;
import com.google.inject.binder.ConstantBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.internal.Objects;
import org.aopalliance.intercept.MethodInterceptor;

import java.util.List;

/**
 * Executes commands against a binder.
 *
 * @author jessewilson@google.com (Jesse Wilson)
 */
public class CommandReplayer {

  /**
   * Returns a module that executes the specified commands
   * using this executing visitor.
   */
  public Module createModule(final Iterable<Command> commands) {
    return new Module() {
      public void configure(Binder binder) {
        replay(binder, commands);
      }
    };
  }

  /**
   * Replays {@code commands} against {@code binder}.
   */
  public void replay(final Binder binder, Iterable<Command> commands) {
    Objects.nonNull(binder, "binder");
    Objects.nonNull(commands, "commands");

    Command.Visitor<Void> visitor = new Command.Visitor<Void>() {
      public Void visitAddMessageError(AddMessageErrorCommand command) {
        replayAddMessageError(binder, command);
        return null;
      }

      public Void visitAddError(AddThrowableErrorCommand command) {
        replayAddError(binder, command);
        return null;
      }

      public Void visitBindInterceptor(BindInterceptorCommand command) {
        replayBindInterceptor(binder, command);
        return null;
      }

      public Void visitBindScope(BindScopeCommand command) {
        replayBindScope(binder, command);
        return null;
      }

      public Void visitRequestStaticInjection(RequestStaticInjectionCommand command) {
        replayRequestStaticInjection(binder, command);
        return null;
      }

      public Void visitBindConstant(BindConstantCommand command) {
        replayBindConstant(binder, command);
        return null;
      }

      public Void visitConvertToTypes(ConvertToTypesCommand command) {
        replayConvertToTypes(binder, command);
        return null;
      }

      public <T> Void visitBind(BindCommand<T> command) {
        replayBind(binder, command);
        return null;
      }

      public <T> Void visitGetProvider(GetProviderCommand<T> command) {
        replayGetProvider(binder, command);
        return null;
      }
    };

    for (Command command : commands) {
      command.acceptVisitor(visitor);
    }
  }

  public void replayAddMessageError(Binder binder, AddMessageErrorCommand command) {
    binder.addError(command.getMessage(), command.getArguments().toArray());
  }

  public void replayAddError(Binder binder, AddThrowableErrorCommand command) {
    binder.addError(command.getThrowable());
  }

  public void replayBindInterceptor(Binder binder, BindInterceptorCommand command) {
    List<MethodInterceptor> interceptors = command.getInterceptors();
    binder.bindInterceptor(command.getClassMatcher(), command.getMethodMatcher(),
        interceptors.toArray(new MethodInterceptor[interceptors.size()]));
  }

  public void replayBindScope(Binder binder, BindScopeCommand command) {
    binder.bindScope(command.getAnnotationType(), command.getScope());
  }

  public void replayRequestStaticInjection(Binder binder, RequestStaticInjectionCommand command) {
    List<Class> types = command.getTypes();
    binder.requestStaticInjection(types.toArray(new Class[types.size()]));
  }

  public void replayBindConstant(Binder binder, BindConstantCommand command) {
    AnnotatedConstantBindingBuilder constantBindingBuilder = binder.bindConstant();

    Key<Object> key = command.getKey();
    ConstantBindingBuilder builder = key.getAnnotation() != null
        ? constantBindingBuilder.annotatedWith(key.getAnnotation())
        : constantBindingBuilder.annotatedWith(key.getAnnotationType());

    command.getTarget().execute(builder);
  }

  public void replayConvertToTypes(Binder binder, ConvertToTypesCommand command) {
    binder.convertToTypes(command.getTypeMatcher(), command.getTypeConverter());
  }

  public <T> void replayBind(Binder binder, BindCommand<T> command) {
    LinkedBindingBuilder<T> lbb = binder.bind(command.getKey());

    BindTarget<T> bindTarget = command.getTarget();
    ScopedBindingBuilder sbb = bindTarget != null
        ? bindTarget.execute(lbb)
        : lbb;

    BindScoping scoping = command.getScoping();
    if (scoping != null) {
      scoping.execute(sbb);
    }
  }

  public <T> void replayGetProvider(Binder binder, GetProviderCommand<T> command) {
    binder.getProvider(command.getKey());
  }
}
