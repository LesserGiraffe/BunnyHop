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

package net.seapanda.bunnyhop.node.view.style;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.seapanda.bunnyhop.node.model.parameter.BhNodeViewStyleId;
import net.seapanda.bunnyhop.node.view.bodyshape.BodyShapeType;
import net.seapanda.bunnyhop.node.view.component.ComponentType;
import net.seapanda.bunnyhop.node.view.connectorshape.ConnectorShapeType;

/**
 * {@link BhNodeViewStyle} の情報を部分的に格納するためのクラス.
 *
 * <p>このオブジェクトが持つノードスタイルのパラメータから
 * {@link BhNodeViewStyle} オブジェクトを生成する機能 ({@link #build}) を提供する.
 *
 * <p>このオブジェクトには下位の {@link BhNodeViewStyleSnippet} オブジェクト (サブスニペット) を追加することができる.
 * 追加されたサブスニペットは, {@link BhNodeViewStyle} オブジェクトを生成する際に使用される.
 * このオブジェクトとサブスニペットが, ノードスタイルの特定のパラメータ (例えばコネクタの幅など) に対し異なる値を持つ場合,
 * このオブジェクトの値が使用される.
 * 複数のサブスニペットが特定のパラメータに対し異なる値を持つ場合, 後から登録したサブスニペットの値が使用される.
 *
 * @author K.Koike
 */
class BhNodeViewStyleSnippet {

  private final List<BhNodeViewStyleSnippet> subSnippets = new LinkedList<>();

  Double paddingTop = null;
  Double paddingBottom = null;
  Double paddingLeft = null;
  Double paddingRight = null;
  BodyShapeType bodyShape = null;
  ConnectorPos connectorPos = null;
  Double connectorShift = null;
  Double connectorWidth = null;
  Double connectorHeight = null;
  ConnectorAlignment connectorAlignment = null;
  ConnectorShapeType connectorShape = null;
  ConnectorShapeType connectorShapeFixed = null;
  NotchPos notchPos = null;
  Double notchWidth = null;
  Double notchHeight = null;
  ConnectorShapeType notchShape =  null;
  ConnectorShapeType notchShapeFixed = null;
  Double connectorBoundsRate = null;
  String[] cssClasses = null;
  ComponentType component = null;
  ChildArrangement baseArrangement = null;
  final ConnectiveSnippet connective = new ConnectiveSnippet();
  final CommonPartSnippet commonPart = new CommonPartSnippet();
  final SpecificPartSnippet specificPart = new SpecificPartSnippet();
  final TextFieldSnippet textField = new TextFieldSnippet();
  final LabelSnippet label = new LabelSnippet();
  final ComboBoxSnippet comboBox = new ComboBoxSnippet();
  final TextAreaSnippet textArea = new TextAreaSnippet();

  /**
   * このオブジェクトにサブスニペットを追加する.
   *
   * @param snippet 追加するサブスニペット
   */
  void addSubSnippet(BhNodeViewStyleSnippet snippet) {
    // 後から追加したものほどリストの手前に追加
    subSnippets.addFirst(snippet);
  }

  /**
   * このオブジェクトにサブスニペットを追加する.
   *
   * @param snippets 追加するサブスニペットのリスト
   */
  void addSubSnippets(SequencedCollection<BhNodeViewStyleSnippet> snippets) {
    snippets.forEach(this::addSubSnippet);
  }

  /**
   * このオブジェクトとサブスニペットから {@link BhNodeViewStyle} オブジェクトを作成する.
   *
   * @param id 作成する {@link BhNodeViewStyle} の ID
   * @return {@link BhNodeViewStyle} オブジェクト
   */
  BhNodeViewStyle build(BhNodeViewStyleId id) {
    var style = new BhNodeViewStyle();
    style.id = id;
    style.paddingTop = findPaddingTop(style.paddingTop);
    style.paddingBottom = findPaddingBottom(style.paddingBottom);
    style.paddingLeft = findPaddingLeft(style.paddingLeft);
    style.paddingRight = findPaddingRight(style.paddingRight);
    style.bodyShape = findBodyShape(style.bodyShape);
    style.connectorPos = findConnectorPos(style.connectorPos);
    style.connectorShift = findConnectorShift(style.connectorShift);
    style.connectorWidth = findConnectorWidth(style.connectorWidth);
    style.connectorHeight = findConnectorHeight(style.connectorHeight);
    style.connectorAlignment = findConnectorAlignment(style.connectorAlignment);
    style.connectorShape = findConnectorShape(style.connectorShape);
    style.connectorShapeFixed = findConnectorShapeFixed(style.connectorShapeFixed);
    style.notchPos = findNotchPos(style.notchPos);
    style.notchWidth = findNotchWidth(style.notchWidth);
    style.notchHeight = findNotchHeight(style.notchHeight);
    style.notchShape = findNotchShape(style.notchShape);
    style.notchShapeFixed = findNotchShapeFixed(style.notchShapeFixed);
    style.connectorBoundsRate = findConnectorBoundsRate(style.connectorBoundsRate);
    style.cssClasses = findCssClasses(style.cssClasses);
    style.component = findComponent(style.component);
    style.baseArrangement = findBaseArrangement(style.baseArrangement);
    connective.populateStyle(style.connective, snippet -> connective);
    commonPart.populateStyle(style.commonPart, snippet -> commonPart);
    specificPart.populateStyle(style.specificPart, snippet -> specificPart);
    textField.populateStyle(style.textField, snippet -> textField);
    label.populateStyle(style.label, snippet -> label);
    comboBox.populateStyle(style.comboBox, snippet -> comboBox);
    textArea.populateStyle(style.textArea, snippet -> textArea);
    return style;
  }

