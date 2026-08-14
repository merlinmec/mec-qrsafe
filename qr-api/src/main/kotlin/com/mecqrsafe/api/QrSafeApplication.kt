package com.mecqrsafe.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class QrSafeApplication

fun main(args: Array<String>) {
    runApplication<QrSafeApplication>(*args)
}
