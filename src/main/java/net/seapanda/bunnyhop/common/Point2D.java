/**
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
package net.seapanda.bunnyhop.common;

import java.io.Serializable;

/**
 * @author K.Koike
 */
public class Point2D implements Serializable {

	public double x;
	public double y;

	public Point2D(double x, double y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * 引数で指定した数の方が現在の値より大きい場合, その数で置き換える.
	 * 片方の要素だけ大きい場合は、その要素だけ置き換える.
	 * */
	public void updateIfGreter(Point2D pos) {
		x = Math.max(x, pos.x);
		y = Math.max(y, pos.y);
	}


	/**
	 * 引数で指定した数の方が現在の値より大きい場合, その数で置き換える.
	 * 片方の要素だけ大きい場合は、その要素だけ置き換える.
	 * */
	public void updateIfGreter(double x, double y) {
		this.x = Math.max(this.x, x);
		this.y = Math.max(this.y, y);
	}

	/**
	 * 引数で指定した数の方が現在の値より小さい場合, その数で置き換える.
	 * 片方の要素だけ小さい場合は、その要素だけ置き換える.
	 * */
	public void updateIfLess(Point2D pos) {
		x = Math.min(x, pos.x);
		y = Math.min(y, pos.y);
	}

	/**
	 * 引数で指定した数の方が現在の値より小さい場合, その数で置き換える.
	 * 片方の要素だけ小さい場合は、その要素だけ置き換える.
	 * */
	public void updateIfLess(double x, double y) {
		this.x = Math.min(this.x, x);
		this.y = Math.min(this.y, y);
	}


	/**
	 * 引数で指定した要素を現在の値に足し込む
	 * */
	public void add(Point2D pos) {
		this.x += pos.x;
		this.y += pos.y;
	}

	/**
	 * 引数で指定した要素を現在の値に足し込む
	 * */
	public void add(double x, double y) {
		this.x += x;
		this.y += y;
	}

	@Override
	public boolean equals(Object point) {

		if (point instanceof Point2D)
			return (x == ((Point2D)point).x) && (y == ((Point2D)point).y);
		else
			return false;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 67 * hash + (int) (Double.doubleToLongBits(this.x) ^ (Double.doubleToLongBits(this.x) >>> 32));
		hash = 67 * hash + (int) (Double.doubleToLongBits(this.y) ^ (Double.doubleToLongBits(this.y) >>> 32));
		return hash;
	}
}
