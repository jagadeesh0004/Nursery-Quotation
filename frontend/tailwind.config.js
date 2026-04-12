/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        sans:    ['Poppins', 'system-ui', 'sans-serif'],
        display: ['Poppins', 'system-ui', 'sans-serif'],
        mono:    ['ui-monospace', 'SFMono-Regular', 'monospace'],
      },
      colors: {
        green: {
          50:  '#fdffe6',
          100: '#F3FF90',
          200: '#e2fc3c',
          300: '#9BEC00',
          400: '#4fe800',
          500: '#06D001',
          600: '#05b309',
          700: '#059212',
          800: '#04700d',
          900: '#034d08',
        },
        surface: '#F8FBF8',
        ink:     '#1A2E1A',
        muted:   '#6B7C6B',
      },
      boxShadow: {
        'sm':   '0 1px 3px rgba(0,0,0,.06), 0 1px 2px rgba(0,0,0,.04)',
        'md':   '0 4px 12px rgba(0,0,0,.07), 0 1px 3px rgba(0,0,0,.04)',
        'lg':   '0 8px 28px rgba(0,0,0,.09), 0 2px 6px rgba(0,0,0,.04)',
        'glow': '0 0 0 3px rgba(6, 208, 1, 0.25)',
      },
      borderRadius: {
        'xl':  '0.75rem',
        '2xl': '1rem',
        '3xl': '1.5rem',
      },
      keyframes: {
        'fade-up':  { from: { opacity: 0, transform: 'translateY(10px)' }, to: { opacity: 1, transform: 'translateY(0)' } },
        'fade-in':  { from: { opacity: 0 }, to: { opacity: 1 } },
        'slide-in': { from: { opacity: 0, transform: 'translateX(-16px)' }, to: { opacity: 1, transform: 'translateX(0)' } },
        'scale-in': { from: { opacity: 0, transform: 'scale(.96)' }, to: { opacity: 1, transform: 'scale(1)' } },
        'shimmer':  { '0%': { backgroundPosition: '-200% 0' }, '100%': { backgroundPosition: '200% 0' } },
        'spin':     { to: { transform: 'rotate(360deg)' } },
      },
      animation: {
        'fade-up':  'fade-up .3s ease both',
        'fade-in':  'fade-in .2s ease both',
        'slide-in': 'slide-in .28s ease both',
        'scale-in': 'scale-in .18s ease both',
        'shimmer':  'shimmer 1.6s linear infinite',
        'spin':     'spin .8s linear infinite',
      },
    },
  },
  plugins: [],
}
