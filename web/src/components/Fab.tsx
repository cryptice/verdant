export function Fab({ onClick, label = '+' }: { onClick: () => void; label?: string }) {
  return (
    <button
      onClick={onClick}
      className="fixed bottom-24 right-4 max-w-2xl w-14 h-14 rounded-full bg-green-primary text-white text-2xl font-bold shadow-lg hover:bg-green-dark transition-colors flex items-center justify-center z-10"
      aria-label={label}
    >
      {label}
    </button>
  )
}
