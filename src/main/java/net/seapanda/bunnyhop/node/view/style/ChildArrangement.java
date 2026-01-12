package net.seapanda.bunnyhop.node.view.style;

import net.seapanda.bunnyhop.common.configuration.BhConstants;

/**
 * 子要素の描画方向.
 */
public enum ChildArrangement {

  ROW(BhConstants.NodeStyleDef.VAL_ROW),
  COLUMN(BhConstants.NodeStyleDef.VAL_COLUMN);

  private final String name;

  ChildArrangement(String name) {
    this.name = name;
  }

  /**
   * タイプ名から列挙子を得る.
   */
  public static ChildArrangement of(String name) {
    for (var val : ChildArrangement.values()) {
      if (val.getName().equals(name)) {
        return val;
      }
    }
    throw new IllegalArgumentException(
        "Unknown %s  (%s)".formatted(ChildArrangement.class.getSimpleName(), name));
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }
}
