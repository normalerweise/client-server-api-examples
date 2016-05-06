package odata

import org.apache.olingo.commons.api.data.Entity
import org.apache.olingo.commons.api.data.EntityCollection
import org.apache.olingo.commons.api.data.Property
import org.apache.olingo.commons.api.data.ValueType
import org.apache.olingo.commons.api.edm.EdmEntitySet
import org.apache.olingo.commons.api.edm.EdmEntityType
import org.apache.olingo.commons.api.ex.ODataRuntimeException
import org.apache.olingo.commons.api.http.HttpMethod
import org.apache.olingo.commons.api.http.HttpStatusCode
import org.apache.olingo.server.api.ODataApplicationException
import org.apache.olingo.server.api.uri.UriParameter

import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

import collection.JavaConverters._
import scala.collection.mutable

object Storage {

  val productList = mutable.ListBuffer.empty[Entity]

  initSampleData()


  def readEntitySetData(edmEntitySet: EdmEntitySet): EntityCollection = {

    // actually, this is only required if we have more than one Entity Sets
    if (edmEntitySet.getName.equals(DemoEdmProvider.ES_PRODUCTS_NAME)) {
      return getProducts
    }

    null
  }

  def readEntityData(edmEntitySet: EdmEntitySet, keyParams: java.util.List[UriParameter]): Entity = {

    val edmEntityType = edmEntitySet.getEntityType

    // actually, this is only required if we have more than one Entity Type
    if (edmEntityType.getName.equals(DemoEdmProvider.ET_PRODUCT_NAME)) {
      return getProduct(edmEntityType, keyParams)
    }

    null
  }

  def createEntityData(edmEntitySet: EdmEntitySet, entityToCreate: Entity): Entity = {

    val edmEntityType = edmEntitySet.getEntityType

    // actually, this is only required if we have more than one Entity Type
    if (edmEntityType.getName.equals(DemoEdmProvider.ET_PRODUCT_NAME)) {
      return createProduct(edmEntityType, entityToCreate)
    }

    null
  }

  /**
    * This method is invoked for PATCH or PUT requests
    **/
  def updateEntityData(edmEntitySet: EdmEntitySet, keyParams: java.util.List[UriParameter], updateEntity: Entity,
                       httpMethod: HttpMethod) = {

    val edmEntityType = edmEntitySet.getEntityType

    // actually, this is only required if we have more than one Entity Type
    if (edmEntityType.getName.equals(DemoEdmProvider.ET_PRODUCT_NAME)) {
      updateProduct(edmEntityType, keyParams, updateEntity, httpMethod)
    }
  }

  def deleteEntityData(edmEntitySet: EdmEntitySet, keyParams: java.util.List[UriParameter]) {

    val edmEntityType = edmEntitySet.getEntityType

    // actually, this is only required if we have more than one Entity Type
    if (edmEntityType.getName.equals(DemoEdmProvider.ET_PRODUCT_NAME)) {
      deleteProduct(edmEntityType, keyParams)
    }
  }

  /* INTERNAL */

  private def getProducts: EntityCollection = {
    val retEntitySet = new EntityCollection

    for (productEntity <- productList) {
      retEntitySet.getEntities.add(productEntity)
    }

    retEntitySet
  }

  def getProduct(edmEntityType: EdmEntityType, keyParams: java.util.List[UriParameter]): Entity = {

    // the list of entities at runtime
    val entitySet = getProducts

    /* generic approach to find the requested entity */
    val requestedEntity = Util.findEntity(edmEntityType, entitySet, keyParams)

    if (requestedEntity == null) {
      // this variable is null if our data doesn't contain an entity for the requested key
      // Throw suitable exception
      throw new ODataApplicationException("Entity for requested key doesn't exist",
        HttpStatusCode.NOT_FOUND.getStatusCode, Locale.ENGLISH)
    }

    requestedEntity
  }

