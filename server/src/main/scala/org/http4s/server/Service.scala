package org.http4s
package server

import scalaz.concurrent.Task
import Service._

final class Service[-A, +B] private (val run: A => Task[Option[B]]) extends AnyVal {
  def apply(a: A): Task[Option[B]] = run(a)

  def contramap[C](f: C => A): Service[C, B] = lift(run.compose(f))

  def map[C](f: B => C): Service[A, C] = lift(run.andThen(_.map(_.map(f))))

  def flatMapTask[C](f: B => Task[Option[C]]): Service[A, C] = lift(run.andThen(_.flatMap {
    case Some(b) => f(b)
    case None    => Task.now(None)
  }))

  // The Monadic 'bind', >>=
  def flatMap[C](f: Service[B, C]): Service[A, C] = flatMapTask(f.run)

  def or[B1 >: B](a: A, default: => Task[B1]): Task[B1] = apply(a).flatMap {
    case Some(b) => Task.now(b)
    case None => default
  }

  def orElse[A1 <: A, B1 >: B](other: Service[A1, B1]): Service[A1, B1] = {
    Service.lift { a :A1 =>  run(a).flatMap {
      case r@ Some(_) => Task.now(r)
      case None       => other(a)
    }}
  }
}

object Service {
  def lift[A, B](f: A => Task[Option[B]]): Service[A, B] = new Service[A, B](f)

  private val TaskNone = Task(None)

  def apply[A, B](pf: PartialFunction[A, Task[B]]): Service[A, B] = lift {
    pf.lift.andThen {
      case Some(respTask) => respTask.map(Some(_))
      case None => TaskNone
    }
  }

  def empty[A, B]: Service[A, B] = lift(Function.const(TaskNone))
}
