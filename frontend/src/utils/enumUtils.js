// Convert string values like "new", "creditsPayment", "full-return" to enum-friendly
// format: UPPER_SNAKE_CASE (e.g., NEW, CREDITS_PAYMENT, FULL_RETURN)
export function toEnum(value) {
  if (value == null) return value;
  let s = String(value);
  if (!s) return s;
  // Insert underscores for camelCase boundaries
  s = s.replace(/([a-z0-9])([A-Z])/g, "$1_$2");
  // Normalize hyphens/spaces to underscores
  s = s.replace(/[\-\s]+/g, "_");
  // Collapse multiple underscores and trim
  s = s.replace(/_+/g, "_").replace(/^_+|_+$/g, "");
  return s.toUpperCase();
}
