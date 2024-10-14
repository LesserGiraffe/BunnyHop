package net.seapanda.bunnyhop.model.node.attribute;

import net.seapanda.bunnyhop.common.constant.BhConstants;
import net.seapanda.bunnyhop.common.tools.Util;

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
    throw new IllegalArgumentException(
        Util.INSTANCE.getCurrentMethodName() + " - unknown BhNode type name  " + typeName);
  }

  public String getName() {
    return typeName;
  }

  @Override
  public String toString() {
    return typeName;
  }
}
