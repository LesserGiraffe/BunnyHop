package net.seapanda.bunnyhop.model.factory;

import net.seapanda.bunnyhop.model.node.BhNode;

/**
 * {@link BhNode} の作成時に参照するデータの構築に失敗したことを表す例外.
 *
 * @author K.Koike
 */
public class NodeTemplateConstructionException extends Exception {
  public NodeTemplateConstructionException(String msg) {
    super(msg);
  }
}
