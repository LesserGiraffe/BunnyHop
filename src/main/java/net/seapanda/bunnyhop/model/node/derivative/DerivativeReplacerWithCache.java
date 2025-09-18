/*
 * Copyright 2017 K.Koike
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenss/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.seapanda.bunnyhop.model.node.derivative;

import java.util.Set;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.BhNode.Swapped;
import net.seapanda.bunnyhop.service.DerivativeCache;
import net.seapanda.bunnyhop.undo.UserOperation;

/**
 * 派生ノードの入れ替え処理を行うクラス.
 *
 * @author K.Koike
 */
public class DerivativeReplacerWithCache implements DerivativeReplacer {
  
  private DerivativeCache cache;

  public DerivativeReplacerWithCache(DerivativeCache cache) {
    this.cache = cache;
  }

  @Override
  public Set<Swapped> replace(BhNode orgOfNewDervs, BhNode orgOfOldDervs, UserOperation userOpe) {
    return DerivativeDisplacer.displace(orgOfNewDervs, orgOfOldDervs, cache, userOpe);
  }
}
