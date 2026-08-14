package com.mecqrsafe.qrengine

/**
 * Nível de correção de erro Reed-Solomon do QR (% de codewords que podem
 * ser perdidos/cobertos e ainda assim o QR ser lido corretamente).
 *
 * O `qr-validation` sobe automaticamente o nível conforme o logo cresce
 * (ver "Auto-upgrade de correção de erro" na seção 5 de
 * `DECISOES-E-TRADEOFFS.txt`) — a ordem importa: sobe a correção primeiro,
 * valida a área do logo depois.
 */
enum class ErrorCorrectionLevel(val recoveryCapacityPercent: Int) {
    L(7),
    M(15),
    Q(25),
    H(30),
}
