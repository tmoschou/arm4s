package io.tmos.arm

trait ManagedResource[A]{
  def apply[B](f: A => B): B
  def map[B](f: A => B): B = apply(f)
  def flatMap[B](f: A => B): B  = apply(f)
  def foreach(f: A => Unit): Unit  = apply(f)
}




