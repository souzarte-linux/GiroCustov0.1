package com.example

import com.example.data.GiroCustoDatabase
import com.example.network.fipe.FipeApi
import com.example.network.fipe.FipeNetworkClient
import com.example.network.viacep.ViaCepApi
import com.example.network.viacep.ViaCepNetworkClient

/**
 * Extensões de conveniência para testes unitários em src/test.
 * Provê acessadores de retrocompatibilidade para que testes legados possam
 * interagir com FipeNetworkClient, ViaCepNetworkClient e GiroCustoDatabase
 * sem exigir propriedades públicas de teste em código de produção.
 */
var FipeNetworkClient.Companion.testApi: FipeApi?
    get() = api
    set(value) = setApi(value)

var ViaCepNetworkClient.Companion.testApi: ViaCepApi?
    get() = api
    set(value) = setApi(value)