  /**
   * このオブジェクトとサブスニペットから null でない {@link #paddingTop} を探す.
   * 見つからない場合は {@code defaultVal} を返す.
   *
   * <p>このオブジェクトの {@link #paddingTop} が null でない場合, その値を返す.
   * {@link #subSnippets} を先頭から走査して, その要素から再帰的に辿れる
   * {@link BhNodeViewStyleSnippet} が持つ null でない {@link #paddingTop} があればそれを返す.
   */
  private double findPaddingTop(double defaultVal) {
    return find(paddingTop, snippet -> snippet.paddingTop).orElse(defaultVal);
  }

  private double findPaddingBottom(double defaultVal) {
    return find(paddingBottom, snippet -> snippet.paddingBottom).orElse(defaultVal);
  }

  private double findPaddingLeft(double defaultVal) {
    return find(paddingLeft, snippet -> snippet.paddingLeft).orElse(defaultVal);
  }

  private double findPaddingRight(double defaultVal) {
    return find(paddingRight, snippet -> snippet.paddingRight).orElse(defaultVal);
  }

  private BodyShapeType findBodyShape(BodyShapeType defaultVal) {
    return find(bodyShape, snippet -> snippet.bodyShape).orElse(defaultVal);
  }

  private ConnectorPos findConnectorPos(ConnectorPos defaultVal) {
    return find(connectorPos, snippet -> snippet.connectorPos).orElse(defaultVal);
  }

  private double findConnectorShift(double defaultVal) {
    return find(connectorShift, snippet -> snippet.connectorShift).orElse(defaultVal);
  }

  private double findConnectorWidth(double defaultVal) {
    return find(connectorWidth, snippet -> snippet.connectorWidth).orElse(defaultVal);
  }

  private double findConnectorHeight(double defaultVal) {
    return find(connectorHeight, snippet -> snippet.connectorHeight).orElse(defaultVal);
  }

  private ConnectorAlignment findConnectorAlignment(ConnectorAlignment defaultVal) {
    return find(connectorAlignment, snippet -> snippet.connectorAlignment).orElse(defaultVal);
  }

  private ConnectorShapeType findConnectorShape(ConnectorShapeType defaultVal) {
    return find(connectorShape, snippet -> snippet.connectorShape).orElse(defaultVal);
  }

  private ConnectorShapeType findConnectorShapeFixed(ConnectorShapeType defaultVal) {
    return find(connectorShapeFixed, snippet -> snippet.connectorShapeFixed).orElse(defaultVal);
  }

  private NotchPos findNotchPos(NotchPos defaultVal) {
    return find(notchPos, snippet -> snippet.notchPos).orElse(defaultVal);
  }

  private double findNotchWidth(double defaultVal) {
    return find(notchWidth, snippet -> snippet.notchWidth).orElse(defaultVal);
  }

  private double findNotchHeight(double defaultVal) {
    return find(notchHeight, snippet -> snippet.notchHeight).orElse(defaultVal);
  }

  private ConnectorShapeType findNotchShape(ConnectorShapeType defaultVal) {
    return find(notchShape, snippet -> snippet.notchShape).orElse(defaultVal);
  }

  private ConnectorShapeType findNotchShapeFixed(ConnectorShapeType defaultVal) {
    return find(notchShapeFixed, snippet -> snippet.notchShapeFixed).orElse(defaultVal);
  }

