package com.github.nethad.clustermeister.integration.sc06b

import akka.dispatch.Await
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.lang.reflect.Method
import akka.actor.ActorRef
import akka.util.Timeout
import akka.util.Duration
import akka.util.duration._
import java.util.concurrent.TimeUnit
import akka.dispatch.Future
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicInteger
import akka.pattern.ask

/**
 * Used to create proxies
 */
object AkkaProxy {

  def newStringProxy(actor: ActorRef): SNode = newInstance[SNode](actor)

  def newInstance[T <: Any: Manifest](actor: ActorRef, sentMessagesCounter: AtomicInteger = new AtomicInteger(0), receivedMessagesCounter: AtomicInteger = new AtomicInteger(0), timeout: Timeout = Timeout(300 seconds)): T = {
    val c = manifest[T].erasure
    Proxy.newProxyInstance(
      c.getClassLoader,
      Array[Class[_]](c),
      new AkkaProxy(actor, sentMessagesCounter, receivedMessagesCounter, timeout)).asInstanceOf[T]
  }

}

/**
 *  Proxy that does RPC over Akka
 */
class AkkaProxy[ProxiedClass](actor: ActorRef, sentMessagesCounter: AtomicInteger, receivedMessagesCounter: AtomicInteger, timeout: Timeout) extends InvocationHandler with Serializable {

  override def toString = "ProxyFor" + actor.toString

  implicit val t = timeout

  def invoke(proxy: Object, method: Method, arguments: Array[Object]) = {
    val command = new Command[ProxiedClass](method.getDeclaringClass.getName, method.toString, arguments)
    try {
      val resultFuture: Future[Any] = actor ? Request(command, returnResult = true)
      sentMessagesCounter.incrementAndGet
      val result = Await.result(resultFuture, timeout.duration)
      receivedMessagesCounter.incrementAndGet
      result.asInstanceOf[AnyRef]
    } catch {
      case e: Exception =>
        println("Exception in proxy method `" + method.getName + "(" + { if (arguments != null) { arguments.foldLeft("")(_ + ", " + _) + ")`: " } else { "`: " } } + e + " from " + actor + " " + e.printStackTrace)
        throw e
    }
  }

}

case class Command[ParameterType](className: String, methodDescription: String, arguments: Array[Object]) extends Function1[ParameterType, AnyRef] {
  def apply(proxiedClass: ParameterType) = {
    val clazz = Class.forName(className)
    val methods = clazz.getMethods map (method => (method.toString, method)) toMap
    val method = methods(methodDescription)
    val result = method.invoke(proxiedClass, arguments: _*)
    result
  }

  override def toString: String = {
    className + "." + methodDescription + { if (arguments != null) { "(" + arguments.toList.mkString("(", ", ", ")") } else { "" } }
  }

}