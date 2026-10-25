/**
 * Upper bound for every `<input type="date">`. Without a max, Chrome accepts a
 * six-digit year and only moves on to the month after the sixth digit; a
 * four-digit cap makes it advance after the year as people expect.
 */
export const DATE_INPUT_MAX = '9999-12-31'
