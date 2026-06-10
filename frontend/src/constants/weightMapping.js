// Mapping of dimension keys to unit weights (kg)
const WEIGHT_MAP = {
  // Bag dimensions
  '5x6': 0.5,
  '6x8': 0.9,
  '7x8': 1.3,
  '8x10': 3.3,
  '9x10': 4.5,
  '10x12': 6,
  '12x12': 7.5,
  '12x13': 8,
  '13x13': 8,
  '14x15': 16,
  '15x16': 16,
  '16x16': 22,
  '18x18': 30,
  '20x20': 40,
  '21x21': 50,
  '24x24': 75,
  '25x25': 85,
  '30x30': 150,
  '36x36': 220,
  '40x40': 250,
  // Pot sizes (inches)
  '3"': 0.5,
  '4"': 1,
  '5"': 1.5,
  '6"': 2,
  '7"': 3,
  '8"': 4,
  '9"': 6,
  '10"': 8,
  '11"': 10,
  '12"': 12,
  '13"': 13.5,
  '14"': 15,
  '15"': 18,
  '16"': 22,
  '17"': 26,
  '18"': 30,
  '20"': 50,
  '24"': 80,
  '30"': 150,
  '36"': 220,
  '40"': 250,
}

function normalizeDim(dim) {
  if (!dim) return ''
  return String(dim).toLowerCase().replace(/\s+/g, '').replace(/“|”/g, '"')
}

export function getUnitWeightFromDimension(dim) {
  const k = normalizeDim(dim)
  if (!k) return 0
  if (WEIGHT_MAP[k] !== undefined) return WEIGHT_MAP[k]
  // try numeric inch like '3' => '3"'
  if (/^\d+(?:\.\d+)?$/.test(k)) {
    const key = `${k}\"`
    if (WEIGHT_MAP[key] !== undefined) return WEIGHT_MAP[key]
  }
  // extract first number as fallback
  const m = k.match(/(\d+(?:\.\d+)?)/)
  if (m) return parseFloat(m[1]) || 0
  return 0
}

export function formatWeight(kg) {
  const n = Number(kg) || 0
  // Round to 1 significant figure
  function roundToSignificant(x, sig = 1) {
    if (x === 0) return 0
    const d = Math.ceil(Math.log10(Math.abs(x)))
    const factor = Math.pow(10, sig - d)
    return Math.round(x * factor) / factor
  }

  if (n >= 1000) {
    const tons = n / 1000
    return `${tons.toFixed(1)} tons`
  }

  return `${n.toFixed(1)} kg`
}

export default WEIGHT_MAP
