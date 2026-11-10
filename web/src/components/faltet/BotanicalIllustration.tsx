/** Decorative linework for the growing overview; deliberately absent from the accessibility tree. */
export function BotanicalIllustration() {
  return (
    <svg className="botanical-art" viewBox="0 0 360 320" fill="none" aria-hidden="true" focusable="false">
      <circle cx="222" cy="153" r="135" fill="currentColor" opacity="0.04" />
      <circle cx="222" cy="153" r="112" stroke="currentColor" opacity="0.1" />
      <g stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" strokeLinejoin="round">
        <path d="M152 329C188 255 212 197 218 105M181 257C148 215 128 169 110 134M197 217C239 194 267 160 282 124" />
        <path d="M191 236C149 237 131 209 137 183C168 184 187 206 191 236Z" fill="currentColor" fillOpacity="0.08" />
        <path d="M200 204C222 177 246 173 262 183C248 210 222 220 200 204Z" fill="currentColor" fillOpacity="0.08" />
        <path d="M169 286C204 248 232 246 244 257C226 286 196 295 169 286Z" fill="currentColor" fillOpacity="0.08" />
        <path d="M154 213C118 210 96 187 101 168C130 174 146 191 154 213Z" />
        <path d="M214 155C184 149 172 129 176 111C200 115 213 130 214 155Z" />
        <path d="M137 183L191 236M200 204L262 183M169 286L244 257" opacity="0.5" />
        <g transform="translate(219 79)">
          {[0, 45, 90, 135, 180, 225, 270, 315].map(angle => (
            <ellipse key={angle} cx="0" cy="-25" rx="12" ry="24" transform={`rotate(${angle})`} fill="currentColor" fillOpacity="0.035" />
          ))}
          <circle r="11" fill="#dfbb69" stroke="#dfbb69" />
          <circle r="5" stroke="#244c3b" />
        </g>
        <g transform="translate(108 116) rotate(-18)">
          {[0, 60, 120, 180, 240, 300].map(angle => (
            <ellipse key={angle} cx="0" cy="-17" rx="9" ry="17" transform={`rotate(${angle})`} />
          ))}
          <circle r="7" fill="#dfbb69" stroke="#dfbb69" />
        </g>
        <path d="M282 125C254 110 257 90 265 85C270 98 276 101 282 104C282 85 292 75 299 77C305 96 299 113 282 125Z" fill="currentColor" fillOpacity="0.12" />
      </g>
      <g fill="currentColor" opacity="0.4"><circle cx="57" cy="225" r="2" /><circle cx="300" cy="211" r="2" /><circle cx="149" cy="43" r="1.5" /></g>
    </svg>
  )
}
