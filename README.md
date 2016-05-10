# client-server-api-examples
Example implementations  in play-scala for several API Standards like oData, GraphQl...

##play-scala-olingo
OData 4.0 Server example using the Apache Olingo library
Implements the tutorial described on:
https://olingo.apache.org/doc/odata4/tutorials/write/tutorial_write.html

##play-scala-sangria
GraphQL Server example using the Sangria library
Implements the Apache Olingo "Product" tutorial.
List Products:
CODE
`{
    "query": "query Example { products {  id name description }  }",
    "operationName": "products"
}`

Update Product:
`{
    "query": "mutation Mut($id: String!, $name: String!, $description: String!) { updProduct(id: $id, name: $name, description: $description ) { id name description } }",
    "operationName": "updProduct",
 "variables": {
   "id": "1",
   "name": "new Name",
   "description": "new Description"
}
}`
