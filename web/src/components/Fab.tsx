export function Fab({ onClick, label = '+' }: { onClick: () => void; label?: string }) {
  return (
    <button
      onClick={onClick}
      className="fixed bottom-6 right-6 w-10 h-10 rounded-lg bg-accent text-white text-xl font-medium shadow-md hover:bg-accent-hover transition-colors flex items-center justify-center z-10"
      aria-label={label}
    >
      {label}
    </button>
  )
}