  private double findConnectorBoundsRate(double defaultVal) {
    return find(connectorBoundsRate, snippet -> snippet.connectorBoundsRate).orElse(defaultVal);
  }

  private String[] findCssClasses(String[] defaultVal) {
    return find(cssClasses, snippet -> snippet.cssClasses).orElse(defaultVal);
  }

  private ComponentType findComponent(ComponentType defaultVal) {
    return find(component, snippet -> snippet.component).orElse(defaultVal);
  }

  private ChildArrangement findBaseArrangement(ChildArrangement defaultVal) {
    return find(baseArrangement, snippet -> snippet.baseArrangement).orElse(defaultVal);
  }

  private <T> Optional<T> find(T obj, Function<BhNodeViewStyleSnippet, T> getter) {
    if (obj != null) {
      return Optional.of(obj);
    }
    for (var subSnippet : subSnippets) {
      T val = getter.apply(subSnippet);
      if (val != null) {
        return Optional.of(val);
      }
    }
    return Optional.empty();
  }


  class TextFieldSnippet {
    Double minWidth = null;
    Boolean editable = null;
    String cssClass = null;

    private void populateStyle(
        BhNodeViewStyle.TextField style,
        Function<BhNodeViewStyleSnippet, TextFieldSnippet> getter) {
      style.minWidth = findMinWidth(style.minWidth, getter);
      style.editable = findEditable(style.editable, getter);
      style.cssClass = findCssClass(style.cssClass, getter);
    }

    private double findMinWidth(
        double defaultVal, Function<BhNodeViewStyleSnippet, TextFieldSnippet> getter) {
      return find(minWidth, snippet -> getter.apply(snippet).minWidth).orElse(defaultVal);
    }

    private boolean findEditable(
        boolean defaultVal, Function<BhNodeViewStyleSnippet, TextFieldSnippet> getter) {
      return find(editable, snippet -> getter.apply(snippet).editable).orElse(defaultVal);
    }

    private String findCssClass(
        String defaultVal, Function<BhNodeViewStyleSnippet, TextFieldSnippet> getter) {
      return find(cssClass, snippet -> getter.apply(snippet).cssClass).orElse(defaultVal);
    }
  }

  class LabelSnippet {
    String cssClass = null;

    private void populateStyle(
        BhNodeViewStyle.Label style, Function<BhNodeViewStyleSnippet, LabelSnippet> getter) {
      style.cssClass = findCssClass(style.cssClass, getter);
    }

    private String findCssClass(
        String defaultVal, Function<BhNodeViewStyleSnippet, LabelSnippet> getter) {
      return find(cssClass, snippet -> getter.apply(snippet).cssClass).orElse(defaultVal);
    }
  }

  class ComboBoxSnippet {
    String cssClass = null;

    private void populateStyle(
        BhNodeViewStyle.ComboBox style, Function<BhNodeViewStyleSnippet, ComboBoxSnippet> getter) {
      style.cssClass = findCssClass(style.cssClass, getter);
    }

    private String findCssClass(
        String defaultVal, Function<BhNodeViewStyleSnippet, ComboBoxSnippet> getter) {
      return find(cssClass, snippet -> getter.apply(snippet).cssClass).orElse(defaultVal);
    }
  }

  class ConnectiveSnippet {
    ArrangementSnippet inner = null;
    ArrangementSnippet outer = null;

    private void populateStyle(
        BhNodeViewStyle.Connective style,
        Function<BhNodeViewStyleSnippet, ConnectiveSnippet> getter) {
      style.inner = findInner(style.inner, getter);
      style.outer = findOuter(style.outer, getter);
    }

    private BhNodeViewStyle.Arrangement findInner(
        BhNodeViewStyle.Arrangement defaultVal,
        Function<BhNodeViewStyleSnippet, ConnectiveSnippet> getter) {
      return find(inner, snippet -> getter.apply(snippet).inner)
          .map(arrangement -> arrangement.populateStyle(new BhNodeViewStyle.Arrangement()))
          .orElse(defaultVal);
    }

    private BhNodeViewStyle.Arrangement findOuter(
        BhNodeViewStyle.Arrangement defaultVal,
        Function<BhNodeViewStyleSnippet, ConnectiveSnippet> getter) {
      return find(outer, snippet -> getter.apply(snippet).outer)
          .map(arrangement -> arrangement.populateStyle(new BhNodeViewStyle.Arrangement()))
          .orElse(defaultVal);
    }
  }

