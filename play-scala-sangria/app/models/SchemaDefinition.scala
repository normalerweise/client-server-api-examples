package models

import sangria.schema._

import scala.concurrent.Future

/**
 * Defines a GraphQL schema for the current project
 */
object SchemaDefinition {

  val Product =
    ObjectType(
      "Product",
      "A thing that is sold",
      fields[Unit, Product](
        Field("id", StringType,
          Some("The id of the product."),
          tags = ProjectionName("_id") :: Nil,
          resolve = _.value.id),
        Field("name", OptionType(StringType),
          Some("The name of the product."),
          resolve = _.value.name),
        Field("description", OptionType(StringType),
          Some("The description of the product."),
          resolve = _.value.name)
      ))

  val ID = Argument("id", StringType, description = "id of the character")

  val Query = ObjectType(
    "Query", fields[CharacterRepo, Unit](
      Field("product", OptionType(Product),
        arguments = ID :: Nil,
        resolve = ctx => ctx.ctx.getProduct(ctx arg ID)),
      Field("products", ListType(Product),
        resolve = ctx => ctx.ctx.getProducts)
    ))


  val NameArg = Argument("name", StringType, description = "name of the product")
  val DescriptionNameArg = Argument("description", StringType, description = "description of the product")

  val mutationType = ObjectType(
    "Mut", fields[CharacterRepo, Unit](
    Field("updProduct", OptionType(Product),
      arguments = ID :: NameArg :: DescriptionNameArg :: Nil,
      resolve = c => c.ctx.updProduct(
        c.arg(ID), Some(c.arg(NameArg)), Some(c.arg(DescriptionNameArg))))))





//  val MutationType = ObjectType("Mutation", fields[CharacterRepo, Unit](shipMutation))

  val ProductSchema = Schema(Query, Some(mutationType))
}
