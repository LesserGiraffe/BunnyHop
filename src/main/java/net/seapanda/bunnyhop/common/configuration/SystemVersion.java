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

package net.seapanda.bunnyhop.common.configuration;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import net.seapanda.bunnyhop.utility.version.PrefixedVersion;

/**
  * システムのバージョン.
  *
  * @author K.Koike
  */
@JsonAdapter(SystemVersion.Adapter.class)
public class SystemVersion extends PrefixedVersion {

  /** システムのバージョンが存在しないことを表すオブジェクト. */
  public static final SystemVersion NONE = new SystemVersion();

  /**
   * {@link SystemVersion} オブジェクトを作成する.
   *
   * @param id 識別子名
   * @return {@link SystemVersion} オブジェクト
   */
  public static SystemVersion of(String id) {
    return new SystemVersion(id);
  }

  /**
   * コンストラクタ.
   *
   * @param version 識別子名
   */
  private SystemVersion(String version) {
    super(version);
  }

  /** デフォルトコンストラクタ. (デシリアライズ用) */
  public SystemVersion() {
    super();
  }

  /** {@link SystemVersion} を JSON の文字列として入出力する {@link TypeAdapter}. */
  static class Adapter extends TypeAdapter<SystemVersion> {

    @Override
    public void write(JsonWriter out, SystemVersion value) throws IOException {
      if (value == null) {
        out.nullValue();
        return;
      }
      out.value(value.toString());
    }

    @Override
    public SystemVersion read(JsonReader in) throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        return null;
      }
      String version = in.nextString();
      if (version.equals(NONE.version)) {
        return NONE;
      }
      try {
        return SystemVersion.of(version);
      } catch (IllegalArgumentException e) {
        throw new IOException(e);
      }
    }
  }
}
