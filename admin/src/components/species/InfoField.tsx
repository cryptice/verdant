export function InfoField({ label, value }: { label: string; value: string | number | null | undefined }) {
  return (
    <div>
      <label className="block text-xs font-medium text-[#787774]">{label}</label>
      <span className="text-sm text-[#37352F]">{value ?? '—'}</span>
    </div>
  )
}
