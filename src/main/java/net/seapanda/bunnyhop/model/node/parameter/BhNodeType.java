package net.seapanda.bunnyhop.model.node.parameter;

import net.seapanda.bunnyhop.common.BhConstants;

/** {@link BhNode} のタイプ. */
public enum BhNodeType {
  
  CONNECTIVE(BhConstants.BhModelDef.ATTR_VAL_CONNECTIVE),
  TEXT(BhConstants.BhModelDef.ATTR_VAL_TEXT);
  private final String typeName;

  private BhNodeType(String typeName) {
    this.typeName = typeName;
  }

  /** タイプ名から列挙子を得る. */
  public static BhNodeType toType(String typeName) {
    for (var type : BhNodeType.values()) {
      if (type.getName().equals(typeName)) {
        return type;
      }
    }
    throw new IllegalArgumentException("unknown BhNode type name " + typeName);
  }

  public String getName() {
    return typeName;
  }

  @Override
  public String toString() {
    return typeName;
  }
}
