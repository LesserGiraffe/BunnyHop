package net.seapanda.bunnyhop.service.accesscontrol;

import net.seapanda.bunnyhop.service.undo.UserOperation;

/**
 * トランザクションに紐づく情報.
 *
 * @param userOpe undo/redo 用コマンドオブジェクト
 */
public record TransactionContext(UserOperation userOpe) {
}
