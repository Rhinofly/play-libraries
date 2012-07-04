package fly.play.dynamoDb.utils

object StreamUtils {
  def add[T](st1: Stream[T], st2: Stream[T]): Stream[T] = {
    if (st1.isEmpty) st2 else Stream.cons(st1.head, add(st1.tail, st2))
  }
}