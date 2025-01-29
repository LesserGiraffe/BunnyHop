/*
 * Copyright 2017 K.Koike
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.seapanda.bunnyhop.model.node.attribute;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.service.BhService;
import org.w3c.dom.Element;

/**
 * ノードが定義された xml の Node タグが持つ属性一覧を保持するクラス.
 *
 * @author K.Koike
 */
public record BhNodeAttributes(
    BhNodeId bhNodeId,
    String name,
    BhNodeVersion version,
    String nodeStyleId,
    String onMovedFromChildToWs,
    String onMovedToChild,
    String onTextChecking,
    String onTextFormatting,
    String onDeletionRequested,
    String onCutRequested,
    String onCopyRequested,
    String onChildReplaced,
    String onFelloNodesCreating,
    String onCompileErrorChecking,
    String onTextOptionsCreating,
    String onTemplateCreated,
    String onDragStarted,
    String initialText) {

  private static Pattern escapeLbrace = Pattern.compile(Pattern.quote("\\{"));
  private static Pattern escapeRbrace = Pattern.compile(Pattern.quote("\\}"));
  /** `\\...\$` */
  private static Pattern escapeDollar = Pattern.compile("^(\\\\)+\\$");
  /** テキスト DB 参照パターン `${a}{b}...{z}` */
  private static Pattern embeded =
      Pattern.compile("^\\$(\\{(((\\\\\\{)|(\\\\\\})|[^\\{\\}])*)\\})+$");
  /** テキスト DB 参照パターン `${a}{b}...{z}` の (a, b, ..., z) を取り出す用. */
  private static Pattern contents =
      Pattern.compile("\\{((?:(?:\\\\\\{)|(?:\\\\\\})|[^\\{\\}])*)\\}");

  /**
   * Node タグが持つ属性一覧を読み取る.
   *
   * @param elem Node タグを表すオブジェクト
   */
  public static BhNodeAttributes of(Element elem) {
    var bhNodeId = BhNodeId.of(elem.getAttribute(BhConstants.BhModelDef.ATTR_BH_NODE_ID));
    String name = elem.getAttribute(BhConstants.BhModelDef.ATTR_NAME);
    String nodeStyleId = elem.getAttribute(BhConstants.BhModelDef.ATTR_NODE_STYLE_ID);
    String onMovedFromChildToWs =
        elem.getAttribute(BhConstants.BhModelDef.ATTR_ON_MOVED_FROM_CHILD_TO_WS);
    String onMovedToChild = elem.getAttribute(BhConstants.BhModelDef.ATTR_ON_MOVED_TO_CHILD);
    String onTextChecking = elem.getAttribute(BhConstants.BhModelDef.ATTR_ON_TEXT_CHECKING);
    String onTextFormatting = elem.getAttribute(BhConstants.BhModelDef.ATTR_ON_TEXT_FORMATTING);
    String onDeletionRequested =
        elem.getAttribute(BhConstants.BhModelDef.ATTR_ON_DELETION_REQUESTED);
    String onCutRequested = elem.getAttribute(BhConstants.BhModelDef.ATTR_ON_CUT_REQUESTED);
    String onCopyRequested = elem.getAttribute(BhConstants.BhModelDef.ATTR_ON_COPY_REQUESTED);
    String onChildReplaced = elem.getAttribute(BhConstants.BhModelDef.ATTR_ON_CHILD_REPLACED);
    String onCompanionNodesCreating =
        elem.getAttribute(BhConstants.BhModelDef.ATTR_ON_COMPANION_NODES_CREATING);
    String onCompileErrorChecking =
        elem.getAttribute(BhConstants.BhModelDef.ATTR_ON_COMPILE_ERROR_CHECKING);
    String onTextOptionsCreating = 
        elem.getAttribute(BhConstants.BhModelDef.ATTR_ON_TEST_OPTIONS_CREATING);
    String onTemplateCreated = elem.getAttribute(BhConstants.BhModelDef.ATTR_ON_TEMPLATE_CREATED);
    String onDragStarted = elem.getAttribute(BhConstants.BhModelDef.ATTR_ON_DRAG_STARTED);
    String initialText = getInitialText(elem);
    var version = BhNodeVersion.of(elem.getAttribute(BhConstants.BhModelDef.ATTR_VERSION));

    return new BhNodeAttributes(
        bhNodeId,
        name,
        version,
        nodeStyleId,
        onMovedFromChildToWs,
        onMovedToChild,
        onTextChecking,
        onTextFormatting,
        onDeletionRequested,
        onCutRequested,
        onCopyRequested,
        onChildReplaced,
        onCompanionNodesCreating,
        onCompileErrorChecking,
        onTextOptionsCreating,
        onTemplateCreated,
        onDragStarted,
        initialText);
  }

  private static String getInitialText(Element elem) {
    String value = elem.getAttribute(BhConstants.BhModelDef.ATTR_INITIAL_TEXT);
    if (embeded.matcher(value).find()) {
      Matcher matcher = contents.matcher(value);
      List<String> textId = matcher.results().map(
          result -> {  
            String tmp = escapeLbrace.matcher(result.group(1)).replaceAll("{");
            return escapeRbrace.matcher(tmp).replaceAll("}");
          }).toList();
      return BhService.textDb().get(textId);
    }

    if (escapeDollar.matcher(value).find()) {
      return value.substring(1);
    }

    return value;
  }
}
