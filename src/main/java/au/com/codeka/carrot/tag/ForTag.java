package au.com.codeka.carrot.tag;

import au.com.codeka.carrot.CarrotEngine;
import au.com.codeka.carrot.CarrotException;
import au.com.codeka.carrot.Scope;
import au.com.codeka.carrot.ValueHelper;
import au.com.codeka.carrot.expr.Identifier;
import au.com.codeka.carrot.expr.Statement;
import au.com.codeka.carrot.expr.StatementParser;
import au.com.codeka.carrot.tmpl.TagNode;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The "for" tag iterates through a loop and execute it's black for each element0.
 */
public class ForTag extends Tag {
  private Identifier loopIdentifier;
  private Statement loopStatement;

  @Override
  public boolean isBlockTag() {
    return true;
  }

  @Override
  public void parseStatement(StatementParser stmtParser) throws CarrotException {
    loopIdentifier = stmtParser.parseIdentifier();
    Identifier inIdentifier = stmtParser.parseIdentifier();
    if (!inIdentifier.evaluate().equalsIgnoreCase("in")) {
      throw new CarrotException("Expected 'in'.");
    }
    loopStatement = stmtParser.parseStatement();
  }

  @Override
  public void render(CarrotEngine engine, Writer writer, TagNode tagNode, Scope scope)
      throws CarrotException, IOException {

    List<Object> objects = ValueHelper.iterate(loopStatement.evaluate(engine.getConfig(), scope));
    Map<String, Object> loop = new HashMap<>();
    for (int i = 0; i < objects.size(); i++) {
      Map<String, Object> context = new HashMap<>();
      context.put(loopIdentifier.evaluate(), objects.get(i));

      loop.put("index", i);
      loop.put("revindex", objects.size() - i - 1);
      loop.put("first", i == 0);
      loop.put("last", i == (objects.size() - 1));
      loop.put("length", objects.size());
      context.put("loop", loop);

      scope.push(context);
      tagNode.renderChildren(engine, writer, scope);
      scope.pop();
    }
  }
}