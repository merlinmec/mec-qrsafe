// Limites iguais aos do servidor (LogoImage no qr-render): aqui só servem
// pra avisar antes do envio — quem barra de verdade é a API.
export const MAX_LOGO_BYTES = 1024 * 1024
export const MAX_LOGO_SIDE = 2048
const ACCEPTED = ['image/png', 'image/jpeg']

export const LOGO_ACCEPT = ACCEPTED.join(',')

export class LogoFileError extends Error {}

/** Lê o arquivo escolhido como data URL, conferindo tipo, tamanho e dimensões. */
export async function readLogoFile(file: File): Promise<string> {
  if (!ACCEPTED.includes(file.type)) throw new LogoFileError('Use uma imagem PNG ou JPEG.')
  if (file.size > MAX_LOGO_BYTES) throw new LogoFileError('A imagem passa de 1 MB. Reduza ou comprima antes de enviar.')
  const dataUrl = await new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result))
    reader.onerror = () => reject(new LogoFileError('Não foi possível ler o arquivo.'))
    reader.readAsDataURL(file)
  })
  const image = await loadImage(dataUrl)
  if (image.naturalWidth > MAX_LOGO_SIDE || image.naturalHeight > MAX_LOGO_SIDE) {
    throw new LogoFileError(`A imagem tem ${image.naturalWidth}×${image.naturalHeight} px; o máximo é ${MAX_LOGO_SIDE}×${MAX_LOGO_SIDE}.`)
  }
  return dataUrl
}

export function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => resolve(image)
    image.onerror = () => reject(new LogoFileError('O arquivo não é uma imagem válida.'))
    image.src = src
  })
}

/**
 * Logo de exemplo, desenhado aqui mesmo: um selo com um QR estilizado.
 * Vem pronto pra a primeira tela já mostrar o que o logo faz com a
 * correção de erro, sem depender de arquivo externo.
 */
export function sampleLogoDataUrl(): string {
  const canvas = document.createElement('canvas')
  canvas.width = canvas.height = 256
  const g = canvas.getContext('2d')
  if (!g) return ''
  g.fillStyle = '#E8A60C'
  g.beginPath()
  g.roundRect(8, 8, 240, 240, 56)
  g.fill()
  // Três "olhos" de QR em tinta escura sobre o ouro, como os finder patterns.
  const eye = (x: number, y: number) => {
    g.fillStyle = '#16140F'
    g.fillRect(x, y, 70, 70)
    g.fillStyle = '#E8A60C'
    g.fillRect(x + 12, y + 12, 46, 46)
    g.fillStyle = '#16140F'
    g.fillRect(x + 22, y + 22, 26, 26)
  }
  eye(44, 44)
  eye(142, 44)
  eye(44, 142)
  g.fillStyle = '#16140F'
  for (const [x, y] of [[142, 142], [180, 142], [161, 172], [142, 196], [190, 190]]) g.fillRect(x, y, 22, 22)
  return canvas.toDataURL('image/png')
}
