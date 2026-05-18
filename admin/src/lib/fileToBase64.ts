export function fileToBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const result = reader.result as string
      resolve(result.split(',')[1]) // strip data:image/...;base64,
    }
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}
