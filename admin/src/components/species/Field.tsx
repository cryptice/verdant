export function Field({ label, value, onChange, onBlur, type = 'text', className = '' }: {
  label: string; value: string; onChange: (v: string) => void; onBlur?: () => void; type?: string; className?: string
}) {
  return (
    <div className={className}>
      <label className="block text-xs font-medium text-[#787774] mb-1.5">{label}</label>
      <input
        type={type}
        value={value}
        onChange={e => onChange(e.target.value)}
        onBlur={onBlur}
        className="w-full px-3 py-2 border border-[#E9E9E7] rounded-md focus:ring-2 focus:ring-[#2EAADC]/30 focus:border-[#2EAADC] outline-none text-sm bg-[#FBFBFA]"
      />
    </div>
  )
}