  static class ArrangementSnippet {
    Double space = null;
    Double paddingTop = null;
    Double paddingRight = null;
    Double paddingBottom = null;
    Double paddingLeft = null;
    ChildArrangement arrangement = null;
    final List<String> cnctrNames = new ArrayList<>();
    final List<ArrangementSnippet> subGroups = new ArrayList<>();

    private BhNodeViewStyle.Arrangement populateStyle(BhNodeViewStyle.Arrangement style) {
      // ArrangementSnippet が保持するパラメータは, 複数のスニペットを合成するのではなく,
      // 単一のオブジェクトを不可分な値として使用する.
      style.space = (space == null) ? style.space : space;
      style.paddingTop = (paddingTop == null) ? style.paddingTop : paddingTop;
      style.paddingRight = (paddingRight == null) ? style.paddingRight : paddingRight;
      style.paddingBottom = (paddingBottom == null) ? style.paddingBottom : paddingBottom;
      style.paddingLeft = (paddingLeft == null) ? style.paddingLeft : paddingLeft;
      style.arrangement = (arrangement == null) ? style.arrangement : arrangement;
      style.cnctrNames = new ArrayList<>(cnctrNames);
      style.subGroups = subGroups.stream()
          .map(subGroup -> subGroup.populateStyle(new BhNodeViewStyle.Arrangement()))
          .collect(Collectors.toCollection(ArrayList::new));
      return style;
    }
  }

  class TextAreaSnippet {
    Double minWidth = null;
    Double minHeight = null;
    Boolean editable = null;
    String cssClass = null;

    private void populateStyle(
        BhNodeViewStyle.TextArea style, Function<BhNodeViewStyleSnippet, TextAreaSnippet> getter) {
      style.minWidth = findMinWidth(style.minWidth, getter);
      style.minHeight = findMinHeight(style.minHeight, getter);
      style.editable = findEditable(style.editable, getter);
      style.cssClass = findCssClass(style.cssClass, getter);
    }

    private double findMinWidth(
        double defaultVal, Function<BhNodeViewStyleSnippet, TextAreaSnippet> getter) {
      return find(minWidth, snippet -> getter.apply(snippet).minWidth).orElse(defaultVal);
    }

    private double findMinHeight(
        double defaultVal, Function<BhNodeViewStyleSnippet, TextAreaSnippet> getter) {
      return find(minHeight, snippet -> getter.apply(snippet).minHeight).orElse(defaultVal);
    }

    private boolean findEditable(
        boolean defaultVal, Function<BhNodeViewStyleSnippet, TextAreaSnippet> getter) {
      return find(editable, snippet -> getter.apply(snippet).editable).orElse(defaultVal);
    }

    private String findCssClass(
        String defaultVal, Function<BhNodeViewStyleSnippet, TextAreaSnippet> getter) {
      return find(cssClass, snippet -> getter.apply(snippet).cssClass).orElse(defaultVal);
    }
  }

  class ButtonSnippet {
    String cssClass = null;

    private void populateStyle(
        BhNodeViewStyle.Button style, Function<BhNodeViewStyleSnippet, ButtonSnippet> getter) {
      style.cssClass = findCssClass(style.cssClass, getter);
    }

    private String findCssClass(
        String defaultVal, Function<BhNodeViewStyleSnippet, ButtonSnippet> getter) {
      return find(cssClass, snippet -> getter.apply(snippet).cssClass).orElse(defaultVal);
    }
  }

  class BreakpointSnippet {
    Double radius = null;
    String cssClass = null;

    private void populateStyle(
        BhNodeViewStyle.Breakpoint style,
        Function<BhNodeViewStyleSnippet, BreakpointSnippet> getter) {
      style.radius = findRadius(style.radius, getter);
      style.cssClass = findCssClass(style.cssClass, getter);
    }

    private double findRadius(
        double defaultVal, Function<BhNodeViewStyleSnippet, BreakpointSnippet> getter) {
      return find(radius, snippet -> getter.apply(snippet).radius).orElse(defaultVal);
    }

    private String findCssClass(
        String defaultVal, Function<BhNodeViewStyleSnippet, BreakpointSnippet> getter) {
      return find(cssClass, snippet -> getter.apply(snippet).cssClass).orElse(defaultVal);
    }
  }

  class ExecStepMarkSnippet {
    Double size = null;
    String cssClass = null;

    private void populateStyle(
        BhNodeViewStyle.ExecStepMark style,
        Function<BhNodeViewStyleSnippet, ExecStepMarkSnippet> getter) {
      style.size = findSize(style.size, getter);
      style.cssClass = findCssClass(style.cssClass, getter);
    }

