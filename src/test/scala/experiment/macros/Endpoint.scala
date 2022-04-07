package experiment.macros


object In {

}

extension (x: String) def /(arg: String) : In[Unit] = In(Tuple(x)) / arg
extension (x: String) def /(arg: In[_]) : In[_] = In(Tuple(x)) / arg

def path[I](x: String) = In[I](Tuple(x))

case class In[I](path: Tuple) {
  def /[I](arg: String): In[I] = copy(path = path ++ Tuple(arg))
  def /[I](arg: In[I]): In[I] = copy(path = path ++ arg.path)
}

def jsonBody[A]: Out[A] = ???

object Out {

}

case class Out[O](out: Tuple) {
  def example(ex: Any): Out[O] = ???
}

trait ErrorOut[E]

object Endpoint {

  def apply(path: String): Endpoint[_, _, _] = ???

  def get: Endpoint[Unit,Unit,Unit] = Endpoint("GET", null, null, null)
}

type HTTP_METHOD = "GET" | "POST"

case class Endpoint[I,O,E](method: HTTP_METHOD, _in: In[I],  _out: Out[O], _errorOut: ErrorOut[E]) {
  def get: Endpoint[I, O, E]  = copy(method = "GET")
  def post: Endpoint[I, O, E] = copy(method = "POST")
  def in[I](in: In[I]): Endpoint[I, O, E] = copy(_in = in)
  def out[O](out: Out[O]): Endpoint[I,O, E] = copy(_out = out)
  def errorOut[E](errorOut: ErrorOut[E]): Endpoint[I,O, E] = copy(_errorOut = errorOut)
}

