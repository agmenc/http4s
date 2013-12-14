package org.http4s.netty.spdy

import com.typesafe.scalalogging.slf4j.Logging
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.spdy.{SpdyDataFrame, SpdyRstStreamFrame, SpdyStreamFrame}
import org.http4s.netty.utils.SpdyStreamContext

/**
 * @author Bryce Anderson
 *         Created on 12/5/13
 */
class NettySpdyPushStream(val streamid: Int,
                     protected val ctx: ChannelHandlerContext,
                     protected val parent: NettySpdyServerHandler,
                     protected val manager: SpdyStreamContext[NettySpdyStream])
          extends NettySpdyServerStream with Logging {

  def handleStreamFrame(msg: SpdyStreamFrame): Unit = msg match {

    case msg: SpdyRstStreamFrame => handleRstFrame(msg)

    case msg: SpdyDataFrame =>
      logger.warn(s"Spdy Push stream received a data frame. Discarding. $msg")
      parent.incrementWindow(msg.content().readableBytes())

    case msg => sys.error(s"Push Stream received invalid reply frame: $msg")
  }
}
