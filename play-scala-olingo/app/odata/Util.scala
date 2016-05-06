package odata

import java.util.Locale

import org.apache.olingo.commons.api.data.{Entity, EntityCollection}
import org.apache.olingo.commons.api.edm._
import org.apache.olingo.commons.api.http.HttpStatusCode
import org.apache.olingo.server.api.ODataApplicationException
import org.apache.olingo.server.api.uri.{UriInfoResource, UriParameter, UriResourceEntitySet}

import scala.collection.JavaConverters._


object Util {

  def getEdmEntitySet(uriInfo: UriInfoResource): EdmEntitySet = {

    val resourcePaths = uriInfo.getUriResourceParts
    // To get the entity set we have to interpret all URI segments
    if (!resourcePaths.get(0).isInstanceOf[UriResourceEntitySet]) {
      // Here we should interpret the whole URI but in this example we do not support navigation so we throw an
      // exception
      throw new ODataApplicationException("Invalid resource type for first segment.", HttpStatusCode.NOT_IMPLEMENTED
        .getStatusCode, Locale.ENGLISH)
    }

    val uriResource = resourcePaths.get(0).asInstanceOf[UriResourceEntitySet]

    uriResource.getEntitySet
  }

  def findEntity(edmEntityType: EdmEntityType, entitySet: EntityCollection,
                 keyParams: java.util.List[UriParameter]): Entity = {

    val entityList = entitySet.getEntities

    // loop over all entities in order to find that one that matches all keys in request
    // e.g. contacts(ContactID=1, CompanyID=1)
    for (entity <- entityList.asScala) {
      val foundEntity = entityMatchesAllKeys(edmEntityType, entity, keyParams)
      if (foundEntity) {
        return entity
      }
    }

    null
  }

  def entityMatchesAllKeys(edmEntityType: EdmEntityType, entity: Entity, keyParams: java.util.List[UriParameter]): Boolean = {

    // loop over all keys
    for (key <- keyParams.asScala) {
      // key
      val keyName = key.getName
      val keyText = key.getText

      // Edm: we need this info for the comparison below
      val edmKeyProperty = edmEntityType.getProperty(keyName).asInstanceOf[EdmProperty]
      val isNullable = edmKeyProperty.isNullable
      val maxLength = edmKeyProperty.getMaxLength
      val precision = edmKeyProperty.getPrecision
      val isUnicode = edmKeyProperty.isUnicode
      val scale = edmKeyProperty.getScale
      // get the EdmType in order to compare
      val edmType = edmKeyProperty.getType
      val edmPrimitiveType = edmType.asInstanceOf[EdmPrimitiveType]

      // Runtime data: the value of the current entity
      // don't need to check for null, this is done in olingo library
      val valueObject = entity.getProperty(keyName).getValue

      // now need to compare the valueObject with the keyText String
      // this is done using the type.valueToString //
      var valueAsString: String = null
      try {
        valueAsString = edmPrimitiveType.valueToString(valueObject, isNullable, maxLength, precision, scale, isUnicode)
      } catch {
        case e: EdmPrimitiveTypeException =>
          throw new ODataApplicationException("Failed to retrieve String value", HttpStatusCode.INTERNAL_SERVER_ERROR
            .getStatusCode, Locale.ENGLISH, e)
      }

      if (valueAsString == null) {
        return false
      }

      val matches = valueAsString.equals(keyText)
      if (!matches) {
        // if any of the key properties is not found in the entity, we don't need to search further
        return false
      }
    }
    true
  }
}
