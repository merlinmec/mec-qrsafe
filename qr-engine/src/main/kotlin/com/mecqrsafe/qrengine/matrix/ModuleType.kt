package com.mecqrsafe.qrengine.matrix

/**
 * O que cada módulo da matriz REPRESENTA, além do bit preto/branco.
 *
 * Decisão de design (seção 4 de `DECISOES-E-TRADEOFFS.txt`): sem isso,
 * o qr-validation precisaria reimplementar a geometria de posicionamento
 * (onde ficam os finder patterns, a quiet zone etc.) só pra saber "esse
 * módulo pode ser coberto por um logo ou não" — duplicando conhecimento
 * que o encoder já tem. Com o tipo explícito, o validador só consulta.
 */
enum class ModuleType {
    FINDER_PATTERN,
    SEPARATOR,
    ALIGNMENT_PATTERN,
    TIMING_PATTERN,
    FORMAT_INFO,
    VERSION_INFO,

    /** Módulo sempre escuro ao lado do format info de baixo-esquerda (exigido pela spec). */
    DARK_MODULE,
    DATA,
    QUIET_ZONE,
}
