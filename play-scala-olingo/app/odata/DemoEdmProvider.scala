package odata

import java.util

import org.apache.olingo.commons.api.edm.provider._
import org.apache.olingo.commons.api.edm.{EdmPrimitiveTypeKind, FullQualifiedName}


object DemoEdmProvider {

  // Service Namespace
  val NAMESPACE = "OData.Demo"

  // EDM Container
  val CONTAINER_NAME = "Container"
  val CONTAINER = new FullQualifiedName(NAMESPACE, CONTAINER_NAME)

  // Entity Types Names
  val ET_PRODUCT_NAME = "Product"
  val ET_PRODUCT_FQN = new FullQualifiedName(NAMESPACE, ET_PRODUCT_NAME)

  // Entity Set Names
  val ES_PRODUCTS_NAME = "Products"

}


/**
  * Created by norman on 4/17/16.
  */
class DemoEdmProvider extends CsdlAbstractEdmProvider {

  import DemoEdmProvider._

  override def getEntityType(entityTypeName: FullQualifiedName): CsdlEntityType = {

    // this method is called for one of the EntityTypes that are configured in the Schema
    if(entityTypeName.equals(ET_PRODUCT_FQN)){

      //create EntityType properties
      val id = new CsdlProperty().setName("ID").setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName)
      val name = new CsdlProperty().setName("Name").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName)
      val description = new CsdlProperty().setName("Description").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName)

      // create CsdlPropertyRef for Key element
      val propertyRef = new CsdlPropertyRef()
      propertyRef.setName("ID")

      // configure EntityType
      val entityType = new CsdlEntityType()
      entityType.setName(ET_PRODUCT_NAME)
      entityType.setProperties(java.util.Arrays.asList(id, name , description))
      entityType.setKey(java.util.Collections.singletonList(propertyRef))

     return entityType
    }
    null
  }

  override def getEntitySet(entityContainer: FullQualifiedName, entitySetName: String): CsdlEntitySet = {
    if(entityContainer.equals(CONTAINER)){
      if(entitySetName.equals(ES_PRODUCTS_NAME)){
        val entitySet = new CsdlEntitySet()
        entitySet.setName(ES_PRODUCTS_NAME)
        entitySet.setType(ET_PRODUCT_FQN)

        return entitySet
      }
    }
    null
  }


  override def getEntityContainer() = {
    // create EntitySets
    val entitySets = new util.ArrayList[CsdlEntitySet]()
    entitySets.add(getEntitySet(CONTAINER, ES_PRODUCTS_NAME))

    // create EntityContainer
    val entityContainer = new CsdlEntityContainer()
    entityContainer.setName(CONTAINER_NAME)
    entityContainer.setEntitySets(entitySets)

    entityContainer
  }


  override def getSchemas(): java.util.List[CsdlSchema] = {

    // create Schema
    val schema = new CsdlSchema()
    schema.setNamespace(NAMESPACE)

    // add EntityTypes
    val entityTypes = new java.util.ArrayList[CsdlEntityType]()
    entityTypes.add(getEntityType(ET_PRODUCT_FQN))
    schema.setEntityTypes(entityTypes)

    // add EntityContainer
    schema.setEntityContainer(getEntityContainer())

    // finally
    val schemas = new java.util.ArrayList[CsdlSchema]()
    schemas.add(schema)

    schemas
  }


  override def getEntityContainerInfo(entityContainerName: FullQualifiedName) : CsdlEntityContainerInfo = {
    // This method is invoked when displaying the Service Document at e.g. http://localhost:8080/DemoService/DemoService.svc
    if (entityContainerName == null || entityContainerName.equals(CONTAINER)) {
      val entityContainerInfo = new CsdlEntityContainerInfo()
      entityContainerInfo.setContainerName(CONTAINER)
      return entityContainerInfo
    }

    null
  }

}
