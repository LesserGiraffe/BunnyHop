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

package net.seapanda.bunnyhop.node.model.parameter;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import net.seapanda.bunnyhop.utility.version.PrefixedVersion;

/**
 * BhNode のバージョン.
 *
 * @author K.Koike
 */
@JsonAdapter(BhNodeVersion.Adapter.class)
public class BhNodeVersion extends PrefixedVersion {

  /** BhNode のバージョンが存在しないことを表すオブジェクト. */
  public static final BhNodeVersion NONE = new BhNodeVersion();

  /**
   * {@link BhNodeVersion} オブジェクトを作成する.
   *
   * @param id 識別子名
   * @return {@link BhNodeVersion} オブジェクト
   */
  public static BhNodeVersion of(String id) {
    return new BhNodeVersion(id);
  }

  /**
   * コンストラクタ.
   *
   * @param version 識別子名
   */
  private BhNodeVersion(String version) {
    super(version);
  }

  /** デフォルトコンストラクタ. (デシリアライズ用) */
  public BhNodeVersion() {
    super();
  }

  /** {@link BhNodeVersion} を JSON の文字列として入出力する {@link TypeAdapter}. */
  static class Adapter extends TypeAdapter<BhNodeVersion> {

    @Override
    public void write(JsonWriter out, BhNodeVersion value) throws IOException {
      if (value == null) {
        out.nullValue();
        return;
      }
      out.value(value.toString());
    }

    @Override
    public BhNodeVersion read(JsonReader in) throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        return null;
      }
      String version = in.nextString();
      if (version.equals(NONE.version)) {
        return NONE;
      }
      try {
        return BhNodeVersion.of(version);
      } catch (IllegalArgumentException e) {
        throw new IOException(e);
      }
    }
  }
}
