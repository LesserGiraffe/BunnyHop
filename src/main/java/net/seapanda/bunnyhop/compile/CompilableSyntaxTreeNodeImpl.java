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

package net.seapanda.bunnyhop.compile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.seapanda.bunnyhop.compiler.api.CompilableSyntaxTreeNode;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.parameter.EntryPointSetting;
import net.seapanda.bunnyhop.node.model.syntaxtree.SyntaxTreeNode;
import net.seapanda.bunnyhop.node.model.syntaxtree.SyntaxTreeNodeBase;

/**
 * {@link CompilableSyntaxTreeNode} の実装クラス.
 *
 * @author K.Koike
 */
class CompilableSyntaxTreeNodeImpl
    extends SyntaxTreeNodeBase<CompilableSyntaxTreeNodeImpl> implements CompilableSyntaxTreeNode {

  private CompilableSyntaxTreeNode original;
  private CompilableSyntaxTreeNodeImpl parent;
  private final List<CompilableSyntaxTreeNodeImpl> children = new ArrayList<>();
  private final String instanceId;
  private final boolean requiresConditionalWait;
  private final boolean isBuiltInEntryPoint;
  private String lexeme;
  private String tag;

  CompilableSyntaxTreeNodeImpl(SyntaxTreeNode org) {
    super(org.getSymbolName());
    instanceId = org.getInstanceId().toString();
    requiresConditionalWait = false;
    isBuiltInEntryPoint = false;
    lexeme = org.getLexeme();
    tag = "";
  }

  CompilableSyntaxTreeNodeImpl(BhNode org) {
    super(org.getSymbolName());
    instanceId = org.getInstanceId().toString();
    requiresConditionalWait = org.isBreakpointGroupLeader();
    isBuiltInEntryPoint = org.getEntryPointSetting() == EntryPointSetting.SET;
    lexeme = org.getLexeme();
    tag = "";
  }

  @Override
  public CompilableSyntaxTreeNodeImpl getParent() {
    return parent;
  }

  public void setParent(CompilableSyntaxTreeNodeImpl parent) {
    this.parent = parent;
  }

  @Override
  public List<? extends CompilableSyntaxTreeNodeImpl> getChildren() {
    return Collections.unmodifiableList(children);
  }

  public void addChild(CompilableSyntaxTreeNodeImpl child) {
    children.add(child);
  }

  @Override
  protected CompilableSyntaxTreeNodeImpl self() {
    return this;
  }

  @Override
  public String getLexeme() {
    return lexeme == null ? "" : lexeme;
  }

  @Override
  public void setLexeme(String val) {
    lexeme = val;
  }

  @Override
  public String getInstanceId() {
    return instanceId;
  }


  @Override
  public String getTag() {
    return tag;
  }

  @Override
  public void setTag(String val) {
    tag = val;
  }

  @Override
  public CompilableSyntaxTreeNode getOriginal() {
    return original;
  }

  public void setOriginal(CompilableSyntaxTreeNode node) {
    original = node;
  }

  /** このノードがエントリポイントとして定義してあるノードであるかどうかを返す. */
  public boolean isBuiltInEntryPoint() {
    return isBuiltInEntryPoint;
  }

  @Override
  public boolean canSetBreakpoint() {
    return requiresConditionalWait;
  }
}
