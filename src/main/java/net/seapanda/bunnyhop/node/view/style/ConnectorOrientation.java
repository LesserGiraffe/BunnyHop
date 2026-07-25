package net.seapanda.bunnyhop.node.view.style;

import net.seapanda.bunnyhop.common.configuration.BhConstants;

/**
 * コネクタの位置.
 */
public enum ConnectorOrientation {

  LEFT(BhConstants.NodeStyleDef.VAL_LEFT),
  TOP(BhConstants.NodeStyleDef.VAL_TOP);

  private final String name;

  ConnectorOrientation(String name) {
    this.name = name;
  }

  /**
   * タイプ名から列挙子を得る.
   */
  public static ConnectorOrientation of(String name) {
    for (var val : ConnectorOrientation.values()) {
      if (val.getName().equals(name)) {
        return val;
      }
    }
    throw new IllegalArgumentException(
        "Unknown %s  (%s)".formatted(ConnectorOrientation.class.getSimpleName(), name));
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }
}
