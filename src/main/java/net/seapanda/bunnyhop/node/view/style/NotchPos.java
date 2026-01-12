package net.seapanda.bunnyhop.node.view.style;

import net.seapanda.bunnyhop.common.configuration.BhConstants;

/**
 * 切り欠きの位置.
 */
public enum NotchPos {

  RIGHT(BhConstants.NodeStyleDef.VAL_RIGHT),
  BOTTOM(BhConstants.NodeStyleDef.VAL_BOTTOM);

  private final String name;

  NotchPos(String name) {
    this.name = name;
  }

  /**
   * タイプ名から列挙子を得る.
   */
  public static NotchPos of(String name) {
    for (var val : NotchPos.values()) {
      if (val.getName().equals(name)) {
        return val;
      }
    }
    throw new IllegalArgumentException(
        "Unknown %s  (%s)".formatted(NotchPos.class.getSimpleName(), name));
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }
}
