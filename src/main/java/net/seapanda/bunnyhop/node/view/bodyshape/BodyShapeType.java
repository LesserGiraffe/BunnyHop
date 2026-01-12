package net.seapanda.bunnyhop.node.view.bodyshape;

import java.util.HashMap;
import java.util.Map;

/**
 * ボディの形の識別子を定義した列挙型.
 */
public enum BodyShapeType {
  BODY_SHAPE_ROUND_RECT("ROUND_RECT", new BodyRoundRect()),
  BODY_SHAPE_NONE("NONE", new BodyNone());

  public final String name;
  public final BodyShape shape;
  private static final Map<String, BodyShapeType> shapeNameToBodyShape =
      new HashMap<>() {{
        put(BodyShapeType.BODY_SHAPE_ROUND_RECT.name, BodyShapeType.BODY_SHAPE_ROUND_RECT);
        put(BodyShapeType.BODY_SHAPE_NONE.name, BodyShapeType.BODY_SHAPE_NONE);
      }};

  BodyShapeType(String shapeName, BodyShape shape) {
    name = shapeName;
    this.shape = shape;
  }

  /**
   * ボディ名からボディの識別子を取得する.
   */
  public static BodyShapeType getByName(String name) {
    return BodyShapeType.shapeNameToBodyShape.get(name);
  }
}
