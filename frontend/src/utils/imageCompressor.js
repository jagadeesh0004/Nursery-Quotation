/**
 * Image Compression & Aspect Ratio Handler
 * - Compresses images while maintaining quality
 * - Handles aspect ratios (1:1, 4:3, etc.)
 * - Max file size: 500KB
 * - Quality: 75% (good quality, small size)
 */

/**
 * Compress image to target size with aspect ratio
 * @param {File} file - The image file to compress
 * @param {Object} options - Configuration
 * @param {number} options.maxWidth - Max width in pixels (default: 1024)
 * @param {number} options.maxHeight - Max height in pixels (default: 1024)
 * @param {number} options.quality - Quality 0-1 (default: 0.75)
 * @param {number} options.maxSizeKB - Max file size in KB (default: 500)
 * @param {string} options.aspectRatio - Aspect ratio '1:1', '4:3', etc. (default: null - preserve original)
 * @returns {Promise<Blob>} Compressed image blob
 */
export async function compressImage(file, options = {}) {
  const {
    maxWidth = 1024,
    maxHeight = 1024,
    quality = 0.75,
    maxSizeKB = 500,
    aspectRatio = null,
  } = options

  return new Promise((resolve, reject) => {
    const reader = new FileReader()

    reader.onload = (event) => {
      const img = new Image()

      img.onload = () => {
        try {
          // Calculate dimensions with aspect ratio
          let width = img.width
          let height = img.height

          if (aspectRatio) {
            ;({ width, height } = cropToAspectRatio(width, height, aspectRatio))
          }

          // Scale down if too large
          if (width > maxWidth || height > maxHeight) {
            const ratio = Math.min(maxWidth / width, maxHeight / height)
            width *= ratio
            height *= ratio
          }

          // Create canvas and draw image
          const canvas = document.createElement('canvas')
          canvas.width = width
          canvas.height = height

          const ctx = canvas.getContext('2d')
          if (!ctx) throw new Error('Could not get canvas context')

          // Draw with cropping if aspect ratio specified
          if (aspectRatio) {
            let cropWidth = img.width
            let cropHeight = img.height

            // Calculate actual crop box from source image
            const [ratioW, ratioH] = aspectRatio.split(':').map(Number)
            const targetRatio = ratioW / ratioH
            const currentRatio = img.width / img.height

            if (currentRatio > targetRatio) {
              // Source is wider, crop horizontal
              cropWidth = img.height * targetRatio
            } else {
              // Source is taller, crop vertical
              cropHeight = img.width / targetRatio
            }

            const cropX = (img.width - cropWidth) / 2
            const cropY = (img.height - cropHeight) / 2

            ctx.drawImage(img, cropX, cropY, cropWidth, cropHeight, 0, 0, width, height)
          } else {
            ctx.drawImage(img, 0, 0, width, height)
          }

          // Compress to blob
          canvas.toBlob(
            (blob) => {
              if (!blob) throw new Error('Could not create blob')

              // Check file size
              if (blob.size > maxSizeKB * 1024) {
                // Recursively compress with lower quality
                const lowerQuality = quality * 0.9
                if (lowerQuality < 0.3) {
                  reject(new Error('Could not compress image to required size'))
                  return
                }
                compressImage(file, {
                  maxWidth,
                  maxHeight,
                  quality: lowerQuality,
                  maxSizeKB,
                  aspectRatio,
                })
                  .then(resolve)
                  .catch(reject)
              } else {
                resolve(blob)
              }
            },
            'image/jpeg',
            quality
          )
        } catch (err) {
          reject(err)
        }
      }

      img.onerror = () => reject(new Error('Could not load image'))
      img.src = event.target.result
    }

    reader.onerror = () => reject(new Error('Could not read file'))
    reader.readAsDataURL(file)
  })
}

/**
 * Calculate crop dimensions for aspect ratio
 * @param {number} width - Original width
 * @param {number} height - Original height
 * @param {string} aspectRatio - Aspect ratio like '1:1', '4:3', '16:9'
 * @returns {{width: number, height: number}} Cropped dimensions
 */
function cropToAspectRatio(width, height, aspectRatio) {
  const [ratioW, ratioH] = aspectRatio.split(':').map(Number)
  const targetRatio = ratioW / ratioH
  const currentRatio = width / height

  if (currentRatio > targetRatio) {
    // Width is too large
    const newWidth = height * targetRatio
    return { width: Math.round(newWidth), height: Math.round(height) }
  } else {
    // Height is too large
    const newHeight = width / targetRatio
    return { width: Math.round(width), height: Math.round(newHeight) }
  }
}

/**
 * Create File from Blob with timestamp
 * @param {Blob} blob - The compressed blob
 * @param {string} originalName - Original filename
 * @returns {File} File object
 */
export function blobToFile(blob, originalName = 'image.jpg') {
  const timestamp = Date.now()
  const name = `${timestamp}_${originalName}`
  return new File([blob], name, { type: blob.type })
}

/**
 * Format file size for display
 * @param {number} bytes - File size in bytes
 * @returns {string} Formatted size (e.g., "2.5 MB")
 */
export function formatFileSize(bytes) {
  if (bytes === 0) return '0 Bytes'
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i]
}

/**
 * Validate image file
 * @param {File} file - File to validate
 * @returns {{valid: boolean, error?: string}} Validation result
 */
export function validateImageFile(file) {
  // Check file type strictly
  const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
  if (!allowedTypes.includes(file.type)) {
    return { valid: false, error: 'Invalid file type. Only JPG, PNG, GIF, WEBP allowed.' }
  }

  // Check file size (before compression)
  const maxSizeMB = 5
  if (file.size > maxSizeMB * 1024 * 1024) {
    return { valid: false, error: `File must be less than ${maxSizeMB}MB` }
  }

  return { valid: true }
}
