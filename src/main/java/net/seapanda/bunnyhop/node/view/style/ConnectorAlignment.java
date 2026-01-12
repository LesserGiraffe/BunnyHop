package net.seapanda.bunnyhop.node.view.style;

import net.seapanda.bunnyhop.common.configuration.BhConstants;

/**
 * コネクタをそろえる部分.
 */
public enum ConnectorAlignment {

  /**
   * コネクタの端をノードボディの端に合わせる.
   */
  CENTER(BhConstants.NodeStyleDef.VAL_CENTER),
  /**
   * コネクタの中央をノードボディの中央に合わせる.
   */
  EDGE(BhConstants.NodeStyleDef.VAL_EDGE);

  private final String name;

  ConnectorAlignment(String name) {
    this.name = name;
  }

  /**
   * タイプ名から列挙子を得る.
   */
  public static ConnectorAlignment of(String name) {
    for (var val : ConnectorAlignment.values()) {
      if (val.getName().equals(name)) {
        return val;
      }
    }
    throw new IllegalArgumentException(
        "Unknown %s  (%s)".formatted(ConnectorAlignment.class.getSimpleName(), name));
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }
}
