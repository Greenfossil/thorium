package com.greenfossil.thorium

import com.linecorp.armeria.common.HttpResponse


given Conversion[HttpResponse, Result] = Result(_)