    private double findSize(
        double defaultVal, Function<BhNodeViewStyleSnippet, ExecStepMarkSnippet> getter) {
      return find(size, snippet -> getter.apply(snippet).size).orElse(defaultVal);
    }

    private String findCssClass(
        String defaultVal, Function<BhNodeViewStyleSnippet, ExecStepMarkSnippet> getter) {
      return find(cssClass, snippet -> getter.apply(snippet).cssClass).orElse(defaultVal);
    }
  }

  class CorruptionMarkSnippet {
    Double size = null;
    String cssClass = null;

    private void populateStyle(
        BhNodeViewStyle.CorruptionMark style,
        Function<BhNodeViewStyleSnippet, CorruptionMarkSnippet> getter) {
      style.size = findSize(style.size, getter);
      style.cssClass = findCssClass(style.cssClass, getter);
    }

    private double findSize(
        double defaultVal, Function<BhNodeViewStyleSnippet, CorruptionMarkSnippet> getter) {
      return find(size, snippet -> getter.apply(snippet).size).orElse(defaultVal);
    }

    private String findCssClass(
        String defaultVal, Function<BhNodeViewStyleSnippet, CorruptionMarkSnippet> getter) {
      return find(cssClass, snippet -> getter.apply(snippet).cssClass).orElse(defaultVal);
    }
  }

  class EntryPointMarkSnippet {
    Double radius = null;
    String cssClass = null;

    private void populateStyle(
        BhNodeViewStyle.EntryPointMark style,
        Function<BhNodeViewStyleSnippet, EntryPointMarkSnippet> getter) {
      style.radius = findRadius(style.radius, getter);
      style.cssClass = findCssClass(style.cssClass, getter);
    }

    private double findRadius(
        double defaultVal, Function<BhNodeViewStyleSnippet, EntryPointMarkSnippet> getter) {
      return find(radius, snippet -> getter.apply(snippet).radius).orElse(defaultVal);
    }

    private String findCssClass(
        String defaultVal, Function<BhNodeViewStyleSnippet, EntryPointMarkSnippet> getter) {
      return find(cssClass, snippet -> getter.apply(snippet).cssClass).orElse(defaultVal);
    }
  }

  class CommonPartSnippet {
    String cssClass = null;
    ChildArrangement arrangement = null;
    ButtonSnippet privateTemplate = new ButtonSnippet();
    BreakpointSnippet breakpoint = new BreakpointSnippet();
    ExecStepMarkSnippet execStepMark = new ExecStepMarkSnippet();
    CorruptionMarkSnippet corruptionMark = new CorruptionMarkSnippet();
    EntryPointMarkSnippet entryPointMark = new EntryPointMarkSnippet();

    private void populateStyle(
        BhNodeViewStyle.CommonPart style,
        Function<BhNodeViewStyleSnippet, CommonPartSnippet> getter) {
      style.cssClass = findCssClass(style.cssClass, getter);
      style.arrangement = findArrangement(style.arrangement, getter);
      privateTemplate.populateStyle(
          style.privateTemplate, snippet -> getter.apply(snippet).privateTemplate);
      breakpoint.populateStyle(style.breakpoint, snippet -> getter.apply(snippet).breakpoint);
      execStepMark.populateStyle(style.execStepMark, snippet -> getter.apply(snippet).execStepMark);
      corruptionMark.populateStyle(
          style.corruptionMark, snippet -> getter.apply(snippet).corruptionMark);
      entryPointMark.populateStyle(
          style.entryPointMark, snippet -> getter.apply(snippet).entryPointMark);
    }

    private String findCssClass(
        String defaultVal, Function<BhNodeViewStyleSnippet, CommonPartSnippet> getter) {
      return find(cssClass, snippet -> getter.apply(snippet).cssClass).orElse(defaultVal);
    }

    private ChildArrangement findArrangement(
        ChildArrangement defaultVal, Function<BhNodeViewStyleSnippet, CommonPartSnippet> getter) {
      return find(arrangement, snippet -> getter.apply(snippet).arrangement).orElse(defaultVal);
    }
  }

  class SpecificPartSnippet {
    String cssClass = null;

    private void populateStyle(
        BhNodeViewStyle.SpecificPart style,
        Function<BhNodeViewStyleSnippet, SpecificPartSnippet> getter) {
      style.cssClass = findCssClass(style.cssClass, getter);
    }

    private String findCssClass(
        String defaultVal, Function<BhNodeViewStyleSnippet, SpecificPartSnippet> getter) {
      return find(cssClass, snippet -> getter.apply(snippet).cssClass).orElse(defaultVal);
    }
  }
}
