package com.ambantis.akmeter
package util

abstract class Generator[T] {

  def requests(n: Int): List[T]

}
