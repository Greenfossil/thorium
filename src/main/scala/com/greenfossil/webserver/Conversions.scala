package com.greenfossil.webserver

import com.linecorp.armeria.common.HttpResponse

given Conversion[HttpResponse, Result] = Result(_)