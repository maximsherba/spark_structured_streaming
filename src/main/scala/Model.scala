

case class Model(
                  sepal_length: Double,
                  sepal_width:  Double,
                  petal_length: Double,
                  petal_width:  Double
               )

object Model {
  def apply(a: Array[String]): Model =
    Model(
      a(0).toDouble,
      a(1).toDouble,
      a(2).toDouble,
      a(3).toDouble
    )

}
