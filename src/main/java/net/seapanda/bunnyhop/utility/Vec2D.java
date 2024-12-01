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

package net.seapanda.bunnyhop.utility;

import java.io.Serializable;

/**
 * 二次元ベクトル.
 *
 * @author K.Koike
 */
public class Vec2D implements Serializable {

  public double x;
  public double y;

  public Vec2D(double x, double y) {
    this.x = x;
    this.y = y;
  }

  /**
   * 引数で指定した数の方が現在の値より大きい場合, その数で置き換える.
   * 片方の要素だけ大きい場合は、その要素だけ置き換える.
   */
  public void updateIfGreter(Vec2D pos) {
    x = Math.max(x, pos.x);
    y = Math.max(y, pos.y);
  }


  /**
   * 引数で指定した数の方が現在の値より大きい場合, その数で置き換える.
   * 片方の要素だけ大きい場合は、その要素だけ置き換える.
   */
  public void updateIfGreater(double x, double y) {
    this.x = Math.max(this.x, x);
    this.y = Math.max(this.y, y);
  }

  /**
   * 引数で指定した数の方が現在の値より小さい場合, その数で置き換える.
   * 片方の要素だけ小さい場合は、その要素だけ置き換える.
   */
  public void updateIfLess(Vec2D pos) {
    x = Math.min(x, pos.x);
    y = Math.min(y, pos.y);
  }

  /**
   * 引数で指定した数の方が現在の値のより小さい場合, その数で置き換える.
   * 片方の要素だけ小さい場合は、その要素だけ置き換える.
   */
  public void updateIfLess(double x, double y) {
    this.x = Math.min(this.x, x);
    this.y = Math.min(this.y, y);
  }

  /** 引数で指定した要素を現在の値に足し込む. */
  public void add(Vec2D pos) {
    add(pos.x, pos.y);
  }

  /** 引数で指定した要素を現在の値に足し込む. */
  public void add(double x, double y) {
    this.x += x;
    this.y += y;
  }

  /** 引数で指定した要素を現在の値から引く. */
  public void sub(Vec2D pos) {
    sub(pos.x, pos.y);
  }

  /** 引数で指定した要素を現在の値から引く. */
  public void sub(double x, double y) {
    this.x -= x;
    this.y -= y;
  }

  @Override
  public boolean equals(Object point) {
    if (point instanceof Vec2D) {
      return (x == ((Vec2D) point).x) && (y == ((Vec2D) point).y);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    int hash = 5;
    hash = 67 * hash
        + (int) (Double.doubleToLongBits(this.x) ^ (Double.doubleToLongBits(this.x) >>> 32));
    hash = 67 * hash
        + (int) (Double.doubleToLongBits(this.y) ^ (Double.doubleToLongBits(this.y) >>> 32));
    return hash;
  }

  @Override
  public String toString() {
    return "x: %s,  y: %s".formatted(x, y);
  }
}
