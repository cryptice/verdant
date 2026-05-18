export function ChipToggle({ label, selected, onClick }: { label: string; selected: boolean; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`px-2.5 py-1 rounded-md text-sm transition-colors ${
        selected
          ? 'bg-[#37352F] text-white'
          : 'bg-[#F0F0EE] text-[#787774] hover:bg-[#E9E9E7]'
      }`}
    >
      {label}
    </button>
  )
}
