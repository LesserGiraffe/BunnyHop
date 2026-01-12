package net.seapanda.bunnyhop.node.view.style;

import net.seapanda.bunnyhop.common.configuration.BhConstants;

/**
 * コネクタの位置.
 */
public enum ConnectorPos {

  LEFT(BhConstants.NodeStyleDef.VAL_LEFT),
  TOP(BhConstants.NodeStyleDef.VAL_TOP);

  private final String name;

  ConnectorPos(String name) {
    this.name = name;
  }

  /**
   * タイプ名から列挙子を得る.
   */
  public static ConnectorPos of(String name) {
    for (var val : ConnectorPos.values()) {
      if (val.getName().equals(name)) {
        return val;
      }
    }
    throw new IllegalArgumentException(
        "Unknown %s  (%s)".formatted(ConnectorPos.class.getSimpleName(), name));
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }
}
