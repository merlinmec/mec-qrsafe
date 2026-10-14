import { useState } from 'react'
import { EditorPage } from './components/EditorPage'
import { DesignProvider } from './design/DesignContext'
import { DEFAULT_LOGO_WIDTH, initialDesign, type QrDesign } from './design/state'
import { sampleLogoDataUrl } from './lib/logo'

/**
 * A primeira tela já abre com o logo de exemplo: quem chega pelo portfólio
 * vê de cara a correção de erro trabalhando (o nível sobe sozinho e os
 * blocos mostram o estrago), sem precisar procurar uma imagem.
 */
function firstDesign(): QrDesign {
  const sample = sampleLogoDataUrl()
  if (!sample) return initialDesign
  return {
    ...initialDesign,
    logo: { imageDataUrl: sample, fileName: 'logo-de-exemplo.png', widthPercent: DEFAULT_LOGO_WIDTH, centerXPercent: 50, centerYPercent: 50 },
  }
}

export function App() {
  const [initial] = useState(firstDesign)
  return (
    <DesignProvider initial={initial}>
      <EditorPage />
    </DesignProvider>
  )
}
