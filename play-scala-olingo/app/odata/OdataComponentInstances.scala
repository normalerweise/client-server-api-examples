package odata

import org.apache.olingo.commons.api.edmx.EdmxReference
import org.apache.olingo.server.api.OData

/**
  * Created by norman on 5/6/16.
  */
object OdataComponentInstances {
  val oData = OData.newInstance()
  val edm = oData.createServiceMetadata(new DemoEdmProvider, new java.util.ArrayList[EdmxReference]())
  val handler = new PlayODataHttpHandlerImpl(oData, edm)
  handler.register(new DemoEntityCollectionProcessor())
}