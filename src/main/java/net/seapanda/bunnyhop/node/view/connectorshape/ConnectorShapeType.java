package net.seapanda.bunnyhop.node.view.connectorshape;

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
  private static final Map<String, ConnectorShapeType> shapeNameToConnectorShape = Map.ofEntries(
      Map.entry(ConnectorShapeType.ARROW.name, ConnectorShapeType.ARROW),
      Map.entry(ConnectorShapeType.CHAR_T.name, ConnectorShapeType.CHAR_T),
      Map.entry(ConnectorShapeType.CHAR_U.name, ConnectorShapeType.CHAR_U),
      Map.entry(ConnectorShapeType.CHAR_V.name, ConnectorShapeType.CHAR_V),
      Map.entry(ConnectorShapeType.CROSS.name, ConnectorShapeType.CROSS),
      Map.entry(ConnectorShapeType.DIAMOND.name, ConnectorShapeType.DIAMOND),
      Map.entry(ConnectorShapeType.HEXAGON.name, ConnectorShapeType.HEXAGON),
      Map.entry(ConnectorShapeType.INV_PENTAGON.name, ConnectorShapeType.INV_PENTAGON),
      Map.entry(ConnectorShapeType.INV_TRAPEZOID.name, ConnectorShapeType.INV_TRAPEZOID),
      Map.entry(ConnectorShapeType.INV_TRIANGLE.name, ConnectorShapeType.INV_TRIANGLE),
      Map.entry(ConnectorShapeType.NONE.name, ConnectorShapeType.NONE),
      Map.entry(ConnectorShapeType.OCTAGON.name, ConnectorShapeType.OCTAGON),
      Map.entry(ConnectorShapeType.PENTAGON.name, ConnectorShapeType.PENTAGON),
      Map.entry(ConnectorShapeType.SQUARE.name, ConnectorShapeType.SQUARE),
      Map.entry(ConnectorShapeType.STAR.name, ConnectorShapeType.STAR),
      Map.entry(ConnectorShapeType.TRAPEZOID.name, ConnectorShapeType.TRAPEZOID),
      Map.entry(ConnectorShapeType.TRIANGLE.name, ConnectorShapeType.TRIANGLE),
      Map.entry(ConnectorShapeType.LIGHTNING.name, ConnectorShapeType.LIGHTNING),
      Map.entry(ConnectorShapeType.SANDGLASS.name, ConnectorShapeType.SANDGLASS));

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
