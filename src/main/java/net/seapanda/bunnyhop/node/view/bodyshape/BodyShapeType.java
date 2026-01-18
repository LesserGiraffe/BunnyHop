package net.seapanda.bunnyhop.node.view.bodyshape;

import java.util.HashMap;
import java.util.Map;

/**
 * ボディの形の識別子を定義した列挙型.
 */
public enum BodyShapeType {
  ROUND_RECT("RoundRect", new BodyRoundRect()),
  NONE("None", new BodyNone());

  public final String name;
  public final BodyShape shape;
  private static final Map<String, BodyShapeType> shapeNameToBodyShape =
      new HashMap<>() {{
          put(BodyShapeType.ROUND_RECT.name, BodyShapeType.ROUND_RECT);
          put(BodyShapeType.NONE.name, BodyShapeType.NONE);
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
