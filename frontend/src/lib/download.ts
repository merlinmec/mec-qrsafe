export function downloadBlob(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  // Dá tempo do browser começar o download antes de liberar a URL.
  window.setTimeout(() => URL.revokeObjectURL(url), 1000)
}
