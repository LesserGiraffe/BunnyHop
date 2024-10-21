package net.seapanda.bunnyhop.model.node.connective;

import java.io.Serializable;
import net.seapanda.bunnyhop.model.node.imitation.ImitCnctPosId;
import net.seapanda.bunnyhop.model.node.imitation.ImitationId;

/**
 * コネクタ生成時のパラメータ.
 *
 * @param name コネクタ名
 * @param fixed 子ノードを固定ノードにする場合 true.  このパラメータの指定がない場合は null にすること.
 * @param imitationId イミテーションID (作成するイミテーションの識別子)
 * @param imitCnctPoint イミテーション接続位置の識別子
 */
public record ConnectorInstantiationParams(
    String name,
    Boolean fixed,
    ImitationId imitationId,
    ImitCnctPosId imitCnctPoint) implements Serializable {}
