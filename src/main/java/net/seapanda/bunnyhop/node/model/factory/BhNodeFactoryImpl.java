
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

package net.seapanda.bunnyhop.node.model.factory;

import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.parameter.BhNodeId;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.service.undo.UserOperation;

/**
 * {@link BhNode} の作成と MVC 構造の作成処理を提供するクラス.
 *
 * @author K.Koike
 */
public class BhNodeFactoryImpl implements BhNodeFactory {
  
  private final BhNodeRepository repository;
  private final BhNodeMvcBuilder mvcBuilder;

  /** コンストラクタ. */
  public BhNodeFactoryImpl(BhNodeRepository repository, BhNodeMvcBuilder mvcBuilder) {
    this.repository = repository;
    this.mvcBuilder = mvcBuilder;
  }

  @Override
  public BhNode create(BhNodeId id, UserOperation userOpe) {
    return repository.getNodeOf(id).copy(userOpe);
  }

  @Override
  public BhNode create(BhNodeId id, MvcType type, UserOperation userOpe) {
    BhNode node = create(id, userOpe);
    if (node != null) {
      constructMvc(node, type);
    }
    return node;
  }

  @Override
  public BhNodeView constructMvc(BhNode node, MvcType type) {
    return mvcBuilder.build(node, type);
  }

  @Override
  public boolean canCreate(BhNodeId id) {
    return repository.hasNodeOf(id);
  }
}
