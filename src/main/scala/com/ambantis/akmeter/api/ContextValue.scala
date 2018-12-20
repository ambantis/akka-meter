package com.ambantis.akmeter
package api

import io.grpc.{Context, Metadata}

case class ContextValue(label: String) {

  val contextKey: Context.Key[String] = Context.key(label)

  val metaKey: Metadata.Key[String] = Metadata.Key.of(label, Metadata.ASCII_STRING_MARSHALLER)

  def headerValue(headers: Metadata): Option[String] = headers.get(metaKey) match {
    case null  => None
    case value => Some(value)
  }

  def contextValue(implicit context: Context): Option[String] = contextKey.get(context) match {
    case null  => None
    case value => Some(value)
  }

  def transfer(from: Metadata, to: Context, fallback: Option[String] = None): Context =
    headerValue(from).orElse(fallback).map(value => to.withValue(contextKey, value)).getOrElse(to)

  def put(value: String, to: Context): Context = to.withValue(contextKey, value)
}

object ContextValue {

  final val XRequestId = ContextValue("x-request-id")
  final val XRequestTimestamp = ContextValue("x-request-timestamp")
}
