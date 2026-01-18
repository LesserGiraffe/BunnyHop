package net.seapanda.bunnyhop.node.view.connectorshape;

import java.util.HashMap;
import java.util.Map;

/**
 * コネクタの形の識別子を定義した列挙型.
 */
public enum ConnectorShapeType {
  ARROW("Arrow", new ConnectorArrow()),
  CHAR_T("CharT", new ConnectorCharT()),
  CHAR_U("CharU", new ConnectorCharU()),
  CHAR_V("CharV", new ConnectorCharV()),
  CROSS("Cross", new ConnectorCross()),
  DIAMOND("Diamond", new ConnectorDiamond()),
  HEXAGON("Hexagon", new ConnectorHexagon()),
  INV_PENTAGON("InvPentagon", new ConnectorInvPentagon()),
  INV_TRAPEZOID("InvTrapezoid", new ConnectorInvTrapezoid()),
  INV_TRIANGLE("InvTriangle", new ConnectorInvTriangle()),
  NONE("None", new ConnectorNone()),
  OCTAGON("Octagon", new ConnectorOctagon()),
  PENTAGON("Pentagon", new ConnectorPentagon()),
  SQUARE("Square", new ConnectorSquare()),
  STAR("Star", new ConnectorStar()),
  TRAPEZOID("Trapezoid", new ConnectorTrapezoid()),
  TRIANGLE("Triangle", new ConnectorTriangle()),
  LIGHTNING("Lightning", new ConnectorLightning()),
  SANDGLASS("Sandglass", new ConnectorSandglass());

  public final String name;
  public final ConnectorShape shape;
  private static final Map<String, ConnectorShapeType> shapeNameToConnectorShape =
      new HashMap<>() {{
          put(ConnectorShapeType.ARROW.name, ConnectorShapeType.ARROW);
          put(ConnectorShapeType.CHAR_T.name, ConnectorShapeType.CHAR_T);
          put(ConnectorShapeType.CHAR_U.name, ConnectorShapeType.CHAR_U);
          put(ConnectorShapeType.CHAR_V.name, ConnectorShapeType.CHAR_V);
          put(ConnectorShapeType.CROSS.name, ConnectorShapeType.CROSS);
          put(ConnectorShapeType.DIAMOND.name, ConnectorShapeType.DIAMOND);
          put(ConnectorShapeType.HEXAGON.name, ConnectorShapeType.HEXAGON);
          put(ConnectorShapeType.INV_PENTAGON.name, ConnectorShapeType.INV_PENTAGON);
          put(ConnectorShapeType.INV_TRAPEZOID.name, ConnectorShapeType.INV_TRAPEZOID);
          put(ConnectorShapeType.INV_TRIANGLE.name, ConnectorShapeType.INV_TRIANGLE);
          put(ConnectorShapeType.NONE.name, ConnectorShapeType.NONE);
          put(ConnectorShapeType.OCTAGON.name, ConnectorShapeType.OCTAGON);
          put(ConnectorShapeType.PENTAGON.name, ConnectorShapeType.PENTAGON);
          put(ConnectorShapeType.SQUARE.name, ConnectorShapeType.SQUARE);
          put(ConnectorShapeType.STAR.name, ConnectorShapeType.STAR);
          put(ConnectorShapeType.TRAPEZOID.name, ConnectorShapeType.TRAPEZOID);
          put(ConnectorShapeType.TRIANGLE.name, ConnectorShapeType.TRIANGLE);
          put(ConnectorShapeType.LIGHTNING.name, ConnectorShapeType.LIGHTNING);
          put(ConnectorShapeType.SANDGLASS.name, ConnectorShapeType.SANDGLASS);
        }};

  ConnectorShapeType(String shapeName, ConnectorShape shape) {
    name = shapeName;
    this.shape = shape;
  }

  /**
   * コネクタ名からコネクタの識別子を取得する.
   */
  public static ConnectorShapeType getByName(String name) {
    return ConnectorShapeType.shapeNameToConnectorShape.get(name);
  }
}
