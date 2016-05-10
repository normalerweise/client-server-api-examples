package models

import sangria.schema.{Deferred, DeferredResolver}

import scala.collection.mutable
import scala.concurrent.Future
import scala.util.Try


case class Product(
                    id: String,
                    name: Option[String],
                    description: Option[String]
                  )

class CharacterRepo {

  import models.CharacterRepo._

  def getProduct(id: String) =
    products find (p => p.id == id)

  def getProducts = products

  def updProduct(id: String, name: Option[String], description: Option[String]): Option[Product] = {
    val product = products.zipWithIndex.find(p => p._1.id == id)
    if (product.isDefined) {
      var newProduct = product.get._1

      if (name.isDefined)
        newProduct = newProduct.copy(name = name)

      if (description.isDefined)
        newProduct = newProduct.copy(description = description)

      products.update(product.get._2, newProduct)
      Some(newProduct)
    } else {
      None
    }

  }
}

object CharacterRepo {
  val products = new mutable.ListBuffer[Product]
  products +=(
    Product(
      id = "1",
      name = Some("Notebook Basic 15"),
      description = Some("Notebook Basic, 1.7GHz - 15 XGA - 1024MB DDR2 SDRAM - 40GB")
    ),
    Product(
      id = "2",
      name = Some("1UMTS PDA"),
      description = Some("Ultrafast 3G UMTS/HSDPA Pocket PC, supports GSM network")
    ),
    Product(
      id = "3",
      name = Some("Ergo Screen"),
      description = Some("19 Optimum Resolution 1024 x 768 @ 85Hz, resolution 1280 x 960"))
    )
}