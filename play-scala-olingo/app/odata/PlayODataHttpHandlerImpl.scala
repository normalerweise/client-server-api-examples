package odata

import akka.stream.scaladsl.StreamConverters
import org.apache.olingo.commons.api.http.HttpMethod
import org.apache.olingo.server.api._
import org.apache.olingo.server.api.processor.Processor
import org.apache.olingo.server.core.ODataHandlerImpl
import org.apache.olingo.server.core.debug.ServerCoreDebugger
import play.api.http.HttpEntity
import play.api.mvc._

/**
  * Created by norman on 5/6/16.
  */

class PlayODataHttpHandlerImpl(oData: OData, serviceMetadata: ServiceMetadata) extends ODataHandler with Handler {

  val debugger = new ServerCoreDebugger(oData)
  val handler = new ODataHandlerImpl(oData, serviceMetadata, debugger)

  override def process(request: ODataRequest): ODataResponse = {
    return handler.process(request)
  }

  def handlerForRequest(request: RequestHeader): (RequestHeader, Handler) = {
    (request, oDataHandler)
  }

  def oDataHandler = Action(BodyParsers.parse.raw)(req =>
    process(req)
  )

  def process(request: Request[RawBuffer]): Result = {
    val odRequest: ODataRequest = new ODataRequest
    var exception: Exception = null
    var odResponse: ODataResponse = null

    fillODataRequest(odRequest, request)
    odResponse = process(odRequest)


    Result(
      header = ResponseHeader(200),
      body = HttpEntity.Streamed(StreamConverters.fromInputStream(odResponse.getContent), None, None)
    )
  }


  private def fillODataRequest(odRequest: ODataRequest, request: Request[RawBuffer]): ODataRequest = {
    val in = request.body.asBytes().get.iterator.asInputStream
    odRequest.setBody(in)
    val method = HttpMethod.valueOf(request.method)
    odRequest.setMethod(method)
    request.headers.headers.foreach( h => odRequest.addHeader(h._1, h._2) )

    fillUriInformation(odRequest, request)

    return odRequest
  }


  private def fillUriInformation(odRequest: ODataRequest, httpRequest: Request[RawBuffer]) {
    // hardcoded in demo implementation
    val basePath = "/oData.svc"

    val host = httpRequest.host
    val protocoll = if(httpRequest.secure) "https" else "http"
    val path = httpRequest.path

    // query string or null
    // mimic servlet behaviour of getQueryString
    val rawQueryString = {
      val q = httpRequest.rawQueryString;
      if(q.isEmpty) null else q
    }
    odRequest.setRawQueryPath(rawQueryString)

    val requestUri =  httpRequest.uri

    // everything from http... to query string
    val rawRequestUri = s"$protocoll://$host$requestUri"
    odRequest.setRawRequestUri(rawRequestUri)

    // everything after host - routing -> basically everything that belongs to oData
    val rawODataPath = path.drop(basePath.length)
    odRequest.setRawODataPath(rawODataPath)

    // everything after host + routing -> basically everything that does not belong to oData
    // hardcoded null in demo impl
    odRequest.setRawBaseUri(basePath)
    val rawServiceResolutionUri = null
    odRequest.setRawServiceResolutionUri(rawServiceResolutionUri)
  }

  override def register(processor: Processor): Unit = handler.register(processor)

  override def register(extension: OlingoExtension): Unit = handler.register(extension)
}