import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { ValidationResponse } from '../api/types'
import { DesignProvider } from '../design/DesignContext'
import { ExportBar } from './ExportBar'
import { ScannabilityIndicator } from './ScannabilityIndicator'

const blocked: ValidationResponse = {
  score: 0.5,
  passed: false,
  errorCorrectionLevel: 'L',
  version: 3,
  violations: [
    {
      rule: 'logo_coverage',
      severity: 'BLOCKING',
      message: 'O logo destrói 15 codewords no bloco mais atingido, e o nível L corrige no máximo 7 por bloco.',
    },
  ],
}

describe('ScannabilityIndicator', () => {
  it('mostra o veredito em texto, a mensagem do servidor e leva à seção que resolve', async () => {
    const onGoToSection = vi.fn()
    render(<ScannabilityIndicator validation={blocked} status="ready" error={null} onGoToSection={onGoToSection} />)

    expect(screen.getByText('Não escaneia')).toBeInTheDocument()
    expect(screen.getByText(/destrói 15 codewords/)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Ajustar em Logo' }))
    expect(onGoToSection).toHaveBeenCalledWith('logo')
  })

  it('sem conexão diz o problema em vez de um veredito', () => {
    render(<ScannabilityIndicator validation={null} status="error" error="A API não respondeu." onGoToSection={() => {}} />)
    expect(screen.getByText('Não foi possível conferir')).toBeInTheDocument()
  })
})

describe('ExportBar', () => {
  const renderBar = (validation: ValidationResponse | null, status: 'ready' | 'checking' = 'ready') =>
    render(
      <DesignProvider>
        <ExportBar validation={validation} status={status} />
      </DesignProvider>,
    )

  it('fica bloqueado quando o QR não passa, dizendo o porquê', () => {
    renderBar(blocked)
    const button = screen.getByRole('button', { name: /Baixar PNG/ })
    expect(button).toBeDisabled()
    expect(button).toHaveAccessibleDescription('Corrija os itens que bloqueiam para exportar.')
  })

  it('libera quando passa e acompanha o formato escolhido', async () => {
    renderBar({ ...blocked, passed: true, violations: [] })
    await userEvent.click(screen.getByRole('radio', { name: 'SVG' }))
    expect(screen.getByRole('button', { name: /Baixar SVG/ })).toBeEnabled()
  })

  it('espera a validação terminar antes de liberar', () => {
    renderBar({ ...blocked, passed: true, violations: [] }, 'checking')
    expect(screen.getByRole('button', { name: /Baixar PNG/ })).toBeDisabled()
  })
})
