package au.com.codeka.carrot.expr;

import au.com.codeka.carrot.CarrotException;
import au.com.codeka.carrot.Configuration;
import au.com.codeka.carrot.Scope;
import au.com.codeka.carrot.ValueHelper;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * A {@link Variable} is has the following EBNF form:
 *
 * <code>variable = identifier ["." variable | "[" expression "]"]</code>
 *
 * See {@link StatementParser} for the full grammar.
 */
public class Variable {
  private final Identifier identifier;
  @Nullable private final Function function;
  @Nullable private final Expression accessExpression;
  @Nullable private final Variable dotVariable;

  public Variable(
      Identifier identifier,
      @Nullable Function function,
      @Nullable Expression accessExpression,
      @Nullable Variable dotVariable) {
    this.identifier = identifier;
    this.function = function;
    this.accessExpression = accessExpression;
    this.dotVariable = dotVariable;
  }

  public Object evaluate(Configuration config, Scope scope) throws CarrotException {
    Object value = scope.resolve(identifier.evaluate());
    if (function != null) {
      value = function.evaluate(value, config, scope);
    }
    return evaluateRecursive(value, config, scope);
  }

  private Object evaluate(Object value, Configuration config, Scope scope) throws CarrotException {
    Object accessor = identifier.evaluate();
    value = access(config, value, accessor, identifier.toString());
    return evaluateRecursive(value, config, scope);
  }

  private Object evaluateRecursive(Object value, Configuration config, Scope scope) throws CarrotException {
    if (accessExpression != null) {
      value = access(
          config, value, accessExpression.evaluate(config, scope), accessExpression.toString());
    }
    if (dotVariable != null) {
      value = dotVariable.evaluate(value, config, scope);
    }
    return value;
  }

  private Object access(
      Configuration config,
      Object value,
      Object accessor,
      String debugName) throws CarrotException {
    if (value == null) {
      throw new CarrotException(debugName + " is null.");
    } else if (value instanceof Map) {
      Map map = (Map) value;
      return map.get(accessor);
    } else if (value instanceof List) {
      List list = (List) value;
      return list.get(ValueHelper.toNumber(accessor).intValue());
    } else if (value.getClass().isArray()) {
      return Array.get(value, ValueHelper.toNumber(accessor).intValue());
    } else {
      // Do some reflection. First, check for a field with the given name.
      try {
        String name = accessor.toString();
        Field field = value.getClass().getField(name);
        field.setAccessible(true);
        return field.get(value);
      } catch(NoSuchFieldException | IllegalAccessException e) {
        // Just keep trying.
      }

      // Next, try a method with the given name
      try {
        String name = accessor.toString();
        Method method = value.getClass().getMethod(name);
        method.setAccessible(true);
        return method.invoke(value);
      } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
        // Just keep trying.
      }

      // Next, try a getter method with the given name (that is, if name is "foo" try "getFoo").
      try {
        String name = accessor.toString();
        name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        name = "get" + name;
        Method method = value.getClass().getMethod(name);
        method.setAccessible(true);
        return method.invoke(value);
      } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
        // Just keep trying.
        throw new CarrotException(e);
      }

   //   throw new CarrotException("Cannot access key '" + accessor + "' in '" + value + "'");
    }
  }

  /** Gets a string representation of the {@link Variable}, useful for debugging. */
  @Override
  public String toString() {
    String str = identifier.toString();
    if (function != null) {
      str += " " + TokenType.DOT + " " + function.toString();
    }
    if (accessExpression != null) {
      str += " " + TokenType.LSQUARE + " " + accessExpression + " " + TokenType.RSQUARE + " ";
    }
    if (dotVariable != null) {
      str += " " + TokenType.DOT + " " + dotVariable.toString();
    }
    return str;
  }
}