package com.ambantis.akmeter
package api

import java.time.Instant

import akka.event.{EventStream, Logging, LoggingAdapter}
import io.grpc.{Context, Contexts, Metadata, ServerCall, ServerCallHandler, ServerInterceptor}

case class ContextServerInterceptor(log: LoggingAdapter) extends ServerInterceptor {

  override def interceptCall[Req, Resp](call: ServerCall[Req, Resp],
                                        requestHeaders: Metadata,
                                        next: ServerCallHandler[Req, Resp]): ServerCall.Listener[Req] = {

    import ContextValue.{XRequestId, XRequestTimestamp}

    val values: List[ContextValue] = List(XRequestId)

    val timestamp: String = Instant.now().toString

    val context: Context =
      values.foldLeft(XRequestTimestamp.put(timestamp, Context.current())) {
        case (ctx, value) => value.transfer(requestHeaders, ctx)
      }

    if (log.isDebugEnabled) {
      val callType = call.getMethodDescriptor.getType.name()
      val methodName = call.getMethodDescriptor.getFullMethodName
      val attrs = call.getAttributes
      val authority = call.getAuthority

      log.info(
        s"got call at $timestamp, callType: $callType, methodName: $methodName, attributes: $attrs, " +
          s" authority: $authority, headers: $requestHeaders, updatedContext: $context"
      )
    }

    Contexts.interceptCall(context, call, requestHeaders, next)
  }

}

object ContextServerInterceptor {

  def apply(eventStream: EventStream): ContextServerInterceptor = {
    val logger = Logging.getLogger(eventStream, classOf[ContextServerInterceptor])
    new ContextServerInterceptor(logger)
  }

}