  def createProduct(edmEntityType: EdmEntityType, entity: Entity): Entity = {

    // the ID of the newly created product entity is generated automatically
    var newId = 1
    while (productIdExists(newId)) {
      newId += 1
    }

    val idProperty = entity.getProperty("ID")
    if (idProperty != null) {
      idProperty.setValue(ValueType.PRIMITIVE, Integer.valueOf(newId))
    } else {
      // as of OData v4 spec, the key property can be omitted from the POST request body
      entity.getProperties.add(new Property(null, "ID", ValueType.PRIMITIVE, newId))
    }
    entity.setId(createId("Products", newId))
    productList += entity

    entity
  }

  def productIdExists(id: Int): Boolean = {

    for (entity <- productList) {
      val existingID = entity.getProperty("ID").getValue.asInstanceOf[Integer]
      if (existingID.intValue() == id) {
        return true
      }
    }

    false
  }

  import util.control.Breaks._

  private def updateProduct(edmEntityType: EdmEntityType, keyParams: java.util.List[UriParameter], entity: Entity,
                            httpMethod: HttpMethod) = {

    val productEntity = getProduct(edmEntityType, keyParams)
    if (productEntity == null) {
      throw new ODataApplicationException("Entity not found", HttpStatusCode.NOT_FOUND.getStatusCode, Locale.ENGLISH)
    }

    // loop over all properties and replace the values with the values of the given payload
    // Note: ignoring ComplexType, as we don't have it in our odata model
    val existingProperties = productEntity.getProperties
    for (existingProp <- existingProperties.asScala) {
      breakable {

        val propName = existingProp.getName
        if (isKey(edmEntityType, propName)) {
          break
        }

        val updateProperty = entity.getProperty(propName)
        // the request payload might not consider ALL properties, so it can be null
        if (updateProperty == null) {
          // if a property has NOT been added to the request payload
          // depending on the HttpMethod, our behavior is different
          if (httpMethod.equals(HttpMethod.PATCH)) {
            // as of the OData spec, in case of PATCH, the existing property is not touched
            break // do nothing
          } else if (httpMethod.equals(HttpMethod.PUT)) {
            // as of the OData spec, in case of PUT, the existing property is set to null (or to default value)
            existingProp.setValue(existingProp.getValueType, null)
            break
          }
        }
        // change the value of the properties
        existingProp.setValue(existingProp.getValueType, updateProperty.getValue)
      }
    }
  }

  private def deleteProduct(edmEntityType: EdmEntityType, keyParams: java.util.List[UriParameter]) = {

    val productEntity = getProduct(edmEntityType, keyParams)
    if (productEntity == null) {
      throw new ODataApplicationException("Entity not found", HttpStatusCode.NOT_FOUND.getStatusCode, Locale.ENGLISH)
    }

    productList -= productEntity
  }

  /* HELPER */

  private def isKey(edmEntityType: EdmEntityType, propertyName: String): Boolean = {
    val keyPropertyRefs = edmEntityType.getKeyPropertyRefs
    for (propRef <- keyPropertyRefs.asScala) {
      val keyPropertyName = propRef.getName
      if (keyPropertyName.equals(propertyName)) {
        return true
      }
    }
    false
  }

  private def initSampleData() = {

    // add some sample product entities
    val e1 = new Entity()
      .addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 1))
      .addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Notebook Basic 15"))
      .addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
        "Notebook Basic, 1.7GHz - 15 XGA - 1024MB DDR2 SDRAM - 40GB"))
    e1.setId(createId("Products", 1))
    productList += e1

    val e2 = new Entity()
      .addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 2))
      .addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "1UMTS PDA"))
      .addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
        "Ultrafast 3G UMTS/HSDPA Pocket PC, supports GSM network"))
    e2.setId(createId("Products", 2))
    productList += e2

    val e3 = new Entity()
      .addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 3))
      .addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Ergo Screen"))
      .addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
        "19 Optimum Resolution 1024 x 768 @ 85Hz, resolution 1280 x 960"))
    e3.setId(createId("Products", 3))
    productList += e3
  }

  private def createId(entitySetName: String, id: Int): URI = {
    try {
      new URI(entitySetName + "(" + String.valueOf(id) + ")")
    } catch {
      case e: URISyntaxException =>
        throw new ODataRuntimeException("Unable to create id for entity: " + entitySetName, e)
    }
  }
}