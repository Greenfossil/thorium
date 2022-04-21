package com.greenfossil.webserver

import com.greenfossil.commons.data.Field
import com.greenfossil.commons.json.JsValue
import com.linecorp.armeria.common.HttpMethod

import scala.util.Try

extension[A](field: Field[A])
  def bindFromRequest()(using request: com.greenfossil.webserver.Request): Field[A] =
    val queryData: List[(String, String)] =
      request.method() match {
        case HttpMethod.POST | HttpMethod.PUT | HttpMethod.PATCH => Nil
        case _ => request.queryParamsList
      }

    request match {

//      case req if req.asMultipartFormData.bodyPart.nonEmpty =>
//        bind(req.asMultipartFormData.asFormUrlEncoded ++ querydata)

      case req if Try(req.asJson).isSuccess =>
//        bind(req.asJson, querydata)
        field.bind(request.asJson)

      case req =>
        field.bind(req.asFormUrlEncoded ++ queryData.groupMap(_._1)(_._2))
    }
