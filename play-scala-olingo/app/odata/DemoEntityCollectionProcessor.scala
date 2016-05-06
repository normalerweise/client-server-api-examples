package odata

import org.apache.olingo.commons.api.data._
import org.apache.olingo.commons.api.format.ContentType
import org.apache.olingo.commons.api.http.{HttpHeader, HttpStatusCode}
import org.apache.olingo.server.api.processor.{EntityCollectionProcessor, EntityProcessor}
import org.apache.olingo.server.api.serializer.{EntityCollectionSerializerOptions, EntitySerializerOptions}
import org.apache.olingo.server.api.uri.{UriInfo, UriResource, UriResourceEntitySet}
import org.apache.olingo.server.api.{OData, ODataRequest, ODataResponse, ServiceMetadata}

/**
  * Created by norman on 4/17/16.
  */
class DemoEntityCollectionProcessor extends EntityProcessor with EntityCollectionProcessor {

  var odata: OData = null
  var serviceMetadata: ServiceMetadata = null

  override def init(odata: OData, serviceMetadata: ServiceMetadata) = {
    this.odata = odata
    this.serviceMetadata = serviceMetadata
  }

  def readEntity(request: ODataRequest, response: ODataResponse,
    uriInfo: UriInfo, responseFormat: ContentType) {

    // 1. retrieve the Entity Type
    val resourcePaths = uriInfo.getUriResourceParts
    // Note: only in our example we can assume that the first segment is the EntitySet
    val uriResourceEntitySet = resourcePaths.get(0).asInstanceOf[UriResourceEntitySet]
    val edmEntitySet = uriResourceEntitySet.getEntitySet

    // 2. retrieve the data from backend
    val keyPredicates = uriResourceEntitySet.getKeyPredicates
    val entity = Storage.readEntityData(edmEntitySet, keyPredicates)

    // 3. serialize
    val entityType = edmEntitySet.getEntityType

    val contextUrl = ContextURL.`with`.entitySet(edmEntitySet).build
    // expand and select currently not supported
    val options = EntitySerializerOptions.`with`.contextURL(contextUrl).build

    val serializer = odata.createSerializer(responseFormat)
    val serializerResult = serializer.entity(serviceMetadata, entityType, entity, options)
    val entityStream = serializerResult.getContent

    //4. configure the response object
    response.setContent(entityStream)
    response.setStatusCode(HttpStatusCode.OK.getStatusCode)
    response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString)
  }

  override def readEntityCollection(request: ODataRequest, response: ODataResponse, uriInfo: UriInfo, responseFormat: ContentType) = {

    // 1st we have retrieve the requested EntitySet from the uriInfo object (representation of the parsed service URI)
    val resourcePaths: java.util.List[UriResource] = uriInfo.getUriResourceParts
    // in our example, the first segment is the EntitySet
    val uriResourceEntitySet = resourcePaths.get(0).asInstanceOf[UriResourceEntitySet]
    val edmEntitySet = uriResourceEntitySet.getEntitySet

    // 2nd: fetch the data from backend for this requested EntitySetName
    // it has to be delivered as EntitySet object
    val entitySet = Storage.readEntitySetData(edmEntitySet)

    // 3rd: create a serializer based on the requested format (json)
    val serializer = odata.createSerializer(responseFormat)

    // 4th: Now serialize the content: transform from the EntitySet object to InputStream
    val edmEntityType = edmEntitySet.getEntityType
    val contextUrl = ContextURL.`with`().entitySet(edmEntitySet).build()

    val id = request.getRawBaseUri + "/" + edmEntitySet.getName
    val opts = EntityCollectionSerializerOptions.`with`().id(id).contextURL(contextUrl).build()
    val serializerResult = serializer.entityCollection(serviceMetadata, edmEntityType, entitySet, opts)
    val serializedContent = serializerResult.getContent

    // Finally: configure the response object: set the body, headers and status code
    response.setContent(serializedContent)
    response.setStatusCode(HttpStatusCode.OK.getStatusCode)
    response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString)
  }

  override def createEntity(request: ODataRequest, response: ODataResponse , uriInfo: UriInfo,
    requestFormat: ContentType, responseFormat: ContentType) {

    // 1. Retrieve the entity type from the URI
    val edmEntitySet = Util.getEdmEntitySet(uriInfo)
    val edmEntityType = edmEntitySet.getEntityType

    // 2. create the data in backend
    // 2.1. retrieve the payload from the POST request for the entity to create and deserialize it
    val requestInputStream = request.getBody
    val deserializer = odata.createDeserializer(requestFormat)
    val result = deserializer.entity(requestInputStream, edmEntityType)
    val requestEntity = result.getEntity
    // 2.2 do the creation in backend, which returns the newly created entity
    val createdEntity = Storage.createEntityData(edmEntitySet, requestEntity)

    // 3. serialize the response (we have to return the created entity)
    val contextUrl = ContextURL.`with`.entitySet(edmEntitySet).build()
    // expand and select currently not supported
    val options = EntitySerializerOptions.`with`.contextURL(contextUrl).build()

    val serializer = this.odata.createSerializer(responseFormat)
    val serializedResponse = serializer.entity(serviceMetadata, edmEntityType, createdEntity, options)

    //4. configure the response object
    response.setContent(serializedResponse.getContent)
    response.setStatusCode(HttpStatusCode.CREATED.getStatusCode)
    response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString)
  }

  def updateEntity(request: ODataRequest, response: ODataResponse , uriInfo: UriInfo,
    requestFormat: ContentType, responseFormat: ContentType) = {

    // 1. Retrieve the entity set which belongs to the requested entity
    val resourcePaths = uriInfo.getUriResourceParts
    // Note: only in our example we can assume that the first segment is the EntitySet
    val uriResourceEntitySet = resourcePaths.get(0).asInstanceOf[UriResourceEntitySet]
    val edmEntitySet = uriResourceEntitySet.getEntitySet
    val edmEntityType = edmEntitySet.getEntityType

    // 2. update the data in backend
    // 2.1. retrieve the payload from the PUT request for the entity to be updated
    val requestInputStream = request.getBody
    val deserializer = this.odata.createDeserializer(requestFormat)
    val result = deserializer.entity(requestInputStream, edmEntityType)
    val requestEntity = result.getEntity
    // 2.2 do the modification in backend
    val keyPredicates = uriResourceEntitySet.getKeyPredicates
    // Note that this updateEntity()-method is invoked for both PUT or PATCH operations
    val httpMethod = request.getMethod
    Storage.updateEntityData(edmEntitySet, keyPredicates, requestEntity, httpMethod)

    //3. configure the response object
    response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode)
  }

  def deleteEntity(request: ODataRequest, response: ODataResponse, uriInfo: UriInfo) {
    // 1. Retrieve the entity set which belongs to the requested entity
    val resourcePaths = uriInfo.getUriResourceParts
    // Note: only in our example we can assume that the first segment is the EntitySet
    val uriResourceEntitySet = resourcePaths.get(0).asInstanceOf[UriResourceEntitySet]
    val edmEntitySet = uriResourceEntitySet.getEntitySet

    // 2. delete the data in backend
    val keyPredicates = uriResourceEntitySet.getKeyPredicates
    Storage.deleteEntityData(edmEntitySet, keyPredicates)

    //3. configure the response object
    response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode)
  }

}
