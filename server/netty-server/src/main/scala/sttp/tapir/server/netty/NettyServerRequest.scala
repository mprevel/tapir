package sttp.tapir.server.netty

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq
import io.netty.handler.codec.http.{FullHttpRequest, QueryStringDecoder}
import sttp.model.{Header, Method, QueryParams, Uri}
import sttp.tapir.model.{AttributeKey, AttributeMap, ConnectionInfo, ServerRequest}
import sttp.tapir.server.netty.internal.RichNettyHttpHeaders

case class NettyServerRequest(req: FullHttpRequest, attributeMap: AttributeMap = new AttributeMap()) extends ServerRequest {
  override lazy val protocol: String = req.protocolVersion().text()
  override lazy val connectionInfo: ConnectionInfo = ConnectionInfo.NoInfo
  override lazy val underlying: Any = req
  override lazy val queryParameters: QueryParams = {
    val decoder = new QueryStringDecoder(req.uri())
    val multiMap = decoder
      .parameters()
      .asScala
      .map { case (k, vs) => k -> vs.asScala.toSeq }
      .toMap

    QueryParams.fromMultiMap(multiMap)
  }
  override lazy val method: Method = Method.unsafeApply(req.method().name())
  override lazy val uri: Uri = Uri.unsafeParse(req.uri())
  override lazy val pathSegments: List[String] = uri.pathSegments.segments.map(_.v).filter(_.nonEmpty).toList
  override lazy val headers: Seq[Header] = req.headers().toHeaderSeq ++ req.trailingHeaders().toHeaderSeq

  override def attribute[T](key: AttributeKey[T]): Option[T] = attributeMap.get(key)
  override def withAttribute[T](key: AttributeKey[T], value: T): ServerRequest = new NettyServerRequest(req, attributeMap.put(key, value))
}
